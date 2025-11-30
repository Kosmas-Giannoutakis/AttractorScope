AttractorScope {
    classvar scopeInstances;
    classvar <defaultDelayTime = 0.01;

    var makeGui, setDelayTime1, setIndex, setRate, setTrailLength, setResolution, setDimension, setRotationSpeed, setColor, setZoom, setStyle, setAutoRotate, setFps;
    var updateColors, rotateX, rotateY, project;
    var rotate4D_XW, rotate4D_YW, rotate4D_ZW, project4Dto3D;
    var rotate5D_XW, rotate5D_YW, rotate5D_ZW, rotate5D_XV, rotate5D_YV, project5Dto4D;
    var rotate6D_XW, rotate6D_YW, rotate6D_ZW, rotate6D_XV, rotate6D_YV, rotate6D_ZV, project6Dto5D;
    var drawLines, drawPoints, drawGlow, drawRibbon, drawHeatMap, drawSmooth;
    var getPointColor, precalculateColors;
    var interpolatePoints;

    var <window, <view, <scopeView;
    var delay1Slider, trailSlider, resolutionSlider, rotationSpeedSlider, zoomSlider;
    var delay1Box, trailBox, resolutionBox, rotationSpeedBox, zoomBox, idxNumBox, rateMenu, dimensionMenu, colorMenu, styleMenu, autoRotateCheckBox, exportButton, fpsBox;

    var <server, synth;
    var maxDelayTime, maxBufSize;
    var aBusSpec, cBusSpec, delaySpec, trailSpec, resolutionSpec, rotationSpeedSpec, zoomSpec, fpsSpec;
    var <>smallSize, <>largeSize;

    var <bus;
    var busSpec;
    var <delayTime1, <delayTime2, <delayTime3, <delayTime4, <delayTime5, <trailLength, <resolution, <dimension, <rotationSpeed, <colorChoice, <drawStyle, <fps;
    var <angleX, <angleY, <angle4D_XW, <angle4D_YW, <angle4D_ZW;
    var <angle5D_XW, <angle5D_YW, <angle5D_ZW, <angle5D_XV, <angle5D_YV;
    var <angle6D_XW, <angle6D_YW, <angle6D_ZW, <angle6D_XV, <angle6D_YV, <angle6D_ZV;
    var <scale, baseScale, <zoom;
    var <autoRotate;
    var <lastMouseX, <lastMouseY;
    var sizeToggle = false;
    var points;
    var <lineColor;
    var readTask;
    var prevPoints;
    var pointInterpolation = 0.9;

    *new {
        arg server, index = 0, bufsize = 4096,
        delayTime1, trailLength = 500, resolution = 800,
        dimension = 3, rotationSpeed = 1.0, zoom = 1.0, rate = \audio, view;
        var bus;

        if(server.isNil) { server = Server.default };
        if(server.isLocal.not) { Error("Can not scope on remote server.").throw };

        bus = if(index.class === Bus) {
            index
        } {
            Bus(rate, index, 1, server)
        };

        ^super.new.initAttractorScope(server, view, bus, bufsize, delayTime1,
            trailLength, resolution, dimension, rotationSpeed, zoom);
    }

    initAttractorScope { arg server_, parent, bus_, bufsize_, delayTime1_,
        trailLength_, resolution_, dimension_, rotationSpeed_, zoom_;

        server = server_;
        synth = AttractorScopeSynth(server);

        maxBufSize = max(bufsize_, 128);
        maxDelayTime = 0.05;
        bus = bus_;

        aBusSpec = ControlSpec(0, server.options.numAudioBusChannels, step: 1);
        cBusSpec = ControlSpec(0, server.options.numControlBusChannels, step: 1);
        busSpec = if(bus.rate === \audio) { aBusSpec } { cBusSpec };

        delaySpec = ControlSpec(0.0001, maxDelayTime, \exponential);
        trailSpec = ControlSpec(10, 5000, \exponential, step: 1);
        resolutionSpec = ControlSpec(100, 2000, \exponential, step: 1);
        rotationSpeedSpec = ControlSpec(0.1, 5.0, \exponential);
        zoomSpec = ControlSpec(0.25, 4.0, \exponential);
        fpsSpec = ControlSpec(10, 120, \lin, 1);

        delayTime1 = delaySpec.constrain(delayTime1_ ? defaultDelayTime);
        delayTime2 = delayTime1 * 2;
        delayTime3 = delayTime1 * 3;
        delayTime4 = delayTime1 * 4;
        delayTime5 = delayTime1 * 5;
        trailLength = trailSpec.constrain(trailLength_);
        resolution = resolutionSpec.constrain(resolution_);
        dimension = dimension_.clip(2, 6);
        rotationSpeed = rotationSpeedSpec.constrain(rotationSpeed_);
        zoom = zoomSpec.constrain(zoom_ ? 1.0);
        colorChoice = 0;
        drawStyle = 0;
        fps = 30;

        smallSize = Size(890, 830);
        largeSize = Size(1160, 1130);

        angleX = 0.6;
        angleY = 0.8;
        angle4D_XW = 0.0;
        angle4D_YW = 0.0;
        angle4D_ZW = 0.0;
        angle5D_XW = 0.0;
        angle5D_YW = 0.0;
        angle5D_ZW = 0.0;
        angle5D_XV = 0.0;
        angle5D_YV = 0.0;
        angle6D_XW = 0.0;
        angle6D_YW = 0.0;
        angle6D_ZW = 0.0;
        angle6D_XV = 0.0;
        angle6D_YV = 0.0;
        angle6D_ZV = 0.0;
        baseScale = 150;
        scale = baseScale * zoom;
        autoRotate = true;
        lastMouseX = nil;
        lastMouseY = nil;
        points = [];
        prevPoints = [];

        lineColor = Color.new255(255, 218, 0);

        // Proper trail fade using pow - newest points are brightest
        precalculateColors = { arg numPoints, screenPoints, originalPoints;
            var colors;

            colors = Array.newClear(numPoints);

            // Choose the fastest path based on color mode
            colorChoice.switch(
                9, {  // Rainbow
                    var timeOffset = (SystemClock.seconds * 0.1 % 1.0);
                    var hueStep = 1.0 / numPoints;

                    numPoints.do { |i|
                        var alpha, hue, t;
                        t = i / (numPoints - 1);  // 0.0 to 1.0
                        alpha = t.pow(0.5);  // Pow fade
                        hue = (i * hueStep + timeOffset) % 1.0;
                        colors[i] = Color.hsv(hue, 0.8, 1.0, alpha);
                    };
                },
                10, {  // Velocity
                    numPoints.do { |i|
                        var alpha, hue, velocity, dx, dy, t;
                        t = i / (numPoints - 1);
                        alpha = t.pow(0.5);  // Pow fade

                        if(i > 0 and: { screenPoints.notNil }) {
                            dx = screenPoints[i].x - screenPoints[i-1].x;
                            dy = screenPoints[i].y - screenPoints[i-1].y;
                            velocity = sqrt((dx*dx) + (dy*dy));
                            hue = velocity.linlin(0, 50, 0.6, 0.0).clip(0, 1);
                        } {
                            hue = 0.6;
                        };
                        colors[i] = Color.hsv(hue, 0.9, 1.0, alpha);
                    };
                },
                11, {  // Distance from origin
                    numPoints.do { |i|
                        var alpha, hue, distance, pt, t;
                        t = i / (numPoints - 1);
                        alpha = t.pow(0.5);  // Pow fade

                        if(originalPoints.notNil and: { originalPoints[i].notNil }) {
                            pt = originalPoints[i];
                            distance = switch(pt.size,
                                2, { sqrt((pt[0]*pt[0]) + (pt[1]*pt[1])) },
                                3, { sqrt((pt[0]*pt[0]) + (pt[1]*pt[1]) + (pt[2]*pt[2])) },
                                4, { sqrt((pt[0]*pt[0]) + (pt[1]*pt[1]) + (pt[2]*pt[2]) + (pt[3]*pt[3])) },
                                5, { sqrt((pt[0]*pt[0]) + (pt[1]*pt[1]) + (pt[2]*pt[2]) + (pt[3]*pt[3]) + (pt[4]*pt[4])) },
                                6, { sqrt((pt[0]*pt[0]) + (pt[1]*pt[1]) + (pt[2]*pt[2]) + (pt[3]*pt[3]) + (pt[4]*pt[4]) + (pt[5]*pt[5])) }
                            );
                            hue = distance.linlin(0, 1, 0.66, 0.0).clip(0, 1);
                        } {
                            hue = 0.5;
                        };
                        colors[i] = Color.hsv(hue, 0.9, 1.0, alpha);
                    };
                },
                12, {  // Curvature
                    numPoints.do { |i|
                        var alpha, hue, t;
                        t = i / (numPoints - 1);
                        alpha = t.pow(0.5);  // Pow fade

                        if(i > 0 and: { i < (numPoints - 1) } and: { screenPoints.notNil }) {
                            var v1x, v1y, v2x, v2y, mag1, mag2, dot, angle, curvature;
                            var p0, p1, p2;

                            p0 = screenPoints[i-1];
                            p1 = screenPoints[i];
                            p2 = screenPoints[i+1];

                            v1x = p1.x - p0.x;
                            v1y = p1.y - p0.y;
                            v2x = p2.x - p1.x;
                            v2y = p2.y - p1.y;

                            mag1 = sqrt((v1x*v1x) + (v1y*v1y));
                            mag2 = sqrt((v2x*v2x) + (v2y*v2y));

                            if(mag1 > 0.01 and: { mag2 > 0.01 }) {
                                dot = (v1x*v2x) + (v1y*v2y);
                                angle = acos((dot / (mag1 * mag2)).clip(-1, 1));
                                curvature = angle / pi;
                                hue = curvature.linlin(0, 0.5, 0.33, 0.0).clip(0, 1);
                            } {
                                hue = 0.33;
                            };
                        } {
                            hue = 0.33;
                        };
                        colors[i] = Color.hsv(hue, 0.9, 1.0, alpha);
                    };
                },
                {  // Static colors (0-8)
                    var baseColor = lineColor;
                    numPoints.do { |i|
                        var alpha, t;
                        t = i / (numPoints - 1);
                        alpha = t.pow(0.5);  // Pow fade
                        colors[i] = baseColor.copy.alpha_(alpha);
                    };
                }
            );

            colors;
        };

        // Helper function to interpolate points between frames
        interpolatePoints = { arg screenPoints;
            var interpolatedPoints;

            if(prevPoints.size != screenPoints.size) {
                prevPoints = screenPoints.copy;
            };

            interpolatedPoints = screenPoints.collect { |pt, i|
                var prevPt = prevPoints[i] ? pt;
                Point(
                    prevPt.x + (pointInterpolation * (pt.x - prevPt.x)),
                    prevPt.y + (pointInterpolation * (pt.y - prevPt.y))
                );
            };

            prevPoints = interpolatedPoints.copy;
            interpolatedPoints;
        };

        // Function to get color for a point (kept for compatibility)
        getPointColor = { arg i, numPoints, screenPoints, originalPoints;
            var baseAlpha, hue, velocity, distance, curvature;
            var v1x, v1y, v2x, v2y, angle, dot, mag1, mag2;

            colorChoice.switch(
                9, {  // Rainbow
                    hue = (i / numPoints + (SystemClock.seconds * 0.1 % 1.0)) % 1.0;
                    Color.hsv(hue, 0.8, 1.0, 1.0);
                },
                10, {  // Velocity
                    if(i > 0) {
                        velocity = screenPoints[i].dist(screenPoints[i-1]);
                        hue = velocity.linlin(0, 50, 0.6, 0.0).clip(0, 1);
                    } {
                        hue = 0.6;
                    };
                    Color.hsv(hue, 0.9, 1.0, 1.0);
                },
                11, {  // Distance from origin
                    if(originalPoints.notNil and: { originalPoints[i].notNil }) {
                        distance = sqrt(originalPoints[i].collect({ |v| v*v }).sum);
                        hue = distance.linlin(0, 1, 0.66, 0.0).clip(0, 1);
                    } {
                        hue = 0.5;
                    };
                    Color.hsv(hue, 0.9, 1.0, 1.0);
                },
                12, {  // Curvature
                    if(i > 0 and: { i < (screenPoints.size - 1) }) {
                        v1x = screenPoints[i].x - screenPoints[i-1].x;
                        v1y = screenPoints[i].y - screenPoints[i-1].y;
                        v2x = screenPoints[i+1].x - screenPoints[i].x;
                        v2y = screenPoints[i+1].y - screenPoints[i].y;

                        mag1 = sqrt((v1x*v1x) + (v1y*v1y));
                        mag2 = sqrt((v2x*v2x) + (v2y*v2y));

                        if(mag1 > 0.01 and: { mag2 > 0.01 }) {
                            dot = (v1x*v2x) + (v1y*v2y);
                            angle = acos((dot / (mag1 * mag2)).clip(-1, 1));
                            curvature = angle / pi;
                            hue = curvature.linlin(0, 0.5, 0.33, 0.0).clip(0, 1);
                        } {
                            hue = 0.33;
                        };
                    } {
                        hue = 0.33;
                    };
                    Color.hsv(hue, 0.9, 1.0, 1.0);
                },
                {  // Static colors
                    lineColor;
                }
            );
        };

        // Drawing style functions
        drawLines = { arg interpolatedPoints, bounds, numPoints;
            var colors;

            Pen.width = 1.5;
            Pen.joinStyle = 1;
            Pen.capStyle = 1;

            colors = precalculateColors.value(numPoints, interpolatedPoints, points);

            Pen.use {
                Pen.moveTo(interpolatedPoints[0]);
                Pen.strokeColor = colors[0];

                (numPoints - 1).do { |i|
                    var idx = i + 1;
                    Pen.lineTo(interpolatedPoints[idx]);
                    Pen.strokeColor = colors[idx];
                    if(i < (numPoints - 2)) {
                        Pen.stroke;
                        Pen.moveTo(interpolatedPoints[idx]);
                    } {
                        Pen.stroke;
                    };
                };
            };
        };

        drawPoints = { arg interpolatedPoints, bounds, numPoints;
            var colors, pointSize;

            pointSize = (3 * zoom).clip(2, 5.0);

            colors = precalculateColors.value(numPoints, interpolatedPoints, points);

            interpolatedPoints.do { |pt, i|
                Pen.fillColor = colors[i];
                Pen.fillOval(Rect(pt.x - (pointSize/2), pt.y - (pointSize/2), pointSize, pointSize));
            };
        };

        drawSmooth = { arg interpolatedPoints, bounds, numPoints;
            var colors;

            Pen.width = 2;
            Pen.joinStyle = 1;
            Pen.capStyle = 1;

            if(numPoints < 4) {
                drawLines.value(interpolatedPoints, bounds, numPoints);
            } {
                colors = precalculateColors.value(numPoints, interpolatedPoints, points);

                Pen.use {
                    (numPoints - 1).do { |i|
                        var p0, p1, p2, p3, segments;

                        p0 = if(i > 0) { interpolatedPoints[i-1] } { interpolatedPoints[i] };
                        p1 = interpolatedPoints[i];
                        p2 = interpolatedPoints[i+1];
                        p3 = if(i < (numPoints - 2)) { interpolatedPoints[i+2] } { interpolatedPoints[i+1] };

                        segments = 6;

                        Pen.moveTo(p1);
                        segments.do { |t|
                            var tt = (t + 1) / segments;
                            var tt2 = tt * tt;
                            var tt3 = tt2 * tt;
                            var q1, q2, q3, q4, px, py;

                            q1 = tt3.neg + (2 * tt2) - tt;
                            q2 = (3 * tt3) - (5 * tt2) + 2;
                            q3 = (-3 * tt3) + (4 * tt2) + tt;
                            q4 = tt3 - tt2;

                            px = 0.5 * ((p0.x * q1) + (p1.x * q2) + (p2.x * q3) + (p3.x * q4));
                            py = 0.5 * ((p0.y * q1) + (p1.y * q2) + (p2.y * q3) + (p3.y * q4));

                            Pen.lineTo(Point(px, py));
                        };

                        Pen.strokeColor = colors[i];
                        Pen.stroke;
                    };
                };
            };
        };

        drawGlow = { arg interpolatedPoints, bounds, numPoints;
            var colors, glowLevels;

            glowLevels = 2;

            colors = precalculateColors.value(numPoints, interpolatedPoints, points);

            Pen.use {
                var coreSize;
                glowLevels.do { |level|
                    var size = (level + 1) * 1.8 * (zoom + 1);
                    var alphaScale = (1 - (level / glowLevels)).pow(1.5) * 0.5;

                    interpolatedPoints.do { |pt, i|
                        var glowColor = colors[i].copy;
                        glowColor.alpha = glowColor.alpha * alphaScale;
                        Pen.fillColor = glowColor;
                        Pen.fillOval(Rect(pt.x - (size/2), pt.y - (size/2), size, size));
                    };
                };

                coreSize = 1.5;
                interpolatedPoints.do { |pt, i|
                    Pen.fillColor = colors[i];
                    Pen.fillOval(Rect(pt.x - (coreSize/2), pt.y - (coreSize/2), coreSize, coreSize));
                };
            };
        };

        drawRibbon = { arg interpolatedPoints, bounds, numPoints;
            var colors, ribbonWidth;

            ribbonWidth = 1.2;
            Pen.joinStyle = 1;
            Pen.capStyle = 1;

            colors = precalculateColors.value(numPoints, interpolatedPoints, points);

            Pen.use {
                (numPoints - 1).do { |i|
                    var p1, p2, perp, px, py, norm;
                    var ribbonColor;

                    p1 = interpolatedPoints[i];
                    p2 = interpolatedPoints[i+1];

                    px = p2.y - p1.y;
                    py = (p2.x - p1.x).neg;
                    norm = sqrt((px*px) + (py*py));

                    if(norm > 0.01) {
                        px = (px / norm) * ribbonWidth;
                        py = (py / norm) * ribbonWidth;

                        ribbonColor = colors[i];

                        Pen.fillColor = ribbonColor;
                        Pen.moveTo(Point(p1.x - px, p1.y - py));
                        Pen.lineTo(Point(p1.x + px, p1.y + py));
                        Pen.lineTo(Point(p2.x + px, p2.y + py));
                        Pen.lineTo(Point(p2.x - px, p2.y - py));
                        Pen.fill;
                    };
                };
            };
        };

        drawHeatMap = { arg screenPoints, bounds, numPoints;
            var gridSize, grid, maxVisits;
            var cellWidth, cellHeight;
            var gridW, gridH;

            gridSize = 50;
            gridW = bounds.width;
            gridH = bounds.height;
            cellWidth = gridW / gridSize;
            cellHeight = gridH / gridSize;

            grid = Array.fill(gridSize * gridSize, 0);

            screenPoints.do { |pt|
                var gridX, gridY, idx;
                gridX = (pt.x / cellWidth).floor.asInteger.clip(0, gridSize - 1);
                gridY = (pt.y / cellHeight).floor.asInteger.clip(0, gridSize - 1);
                idx = (gridY * gridSize) + gridX;
                grid[idx] = grid[idx] + 1;
            };

            maxVisits = 1;
            grid.do { |visits|
                if(visits > maxVisits) { maxVisits = visits };
            };

            Pen.use {
                gridSize.do { |x|
                    gridSize.do { |y|
                        var idx, visits, intensity, hue, color;
                        idx = (y * gridSize) + x;
                        visits = grid[idx];

                        if(visits > 0) {
                            intensity = (visits / maxVisits).sqrt;
                            hue = intensity.linlin(0, 1, 0.66, 0.0);
                            color = Color.hsv(hue, 0.9, intensity, intensity * 0.8);

                            Pen.fillColor = color;
                            Pen.fillRect(Rect(x * cellWidth, y * cellHeight, cellWidth, cellHeight));
                        };
                    };
                };
            };
        };

        // 3D rotations
        rotateX = { |point, angle|
            var y = point[1], z = point[2];
            var cosA = cos(angle), sinA = sin(angle);
            [point[0], (y * cosA) - (z * sinA), (y * sinA) + (z * cosA)];
        };

        rotateY = { |point, angle|
            var x = point[0], z = point[2];
            var cosA = cos(angle), sinA = sin(angle);
            [(x * cosA) + (z * sinA), point[1], (z * cosA) - (x * sinA)];
        };

        project = { |point3D|
            var perspective = 400;
            var z = point3D[2] + perspective;
            var factor = perspective / z.max(1);
            [point3D[0] * factor, point3D[1] * factor];
        };

        // 4D rotations
        rotate4D_XW = { |point4D, angle|
            var x = point4D[0], w = point4D[3];
            var cosA = cos(angle), sinA = sin(angle);
            [
                (x * cosA) - (w * sinA),
                point4D[1],
                point4D[2],
                (x * sinA) + (w * cosA)
            ];
        };

        rotate4D_YW = { |point4D, angle|
            var y = point4D[1], w = point4D[3];
            var cosA = cos(angle), sinA = sin(angle);
            [
                point4D[0],
                (y * cosA) - (w * sinA),
                point4D[2],
                (y * sinA) + (w * cosA)
            ];
        };

        rotate4D_ZW = { |point4D, angle|
            var z = point4D[2], w = point4D[3];
            var cosA = cos(angle), sinA = sin(angle);
            [
                point4D[0],
                point4D[1],
                (z * cosA) - (w * sinA),
                (z * sinA) + (w * cosA)
            ];
        };

        project4Dto3D = { |point4D|
            var perspective4D = 2;
            var w = point4D[3] + perspective4D;
            var factor = perspective4D / w.max(0.1);
            [
                point4D[0] * factor,
                point4D[1] * factor,
                point4D[2] * factor
            ];
        };

        // 5D rotations
        rotate5D_XW = { |point5D, angle|
            var x = point5D[0], w = point5D[3];
            var cosA = cos(angle), sinA = sin(angle);
            [
                (x * cosA) - (w * sinA),
                point5D[1],
                point5D[2],
                (x * sinA) + (w * cosA),
                point5D[4]
            ];
        };

        rotate5D_YW = { |point5D, angle|
            var y = point5D[1], w = point5D[3];
            var cosA = cos(angle), sinA = sin(angle);
            [
                point5D[0],
                (y * cosA) - (w * sinA),
                point5D[2],
                (y * sinA) + (w * cosA),
                point5D[4]
            ];
        };

        rotate5D_ZW = { |point5D, angle|
            var z = point5D[2], w = point5D[3];
            var cosA = cos(angle), sinA = sin(angle);
            [
                point5D[0],
                point5D[1],
                (z * cosA) - (w * sinA),
                (z * sinA) + (w * cosA),
                point5D[4]
            ];
        };

        rotate5D_XV = { |point5D, angle|
            var x = point5D[0], v = point5D[4];
            var cosA = cos(angle), sinA = sin(angle);
            [
                (x * cosA) - (v * sinA),
                point5D[1],
                point5D[2],
                point5D[3],
                (x * sinA) + (v * cosA)
            ];
        };

        rotate5D_YV = { |point5D, angle|
            var y = point5D[1], v = point5D[4];
            var cosA = cos(angle), sinA = sin(angle);
            [
                point5D[0],
                (y * cosA) - (v * sinA),
                point5D[2],
                point5D[3],
                (y * sinA) + (v * cosA)
            ];
        };

        project5Dto4D = { |point5D|
            var perspective5D = 2;
            var v = point5D[4] + perspective5D;
            var factor = perspective5D / v.max(0.1);
            [
                point5D[0] * factor,
                point5D[1] * factor,
                point5D[2] * factor,
                point5D[3] * factor
            ];
        };

        // 6D rotations
        rotate6D_XW = { |point6D, angle|
            var x = point6D[0], w = point6D[3];
            var cosA = cos(angle), sinA = sin(angle);
            [
                (x * cosA) - (w * sinA),
                point6D[1],
                point6D[2],
                (x * sinA) + (w * cosA),
                point6D[4],
                point6D[5]
            ];
        };

        rotate6D_YW = { |point6D, angle|
            var y = point6D[1], w = point6D[3];
            var cosA = cos(angle), sinA = sin(angle);
            [
                point6D[0],
                (y * cosA) - (w * sinA),
                point6D[2],
                (y * sinA) + (w * cosA),
                point6D[4],
                point6D[5]
            ];
        };

        rotate6D_ZW = { |point6D, angle|
            var z = point6D[2], w = point6D[3];
            var cosA = cos(angle), sinA = sin(angle);
            [
                point6D[0],
                point6D[1],
                (z * cosA) - (w * sinA),
                (z * sinA) + (w * cosA),
                point6D[4],
                point6D[5]
            ];
        };

        rotate6D_XV = { |point6D, angle|
            var x = point6D[0], v = point6D[4];
            var cosA = cos(angle), sinA = sin(angle);
            [
                (x * cosA) - (v * sinA),
                point6D[1],
                point6D[2],
                point6D[3],
                (x * sinA) + (v * cosA),
                point6D[5]
            ];
        };

        rotate6D_YV = { |point6D, angle|
            var y = point6D[1], v = point6D[4];
            var cosA = cos(angle), sinA = sin(angle);
            [
                point6D[0],
                (y * cosA) - (v * sinA),
                point6D[2],
                point6D[3],
                (y * sinA) + (v * cosA),
                point6D[5]
            ];
        };

        rotate6D_ZV = { |point6D, angle|
            var z = point6D[2], v = point6D[4];
            var cosA = cos(angle), sinA = sin(angle);
            [
                point6D[0],
                point6D[1],
                (z * cosA) - (v * sinA),
                point6D[3],
                (z * sinA) + (v * cosA),
                point6D[5]
            ];
        };

        project6Dto5D = { |point6D|
            var perspective6D = 2;
            var u = point6D[5] + perspective6D;
            var factor = perspective6D / u.max(0.1);
            [
                point6D[0] * factor,
                point6D[1] * factor,
                point6D[2] * factor,
                point6D[3] * factor,
                point6D[4] * factor
            ];
        };

        makeGui = { arg parent;
            var gizmo;

            if(window.notNil) { window.close };

            if(parent.isNil) {
                view = window = Window(
                    bounds: (smallSize).asRect.center_(Window.availableBounds.center)
                ).name_("Attractor Scope");
            } {
                view = View(parent, Rect(0, 0, 860, 830));
                window = nil;
            };

            scopeView = UserView();
            scopeView.drawFunc = { this.draw };
            scopeView.animate = true;
            scopeView.frameRate = fps;
            scopeView.minHeight_(500);
            scopeView.canFocus = true;

            // Mouse actions
            scopeView.mouseDownAction = { |view, x, y|
                setAutoRotate.value(false);
                lastMouseX = x;
                lastMouseY = y;
                view.focus;
            };

            scopeView.mouseMoveAction = { |view, x, y|
                var deltaX, deltaY;
                var sensitivity = 0.01;

                if(lastMouseX.notNil) {
                    deltaX = x - lastMouseX;
                    deltaY = y - lastMouseY;
                    angleY = angleY + (deltaX * sensitivity);
                    angleX = angleX + (deltaY * sensitivity);
                    lastMouseX = x;
                    lastMouseY = y;
                };
            };

            scopeView.mouseUpAction = {
                lastMouseX = nil;
                lastMouseY = nil;
            };

            delay1Slider = Slider().orientation_(\horizontal).focusColor_(Color.clear);
            trailSlider = Slider().orientation_(\horizontal).focusColor_(Color.clear);
            resolutionSlider = Slider().orientation_(\horizontal).focusColor_(Color.clear);
            rotationSpeedSlider = Slider().orientation_(\horizontal).focusColor_(Color.clear);
            zoomSlider = Slider().orientation_(\horizontal).focusColor_(Color.clear);

            dimensionMenu = PopUpMenu().items_(["2D", "3D", "4D", "5D", "6D"]);
            colorMenu = PopUpMenu().items_(["Yellow", "Cyan", "Magenta", "Green", "Red", "Blue", "Orange", "Purple", "White", "Rainbow", "Velocity", "Distance", "Curvature"]);
            styleMenu = PopUpMenu().items_(["Lines", "Points", "Smooth", "Glow", "Ribbon", "Heat Map"]);
            rateMenu = PopUpMenu().items_(["Audio", "Control"]);

            autoRotateCheckBox = CheckBox().value_(autoRotate);
            exportButton = Button().states_([["Export PNG", Color.black, Color.gray(0.9)]]).maxWidth_(100);

            idxNumBox = NumberBox().decimals_(0).step_(1).scroll_step_(1);
            delay1Box = NumberBox().decimals_(4).step_(0.0001).scroll_step_(0.0001);
            trailBox = NumberBox().decimals_(0).step_(10).scroll_step_(10);
            resolutionBox = NumberBox().decimals_(0).step_(10).scroll_step_(10);
            rotationSpeedBox = NumberBox().decimals_(2).step_(0.1).scroll_step_(0.1);
            zoomBox = NumberBox().decimals_(2).step_(0.1).scroll_step_(0.1);
            fpsBox = NumberBox().decimals_(0).step_(1).scroll_step_(5).fixedWidth_(45).clipLo_(10).clipHi_(120);
            fpsBox.value = fps;

            gizmo = "999".bounds(idxNumBox.font).width + 20;
            idxNumBox.fixedWidth = gizmo;
            idxNumBox.align = \center;

            delay1Box.fixedWidth_(70);
            trailBox.fixedWidth_(70).clipLo_(10).clipHi_(5000);
            resolutionBox.fixedWidth_(70).clipLo_(100).clipHi_(2000);
            rotationSpeedBox.fixedWidth_(70).clipLo_(0.1).clipHi_(5.0);
            zoomBox.fixedWidth_(70).clipLo_(0.25).clipHi_(4.0);

            delay1Slider.value_(delaySpec.unmap(delayTime1));
            trailSlider.value_(trailSpec.unmap(trailLength));
            resolutionSlider.value_(resolutionSpec.unmap(resolution));
            rotationSpeedSlider.value_(rotationSpeedSpec.unmap(rotationSpeed));
            zoomSlider.value_(zoomSpec.unmap(zoom));

            dimensionMenu.value_(dimension - 2);
            colorMenu.value_(colorChoice);
            styleMenu.value_(drawStyle);
            rateMenu.value_(if(bus.rate === \audio) { 0 } { 1 });

            idxNumBox.clipLo_(busSpec.minval).clipHi_(busSpec.maxval).value_(bus.index);
            delay1Box.value_(delayTime1);
            trailBox.value_(trailLength);
            resolutionBox.value_(resolution);
            rotationSpeedBox.value_(rotationSpeed);
            zoomBox.value_(zoom);

            view.layout =
            VLayout(
                HLayout(
                    StaticText().string_("FPS:"),
                    fpsBox,
                    StaticText().string_("Dimension:"),
                    dimensionMenu,
                    StaticText().string_("Color:"),
                    colorMenu,
                    StaticText().string_("Style:"),
                    styleMenu,
                    [StaticText().string_("Auto-Rotate:"), align: \right],
                    autoRotateCheckBox,
                    [StaticText().string_("Input:"), align: \right],
                    rateMenu,
                    idxNumBox,
                    exportButton,
                    nil
                ).margins_(2).spacing_(4),
                scopeView,
                HLayout(
                    StaticText().string_("Delay (Dt):").fixedWidth_(70),
                    delay1Slider.maxHeight_(20),
                    delay1Box
                ).margins_(0).spacing_(4),
                HLayout(
                    StaticText().string_("Trail:").fixedWidth_(70),
                    trailSlider.maxHeight_(20),
                    trailBox
                ).margins_(0).spacing_(4),
                HLayout(
                    StaticText().string_("Resolution:").fixedWidth_(70),
                    resolutionSlider.maxHeight_(20),
                    resolutionBox
                ).margins_(0).spacing_(4),
                HLayout(
                    StaticText().string_("Rot Speed:").fixedWidth_(70),
                    rotationSpeedSlider.maxHeight_(20),
                    rotationSpeedBox
                ).margins_(0).spacing_(4),
                HLayout(
                    StaticText().string_("Zoom:").fixedWidth_(70),
                    zoomSlider.maxHeight_(20),
                    zoomBox
                ).margins_(0).spacing_(4)
            ).margins_(2).spacing_(0);

            scopeView.focus;

            delay1Slider.action = { |me| setDelayTime1.value(delaySpec.map(me.value)) };
            delay1Slider.mouseUpAction = { scopeView.focus };
            delay1Box.action = { |me| setDelayTime1.value(me.value) };
            trailSlider.action = { |me| setTrailLength.value(trailSpec.map(me.value).asInteger) };
            trailSlider.mouseUpAction = { scopeView.focus };
            trailBox.action = { |me| setTrailLength.value(me.value) };
            resolutionSlider.action = { |me| setResolution.value(resolutionSpec.map(me.value).asInteger) };
            resolutionSlider.mouseUpAction = { scopeView.focus };
            resolutionBox.action = { |me| setResolution.value(me.value) };
            rotationSpeedSlider.action = { |me| setRotationSpeed.value(rotationSpeedSpec.map(me.value)) };
            rotationSpeedSlider.mouseUpAction = { scopeView.focus };
            rotationSpeedBox.action = { |me| setRotationSpeed.value(me.value) };
            zoomSlider.action = { |me| setZoom.value(zoomSpec.map(me.value)) };
            zoomSlider.mouseUpAction = { scopeView.focus };
            zoomBox.action = { |me| setZoom.value(me.value) };
            autoRotateCheckBox.action = { |cb| setAutoRotate.value(cb.value) };
            exportButton.action = { this.exportScreenshot };
            dimensionMenu.action = { |me| setDimension.value(me.value + 2) };
            colorMenu.action = { |me| setColor.value(me.value) };
            styleMenu.action = { |me| setStyle.value(me.value) };
            idxNumBox.action = { |me| setIndex.value(me.value) };
            rateMenu.action = { |me| setRate.value(me.value) };
            fpsBox.action = { |me| setFps.value(me.value) };
            scopeView.keyDownAction = { |v, char, mod| this.keyDown(char, mod) };
            view.onClose = { view = nil; this.quit; };

            if(window.notNil) { window.front };
        };

        setDelayTime1 = { arg val;
            delayTime1 = delaySpec.constrain(val);
            delayTime2 = delayTime1 * 2;
            delayTime3 = delayTime1 * 3;
            delayTime4 = delayTime1 * 4;
            delayTime5 = delayTime1 * 5;

            if(synth.notNil and: { synth.delaySynth.notNil }) {
                synth.setDelayTime1(delayTime1);
                synth.setDelayTime2(delayTime2);
                synth.setDelayTime3(delayTime3);
                synth.setDelayTime4(delayTime4);
                synth.setDelayTime5(delayTime5);
            };

            if(delay1Box.notNil) { delay1Box.value = delayTime1 };
            if(delay1Slider.notNil) { delay1Slider.value = delaySpec.unmap(delayTime1) };
        };

        setTrailLength = { arg val;
            trailLength = trailSpec.constrain(val).asInteger;
            trailBox.value = trailLength;
            trailSlider.value = trailSpec.unmap(trailLength);
        };

        setResolution = { arg val;
            resolution = resolutionSpec.constrain(val).asInteger;
            resolutionBox.value = resolution;
            resolutionSlider.value = resolutionSpec.unmap(resolution);
        };

        setRotationSpeed = { arg val;
            rotationSpeed = rotationSpeedSpec.constrain(val);
            rotationSpeedBox.value = rotationSpeed;
            rotationSpeedSlider.value = rotationSpeedSpec.unmap(rotationSpeed);
        };

        setZoom = { arg val;
            zoom = zoomSpec.constrain(val);
            scale = baseScale * zoom;
            zoomBox.value = zoom;
            zoomSlider.value = zoomSpec.unmap(zoom);
        };

        setFps = { arg val;
            fps = fpsSpec.constrain(val).asInteger;
            if(scopeView.notNil) { scopeView.frameRate = fps };
            if(fpsBox.notNil) { fpsBox.value = fps };
        };

        setColor = { arg val;
            colorChoice = val;
            colorMenu.value = colorChoice;
            lineColor = val.switch(
                0, { Color.new255(255, 218, 0) },
                1, { Color.new255(0, 255, 255) },
                2, { Color.new255(255, 0, 255) },
                3, { Color.new255(0, 255, 0) },
                4, { Color.new255(255, 0, 0) },
                5, { Color.new255(0, 100, 255) },
                6, { Color.new255(255, 165, 0) },
                7, { Color.new255(200, 0, 255) },
                8, { Color.new255(255, 255, 255) },
                9, { Color.new255(255, 218, 0) },
                10, { Color.new255(255, 218, 0) }
            );
        };

        setStyle = { arg val;
            drawStyle = val.clip(0, 5);
            styleMenu.value = drawStyle;
        };

        setDimension = { arg val;
            dimension = val.clip(2, 6);
            dimensionMenu.value = dimension - 2;
            this.run;
        };

        setIndex = { arg i;
            bus = Bus(bus.rate, i, 1, bus.server);
            if(synth.notNil) { synth.setBusIndex(i) };
        };

        setRate = { arg val;
            val.switch(
                0, {
                    bus = Bus(\audio, bus.index, 1, bus.server);
                    busSpec = aBusSpec;
                },
                1, {
                    bus = Bus(\control, bus.index, 1, bus.server);
                    busSpec = cBusSpec;
                }
            );
            if(synth.notNil) { synth.setRate(val) };
            if(idxNumBox.notNil) {
                idxNumBox.clipLo_(busSpec.minval).clipHi_(busSpec.maxval).value_(bus.index);
            };
            this.index = bus.index;
        };

        setAutoRotate = { arg bool;
            autoRotate = bool;
            if(autoRotateCheckBox.notNil) {
                autoRotateCheckBox.value = autoRotate;
            };
        };

        updateColors = {
            // Keep current color choice
        };

        makeGui.value(parent);

        updateColors.value;

        ServerTree.add(this, server);
        ServerQuit.add(this, server);

        this.run;
    }

    quit {
        this.stop;
        synth.free;
        ServerTree.remove(this, server);
        ServerQuit.remove(this, server);
        if(window.notNil) { window.close };
    }

    free { this.quit }

    doOnCmdPeriod {
        if(readTask.notNil) { readTask.stop; readTask = nil };
        points = [];
    }

    doOnServerTree {
        synth.play(maxBufSize, bus, delayTime1, delayTime2, delayTime3,
            delayTime4, delayTime5, dimension, { |oldSynth, oldBuffer|
                if(oldSynth.notNil) {
                    server.sendBundle(nil, ['/error', -1], [11, oldSynth.nodeID], ['/error', -2]);
                };
                if(oldBuffer.notNil) { oldBuffer.free };
                this.startReading;
        });
    }

    doOnServerQuit {
        if(readTask.notNil) { readTask.stop; readTask = nil };
    }

    run {
        synth.play(maxBufSize, bus, delayTime1, delayTime2, delayTime3, delayTime4, delayTime5, dimension, {
            arg oldSynth, oldBuffer;

            if(view.notNil) {
                var sampleRate = server.sampleRate;
                var bufferFillTime;

                if(sampleRate.isNil or: { sampleRate <= 0 }) {
                    "WARNING: Sample rate not available, using default 44100".postln;
                    sampleRate = 44100;
                };

                bufferFillTime = (maxBufSize / sampleRate) * 2;
                bufferFillTime = (bufferFillTime + maxDelayTime + 0.2).max(0.4);

                "New dimension ready in %s...".format(bufferFillTime.round(0.01)).postln;

                AppClock.sched(bufferFillTime, {
                    if(oldSynth.notNil) {
                        server.sendBundle(nil, ['/error', -1], [11, oldSynth.nodeID], ['/error', -2]);
                    };
                    if(oldBuffer.notNil) {
                        oldBuffer.free;
                    };

                    this.stopReading;
                    points = [];
                    this.startReading;
                    "Switched to %D!".format(dimension).postln;
                });
            };
        });
    }

    stop {
        this.stopReading;
        synth.stop;
    }

    startReading {
        var buffer, bufferDimension;
        var isReading = false;

        this.stopReading;

        buffer = synth.buffer;
        bufferDimension = dimension;

        if(buffer.isNil) {
            "ERROR: Buffer is nil!".postln;
            ^this;
        };

        readTask = Routine({
            inf.do {
                if(buffer.notNil and: { buffer.numFrames.notNil } and: { isReading.not }) {
                    isReading = true;

                    buffer.loadToFloatArray(action: { |data|
                        var numFrames = buffer.numFrames;
                        var readStep, sampleStep;
                        var maxPoints, actualPoints, writeIdx;
                        var sampledPoints;

                        if(numFrames.notNil) {
                            readStep = 4;

                            maxPoints = (numFrames / readStep).asInteger;
                            sampleStep = max(1, (maxPoints / resolution).ceil.asInteger);
                            actualPoints = (maxPoints / sampleStep).asInteger;

                            sampledPoints = Array.newClear(actualPoints);
                            writeIdx = 0;

                            actualPoints.do { |i|
                                var frameIdx = i * sampleStep * readStep;
                                var dataIdx = frameIdx * bufferDimension;
                                var point;

                                if(dataIdx + (bufferDimension - 1) < data.size) {
                                    point = Array.newClear(bufferDimension);
                                    bufferDimension.do { |d|
                                        point[d] = data[dataIdx + d];
                                    };
                                    sampledPoints[writeIdx] = point;
                                    writeIdx = writeIdx + 1;
                                };
                            };

                            if(writeIdx > 0 and: { writeIdx < actualPoints }) {
                                sampledPoints = sampledPoints.copyRange(0, (writeIdx - 1).asInteger);
                            };

                            if(writeIdx > 0) {
                                if(sampledPoints.size > trailLength) {
                                    var startIdx = (sampledPoints.size - trailLength).asInteger.max(0);
                                    points = sampledPoints.copyRange(startIdx, (sampledPoints.size - 1).asInteger);
                                } {
                                    points = sampledPoints;
                                };
                            };
                        };

                        isReading = false;
                    });
                };

                (1/fps).wait;
            };
        });

        AppClock.sched(0, readTask);
    }

    stopReading {
        if(readTask.notNil) {
            readTask.stop;
            readTask = nil;
        };
    }

    draw {
        if(points.size > 0 and: { points[0].notNil }) {
            points[0].size.switch(
                2, { this.draw2D },
                3, { this.draw3D },
                4, { this.draw4D },
                5, { this.draw5D },
                6, { this.draw6D }
            );
        } {
            Pen.fillColor = Color.black;
            Pen.fillRect(scopeView.bounds);
        };
    }

    drawWithStyle { arg screenPoints, bounds, numPoints, dimensionLabel;
        var styleName, colorName, interpolatedPoints;

        if(drawStyle != 5) {
            interpolatedPoints = interpolatePoints.value(screenPoints);
        };

        drawStyle.switch(
            0, { drawLines.value(interpolatedPoints, bounds, numPoints) },
            1, { drawPoints.value(interpolatedPoints, bounds, numPoints) },
            2, { drawSmooth.value(interpolatedPoints, bounds, numPoints) },
            3, { drawGlow.value(interpolatedPoints, bounds, numPoints) },
            4, { drawRibbon.value(interpolatedPoints, bounds, numPoints) },
            5, { drawHeatMap.value(screenPoints, bounds, numPoints) }
        );

        styleName = ["Lines", "Points", "Smooth", "Glow", "Ribbon", "Heat Map"][drawStyle];
        colorName = ["Yellow", "Cyan", "Magenta", "Green", "Red", "Blue", "Orange", "Purple", "White", "Rainbow", "Velocity", "Distance", "Curvature"][colorChoice];

        Pen.stringAtPoint(
            dimensionLabel ++ " | " ++ styleName ++ " | " ++ colorName ++ " | Bus " ++ bus.index ++ " (" ++ bus.rate ++ ") | Trail: " ++ trailLength ++
            " | Res: " ++ resolution ++ " | Points: " ++ numPoints ++
            " | Dt=" ++ delayTime1.round(0.0001) ++ "s" ++
            if(dimension > 2, " | Speed: " ++ rotationSpeed.round(0.1) ++ "× | " ++ if(autoRotate, "Auto", "Paused"), "") ++
            " | Zoom: " ++ zoom.round(0.01) ++ "× | FPS: " ++ fps,
            Point(10, 10),
            Font.default,
            Color.white
        );
    }

    draw2D {
        var bounds, centerX, centerY, numPoints;
        var screenX, screenY;
        var screenPoints;

        bounds = scopeView.bounds;
        centerX = bounds.width / 2;
        centerY = bounds.height / 2;
        numPoints = points.size;

        Pen.fillColor = Color.black;
        Pen.fillRect(bounds);

        if(numPoints > 1) {
            screenPoints = Array.newClear(numPoints);

            numPoints.do { |i|
                var pt = points[i];
                if(pt.notNil and: { pt.size == 2 }) {
                    screenX = (pt[0] * scale) + centerX;
                    screenY = (pt[1] * scale) + centerY;
                    screenPoints[i] = Point(screenX, screenY);
                } {
                    screenPoints[i] = Point(centerX, centerY);
                };
            };

            this.drawWithStyle(screenPoints, bounds, numPoints, "2D");
        };
    }

    draw3D {
        var bounds, centerX, centerY, numPoints;
        var screenX, screenY;
        var screenPoints;
        var cosX, sinX, cosY, sinY;

        bounds = scopeView.bounds;
        centerX = bounds.width / 2;
        centerY = bounds.height / 2;
        numPoints = points.size;

        Pen.fillColor = Color.black;
        Pen.fillRect(bounds);

        if(autoRotate) {
            angleY = angleY + (0.01 * rotationSpeed);
            angleX = angleX + (0.005 * rotationSpeed);
        };

        cosX = cos(angleX); sinX = sin(angleX);
        cosY = cos(angleY); sinY = sin(angleY);

        if(numPoints > 1) {
            screenPoints = Array.newClear(numPoints);

            numPoints.do { |i|
                var pt = points[i];
                if(pt.notNil and: { pt.size == 3 }) {
                    var rx, ry, rz, y, z, x;
                    var projX, projY, zProj, factor;

                    y = pt[1]; z = pt[2];
                    ry = (y * cosX) - (z * sinX);
                    rz = (y * sinX) + (z * cosX);

                    x = pt[0];
                    rx = (x * cosY) + (rz * sinY);
                    rz = (rz * cosY) - (x * sinY);

                    rx = rx * scale;
                    ry = ry * scale;
                    rz = rz * scale;

                    zProj = rz + 400;
                    factor = 400 / zProj.max(1);
                    projX = rx * factor;
                    projY = ry * factor;

                    screenX = projX + centerX;
                    screenY = projY + centerY;
                    screenPoints[i] = Point(screenX, screenY);
                } {
                    screenPoints[i] = Point(centerX, centerY);
                };
            };

            this.drawWithStyle(screenPoints, bounds, numPoints, "3D");
        };
    }

    draw4D {
        var bounds, centerX, centerY, numPoints;
        var rotated4D, projected3D, screenX, screenY;
        var screenPoints;
        var cosXW, sinXW, cosYW, sinYW, cosZW, sinZW;
        var cosX, sinX, cosY, sinY;

        bounds = scopeView.bounds;
        centerX = bounds.width / 2;
        centerY = bounds.height / 2;
        numPoints = points.size;

        Pen.fillColor = Color.black;
        Pen.fillRect(bounds);

        if(autoRotate) {
            angle4D_XW = angle4D_XW + (0.005 * rotationSpeed);
            angle4D_YW = angle4D_YW + (0.007 * rotationSpeed);
            angle4D_ZW = angle4D_ZW + (0.009 * rotationSpeed);
            angleY = angleY + (0.01 * rotationSpeed);
            angleX = angleX + (0.005 * rotationSpeed);
        };

        cosXW = cos(angle4D_XW); sinXW = sin(angle4D_XW);
        cosYW = cos(angle4D_YW); sinYW = sin(angle4D_YW);
        cosZW = cos(angle4D_ZW); sinZW = sin(angle4D_ZW);
        cosX = cos(angleX); sinX = sin(angleX);
        cosY = cos(angleY); sinY = sin(angleY);

        if(numPoints > 1) {
            screenPoints = Array.newClear(numPoints);

            numPoints.do { |i|
                var pt = points[i];
                if(pt.notNil and: { pt.size == 4 }) {
                    var rx4, ry4, rz4, rw4, x4, y4, z4, w4;
                    var p3x, p3y, p3z, projFactor, w;
                    var rx3, ry3, rz3, y3, z3, x3;
                    var projX, projY, zProj, factor3d;

                    x4 = pt[0]; y4 = pt[1]; z4 = pt[2]; w4 = pt[3];

                    rx4 = (x4 * cosXW) - (w4 * sinXW);
                    rw4 = (x4 * sinXW) + (w4 * cosXW);
                    ry4 = y4; rz4 = z4;

                    x4 = rx4; y4 = ry4; w4 = rw4;
                    ry4 = (y4 * cosYW) - (w4 * sinYW);
                    rw4 = (y4 * sinYW) + (w4 * cosYW);
                    rx4 = x4; rz4 = rz4;

                    z4 = rz4; w4 = rw4;
                    rz4 = (z4 * cosZW) - (w4 * sinZW);
                    rw4 = (z4 * sinZW) + (w4 * cosZW);

                    w = rw4 + 2;
                    projFactor = 2 / w.max(0.1);
                    p3x = rx4 * projFactor;
                    p3y = ry4 * projFactor;
                    p3z = rz4 * projFactor;

                    y3 = p3y; z3 = p3z;
                    ry3 = (y3 * cosX) - (z3 * sinX);
                    rz3 = (y3 * sinX) + (z3 * cosX);

                    x3 = p3x;
                    rx3 = (x3 * cosY) + (rz3 * sinY);
                    rz3 = (rz3 * cosY) - (x3 * sinY);

                    rx3 = rx3 * scale;
                    ry3 = ry3 * scale;
                    rz3 = rz3 * scale;

                    zProj = rz3 + 400;
                    factor3d = 400 / zProj.max(1);
                    projX = rx3 * factor3d;
                    projY = ry3 * factor3d;

                    screenX = projX + centerX;
                    screenY = projY + centerY;
                    screenPoints[i] = Point(screenX, screenY);
                } {
                    screenPoints[i] = Point(centerX, centerY);
                };
            };

            this.drawWithStyle(screenPoints, bounds, numPoints, "4D");
        };
    }

    draw5D {
        var bounds, centerX, centerY, numPoints;
        var screenX, screenY;
        var screenPoints;
        var cosXW, sinXW, cosYW, sinYW, cosZW, sinZW, cosXV, sinXV, cosYV, sinYV;
        var cosXW4, sinXW4, cosYW4, sinYW4, cosZW4, sinZW4;
        var cosX, sinX, cosY, sinY;

        bounds = scopeView.bounds;
        centerX = bounds.width / 2;
        centerY = bounds.height / 2;
        numPoints = points.size;

        Pen.fillColor = Color.black;
        Pen.fillRect(bounds);

        if(autoRotate) {
            angle5D_XW = angle5D_XW + (0.004 * rotationSpeed);
            angle5D_YW = angle5D_YW + (0.006 * rotationSpeed);
            angle5D_ZW = angle5D_ZW + (0.008 * rotationSpeed);
            angle5D_XV = angle5D_XV + (0.005 * rotationSpeed);
            angle5D_YV = angle5D_YV + (0.007 * rotationSpeed);
            angle4D_XW = angle4D_XW + (0.005 * rotationSpeed);
            angle4D_YW = angle4D_YW + (0.007 * rotationSpeed);
            angle4D_ZW = angle4D_ZW + (0.009 * rotationSpeed);
            angleY = angleY + (0.01 * rotationSpeed);
            angleX = angleX + (0.005 * rotationSpeed);
        };

        cosXW = cos(angle5D_XW); sinXW = sin(angle5D_XW);
        cosYW = cos(angle5D_YW); sinYW = sin(angle5D_YW);
        cosZW = cos(angle5D_ZW); sinZW = sin(angle5D_ZW);
        cosXV = cos(angle5D_XV); sinXV = sin(angle5D_XV);
        cosYV = cos(angle5D_YV); sinYV = sin(angle5D_YV);
        cosXW4 = cos(angle4D_XW); sinXW4 = sin(angle4D_XW);
        cosYW4 = cos(angle4D_YW); sinYW4 = sin(angle4D_YW);
        cosZW4 = cos(angle4D_ZW); sinZW4 = sin(angle4D_ZW);
        cosX = cos(angleX); sinX = sin(angleX);
        cosY = cos(angleY); sinY = sin(angleY);

        if(numPoints > 1) {
            screenPoints = Array.newClear(numPoints);

            numPoints.do { |i|
                var pt = points[i];
                if(pt.notNil and: { pt.size == 5 }) {
                    var rx5, ry5, rz5, rw5, rv5, x5, y5, z5, w5, v5;
                    var p4x, p4y, p4z, p4w, projFactor5, v;
                    var rx4, ry4, rz4, rw4, x4, y4, z4, w4, projFactor4, w;
                    var p3x, p3y, p3z;
                    var rx3, ry3, rz3, y3, z3, x3;
                    var projX, projY, zProj, factor3d;

                    x5 = pt[0]; y5 = pt[1]; z5 = pt[2]; w5 = pt[3]; v5 = pt[4];

                    rx5 = (x5 * cosXW) - (w5 * sinXW);
                    rw5 = (x5 * sinXW) + (w5 * cosXW);
                    ry5 = y5; rz5 = z5; rv5 = v5;

                    y5 = ry5; w5 = rw5;
                    ry5 = (y5 * cosYW) - (w5 * sinYW);
                    rw5 = (y5 * sinYW) + (w5 * cosYW);

                    z5 = rz5; w5 = rw5;
                    rz5 = (z5 * cosZW) - (w5 * sinZW);
                    rw5 = (z5 * sinZW) + (w5 * cosZW);

                    x5 = rx5; v5 = rv5;
                    rx5 = (x5 * cosXV) - (v5 * sinXV);
                    rv5 = (x5 * sinXV) + (v5 * cosXV);

                    y5 = ry5; v5 = rv5;
                    ry5 = (y5 * cosYV) - (v5 * sinYV);
                    rv5 = (y5 * sinYV) + (v5 * cosYV);

                    v = rv5 + 2;
                    projFactor5 = 2 / v.max(0.1);
                    p4x = rx5 * projFactor5;
                    p4y = ry5 * projFactor5;
                    p4z = rz5 * projFactor5;
                    p4w = rw5 * projFactor5;

                    x4 = p4x; y4 = p4y; z4 = p4z; w4 = p4w;

                    rx4 = (x4 * cosXW4) - (w4 * sinXW4);
                    rw4 = (x4 * sinXW4) + (w4 * cosXW4);
                    ry4 = y4; rz4 = z4;

                    y4 = ry4; w4 = rw4;
                    ry4 = (y4 * cosYW4) - (w4 * sinYW4);
                    rw4 = (y4 * sinYW4) + (w4 * cosYW4);

                    z4 = rz4; w4 = rw4;
                    rz4 = (z4 * cosZW4) - (w4 * sinZW4);
                    rw4 = (z4 * sinZW4) + (w4 * cosZW4);

                    w = rw4 + 2;
                    projFactor4 = 2 / w.max(0.1);
                    p3x = rx4 * projFactor4;
                    p3y = ry4 * projFactor4;
                    p3z = rz4 * projFactor4;

                    y3 = p3y; z3 = p3z;
                    ry3 = (y3 * cosX) - (z3 * sinX);
                    rz3 = (y3 * sinX) + (z3 * cosX);

                    x3 = p3x;
                    rx3 = (x3 * cosY) + (rz3 * sinY);
                    rz3 = (rz3 * cosY) - (x3 * sinY);

                    rx3 = rx3 * scale;
                    ry3 = ry3 * scale;
                    rz3 = rz3 * scale;

                    zProj = rz3 + 400;
                    factor3d = 400 / zProj.max(1);
                    projX = rx3 * factor3d;
                    projY = ry3 * factor3d;

                    screenX = projX + centerX;
                    screenY = projY + centerY;
                    screenPoints[i] = Point(screenX, screenY);
                } {
                    screenPoints[i] = Point(centerX, centerY);
                };
            };

            this.drawWithStyle(screenPoints, bounds, numPoints, "5D");
        };
    }

    draw6D {
        var bounds, centerX, centerY, numPoints;
        var screenX, screenY;
        var screenPoints;
        var cosXW6, sinXW6, cosYW6, sinYW6, cosZW6, sinZW6, cosXV6, sinXV6, cosYV6, sinYV6, cosZV6, sinZV6;
        var cosXW5, sinXW5, cosYW5, sinYW5, cosZW5, sinZW5, cosXV5, sinXV5, cosYV5, sinYV5;
        var cosXW4, sinXW4, cosYW4, sinYW4, cosZW4, sinZW4;
        var cosX, sinX, cosY, sinY;

        bounds = scopeView.bounds;
        centerX = bounds.width / 2;
        centerY = bounds.height / 2;
        numPoints = points.size;

        Pen.fillColor = Color.black;
        Pen.fillRect(bounds);

        if(autoRotate) {
            angle6D_XW = angle6D_XW + (0.003 * rotationSpeed);
            angle6D_YW = angle6D_YW + (0.005 * rotationSpeed);
            angle6D_ZW = angle6D_ZW + (0.007 * rotationSpeed);
            angle6D_XV = angle6D_XV + (0.004 * rotationSpeed);
            angle6D_YV = angle6D_YV + (0.006 * rotationSpeed);
            angle6D_ZV = angle6D_ZV + (0.008 * rotationSpeed);
            angle5D_XW = angle5D_XW + (0.004 * rotationSpeed);
            angle5D_YW = angle5D_YW + (0.006 * rotationSpeed);
            angle5D_ZW = angle5D_ZW + (0.008 * rotationSpeed);
            angle5D_XV = angle5D_XV + (0.005 * rotationSpeed);
            angle5D_YV = angle5D_YV + (0.007 * rotationSpeed);
            angle4D_XW = angle4D_XW + (0.005 * rotationSpeed);
            angle4D_YW = angle4D_YW + (0.007 * rotationSpeed);
            angle4D_ZW = angle4D_ZW + (0.009 * rotationSpeed);
            angleY = angleY + (0.01 * rotationSpeed);
            angleX = angleX + (0.005 * rotationSpeed);
        };

        cosXW6 = cos(angle6D_XW); sinXW6 = sin(angle6D_XW);
        cosYW6 = cos(angle6D_YW); sinYW6 = sin(angle6D_YW);
        cosZW6 = cos(angle6D_ZW); sinZW6 = sin(angle6D_ZW);
        cosXV6 = cos(angle6D_XV); sinXV6 = sin(angle6D_XV);
        cosYV6 = cos(angle6D_YV); sinYV6 = sin(angle6D_YV);
        cosZV6 = cos(angle6D_ZV); sinZV6 = sin(angle6D_ZV);
        cosXW5 = cos(angle5D_XW); sinXW5 = sin(angle5D_XW);
        cosYW5 = cos(angle5D_YW); sinYW5 = sin(angle5D_YW);
        cosZW5 = cos(angle5D_ZW); sinZW5 = sin(angle5D_ZW);
        cosXV5 = cos(angle5D_XV); sinXV5 = sin(angle5D_XV);
        cosYV5 = cos(angle5D_YV); sinYV5 = sin(angle5D_YV);
        cosXW4 = cos(angle4D_XW); sinXW4 = sin(angle4D_XW);
        cosYW4 = cos(angle4D_YW); sinYW4 = sin(angle4D_YW);
        cosZW4 = cos(angle4D_ZW); sinZW4 = sin(angle4D_ZW);
        cosX = cos(angleX); sinX = sin(angleX);
        cosY = cos(angleY); sinY = sin(angleY);

        if(numPoints > 1) {
            screenPoints = Array.newClear(numPoints);

            numPoints.do { |i|
                var pt = points[i];
                if(pt.notNil and: { pt.size == 6 }) {
                    var rx6, ry6, rz6, rw6, rv6, ru6, x6, y6, z6, w6, v6, u6;
                    var p5x, p5y, p5z, p5w, p5v, projFactor6, u;
                    var rx5, ry5, rz5, rw5, rv5, x5, y5, z5, w5, v5, projFactor5, v;
                    var p4x, p4y, p4z, p4w;
                    var rx4, ry4, rz4, rw4, x4, y4, z4, w4, projFactor4, w;
                    var p3x, p3y, p3z;
                    var rx3, ry3, rz3, y3, z3, x3;
                    var projX, projY, zProj, factor3d;

                    x6 = pt[0]; y6 = pt[1]; z6 = pt[2]; w6 = pt[3]; v6 = pt[4]; u6 = pt[5];

                    rx6 = (x6 * cosXW6) - (w6 * sinXW6);
                    rw6 = (x6 * sinXW6) + (w6 * cosXW6);
                    ry6 = y6; rz6 = z6; rv6 = v6; ru6 = u6;

                    y6 = ry6; w6 = rw6;
                    ry6 = (y6 * cosYW6) - (w6 * sinYW6);
                    rw6 = (y6 * sinYW6) + (w6 * cosYW6);

                    z6 = rz6; w6 = rw6;
                    rz6 = (z6 * cosZW6) - (w6 * sinZW6);
                    rw6 = (z6 * sinZW6) + (w6 * cosZW6);

                    x6 = rx6; v6 = rv6;
                    rx6 = (x6 * cosXV6) - (v6 * sinXV6);
                    rv6 = (x6 * sinXV6) + (v6 * cosXV6);

                    y6 = ry6; v6 = rv6;
                    ry6 = (y6 * cosYV6) - (v6 * sinYV6);
                    rv6 = (y6 * sinYV6) + (v6 * cosYV6);

                    z6 = rz6; v6 = rv6;
                    rz6 = (z6 * cosZV6) - (v6 * sinZV6);
                    rv6 = (z6 * sinZV6) + (v6 * cosZV6);

                    u = ru6 + 2;
                    projFactor6 = 2 / u.max(0.1);
                    p5x = rx6 * projFactor6;
                    p5y = ry6 * projFactor6;
                    p5z = rz6 * projFactor6;
                    p5w = rw6 * projFactor6;
                    p5v = rv6 * projFactor6;

                    x5 = p5x; y5 = p5y; z5 = p5z; w5 = p5w; v5 = p5v;

                    rx5 = (x5 * cosXW5) - (w5 * sinXW5);
                    rw5 = (x5 * sinXW5) + (w5 * cosXW5);
                    ry5 = y5; rz5 = z5; rv5 = v5;

                    y5 = ry5; w5 = rw5;
                    ry5 = (y5 * cosYW5) - (w5 * sinYW5);
                    rw5 = (y5 * sinYW5) + (w5 * cosYW5);

                    z5 = rz5; w5 = rw5;
                    rz5 = (z5 * cosZW5) - (w5 * sinZW5);
                    rw5 = (z5 * sinZW5) + (w5 * cosZW5);

                    x5 = rx5; v5 = rv5;
                    rx5 = (x5 * cosXV5) - (v5 * sinXV5);
                    rv5 = (x5 * sinXV5) + (v5 * cosXV5);

                    y5 = ry5; v5 = rv5;
                    ry5 = (y5 * cosYV5) - (v5 * sinYV5);
                    rv5 = (y5 * sinYV5) + (v5 * cosYV5);

                    v = rv5 + 2;
                    projFactor5 = 2 / v.max(0.1);
                    p4x = rx5 * projFactor5;
                    p4y = ry5 * projFactor5;
                    p4z = rz5 * projFactor5;
                    p4w = rw5 * projFactor5;

                    x4 = p4x; y4 = p4y; z4 = p4z; w4 = p4w;

                    rx4 = (x4 * cosXW4) - (w4 * sinXW4);
                    rw4 = (x4 * sinXW4) + (w4 * cosXW4);
                    ry4 = y4; rz4 = z4;

                    y4 = ry4; w4 = rw4;
                    ry4 = (y4 * cosYW4) - (w4 * sinYW4);
                    rw4 = (y4 * sinYW4) + (w4 * cosYW4);

                    z4 = rz4; w4 = rw4;
                    rz4 = (z4 * cosZW4) - (w4 * sinZW4);
                    rw4 = (z4 * sinZW4) + (w4 * cosZW4);

                    w = rw4 + 2;
                    projFactor4 = 2 / w.max(0.1);
                    p3x = rx4 * projFactor4;
                    p3y = ry4 * projFactor4;
                    p3z = rz4 * projFactor4;

                    y3 = p3y; z3 = p3z;
                    ry3 = (y3 * cosX) - (z3 * sinX);
                    rz3 = (y3 * sinX) + (z3 * cosX);

                    x3 = p3x;
                    rx3 = (x3 * cosY) + (rz3 * sinY);
                    rz3 = (rz3 * cosY) - (x3 * sinY);

                    rx3 = rx3 * scale;
                    ry3 = ry3 * scale;
                    rz3 = rz3 * scale;

                    zProj = rz3 + 400;
                    factor3d = 400 / zProj.max(1);
                    projX = rx3 * factor3d;
                    projY = ry3 * factor3d;

                    screenX = projX + centerX;
                    screenY = projY + centerY;
                    screenPoints[i] = Point(screenX, screenY);
                } {
                    screenPoints[i] = Point(centerX, centerY);
                };
            };

            this.drawWithStyle(screenPoints, bounds, numPoints, "6D");
        };
    }

    index { ^bus.index }
    index_ { arg i; idxNumBox.value = i }
    bus_ { arg b; bus = b; }
    style { ^drawStyle }
    style_ { arg val; setStyle.value(val) }
    fps_ { arg val; setFps.value(val) }

    resetView {
        angleX = 0.6;
        angleY = 0.8;
        angle4D_XW = 0.0;
        angle4D_YW = 0.0;
        angle4D_ZW = 0.0;
        angle5D_XW = 0.0;
        angle5D_YW = 0.0;
        angle5D_ZW = 0.0;
        angle5D_XV = 0.0;
        angle5D_YV = 0.0;
        angle6D_XW = 0.0;
        angle6D_YW = 0.0;
        angle6D_ZW = 0.0;
        angle6D_XV = 0.0;
        angle6D_YV = 0.0;
        angle6D_ZV = 0.0;
        setZoom.value(1.0);
    }

    resetAll {
        setDelayTime1.value(defaultDelayTime);
        setTrailLength.value(500);
        setResolution.value(800);
        setColor.value(0);
        setStyle.value(0);
        setRotationSpeed.value(1.0);
        setAutoRotate.value(true);
        setFps.value(30);
        this.resetView;
        if(dimension != 3) {
            setDimension.value(3);
        };
    }

    clearPoints {
        points = [];
    }

    toggleSize {
        sizeToggle = sizeToggle.not;
        if(window.notNil) {
            if(sizeToggle) {
                window.bounds = largeSize.asRect.center_(window.bounds.center);
            } {
                window.bounds = smallSize.asRect.center_(window.bounds.center);
            };
        };
    }

    exportScreenshot {
        var img, filename, timestamp, filepath, picturesDir;

        if(scopeView.isNil) {
            "Cannot export: scopeView is not available".postln;
            ^this;
        };

        try {
            timestamp = Date.getDate.format("%Y%m%d_%H%M%S");
            filename = "AttractorScope_" ++ dimension ++ "D_" ++ timestamp ++ ".png";
            picturesDir = Platform.userHomeDir +/+ "Pictures";
            filepath = picturesDir +/+ filename;

            img = Image.fromWindow(scopeView);
            img.write(filepath, "png");
            img.free;

            ("Screenshot saved: " ++ filename).postln;
            ("Saved to: " ++ filepath).postln;
        } {
            "Error exporting screenshot".postln;
        };
    }

    keyDown { arg char, mod;
        case(
            { char === $] }, { delay1Slider.increment; delay1Slider.doAction },
            { char === $[ }, { delay1Slider.decrement; delay1Slider.doAction },
            { char === ${ }, { trailSlider.decrement; trailSlider.doAction },
            { char === $} }, { trailSlider.increment; trailSlider.doAction },
            { char === $< }, { resolutionSlider.decrement; resolutionSlider.doAction },
            { char === $> }, { resolutionSlider.increment; resolutionSlider.doAction },
            { char === $, }, { rotationSpeedSlider.decrement; rotationSpeedSlider.doAction },
            { char === $. }, { rotationSpeedSlider.increment; rotationSpeedSlider.doAction },
            { char === $= }, { zoomSlider.increment; zoomSlider.doAction },
            { char === $- }, { zoomSlider.decrement; zoomSlider.doAction },
            { char === $s }, { this.style = (drawStyle + 1) % 6 },
            { char === $2 }, { setDimension.value(2) },
            { char === $3 }, { setDimension.value(3) },
            { char === $4 }, { setDimension.value(4) },
            { char === $5 }, { setDimension.value(5) },
            { char === $6 }, { setDimension.value(6) },
            { char === $m }, { this.toggleSize },
            { char === $r }, { this.resetAll },
            { char === $a }, { setAutoRotate.value(autoRotate.not) },
            { char === $c }, { setColor.value((colorChoice + 1) % 13) },
            { char === $e }, { this.exportScreenshot },
            { char === $f }, { setFps.value((fps + 10).clip(10, 120)) },
            { ^false }
        );
        ^true;
    }
}
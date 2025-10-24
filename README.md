# AttractorScope

A real-time audio visualization tool for SuperCollider that creates stunning attractor visualizations from audio or control bus signals in 2D through 6D space.

![AttractorScope Demo](assets/demo.gif)

## Overview

AttractorScope is a powerful visualization class for SuperCollider that transforms audio signals into dynamic, multi-dimensional attractor visualizations. By using delayed versions of input signals as coordinates in 2D-6D space, it creates beautiful phase space portraits that reveal the underlying structure and behavior of audio signals.

## Features

### Multi-Dimensional Visualization
- **2D-6D Support**: Visualize audio signals in 2, 3, 4, 5, or 6 dimensions
- **Dynamic Projection**: Higher dimensions are projected down to 2D screen space with interactive rotation controls
- **Real-time Rotation**: Rotate through any dimensional plane with smooth animation

### Rendering Styles
- **Lines**: Classic attractor trail visualization
- **Points**: Discrete point-based rendering
- **Glow**: Points with radial glow effect
- **Ribbon**: 3D-style ribbon with shading
- **HeatMap**: Density-based heat map visualization
- **Smooth**: Anti-aliased smooth lines with enhanced visual quality

### Color Schemes
- **Static Colors**: 9 preset color schemes (Yellow, White, Cyan, Magenta, Green, Red, Blue, Orange, Purple)
- **Rainbow**: Animated rainbow gradient
- **Velocity**: Color based on movement speed
- **Distance**: Color based on distance from origin
- **Curvature**: Color based on path curvature

### Interactive Controls
- Full GUI with sliders and menus
- Comprehensive keyboard shortcuts
- Mouse interaction for manual rotation
- Resizable window (toggle between small/large)
- Auto-rotation mode

## Installation

1. Clone this repository into your SuperCollider extensions folder:
```bash
cd ~/Library/Application Support/SuperCollider/Extensions/
git clone https://github.com/yourusername/AttractorScope.git
```

2. Recompile the SuperCollider class library (Ctrl/Cmd + Shift + L)

## Quick Start

### Basic Usage

```supercollider
// Boot the server
s.boot;

// Create a simple test signal (sine wave)
{SinOsc.ar(440) * 0.5}.play;

// Create an AttractorScope on audio bus 0
a = AttractorScope(s, 0);
```

### 3D Lorenz-like Attractor

```supercollider
(
// Create a chaotic audio signal
{
    var x, y, z, dt = 0.001, sigma = 10, rho = 28, beta = 8/3;
    x = LocalIn.ar(3);
    x = x + ([
        sigma * (x[1] - x[0]),
        x[0] * (rho - x[2]) - x[1],
        x[0] * x[1] - beta * x[2]
    ] * dt);
    LocalOut.ar(x);
    x * 0.1;
}.play;

// Visualize it
a = AttractorScope(s, 0, dimension: 3, trailLength: 1000, resolution: 1000);
)
```

### Custom Parameters

```supercollider
a = AttractorScope(
    server: s,
    index: 0,              // Audio bus index
    bufsize: 4096,         // Buffer size for reading
    delayTime1: 0.01,      // Base delay time in seconds
    trailLength: 500,      // Number of points to display
    resolution: 800,       // Points per second
    dimension: 3,          // Dimensionality (2-6)
    rotationSpeed: 1.0,    // Auto-rotation speed
    zoom: 1.0,             // Zoom level
    rate: \audio           // Bus rate (\audio or \control)
);
```

## Keyboard Shortcuts

### Dimension Control
- `2` - Switch to 2D mode
- `3` - Switch to 3D mode
- `4` - Switch to 4D mode
- `5` - Switch to 5D mode
- `6` - Switch to 6D mode

### Visual Controls
- `c` - Cycle through color schemes
- `s` - Cycle through drawing styles
- `m` - Toggle window size (small/large)
- `r` - Reset view (angles and zoom)
- `a` - Toggle auto-rotation
- Space - Clear all points

### Navigation
- `+`/`=` - Increase rotation speed
- `-` - Decrease rotation speed
- `z` - Zoom in
- `_` - Zoom out
- Mouse drag - Manual rotation (disables auto-rotation)

### Parameter Adjustment
- Arrow keys can be used to navigate GUI controls
- Sliders can be adjusted by clicking and dragging

## Parameters

### Core Parameters

| Parameter | Type | Range | Default | Description |
|-----------|------|-------|---------|-------------|
| `server` | Server | - | default | SuperCollider server instance |
| `index` | Integer/Bus | 0-N | 0 | Audio or control bus to visualize |
| `bufsize` | Integer | 128+ | 4096 | Internal buffer size |
| `delayTime1` | Float | 0.0001-0.05 | 0.01 | Base delay time (creates attractor structure) |
| `trailLength` | Integer | 10-5000 | 500 | Number of visible trail points |
| `resolution` | Integer | 100-2000 | 800 | Points computed per second |
| `dimension` | Integer | 2-6 | 3 | Visualization dimensionality |
| `rotationSpeed` | Float | 0.1-5.0 | 1.0 | Auto-rotation speed multiplier |
| `zoom` | Float | 0.25-4.0 | 1.0 | Initial zoom level |
| `rate` | Symbol | - | \audio | Bus rate: `\audio` or `\control` |

### Delay Times

The scope uses multiple delayed versions of the input signal to create coordinates in N-dimensional space:
- `delayTime1`: First delay (base)
- `delayTime2`: Second delay (2 × base)
- `delayTime3`: Third delay (3 × base)
- `delayTime4`: Fourth delay (4 × base)
- `delayTime5`: Fifth delay (5 × base)

Different delay times create different attractor shapes. Experiment with values between 0.001 and 0.1 seconds.

## Methods

### Instance Methods

```supercollider
.run            // Start/resume scope
.stop           // Pause scope
.free           // Free all resources
.toggleSize     // Toggle between small and large window
.clearPoints    // Clear all displayed points
.resetView      // Reset rotation angles and zoom

// Setters
.index_(n)           // Change bus index
.delayTime1_(t)      // Set base delay time
.trailLength_(n)     // Set number of trail points
.resolution_(n)      // Set resolution (points/sec)
.dimension_(n)       // Set dimensionality (2-6)
.rotationSpeed_(s)   // Set rotation speed
.zoom_(z)            // Set zoom level
.color_(n)           // Set color scheme (0-12)
.style_(n)           // Set drawing style (0-5)
.autoRotate_(bool)   // Enable/disable auto-rotation
```

### Class Methods

```supercollider
AttractorScope.defaultDelayTime_(t)  // Set default delay time for all instances
AttractorScope.defaultDelayTime      // Get default delay time
```

## Advanced Examples

### Modulated Attractor

```supercollider
(
// Create a signal with slow modulation
{
    var freq = SinOsc.kr(0.1).range(200, 400);
    var mod = SinOsc.kr(0.05).range(0.5, 2);
    SinOsc.ar(freq) * mod * 0.3;
}.play;

// Visualize with custom settings
a = AttractorScope(s, 0,
    delayTime1: 0.015,
    trailLength: 800,
    resolution: 1200,
    dimension: 3,
    rotationSpeed: 0.5
);
a.color = 9;  // Rainbow color
a.style = 5;  // Smooth rendering
)
```

### Multi-Channel Setup

```supercollider
(
// Create independent signals on multiple buses
{
    var sig1 = SinOsc.ar(440 * [1, 1.01]).sum * 0.3;
    var sig2 = Pulse.ar(110) * 0.3;
    var sig3 = WhiteNoise.ar(0.1) * SinOsc.kr(2).range(0, 1);
    
    Out.ar(0, sig1);
    Out.ar(1, sig2);
    Out.ar(2, sig3);
}.play;

// Create scopes for each
~scopes = [
    AttractorScope(s, 0, dimension: 2),
    AttractorScope(s, 1, dimension: 3),
    AttractorScope(s, 2, dimension: 4)
];
)
```

### Control Rate Scope

```supercollider
(
// Create a control rate signal
~ctlBus = Bus.control(s, 1);
{Out.kr(~ctlBus, SinOsc.kr(1))}.play;

// Scope it (note rate: \control)
a = AttractorScope(s, ~ctlBus.index, rate: \control, resolution: 200);
)
```

### Recording Animation

```supercollider
(
// Set up recording
~scope = AttractorScope(s, 0, dimension: 3);
~scope.window.bounds = Rect(100, 100, 1920, 1080);  // Set window size

// Use Window's built-in screenshot capability or third-party screen recording
// The scope updates in real-time at the specified resolution
)
```

## Technical Details

### How It Works

AttractorScope creates N-dimensional visualizations by:

1. **Signal Delays**: Takes an input signal and creates multiple delayed versions using `DelayN` UGens
2. **Coordinate Mapping**: Uses these delays as coordinates in N-dimensional space
   - 2D: (signal, delay1)
   - 3D: (signal, delay1, delay2)
   - 4D-6D: (signal, delay1, delay2, delay3, [delay4], [delay5])
3. **Dimensional Rotation**: For 4D-6D, applies rotation matrices in higher-dimensional planes
4. **Projection**: Projects higher dimensions down to 3D, then to 2D screen space
5. **Rendering**: Draws the resulting trail using various rendering techniques

### Performance Optimization

- **Point Interpolation**: Smooth motion between frames (configurable)
- **Color Pre-calculation**: Static colors are cached for performance
- **Adaptive Resolution**: Adjustable points-per-second for CPU management
- **Efficient Drawing**: Optimized Pen operations with minimal state changes

### Buffer Management

The class automatically manages:
- Audio buffers for data storage
- Audio buses for delayed signals
- SynthDefs for each dimension
- Proper cleanup on server quit

## Troubleshooting

### Scope window is blank
- Ensure the server is running: `s.boot`
- Check that audio is being sent to the specified bus
- Try increasing the `resolution` parameter
- Verify the bus index is correct

### Performance issues
- Reduce `trailLength` (try 200-300)
- Reduce `resolution` (try 400-600)
- Switch to a simpler drawing style (Lines or Points)
- Use a static color scheme instead of Rainbow/Velocity

### No attractor visible, just noise
- Adjust `delayTime1` - try values between 0.005 and 0.02
- Ensure your audio signal has some periodic or chaotic structure
- Try different dimension settings

### Scope closes unexpectedly
- Check for SuperCollider error messages in the post window
- Ensure server has enough resources
- Try reducing buffer size

## Credits

Created for SuperCollider audio visualization and analysis.

## License

[Specify your license here - e.g., GPL-3.0, MIT, etc.]

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Changelog

### Version 1.0.0
- Initial release
- Support for 2D-6D visualization
- 6 rendering styles
- 13 color schemes
- Interactive GUI with keyboard controls
- Auto-rotation and manual rotation modes

## Related Projects

- [SuperCollider](https://supercollider.github.io/) - Audio synthesis and algorithmic composition platform
- Other SuperCollider scope classes: `Stethoscope`, `FreqScope`, `FreqScopeView`

## Contact

[Your contact information or link to issues page]

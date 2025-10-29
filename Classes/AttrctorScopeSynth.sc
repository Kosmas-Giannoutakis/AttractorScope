// AttractorScopeSynth class
AttractorScopeSynth {
    var server, <buffer, synthDefName, synth;
    var <delaySynth, <delayBuses;
    var playThread;

    *new { arg server;
        var instance;
        server = server ? Server.default;
        instance = super.newCopyArgs(server);
        ServerQuit.add(instance);
        ^instance;
    }

    play { arg bufSize, bus, delayTime1, delayTime2, delayTime3, delayTime4, delayTime5, dimension, onReady;
        var synthDef, synthArgs;
        var oldSynth, oldBuffer;

        if(server.serverRunning.not) { ^this };

        oldSynth = synth;
        oldBuffer = buffer;
        synth = nil;
        buffer = nil;

        // Always stop and clear delaySynth reference
        if(delaySynth.notNil) {
            server.sendBundle(nil, ['/error', -1], [11, delaySynth.nodeID], ['/error', -2]);
            delaySynth = nil;
        };

        playThread = fork {
            // Create delayBuses if needed
            if(delayBuses.isNil) {
                delayBuses = Bus.audio(server, 6);
                server.sync;

                SynthDef(\attractorDelays, {
                    arg busIndex = 0, rate = 0, out = 0,
                    dt1 = 0.01, dt2 = 0.02, dt3 = 0.03, dt4 = 0.04, dt5 = 0.05;
                    var sig, d1, d2, d3, d4, d5;

                    sig = Select.ar(rate, [
                        InFeedback.ar(busIndex, 1),
                        K2A.ar(In.kr(busIndex, 1))
                    ]);

                    d1 = DelayN.ar(sig, 0.1, dt1);
                    d2 = DelayN.ar(sig, 0.1, dt2);
                    d3 = DelayN.ar(sig, 0.1, dt3);
                    d4 = DelayN.ar(sig, 0.1, dt4);
                    d5 = DelayN.ar(sig, 0.1, dt5);

                    Out.ar(out, [sig, d1, d2, d3, d4, d5]);
                }).send(server);

                server.sync;
            };

            // Always create a fresh delaySynth
            delaySynth = Synth.head(server.defaultGroup, \attractorDelays, [
                \out, delayBuses.index,
                \busIndex, bus.index,
                \rate, if(\audio === bus.rate, 0, 1),
                \dt1, delayTime1,
                \dt2, delayTime2,
                \dt3, delayTime3,
                \dt4, delayTime4,
                \dt5, delayTime5
            ]);

            server.sync;

            buffer = Buffer.alloc(server, bufSize, dimension);
            server.sync;

            synthDefName = "attractorscope" ++ dimension ++ "_" ++ buffer.bufnum;

            synthDef = dimension.switch(
                2, {
                    SynthDef(synthDefName, { arg delayBusIndex, bufnum;
                        var signals, writePos;
                        signals = In.ar(delayBusIndex, 2);
                        writePos = Phasor.ar(0, 1, 0, BufFrames.kr(bufnum));
                        BufWr.ar(signals, bufnum, writePos, 1);
                    });
                },
                3, {
                    SynthDef(synthDefName, { arg delayBusIndex, bufnum;
                        var signals, writePos;
                        signals = In.ar(delayBusIndex, 3);
                        writePos = Phasor.ar(0, 1, 0, BufFrames.kr(bufnum));
                        BufWr.ar(signals, bufnum, writePos, 1);
                    });
                },
                4, {
                    SynthDef(synthDefName, { arg delayBusIndex, bufnum;
                        var signals, writePos;
                        signals = In.ar(delayBusIndex, 4);
                        writePos = Phasor.ar(0, 1, 0, BufFrames.kr(bufnum));
                        BufWr.ar(signals, bufnum, writePos, 1);
                    });
                },
                5, {
                    SynthDef(synthDefName, { arg delayBusIndex, bufnum;
                        var signals, writePos;
                        signals = In.ar(delayBusIndex, 5);
                        writePos = Phasor.ar(0, 1, 0, BufFrames.kr(bufnum));
                        BufWr.ar(signals, bufnum, writePos, 1);
                    });
                },
                6, {
                    SynthDef(synthDefName, { arg delayBusIndex, bufnum;
                        var signals, writePos;
                        signals = In.ar(delayBusIndex, 6);
                        writePos = Phasor.ar(0, 1, 0, BufFrames.kr(bufnum));
                        BufWr.ar(signals, bufnum, writePos, 1);
                    });
                }
            );

            synthArgs = [
                \delayBusIndex, delayBuses.index,
                \bufnum, buffer.bufnum
            ];

            synthDef.send(server);
            server.sync;

            synth = Synth.tail(server.defaultGroup, synthDef.name, synthArgs);
            server.sync;

            "Scope synth ready, reading % channels".format(dimension).postln;

            if(onReady.notNil) {
                {
                    onReady.value(oldSynth, oldBuffer);
                }.defer;
            };
        };
    }

    stop {
        if(playThread.notNil) { playThread.stop; playThread = nil };
        if(synth.notNil) {
            server.sendBundle(nil, ['/error', -1], [11, synth.nodeID], ['/error', -2]);
            synth = nil;
        };
        if(delaySynth.notNil) {
            server.sendBundle(nil, ['/error', -1], [11, delaySynth.nodeID], ['/error', -2]);
            delaySynth = nil;
        };
    }

    isRunning { ^playThread.notNil }

    setBusIndex { arg index;
        if(delaySynth.notNil) {
            delaySynth.set(\busIndex, index);
        };
    }

    setRate { arg rate;
        if(delaySynth.notNil) {
            delaySynth.set(\rate, rate);
        };
    }

    setDelayTime1 { arg time; if(delaySynth.notNil) { delaySynth.set(\dt1, time) }; }
    setDelayTime2 { arg time; if(delaySynth.notNil) { delaySynth.set(\dt2, time) }; }
    setDelayTime3 { arg time; if(delaySynth.notNil) { delaySynth.set(\dt3, time) }; }
    setDelayTime4 { arg time; if(delaySynth.notNil) { delaySynth.set(\dt4, time) }; }
    setDelayTime5 { arg time; if(delaySynth.notNil) { delaySynth.set(\dt5, time) }; }

    free {
        this.stop;

        if(delayBuses.notNil) {
            delayBuses.free;
            delayBuses = nil;
        };

        if(buffer.notNil) {
            buffer.free;
            buffer = nil;
        };

        ServerQuit.remove(this, server);
    }

    doOnServerQuit {
        buffer = nil;
        synth = nil;
        delaySynth = nil;
        delayBuses = nil;
    }
}
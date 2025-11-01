# AttractorScope

A real-time audio visualization tool for **SuperCollider** that generates attractor-based visualizations from audio or control bus signals in 2D through 6D space.

![AttractorScope Demo](assets/demo.gif)

---

## Overview

**AttractorScope** is a visualization class for SuperCollider that transforms audio or control-rate signals into dynamic, multi-dimensional **phase-space embeddings**.
By constructing delayed coordinate representations of input signals in 2D–6D space, it provides an intuitive way to observe core concepts from nonlinear dynamics and chaos theory, such as the underlying structure, periodicity, and complexity of sound signals.

This approach is based on **delay coordinate embedding**, a method introduced by **Packard et al. (1980)** and formalized by **Takens (1981)**.
In this technique, a single time series is reconstructed in an N-dimensional phase space using time-delayed versions of the signal:

$$
x(t) = [s(t), s(t - \tau), s(t - 2\tau), \dots, s(t - (N-1)\tau)]
$$


where $s(t)$ is the signal, $\tau$ is the delay time, and $N$ is the embedding dimension.
This process reveals the attractor geometry of the underlying dynamical system, allowing visual analysis of periodic, quasi-periodic, and chaotic behaviors.

**References:**
- Packard, N. H., Crutchfield, J. P., Farmer, J. D., & Shaw, R. S. (1980). *Geometry from a time series.* Physical Review Letters, 45(9), 712–716.
- Takens F. (1981). Detecting strange attractors in turbulence. In Dynamical Systems and Turbulence: Proceedings of a Symposium Held at the University of Warwick 1979/80, ed. D Rand, L-S Young, pp. 366–81. Berlin: Springer-Verlag
- Kantz, H., & Schreiber, T. (2004). *Nonlinear Time Series Analysis* (2nd ed.). Cambridge University Press.

---

## Features

### Multi-Dimensional Visualization
- **2D–6D Support**: Visualize signals in up to six reconstructed dimensions
- **Dynamic Projection**: Higher dimensions projected into 2D screen space
- **Real-time Rotation**: Continuous rotation through any dimensional plane

### Rendering Styles
- **Lines** – Classic attractor trail visualization
- **Points** – Discrete point-based rendering
- **Glow** – Points with a soft glow effect
- **Ribbon** – 3D-style ribbon with depth shading
- **HeatMap** – Density-based trajectory visualization
- **Smooth** – Anti-aliased smooth lines

### Color Schemes
- **Static Colors**: Nine presets (Yellow, White, Cyan, Magenta, Green, Red, Blue, Orange, Purple)
- **Rainbow**: Animated color cycling
- **Velocity**: Color based on motion speed
- **Distance**: Color based on distance from origin
- **Curvature**: Color based on trajectory curvature

### Interactive Controls
- Full GUI with sliders and menus
- Keyboard shortcuts and mouse interaction
- Resizable window (toggle between small/large)
- Auto-rotation mode

---

## Installation

### Option 1: Install as Quark (Recommended)

The easiest way to install AttractorScope is using SuperCollider's Quark system:

```supercollider
// Install from GitHub
Quarks.install("https://github.com/Kosmas-Giannoutakis/AttractorScope.git");

// Recompile the class library
thisProcess.recompile;
```

That's it! AttractorScope is now ready to use.

**To update later:**
```supercollider
Quarks.update("AttractorScope");
thisProcess.recompile;
```

**To uninstall:**
```supercollider
Quarks.uninstall("AttractorScope");
thisProcess.recompile;
```

### Option 2: Manual Installation

Alternatively, you can manually clone the repository into your **SuperCollider Extensions** directory.

#### **macOS**
```bash
cd ~/Library/Application\ Support/SuperCollider/Extensions/
git clone https://github.com/Kosmas-Giannoutakis/AttractorScope.git
```

#### **Linux**
```bash
cd ~/.local/share/SuperCollider/Extensions/
git clone https://github.com/Kosmas-Giannoutakis/AttractorScope.git
```

#### **Windows**
```bash
cd "%AppData%\SuperCollider\Extensions"
git clone https://github.com/Kosmas-Giannoutakis/AttractorScope.git
```

Then recompile the SuperCollider class library:
**SuperCollider → Language → Recompile Class Library**
or press `Ctrl` + `Shift` + `L` (`Cmd` + `Shift` + `L` on macOS).

---

## Quick Start

### Basic Usage
```supercollider
// Boot the server
s.boot;

// Simple test signal
{ SinOsc.ar(440) * 0.5 }.play;

// Create an AttractorScope on audio bus 0
a = AttractorScope(s, 0);
```

### Noisy Phase Self-Modulation Example
```supercollider
(
// Noisy phase self-modulation
x = {
    var fb = LocalIn.ar(2);
    var freq = 120;
    var mod = SinOsc.kr(0.05).range(0, 2.5);
    var sig = SinOsc.ar(freq, fb * mod, 1, fb * 0.5);
    sig = LeakDC.ar(sig);
    LocalOut.ar(sig);
    sig
}.play;

// Visualize it
a = AttractorScope(s, 0, dimension: 3, trailLength: 1000, resolution: 1000);
)
```

### Polyrhythmic Harmonic Series with Synchronized Modulation Example
```supercollider
(
x = {
    var fundamental = 60;
    var baseModRate = 1/8;  // Base modulation rate in Hz
    var sig = Mix.fill(16, { |i|
        var harmonic = i + 1;
        var freq = fundamental * harmonic;
        var modRate = baseModRate * harmonic;  // Each harmonic modulated proportionally faster
        var ampMod = SinOsc.kr(modRate).range(0, 1);  // Amplitude modulator
        var amp = (1 / harmonic) * ampMod;  // Diminishing amplitude with modulation
        SinOsc.ar(freq, 0, amp * 0.5)
    });
    sig ! 2
}.play;

// Visualize in 4D with longer trail
a = AttractorScope(
    server: s,
    index: 0,
    dimension: 4,
    trailLength: 3000,
    resolution: 2000,
    delayTime1: 0.015
);
)
```

### Custom Parameters
```supercollider
a = AttractorScope(
    server: s,
    index: 0,
    bufsize: 4096,
    delayTime1: 0.01,
    trailLength: 500,
    resolution: 800,
    dimension: 3,
    rotationSpeed: 1.0,
    zoom: 1.0,
    rate: \audio
);
```

---

### **Mouse Click & Drag Action**

You can manually control the rotation of the visualization in 3D and higher dimensions.

*   **Click and Drag:** Click anywhere inside the visualization window and drag the mouse to manually rotate the attractor.
    *   Dragging **horizontally** (left and right) rotates the view around the **Y-axis**.
    *   Dragging **vertically** (up and down) rotates the view around the **X-axis**.
*   **Auto-Rotation Pause:** The moment you click to drag, `Auto-Rotate` is automatically disabled, giving you full control. You can re-enable it using the 'a' key or the checkbox.
*   **Release:** When you release the mouse button, the manual rotation stops, and the view remains in its new orientation.

---

### Screenshot Export

AttractorScope can capture high-quality screenshots of your visualizations.

**How to Export:**
- Press **`e`** key, or
- Click the **"Export PNG"** button in the GUI

**Output:**
- **Format:** PNG (lossless quality)
- **Location:** Saved to your `Pictures` folder
- **Filename:** Automatically timestamped as `AttractorScope_[dimension]D_[timestamp].png`
  - Example: `AttractorScope_3D_20241030_143022.png`

The screenshot captures only the visualization area (black canvas with attractor), excluding GUI controls for clean exports.

**Note:** The file path is printed to the Post Window after each successful export.

---

## Keyboard Shortcuts

| Key | Function |
| :--- | :--- |
| **`2`**–**`6`** | Set visualization dimension directly to 2D, 3D, 4D, 5D, or 6D. |
| **`c`** | Cycle through the available **color schemes**. |
| **`s`** | Cycle through the available rendering **styles**. |
| **`a`** | Toggle **auto-rotation** on or off. |
| **`r`** | **Reset** the view's rotation and zoom to their default states. |
| **`m`** | Toggle the main window **size** between small and large presets. |
| **`[`** / **`]`** | Decrease / Increase the base **Delay (Dt)**. |
| **`{`** / **`}`** | Decrease / Increase the **Trail** length. |
| **`<`** / **`>`** | Decrease / Increase the **Resolution**. |
| **`,`** / **`.`** | Decrease / Increase the **Rotation Speed**. |
| **`-`** / **`=`** | Decrease / Increase the **Zoom** level. |
| **`e`** | **Export** a PNG screenshot of the current visualization to your Pictures folder. |

---

## Parameters

### Core Parameters

| Parameter | Type | Range | Default | Description |
| :--- | :--- | :--- | :--- | :--- |
| `server` | Server | - | `default` | SuperCollider server instance |
| `index` | Integer/Bus | 0–N | 0 | Audio or control bus index |
| `bufsize` | Integer | 128+ | 4096 | Internal buffer size |
| `delayTime1` | Float | 0.0001–0.05 | 0.01 | Base delay time |
| `trailLength` | Integer | 10–5000 | 500 | Number of visible trail points |
| `resolution` | Integer | 100–2000 | 800 | Points computed per second |
| `dimension` | Integer | 2–6 | 3 | Embedding dimensionality |
| `rotationSpeed` | Float | 0.1–5.0 | 1.0 | Auto-rotation speed multiplier |
| `zoom` | Float | 0.25–4.0 | 1.0 | Initial zoom level |
| `rate` | Symbol | - | `\audio` | Bus rate: `\audio` or `\control` |

### Choosing Embedding Parameters

The quality of attractor reconstruction depends on two key parameters:

**Delay Time (τ):**
- Determines temporal spacing between coordinates
- **Too small**: coordinates are redundant (attractor collapses)
- **Too large**: coordinates are uncorrelated (structure is lost)
- **General guideline**: Start with 0.005–0.02s for audio signals
- **For periodic signals**: Use 1/10 to 1/4 of the period
- **For complex signals**: Adjust until clear structure emerges

**Embedding Dimension (N):**
- Must be high enough to "unfold" the attractor without false overlaps
- **N = 2**: Simple periodic signals only
- **N = 3**: Most audio signals (good default)
- **N = 4–5**: Complex or chaotic signals
- **N = 6**: Rarely needed, very high-dimensional dynamics
---

## Technical Details

### Implementation Overview
- **Signal Delays** – Uses `DelayN` UGens to create delayed signal versions
- **Coordinate Mapping** – Constructs N-dimensional vectors from delayed samples
- **Rotation Matrices** – Applies multi-dimensional rotations
- **Projection** – Projects higher dimensions into 2D space for visualization
- **Rendering** – Draws the trajectory using optimized `Pen`-based rendering

### Performance
- Adaptive resolution for CPU efficiency
- Cached color palettes
- Smooth interpolation between frames
- Optimized drawing operations

---

## Contributing
Contributions are welcome! If you find a bug, have an idea for a new feature, or want to improve the implementation, please feel free to open an issue or submit a pull request.

## License
Licensed under the **GNU General Public License v3.0**. See [LICENSE](LICENSE) for details.

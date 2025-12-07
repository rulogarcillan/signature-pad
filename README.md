# Compose Multiplatform Signature Pad

[![Maven Central](https://img.shields.io/maven-central/v/com.tuppersoft/signature-pad?color=32cd32)](https://central.sonatype.com/artifact/com.tuppersoft/signature-pad)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Platforms](https://img.shields.io/badge/Platforms-Android%20%7C%20Desktop-brightgreen.svg)](https://kotlinlang.org/docs/multiplatform.html)
[![Kotlin](https://img.shields.io/badge/Kotlin-100%25-purple.svg)](https://kotlinlang.org)

A modern **Compose Multiplatform** library for capturing smooth signature drawings with **UNDO/REDO** functionality. Supports **Android** and **Desktop (JVM)**.

Enhanced fork of [gcacace/android-signaturepad](https://github.com/gcacace/android-signaturepad), completely rewritten in Kotlin for KMP.

<p align="center">
  <img src="ART/sign.gif" alt="Signature Pad Demo"/>
</p>

## Features

### Drawing Engine
- 🎨 **Smooth Bézier Curves**: Catmull-Rom spline interpolation for natural stroke rendering
- 📏 **Velocity-Based Width**: Dynamic stroke width based on drawing speed (faster = thinner, slower = thicker)
- 🎯 **Pressure Simulation**: Gamma curve adjustment for realistic pen pressure feel
- 🔄 **Adaptive Smoothing**: EMA filters for velocity and width transitions (configurable 0.0-1.0)
- 📐 **Noise Filtering**: Configurable input threshold to eliminate jitter

### Configuration & Customization
- 🖊️ **3 Pre-configured Presets**: Fountain pen, BIC pen, Edding marker with optimized parameters
- ⚙️ **9 Adjustable Parameters**: Width range, color, velocity thresholds, smoothing weights, pressure gamma, noise threshold
- 🎨 **Custom Color Support**: Any color for pen strokes

### User Experience
- ♻️ **Full UNDO/REDO**: Complete history management for individual strokes
- 📤 **Multiple Export Formats**: Bitmap (white/transparent background), SVG with auto-crop support
- ✂️ **Smart Crop**: Automatic content detection with configurable padding
- 🏗️ **Compose Best Practices**: State hoisting, explicit API mode, immutable data structures

## Installation

[![Maven Central](https://img.shields.io/maven-central/v/com.tuppersoft/signature-pad?color=32cd32)](https://central.sonatype.com/artifact/com.tuppersoft/signature-pad)
```gradle
dependencies {
    implementation("com.tuppersoft:signature-pad:$lastVersion")
}
```

## Quick Start

```kotlin
@Composable
fun SignatureScreen() {
    val state = rememberSignaturePadState()
    
    Column {
        SignaturePad(
            state = state,
            config = SignaturePadConfig.fountainPen(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
```
## Usage

### Writing Instrument Presets

```kotlin
// Fountain Pen (1-4dp) - Elegant, variable width
SignaturePad(config = SignaturePadConfig.fountainPen())

// BIC Pen (1.8-2.8dp) - Nearly uniform with subtle ink accumulation
SignaturePad(config = SignaturePadConfig.pen())

// Marker (5-7dp) - Bold, thick strokes
SignaturePad(config = SignaturePadConfig.marker())
```

### Custom Configuration

```kotlin
SignaturePad(
    config = SignaturePadConfig(
        penMinWidth = 2.dp,              // Thinnest line
        penMaxWidth = 8.dp,               // Thickest line
        penColor = Color.Blue,            // Pen color
        velocitySmoothness = 0.8f,        // Drawing smoothness (0.0-1.0)
        widthSmoothness = 0.7f,           // Width transition smoothness (0.0-1.0)
        minVelocity = 0f,                 // Speed for max width (px/ms)
        maxVelocity = 10f,                // Speed for min width (px/ms)
        widthVariation = 1.5f,            // Thickness contrast (1.0 = linear)
        inputNoiseThreshold = 1.0f        // Hand shake filter (px)
    )
)
```

### State Management

```kotlin
val state = rememberSignaturePadState()

// Check state
state.isEmpty           // Boolean
state.canUndo()        // Boolean
state.canRedo()        // Boolean

// Actions
state.undo()           // Boolean (returns true if successful)
state.redo()           // Boolean (returns true if successful)
state.clear()          // Unit
```

### Export

```kotlin
// SVG
val svg: String = state.toSvg()

// Bitmap with white background
val bitmap = state.toBitmap()

// Bitmap with transparent background
val transparentBitmap = state.toTransparentBitmap()

// Auto-crop to signature bounds
val croppedBitmap = state.toBitmap(
    crop = true,
    paddingCrop = 16  // pixels of padding around signature
)
```

### Callbacks

```kotlin
SignaturePad(
    state = state,
    config = SignaturePadConfig.fountainPen(),
    onStartSign = { /* User started drawing */ },
    onSign = { /* Signature updated */ },
    onClear = { /* Signature cleared */ }
)
```

## Configuration Parameters

| Parameter             | Type  | Range   | Default            | User Question                                  | Description                                                      |
| --------------------- | ----- | ------- | ------------------ | ---------------------------------------------- | ---------------------------------------------------------------- |
| `penMinWidth`         | Dp    | -       | 1.0.dp             | *"What's the thinnest my line can be?"*        | Minimum stroke width (fast drawing)                              |
| `penMaxWidth`         | Dp    | -       | 4.0.dp             | *"What's the thickest my line can be?"*        | Maximum stroke width (slow drawing)                              |
| `penColor`            | Color | -       | Ink Blue (#003D82) | *"What color is my pen?"*                      | Stroke color                                                     |
| `velocitySmoothness`  | Float | 0.0-1.0 | 0.85               | *"How smooth should the drawing feel?"*        | Stroke smoothness (0.0 = jumpy, 1.0 = very smooth)               |
| `widthSmoothness`     | Float | 0.0-1.0 | 0.7                | *"How gradual should thickness changes be?"*   | Width transition smoothness (0.0 = abrupt, 1.0 = gradual)        |
| `minVelocity`         | Float | 0.0+    | 0.0                | *"When does the line stop getting thicker?"*   | Velocity for maximum width (px/ms)                               |
| `maxVelocity`         | Float | 0.0+    | 8.0                | *"When does the line stop getting thinner?"*   | Velocity for minimum width (px/ms)                               |
| `widthVariation`      | Float | 0.5-3.0 | 1.5                | *"How much should thickness vary with speed?"* | Width contrast (1.0 = linear, >1.0 = more contrast, <1.0 = less) |
| `inputNoiseThreshold` | Float | 0.0+    | 0.8                | *"How much should I filter hand shake?"*       | Min distance between points to filter tremor (px)                |

## License

```
Copyright 2025 Tuppersoft by Rulo Garcillan
Copyright 2014-2016 Gianluca Cacace

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

This is a derivative work based on [android-signaturepad](https://github.com/gcacace/android-signaturepad) by Gianluca Cacace.

## Contributing

Contributions are welcome! Please submit a Pull Request.

---

**⭐ If you find this library useful, please star the repo!**


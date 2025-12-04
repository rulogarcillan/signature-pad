# Android Signature Pad - Jetpack Compose
[![Maven Central Version](https://img.shields.io/maven-central/v/com.tuppersoft/signature-pad?color=32cd32)](https://central.sonatype.com/artifact/com.tuppersoft/signature-pad)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=21)
[![Kotlin](https://img.shields.io/badge/Kotlin-100%25-purple.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Compose-Ready-brightgreen.svg)](https://developer.android.com/jetpack/compose)

A modern **Jetpack Compose** library for capturing smooth signature drawings with **UNDO/REDO functionality**. Enhanced fork of [gcacace/android-signaturepad](https://github.com/gcacace/android-signaturepad), completely rewritten in Kotlin with Compose-first architecture.

<p align="center">
  <img src="ART/sign.gif" alt="Signature Pad Demo"/>
</p>

## ✨ Features

- 🎨 **Smooth Bézier Curves**: Variable width based on drawing velocity with Hermite smoothstep interpolation
- ♻️ **UNDO/REDO**: Full history management for individual strokes
- 📱 **Jetpack Compose Native**: Built with Compose best practices
- 🖊️ **Writing Instrument Presets**: Fountain pen, BIC, marker, Edding styles
- 🎨 **Highly Customizable**: Pen color, stroke width, velocity smoothing
- 📤 **Export**: Bitmap and SVG format support
- 🏗️ **State Hoisting**: Follows Compose architecture patterns
- 🔒 **Type Safe**: Explicit API mode with strict null-safety

## 📦 Installation

[![Maven Central Version](https://img.shields.io/maven-central/v/com.tuppersoft/signature-pad?color=32cd32)](https://central.sonatype.com/artifact/com.tuppersoft/signature-pad)

```kotlin
dependencies {
    implementation("com.tuppersoft:signature-pad:$lastVersion")
}
```

## 🚀 Quick Start

```kotlin
@Composable
fun SignatureScreen() {
    val state = rememberSignaturePadState()
    
    Column {
        SignaturePad(
            state = state,
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
        Row {
            Button(
                onClick = { state.undo() },
                enabled = state.canUndo()
            ) {
                Text("Undo")
            }
            Button(
                onClick = { state.redo() },
                enabled = state.canRedo()
            ) {
                Text("Redo")
            }
            Button(
                onClick = { state.clear() },
                enabled = !state.isEmpty
            ) {
                Text("Clear")
            }
        }
    }
}
```

## 🖊️ Writing Instrument Presets

```kotlin
// Fountain Pen - Elegant with moderate contrast (1-4.5dp)
val state = rememberSignaturePadState(
    signaturePadConfig = SignaturePadConfig.fountainPen()
)

// BIC Pen - Uniform and consistent (2-2.5dp)
val state = rememberSignaturePadState(
    signaturePadConfig = SignaturePadConfig.bicPen()
)

// Marker - Thick and uniform (3-4dp)
val state = rememberSignaturePadState(
    signaturePadConfig = SignaturePadConfig.marker()
)

// Edding - Very bold (5-6.5dp)
val state = rememberSignaturePadState(
    signaturePadConfig = SignaturePadConfig.edding()
)

// Custom configuration
val state = rememberSignaturePadState(
    signaturePadConfig = SignaturePadConfig(
        penMinWidth = 2.dp,
        penMaxWidth = 8.dp,
        penColor = Color.Blue,
        velocityFilterWeight = 0.8f  // 0.3-0.5: responsive, 0.6-0.8: balanced, 0.85-0.95: smooth
    )
)
```

## 📖 API Reference

### State Management

```kotlin
val state = rememberSignaturePadState(
    signaturePadConfig = SignaturePadConfig.fountainPen(penColor = Color.Blue)
)

// Properties
state.isEmpty           // Boolean: true if no signature
state.canUndo()        // Boolean: true if undo is available
state.canRedo()        // Boolean: true if redo is available

// Actions
state.undo()           // Boolean: returns true if undo was successful
state.redo()           // Boolean: returns true if redo was successful
state.clear()          // Unit: clears all strokes

// Dynamic configuration
state.penMinWidth = 3.dp
state.penMaxWidth = 8.dp
state.penColor = Color.Red
state.velocityFilterWeight = 0.85f
```

### Callbacks

```kotlin
SignaturePad(
    state = state,
    modifier = modifier,
    onStartSign = { /* Called when user starts drawing */ },
    onSign = { /* Called when signature is updated */ },
    onClear = { /* Called when signature is cleared */ }
)
```

### Export

```kotlin
// SVG
val svg = state.toSvg()

// Bitmap (transparent or white background)
val transparentBitmap = state.toTransparentBitmap()
val whiteBitmap = state.toBitmap()
```

## 🏗️ Architecture

### Key Features

- **Immutable Data**: All stroke data uses immutable collections
- **Adaptive Sampling**: Bézier curves use 10-30 steps based on length
- **Hermite Smoothstep**: Width interpolation (3t² - 2t³) for natural transitions
- **Explicit API**: All public APIs documented with KDoc
- **Type Safety**: Strict null-safety, no `!!` operators
- **Compose Best Practices**: `rememberUpdatedState`, proper state hoisting

### Performance

- ✅ Object reuse (cached Bézier and control point instances)
- ✅ Adaptive sampling (more steps for longer curves)
- ✅ Optimized bitmap redrawing for UNDO/REDO
- ✅ Real-time drawing with incremental updates

## 🤝 Contributing

Contributions are welcome! Please submit a Pull Request.

```bash
git clone https://github.com/rulogarcillan/signature-pad.git
cd signature-pad
./gradlew build
```

### Code Quality

This project uses:
- **Detekt** for static analysis
- **Detekt Compose Rules** for Compose best practices
- **Explicit API mode** for clear public APIs
- **KDoc** for comprehensive documentation

```bash
# Run code quality checks
./gradlew detekt
```

## 📄 License

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

This is a derivative work based on [android-signaturepad](https://github.com/gcacace/android-signaturepad) by Gianluca Cacace, complying with Apache License 2.0 requirements.

## 📧 Support

For questions or issues, please [open an issue](https://github.com/rulogarcillan/signature-pad/issues) on GitHub.

---

**⭐ If you find this library useful, please star the repo!**




package com.tuppersoft.signaturepad.sample.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.tuppersoft.signaturepad.sample.App

/**
 * Desktop launcher for the Signature Pad KMP sample app.
 *
 * This is the entry point for the desktop (JVM) version of the app.
 * It creates a window and launches the common multiplatform UI defined in App.kt.
 */
public fun main(): Unit = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Signature Pad KMP Demo"
    ) {
        App()
    }
}

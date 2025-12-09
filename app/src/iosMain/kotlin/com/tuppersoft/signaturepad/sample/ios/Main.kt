package com.tuppersoft.signaturepad.sample.ios

import androidx.compose.ui.window.ComposeUIViewController
import com.tuppersoft.signaturepad.sample.App
import platform.UIKit.UIViewController

/**
 * Entry point for iOS application.
 * This function is called from Swift/Objective-C code.
 */
@Suppress("unused", "FunctionName")
fun MainViewController(): UIViewController = ComposeUIViewController { App() }


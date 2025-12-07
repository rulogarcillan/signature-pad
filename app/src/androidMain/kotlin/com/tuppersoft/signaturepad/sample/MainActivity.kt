package com.tuppersoft.signaturepad.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tuppersoft.signaturepad.sample.theme.AndroidSignaturePadTheme

/**
 * Main activity for the Android Signature Pad sample app.
 *
 * This activity simply launches the common multiplatform UI defined in App.kt.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidSignaturePadTheme {
                App()
            }
        }
    }
}

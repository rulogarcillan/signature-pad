package com.tuppersoft.signaturepad.preview
 
 import androidx.compose.foundation.layout.Column
 import androidx.compose.foundation.layout.fillMaxSize
 import androidx.compose.foundation.layout.padding
 import androidx.compose.material.Text
 import androidx.compose.ui.Alignment
 import androidx.compose.ui.Modifier
 import androidx.compose.ui.graphics.Color
 import androidx.compose.ui.unit.dp
 import androidx.compose.ui.window.Window
 import androidx.compose.ui.window.application
 import com.tuppersoft.signaturepad.compose.SignaturePad
 import com.tuppersoft.signaturepad.compose.SignaturePadConfig
 import com.tuppersoft.signaturepad.compose.rememberSignaturePadState
 
 fun main() = application {
     Window(onCloseRequest = ::exitApplication, title = "Signature Pad KMP Demo") {
         val state = rememberSignaturePadState()
         
         Column(
             modifier = Modifier.fillMaxSize().padding(16.dp),
             horizontalAlignment = Alignment.CenterHorizontally
         ) {
             Text("Signature Pad Desktop Demo")
             
             SignaturePad(
                 modifier = Modifier.fillMaxSize().weight(1f),
                 state = state,
                 config = SignaturePadConfig.fountainPen(penColor = Color.Blue)
             )
         }
     }
 }

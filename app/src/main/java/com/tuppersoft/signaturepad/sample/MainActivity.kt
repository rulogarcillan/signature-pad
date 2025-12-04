package com.tuppersoft.signaturepad.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Icons.AutoMirrored.Filled
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tuppersoft.signaturepad.compose.rememberSignaturePadState
import com.tuppersoft.signaturepad.sample.theme.AndroidSignaturepadTheme
import com.tuppersoft.signaturepad.compose.SignaturePad as SignaturePadCompose

/**
 * Main activity showcasing the Signature Pad in a Jetpack Compose layout.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidSignaturepadTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        SignaturePadScreenWithToggle()
                    }
                }
            }
        }
    }
}

@Composable
private fun SignaturePadScreenWithToggle() {
    Column(modifier = Modifier.fillMaxSize()) {
        SignaturePadComposeVersion()
    }
}

@Composable
private fun SignaturePadComposeVersion() {
    val state = rememberSignaturePadState()
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Sign here",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp)
        )

        SignaturePadCompose(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(8.dp)
                )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { state.undo() },
                enabled = state.canUndo(),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Filled.Undo,
                    contentDescription = "Undo"
                )
            }

            Button(
                onClick = { state.redo() },
                enabled = state.canRedo(),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Filled.Redo,
                    contentDescription = "Redo"
                )
            }

            Button(
                onClick = { state.clear() },
                enabled = !state.isEmpty,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Clear"
                )
            }
        }
    }
}

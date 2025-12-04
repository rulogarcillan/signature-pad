package com.tuppersoft.signaturepad.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
                SignaturePadComposeVersion()
            }
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun SignaturePadComposeVersion() {
    val state = rememberSignaturePadState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.White,
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { state.undo() },
                    enabled = state.canUndo(),
                    modifier = Modifier
                        .size(48.dp)
                        .border(
                            width = 1.dp,
                            color = if (state.canUndo()) Color(0xFFE0E0E0) else Color(0xFFF5F5F5),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (state.canUndo()) Color(0xFF333333) else Color(0xFFBDBDBD)
                    )
                }

                IconButton(
                    onClick = { state.redo() },
                    enabled = state.canRedo(),
                    modifier = Modifier
                        .size(48.dp)
                        .border(
                            width = 1.dp,
                            color = if (state.canRedo()) Color(0xFFE0E0E0) else Color(0xFFF5F5F5),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (state.canRedo()) Color(0xFF333333) else Color(0xFFBDBDBD)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                TextButton(
                    onClick = { state.clear() },
                    enabled = !state.isEmpty,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFF666666),
                        disabledContentColor = Color(0xFFBDBDBD)
                    )
                ) {
                    Text(
                        text = "Clear",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            SignaturePadCompose(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                // Línea de firma
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFE0E0E0))
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "I agree terms and conditions",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9E9E9E),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

package com.tuppersoft.signaturepad.sample

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
import com.tuppersoft.signaturepad.compose.SignaturePad
import com.tuppersoft.signaturepad.compose.rememberSignaturePadState
import com.tuppersoft.signaturepad.sample.theme.SignaturePadTheme

/**
 * Main application composable - shared across all platforms.
 *
 * This composable contains the complete UI for the Signature Pad sample app,
 * including the signature canvas, undo/redo buttons, and clear functionality.
 */
@Composable
public fun App() {
    SignaturePadTheme {
        SignaturePadScreen()
    }
}

@Composable
private fun SignaturePadScreen() {
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
            SignaturePad(
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
                // Signature line
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

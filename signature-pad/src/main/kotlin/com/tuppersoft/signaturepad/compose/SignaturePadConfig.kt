package com.tuppersoft.signaturepad.compose

import androidx.annotation.FloatRange
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.pow

/**
 * Configuration for [SignaturePad] drawing behavior.
 *
 * Encapsulates stroke width, color, and smoothing parameters with preset
 * configurations for common writing instruments.
 *
 * Presets: [fountainPen], [bicPen], [marker], [edding]
 *
 * Example:
 * ```
 * val config = SignaturePadConfig.fountainPen(penColor = Color.Blue)
 * ```
 *
 * @property penMinWidth Minimum stroke width when drawing fast.
 * @property penMaxWidth Maximum stroke width when drawing slowly.
 * @property penColor Color of the drawn strokes.
 * @property velocityFilterWeight Smoothing factor for velocity (0.0-1.0).
 * @property widthFilterWeight Smoothing factor for width transitions (0.0-1.0).
 * @property minVelocity Minimum velocity threshold in pixels per millisecond.
 * @property maxVelocity Maximum velocity threshold in pixels per millisecond.
 * @property pressureGamma Gamma curve factor for pressure simulation (0.5-3.0).
 * @property inputNoiseThreshold Minimum distance in pixels between consecutive points.
 */
@Immutable
public data class SignaturePadConfig(
    val penMinWidth: Dp = DefaultPenMinWidth,
    val penMaxWidth: Dp = DefaultPenMaxWidth,
    val penColor: Color = DefaultPenColor,
    @FloatRange(from = 0.0, to = 1.0) val velocityFilterWeight: Float = DefaultVelocityFilterWeight,
    @FloatRange(from = 0.0, to = 1.0) val widthFilterWeight: Float = DefaultWidthFilterWeight,
    @FloatRange(from = 0.0) val minVelocity: Float = DefaultMinVelocity,
    @FloatRange(from = 0.0) val maxVelocity: Float = DefaultMaxVelocity,
    @FloatRange(from = 0.5, to = 3.0) val pressureGamma: Float = DefaultPressureGamma,
    @FloatRange(from = 0.0) val inputNoiseThreshold: Float = DefaultInputNoiseThreshold,
) {

    public companion object {
        /**
         * Fountain pen: elegant with moderate contrast (1-4.5dp).
         * Best for elegant signatures and formal documents.
         */
        public fun fountainPen(penColor: Color = DefaultPenColor): SignaturePadConfig {
            return SignaturePadConfig(
                penMinWidth = 1.0.dp,
                penMaxWidth = 4.5.dp,
                velocityFilterWeight = 0.85f,
                widthFilterWeight = 0.7f,
                minVelocity = 0f,
                maxVelocity = 8f,
                pressureGamma = 1.5f,
                inputNoiseThreshold = 0.8f,
                penColor = penColor
            )
        }

        /**
         * BIC pen: uniform and consistent (2-2.5dp).
         * Best for everyday signatures and forms.
         */
        public fun bicPen(penColor: Color = DefaultPenColor): SignaturePadConfig {
            return SignaturePadConfig(
                penMinWidth = 2.dp,
                penMaxWidth = 2.5.dp,
                velocityFilterWeight = 0.95f,
                widthFilterWeight = 0.8f,
                minVelocity = 0f,
                maxVelocity = 12f,
                pressureGamma = 1.0f,
                inputNoiseThreshold = 1.0f,
                penColor = penColor
            )
        }

        /**
         * Marker: thick and uniform (3-4dp).
         * Best for bold signatures and emphasis.
         */
        public fun marker(penColor: Color = DefaultPenColor): SignaturePadConfig {
            return SignaturePadConfig(
                penMinWidth = 3.dp,
                penMaxWidth = 4.dp,
                velocityFilterWeight = 0.92f,
                widthFilterWeight = 0.85f,
                minVelocity = 0f,
                maxVelocity = 15f,
                pressureGamma = 1.2f,
                inputNoiseThreshold = 1.2f,
                penColor = penColor
            )
        }

        /**
         * Edding marker: very bold (5-6.5dp).
         * Best for maximum visibility.
         */
        public fun edding(penColor: Color = DefaultPenColor): SignaturePadConfig {
            return SignaturePadConfig(
                penMinWidth = 5.dp,
                penMaxWidth = 6.5.dp,
                velocityFilterWeight = 0.93f,
                widthFilterWeight = 0.88f,
                minVelocity = 0f,
                maxVelocity = 18f,
                pressureGamma = 1.1f,
                inputNoiseThreshold = 1.5f,
                penColor = penColor
            )
        }
    }
}

/**
 * Calculates stroke width based on velocity using pressure simulation.
 * Formula: W = W_min + (W_max - W_min) · (1 - v_norm)^γ
 */
internal fun SignaturePadConfig.calculateStrokeWidth(
    velocity: Float,
    minWidthPx: Float,
    maxWidthPx: Float
): Float {
    val velocityRange = maxVelocity - minVelocity
    val normalizedVelocity = if (velocityRange > 0f) {
        ((velocity - minVelocity) / velocityRange).coerceIn(0f, 1f)
    } else {
        0.5f
    }

    val simulatedPressure = 1f - normalizedVelocity
    val pressureWithGamma = simulatedPressure.pow(pressureGamma)

    return minWidthPx + (maxWidthPx - minWidthPx) * pressureWithGamma
}

// ========================================
// Default Values
// ========================================

private val DefaultPenMinWidth: Dp = 4.dp
private val DefaultPenMaxWidth: Dp = 7.dp
private val DefaultPenColor: Color = Color.Black
private const val DefaultVelocityFilterWeight: Float = 0.9f
private const val DefaultWidthFilterWeight: Float = 0.7f
private const val DefaultMinVelocity: Float = 0f
private const val DefaultMaxVelocity: Float = 10f
private const val DefaultPressureGamma: Float = 1.5f
private const val DefaultInputNoiseThreshold: Float = 1.0f

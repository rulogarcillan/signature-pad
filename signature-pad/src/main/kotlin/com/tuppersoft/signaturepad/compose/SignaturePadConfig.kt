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
 * Available presets: [fountainPen], [pen], [marker]
 *
 * Example:
 * ```
 * val config = SignaturePadConfig.fountainPen(penColor = Color.Blue)
 * ```
 *
 * @property penMinWidth Minimum stroke width when drawing fast. *"What's the thinnest my line can be?"*
 * @property penMaxWidth Maximum stroke width when drawing slowly. *"What's the thickest my line can be?"*
 * @property penColor Color of the drawn strokes. *"What color is my pen?"*
 * @property velocitySmoothness How smooth the stroke appears (0.0 = responsive but jumpy, 1.0 = very smooth). *"How smooth should the drawing feel?"*
 * @property widthSmoothness How gradual width changes are (0.0 = abrupt changes, 1.0 = smooth transitions). *"How gradual should thickness changes be?"*
 * @property minVelocity Minimum velocity threshold (px/ms). At this speed, stroke reaches maximum width. *"When does the line stop getting thicker?"*
 * @property maxVelocity Maximum velocity threshold (px/ms). At this speed, stroke reaches minimum width. *"When does the line stop getting thinner?"*
 * @property widthVariation How much width varies with speed (1.0 = linear response, >1.0 = more contrast, <1.0 = less contrast). *"How much should thickness vary with speed?"*
 * @property inputNoiseThreshold Minimum distance in pixels between consecutive points to filter hand tremor. *"How much should I filter hand shake?"*
 */
@Immutable
public data class SignaturePadConfig(
    val penMinWidth: Dp = DefaultPenMinWidth,
    val penMaxWidth: Dp = DefaultPenMaxWidth,
    val penColor: Color = DefaultPenColor,
    @FloatRange(from = 0.0, to = 1.0) val velocitySmoothness: Float = DefaultVelocitySmoothness,
    @FloatRange(from = 0.0, to = 1.0) val widthSmoothness: Float = DefaultWidthSmoothness,
    @FloatRange(from = 0.0) val minVelocity: Float = DefaultMinVelocity,
    @FloatRange(from = 0.0) val maxVelocity: Float = DefaultMaxVelocity,
    @FloatRange(from = 0.5, to = 3.0) val widthVariation: Float = DefaultWidthVariation,
    @FloatRange(from = 0.0) val inputNoiseThreshold: Float = DefaultInputNoiseThreshold,
) {

    public companion object {
        /**
         * Fountain pen: elegant with moderate contrast (1-4dp).
         * Best for elegant signatures and formal documents.
         */
        public fun fountainPen(penColor: Color = DefaultPenColor): SignaturePadConfig {
            return SignaturePadConfig(
                penMinWidth = DefaultPenMinWidth,
                penMaxWidth = DefaultPenMaxWidth,
                velocitySmoothness = DefaultVelocitySmoothness,
                widthSmoothness = DefaultWidthSmoothness,
                minVelocity = DefaultMinVelocity,
                maxVelocity = DefaultMaxVelocity,
                widthVariation = DefaultWidthVariation,
                inputNoiseThreshold = DefaultInputNoiseThreshold,
                penColor = penColor
            )
        }

        /**
         * Pen: uniform and consistent (2-2.5dp).
         * Best for everyday signatures and forms.
         */
        public fun pen(penColor: Color = DefaultPenColor): SignaturePadConfig {
            return SignaturePadConfig(
                penMinWidth = 2.dp,
                penMaxWidth = 2.5.dp,
                velocitySmoothness = 0.95f,
                widthSmoothness = 0.8f,
                minVelocity = 0f,
                maxVelocity = 12f,
                widthVariation = 1.0f,
                inputNoiseThreshold = 1.0f,
                penColor = penColor
            )
        }

        /**
         * Marker: very bold (5-6.5dp).
         * Best for maximum visibility.
         */
        public fun marker(penColor: Color = DefaultPenColor): SignaturePadConfig {
            return SignaturePadConfig(
                penMinWidth = 5.dp,
                penMaxWidth = 6.5.dp,
                velocitySmoothness = 0.93f,
                widthSmoothness = 0.88f,
                minVelocity = 0f,
                maxVelocity = 18f,
                widthVariation = 1.1f,
                inputNoiseThreshold = 1.5f,
                penColor = penColor
            )
        }
    }
}

/**
 * Calculates stroke width based on velocity using curve transformation.
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

    val widthFactor = 1f - normalizedVelocity
    val curvedWidthFactor = widthFactor.pow(widthVariation)

    return minWidthPx + (maxWidthPx - minWidthPx) * curvedWidthFactor
}

// ========================================
// Default Values
// ========================================

private val DefaultPenMinWidth: Dp = 1.0.dp
private val DefaultPenMaxWidth: Dp = 4.dp
private val DefaultPenColor: Color = Color(0xFF003D82)
private const val DefaultVelocitySmoothness: Float = 0.85f
private const val DefaultWidthSmoothness: Float = 0.7f
private const val DefaultMinVelocity: Float = 0f
private const val DefaultMaxVelocity: Float = 8f
private const val DefaultWidthVariation: Float = 1.5f
private const val DefaultInputNoiseThreshold: Float = 0.8f

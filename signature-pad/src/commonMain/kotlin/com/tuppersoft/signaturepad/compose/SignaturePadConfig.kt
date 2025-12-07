package com.tuppersoft.signaturepad.compose

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
 * All numeric parameters are validated at initialization time using `require()` checks,
 * ensuring type-safe configuration across all platforms (KMP replacement for Android's `@FloatRange`).
 *
 * Available presets: [fountainPen], [pen], [marker]
 *
 * Example:
 * ```
 * val config = SignaturePadConfig.fountainPen(penColor = Color.Blue)
 * ```
 *
 * @property penMinWidth Minimum stroke width when drawing fast. Must be > 0. *"What's the thinnest my line can be?"*
 * @property penMaxWidth Maximum stroke width when drawing slowly. Must be >= penMinWidth. *"What's the thickest my line can be?"*
 * @property penColor Color of the drawn strokes. *"What color is my pen?"*
 * @property velocitySmoothness How smooth the stroke appears. Range: [0.0, 1.0]. *"How smooth should the drawing feel?"*
 * @property widthSmoothness How gradual width changes are. Range: [0.0, 1.0]. *"How gradual should thickness changes be?"*
 * @property minVelocity Minimum velocity threshold (px/ms). Must be >= 0.0. *"When does the line stop getting thicker?"*
 * @property maxVelocity Maximum velocity threshold (px/ms). Must be >= 0.0. *"When does the line stop getting thinner?"*
 * @property widthVariation How much width varies with speed. Range: [0.5, 3.0]. *"How much should thickness vary with speed?"*
 * @property inputNoiseThreshold Minimum distance in pixels to filter hand tremor. Must be >= 0.0. *"How much should I filter hand shake?"*
 *
 * @throws IllegalArgumentException if any parameter is out of its valid range
 */
@Immutable
public data class SignaturePadConfig(
    val penMinWidth: Dp = DefaultPenMinWidth,
    val penMaxWidth: Dp = DefaultPenMaxWidth,
    val penColor: Color = DefaultPenColor,
    val velocitySmoothness: Float = DefaultVelocitySmoothness,
    val widthSmoothness: Float = DefaultWidthSmoothness,
    val minVelocity: Float = DefaultMinVelocity,
    val maxVelocity: Float = DefaultMaxVelocity,
    val widthVariation: Float = DefaultWidthVariation,
    val inputNoiseThreshold: Float = DefaultInputNoiseThreshold,
) {

    init {
        require(velocitySmoothness in 0.0f..1.0f) {
            "velocitySmoothness must be between 0.0 and 1.0, got $velocitySmoothness"
        }
        require(widthSmoothness in 0.0f..1.0f) {
            "widthSmoothness must be between 0.0 and 1.0, got $widthSmoothness"
        }
        require(minVelocity >= 0.0f) {
            "minVelocity must be >= 0.0, got $minVelocity"
        }
        require(maxVelocity >= 0.0f) {
            "maxVelocity must be >= 0.0, got $maxVelocity"
        }
        require(widthVariation in 0.5f..3.0f) {
            "widthVariation must be between 0.5 and 3.0, got $widthVariation"
        }
        require(inputNoiseThreshold >= 0.0f) {
            "inputNoiseThreshold must be >= 0.0, got $inputNoiseThreshold"
        }
        require(penMinWidth.value > 0) {
            "penMinWidth must be > 0, got ${penMinWidth.value}dp"
        }
        require(penMaxWidth.value > 0) {
            "penMaxWidth must be > 0, got ${penMaxWidth.value}dp"
        }
        require(penMaxWidth >= penMinWidth) {
            "penMaxWidth (${penMaxWidth.value}dp) must be >= penMinWidth (${penMinWidth.value}dp)"
        }
    }

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
         * Pen: nearly uniform with subtle ink accumulation in curves (1.8-2.8dp).
         * Simulates real ballpoint pen behavior: consistent width but slightly thicker
         * in curves and direction changes where ink accumulates.
         * Best for everyday signatures and forms.
         */
        public fun pen(penColor: Color = DefaultPenColor): SignaturePadConfig {
            return SignaturePadConfig(
                penMinWidth = 1.8.dp,
                penMaxWidth = 2.8.dp,
                velocitySmoothness = 0.95f,
                widthSmoothness = 0.8f,
                minVelocity = 0f,
                maxVelocity = 8f,
                widthVariation = 1.2f,
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

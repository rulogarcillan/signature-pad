package com.tuppersoft.signaturepad.rendering
import android.graphics.Canvas
import android.graphics.Paint
import com.tuppersoft.signaturepad.geometry.Bezier
import kotlin.math.ceil
/**
 * Android-specific rendering utilities for Bézier curves.
 *
 * This file contains functions that use Android's Canvas API to render
 * Bézier curves with variable width. These functions are not portable
 * to other platforms without adaptation.
 */
/**
 * Draws a Bézier curve on a canvas with variable width.
 *
 * This function renders a smooth Bézier curve by sampling points along the curve
 * and drawing them with gradually changing stroke width based on velocity.
 *
 * The curve is divided into steps based on its length to ensure smooth rendering.
 * The stroke width transitions smoothly from [startWidth] to [endWidth] using
 * cubic interpolation (t³).
 *
 * The point coordinates are calculated using the cubic Bézier formula:
 * ```
 * B(t) = (1-t)³·P₀ + 3(1-t)²t·P₁ + 3(1-t)t²·P₂ + t³·P₃
 * ```
 * Where:
 * - t is the normalized position along the curve (0.0 to 1.0)
 * - P₀ is the start point
 * - P₁, P₂ are the control points
 * - P₃ is the end point
 *
 * @param canvas The Android canvas to draw on.
 * @param paint The paint configuration (color, caps, joins, etc).
 * @param curve The Bézier curve to draw.
 * @param startWidth Width at the start of the curve in pixels.
 * @param endWidth Width at the end of the curve in pixels.
 */
public fun drawBezierCurve(
    canvas: Canvas,
    paint: Paint,
    curve: Bezier,
    startWidth: Float,
    endWidth: Float
) {
    val originalStrokeWidth: Float = paint.strokeWidth
    val widthChange: Float = endWidth - startWidth
    val numberOfSteps: Int = ceil(x = curve.length()).toInt()
    repeat(times = numberOfSteps) { stepIndex ->
        val t: Float = stepIndex.toFloat() / numberOfSteps
        val tSquared: Float = t * t
        val tCubed: Float = tSquared * t
        val oneMinusT: Float = 1f - t
        val oneMinusTSquared: Float = oneMinusT * oneMinusT
        val oneMinusTCubed: Float = oneMinusTSquared * oneMinusT
        val pointX: Float = oneMinusTCubed * curve.startPoint.x +
            3f * oneMinusTSquared * t * curve.control1.x +
            3f * oneMinusT * tSquared * curve.control2.x +
            tCubed * curve.endPoint.x
        val pointY: Float = oneMinusTCubed * curve.startPoint.y +
            3f * oneMinusTSquared * t * curve.control1.y +
            3f * oneMinusT * tSquared * curve.control2.y +
            tCubed * curve.endPoint.y
        val currentStrokeWidth: Float = startWidth + tCubed * widthChange
        paint.strokeWidth = currentStrokeWidth
        canvas.drawPoint(pointX, pointY, paint)
    }
    paint.strokeWidth = originalStrokeWidth
}

package com.tuppersoft.signaturepad.geometry

import kotlin.math.sqrt

/**
 * Represents a cubic Bézier curve defined by four control points.
 *
 * This class is designed for reuse through the [set] method to avoid unnecessary
 * object allocation during signature drawing. A single instance can be cached and
 * updated with new control points for each curve segment.
 *
 * The cubic Bézier curve is defined by the parametric equation:
 * B(t) = (1-t)³P₀ + 3(1-t)²tP₁ + 3(1-t)t²P₂ + t³P₃, where t ∈ [0,1]
 *
 * All control points are initialized to (0, 0) by default to ensure non-null values.
 *
 * @property startPoint The starting point (P₀) of the curve.
 * @property control1 The first control point (P₁) of the curve.
 * @property control2 The second control point (P₂) of the curve.
 * @property endPoint The ending point (P₃) of the curve.
 */
public class Bezier {
    /**
     * The starting point of the Bézier curve.
     * Initialized to origin (0, 0) by default.
     */

    public var startPoint: TimedPoint = TimedPoint(x = 0f, y = 0f)

    /**
     * The first control point of the Bézier curve.
     * Initialized to origin (0, 0) by default.
     */

    public var control1: TimedPoint = TimedPoint(x = 0f, y = 0f)

    /**
     * The second control point of the Bézier curve.
     * Initialized to origin (0, 0) by default.
     */

    public var control2: TimedPoint = TimedPoint(x = 0f, y = 0f)

    /**
     * The ending point of the Bézier curve.
     * Initialized to origin (0, 0) by default.
     */

    public var endPoint: TimedPoint = TimedPoint(x = 0f, y = 0f)

    /**
     * Sets all four control points of the Bézier curve.
     *
     * This method allows for object reuse by updating the control points
     * of an existing instance rather than creating a new one.
     *
     * @param startPoint The starting point of the curve.
     * @param control1 The first control point of the curve.
     * @param control2 The second control point of the curve.
     * @param endPoint The ending point of the curve.
     * @return This Bezier instance for method chaining.
     */
    public fun set(
        startPoint: TimedPoint,
        control1: TimedPoint,
        control2: TimedPoint,
        endPoint: TimedPoint
    ): Bezier {
        this.startPoint = startPoint
        this.control1 = control1
        this.control2 = control2
        this.endPoint = endPoint
        return this
    }

    /**
     * Calculates the approximate length of the Bézier curve.
     *
     * The length is approximated by dividing the curve into 10 line segments
     * and summing their Euclidean distances. This provides a good balance
     * between accuracy and performance for real-time drawing.
     *
     * @return The approximate length of the curve in pixels.
     */
    public fun length(): Float {
        val steps = 10
        var length = 0f
        var px = 0.0
        var py = 0.0

        for (i in 0..steps) {
            val t: Float = i.toFloat() / steps
            val cx: Double =
                point(t = t, start = startPoint.x, c1 = control1.x, c2 = control2.x, end = endPoint.x)
            val cy: Double =
                point(t = t, start = startPoint.y, c1 = control1.y, c2 = control2.y, end = endPoint.y)

            if (i > 0) {
                val xDiff: Double = cx - px
                val yDiff: Double = cy - py
                length += sqrt(x = xDiff * xDiff + yDiff * yDiff).toFloat()
            }
            px = cx
            py = cy
        }
        return length
    }

    /**
     * Calculates a point on the Bézier curve at parameter t.
     *
     * Uses the cubic Bézier formula:
     * B(t) = (1-t)³·start + 3(1-t)²t·c1 + 3(1-t)t²·c2 + t³·end
     *
     * This method is private as it's only used internally by [length].
     * External users should use [set] and [length] for curve manipulation.
     *
     * @param t The parameter value, typically in range [0,1] where:
     *          - t=0 returns the start point
     *          - t=1 returns the end point
     *          - 0<t<1 returns points along the curve
     * @param start The starting coordinate value.
     * @param c1 The first control point coordinate value.
     * @param c2 The second control point coordinate value.
     * @param end The ending coordinate value.
     * @return The calculated coordinate value at parameter t.
     */
    private fun point(t: Float, start: Float, c1: Float, c2: Float, end: Float): Double {
        val oneMinusT: Double = 1.0 - t
        return start * oneMinusT * oneMinusT * oneMinusT +
            3.0 * c1 * oneMinusT * oneMinusT * t +
            3.0 * c2 * oneMinusT * t * t +
            end * t * t * t
    }
}

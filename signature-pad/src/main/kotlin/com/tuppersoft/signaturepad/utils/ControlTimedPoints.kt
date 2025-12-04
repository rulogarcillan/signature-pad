package com.tuppersoft.signaturepad.utils

/**
 * Holds a pair of control points for a Bézier curve segment.
 *
 * This class is designed for reuse through the [set] method to avoid unnecessary
 * object allocation during signature drawing. A single instance can be cached and
 * updated with new control points for each curve segment calculation.
 *
 * All control points are initialized to (0, 0) by default to ensure non-null values.
 *
 * @property c1 The first control point of the curve segment.
 * @property c2 The second control point of the curve segment.
 */
public class ControlTimedPoints {
    /**
     * The first control point.
     * Initialized to origin (0, 0) by default.
     */

    public var c1: TimedPoint = TimedPoint(x = 0f, y = 0f)

    /**
     * The second control point.
     * Initialized to origin (0, 0) by default.
     */

    public var c2: TimedPoint = TimedPoint(x = 0f, y = 0f)

    /**
     * Sets both control points.
     *
     * This method allows for object reuse by updating the control points
     * of an existing instance rather than creating a new one.
     *
     * @param c1 The first control point.
     * @param c2 The second control point.
     * @return This ControlTimedPoints instance for method chaining.
     */
    public fun set(c1: TimedPoint, c2: TimedPoint): ControlTimedPoints {
        this.c1 = c1
        this.c2 = c2
        return this
    }
}

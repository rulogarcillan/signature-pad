package com.tuppersoft.signaturepad.geometry
import kotlin.math.sqrt
/**
 * Mathematical utilities for Bézier curve calculations.
 *
 * This file contains pure mathematical functions for calculating control points
 * and other Bézier curve operations. All functions are portable to Kotlin Multiplatform.
 */
/**
 * Calculates control points for a smooth Bézier curve using Catmull-Rom interpolation.
 *
 * This function generates control points (P₁, P₂) for a cubic Bézier curve that
 * smoothly interpolates three consecutive points in a stroke. The resulting curve
 * passes through the middle point (s2) with continuous first derivative (C1 continuity).
 *
 * The algorithm is based on Catmull-Rom splines, which ensure smooth transitions
 * between consecutive curve segments by:
 * 1. Calculating midpoints between consecutive points
 * 2. Weighting by segment lengths for adaptive smoothing
 * 3. Computing tangent-preserving control points
 *
 * Mathematical foundation:
 * - Uses centripetal parameterization (weighted by distance)
 * - Preserves tangent direction at connection points
 * - Avoids overshooting and self-intersections
 *
 * @param s1 The previous point in the stroke.
 * @param s2 The current point (the curve will pass through this point).
 * @param s3 The next point in the stroke.
 * @param cache A reusable [ControlTimedPoints] object to avoid allocations.
 *              The function will update this object with the new control points.
 * @return The [cache] object with updated control points (c1, c2).
 *
 * @see ControlTimedPoints
 * @see Bezier
 */
public fun calculateControlPoints(
    s1: TimedPoint,
    s2: TimedPoint,
    s3: TimedPoint,
    cache: ControlTimedPoints
): ControlTimedPoints {
    // Calculate vectors between consecutive points
    val dx1: Float = s1.x - s2.x
    val dy1: Float = s1.y - s2.y
    val dx2: Float = s2.x - s3.x
    val dy2: Float = s2.y - s3.y
    // Calculate midpoints of segments
    val m1X: Float = (s1.x + s2.x) / 2f
    val m1Y: Float = (s1.y + s2.y) / 2f
    val m2X: Float = (s2.x + s3.x) / 2f
    val m2Y: Float = (s2.y + s3.y) / 2f
    // Calculate segment lengths for weighting
    val l1: Float = sqrt(x = dx1 * dx1 + dy1 * dy1)
    val l2: Float = sqrt(x = dx2 * dx2 + dy2 * dy2)
    // Calculate vectors between midpoints
    val dxm: Float = m1X - m2X
    val dym: Float = m1Y - m2Y
    // Weight factor based on segment length ratio
    // This ensures that short and long segments blend smoothly
    var k: Float = l2 / (l1 + l2)
    if (k.isNaN()) k = 0f
    // Calculate weighted center point
    val cmX: Float = m2X + dxm * k
    val cmY: Float = m2Y + dym * k
    // Calculate translation to make tangent pass through s2
    val tx: Float = s2.x - cmX
    val ty: Float = s2.y - cmY
    // Generate control points by translating midpoints
    return cache.set(
        c1 = TimedPoint(x = m1X + tx, y = m1Y + ty),
        c2 = TimedPoint(x = m2X + tx, y = m2Y + ty)
    )
}

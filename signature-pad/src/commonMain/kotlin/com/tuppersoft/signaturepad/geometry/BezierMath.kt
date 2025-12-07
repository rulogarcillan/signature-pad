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
    // Calculate direction vectors between consecutive points
    val deltaX1 = s1.x - s2.x
    val deltaY1 = s1.y - s2.y
    val deltaX2 = s2.x - s3.x
    val deltaY2 = s2.y - s3.y

    // Calculate midpoints of each segment
    val midpoint1X = (s1.x + s2.x) / 2f
    val midpoint1Y = (s1.y + s2.y) / 2f
    val midpoint2X = (s2.x + s3.x) / 2f
    val midpoint2Y = (s2.y + s3.y) / 2f

    // Calculate length of each segment for adaptive weighting
    val length1 = sqrt(deltaX1 * deltaX1 + deltaY1 * deltaY1)
    val length2 = sqrt(deltaX2 * deltaX2 + deltaY2 * deltaY2)

    // Calculate vector between the two midpoints
    val midpointsDeltaX = midpoint1X - midpoint2X
    val midpointsDeltaY = midpoint1Y - midpoint2Y

    // Calculate weight factor based on segment length ratio
    // This ensures smooth blending between short and long segments
    val totalLength = length1 + length2
    val weight = if (totalLength > 0f) {
        length2 / totalLength
    } else {
        0f
    }

    // Calculate weighted center point
    val centerX = midpoint2X + midpointsDeltaX * weight
    val centerY = midpoint2Y + midpointsDeltaY * weight

    // Calculate translation vector to align tangent through s2
    val translationX = s2.x - centerX
    val translationY = s2.y - centerY

    // Generate control points by applying translation to midpoints
    return cache.set(
        c1 = TimedPoint(x = midpoint1X + translationX, y = midpoint1Y + translationY),
        c2 = TimedPoint(x = midpoint2X + translationX, y = midpoint2Y + translationY)
    )
}

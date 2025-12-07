package com.tuppersoft.signaturepad.geometry

import kotlin.time.Clock
import kotlin.math.sqrt
import kotlin.time.ExperimentalTime

/**
 * Represents a point in time with x, y coordinates and an automatically captured timestamp.
 *
 * This class is used to track individual points during signature capture,
 * allowing for velocity and distance calculations between consecutive points.
 *
 * The timestamp is captured automatically when the point is created and cannot be overridden,
 * ensuring accurate velocity calculations based on the actual moment of creation.
 *
 * @property x The X coordinate of the point in pixels.
 * @property y The Y coordinate of the point in pixels.
 */
public data class TimedPoint(
    val x: Float,
    val y: Float
) {
    /**
     * Timestamp in milliseconds since Unix epoch when this point was created.
     *
     * This value is captured automatically and is immutable. It represents the exact
     * moment this point was instantiated, which is essential for calculating drawing
     * velocity between consecutive points.
     */
    @OptIn(ExperimentalTime::class)
    private val timestamp: Long = Clock.System.now().toEpochMilliseconds()

    /**
     * Calculates the velocity from a start point to this point.
     *
     * The velocity is calculated as the Euclidean distance divided by the elapsed time.
     * If the result is infinite, NaN, or the time difference is <= 0, returns 0.
     *
     * @param start The initial point from which to calculate velocity.
     * @return The velocity in pixels per millisecond. Returns 0 if the calculation is invalid.
     */
    public fun velocityFrom(start: TimedPoint): Float {
        var diff: Long = this.timestamp - start.timestamp
        if (diff <= 0) {
            diff = 1
        }
        var velocity: Float = distanceTo(point = start) / diff
        if (velocity.isInfinite() || velocity.isNaN()) {
            velocity = 0f
        }
        return velocity
    }

    /**
     * Calculates the Euclidean distance between this point and another point.
     *
     * Uses the Pythagorean theorem: √((x₂-x₁)² + (y₂-y₁)²)
     *
     * This method is private as it's only used internally for velocity calculation.
     *
     * @param point The destination point to calculate the distance to.
     * @return The distance in pixels between the two points.
     */
    private fun distanceTo(point: TimedPoint): Float {
        val dx: Float = point.x - this.x
        val dy: Float = point.y - this.y
        return sqrt(x = (dx * dx + dy * dy).toDouble()).toFloat()
    }
}

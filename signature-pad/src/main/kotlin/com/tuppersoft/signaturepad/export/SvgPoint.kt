package com.tuppersoft.signaturepad.export

import com.tuppersoft.signaturepad.geometry.TimedPoint

/**
 * Represents a point as it would be in the generated SVG document.
 *
 * This class stores integer coordinates optimized for SVG generation,
 * where decimal precision is typically not significant.
 *
 * @property x The X coordinate in the SVG coordinate system.
 * @property y The Y coordinate in the SVG coordinate system.
 */
public data class SvgPoint(
    val x: Int,
    val y: Int
) {
    /**
     * Creates an SvgPoint from a TimedPoint.
     *
     * Rounds the floating-point coordinates to integers, as decimals are
     * mostly non-significant in the produced SVG image.
     *
     * @param point The TimedPoint to convert to SVG coordinates.
     */
    public constructor(point: TimedPoint) : this(
        x = Math.round(point.x),
        y = Math.round(point.y)
    )

    /**
     * Converts this point to absolute SVG coordinates.
     *
     * This method is private as external users should use [toString] which calls this internally.
     *
     * @return A string representation in the format "x,y".
     */
    private fun toAbsoluteCoordinates(): String {
        return buildString {
            append(x)
            append(",")
            append(y)
        }
    }

    /**
     * Converts this point to relative SVG coordinates based on a reference point.
     *
     * Calculates the offset from the reference point and returns it as a string.
     *
     * @param referencePoint The point to calculate relative coordinates from.
     * @return A string representation of the relative coordinates in the format "dx,dy".
     */
    public fun toRelativeCoordinates(referencePoint: SvgPoint): String {
        return SvgPoint(x = x - referencePoint.x, y = y - referencePoint.y).toString()
    }

    /**
     * Returns the string representation of this point.
     *
     * @return The absolute coordinates as a string in the format "x,y".
     */
    public override fun toString(): String {
        return toAbsoluteCoordinates()
    }
}

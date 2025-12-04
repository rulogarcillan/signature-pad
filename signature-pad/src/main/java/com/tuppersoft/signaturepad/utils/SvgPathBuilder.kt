package com.tuppersoft.signaturepad.utils

/**
 * Builds an SVG path element as a string.
 *
 * This class constructs a single SVG `<path>` element representing a continuous stroke
 * with a specific stroke width. It uses cubic Bézier curves to create smooth paths.
 *
 * Reference: [SVG Tiny 1.2 Paths Specification](https://www.w3.org/TR/SVGTiny12/paths.html)
 *
 * @property startPoint The starting point of the path.
 * @property strokeWidth The stroke width in pixels.
 */
public class SvgPathBuilder(
    private val startPoint: SvgPoint,
    public val strokeWidth: Int
) {
    /**
     * The last point added to the path.
     *
     * Used internally for calculating relative coordinates.
     * Public read access for comparison in [SvgBuilder.append].
     */
    public var lastPoint: SvgPoint = startPoint
        private set

    private val stringBuilder: StringBuilder = StringBuilder().apply {
        append(SVG_RELATIVE_CUBIC_BEZIER_CURVE)
    }

    /**
     * Companion object containing SVG command constants.
     */
    public companion object {
        /**
         * SVG command for relative cubic Bézier curve.
         */
        public const val SVG_RELATIVE_CUBIC_BEZIER_CURVE: Char = 'c'

        /**
         * SVG command for move to absolute position.
         */
        public const val SVG_MOVE: Char = 'M'
    }

    /**
     * Appends a cubic Bézier curve to the path.
     *
     * @param controlPoint1 The first control point of the curve.
     * @param controlPoint2 The second control point of the curve.
     * @param endPoint The end point of the curve.
     * @return This SvgPathBuilder instance for method chaining.
     */
    public fun append(
        controlPoint1: SvgPoint,
        controlPoint2: SvgPoint,
        endPoint: SvgPoint
    ): SvgPathBuilder {
        stringBuilder.append(
            makeRelativeCubicBezierCurve(
                controlPoint1 = controlPoint1,
                controlPoint2 = controlPoint2,
                endPoint = endPoint
            )
        )
        lastPoint = endPoint
        return this
    }

    /**
     * Converts the path to an SVG `<path>` element string.
     *
     * @return The complete SVG path element as a string.
     */
    public override fun toString(): String = buildString {
        append("<path ")
        append("stroke-width=\"$strokeWidth\" ")
        append("d=\"$SVG_MOVE$startPoint$stringBuilder\"/>")
    }

    /**
     * Creates a relative cubic Bézier curve string.
     *
     * Converts absolute coordinates to relative coordinates based on [lastPoint].
     * Discards zero curves (where all coordinates are 0,0 0,0 0,0).
     *
     * @param controlPoint1 The first control point.
     * @param controlPoint2 The second control point.
     * @param endPoint The end point.
     * @return The relative curve string, or empty string if it's a zero curve.
     */
    private fun makeRelativeCubicBezierCurve(
        controlPoint1: SvgPoint,
        controlPoint2: SvgPoint,
        endPoint: SvgPoint
    ): String {
        val svg: String = buildString {
            append(controlPoint1.toRelativeCoordinates(referencePoint = lastPoint))
            append(" ")
            append(controlPoint2.toRelativeCoordinates(referencePoint = lastPoint))
            append(" ")
            append(endPoint.toRelativeCoordinates(referencePoint = lastPoint))
            append(" ")
        }

        // Discard zero curve to optimize SVG size
        return if (svg == "0,0 0,0 0,0 ") "" else svg
    }
}

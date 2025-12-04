package com.tuppersoft.signaturepad.export

import com.tuppersoft.signaturepad.geometry.Bezier

/**
 * Builds a complete SVG document from multiple paths.
 *
 * This class manages the construction of an SVG document by accumulating multiple
 * path elements. It automatically creates new paths when the stroke width changes.
 *
 * The generated SVG follows the SVG Tiny 1.2 specification and can be used for:
 * - Exporting signatures as scalable vector graphics
 * - Sharing signatures across platforms
 * - Printing signatures in high resolution
 *
 * Example usage:
 * ```
 * val builder = SvgBuilder()
 * builder.append(curve = curve1, strokeWidth = width1)
 * builder.append(curve = curve2, strokeWidth = width2)
 * val svg = builder.build(width = 800, height = 600)
 * ```
 */
public class SvgBuilder {
    private val svgPathsBuilder = StringBuilder()
    private var currentPathBuilder: SvgPathBuilder? = null

    /**
     * Clears all accumulated paths and resets the builder state.
     */
    public fun clear() {
        svgPathsBuilder.setLength(0)
        currentPathBuilder = null
    }

    /**
     * Builds the complete SVG document.
     *
     * Generates a well-formed SVG XML document with all accumulated paths.
     * The SVG uses black stroke color, round line caps and joins, and no fill.
     *
     * @param width The width of the SVG viewport in pixels.
     * @param height The height of the SVG viewport in pixels.
     * @return The complete SVG document as a string.
     */
    public fun build(width: Int, height: Int): String {
        if (isPathStarted()) {
            appendCurrentPath()
        }

        return buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n")
            append("<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.2\" baseProfile=\"tiny\" ")
            append("height=\"$height\" ")
            append("width=\"$width\" ")
            append("viewBox=\"0 0 $width $height\">")
            append("<g stroke-linejoin=\"round\" stroke-linecap=\"round\" fill=\"none\" stroke=\"black\">")
            append(svgPathsBuilder)
            append("</g>")
            append("</svg>")
        }
    }

    /**
     * Appends a Bézier curve to the SVG document.
     *
     * If the stroke width changes or this is the first curve, a new path is started.
     * Otherwise, the curve is appended to the current path.
     *
     * @param curve The Bézier curve to append.
     * @param strokeWidth The stroke width for this curve.
     * @return This SvgBuilder instance for method chaining.
     */
    public fun append(curve: Bezier, strokeWidth: Float): SvgBuilder {
        val roundedStrokeWidth: Int = Math.round(strokeWidth)
        val curveStartSvgPoint = SvgPoint(point = curve.startPoint)
        val curveControlSvgPoint1 = SvgPoint(point = curve.control1)
        val curveControlSvgPoint2 = SvgPoint(point = curve.control2)
        val curveEndSvgPoint = SvgPoint(point = curve.endPoint)

        if (!isPathStarted()) {
            startNewPath(roundedStrokeWidth = roundedStrokeWidth, curveStartSvgPoint = curveStartSvgPoint)
        }

        currentPathBuilder?.let { builder: SvgPathBuilder ->
            if (curveStartSvgPoint != builder.lastPoint ||
                roundedStrokeWidth != builder.strokeWidth
            ) {
                appendCurrentPath()
                startNewPath(roundedStrokeWidth = roundedStrokeWidth, curveStartSvgPoint = curveStartSvgPoint)
            }
        }

        currentPathBuilder?.append(
            controlPoint1 = curveControlSvgPoint1,
            controlPoint2 = curveControlSvgPoint2,
            endPoint = curveEndSvgPoint
        )
        return this
    }

    /**
     * Starts a new path with the given stroke width and start point.
     */
    private fun startNewPath(roundedStrokeWidth: Int, curveStartSvgPoint: SvgPoint) {
        currentPathBuilder = SvgPathBuilder(
            startPoint = curveStartSvgPoint,
            strokeWidth = roundedStrokeWidth
        )
    }

    /**
     * Appends the current path to the accumulated paths.
     */
    private fun appendCurrentPath() {
        svgPathsBuilder.append(currentPathBuilder)
    }

    /**
     * Checks if a path has been started.
     */
    private fun isPathStarted(): Boolean = currentPathBuilder != null
}

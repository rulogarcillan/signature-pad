package com.tuppersoft.signaturepad.export

import android.graphics.Canvas
import android.graphics.Paint
import androidx.annotation.Px
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.createBitmap
import com.tuppersoft.signaturepad.compose.Stroke
import com.tuppersoft.signaturepad.rendering.drawBezierCurve

/**
 * Exports signature strokes to various formats (Bitmap, SVG).
 *
 * This class is responsible for converting signature strokes into
 * different export formats without coupling the state management
 * logic to rendering and export operations.
 *
 * Handles:
 * - Bitmap export with white or transparent background
 * - Optional cropping with padding
 * - SVG export
 */
internal class SignatureExporter {

    private val svgBuilder by lazy { SvgBuilder() }

    /**
     * Exports strokes to a Bitmap with white background.
     */
    fun toBitmap(
        strokes: List<Stroke>,
        size: IntSize,
        penColor: Color,
        crop: Boolean = false,
        @Px paddingCrop: Int = 0
    ): android.graphics.Bitmap {
        val fullBitmap = createBitmap(size.width, size.height)
        val canvas = Canvas(fullBitmap)

        canvas.drawColor(android.graphics.Color.WHITE)
        renderStrokesToCanvas(canvas, strokes, penColor)

        return if (crop && strokes.isNotEmpty()) {
            cropBitmap(fullBitmap, strokes, paddingCrop, android.graphics.Color.WHITE)
        } else {
            fullBitmap
        }
    }

    /**
     * Exports strokes to a Bitmap with transparent background.
     */
    fun toTransparentBitmap(
        strokes: List<Stroke>,
        size: IntSize,
        penColor: Color,
        crop: Boolean = false,
        @Px paddingCrop: Int = 0
    ): android.graphics.Bitmap {
        val fullBitmap = createBitmap(size.width, size.height)
        val canvas = Canvas(fullBitmap)

        renderStrokesToCanvas(canvas, strokes, penColor)

        return if (crop && strokes.isNotEmpty()) {
            cropBitmap(fullBitmap, strokes, paddingCrop, android.graphics.Color.TRANSPARENT)
        } else {
            fullBitmap
        }
    }

    /**
     * Exports strokes to SVG format.
     */
    fun toSvg(
        strokes: List<Stroke>,
        size: IntSize
    ): String {
        svgBuilder.clear()

        strokes.forEach { stroke ->
            stroke.curves.forEach { curve ->
                svgBuilder.append(
                    curve = curve.bezier,
                    strokeWidth = (curve.startWidth + curve.endWidth) / 2f
                )
            }
        }

        return svgBuilder.build(width = size.width, height = size.height)
    }

    private fun renderStrokesToCanvas(
        canvas: Canvas,
        strokes: List<Stroke>,
        penColor: Color
    ) {
        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = penColor.toArgb()
        }

        strokes.forEach { stroke ->
            stroke.curves.forEach { curve ->
                drawBezierCurve(
                    canvas = canvas,
                    paint = paint,
                    curve = curve.bezier,
                    startWidth = curve.startWidth,
                    endWidth = curve.endWidth
                )
            }
        }
    }

    private fun calculateSignatureBounds(strokes: List<Stroke>): android.graphics.RectF? {
        if (strokes.isEmpty()) return null

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        strokes.forEach { stroke ->
            stroke.curves.forEach { curve ->
                val maxStrokeWidth = maxOf(curve.startWidth, curve.endWidth)

                listOf(
                    curve.bezier.startPoint,
                    curve.bezier.control1,
                    curve.bezier.control2,
                    curve.bezier.endPoint
                ).forEach { point ->
                    minX = minOf(minX, point.x - maxStrokeWidth / 2)
                    minY = minOf(minY, point.y - maxStrokeWidth / 2)
                    maxX = maxOf(maxX, point.x + maxStrokeWidth / 2)
                    maxY = maxOf(maxY, point.y + maxStrokeWidth / 2)
                }
            }
        }

        return android.graphics.RectF(minX, minY, maxX, maxY)
    }

    private fun cropBitmap(
        source: android.graphics.Bitmap,
        strokes: List<Stroke>,
        @Px padding: Int,
        backgroundColor: Int
    ): android.graphics.Bitmap {
        return calculateSignatureBounds(strokes)?.let { bounds ->
            val left = (bounds.left - padding).toInt().coerceAtLeast(0)
            val top = (bounds.top - padding).toInt().coerceAtLeast(0)
            val right = (bounds.right + padding).toInt().coerceAtMost(source.width)
            val bottom = (bounds.bottom + padding).toInt().coerceAtMost(source.height)

            val width = right - left
            val height = bottom - top

            if (width > 0 && height > 0) {
                val croppedBitmap = createBitmap(width, height)
                val canvas = Canvas(croppedBitmap)

                canvas.drawColor(backgroundColor)

                val srcRect = android.graphics.Rect(left, top, right, bottom)
                val dstRect = android.graphics.Rect(0, 0, width, height)
                canvas.drawBitmap(source, srcRect, dstRect, null)

                croppedBitmap
            } else {
                source
            }
        } ?: source
    }
}

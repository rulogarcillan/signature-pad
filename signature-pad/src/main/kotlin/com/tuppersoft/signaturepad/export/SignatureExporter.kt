package com.tuppersoft.signaturepad.export

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.unit.IntSize
import com.tuppersoft.signaturepad.compose.Stroke
import com.tuppersoft.signaturepad.rendering.drawBezierCurve
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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
        paddingCrop: Int = 0
    ): ImageBitmap {
        return renderBitmap(
            strokes = strokes,
            size = size,
            penColor = penColor,
            backgroundColor = Color.White,
            crop = crop,
            paddingCrop = paddingCrop
        )
    }

    /**
     * Exports strokes to a Bitmap with transparent background.
     */
    fun toTransparentBitmap(
        strokes: List<Stroke>,
        size: IntSize,
        penColor: Color,
        crop: Boolean = false,
        paddingCrop: Int = 0
    ): ImageBitmap {
        return renderBitmap(
            strokes = strokes,
            size = size,
            penColor = penColor,
            backgroundColor = Color.Transparent,
            crop = crop,
            paddingCrop = paddingCrop
        )
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

    private fun renderBitmap(
        strokes: List<Stroke>,
        size: IntSize,
        penColor: Color,
        backgroundColor: Color,
        crop: Boolean,
        paddingCrop: Int
    ): ImageBitmap {
        // 1. Calculate the effective bounds (either full size or cropped content)
        val bounds: Rect = if (crop && strokes.isNotEmpty()) {
            calculateSignatureBounds(strokes, paddingCrop, size)
        } else {
            Rect(0f, 0f, size.width.toFloat(), size.height.toFloat())
        }

        // 2. Safely determine dimensions (avoid <= 0)
        val width = max(1, bounds.width.roundToInt())
        val height = max(1, bounds.height.roundToInt())

        // 3. Create the target bitmap
        val imageBitmap = ImageBitmap(width, height, ImageBitmapConfig.Argb8888)
        val canvas = Canvas(imageBitmap)

        // 4. Draw Background
        val bgPaint = Paint().apply { color = backgroundColor }
        canvas.drawRect(
            left = 0f,
            top = 0f,
            right = width.toFloat(),
            bottom = height.toFloat(),
            paint = bgPaint
        )

        // 5. Apply translation if we are cropping (moving the origin)
        canvas.save()
        if (crop) {
            canvas.translate(-bounds.left, -bounds.top)
        }

        // 6. Reuse rendering logic
        renderStrokesToCanvas(canvas, strokes, penColor)

        canvas.restore()

        return imageBitmap
    }

    private fun renderStrokesToCanvas(
        canvas: Canvas,
        strokes: List<Stroke>,
        penColor: Color
    ) {
        val paint = Paint().apply {
            isAntiAlias = true
            style = PaintingStyle.Stroke
            strokeCap = StrokeCap.Round
            strokeJoin = StrokeJoin.Round
            color = penColor
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

    private fun calculateSignatureBounds(
        strokes: List<Stroke>,
        padding: Int,
        maxSize: IntSize
    ): Rect {
        if (strokes.isEmpty()) return Rect.Zero

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        strokes.forEach { stroke ->
            stroke.curves.forEach { curve ->
                val maxStrokeWidth = max(curve.startWidth, curve.endWidth)

                listOf(
                    curve.bezier.startPoint,
                    curve.bezier.control1,
                    curve.bezier.control2,
                    curve.bezier.endPoint
                ).forEach { point ->
                    minX = min(minX, point.x - maxStrokeWidth / 2)
                    minY = min(minY, point.y - maxStrokeWidth / 2)
                    maxX = max(maxX, point.x + maxStrokeWidth / 2)
                    maxY = max(maxY, point.y + maxStrokeWidth / 2)
                }
            }
        }

        // Apply padding and clamp to max size
        val left = (minX - padding).coerceAtLeast(0f)
        val top = (minY - padding).coerceAtLeast(0f)
        val right = (maxX + padding).coerceAtMost(maxSize.width.toFloat())
        val bottom = (maxY + padding).coerceAtMost(maxSize.height.toFloat())

        return Rect(left, top, right, bottom)
    }
}

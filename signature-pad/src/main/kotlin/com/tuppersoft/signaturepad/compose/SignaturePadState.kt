package com.tuppersoft.signaturepad.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.createBitmap
import com.tuppersoft.signaturepad.compose.SignaturePadConfig.Companion.fountainPen
import com.tuppersoft.signaturepad.export.SvgBuilder
import com.tuppersoft.signaturepad.geometry.Bezier
import com.tuppersoft.signaturepad.geometry.TimedPoint
import com.tuppersoft.signaturepad.rendering.drawBezierCurve

/**
 * State holder for [SignaturePad] composable.
 *
 * Manages the mutable state of a signature pad:
 * - Stroke history for UNDO/REDO
 * - Current drawing state
 * - Internal drawing calculations
 *
 * Configuration is managed by the composable.
 *
 * Example:
 * ```
 * val state = rememberSignaturePadState()
 * SignaturePad(
 *     state = state,
 *     config = SignaturePadConfig.fountainPen(penColor = Color.Blue)
 * )
 * ```
 */
@Stable
public class SignaturePadState {

    // ========================================
    // Configuration
    // ========================================

    /** Current configuration (updated by SignaturePad composable). */
    internal var config: SignaturePadConfig by mutableStateOf(fountainPen())

    // ========================================
    // Public State
    // ========================================

    /** Whether the signature is empty (no strokes drawn). */
    public val isEmpty: Boolean by derivedStateOf { _strokes.isEmpty() }

    /** Size of the SignaturePad layout in pixels. */
    public var layoutSize: IntSize by mutableStateOf(IntSize.Zero)
        private set

    // ========================================
    // Internal State
    // ========================================

    // Stroke collections
    private val _strokes = mutableStateListOf<Stroke>()
    private val _undoneStrokes = mutableStateListOf<Stroke>()
    private val _currentPoints = mutableStateListOf<TimedPoint>()
    private val _currentCurves = mutableStateListOf<StrokeCurve>()

    internal val strokes: List<Stroke> = _strokes
    internal val currentPoints: List<TimedPoint> = _currentPoints
    internal val currentCurves: List<StrokeCurve> = _currentCurves

    // Drawing state
    private var _lastVelocity by mutableFloatStateOf(0f)
    private var _lastWidth by mutableFloatStateOf(0f)

    internal val lastVelocity: Float get() = _lastVelocity
    internal val lastWidth: Float get() = _lastWidth

    // Export builder (lazy)
    private val svgBuilder by lazy { SvgBuilder() }

    // ========================================
    // Public Operations
    // ========================================

    /**
     * Checks if an UNDO operation is possible.
     */
    public fun canUndo(): Boolean = _strokes.isNotEmpty()

    /**
     * Checks if a REDO operation is possible.
     */
    public fun canRedo(): Boolean = _undoneStrokes.isNotEmpty()

    /**
     * Undoes the last stroke.
     * @return `true` if successful.
     */
    public fun undo(): Boolean {
        if (!canUndo()) return false
        val lastStroke = _strokes.removeAt(_strokes.lastIndex)
        _undoneStrokes.add(lastStroke)
        return true
    }

    /**
     * Redoes the last undone stroke.
     * @return `true` if successful.
     */
    public fun redo(): Boolean {
        if (!canRedo()) return false
        val stroke = _undoneStrokes.removeAt(_undoneStrokes.lastIndex)
        _strokes.add(stroke)
        return true
    }

    /** Clears all strokes and resets the state. */
    public fun clear() {
        _strokes.clear()
        _undoneStrokes.clear()
        _currentPoints.clear()
        _currentCurves.clear()
        _lastVelocity = 0f
        _lastWidth = 0f
    }

    // ========================================
    // Export Operations
    // ========================================

    /**
     * Exports the signature as an SVG string.
     * Uses [layoutSize] for viewport dimensions.
     */
    public fun toSvg(): String {
        svgBuilder.clear()
        _strokes.forEach { stroke ->
            stroke.curves.forEach { curve ->
                svgBuilder.append(
                    curve = curve.bezier,
                    strokeWidth = (curve.startWidth + curve.endWidth) / 2f
                )
            }
        }
        return svgBuilder.build(width = layoutSize.width, height = layoutSize.height)
    }

    /** Exports the signature as a Bitmap with white background. */
    public fun toBitmap(): android.graphics.Bitmap {
        val bitmap = createBitmap(layoutSize.width, layoutSize.height)
        val canvas = android.graphics.Canvas(bitmap)

        canvas.drawColor(android.graphics.Color.WHITE)
        renderStrokesToCanvas(canvas)

        return bitmap
    }

    /** Exports the signature as a Bitmap with transparent background. */
    public fun toTransparentBitmap(): android.graphics.Bitmap {
        val bitmap = createBitmap(layoutSize.width, layoutSize.height)
        val canvas = android.graphics.Canvas(bitmap)

        renderStrokesToCanvas(canvas)

        return bitmap
    }

    // ========================================
    // Internal Operations
    // ========================================

    internal fun updateLayoutSize(size: IntSize) {
        layoutSize = size
    }

    internal fun updateVelocity(value: Float) {
        _lastVelocity = value
    }

    internal fun updateWidth(value: Float) {
        _lastWidth = value
    }

    internal fun addStroke(stroke: Stroke) {
        _strokes.add(stroke)
        _undoneStrokes.clear()
    }

    internal fun addCurrentPoint(point: TimedPoint) {
        _currentPoints.add(point)
    }

    internal fun removeFirstCurrentPoint() {
        if (_currentPoints.isNotEmpty()) {
            _currentPoints.removeAt(0)
        }
    }

    internal fun addCurrentCurve(curve: StrokeCurve) {
        _currentCurves.add(curve)
    }

    internal fun clearCurrentStroke() {
        _currentPoints.clear()
        _currentCurves.clear()
    }

    // ========================================
    // Private Helpers
    // ========================================

    private fun renderStrokesToCanvas(canvas: android.graphics.Canvas) {
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
            color = config.penColor.toArgb()
        }

        _strokes.forEach { stroke ->
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
}

/**
 * Represents a single stroke (continuous line drawn without lifting).
 * A stroke consists of one or more Bézier curves.
 */
@Immutable
internal data class Stroke(
    val curves: List<StrokeCurve>
)

/**
 * Represents a single Bézier curve segment within a stroke.
 * Width varies based on drawing velocity for natural appearance.
 */
@Immutable
internal data class StrokeCurve(
    val bezier: Bezier,
    val startWidth: Float,
    val endWidth: Float
)

/**
 * Remembers and creates a [SignaturePadState] instance.
 */
@Composable
public fun rememberSignaturePadState(): SignaturePadState {
    return remember { SignaturePadState() }
}

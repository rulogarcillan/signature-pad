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
import androidx.compose.ui.unit.IntSize
import com.tuppersoft.signaturepad.compose.SignaturePadConfig.Companion.fountainPen
import com.tuppersoft.signaturepad.export.SignatureExporter
import com.tuppersoft.signaturepad.geometry.Bezier
import com.tuppersoft.signaturepad.geometry.TimedPoint

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
    // Private Properties
    // ========================================

    private val _strokes = mutableStateListOf<Stroke>()
    private val _undoneStrokes = mutableStateListOf<Stroke>()
    private val _currentPoints = mutableStateListOf<TimedPoint>()
    private val _currentCurves = mutableStateListOf<StrokeCurve>()

    private var _lastVelocity by mutableFloatStateOf(0f)
    private var _lastWidth by mutableFloatStateOf(0f)

    private var layoutSize: IntSize by mutableStateOf(IntSize.Zero)

    private val exporter by lazy { SignatureExporter() }

    // ========================================
    // Public Properties
    // ========================================

    /** Whether the signature is empty (no strokes drawn). */
    public val isEmpty: Boolean by derivedStateOf { _strokes.isEmpty() }

    // ========================================
    // Internal Properties
    // ========================================

    /** Current configuration (updated by SignaturePad composable). */
    internal var config: SignaturePadConfig by mutableStateOf(fountainPen())

    internal val strokes: List<Stroke> = _strokes
    internal val currentPoints: List<TimedPoint> = _currentPoints
    internal val currentCurves: List<StrokeCurve> = _currentCurves

    internal val lastVelocity: Float get() = _lastVelocity
    internal val lastWidth: Float get() = _lastWidth

    // ========================================
    // Public Functions
    // ========================================

    /**
     * Whether an UNDO operation can be performed.
     */
    public fun canUndo(): Boolean = _strokes.isNotEmpty()

    /**
     * Whether a REDO operation can be performed.
     */
    public fun canRedo(): Boolean = _undoneStrokes.isNotEmpty()

    /**
     * Performs an UNDO operation.
     * @return True if successful, false if no strokes to undo.
     */
    public fun undo(): Boolean {
        if (!canUndo()) return false
        val lastStroke = _strokes.removeAt(_strokes.lastIndex)
        _undoneStrokes.add(lastStroke)
        return true
    }

    /**
     * Performs a REDO operation.
     * @return True if successful, false if no strokes to redo.
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

    /** Exports the signature as an SVG string. */
    public fun toSvg(): String {
        return exporter.toSvg(strokes = _strokes, size = layoutSize)
    }

    /**
     * Exports the signature as a Bitmap.
     *
     * @param crop Whether to crop the bitmap to the drawn content.
     * @param paddingCrop Extra padding (in pixels) around the cropped content.
     * @return The exported Bitmap.
     */
    public fun toBitmap(
        crop: Boolean = false,
        paddingCrop: Int = 0
    ): android.graphics.Bitmap {
        return exporter.toBitmap(
            strokes = _strokes,
            size = layoutSize,
            penColor = config.penColor,
            crop = crop,
            paddingCrop = paddingCrop
        )
    }

    /**
     * Exports the signature as a transparent Bitmap.
     *
     * @param crop Whether to crop the bitmap to the drawn content.
     * @param paddingCrop Extra padding (in pixels) around the cropped content.
     * @return The exported transparent Bitmap.
     */
    public fun toTransparentBitmap(
        crop: Boolean = false,
        paddingCrop: Int = 0
    ): android.graphics.Bitmap {
        return exporter.toTransparentBitmap(
            strokes = _strokes,
            size = layoutSize,
            penColor = config.penColor,
            crop = crop,
            paddingCrop = paddingCrop
        )
    }

    // ========================================
    // Internal Functions
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
}

/** A single stroke (continuous line without lifting). */
@Immutable
internal data class Stroke(
    val curves: List<StrokeCurve>
)

/** A Bézier curve segment with variable width. */
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

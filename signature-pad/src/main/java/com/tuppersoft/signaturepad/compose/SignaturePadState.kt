package com.tuppersoft.signaturepad.compose

import androidx.annotation.FloatRange
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import com.tuppersoft.signaturepad.compose.SignaturePadConfig.Companion.bicPen
import com.tuppersoft.signaturepad.compose.SignaturePadConfig.Companion.edding
import com.tuppersoft.signaturepad.compose.SignaturePadConfig.Companion.fountainPen
import com.tuppersoft.signaturepad.compose.SignaturePadConfig.Companion.marker
import com.tuppersoft.signaturepad.utils.Bezier
import com.tuppersoft.signaturepad.utils.SvgBuilder
import com.tuppersoft.signaturepad.utils.TimedPoint
import com.tuppersoft.signaturepad.utils.drawBezierCurve
import kotlin.math.max

/**
 * State holder for [SignaturePad] composable.
 *
 * This class manages the state of a signature pad, including:
 * - Drawing parameters (pen width, color, velocity filter)
 * - Stroke history for UNDO/REDO functionality
 * - Current drawing state (points and curves)
 * - Export capabilities (SVG)
 *
 * The state follows Compose best practices with immutable public APIs
 * and private mutable backing fields for internal state management.
 *
 * Use [rememberSignaturePadState] to create and remember an instance of this state.
 *
 * Example usage:
 * ```
 * val state = rememberSignaturePadState(
 *     penMinWidth = 2.dp,
 *     penMaxWidth = 5.dp,
 *     penColor = Color.Blue
 * )
 *
 * SignaturePad(
 *     state = state,
 *     modifier = Modifier.fillMaxSize()
 * )
 *
 * Button(onClick = { state.undo() }, enabled = state.canUndo()) {
 *     Text("Undo")
 * }
 * ```
 *
 * @param penMinWidth Minimum stroke width in dp, default is 2.dp.
 * @param penMaxWidth Maximum stroke width in dp, default is 5.dp.
 * @param penColor Stroke color, default is [Color.Black].
 * @param velocityFilterWeight Weight for velocity smoothing (0.0-1.0), default is 0.9.
 *        Higher values make stroke width changes smoother.
 */
@Stable
public class SignaturePadState(
    penMinWidth: Dp = PenMinWidth,
    penMaxWidth: Dp = PenMaxWidth,
    penColor: Color = PenColor,
    @FloatRange(from = 0.0, to = 1.0) velocityFilterWeight: Float = VelocityFilterWeight,
) {
    /**
     * Minimum stroke width in dp.
     *
     * Can be modified at runtime to change the pen thickness dynamically.
     * Must be less than or equal to [penMaxWidth].
     */
    public var penMinWidth: Dp by mutableStateOf(value = penMinWidth)

    /**
     * Maximum stroke width in dp.
     *
     * Can be modified at runtime to change the pen thickness dynamically.
     * Must be greater than or equal to [penMinWidth].
     */
    public var penMaxWidth: Dp by mutableStateOf(value = penMaxWidth)

    /**
     * Stroke color.
     *
     * Can be modified at runtime to change the pen color dynamically.
     */
    public var penColor: Color by mutableStateOf(value = penColor)

    /**
     * Velocity filter weight (0.0-1.0).
     *
     * Higher values make stroke width changes smoother.
     * Lower values make the stroke width more responsive to velocity changes.
     *
     * Valid range: 0.0 (no smoothing) to 1.0 (maximum smoothing).
     * Default: 0.9
     */
    public var velocityFilterWeight: Float by mutableFloatStateOf(value = velocityFilterWeight)

    /**
     * Whether the signature is empty (no strokes drawn).
     *
     * This property is automatically derived from the strokes state.
     * It returns `true` when there are no strokes in the history.
     *
     * Use this property to enable/disable save or export buttons in your UI.
     */
    public val isEmpty: Boolean by derivedStateOf { _strokes.isEmpty() }

    /**
     * Size of the SignaturePad layout.
     *
     * This property stores the actual dimensions of the signature pad canvas.
     * It's automatically updated when the layout size changes.
     *
     * Use this property when exporting the signature to ensure the bitmap
     * has the same dimensions as the visible drawing area.
     *
     * Default: IntSize.Zero (before the layout is measured)
     */
    public var layoutSize: IntSize by mutableStateOf(value = IntSize.Zero)
        private set

    /**
     * Updates the layout size (internal use).
     *
     * This method is called automatically by the SignaturePad composable
     * when the layout size changes.
     *
     * @param size The new layout size in pixels.
     */
    internal fun updateLayoutSize(size: IntSize) {
        layoutSize = size
    }

    // Private mutable state for internal calculations
    private var _lastVelocity: Float by mutableFloatStateOf(value = 0f)
    private var _lastWidth: Float by mutableFloatStateOf(value = 0f)

    /**
     * Current velocity for stroke width calculation (internal use).
     *
     * This value is used internally to calculate the next stroke width
     * based on drawing velocity.
     */
    internal val lastVelocity: Float
        get() = _lastVelocity

    /**
     * Current stroke width (internal use).
     *
     * This value represents the width of the most recently drawn stroke segment.
     */
    internal val lastWidth: Float
        get() = _lastWidth

    /**
     * Updates the last velocity value (internal use).
     *
     * @param value The new velocity value.
     */
    internal fun updateVelocity(value: Float) {
        _lastVelocity = value
    }

    /**
     * Updates the last width value (internal use).
     *
     * @param value The new width value in pixels.
     */
    internal fun updateWidth(value: Float) {
        _lastWidth = value
    }

    // Private mutable collections for internal state management
    private val _strokes: MutableList<Stroke> = mutableStateListOf()
    private val _undoneStrokes: MutableList<Stroke> = mutableStateListOf()
    private val _currentPoints: MutableList<TimedPoint> = mutableStateListOf()
    private val _currentCurves: MutableList<StrokeCurve> = mutableStateListOf()

    /**
     * List of completed strokes for UNDO/REDO (read-only, internal use).
     *
     * This list contains all the strokes that have been drawn and completed.
     * Each stroke consists of one or more Bézier curves.
     */
    internal val strokes: List<Stroke> = _strokes

    /**
     * Stack of undone strokes for REDO (read-only, internal use).
     *
     * This list contains strokes that have been undone and can be redone.
     * It is cleared when a new stroke is added.
     */
    internal val undoneStrokes: List<Stroke> = _undoneStrokes

    /**
     * Points of the current stroke being drawn (read-only, internal use).
     *
     * This list accumulates points as the user draws, before they are
     * converted into Bézier curves.
     */
    internal val currentPoints: List<TimedPoint> = _currentPoints

    /**
     * Curves of the current stroke being drawn (read-only, internal use).
     *
     * This list accumulates Bézier curves as the user draws, representing
     * the smooth interpolation of the input points.
     */
    internal val currentCurves: List<StrokeCurve> = _currentCurves

    /**
     * Adds a completed stroke to the history (internal use).
     *
     * This method also clears the redo stack, as adding a new stroke
     * invalidates any previously undone strokes.
     *
     * @param stroke The completed stroke to add.
     */
    internal fun addStroke(stroke: Stroke) {
        _strokes.add(element = stroke)
        _undoneStrokes.clear()
    }

    /**
     * Adds a point to the current stroke (internal use).
     *
     * @param point The timed point to add to the current stroke.
     */
    internal fun addCurrentPoint(point: TimedPoint) {
        _currentPoints.add(element = point)
    }

    /**
     * Removes the first point from the current stroke (sliding window, internal use).
     *
     * This is used to maintain a fixed-size window of points for
     * Bézier curve calculation.
     */
    internal fun removeFirstCurrentPoint() {
        if (_currentPoints.isNotEmpty()) {
            _currentPoints.removeAt(index = 0)
        }
    }

    /**
     * Adds a curve to the current stroke (internal use).
     *
     * @param curve The Bézier curve to add to the current stroke.
     */
    internal fun addCurrentCurve(curve: StrokeCurve) {
        _currentCurves.add(element = curve)
    }

    /**
     * Clears the current stroke points and curves (internal use).
     *
     * This is called when a stroke is completed or when starting a new stroke.
     */
    internal fun clearCurrentStroke() {
        _currentPoints.clear()
        _currentCurves.clear()
    }

    /**
     * SVG builder for export (lazy initialization).
     *
     * The SVG builder is created only when [toSvg] is called for the first time,
     * optimizing memory usage when SVG export is not needed.
     */
    private val svgBuilder: SvgBuilder by lazy { SvgBuilder() }

    /**
     * Checks if undo is possible.
     *
     * @return `true` if there are strokes to undo, `false` otherwise.
     */
    public fun canUndo(): Boolean = _strokes.isNotEmpty()

    /**
     * Checks if redo is possible.
     *
     * @return `true` if there are strokes to redo, `false` otherwise.
     */
    public fun canRedo(): Boolean = _undoneStrokes.isNotEmpty()

    /**
     * Undoes the last stroke.
     *
     * Removes the most recent stroke from the history and adds it to
     * the undo stack, allowing it to be redone later.
     *
     * @return `true` if undo was performed, `false` if there was nothing to undo.
     */
    public fun undo(): Boolean {
        if (!canUndo()) return false

        val lastStroke: Stroke = _strokes.removeAt(index = _strokes.lastIndex)
        _undoneStrokes.add(element = lastStroke)

        return true
    }

    /**
     * Redoes the last undone stroke.
     *
     * Removes the most recent undone stroke from the undo stack and
     * adds it back to the history.
     *
     * @return `true` if redo was performed, `false` if there was nothing to redo.
     */
    public fun redo(): Boolean {
        if (!canRedo()) return false

        val stroke: Stroke = _undoneStrokes.removeAt(index = _undoneStrokes.lastIndex)
        _strokes.add(element = stroke)

        return true
    }

    /**
     * Clears all strokes and resets the state.
     *
     * This removes all drawn strokes, clears undo/redo history,
     * and resets internal drawing state (velocity and width).
     *
     * After calling this method, [isEmpty] will return `true`.
     */
    public fun clear() {
        _strokes.clear()
        _undoneStrokes.clear()
        _currentPoints.clear()
        _currentCurves.clear()
        svgBuilder.clear()
        _lastVelocity = 0f
        _lastWidth = 0f
    }

    /**
     * Exports the signature as an SVG string.
     *
     * The SVG format allows for scalable, resolution-independent representation
     * of the signature. The exported SVG contains all strokes as path elements
     * using the dimensions stored in [layoutSize] (automatically captured from
     * the SignaturePad composable).
     *
     * The SVG viewport dimensions match exactly the size of the SignaturePad canvas,
     * ensuring the exported signature appears identical to what was drawn.
     *
     * @return The SVG representation of the signature as a string, with viewport
     *         dimensions matching the SignaturePad layout size.
     * @throws IllegalStateException if called before the layout size is measured
     *         (when layoutSize is IntSize.Zero).
     */
    public fun toSvg(): String {
        svgBuilder.clear()
        _strokes.forEach { stroke: Stroke ->
            stroke.curves.forEach { curve: StrokeCurve ->
                svgBuilder.append(
                    curve = curve.bezier,
                    strokeWidth = (curve.startWidth + curve.endWidth) / 2f
                )
            }
        }
        return svgBuilder.build(width = layoutSize.width, height = layoutSize.height)
    }

    /**
     * Exports the signature as a Bitmap with white background.
     *
     * This function renders all strokes onto a bitmap using the dimensions stored
     * in [layoutSize] (automatically captured from the SignaturePad composable).
     * The background is filled with white color, suitable for printing or sharing.
     *
     * The bitmap dimensions match exactly the size of the SignaturePad canvas,
     * ensuring the exported signature appears identical to what was drawn.
     *
     * @return A Bitmap containing the signature with white background, sized to match
     *         the SignaturePad layout dimensions.
     * @throws IllegalStateException if called before the layout size is measured
     *         (when layoutSize is IntSize.Zero).
     */
    public fun toBitmap(): android.graphics.Bitmap {
        val bitmap: android.graphics.Bitmap = createBitmap(layoutSize.width, layoutSize.height)
        val canvas = android.graphics.Canvas(bitmap)

        // Draw white background
        canvas.drawColor(android.graphics.Color.WHITE)

        // Draw signature
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
            color = penColor.toArgb()
        }

        _strokes.forEach { stroke: Stroke ->
            stroke.curves.forEach { curve: StrokeCurve ->
                drawBezierCurve(
                    canvas = canvas,
                    paint = paint,
                    curve = curve.bezier,
                    startWidth = curve.startWidth,
                    endWidth = curve.endWidth
                )
            }
        }

        return bitmap
    }

    /**
     * Exports the signature as a transparent Bitmap.
     *
     * This function renders all strokes onto a bitmap using the dimensions stored
     * in [layoutSize] (automatically captured from the SignaturePad composable).
     * The background is transparent, suitable for overlaying on other images or
     * compositing with other graphics.
     *
     * The bitmap dimensions match exactly the size of the SignaturePad canvas,
     * ensuring the exported signature appears identical to what was drawn.
     *
     * @return A Bitmap containing the signature with transparent background, sized
     *         to match the SignaturePad layout dimensions.
     * @throws IllegalStateException if called before the layout size is measured
     *         (when layoutSize is IntSize.Zero).
     */
    public fun toTransparentBitmap(): android.graphics.Bitmap {
        val bitmap: android.graphics.Bitmap = createBitmap(layoutSize.width, layoutSize.height)
        val canvas = android.graphics.Canvas(bitmap)

        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
            color = penColor.toArgb()
        }

        _strokes.forEach { stroke: Stroke ->
            stroke.curves.forEach { curve: StrokeCurve ->
                drawBezierCurve(
                    canvas = canvas,
                    paint = paint,
                    curve = curve.bezier,
                    startWidth = curve.startWidth,
                    endWidth = curve.endWidth
                )
            }
        }

        return bitmap
    }

    /**
     * Calculates stroke width based on drawing velocity (internal use).
     *
     * The stroke width varies inversely with velocity - faster movements
     * produce thinner strokes, slower movements produce thicker strokes.
     *
     * @param velocity Current drawing velocity.
     * @param minWidthPx Minimum stroke width in pixels.
     * @param maxWidthPx Maximum stroke width in pixels.
     * @return Calculated stroke width in pixels, clamped between min and max.
     */
    internal fun calculateStrokeWidth(
        velocity: Float,
        minWidthPx: Float,
        maxWidthPx: Float
    ): Float {
        return max(a = maxWidthPx / (velocity + 1f), b = minWidthPx)
    }
}

/**
 * Represents a single stroke (continuous line drawn without lifting finger).
 *
 * A stroke consists of one or more Bézier curves that interpolate
 * the raw input points into a smooth line. Each stroke is created when
 * the user touches the screen and continues until they lift their finger.
 *
 * Strokes are immutable and form the basis of the UNDO/REDO functionality,
 * where entire strokes can be removed or restored as atomic units.
 *
 * This class is internal as it's an implementation detail of the signature pad state.
 * Users interact with strokes through high-level operations like undo(), redo(), and clear().
 *
 * @property curves List of Bézier curves that make up this stroke.
 *                  A stroke always contains at least one curve, though typically
 *                  contains multiple curves for smooth continuous drawing.
 */
@Immutable
internal data class Stroke(
    val curves: List<StrokeCurve>
)

/**
 * Represents a single Bézier curve segment within a stroke.
 *
 * Each curve segment has a varying width based on drawing velocity,
 * creating a more natural and organic appearance. Faster movements
 * produce thinner lines, while slower movements produce thicker lines.
 *
 * This class is immutable to ensure thread-safety and allow for
 * efficient UNDO/REDO operations without data corruption.
 *
 * This class is internal as it's an implementation detail of the signature pad state.
 * Users interact with curves indirectly through drawing gestures and export operations.
 *
 * @property bezier The Bézier curve geometry (start point, control points, end point).
 * @property startWidth Width at the start of the curve in pixels.
 *                      This value is calculated based on the velocity at the start point.
 * @property endWidth Width at the end of the curve in pixels.
 *                    This value is calculated based on the velocity at the end point.
 */
@Immutable
internal data class StrokeCurve(
    val bezier: Bezier,
    val startWidth: Float,
    val endWidth: Float
)

/**
 * Creates and remembers a [SignaturePadState].
 *
 * Use predefined configurations from [SignaturePadConfig] companion object:
 * - [SignaturePadConfig.fountainPen] - Elegant pen with moderate contrast (1-4.5dp)
 * - [SignaturePadConfig.bicPen] - Uniform ballpoint (2-2.5dp)
 * - [SignaturePadConfig.marker] - Thick marker (3-4dp)
 * - [SignaturePadConfig.edding] - Very bold marker (5-6.5dp)
 *
 * **Note**: State is not persisted across configuration changes.
 *
 * ```
 * // Default
 * val state = rememberSignaturePadState()
 *
 * // With preset
 * val state = rememberSignaturePadState(
 *     signaturePadConfig = SignaturePadConfig.fountainPen()
 * )
 *
 * // Custom
 * val state = rememberSignaturePadState(
 *     signaturePadConfig = SignaturePadConfig(
 *         penMinWidth = 2.dp,
 *         penMaxWidth = 5.dp,
 *         penColor = Color.Blue,
 *         velocityFilterWeight = 0.85f
 *     )
 * )
 * ```
 *
 * @param signaturePadConfig Configuration for pen behavior and appearance.
 * @return The remembered [SignaturePadState] instance.
 */
@Composable
public fun rememberSignaturePadState(
    signaturePadConfig: SignaturePadConfig = SignaturePadConfig.fountainPen()
): SignaturePadState {
    return remember {
        SignaturePadState(
            penMinWidth = signaturePadConfig.penMinWidth,
            penMaxWidth = signaturePadConfig.penMaxWidth,
            penColor = signaturePadConfig.penColor,
            velocityFilterWeight = signaturePadConfig.velocityFilterWeight
        )
    }
}

/**
 * Configuration for [SignaturePad] drawing behavior.
 *
 * Encapsulates stroke width, color, and smoothing parameters with preset
 * configurations for common writing instruments.
 *
 * **Presets**: [fountainPen], [bicPen], [marker], [edding]
 *
 * ```
 * // With preset
 * val config = SignaturePadConfig.fountainPen(penColor = Color.Blue)
 *
 * // Custom
 * val config = SignaturePadConfig(
 *     penMinWidth = 2.dp,
 *     penMaxWidth = 8.dp,
 *     penColor = Color.Red,
 *     velocityFilterWeight = 0.8f
 * )
 * ```
 *
 * @property penMinWidth Minimum stroke width when drawing fast.
 * @property penMaxWidth Maximum stroke width when drawing slowly.
 * @property penColor Color of the drawn strokes.
 * @property velocityFilterWeight Smoothing factor (0.0-1.0).
 *           Higher = smoother (0.8-0.95), Lower = more responsive (0.4-0.6).
 */
@Immutable
public data class SignaturePadConfig(
    val penMinWidth: Dp = PenMinWidth,
    val penMaxWidth: Dp = PenMaxWidth,
    val penColor: Color = PenColor,
    @FloatRange(from = 0.0, to = 1.0) val velocityFilterWeight: Float = VelocityFilterWeight,
) {

    /**
     * Converts this configuration to a [SignaturePadState].
     *
     * Useful for initializing a [SignaturePadState] with this config.
     *
     * @return A new [SignaturePadState] instance with parameters from this config.
     *
     * sample usage:
     * ```
     * val state = SignaturePadConfig.fountainPen().toSignaturePadState()
     * ```
     */
    public fun toSignaturePadState(): SignaturePadState {
        return SignaturePadState(
            penMinWidth = penMinWidth,
            penMaxWidth = penMaxWidth,
            penColor = penColor,
            velocityFilterWeight = velocityFilterWeight
        )
    }

    /**
     * Predefined configurations for common writing instruments.
     */
    public companion object {
        /**
         * Fountain pen: elegant with moderate contrast (1-4.5dp, smoothing 0.85).
         * Best for elegant signatures and formal documents.
         */
        public fun fountainPen(penColor: Color = PenColor): SignaturePadConfig {
            return SignaturePadConfig(
                penMinWidth = 1.0.dp,
                penMaxWidth = 4.5.dp,
                velocityFilterWeight = 0.85f,
                penColor = penColor
            )
        }

        /**
         * BIC pen: uniform and consistent (2-2.5dp, smoothing 0.95).
         * Best for everyday signatures and forms.
         */
        public fun bicPen(penColor: Color = PenColor): SignaturePadConfig {
            return SignaturePadConfig(
                penMinWidth = 2.dp,
                penMaxWidth = 2.5.dp,
                velocityFilterWeight = 0.95f,
                penColor = penColor
            )
        }

        /**
         * Marker: thick and uniform (3-4dp, smoothing 0.92).
         * Best for bold signatures and emphasis.
         */
        public fun marker(penColor: Color = PenColor): SignaturePadConfig {
            return SignaturePadConfig(
                penMinWidth = 3.dp,
                penMaxWidth = 4.dp,
                velocityFilterWeight = 0.92f,
                penColor = penColor
            )
        }

        /**
         * Edding marker: very bold (5-6.5dp, smoothing 0.93).
         * Best for maximum visibility and industrial use.
         */
        public fun edding(penColor: Color = PenColor): SignaturePadConfig {
            return SignaturePadConfig(
                penMinWidth = 5.dp,
                penMaxWidth = 6.5.dp,
                velocityFilterWeight = 0.93f,
                penColor = penColor
            )
        }
    }
}

/**
 * Default minimum pen width.
 */
private val PenMinWidth: Dp = 4.dp

/**
 * Default maximum pen width.
 */
private val PenMaxWidth: Dp = 7.dp

/**
 * Default pen color.
 */
private val PenColor: Color = Color.Black

/**
 * Default velocity filter weight.
 */
private const val VelocityFilterWeight: Float = 0.9f

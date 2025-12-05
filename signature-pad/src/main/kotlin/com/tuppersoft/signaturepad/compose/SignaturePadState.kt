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
import com.tuppersoft.signaturepad.export.SvgBuilder
import com.tuppersoft.signaturepad.geometry.Bezier
import com.tuppersoft.signaturepad.geometry.TimedPoint
import com.tuppersoft.signaturepad.rendering.drawBezierCurve
import kotlin.math.pow

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
 * @param widthFilterWeight Weight for width smoothing (0.0-1.0), default is 0.7.
 *        Controls the inertia of ink flow for smoother width transitions.
 * @param minVelocity Minimum velocity threshold in pixels per millisecond, default is 0.
 *        Drawing slower than this uses maximum width.
 * @param maxVelocity Maximum velocity threshold in pixels per millisecond, default is 10.
 *        Drawing faster than this uses minimum width.
 * @param pressureGamma Gamma curve factor for pressure simulation (0.5-3.0), default is 1.5.
 *        Controls non-linearity of pressure response. 1.0 = linear, 1.5 = natural.
 * @param inputNoiseThreshold Minimum distance in pixels between points, default is 1.0.
 *        Points closer than this are filtered to reduce input noise.
 * @param enableInkBleed Enable ink bleed effect at stroke end, default is false.
 *        When true, draws a blob when pen stops slowly, simulating ink bleeding.
 */
@Stable
public class SignaturePadState(
    penMinWidth: Dp = PenMinWidth,
    penMaxWidth: Dp = PenMaxWidth,
    penColor: Color = PenColor,
    @FloatRange(from = 0.0, to = 1.0) velocityFilterWeight: Float = VelocityFilterWeight,
    @FloatRange(from = 0.0, to = 1.0) widthFilterWeight: Float = WidthFilterWeight,
    @FloatRange(from = 0.0) minVelocity: Float = MinVelocity,
    @FloatRange(from = 0.0) maxVelocity: Float = MaxVelocity,
    @FloatRange(from = 0.5, to = 3.0) pressureGamma: Float = PressureGamma,
    @FloatRange(from = 0.0) inputNoiseThreshold: Float = InputNoiseThreshold,
    enableInkBleed: Boolean = false,
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
     * Width filter weight (0.0-1.0).
     *
     * Controls the smoothing of width transitions, simulating ink flow inertia.
     * Higher values (0.7-0.8) create smoother, more gradual width changes.
     * Lower values (0.5-0.6) make width more responsive to velocity changes.
     *
     * This is critical for natural-looking strokes, as it prevents abrupt
     * changes in line thickness when drawing speed varies.
     *
     * Valid range: 0.0 (no smoothing) to 1.0 (maximum smoothing).
     * Default: 0.7
     */
    public var widthFilterWeight: Float by mutableFloatStateOf(value = widthFilterWeight)

    /**
     * Minimum velocity threshold in pixels per millisecond.
     *
     * Drawing slower than this velocity will use maximum pen width.
     * Used for normalizing velocity to the [0, 1] range.
     *
     * Valid range: >= 0.0
     * Default: 0.0
     */
    public var minVelocity: Float by mutableFloatStateOf(value = minVelocity)

    /**
     * Maximum velocity threshold in pixels per millisecond.
     *
     * Drawing faster than this velocity will use minimum pen width.
     * Used for normalizing velocity to the [0, 1] range.
     *
     * Valid range: >= 0.0
     * Default: 10.0
     */
    public var maxVelocity: Float by mutableFloatStateOf(value = maxVelocity)

    /**
     * Pressure gamma factor for non-linear pressure response (0.5-3.0).
     *
     * Controls the curvature of the pressure-to-width mapping:
     * - 1.0 = Linear response (pressure proportional to width)
     * - 1.5 = Natural, smooth response (recommended for most use cases)
     * - 2.0 = Pronounced pressure effect (more dramatic width variation)
     *
     * The gamma curve is applied as: pressure^gamma, where pressure is
     * the normalized inverse velocity [0, 1].
     *
     * Valid range: 0.5 to 3.0
     * Default: 1.5
     */
    public var pressureGamma: Float by mutableFloatStateOf(value = pressureGamma)

    /**
     * Input noise threshold in pixels.
     *
     * Minimum distance between consecutive input points. Points closer than
     * this threshold are filtered out to reduce sensor noise and jitter.
     *
     * Typical values:
     * - 0.5-1.0 for high-precision input (stylus)
     * - 1.0-2.0 for touch input (finger)
     *
     * Valid range: >= 0.0
     * Default: 1.0
     */
    public var inputNoiseThreshold: Float by mutableFloatStateOf(value = inputNoiseThreshold)

    /**
     * Enable ink bleed effect when stroke ends at low velocity.
     *
     * When true and the pen stops slowly (velocity < 0.5 px/ms), a circular
     * blob is drawn at the final position to simulate ink bleeding on paper.
     * This adds realism for fountain pen and similar writing instruments.
     *
     * Default: false
     */
    public var enableInkBleed: Boolean by mutableStateOf(value = enableInkBleed)

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
     * Calculates stroke width based on drawing velocity using normalized pressure model.
     *
     * This function implements the stylographic pen model with the following steps:
     * 1. Normalize velocity to [0, 1] range using [minVelocity] and [maxVelocity] thresholds
     * 2. Calculate simulated pressure as inverse of normalized velocity
     * 3. Apply gamma curve for non-linear response using [pressureGamma]
     * 4. Map the result to the width range [minWidthPx, maxWidthPx]
     *
     * Mathematical formula:
     * ```
     * v_norm = clamp((v - v_min) / (v_max - v_min), 0, 1)
     * ρ_sim = 1 - v_norm                    // Inverse: slow = high pressure
     * ρ_gamma = ρ_sim^γ                     // Apply gamma curve
     * W = W_min + (W_max - W_min) · ρ_gamma // Map to width range
     * ```
     *
     * Physical interpretation:
     * - When velocity ≈ 0 (pen stopped) → pressure = 1.0 → width = maximum (ink pools)
     * - When velocity = max (fast stroke) → pressure = 0.0 → width = minimum (light touch)
     *
     * @param velocity Current drawing velocity in pixels per millisecond.
     * @param minWidthPx Minimum stroke width in pixels (used at high velocity).
     * @param maxWidthPx Maximum stroke width in pixels (used at low velocity).
     * @return Calculated stroke width in pixels, smoothly varying with velocity.
     *
     * @see pressureGamma For controlling the non-linearity of the response curve.
     * @see minVelocity For setting the slow velocity threshold.
     * @see maxVelocity For setting the fast velocity threshold.
     */
    internal fun calculateStrokeWidth(
        velocity: Float,
        minWidthPx: Float,
        maxWidthPx: Float
    ): Float {
        // Step 1: Normalize velocity to [0, 1] range
        val velocityRange = maxVelocity - minVelocity
        val normalizedVelocity = if (velocityRange > 0f) {
            ((velocity - minVelocity) / velocityRange).coerceIn(0f, 1f)
        } else {
            // Edge case: if min and max velocities are equal, use mid-pressure
            0.5f
        }

        // Step 2: Calculate simulated pressure (inverse of velocity)
        // When velocity is 0 (stopped) → pressure = 1.0 (maximum width)
        // When velocity is max (fast) → pressure = 0.0 (minimum width)
        val simulatedPressure = 1f - normalizedVelocity

        // Step 3: Apply gamma curve for non-linear response
        // gamma = 1.0 → linear response
        // gamma > 1.0 → more gradual pressure build-up (natural feel)
        // gamma < 1.0 → more abrupt pressure changes
        val pressureWithGamma = simulatedPressure.pow(pressureGamma)

        // Step 4: Map pressure to width range
        return minWidthPx + (maxWidthPx - minWidthPx) * pressureWithGamma
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
            velocityFilterWeight = signaturePadConfig.velocityFilterWeight,
            widthFilterWeight = signaturePadConfig.widthFilterWeight,
            minVelocity = signaturePadConfig.minVelocity,
            maxVelocity = signaturePadConfig.maxVelocity,
            pressureGamma = signaturePadConfig.pressureGamma,
            inputNoiseThreshold = signaturePadConfig.inputNoiseThreshold,
            enableInkBleed = signaturePadConfig.enableInkBleed
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
 * @property velocityFilterWeight Smoothing factor for velocity (0.0-1.0).
 *           Higher = smoother (0.8-0.95), Lower = more responsive (0.4-0.6).
 * @property widthFilterWeight Smoothing factor for width transitions (0.0-1.0).
 *           Controls the inertia of ink flow. Higher values (0.7-0.8) create smoother
 *           width changes, while lower values (0.5-0.6) are more responsive.
 * @property minVelocity Minimum velocity threshold in pixels per millisecond.
 *           Drawing slower than this will use maximum width. Default is 0.
 * @property maxVelocity Maximum velocity threshold in pixels per millisecond.
 *           Drawing faster than this will use minimum width. Default is 10.
 * @property pressureGamma Gamma curve factor for pressure simulation (0.5-3.0).
 *           Controls the non-linearity of the pressure response curve.
 *           - 1.0 = Linear response
 *           - 1.5 = Natural, smooth response (recommended)
 *           - 2.0 = Very pronounced pressure effect
 * @property inputNoiseThreshold Minimum distance in pixels between consecutive points.
 *           Points closer than this threshold will be filtered out to reduce input noise.
 *           Typical values: 0.5-2.0 pixels. Default is 1.0.
 * @property enableInkBleed Enable ink bleed effect when stroke ends at low velocity.
 *           When true, a circular blob is drawn at the end point if the pen stops slowly,
 *           simulating real ink bleeding on paper. Default is false.
 */
@Immutable
public data class SignaturePadConfig(
    val penMinWidth: Dp = PenMinWidth,
    val penMaxWidth: Dp = PenMaxWidth,
    val penColor: Color = PenColor,
    @FloatRange(from = 0.0, to = 1.0) val velocityFilterWeight: Float = VelocityFilterWeight,
    @FloatRange(from = 0.0, to = 1.0) val widthFilterWeight: Float = WidthFilterWeight,
    @FloatRange(from = 0.0) val minVelocity: Float = MinVelocity,
    @FloatRange(from = 0.0) val maxVelocity: Float = MaxVelocity,
    @FloatRange(from = 0.5, to = 3.0) val pressureGamma: Float = PressureGamma,
    @FloatRange(from = 0.0) val inputNoiseThreshold: Float = InputNoiseThreshold,
    val enableInkBleed: Boolean = false,
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
            velocityFilterWeight = velocityFilterWeight,
            widthFilterWeight = widthFilterWeight,
            minVelocity = minVelocity,
            maxVelocity = maxVelocity,
            pressureGamma = pressureGamma,
            inputNoiseThreshold = inputNoiseThreshold,
            enableInkBleed = enableInkBleed
        )
    }

    /**
     * Predefined configurations for common writing instruments.
     */
    public companion object {
        /**
         * Fountain pen: elegant with moderate contrast (1-4.5dp, smoothing 0.85).
         * Best for elegant signatures and formal documents.
         * Features natural pressure response and ink bleed effect.
         */
        public fun fountainPen(penColor: Color = PenColor): SignaturePadConfig {
            return SignaturePadConfig(
                penMinWidth = 1.0.dp,
                penMaxWidth = 4.5.dp,
                velocityFilterWeight = 0.85f,
                widthFilterWeight = 0.7f,
                minVelocity = 0f,
                maxVelocity = 8f,
                pressureGamma = 1.5f,
                inputNoiseThreshold = 0.8f,
                enableInkBleed = true,
                penColor = penColor
            )
        }

        /**
         * BIC pen: uniform and consistent (2-2.5dp, smoothing 0.95).
         * Best for everyday signatures and forms.
         * Minimal width variation for consistent lines.
         */
        public fun bicPen(penColor: Color = PenColor): SignaturePadConfig {
            return SignaturePadConfig(
                penMinWidth = 2.dp,
                penMaxWidth = 2.5.dp,
                velocityFilterWeight = 0.95f,
                widthFilterWeight = 0.8f,
                minVelocity = 0f,
                maxVelocity = 12f,
                pressureGamma = 1.0f,
                inputNoiseThreshold = 1.0f,
                enableInkBleed = false,
                penColor = penColor
            )
        }

        /**
         * Marker: thick and uniform (3-4dp, smoothing 0.92).
         * Best for bold signatures and emphasis.
         * Medium width variation with smooth transitions.
         */
        public fun marker(penColor: Color = PenColor): SignaturePadConfig {
            return SignaturePadConfig(
                penMinWidth = 3.dp,
                penMaxWidth = 4.dp,
                velocityFilterWeight = 0.92f,
                widthFilterWeight = 0.85f,
                minVelocity = 0f,
                maxVelocity = 15f,
                pressureGamma = 1.2f,
                inputNoiseThreshold = 1.2f,
                enableInkBleed = false,
                penColor = penColor
            )
        }

        /**
         * Edding marker: very bold (5-6.5dp, smoothing 0.93).
         * Best for maximum visibility and industrial use.
         * High width smoothing for stable, bold lines.
         */
        public fun edding(penColor: Color = PenColor): SignaturePadConfig {
            return SignaturePadConfig(
                penMinWidth = 5.dp,
                penMaxWidth = 6.5.dp,
                velocityFilterWeight = 0.93f,
                widthFilterWeight = 0.88f,
                minVelocity = 0f,
                maxVelocity = 18f,
                pressureGamma = 1.1f,
                inputNoiseThreshold = 1.5f,
                enableInkBleed = false,
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

/**
 * Default width filter weight.
 * Controls smoothing of width transitions (ink flow inertia).
 */
private const val WidthFilterWeight: Float = 0.7f

/**
 * Default minimum velocity threshold in pixels per millisecond.
 * Drawing slower than this uses maximum width.
 */
private const val MinVelocity: Float = 0f

/**
 * Default maximum velocity threshold in pixels per millisecond.
 * Drawing faster than this uses minimum width.
 */
private const val MaxVelocity: Float = 10f

/**
 * Default pressure gamma factor.
 * 1.5 provides a natural, smooth pressure response curve.
 */
private const val PressureGamma: Float = 1.5f

/**
 * Default input noise threshold in pixels.
 * Points closer than this are filtered out to reduce sensor noise.
 */
private const val InputNoiseThreshold: Float = 1.0f


package com.tuppersoft.signaturepad.compose

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.createBitmap
import com.tuppersoft.signaturepad.geometry.Bezier
import com.tuppersoft.signaturepad.geometry.ControlTimedPoints
import com.tuppersoft.signaturepad.geometry.TimedPoint
import com.tuppersoft.signaturepad.geometry.calculateControlPoints
import com.tuppersoft.signaturepad.rendering.drawBezierCurve
import kotlin.math.sqrt

/**
 * A Composable for capturing smooth signature drawings with Bézier curve interpolation.
 *
 * This component provides a drawing surface where users can create signatures with smooth,
 * velocity-based stroke width. It supports UNDO/REDO operations and exports to Bitmap/SVG.
 *
 * Example usage:
 * ```
 * val state = rememberSignaturePadState()
 *
 * SignaturePad(
 *     state = state,
 *     modifier = Modifier.fillMaxWidth().height(300.dp)
 * )
 *
 * // Control buttons
 * Button(onClick = { state.undo() }, enabled = state.canUndo()) {
 *     Text("Undo")
 * }
 * ```
 *
 * @param state The state holder for this signature pad.
 * @param modifier The modifier to be applied to this signature pad.
 * @param onStartSign Called when the user starts signing.
 * @param onSign Called when the signature is updated.
 * @param onClear Called when the signature is cleared.
 */
@Composable
public fun SignaturePad(
    state: SignaturePadState,
    modifier: Modifier = Modifier,
    onStartSign: () -> Unit = {},
    onSign: () -> Unit = {},
    onClear: () -> Unit = {}
) {
    val density = LocalDensity.current
    var size by remember { mutableStateOf(IntSize.Zero) }
    var signatureBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var drawVersion by remember { mutableIntStateOf(0) }
    val penMinWidthPx by remember { derivedStateOf { with(density) { state.penMinWidth.toPx() } } }
    val penMaxWidthPx by remember { derivedStateOf { with(density) { state.penMaxWidth.toPx() } } }
    val paint = rememberSignaturePaint(penColor = state.penColor)
    val controlTimedPoints = remember { ControlTimedPoints() }
    val bezierCache = remember { Bezier() }

    InitializeSignatureEffects(
        state = state,
        penMinWidthPx = penMinWidthPx,
        penMaxWidthPx = penMaxWidthPx,
        onClear = onClear,
        onSign = onSign
    )

    RedrawStrokesEffect(
        state = state,
        size = size,
        paint = paint,
        onBitmapUpdate = { signatureBitmap = it }
    )

    Canvas(
        modifier = modifier
            .onSizeChanged { newSize ->
                size = newSize
                state.updateLayoutSize(newSize)
                if (newSize.width > 0 && newSize.height > 0) {
                    signatureBitmap = createBitmap(newSize.width, newSize.height)
                }
            }
            .pointerInput(penMinWidthPx, penMaxWidthPx) {
                handleSignatureGestures(
                    state = state,
                    paint = paint,
                    penMinWidthPx = penMinWidthPx,
                    penMaxWidthPx = penMaxWidthPx,
                    controlPointsCache = controlTimedPoints,
                    bezierCache = bezierCache,
                    signatureBitmap = { signatureBitmap },
                    onDrawVersionIncrement = { drawVersion++ },
                    onStartSign = onStartSign
                )
            }
    ) {
        if (drawVersion >= 0) {

            signatureBitmap?.let { bitmap ->
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawBitmap(bitmap, 0f, 0f, null)
                }
            }
        }
    }
}


/**
 * Creates and remembers a Paint object configured for signature drawing.
 */
@Composable
private fun rememberSignaturePaint(penColor: Color): Paint {
    val paint = remember {
        Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
    }

    LaunchedEffect(penColor) {
        paint.color = penColor.toArgb()
    }

    return paint
}

/**
 * Initializes signature effects for width and callbacks.
 */
@Composable
private fun InitializeSignatureEffects(
    state: SignaturePadState,
    penMinWidthPx: Float,
    penMaxWidthPx: Float,
    onClear: () -> Unit,
    onSign: () -> Unit
) {
    val currentOnClear by rememberUpdatedState(newValue = onClear)
    val currentOnSign by rememberUpdatedState(newValue = onSign)

    LaunchedEffect(penMinWidthPx, penMaxWidthPx) {
        if (state.lastWidth == 0f) {
            state.updateWidth((penMinWidthPx + penMaxWidthPx) / 2f)
        }
    }

    LaunchedEffect(state.isEmpty) {
        if (state.isEmpty) {
            currentOnClear()
        } else {
            currentOnSign()
        }
    }
}

/**
 * Effect that redraws all strokes when they change (for UNDO/REDO).
 */
@Composable
private fun RedrawStrokesEffect(
    state: SignaturePadState,
    size: IntSize,
    paint: Paint,
    onBitmapUpdate: (Bitmap) -> Unit
) {
    val strokesVersion by remember { derivedStateOf { state.strokes.size } }
    val currentOnBitmapUpdate by rememberUpdatedState(newValue = onBitmapUpdate)

    LaunchedEffect(strokesVersion, size, state.penColor) {
        if (size.width > 0 && size.height > 0) {
            val bitmap = createBitmap(size.width, size.height)
            val canvas = Canvas(bitmap)

            state.strokes.forEach { stroke ->
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

            currentOnBitmapUpdate(bitmap)
        }
    }
}

/**
 * Handles signature drawing gestures.
 */
private suspend fun PointerInputScope.handleSignatureGestures(
    state: SignaturePadState,
    paint: Paint,
    penMinWidthPx: Float,
    penMaxWidthPx: Float,
    controlPointsCache: ControlTimedPoints,
    bezierCache: Bezier,
    signatureBitmap: () -> Bitmap?,
    onDrawVersionIncrement: () -> Unit,
    onStartSign: () -> Unit
) {
    detectDragGestures(
        onDragStart = { offset ->
            state.clearCurrentStroke()
            state.addCurrentPoint(TimedPoint(offset.x, offset.y))
            onStartSign()
        },
        onDrag = { change, _ ->
            handleDragEvent(
                state = state,
                change = change,
                paint = paint,
                penMinWidthPx = penMinWidthPx,
                penMaxWidthPx = penMaxWidthPx,
                controlPointsCache = controlPointsCache,
                bezierCache = bezierCache,
                signatureBitmap = signatureBitmap,
                onDrawVersionIncrement = onDrawVersionIncrement
            )
        },
        onDragEnd = {
            // Ink bleed effect: draw a blob if pen stopped slowly
            if (state.enableInkBleed && state.currentPoints.size >= 2) {
                val lastPoint = state.currentPoints.last()
                val prevPoint = state.currentPoints[state.currentPoints.lastIndex - 1]
                val finalVelocity = lastPoint.velocityFrom(prevPoint)

                // Threshold for considering "stopped" (slow velocity)
                val bleedVelocityThreshold = 0.5f

                if (finalVelocity < bleedVelocityThreshold) {
                    signatureBitmap()?.let { bitmap ->
                        val canvas = Canvas(bitmap)

                        // Save original style
                        val originalStyle = paint.style

                        // Draw circular blob to simulate ink bleeding
                        paint.style = Paint.Style.FILL
                        val bleedRadius = penMaxWidthPx / 2f
                        canvas.drawCircle(
                            lastPoint.x,
                            lastPoint.y,
                            bleedRadius,
                            paint
                        )

                        // Restore original style
                        paint.style = originalStyle
                        onDrawVersionIncrement()
                    }
                }
            }

            if (state.currentCurves.isNotEmpty()) {
                state.addStroke(Stroke(curves = state.currentCurves.toList()))
            }
            state.clearCurrentStroke()
        }
    )
}

/**
 * Handles a single drag event during signature drawing.
 */
private fun handleDragEvent(
    state: SignaturePadState,
    change: androidx.compose.ui.input.pointer.PointerInputChange,
    paint: Paint,
    penMinWidthPx: Float,
    penMaxWidthPx: Float,
    controlPointsCache: ControlTimedPoints,
    bezierCache: Bezier,
    signatureBitmap: () -> Bitmap?,
    onDrawVersionIncrement: () -> Unit
) {
    val point = TimedPoint(change.position.x, change.position.y)

    // Filter input noise - discard points too close to the last point
    if (state.currentPoints.isNotEmpty()) {
        val lastPoint = state.currentPoints.last()
        val dx = point.x - lastPoint.x
        val dy = point.y - lastPoint.y
        val distance = sqrt(dx * dx + dy * dy)

        // If point is closer than threshold, it's likely sensor noise - discard it
        if (distance < state.inputNoiseThreshold) {
            return
        }
    }

    state.addCurrentPoint(point)

    val pointsCount = state.currentPoints.size
    when {
        pointsCount > 3 -> {
            processBezierCurve(
                state = state,
                paint = paint,
                penMinWidthPx = penMinWidthPx,
                penMaxWidthPx = penMaxWidthPx,
                controlPointsCache = controlPointsCache,
                bezierCache = bezierCache,
                signatureBitmap = signatureBitmap,
                onDrawVersionIncrement = onDrawVersionIncrement
            )
        }

        pointsCount == 1 -> {
            val firstPoint = state.currentPoints[0]
            state.addCurrentPoint(TimedPoint(firstPoint.x, firstPoint.y))
        }
    }
}

/**
 * Processes and draws a Bézier curve for the current stroke.
 */
private fun processBezierCurve(
    state: SignaturePadState,
    paint: Paint,
    penMinWidthPx: Float,
    penMaxWidthPx: Float,
    controlPointsCache: ControlTimedPoints,
    bezierCache: Bezier,
    signatureBitmap: () -> Bitmap?,
    onDrawVersionIncrement: () -> Unit
) {
    val tmp1 = calculateControlPoints(
        state.currentPoints[0],
        state.currentPoints[1],
        state.currentPoints[2],
        controlPointsCache
    )
    val c2 = tmp1.c2

    val tmp2 = calculateControlPoints(
        state.currentPoints[1],
        state.currentPoints[2],
        state.currentPoints[3],
        controlPointsCache
    )
    val c3 = tmp2.c1

    val curve = bezierCache.set(
        state.currentPoints[1],
        c2,
        c3,
        state.currentPoints[2]
    )

    var velocity = curve.endPoint.velocityFrom(curve.startPoint)
    velocity = if (velocity.isNaN()) 0f else velocity

    velocity = state.velocityFilterWeight * velocity +
            (1 - state.velocityFilterWeight) * state.lastVelocity

    // Calculate target width based on filtered velocity
    val targetWidth = state.calculateStrokeWidth(
        velocity,
        penMinWidthPx,
        penMaxWidthPx
    )

    // Apply EMA filter to width for smoother transitions (ink flow inertia)
    val newWidth = if (state.lastWidth > 0f) {
        // Smooth transition from previous width
        state.widthFilterWeight * targetWidth +
        (1 - state.widthFilterWeight) * state.lastWidth
    } else {
        // First sample, use target width directly
        targetWidth
    }

    state.addCurrentCurve(
        StrokeCurve(
            bezier = Bezier().set(
                curve.startPoint,
                curve.control1,
                curve.control2,
                curve.endPoint
            ),
            startWidth = state.lastWidth,
            endWidth = newWidth
        )
    )

    signatureBitmap()?.let { bitmap ->
        val canvas = Canvas(bitmap)
        drawBezierCurve(
            canvas = canvas,
            paint = paint,
            curve = curve,
            startWidth = state.lastWidth,
            endWidth = newWidth
        )
        onDrawVersionIncrement()
    }

    state.updateVelocity(velocity)
    state.updateWidth(newWidth)
    state.removeFirstCurrentPoint()
}

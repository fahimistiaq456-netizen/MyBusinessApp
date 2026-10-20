package com.example.mybusinessapp.sidepanel

import android.app.Activity
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.sqrt

private val RAINBOW_COLORS = listOf(
    Color(0xFF7F3FF2), Color(0xFF3F8EF2), Color(0xFF3FF2C5),
    Color(0xFFF2E23F), Color(0xFFF29F3F), Color(0xFFF23F55),
    Color.Transparent
)

@Composable
fun SidePanelLayout(
    modifier: Modifier = Modifier,
    state: SidePanelStateHolder,
    arrangement: SidePanelArrangement = SidePanelArrangement.End,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    dragHandleContent: @Composable (isExpanded: Boolean, progress: Float) -> Unit,
    content: @Composable (Float) -> Unit
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val scope = rememberCoroutineScope()

    val outlineShape = remember(state.arcProgress, arrangement, state.animatedWidth) {
        ArcShape(state.arcProgress, arrangement, state.animatedWidth, state.currentState)
    }

    Layout(
        content = {
            Box(
                modifier = modifier
                    .fillMaxHeight()
                    .background(color = containerColor, shape = outlineShape)
            ) {
                content(state.arcProgress)
            }

            if (state.currentState == SidePanelState.Collapsed && state.arcProgress > 0.01f) {
                RainbowAnimationCanvas(state, outlineShape)
            } else {
                Spacer(Modifier)
            }

            DragHandle(state, arrangement, dragHandleContent, scope)
        }
    ) { measurables, constraints ->
        val sheetPlaceable = measurables[0].measure(constraints.copy(minWidth = 0, maxWidth = constraints.maxWidth))
        val canvasPlaceable = measurables[1].measure(
            constraints.copy(
                minWidth = sheetPlaceable.width,
                maxWidth = sheetPlaceable.width,
                minHeight = sheetPlaceable.height,
                maxHeight = sheetPlaceable.height
            )
        )
        val dragHandlePlaceable = measurables[2].measure(constraints)

        scope.launch { state.setMeasuredWidth(sheetPlaceable.width.toFloat()) }

        layout(constraints.maxWidth, constraints.maxHeight) {
            val positions = calculatePositions(
                arrangement, state.animatedWidth.toInt(), screenWidthPx.toInt(),
                sheetPlaceable.width, dragHandlePlaceable.width,
                constraints.maxHeight, sheetPlaceable.height, dragHandlePlaceable.height
            )
            sheetPlaceable.place(positions.sheetX, positions.sheetY)
            canvasPlaceable.place(positions.sheetX, positions.sheetY)
            dragHandlePlaceable.place(positions.dragHandleX, positions.dragHandleY)
        }
    }
}

@Composable
private fun RainbowAnimationCanvas(state: SidePanelStateHolder, outlineShape: ArcShape) {
    Canvas(
        modifier = Modifier
            .fillMaxHeight()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    ) {
        drawRainbowAnimation(state, outlineShape, size, layoutDirection)
    }
}

@Composable
private fun DragHandle(
    state: SidePanelStateHolder,
    arrangement: SidePanelArrangement,
    dragHandleContent: @Composable (Boolean, Float) -> Unit,
    scope: CoroutineScope
) {
    var dragHandleRect by remember { mutableStateOf(android.graphics.Rect(0, 0, 0, 0)) }

    ExcludeDragHandleRectFromGesture(dragHandleRect)

    Box(
        modifier = Modifier
            .onGloballyPositioned {
                val pos = it.localToWindow(Offset.Zero)
                dragHandleRect = android.graphics.Rect(
                    pos.x.toInt(), pos.y.toInt(),
                    (pos.x + it.size.width).toInt(), (pos.y + it.size.height).toInt()
                )
            }
            .pointerInput(state) {
                detectDragGestures(
                    onDragEnd = { scope.launch { state.handleDragEnd() } },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            state.handleDrag(if (arrangement == SidePanelArrangement.End) -dragAmount.x else dragAmount.x)
                        }
                    }
                )
            }
    ) {
        dragHandleContent(state.currentState == SidePanelState.Expanded, state.arcProgress)
    }
}

enum class SidePanelState { Expanded, Collapsed }
enum class SidePanelArrangement { Start, End }

@Stable
class SidePanelStateHolder(initialState: SidePanelState, private val animationSpec: FiniteAnimationSpec<Float>) {
    var currentState by mutableStateOf(initialState)
        private set

    private val sheetWidth = Animatable(0f)
    private var measuredSheetWidthPx by mutableFloatStateOf(0f)

    val animatedWidth: Float get() = sheetWidth.value
    val arcProgress: Float get() = if (measuredSheetWidthPx > 0) (sheetWidth.value / measuredSheetWidthPx).coerceIn(0f, 1f) else 0f

    suspend fun setMeasuredWidth(width: Float) {
        if (measuredSheetWidthPx != width) {
            measuredSheetWidthPx = width
            sheetWidth.snapTo(if (currentState == SidePanelState.Expanded) width else 0f)
        }
    }

    suspend fun toggle() {
        val target = if (currentState == SidePanelState.Expanded) 0f else measuredSheetWidthPx
        currentState = if (currentState == SidePanelState.Expanded) SidePanelState.Collapsed else SidePanelState.Expanded
        sheetWidth.animateTo(target, animationSpec)
    }

    suspend fun collapse() {
        if (currentState == SidePanelState.Expanded) toggle()
    }

    suspend fun handleDrag(delta: Float) {
        sheetWidth.snapTo((sheetWidth.value + delta).coerceIn(0f, measuredSheetWidthPx))
    }

    suspend fun handleDragEnd() {
        val target = if (sheetWidth.value > measuredSheetWidthPx * 0.5f) measuredSheetWidthPx else 0f
        currentState = if (target > 0) SidePanelState.Expanded else SidePanelState.Collapsed
        sheetWidth.animateTo(target, animationSpec)
    }
}

@Composable
fun rememberSidePanelState(
    initialState: SidePanelState = SidePanelState.Collapsed,
    animationSpec: FiniteAnimationSpec<Float> = spring(dampingRatio = 0.8f, stiffness = 300f)
): SidePanelStateHolder {
    return remember { SidePanelStateHolder(initialState, animationSpec) }
}

class ArcShape(
    private val progress: Float,
    private val arrangement: SidePanelArrangement,
    private val maxArcHeight: Float,
    private val state: SidePanelState
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val width = size.width * progress

        if (width <= 0) return Outline.Generic(path)

        val isEnd = arrangement == SidePanelArrangement.End
        val x = if (isEnd) size.width - width else width

        path.moveTo(if (isEnd) size.width else 0f, 0f)
        path.lineTo(if (isEnd) size.width else 0f, size.height)
        path.lineTo(x, size.height)
        path.cubicTo(x + (if (isEnd) -100f else 100f) * progress, size.height / 2, x, size.height / 2, x, 0f)
        path.close()

        return Outline.Generic(path)
    }
}

private fun DrawScope.drawRainbowAnimation(state: SidePanelStateHolder, shape: ArcShape, size: Size, ld: LayoutDirection) {
    val outline = shape.createOutline(size, ld, this)
    val path = (outline as? Outline.Generic)?.path ?: return

    clipPath(path) {
        val center = Offset(size.width / 2, size.height / 2)
        val maxRadius = sqrt(size.width * size.width + size.height * size.height)

        RAINBOW_COLORS.forEachIndexed { index, color ->
            if (color != Color.Transparent) {
                drawCircle(color, maxRadius * state.arcProgress - (index * 50), center)
            }
        }
    }
}

@Composable
private fun ExcludeDragHandleRectFromGesture(rect: android.graphics.Rect) {
    val context = LocalContext.current
    LaunchedEffect(rect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            (context as? Activity)?.window?.decorView?.systemGestureExclusionRects = listOf(rect)
        }
    }
}

private data class LayoutPositions(val sheetX: Int, val sheetY: Int, val dragHandleX: Int, val dragHandleY: Int)

private fun calculatePositions(
    arrangement: SidePanelArrangement, currentW: Int, screenW: Int, sheetW: Int,
    handleW: Int, maxH: Int, sheetH: Int, handleH: Int
): LayoutPositions {
    val isEnd = arrangement == SidePanelArrangement.End
    val sheetX = if (isEnd) screenW - currentW else currentW - sheetW
    val handleX = if (isEnd) screenW - currentW - (handleW / 2) else currentW - (handleW / 2)
    return LayoutPositions(sheetX, (maxH - sheetH) / 2, handleX, (maxH - handleH) / 2)
}

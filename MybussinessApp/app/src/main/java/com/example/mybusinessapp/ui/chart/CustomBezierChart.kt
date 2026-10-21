package com.example.mybusinessapp.ui.chart

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mybusinessapp.ui.FluidGlassCard
import kotlin.math.roundToInt

data class ChartPoint(
    val label: String,
    val value: Double,
    val displayValue: String
)

@Composable
fun InteractiveBezierChart(
    points: List<ChartPoint> = listOf(
        ChartPoint("Mon", 11200.0, "$11,200"),
        ChartPoint("Tue", 12450.0, "$12,450"),
        ChartPoint("Wed", 10800.0, "$10,800"),
        ChartPoint("Thu", 13900.0, "$13,900"),
        ChartPoint("Fri", 13100.0, "$13,100"),
        ChartPoint("Sat", 15400.0, "$15,400"),
        ChartPoint("Sun", 14230.5, "$14,230.50")
    ),
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    gradientColors: List<Color> = listOf(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
        Color.Transparent
    )
) {
    var scrubberX by remember { mutableStateOf<Float?>(null) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing)
        )
    }

    val minValue = remember(points) { points.minOf { it.value } * 0.9 }
    val maxValue = remember(points) { points.maxOf { it.value } * 1.05 }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .pointerInput(points) {
                detectDragGestures(
                    onDragStart = { offset ->
                        scrubberX = offset.x.coerceIn(0f, size.width.toFloat())
                    },
                    onDragEnd = {
                        scrubberX = null
                        selectedIndex = null
                    },
                    onDragCancel = {
                        scrubberX = null
                        selectedIndex = null
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        scrubberX = change.position.x.coerceIn(0f, size.width.toFloat())
                    }
                )
            }
            .pointerInput(points) {
                detectTapGestures(
                    onPress = { offset ->
                        scrubberX = offset.x.coerceIn(0f, size.width.toFloat())
                        tryAwaitRelease()
                        scrubberX = null
                        selectedIndex = null
                    }
                )
            }
    ) {
        val totalWidth = constraints.maxWidth.toFloat()
        val totalHeight = constraints.maxHeight.toFloat()
        val paddingBottom = 40f
        val chartHeight = totalHeight - paddingBottom

        // Calculate selected index based on scrubberX
        val stepX = totalWidth / (points.size - 1)
        if (scrubberX != null && points.isNotEmpty()) {
            val idx = (scrubberX!! / stepX).roundToInt().coerceIn(0, points.size - 1)
            selectedIndex = idx
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            if (points.size < 2) return@Canvas

            // 1. Draw subtle horizontal gridlines
            val gridLines = 4
            for (i in 0..gridLines) {
                val y = (chartHeight / gridLines) * i
                drawLine(
                    color = Color.White.copy(alpha = 0.05f),
                    start = Offset(0f, y),
                    end = Offset(totalWidth, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // 2. Compute smooth Cubic Bezier Curve
            val coords = points.mapIndexed { index, point ->
                val x = index * stepX
                val normalizedY = ((point.value - minValue) / (maxValue - minValue)).toFloat()
                val y = chartHeight - (normalizedY * chartHeight * animProgress.value)
                Offset(x, y)
            }

            val strokePath = Path()
            val fillPath = Path()

            strokePath.moveTo(coords.first().x, coords.first().y)
            fillPath.moveTo(coords.first().x, chartHeight)
            fillPath.lineTo(coords.first().x, coords.first().y)

            for (i in 0 until coords.size - 1) {
                val p0 = coords[i]
                val p1 = coords[i + 1]

                // Cubic Bezier control points for smooth tension
                val controlPoint1 = Offset(p0.x + (p1.x - p0.x) / 2f, p0.y)
                val controlPoint2 = Offset(p0.x + (p1.x - p0.x) / 2f, p1.y)

                strokePath.cubicTo(
                    controlPoint1.x, controlPoint1.y,
                    controlPoint2.x, controlPoint2.y,
                    p1.x, p1.y
                )

                fillPath.cubicTo(
                    controlPoint1.x, controlPoint1.y,
                    controlPoint2.x, controlPoint2.y,
                    p1.x, p1.y
                )
            }

            fillPath.lineTo(coords.last().x, chartHeight)
            fillPath.close()

            // 3. Draw gradient area fill
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = gradientColors,
                    startY = 0f,
                    endY = chartHeight
                ),
                style = Fill
            )

            // 4. Draw main bezier stroke with glow
            drawPath(
                path = strokePath,
                color = lineColor.copy(alpha = 0.3f),
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawPath(
                path = strokePath,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // 5. Draw data points
            coords.forEachIndexed { index, coord ->
                val isSelected = selectedIndex == index
                val pointRadius = if (isSelected) 6.dp.toPx() else 3.5.dp.toPx()

                drawCircle(
                    color = Color(0xFF0F0518),
                    radius = pointRadius + 2.dp.toPx(),
                    center = coord
                )
                drawCircle(
                    color = if (isSelected) Color.White else lineColor,
                    radius = pointRadius,
                    center = coord
                )
            }

            // 6. Draw interactive vertical scrubber line if active
            selectedIndex?.let { idx ->
                val activePoint = coords[idx]
                drawLine(
                    color = Color.White.copy(alpha = 0.7f),
                    start = Offset(activePoint.x, 0f),
                    end = Offset(activePoint.x, chartHeight),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )

                // Highlight pulse ring
                drawCircle(
                    color = lineColor.copy(alpha = 0.35f),
                    radius = 14.dp.toPx(),
                    center = activePoint
                )
                drawCircle(
                    color = Color.White,
                    radius = 5.dp.toPx(),
                    center = activePoint
                )
            }
        }

        // 7. Interactive Floating Scrubber Tooltip
        selectedIndex?.let { idx ->
            val point = points[idx]
            val xPos = (idx * stepX).coerceIn(40f, totalWidth - 140f)

            Box(
                modifier = Modifier
                    .offset { IntOffset(xPos.roundToInt(), 0) }
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1E0B36).copy(alpha = 0.95f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "${point.label} • Live Scrubber",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = point.displayValue,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }

        // 8. Bottom X-axis labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEachIndexed { index, p ->
                val isSelected = selectedIndex == index
                Text(
                    text = p.label,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

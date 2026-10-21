package com.example.mybusinessapp.ui.chart

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdsClick
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AnimatedRadialProgress(
    targetPercentage: Float = 0.85f,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 14.dp,
    trackColor: Color = Color.White.copy(alpha = 0.1f),
    startColor: Color = Color(0xFFFF4D4D),
    endColor: Color = Color(0xFFFFB74D),
    showInnerDetails: Boolean = true
) {
    val progressAnim = remember { Animatable(0f) }

    LaunchedEffect(targetPercentage) {
        progressAnim.animateTo(
            targetValue = targetPercentage,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Box(
        modifier = modifier.size(160.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val diameter = size.minDimension - strokePx
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)

            // Background circle track
            drawArc(
                color = trackColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Active animated gradient progress arc
            val sweepAngle = 270f * progressAnim.value
            if (sweepAngle > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(startColor, endColor, startColor),
                        center = center
                    ),
                    startAngle = 135f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        if (showInnerDetails) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.AdsClick,
                    contentDescription = null,
                    tint = endColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${(progressAnim.value * 100).toInt()}%",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "TARGET",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.6f),
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

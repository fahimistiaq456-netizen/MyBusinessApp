package com.example.mybusinessapp.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.mybusinessapp.ui.theme.GradientGlassBorder

// 1. THE FLUID ANIMATED BACKGROUND
@Composable
fun FluidBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val bgBase = MaterialTheme.colorScheme.background

    Box(modifier = modifier.fillMaxSize().background(bgBase)) {
        // Top Left Ambient Glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                            Color.Transparent
                        ),
                        center = Offset(-200f, -200f),
                        radius = 1200f
                    )
                )
        )
        // Bottom Right Ambient Glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                            Color.Transparent
                        ),
                        center = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                        radius = 1200f
                    )
                )
        )

        // Content overlay
        content()
    }
}

// 2. THE GLASS CARD WITH 3D TILT & MATTE FINISH
@Composable
fun FluidGlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // 3D Tilt Effect Values
    val tiltX by animateFloatAsState(targetValue = if (isPressed) 8f else 0f, animationSpec = tween(150), label = "tiltX")
    val tiltY by animateFloatAsState(targetValue = if (isPressed) -5f else 0f, animationSpec = tween(150), label = "tiltY")
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.97f else 1f, animationSpec = tween(150), label = "scale")

    val containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f) 
    val borderColor = GradientGlassBorder

    val cardModifier = modifier
        .graphicsLayer {
            rotationX = tiltX
            rotationY = tiltY
            scaleX = scale
            scaleY = scale
            cameraDistance = 12f * density
        }
        .shadow(
            elevation = if (isPressed) 4.dp else 16.dp,
            shape = shape,
            ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            spotColor = Color.Black.copy(alpha = 0.6f)
        )
        .clip(shape)
        .background(
            Brush.verticalGradient(
                listOf(
                    containerColor,
                    containerColor.copy(alpha = 0.5f)
                )
            )
        )
        .background(
            Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = 0.12f), Color.Transparent),
                start = Offset(0f, 0f),
                end = Offset(400f, 400f)
            )
        )
        .border(1.2.dp, borderColor, shape)

    Box(
        modifier = if (onClick != null) {
            cardModifier.clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
        } else {
            cardModifier
        }
    ) {
        content()
    }
}

// 3. SOFT 3D ICON (MATTE FINISH & AMBIENT SHADOWS)
@Composable
fun Soft3DIcon(
    imageVector: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp
) {
    val density = LocalDensity.current
    val sizePx = with(density) { size.toPx() }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Soft Ambient Shadow Layer
        Canvas(modifier = Modifier.size(size)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.25f), Color.Transparent),
                    center = center.copy(y = center.y + 10f),
                    radius = sizePx / 1.8f
                )
            )
        }

        // Matte 3D Body
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
                .graphicsLayer {
                    shadowElevation = 8f
                    shape = CircleShape
                    clip = true
                }
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            color.copy(alpha = 0.95f),
                            color.copy(alpha = 0.7f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(100f, 100f)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Inner Highlight for 3D depth
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.25f), Color.Transparent),
                            center = Offset(0f, 0f),
                            radius = sizePx
                        )
                    )
            )

            // The Icon itself
            Icon(
                imageVector = imageVector,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

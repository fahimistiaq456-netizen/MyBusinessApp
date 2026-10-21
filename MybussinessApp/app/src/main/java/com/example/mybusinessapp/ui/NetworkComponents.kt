package com.example.mybusinessapp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mybusinessapp.network.NetworkStatus
import com.example.mybusinessapp.network.PingStatus

/**
 * Real-time status pill (Connected / Unreachable with round-trip latency in ms)
 * displayed next to the backend endpoint field.
 */
@Composable
fun ConnectionStatusPill(
    status: PingStatus,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val (bgGradient, borderColor, contentColor, _, text) = when (status) {
        is PingStatus.Pinging -> {
            PillStyle(
                Brush.horizontalGradient(listOf(Color(0x33F59E0B), Color(0x22D97706))),
                Color(0x88F59E0B),
                Color(0xFFFBBF24),
                Icons.Rounded.Sync,
                "Pinging..."
            )
        }
        is PingStatus.Connected -> {
            PillStyle(
                Brush.horizontalGradient(listOf(Color(0x3310B981), Color(0x22059669))),
                Color(0x8810B981),
                Color(0xFF34D399),
                Icons.Rounded.CheckCircle,
                "Connected • ${status.latencyMs}ms"
            )
        }
        is PingStatus.Unreachable -> {
            PillStyle(
                Brush.horizontalGradient(listOf(Color(0x33EF4444), Color(0x22DC2626))),
                Color(0x88EF4444),
                Color(0xFFF87171),
                Icons.Rounded.ErrorOutline,
                "Unreachable"
            )
        }
        is PingStatus.Idle -> {
            PillStyle(
                Brush.horizontalGradient(listOf(Color(0x2264748B), Color(0x11475569))),
                Color(0x4464748B),
                Color(0xFF94A3B8),
                Icons.Rounded.Dns,
                "Check Ping"
            )
        }
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable { onRetry() },
        shape = RoundedCornerShape(50),
        color = Color.Transparent,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier
                .background(bgGradient)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(
                            if (status is PingStatus.Pinging) contentColor.copy(alpha = pulseAlpha)
                            else contentColor
                        )
                )
                Text(
                    text = text,
                    color = contentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private data class PillStyle(
    val bg: Brush,
    val border: Color,
    val content: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String
)

/**
 * Animated offline banner displayed when device internet connectivity is lost or unavailable.
 */
@Composable
fun OfflineBanner(
    networkStatus: NetworkStatus,
    modifier: Modifier = Modifier
) {
    val isOffline = networkStatus != NetworkStatus.Available

    AnimatedVisibility(
        visible = isOffline,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            color = Color(0xF07F1D1D), // Crimson glass
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0x88EF4444)),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.WifiOff,
                    contentDescription = "Offline",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "No Internet Connection — Working in Offline Mode",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

package app.balancebeacon.mobileandroid.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.balancebeacon.mobileandroid.ui.theme.GlassPanel

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer-alpha"
    )
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color.White.copy(alpha = alpha))
    )
}

@Composable
fun SkeletonLine(
    width: Dp = 200.dp,
    height: Dp = 14.dp,
    modifier: Modifier = Modifier
) {
    SkeletonBox(modifier = modifier.width(width).height(height))
}

@Composable
fun SkeletonCard(modifier: Modifier = Modifier) {
    GlassPanel(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SkeletonLine(width = 160.dp, height = 18.dp)
            SkeletonLine(width = 240.dp, height = 14.dp)
            SkeletonLine(width = 200.dp, height = 14.dp)
            SkeletonLine(width = 120.dp, height = 14.dp)
        }
    }
}

@Composable
fun SkeletonListScreen(
    itemCount: Int = 4,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(itemCount) {
            SkeletonCard()
        }
    }
}

@Composable
fun SkeletonFormScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SkeletonLine(width = 140.dp, height = 20.dp)
                SkeletonLine(modifier = Modifier.fillMaxWidth(), height = 48.dp)
                SkeletonLine(modifier = Modifier.fillMaxWidth(), height = 48.dp)
                SkeletonLine(width = 120.dp, height = 40.dp, modifier = Modifier)
            }
        }
        repeat(3) {
            SkeletonCard()
        }
    }
}

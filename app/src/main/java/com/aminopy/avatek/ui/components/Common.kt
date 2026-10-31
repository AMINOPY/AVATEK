package com.aminopy.avatek.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aminopy.avatek.ui.theme.AccentButton
import com.aminopy.avatek.ui.theme.Artwork
import com.aminopy.avatek.ui.theme.Emphasized
import com.aminopy.avatek.ui.theme.LocalPalette
import com.aminopy.avatek.ui.theme.LocalUi
import com.aminopy.avatek.ui.theme.bounceClick
import com.aminopy.avatek.ui.theme.glass
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Three tiny bars bouncing: shown next to the track that is currently playing. */
@Composable
fun PlayingBars(modifier: Modifier = Modifier, color: Color = LocalPalette.current.accent, playing: Boolean = true) {
    val ui = LocalUi.current
    val t = rememberInfiniteTransition(label = "bars")
    val a = t.animateFloat(0.25f, 1f, infiniteRepeatable(tween(520, easing = LinearEasing), RepeatMode.Reverse), label = "a")
    val b = t.animateFloat(0.3f, 1f, infiniteRepeatable(tween(740, easing = LinearEasing), RepeatMode.Reverse), label = "b")
    val c = t.animateFloat(0.2f, 1f, infiniteRepeatable(tween(610, easing = LinearEasing), RepeatMode.Reverse), label = "c")
    Canvas(modifier.size(16.dp)) {
        val bw = size.width / 5f
        val moving = playing && ui.animations
        val hs = if (moving) listOf(a.value, b.value, c.value) else listOf(0.35f, 0.35f, 0.35f)
        hs.forEachIndexed { i, h ->
            val bh = size.height * h
            drawRoundRect(
                color = color,
                topLeft = Offset(i * 2 * bw, size.height - bh),
                size = Size(bw, bh),
                cornerRadius = CornerRadius(bw / 2f, bw / 2f)
            )
        }
    }
}

/** Heart with a spring pop and a particle burst when it becomes active. */
@Composable
fun FavoriteButton(
    active: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val p = LocalPalette.current
    val scale = remember { Animatable(1f) }
    val burst = remember { Animatable(0f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(active) {
        if (first) { first = false; return@LaunchedEffect }
        if (active) {
            launch { burst.snapTo(0f); burst.animateTo(1f, tween(700, easing = Emphasized)) }
            scale.snapTo(0.55f)
            scale.animateTo(1f, spring(0.32f, 360f))
        } else {
            scale.snapTo(0.8f)
            scale.animateTo(1f, spring(0.5f, 500f))
        }
    }
    val heart = Color(0xFFFF4D6D)
    Box(modifier.size(size).bounceClick(onClick = onToggle), contentAlignment = Alignment.Center) {
        Canvas(Modifier.requiredSize(size * 2f)) {
            val b = burst.value
            if (b > 0.01f && b < 0.99f) {
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                for (i in 0 until 8) {
                    val ang = (i * 45f) * (PI.toFloat() / 180f)
                    val r = size.toPx() * (0.32f + 0.55f * b)
                    drawCircle(
                        color = (if (i % 2 == 0) heart else p.accent).copy(alpha = 1f - b),
                        radius = 3.dp.toPx() * (1f - b * 0.6f),
                        center = Offset(center.x + cos(ang) * r, center.y + sin(ang) * r)
                    )
                }
            }
        }
        Icon(
            imageVector = if (active) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = if (active) "حذف از علاقه‌مندی‌ها" else "افزودن به علاقه‌مندی‌ها",
            tint = if (active) heart else p.muted,
            modifier = Modifier.size(size * 0.56f).graphicsLayer { scaleX = scale.value; scaleY = scale.value }
        )
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val p = LocalPalette.current
    val ui = LocalUi.current
    val t = rememberInfiniteTransition(label = "float")
    val bob = t.animateFloat(-8f, 8f, infiniteRepeatable(tween(2600), RepeatMode.Reverse), label = "bob")
    Column(
        modifier.fillMaxWidth().padding(horizontal = 36.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(96.dp).graphicsLayer { translationY = if (ui.animations) bob.value else 0f }
                .glass(CircleShape, strength = 0.9f),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = p.accent, modifier = Modifier.size(44.dp)) }
        Text(title, color = p.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(subtitle, color = p.muted, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
        if (actionLabel != null && onAction != null) {
            AccentButton(actionLabel, onAction, Modifier.padding(top = 6.dp))
        }
    }
}

/** Standard large screen title with optional trailing glass buttons. */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    val p = LocalPalette.current
    Row(
        modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = p.text, fontSize = 28.sp, fontWeight = FontWeight.Black)
            if (subtitle != null) Text(subtitle, color = p.muted, fontSize = 12.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically, content = trailing)
    }
}

@Composable
fun CoverCard(
    title: String,
    subtitle: String,
    seed: String,
    trackId: Long,
    albumId: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 148.dp,
    shape: Shape = RoundedCornerShape(24.dp)
) {
    val p = LocalPalette.current
    Column(
        modifier.width(size).bounceClick(pressedScale = 0.95f, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.size(size).glass(shape, strength = 0.4f).padding(3.dp)) {
            Artwork(seed, trackId, albumId, Modifier.fillMaxSize(), shape = RoundedCornerShape(21.dp), sizePx = 420)
        }
        Column(Modifier.padding(horizontal = 4.dp)) {
            Text(title, color = p.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = p.muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun ToastHost(message: String?, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    var last by remember { mutableStateOf("") }
    if (message != null) last = message
    Box(modifier.fillMaxSize().statusBarsPadding().padding(top = 12.dp), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(
            visible = message != null,
            enter = slideInVertically(spring(0.6f, 420f)) { -it * 2 } + fadeIn(tween(200)),
            exit = slideOutVertically(tween(260)) { -it * 2 } + fadeOut(tween(200))
        ) {
            Text(
                last, color = p.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .glass(RoundedCornerShape(50), tint = p.accent, strength = 1f)
                    .padding(horizontal = 20.dp, vertical = 11.dp)
            )
        }
    }
}

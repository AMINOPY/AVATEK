package com.aminopy.avatek.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.aminopy.avatek.ui.theme.Emphasized
import com.aminopy.avatek.ui.theme.LocalPalette
import com.aminopy.avatek.ui.theme.LocalUi
import com.aminopy.avatek.ui.theme.bounceClick
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

/** Continuous rotation angle that freezes when paused (like a real platter). */
@Composable
fun rememberSpin(playing: Boolean, periodMs: Int = 20000): State<Float> {
    val ui = LocalUi.current
    val angle = remember { Animatable(0f) }
    LaunchedEffect(playing, ui.animations) {
        if (playing && ui.animations) {
            while (isActive) angle.animateTo(angle.value + 360f, tween(periodMs, easing = LinearEasing))
        }
    }
    return angle.asState()
}

@Composable
fun PlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 78.dp
) {
    val p = LocalPalette.current
    val ui = LocalUi.current
    val t = rememberInfiniteTransition(label = "halo")
    val halo = t.animateFloat(0f, 1f, infiniteRepeatable(tween(1900, easing = Emphasized)), label = "h")
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        if (isPlaying && ui.animations) {
            Box(
                Modifier.fillMaxSize().graphicsLayer {
                    val s = 1f + 0.38f * halo.value
                    scaleX = s; scaleY = s; alpha = 0.45f * (1f - halo.value)
                }.background(p.accent, CircleShape)
            )
        }
        Box(
            Modifier.fillMaxSize()
                .bounceClick(pressedScale = 0.9f, onClick = onClick)
                .shadow(22.dp, CircleShape, ambientColor = p.accent, spotColor = p.accent)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(p.accent, p.accentDeep))),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = isPlaying,
                transitionSpec = {
                    (scaleIn(spring(0.55f, 520f), initialScale = 0.4f) + fadeIn(tween(120))) togetherWith
                        (scaleOut(tween(110), targetScale = 0.4f) + fadeOut(tween(90)))
                },
                label = "playPause"
            ) { playing ->
                Icon(
                    if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (playing) "توقف" else "پخش",
                    tint = Color(0xFF14100A),
                    modifier = Modifier.size(size * 0.48f)
                )
            }
        }
    }
}

/**
 * Decorative rhythm bars. It is an animation driven by the playing state, not an analysis of the audio signal.
 */
@Composable
fun RhythmVisualizer(playing: Boolean, modifier: Modifier = Modifier, bars: Int = 40) {
    val ui = LocalUi.current
    val p = LocalPalette.current
    val energy by animateFloatAsState(if (playing) 1f else 0.05f, tween(800), label = "energy")
    val time = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(playing, ui.animations) {
        if (playing && ui.animations) {
            var last = 0L
            while (isActive) {
                androidx.compose.runtime.withFrameNanos { n ->
                    if (last == 0L) last = n
                    time.floatValue += (n - last) / 1_000_000_000f
                    last = n
                }
            }
        }
    }
    Canvas(modifier) {
        val t = time.floatValue
        val gap = 3.dp.toPx()
        val bw = (size.width - gap * (bars - 1)) / bars
        for (i in 0 until bars) {
            val x = i / (bars - 1f)
            val envelope = 0.5f + 0.5f * sin(x * PI.toFloat())
            val v = (sin(t * 3.1f + i * 0.55f) * 0.5f + 0.5f) * 0.55f + (sin(t * 5.3f + i * 1.37f) * 0.5f + 0.5f) * 0.45f
            val h = max(size.height * (0.06f + 0.94f * v * envelope * energy), 3.dp.toPx())
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(p.accent, p.secondary.copy(alpha = 0.25f)),
                    startY = size.height - h, endY = size.height
                ),
                topLeft = Offset(i * (bw + gap), size.height - h),
                size = Size(bw, h),
                cornerRadius = CornerRadius(bw / 2f, bw / 2f)
            )
        }
    }
}

/** Seek bar with a glowing thumb that grows while dragging. Always laid out left-to-right. */
@Composable
fun GlowSeekBar(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val p = LocalPalette.current
    var drag by remember { mutableStateOf<Float?>(null) }
    val fraction = drag ?: if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val grow by animateFloatAsState(if (drag != null) 1f else 0f, spring(0.5f, 500f), label = "grow")
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier.fillMaxWidth().height(38.dp)
                .pointerInput(durationMs) {
                    detectTapGestures { o -> if (durationMs > 0) onSeek((o.x / size.width * durationMs).toLong()) }
                }
                .pointerInput(durationMs) {
                    detectHorizontalDragGestures(
                        onDragStart = { o -> drag = (o.x / size.width).coerceIn(0f, 1f) },
                        onHorizontalDrag = { change, _ -> drag = (change.position.x / size.width).coerceIn(0f, 1f) },
                        onDragEnd = { drag?.let { f -> if (durationMs > 0) onSeek((f * durationMs).toLong()) }; drag = null },
                        onDragCancel = { drag = null }
                    )
                }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val cy = size.height / 2f
                val th = 6.dp.toPx() + 3.dp.toPx() * grow
                val r = CornerRadius(th / 2f, th / 2f)
                drawRoundRect(Color.White.copy(alpha = 0.15f), Offset(0f, cy - th / 2f), Size(size.width, th), r)
                val x = size.width * fraction
                if (x > 0.5f) {
                    drawRoundRect(
                        Brush.horizontalGradient(listOf(p.accentDeep, p.accent), startX = 0f, endX = max(x, 1f)),
                        Offset(0f, cy - th / 2f), Size(x, th), r
                    )
                }
                drawCircle(p.accent.copy(alpha = 0.22f), radius = (11.dp + 9.dp * grow).toPx(), center = Offset(x, cy))
                drawCircle(Color.White, radius = (5.5.dp + 3.dp * grow).toPx(), center = Offset(x, cy))
            }
        }
    }
}

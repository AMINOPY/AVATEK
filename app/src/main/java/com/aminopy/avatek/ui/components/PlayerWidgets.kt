package com.aminopy.avatek.ui.components

import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import com.aminopy.avatek.ui.fa
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min
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
 * Rhythm bars driven by the real audio: the playback engine runs an FFT over the PCM being played
 * ([com.aminopy.avatek.playback.AudioLevels]) and each bar follows one frequency band; the whole row pulses on beats.
 */
@Composable
fun RhythmVisualizer(playing: Boolean, modifier: Modifier = Modifier, bars: Int = 40) {
    val ui = LocalUi.current
    val p = LocalPalette.current
    val energy by animateFloatAsState(if (playing) 1f else 0f, tween(500), label = "energy")
    val tick = remember { mutableFloatStateOf(0f) }
    val shown = remember(bars) { FloatArray(bars) }
    LaunchedEffect(playing, ui.animations) {
        if (ui.animations) {
            var last = 0L
            while (isActive) {
                androidx.compose.runtime.withFrameNanos { n ->
                    if (last == 0L) last = n
                    tick.floatValue += (n - last) / 1_000_000_000f
                    last = n
                }
            }
        }
    }
    Canvas(modifier) {
        @Suppress("UNUSED_VARIABLE") val t = tick.floatValue
        val levels = com.aminopy.avatek.playback.AudioLevels
        val src = levels.bands
        val beat = levels.beat * energy
        val gap = 3.dp.toPx()
        val bw = (size.width - gap * (bars - 1)) / bars
        for (i in 0 until bars) {
            val band = (i * src.size / bars).coerceIn(0, src.size - 1)
            val target = (src[band] * 1.15f).coerceIn(0f, 1f) * energy
            shown[i] += (target - shown[i]) * (if (target > shown[i]) 0.6f else 0.25f)
            val h = max(size.height * (0.05f + 0.95f * shown[i] * (1f + 0.12f * beat)).coerceAtMost(1f), 3.dp.toPx())
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(p.accent.copy(alpha = 0.7f + 0.3f * beat), p.secondary.copy(alpha = 0.25f)),
                    startY = size.height - h, endY = size.height
                ),
                topLeft = Offset(i * (bw + gap), size.height - h),
                size = Size(bw, h),
                cornerRadius = CornerRadius(bw / 2f, bw / 2f)
            )
        }
    }
}

/**
 * Seek bar drawn as the track's real waveform. Dragging magnifies the bars around the finger, leaves a streak
 * from where you started, and shows how many seconds you are jumping; double-tap the left/right half for -/+10s.
 */
@Composable
fun WaveSeekBar(
    positionMs: Long,
    durationMs: Long,
    peaks: FloatArray?,
    playing: Boolean,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val p = LocalPalette.current
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    var drag by remember { mutableStateOf<Float?>(null) }
    var origin by remember { mutableFloatStateOf(0f) }
    val frac = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val fraction = drag ?: frac
    val fracNow by androidx.compose.runtime.rememberUpdatedState(frac)
    val posNow by androidx.compose.runtime.rememberUpdatedState(positionMs)
    val grow by animateFloatAsState(if (drag != null) 1f else 0f, spring(0.55f, 420f), label = "wgrow")
    val tick = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(playing) {
        while (isActive) { androidx.compose.runtime.withFrameNanos { tick.floatValue = it / 1e9f } }
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier.fillMaxWidth().height(76.dp)
                .pointerInput(durationMs) {
                    detectTapGestures(
                        onDoubleTap = { o ->
                            if (durationMs > 0) {
                                val d = if (o.x < size.width / 2f) -10_000L else 10_000L
                                onSeek((posNow + d).coerceIn(0L, durationMs))
                            }
                        },
                        onTap = { o -> if (durationMs > 0) onSeek((o.x / size.width * durationMs).toLong()) }
                    )
                }
                .pointerInput(durationMs) {
                    detectHorizontalDragGestures(
                        onDragStart = { o -> origin = fracNow; drag = (o.x / size.width).coerceIn(0f, 1f) },
                        onHorizontalDrag = { change, _ -> drag = (change.position.x / size.width).coerceIn(0f, 1f) },
                        onDragEnd = { drag?.let { f -> if (durationMs > 0) onSeek((f * durationMs).toLong()) }; drag = null },
                        onDragCancel = { drag = null }
                    )
                }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                @Suppress("UNUSED_VARIABLE") val t = tick.floatValue
                val count = peaks?.size ?: 96
                val beat = if (playing) com.aminopy.avatek.playback.AudioLevels.beat else 0f
                val gap = 2.dp.toPx()
                val bw = (size.width - gap * (count - 1)) / count
                val mid = size.height / 2f
                val maxH = size.height * 0.82f
                if (grow > 0.01f) {
                    val x0 = origin * size.width
                    val x1 = fraction * size.width
                    drawRect(
                        Brush.horizontalGradient(
                            if (x1 >= x0) listOf(Color.Transparent, p.accent.copy(alpha = 0.22f * grow)) else listOf(p.accent.copy(alpha = 0.22f * grow), Color.Transparent),
                            startX = min(x0, x1), endX = max(x0, x1)
                        ),
                        Offset(min(x0, x1), 0f), Size(abs(x1 - x0), size.height)
                    )
                }
                for (i in 0 until count) {
                    val x = i / (count - 1f)
                    val base = peaks?.get(i) ?: 0.18f
                    val d = (x - fraction) / 0.07f
                    val lens = 1f + 0.9f * grow * exp(-d * d)
                    val nearHead = exp(-((x - fraction) / 0.025f).let { it * it })
                    val h = (maxH * base * lens * (1f + 0.25f * beat * nearHead)).coerceIn(3.dp.toPx(), size.height)
                    val played = x <= fraction
                    drawRoundRect(
                        if (played) p.accent.copy(alpha = 0.55f + 0.45f * nearHead) else Color.White.copy(alpha = 0.22f),
                        Offset(i * (bw + gap), mid - h / 2f), Size(bw, h), CornerRadius(bw / 2f, bw / 2f)
                    )
                }
                val hx = size.width * fraction
                drawCircle(p.accent.copy(alpha = 0.18f + 0.2f * beat), radius = (10.dp + 8.dp * grow + 7.dp * beat).toPx(), center = Offset(hx, mid))
                drawRoundRect(Color.White, Offset(hx - 1.5.dp.toPx(), 2.dp.toPx()), Size(3.dp.toPx(), size.height - 4.dp.toPx()), CornerRadius(2.dp.toPx()))
                if (grow > 0.05f && durationMs > 0) {
                    val sec = ((fraction - origin) * durationMs / 1000f).toInt()
                    val label = (if (sec >= 0) "+" else "−") + abs(sec).fa() + "ث"
                    val r = measurer.measure(label, androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
                    val bx = (hx - r.size.width / 2f - 8.dp.toPx()).coerceIn(0f, size.width - r.size.width - 16.dp.toPx())
                    drawRoundRect(p.accentDeep.copy(alpha = 0.92f * grow), Offset(bx, 0f), Size(r.size.width + 16.dp.toPx(), r.size.height + 6.dp.toPx()), CornerRadius(12.dp.toPx()))
                    drawText(r, topLeft = Offset(bx + 8.dp.toPx(), 3.dp.toPx()), alpha = grow)
                }
            }
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

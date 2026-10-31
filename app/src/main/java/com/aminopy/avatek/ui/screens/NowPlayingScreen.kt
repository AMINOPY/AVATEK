package com.aminopy.avatek.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.aminopy.avatek.state.AvatekViewModel
import com.aminopy.avatek.ui.AppUi
import com.aminopy.avatek.ui.Sheet
import com.aminopy.avatek.ui.components.FavoriteButton
import com.aminopy.avatek.ui.components.WaveSeekBar
import com.aminopy.avatek.ui.components.PlayPauseButton
import com.aminopy.avatek.ui.components.RhythmVisualizer
import com.aminopy.avatek.ui.fa
import com.aminopy.avatek.ui.faDuration
import com.aminopy.avatek.ui.nav.Route
import com.aminopy.avatek.ui.theme.Artwork
import com.aminopy.avatek.ui.components.rememberSpin
import com.aminopy.avatek.ui.theme.AuroraBackground
import com.aminopy.avatek.ui.theme.GlassIconButton
import com.aminopy.avatek.ui.theme.LocalPalette
import com.aminopy.avatek.ui.theme.LocalUi
import com.aminopy.avatek.ui.theme.bounceClick
import com.aminopy.avatek.ui.theme.glass
import com.aminopy.avatek.ui.theme.rememberAmbient
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min

@Composable
fun NowPlayingScreen(vm: AvatekViewModel, ui: AppUi) {
    val p = LocalPalette.current
    val settings = LocalUi.current
    val track = vm.currentTrack
    if (track == null) {
        LaunchedEffect(Unit) { ui.closePlayer() }
        return
    }
    val ambient = rememberAmbient(track.id, track.albumId, track.albumKey, p.secondary)
    val scope = rememberCoroutineScope()
    val offsetY = remember { Animatable(0f) }

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = offsetY.value
                val f = (offsetY.value / 1400f).coerceIn(0f, 0.12f)
                scaleX = 1f - f; scaleY = 1f - f
                alpha = 1f - (offsetY.value / 1600f).coerceIn(0f, 0.5f)
            }
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { d -> scope.launch { offsetY.snapTo((offsetY.value + d).coerceAtLeast(0f)) } },
                onDragStopped = { velocity ->
                    if (offsetY.value > 340f || velocity > 1800f) ui.closePlayer()
                    else scope.launch { offsetY.animateTo(0f, spring(0.7f, 380f)) }
                }
            )
    ) {
        AuroraBackground(ambient)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(0.2f), Color.Black.copy(0.55f)))))

        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 22.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // top bar
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(Icons.Rounded.KeyboardArrowDown, "بستن", { ui.closePlayer() })
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("در حال پخش از", color = p.muted, fontSize = 10.sp, letterSpacing = 1.sp)
                    Text(
                        track.album, color = p.text, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.bounceClick(pressedScale = 0.95f) {
                            ui.closePlayer(); ui.nav.push(Route.Album(track.albumKey))
                        }
                    )
                }
                GlassIconButton(Icons.Rounded.QueueMusic, "صف پخش", { ui.show(Sheet.Queue) })
            }

            // vinyl
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                val diameter = min(maxWidth.value, maxHeight.value * 0.96f).dp
                val swipe = remember { FloatHolder() }
                AnimatedContent(
                    targetState = track.id,
                    transitionSpec = {
                        val forward = vm.queueIds.indexOf(targetState) >= vm.queueIds.indexOf(initialState)
                        val dir = if (forward) 1 else -1
                        (slideInHorizontally(spring(0.8f, 300f)) { it * dir / 2 } + fadeIn(tween(260)) + scaleIn(initialScale = 0.82f)) togetherWith
                            (slideOutHorizontally(tween(260)) { -it * dir / 2 } + fadeOut(tween(200)) + scaleOut(targetScale = 0.82f)) using
                            SizeTransform(clip = false)
                    },
                    label = "vinyl"
                ) { id ->
                    val t = vm.trackById(id) ?: track
                    Box(
                        Modifier.size(diameter).pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragStart = { swipe.value = 0f },
                                onDragEnd = { if (swipe.value < -110f) vm.next() else if (swipe.value > 110f) vm.previous() },
                                onHorizontalDrag = { c, d -> c.consume(); swipe.value += d }
                            )
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        Vinyl(t.albumKey, t.id, t.albumId, vm.isPlaying, ambient, diameter)
                    }
                }
            }

            // title + favourite
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                AnimatedContent(
                    targetState = track.id,
                    modifier = Modifier.weight(1f),
                    transitionSpec = {
                        (slideInVertically(tween(380)) { it / 3 } + fadeIn(tween(320))) togetherWith
                            (slideOutVertically(tween(220)) { -it / 3 } + fadeOut(tween(160)))
                    },
                    label = "title"
                ) { id ->
                    val t = vm.trackById(id) ?: track
                    Column {
                        Text(t.title, color = p.text, fontSize = 23.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            t.artist, color = p.accent, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.bounceClick(pressedScale = 0.96f) { ui.closePlayer(); ui.nav.push(Route.Artist(t.artist)) }
                        )
                    }
                }
                FavoriteButton(track.id in vm.favorites, { vm.toggleFavorite(track.id) }, size = 52.dp)
            }

            if (settings.visualizer) {
                RhythmVisualizer(vm.isPlaying, Modifier.fillMaxWidth().height(38.dp).padding(top = 8.dp), bars = 44)
            }

            // seek
            val context = androidx.compose.ui.platform.LocalContext.current
            var peaks by remember(track.id) { mutableStateOf<FloatArray?>(null) }
            LaunchedEffect(track.id) {
                peaks = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    com.aminopy.avatek.playback.WaveformExtractor.extract(context, track.uri, alive = { true })
                }
            }
            WaveSeekBar(vm.positionMs, vm.durationMs, peaks, vm.isPlaying, { vm.seekTo(it) }, Modifier.padding(top = 4.dp))
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(faDuration(vm.positionMs), color = p.muted, fontSize = 11.sp)
                    Text(faDuration(vm.durationMs), color = p.muted, fontSize = 11.sp)
                }
            }

            // transport – always left-to-right, like a physical deck
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassIconButton(Icons.Rounded.Shuffle, "تصادفی", { vm.toggleShuffle() }, active = vm.shuffle, size = 46.dp)
                    GlassIconButton(Icons.Rounded.SkipPrevious, "قبلی", { vm.previous() }, size = 54.dp)
                    PlayPauseButton(vm.isPlaying, { vm.togglePlay() }, size = 80.dp)
                    GlassIconButton(Icons.Rounded.SkipNext, "بعدی", { vm.next() }, size = 54.dp)
                    GlassIconButton(
                        if (vm.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        "تکرار", { vm.cycleRepeat() }, active = vm.repeatMode != Player.REPEAT_MODE_OFF, size = 46.dp
                    )
                }
            }

            // quick tools
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                ToolPill(Icons.Rounded.QueueMusic, "صف ${vm.queueIds.size.fa()}") { ui.show(Sheet.Queue) }
                ToolPill(Icons.Rounded.Bedtime, vm.sleepRemainingMs?.let { faDuration(it) } ?: "خواب", active = vm.sleepRemainingMs != null) { ui.show(Sheet.Sleep) }
                ToolPill(Icons.Rounded.Speed, "${vm.speed}x".fa(), active = vm.speed != 1f) { ui.show(Sheet.Speed) }
                ToolPill(Icons.Rounded.PlaylistAdd, "پلی‌لیست") { ui.show(Sheet.AddToPlaylist(track.id)) }
            }
        }
    }
}

/** Tiny mutable float that survives recomposition without triggering it (gesture accumulator). */
private class FloatHolder { var value: Float = 0f }

@Composable
private fun ToolPill(icon: ImageVector, label: String, active: Boolean = false, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier.bounceClick(pressedScale = 0.92f, onClick = onClick)
            .glass(RoundedCornerShape(50), tint = if (active) p.accent else Color.White, strength = 0.6f)
            .padding(horizontal = 13.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, null, tint = if (active) p.accent else p.text, modifier = Modifier.size(17.dp))
        Text(label, color = if (active) p.accent else p.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/**
 * A spinning record: grooves, light sheen, and the cover as its label. A tonearm swings onto
 * the disc when playback starts and lifts away when it pauses.
 */
@Composable
private fun Vinyl(seed: String, trackId: Long, albumId: Long, playing: Boolean, glow: Color, diameter: Dp) {
    val p = LocalPalette.current
    val spin = rememberSpin(playing, 18000)
    val arm by animateFloatAsState(if (playing) 0f else -24f, spring(0.55f, 90f), label = "arm")
    Box(Modifier.size(diameter), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(diameter * 0.94f)
                .shadow(44.dp, CircleShape, ambientColor = glow, spotColor = glow)
                .graphicsLayer { rotationZ = spin.value }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val c = Offset(size.width / 2f, size.height / 2f)
                val r = size.minDimension / 2f
                drawCircle(Brush.radialGradient(listOf(Color(0xFF1B1B1F), Color(0xFF050506)), center = c, radius = r), r, c)
                var g = r * 0.34f
                while (g < r * 0.97f) {
                    drawCircle(Color.White.copy(alpha = 0.045f), g, c, style = Stroke(1f))
                    g += r * 0.028f
                }
                // light sheen across the grooves
                drawCircle(
                    Brush.sweepGradient(
                        listOf(Color.Transparent, Color.White.copy(0.16f), Color.Transparent, Color.Transparent, Color.White.copy(0.12f), Color.Transparent),
                        center = c
                    ), r, c
                )
                drawCircle(p.accent.copy(alpha = 0.5f), r, c, style = Stroke(2.dp.toPx()))
            }
            Artwork(
                seed, trackId, albumId,
                Modifier.size(diameter * 0.58f).align(Alignment.Center),
                CircleShape, sizePx = 700
            )
            Box(Modifier.size(diameter * 0.045f).align(Alignment.Center).background(Color(0xFF0A0A0B), CircleShape))
        }
        // tonearm
        Canvas(Modifier.fillMaxSize()) {
            val pivot = Offset(size.width * 0.96f, size.height * 0.05f)
            rotate(degrees = arm, pivot = pivot) {
                val head = Offset(size.width * 0.70f, size.height * 0.60f)
                drawLine(Color(0xFFCFCFD6), pivot, head, strokeWidth = 5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                drawLine(p.accent, Offset(head.x - 6.dp.toPx(), head.y - 4.dp.toPx()), Offset(head.x + 4.dp.toPx(), head.y + 10.dp.toPx()), strokeWidth = 9.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            }
            drawCircle(Color(0xFF2A2A30), 16.dp.toPx(), pivot)
            drawCircle(p.accent, 6.dp.toPx(), pivot)
        }
    }
}

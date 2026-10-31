package com.aminopy.avatek.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.aminopy.avatek.data.AccentTheme
import com.aminopy.avatek.data.AudioTrack
import com.aminopy.avatek.playback.AudioLevels
import com.aminopy.avatek.playback.WaveformExtractor
import com.aminopy.avatek.state.AvatekViewModel
import com.aminopy.avatek.ui.AppUi
import com.aminopy.avatek.ui.LocalBottomInset
import com.aminopy.avatek.ui.LocalPermissionRequester
import com.aminopy.avatek.ui.Sheet
import com.aminopy.avatek.ui.components.FavoriteButton
import com.aminopy.avatek.ui.components.RhythmVisualizer
import com.aminopy.avatek.ui.components.WaveSeekBar
import com.aminopy.avatek.ui.fa
import com.aminopy.avatek.ui.faDuration
import com.aminopy.avatek.ui.nav.Navigator
import com.aminopy.avatek.ui.nav.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

// ---- palette ------------------------------------------------------------------------------------

val WastelandPalette = AvatekPalette(
    accent = Color(0xFFD9B26A), accentDeep = Color(0xFF6B4A22), secondary = Color(0xFF6F8F4E),
    bgTop = Color(0xFF17130C), bgBottom = Color(0xFF070705),
    text = Color(0xFFEFE5D0), muted = Color(0xFFA89E8B), danger = Color(0xFFE0654F)
)

val AccentTheme.isWasteland: Boolean get() = this == AccentTheme.WASTELAND

private val Serif = FontFamily.Serif
private val Moss = Color(0xFF55703A)
private val Bark = Color(0xFF2A2215)

// ---- 3D tilt ------------------------------------------------------------------------------------

/** Card that leans toward the finger in 3D while pressed and springs back on release. */
fun Modifier.tilt3d(maxDeg: Float = 14f, onTap: (() -> Unit)? = null): Modifier = composed {
    val rx = remember { Animatable(0f) }
    val ry = remember { Animatable(0f) }
    val sc = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    this
        .graphicsLayer {
            rotationX = rx.value; rotationY = ry.value
            scaleX = sc.value; scaleY = sc.value
            cameraDistance = 14f * density
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = { o ->
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    scope.launch { ry.animateTo(((o.x - cx) / cx).coerceIn(-1f, 1f) * maxDeg, spring(0.5f, 420f)) }
                    scope.launch { rx.animateTo(-((o.y - cy) / cy).coerceIn(-1f, 1f) * maxDeg, spring(0.5f, 420f)) }
                    scope.launch { sc.animateTo(0.96f, spring(0.5f, 420f)) }
                    tryAwaitRelease()
                    scope.launch { rx.animateTo(0f, spring(0.35f, 260f)) }
                    scope.launch { ry.animateTo(0f, spring(0.35f, 260f)) }
                    scope.launch { sc.animateTo(1f, spring(0.4f, 300f)) }
                },
                onTap = { onTap?.invoke() }
            )
        }
}

// ---- living background ----------------------------------------------------------------------------

private class Ruin(val x: Float, val w: Float, val h: Float, val cut: Float, val windows: List<Offset>)

private class Spore(val x: Float, val y: Float, val speed: Float, val size: Float, val phase: Float)

/**
 * Overgrown ruined city: three parallax layers of broken towers, drifting fog, swaying vines,
 * floating spores that flare with the beat of the real audio, and a heavy vignette.
 */
@Composable
fun WastelandBackground(modifier: Modifier = Modifier.fillMaxSize()) {
    val ui = LocalUi.current
    val tick = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(ui.animations) {
        if (ui.animations) {
            while (true) androidx.compose.runtime.withFrameNanos { tick.floatValue = it / 1e9f }
        }
    }
    val layers = remember {
        val r = Random(7)
        List(3) { layer ->
            List(9 - layer) {
                val w = 0.10f + r.nextFloat() * 0.12f
                Ruin(
                    x = r.nextFloat() * 1.1f - 0.05f, w = w,
                    h = (0.16f + r.nextFloat() * 0.26f) * (1f - layer * 0.12f), cut = r.nextFloat() * 0.06f,
                    windows = List(6) { Offset(0.18f + r.nextFloat() * 0.6f, 0.15f + r.nextFloat() * 0.7f) }
                )
            }
        }
    }
    val spores = remember {
        val r = Random(11)
        List(34) { Spore(r.nextFloat(), r.nextFloat(), 0.01f + r.nextFloat() * 0.03f, 1.2f + r.nextFloat() * 2.6f, r.nextFloat() * 6.28f) }
    }
    Canvas(modifier) {
        val t = tick.floatValue
        val w = size.width
        val h = size.height
        val beat = AudioLevels.beat
        drawRect(Brush.verticalGradient(listOf(Color(0xFF15110A), Color(0xFF2B2113), Color(0xFF0A0906)), 0f, h))
        // hazy light
        drawCircle(
            Brush.radialGradient(listOf(Color(0xFFE2B26A).copy(alpha = 0.30f + 0.08f * beat), Color.Transparent), Offset(w * 0.72f, h * 0.26f), w * 0.8f),
            w * 0.8f, Offset(w * 0.72f, h * 0.26f)
        )
        val tint = listOf(Color(0xFF3A3020), Color(0xFF241D12), Color(0xFF110E09))
        for (layer in 0..2) {
            val par = sin(t * 0.18f + layer) * (5 - layer) * density
            val base = h * (0.60f + layer * 0.06f)
            for (r in layers[layer]) {
                val bw = r.w * w
                val bx = r.x * w + par
                val top = base - r.h * h
                val path = Path().apply {
                    moveTo(bx, h)
                    lineTo(bx, top + r.cut * h)
                    lineTo(bx + bw * 0.45f, top)
                    lineTo(bx + bw * 0.7f, top + r.cut * h * 1.6f)
                    lineTo(bx + bw, top + r.cut * h * 0.4f)
                    lineTo(bx + bw, h)
                    close()
                }
                drawPath(path, tint[layer])
                if (layer < 2) for (o in r.windows) {
                    drawRect(Color.Black.copy(alpha = 0.55f), Offset(bx + o.x * bw, top + o.y * (base - top)), Size(bw * 0.12f, (base - top) * 0.06f))
                }
                // moss on the broken top
                drawPath(path, Brush.verticalGradient(listOf(Moss.copy(alpha = 0.28f - layer * 0.07f), Color.Transparent), top, top + 70.dp.toPx()))
            }
        }
        // fog
        for (i in 0..2) {
            val fx = ((t * (0.012f + i * 0.006f) + i * 0.37f) % 1.4f - 0.2f) * w
            val fy = h * (0.55f + i * 0.13f)
            drawCircle(
                Brush.radialGradient(listOf(Color(0xFF9A8C66).copy(alpha = 0.10f), Color.Transparent), Offset(fx, fy), w * 0.7f),
                w * 0.7f, Offset(fx, fy)
            )
        }
        // vines
        vine(this, w * 0.04f, h * 0.30f, t, 0f, 1f)
        vine(this, w * 0.20f, h * 0.17f, t, 1.7f, 1f)
        vine(this, w * 0.93f, h * 0.36f, t, 3.1f, -1f)
        vine(this, w * 0.78f, h * 0.14f, t, 4.4f, -1f)
        // spores
        for (s in spores) {
            val y = ((s.y - t * s.speed) % 1f + 1f) % 1f
            val x = s.x + sin(t * 0.4f + s.phase) * 0.03f
            val a = (0.25f + 0.55f * (0.5f + 0.5f * sin(t * 2.2f + s.phase))) * (0.7f + 0.8f * beat)
            val c = if (s.phase > 3.2f) Color(0xFFE8C77F) else Color(0xFFB9D68A)
            drawCircle(c.copy(alpha = (a * 0.25f).coerceIn(0f, 1f)), s.size.dp.toPx() * 3.2f, Offset(x * w, y * h))
            drawCircle(c.copy(alpha = a.coerceIn(0f, 1f)), s.size.dp.toPx() * 0.6f, Offset(x * w, y * h))
        }
        drawRect(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f)), Offset(w / 2f, h / 2f), max(w, h) * 0.78f))
    }
}

private fun vine(s: DrawScope, x0: Float, length: Float, t: Float, phase: Float, side: Float) {
    with(s) {
        val steps = 22
        var px = x0
        var py = 0f
        val stroke = 2.4.dp.toPx()
        for (i in 1..steps) {
            val k = i / steps.toFloat()
            val sway = sin(t * 0.7f + phase + k * 2.2f) * 14.dp.toPx() * k
            val nx = x0 + side * 26.dp.toPx() * sin(k * 3.0f) + sway
            val ny = k * length
            drawLine(Color(0xFF2E3F1E), Offset(px, py), Offset(nx, ny), stroke * (1.2f - k * 0.6f), StrokeCap.Round)
            if (i % 2 == 0) {
                val ang = (if (i % 4 == 0) 28f else -28f) + sway
                rotate(ang, Offset(nx, ny)) {
                    drawOval(Moss.copy(alpha = 0.9f), Offset(nx, ny - 3.dp.toPx()), Size(13.dp.toPx() * (1.1f - k * 0.5f), 6.dp.toPx()))
                }
            }
            px = nx; py = ny
        }
    }
}

// ---- navigation bar -------------------------------------------------------------------------------

private data class WTab(val route: Route, val label: String, val icon: ImageVector)

private val WTabs = listOf(
    WTab(Route.Home, "خانه", Icons.Rounded.Home),
    WTab(Route.Library, "کتابخانه", Icons.Rounded.LibraryMusic),
    WTab(Route.Search, "جست‌وجو", Icons.Rounded.Search),
    WTab(Route.Lab, "آزمایشگاه", Icons.Rounded.GraphicEq),
    WTab(Route.Settings, "تنظیمات", Icons.Rounded.Settings)
)

/** Worn leather-and-brass tab bar; the active tab flips in 3D into a lit brass plate. */
@Composable
fun WastelandNavBar(nav: Navigator, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier.fillMaxWidth().height(68.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF2A2216).copy(alpha = 0.96f), Color(0xFF110E09).copy(alpha = 0.98f))))
            .border(1.dp, Brush.verticalGradient(listOf(p.accent.copy(alpha = 0.5f), p.accentDeep.copy(alpha = 0.25f))), shape)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        WTabs.forEach { tab ->
            val sel = nav.stack.first() == tab.route
            val flip by animateFloatAsState(if (sel) 0f else -90f, spring(0.55f, 260f), label = "flip")
            val glow by animateFloatAsState(if (sel) 1f else 0f, tween(400), label = "wglow")
            Box(
                Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(16.dp))
                    .pointerInput(tab.route) { detectTapGestures { nav.selectTab(tab.route) } },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.fillMaxSize()
                        .graphicsLayer { rotationX = flip; cameraDistance = 10f * density; alpha = glow }
                        .background(Brush.verticalGradient(listOf(p.accent.copy(alpha = 0.95f), p.accentDeep)), RoundedCornerShape(16.dp))
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(tab.icon, tab.label, tint = if (sel) Color(0xFF1B1409) else p.muted, modifier = Modifier.size(22.dp))
                    Text(tab.label, fontSize = 9.sp, fontFamily = Serif, fontWeight = FontWeight.Bold, color = if (sel) Color(0xFF1B1409) else p.muted, maxLines = 1)
                }
            }
        }
    }
}

// ---- home ---------------------------------------------------------------------------------------

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Row(modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = p.text, fontFamily = Serif, fontWeight = FontWeight.Black, fontSize = 20.sp)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f).height(1.dp).background(Brush.horizontalGradient(listOf(p.accent.copy(alpha = 0.5f), Color.Transparent))))
    }
}

@Composable
private fun CoverCard(vm: AvatekViewModel, track: AudioTrack, list: List<AudioTrack>) {
    val p = LocalPalette.current
    Column(Modifier.width(112.dp).tilt3d { vm.playTrackIn(list, track) }) {
        Artwork(track.title + track.artist, track.id, track.albumId, Modifier.size(112.dp).border(1.dp, p.accent.copy(alpha = 0.35f), RoundedCornerShape(14.dp)), RoundedCornerShape(14.dp))
        Text(track.title, color = p.text, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
        Text(track.artist, color = p.muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** The survivor on the ridge: a hand-drawn silhouette with backpack and guitar, lit by a flickering sky. */
@Composable
private fun HeroScene(modifier: Modifier) {
    val tick = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) { while (true) androidx.compose.runtime.withFrameNanos { tick.floatValue = it / 1e9f } }
    Canvas(modifier) {
        val t = tick.floatValue
        val w = size.width
        val h = size.height
        drawRect(Brush.verticalGradient(listOf(Color(0xFF3B2D18), Color(0xFF1A140B), Color(0xFF0B0907))))
        drawCircle(
            Brush.radialGradient(listOf(Color(0xFFE9BE72).copy(alpha = 0.45f + 0.08f * sin(t * 1.3f)), Color.Transparent), Offset(w * 0.28f, h * 0.30f), w * 0.55f),
            w * 0.55f, Offset(w * 0.28f, h * 0.30f)
        )
        val ridge = Path().apply {
            moveTo(0f, h); lineTo(0f, h * 0.72f); lineTo(w * 0.25f, h * 0.66f); lineTo(w * 0.5f, h * 0.74f); lineTo(w * 0.8f, h * 0.64f); lineTo(w, h * 0.7f); lineTo(w, h); close()
        }
        drawPath(ridge, Color(0xFF0D0B07))
        val fx = w * 0.62f
        val fy = h * 0.66f
        val u = h * 0.012f
        val body = Color(0xFF050403)
        drawCircle(body, 5.2f * u, Offset(fx, fy - 30 * u))
        drawRoundRect(body, Offset(fx - 6 * u, fy - 25 * u), Size(12 * u, 20 * u), androidx.compose.ui.geometry.CornerRadius(3 * u))
        drawRoundRect(body, Offset(fx - 11 * u, fy - 24 * u), Size(7 * u, 15 * u), androidx.compose.ui.geometry.CornerRadius(2 * u))
        drawLine(body, Offset(fx - 3 * u, fy - 5 * u), Offset(fx - 4 * u, fy + 8 * u), 4 * u)
        drawLine(body, Offset(fx + 3 * u, fy - 5 * u), Offset(fx + 5 * u, fy + 8 * u), 4 * u)
        drawLine(body, Offset(fx + 8 * u, fy - 28 * u), Offset(fx - 9 * u, fy - 4 * u), 2.2f * u, StrokeCap.Round)
        drawOval(body, Offset(fx - 14 * u, fy - 9 * u), Size(9 * u, 7 * u))
    }
}

@Composable
fun WastelandHome(vm: AvatekViewModel, ui: AppUi) {
    val p = LocalPalette.current
    val bottom = LocalBottomInset.current
    val requestPermission = LocalPermissionRequester.current
    val all = vm.tracks
    val forYou = remember(all, vm.playCounts) { (vm.mostPlayed(8) + vm.recentlyAdded(8)).distinctBy { it.id }.take(12) }
    val recent = vm.recentlyPlayed(8)
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = bottom), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("AVATEK", color = p.text, fontFamily = Serif, fontWeight = FontWeight.Black, fontSize = 30.sp, letterSpacing = 3.sp)
                    Text("پس از پایان دنیا، موسیقی هنوز زنده است", color = p.muted, fontSize = 11.sp)
                }
                Icon(Icons.Rounded.Search, "جست‌وجو", tint = p.accent, modifier = Modifier.size(26.dp).pointerInput(Unit) { detectTapGestures { ui.nav.selectTab(Route.Search) } })
            }
        }
        item {
            var dx by remember { mutableFloatStateOf(0f) }
            var dy by remember { mutableFloatStateOf(0f) }
            val ax by animateFloatAsState(dx, spring(0.55f, 200f), label = "hx")
            val ay by animateFloatAsState(dy, spring(0.55f, 200f), label = "hy")
            Box(
                Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(206.dp)
                    .graphicsLayer { rotationY = ax; rotationX = ay; cameraDistance = 16f * density }
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, Brush.linearGradient(listOf(p.accent.copy(alpha = 0.6f), p.accentDeep.copy(alpha = 0.2f))), RoundedCornerShape(24.dp))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = { dx = 0f; dy = 0f }, onDragCancel = { dx = 0f; dy = 0f }
                        ) { change, drag ->
                            change.consume()
                            dx = (dx + drag.x * 0.12f).coerceIn(-14f, 14f)
                            dy = (dy - drag.y * 0.12f).coerceIn(-10f, 10f)
                        }
                    }
            ) {
                HeroScene(Modifier.fillMaxSize())
                Column(Modifier.align(Alignment.CenterStart).padding(22.dp)) {
                    Text("آخرین", color = p.text, fontFamily = Serif, fontWeight = FontWeight.Black, fontSize = 30.sp, lineHeight = 32.sp)
                    Text("بازمانده", color = p.accent, fontFamily = Serif, fontWeight = FontWeight.Black, fontSize = 30.sp, lineHeight = 32.sp)
                    Text("موسیقی متن دنیای خاموش", color = p.muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
                    Row(
                        Modifier.clip(RoundedCornerShape(14.dp))
                            .background(Brush.verticalGradient(listOf(p.accent, p.accentDeep)))
                            .pointerInput(all) { detectTapGestures { if (all.isNotEmpty()) vm.playList(all.shuffled()) else requestPermission() } }
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null, tint = Color(0xFF1B1409))
                        Text("پخش کن", color = Color(0xFF1B1409), fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }
                }
            }
        }
        if (all.isEmpty()) {
            item {
                Text(
                    "کتابخانه خالی است. برای دیدن آهنگ‌ها دسترسی به فایل‌های صوتی را فعال کن.", color = p.muted, fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 20.dp).pointerInput(Unit) { detectTapGestures { requestPermission() } }
                )
            }
        }
        if (forYou.isNotEmpty()) {
            item { SectionTitle("برای تو") }
            item {
                LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(forYou, key = { it.id }) { CoverCard(vm, it, forYou) }
                }
            }
        }
        if (recent.isNotEmpty()) {
            item { SectionTitle("اخیراً پخش‌شده") }
            items(recent, key = { "r" + it.id }) { t ->
                Row(
                    Modifier.padding(horizontal = 20.dp).fillMaxWidth().tilt3d(6f) { vm.playTrackIn(recent, t) }
                        .clip(RoundedCornerShape(16.dp)).background(Bark.copy(alpha = 0.72f))
                        .border(1.dp, p.accent.copy(alpha = 0.18f), RoundedCornerShape(16.dp)).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Artwork(t.title + t.artist, t.id, t.albumId, Modifier.size(48.dp), RoundedCornerShape(10.dp), 128)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(t.title, color = p.text, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(t.artist, color = p.muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(faDuration(t.durationMs), color = p.muted, fontSize = 11.sp)
                }
            }
        }
    }
}

// ---- now playing ----------------------------------------------------------------------------------

@Composable
fun WastelandNowPlaying(vm: AvatekViewModel, ui: AppUi, track: AudioTrack) {
    val p = LocalPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var peaks by remember(track.id) { mutableStateOf<FloatArray?>(null) }
    LaunchedEffect(track.id) {
        peaks = withContext(Dispatchers.IO) { WaveformExtractor.extract(context, track.uri) }
    }
    val tick = remember { mutableFloatStateOf(0f) }
    var angle by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(vm.isPlaying) {
        var last = 0L
        while (true) androidx.compose.runtime.withFrameNanos { n ->
            if (last == 0L) last = n
            val dt = (n - last) / 1e9f
            last = n
            tick.floatValue = n / 1e9f
            if (vm.isPlaying) angle = (angle + dt * (30f + 70f * AudioLevels.bass)) % 360f
        }
    }
    val arm by animateFloatAsState(if (vm.isPlaying) 1f else 0f, spring(0.6f, 90f), label = "arm")
    val tx = remember { Animatable(0f) }
    val ty = remember { Animatable(0f) }

    Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { } }) {
        WastelandBackground()
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(0.15f), Color.Black.copy(0.62f)))))
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 22.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.KeyboardArrowDown, "بستن", tint = p.text, modifier = Modifier.size(34.dp).pointerInput(Unit) { detectTapGestures { ui.closePlayer() } })
                Text("در حال پخش", color = p.muted, fontFamily = Serif, fontSize = 13.sp, letterSpacing = 2.sp, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Icon(Icons.Rounded.QueueMusic, "صف پخش", tint = p.text, modifier = Modifier.size(28.dp).pointerInput(Unit) { detectTapGestures { ui.show(Sheet.Queue) } })
            }
            // 3D turntable
            Box(Modifier.weight(1f).fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                Canvas(
                    Modifier.fillMaxSize()
                        .graphicsLayer {
                            val idle = sin(tick.floatValue * 0.6f) * 3f
                            rotationX = 38f + ty.value + idle
                            rotationY = tx.value + cos(tick.floatValue * 0.5f) * 3f
                            cameraDistance = 18f * density
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd = { scope.launch { tx.animateTo(0f, spring(0.4f, 120f)) }; scope.launch { ty.animateTo(0f, spring(0.4f, 120f)) } },
                                onDragCancel = { scope.launch { tx.animateTo(0f, spring(0.4f, 120f)) }; scope.launch { ty.animateTo(0f, spring(0.4f, 120f)) } }
                            ) { change, d ->
                                change.consume()
                                scope.launch { tx.snapTo((tx.value + d.x * 0.18f).coerceIn(-28f, 28f)) }
                                scope.launch { ty.snapTo((ty.value - d.y * 0.12f).coerceIn(-14f, 14f)) }
                            }
                        }
                ) {
                    val d = min(size.width, size.height) * 0.92f
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val beat = AudioLevels.beat
                    // slab
                    drawRoundRect(Brush.linearGradient(listOf(Color(0xFF3A2D1A), Color(0xFF16110A))), Offset(c.x - d * 0.56f, c.y - d * 0.56f), Size(d * 1.12f, d * 1.12f), androidx.compose.ui.geometry.CornerRadius(d * 0.08f))
                    drawRoundRect(p.accent.copy(alpha = 0.35f), Offset(c.x - d * 0.56f, c.y - d * 0.56f), Size(d * 1.12f, d * 1.12f), androidx.compose.ui.geometry.CornerRadius(d * 0.08f), style = Stroke(1.5.dp.toPx()))
                    val rad = d / 2f * (1f + 0.012f * beat)
                    drawCircle(Color.Black.copy(alpha = 0.5f), rad * 1.04f, c + Offset(0f, 8.dp.toPx()))
                    drawCircle(Color(0xFF080706), rad, c)
                    var g = 0.36f
                    while (g < 0.98f) {
                        drawCircle(Color.White.copy(alpha = 0.035f + 0.02f * sin(g * 40f)), rad * g, c, style = Stroke(0.8.dp.toPx()))
                        g += 0.018f
                    }
                    rotate(angle, c) {
                        drawCircle(Brush.sweepGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.10f), Color.Transparent, Color.Transparent, Color.White.copy(alpha = 0.07f), Color.Transparent), c), rad, c)
                    }
                    drawCircle(p.accent.copy(alpha = 0.25f + 0.4f * beat), rad * 0.34f, c, style = Stroke(2.dp.toPx()))
                    drawCircle(Brush.radialGradient(listOf(p.accent, p.accentDeep), c, rad * 0.32f), rad * 0.32f, c)
                    rotate(angle, c) {
                        drawCircle(Color.Black.copy(alpha = 0.25f), rad * 0.2f, c + Offset(rad * 0.1f, 0f), style = Stroke(1.dp.toPx()))
                    }
                    drawCircle(Color(0xFF0A0806), rad * 0.035f, c)
                    // tonearm
                    val pivot = Offset(c.x + d * 0.5f, c.y - d * 0.42f)
                    val a = (-34f + 22f * arm) * (PI.toFloat() / 180f)
                    val len = d * 0.78f
                    val tip = Offset(pivot.x - len * cos(a + 0.9f), pivot.y + len * sin(a + 1.9f).coerceAtLeast(0.2f) * 0.7f)
                    drawLine(Color(0xFFB9A57A), pivot, tip, 5.dp.toPx(), StrokeCap.Round)
                    drawCircle(Color(0xFF8A7650), 14.dp.toPx(), pivot)
                    drawCircle(Color(0xFF1B1409), 5.dp.toPx(), pivot)
                    drawRoundRect(Color(0xFF2A2215), tip - Offset(7.dp.toPx(), 7.dp.toPx()), Size(14.dp.toPx(), 20.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
                }
                Artwork(track.title + track.artist, track.id, track.albumId, Modifier.size(54.dp).graphicsLayer { alpha = 0f }, CircleShape, 64)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(track.title, color = p.text, fontFamily = Serif, fontWeight = FontWeight.Black, fontSize = 24.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.artist, color = p.accent, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                FavoriteButton(track.id in vm.favorites, { vm.toggleFavorite(track.id) }, size = 48.dp)
            }
            RhythmVisualizer(vm.isPlaying, Modifier.fillMaxWidth().height(34.dp).padding(top = 6.dp), bars = 48)
            WaveSeekBar(vm.positionMs, vm.durationMs, peaks, vm.isPlaying, { vm.seekTo(it) }, Modifier.padding(top = 2.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(faDuration(vm.positionMs), color = p.muted, fontSize = 11.sp)
                Text(faDuration(vm.durationMs), color = p.muted, fontSize = 11.sp)
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Shuffle, "پخش تصادفی", tint = if (vm.shuffle) p.accent else p.muted, modifier = Modifier.size(26.dp).pointerInput(Unit) { detectTapGestures { vm.toggleShuffle() } })
                Icon(Icons.Rounded.SkipPrevious, "قبلی", tint = p.text, modifier = Modifier.size(40.dp).pointerInput(Unit) { detectTapGestures { vm.previous() } })
                val pulse = AudioLevels.beat
                Box(
                    Modifier.size(78.dp).graphicsLayer { val s = 1f + 0.05f * pulse; scaleX = s; scaleY = s }
                        .background(Brush.verticalGradient(listOf(p.accent, p.accentDeep)), CircleShape)
                        .border(2.dp, p.accent.copy(alpha = 0.4f + 0.5f * pulse), CircleShape)
                        .pointerInput(Unit) { detectTapGestures { vm.togglePlay() } },
                    contentAlignment = Alignment.Center
                ) { Icon(if (vm.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "پخش", tint = Color(0xFF1B1409), modifier = Modifier.size(40.dp)) }
                Icon(Icons.Rounded.SkipNext, "بعدی", tint = p.text, modifier = Modifier.size(40.dp).pointerInput(Unit) { detectTapGestures { vm.next() } })
                Icon(
                    if (vm.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, "تکرار",
                    tint = if (vm.repeatMode != Player.REPEAT_MODE_OFF) p.accent else p.muted,
                    modifier = Modifier.size(26.dp).pointerInput(Unit) { detectTapGestures { vm.cycleRepeat() } }
                )
            }
        }
    }
}

// ---- splash ---------------------------------------------------------------------------------------

@Composable
fun WastelandSplash(onFinished: () -> Unit) {
    val p = LocalPalette.current
    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { grow.animateTo(1f, tween(2200)) }
        kotlinx.coroutines.delay(2500)
        onFinished()
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        WastelandBackground()
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.graphicsLayer { alpha = grow.value; translationY = (1f - grow.value) * 30f }) {
            Text(
                "AV", fontFamily = Serif, fontWeight = FontWeight.Black, fontSize = 96.sp,
                style = TextStyle(brush = Brush.verticalGradient(listOf(Color(0xFFD8CDB0), Color(0xFF7B6E50), Moss)), fontSize = 96.sp, fontFamily = Serif, fontWeight = FontWeight.Black)
            )
            Text("AVATEK", color = p.text, fontFamily = Serif, fontSize = 20.sp, letterSpacing = 12.sp)
            Text("موسیقی را حس کن…", color = p.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
        }
        Box(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp).width(150.dp).height(2.dp).background(Color.White.copy(alpha = 0.1f))
        ) { Box(Modifier.fillMaxHeight().fillMaxWidth(grow.value.coerceIn(0.01f, 1f)).background(p.accent)) }
    }
}

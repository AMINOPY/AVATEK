@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.aminopy.avatek.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aminopy.avatek.state.AvatekViewModel
import com.aminopy.avatek.ui.AppUi
import com.aminopy.avatek.ui.nav.Navigator
import com.aminopy.avatek.ui.nav.Route
import com.aminopy.avatek.ui.nav.TabRoutes
import com.aminopy.avatek.ui.theme.Artwork
import com.aminopy.avatek.ui.theme.LocalPalette
import com.aminopy.avatek.ui.theme.LocalUi
import com.aminopy.avatek.ui.theme.bounceClick
import com.aminopy.avatek.ui.theme.glass
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private data class TabSpec(val route: Route, val label: String, val icon: ImageVector)

private val Tabs = listOf(
    TabSpec(Route.Home, "خانه", Icons.Rounded.Home),
    TabSpec(Route.Library, "کتابخانه", Icons.Rounded.LibraryMusic),
    TabSpec(Route.Search, "جست‌وجو", Icons.Rounded.Search),
    TabSpec(Route.Lab, "آزمایشگاه", Icons.Rounded.GraphicEq),
    TabSpec(Route.Settings, "تنظیمات", Icons.Rounded.Settings)
)

/**
 * Floating glass tab bar. The selection "lens" stretches like a liquid drop while it travels:
 * the leading edge moves fast, the trailing edge follows with a softer spring.
 */
@Composable
fun FloatingNavBar(nav: Navigator, haze: HazeState, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val selected = nav.tabIndex(nav.stack.first()).coerceAtLeast(0)
    var last by remember { mutableIntStateOf(selected) }
    var dir by remember { mutableIntStateOf(1) }
    LaunchedEffect(selected) {
        dir = if (selected >= last) 1 else -1
        last = selected
    }
    BoxWithConstraints(
        modifier.fillMaxWidth().height(70.dp).glass(RoundedCornerShape(35.dp), haze, strength = 1f)
    ) {
        val itemPx = with(LocalDensity.current) { (maxWidth / Tabs.size).toPx() }
        val fast = spring<Float>(dampingRatio = 0.72f, stiffness = 760f)
        val slow = spring<Float>(dampingRatio = 0.8f, stiffness = 230f)
        val startPx by animateFloatAsState(selected * itemPx, if (dir > 0) slow else fast, label = "lensStart")
        val endPx by animateFloatAsState((selected + 1) * itemPx, if (dir > 0) fast else slow, label = "lensEnd")
        val lens = RoundedCornerShape(29.dp)
        Box(
            Modifier.fillMaxSize().layout { measurable, c ->
                val w = (endPx - startPx).roundToInt().coerceAtLeast(1)
                val placeable = measurable.measure(Constraints.fixed(w, c.maxHeight))
                layout(c.maxWidth, c.maxHeight) { placeable.placeRelative(startPx.roundToInt(), 0) }
            }.padding(5.dp).clip(lens)
                .background(Brush.verticalGradient(listOf(p.accent.copy(alpha = 0.34f), p.accent.copy(alpha = 0.10f))))
                .border(1.dp, p.accent.copy(alpha = 0.55f), lens)
        )
        Row(Modifier.fillMaxSize()) {
            Tabs.forEach { tab ->
                val isSel = nav.stack.first() == tab.route
                val lift by animateFloatAsState(if (isSel) -2f else 0f, spring(0.5f, 500f), label = "lift")
                val grow by animateFloatAsState(if (isSel) 1.14f else 1f, spring(0.4f, 450f), label = "grow")
                Column(
                    Modifier.weight(1f).fillMaxHeight().bounceClick(pressedScale = 0.88f) { nav.selectTab(tab.route) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        tab.icon, tab.label,
                        tint = if (isSel) p.accent else p.muted,
                        modifier = Modifier.size(24.dp).graphicsLayer { scaleX = grow; scaleY = grow; translationY = lift * this.density }
                    )
                    Text(
                        tab.label, fontSize = 10.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSel) p.text else p.muted, maxLines = 1
                    )
                }
            }
        }
    }
}

/** Floating mini player: spinning cover, progress ring, swipe to skip, tap to expand. */
@Composable
fun MiniPlayer(vm: AvatekViewModel, ui: AppUi, haze: HazeState, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val settings = LocalUi.current
    val track = vm.currentTrack
    val scope = rememberCoroutineScope()
    val dragX = remember { Animatable(0f) }
    AnimatedVisibility(
        visible = track != null && !ui.playerOpen,
        modifier = modifier,
        enter = slideInVertically(spring(0.7f, 380f)) { it } + fadeIn(tween(250)) + scaleIn(initialScale = 0.9f),
        exit = slideOutVertically(tween(250)) { it } + fadeOut(tween(200)) + scaleOut(targetScale = 0.9f)
    ) {
        val shown = track ?: return@AnimatedVisibility
        val spin = rememberSpin(vm.isPlaying, 12000)
        val progress = if (vm.durationMs > 0) (vm.positionMs.toFloat() / vm.durationMs).coerceIn(0f, 1f) else 0f
        Row(
            Modifier
                .fillMaxWidth()
                .height(66.dp)
                .graphicsLayer { translationX = dragX.value * 0.4f; alpha = 1f - (abs(dragX.value) / 900f).coerceAtMost(0.5f) }
                .glass(RoundedCornerShape(33.dp), haze, tint = p.accent, strength = 1f)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val d = dragX.value
                            if (d < -140f) vm.next() else if (d > 140f) vm.previous()
                            scope.launch { dragX.animateTo(0f, spring(0.6f, 420f)) }
                        },
                        onDragCancel = { scope.launch { dragX.animateTo(0f) } },
                        onHorizontalDrag = { change, amount -> change.consume(); scope.launch { dragX.snapTo(dragX.value + amount) } }
                    )
                }
                .bounceClick(pressedScale = 0.98f) { ui.openPlayer() }
                .padding(horizontal = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Artwork(
                shown.albumKey, shown.id, shown.albumId,
                Modifier.size(48.dp).graphicsLayer { rotationZ = spin.value }, CircleShape
            )
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    shown.title, color = p.text, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                )
                Text(shown.artist, color = p.muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(Modifier.size(46.dp).bounceClick(pressedScale = 0.88f) { vm.togglePlay() }, contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val sw = 3.dp.toPx()
                    val inset = sw / 2f
                    val arcSize = Size(size.width - sw, size.height - sw)
                    drawArc(Color.White.copy(alpha = 0.18f), 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(sw))
                    drawArc(p.accent, -90f, 360f * progress, false, Offset(inset, inset), arcSize, style = Stroke(sw, cap = StrokeCap.Round))
                }
                Icon(
                    if (vm.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    if (vm.isPlaying) "توقف" else "پخش", tint = p.text, modifier = Modifier.size(24.dp)
                )
            }
            Box(Modifier.size(42.dp).bounceClick(pressedScale = 0.85f) { vm.next() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.SkipNext, "بعدی", tint = p.text.copy(alpha = if (settings.animations) 0.9f else 1f), modifier = Modifier.size(24.dp))
            }
        }
    }
}

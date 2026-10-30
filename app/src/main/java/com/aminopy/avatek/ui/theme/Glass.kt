package com.aminopy.avatek.ui.theme

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Shared blur source: the main content layer. Floating glass (bars, sheets) samples it. */
val LocalHaze = staticCompositionLocalOf<HazeState?> { null }

/**
 * Liquid-glass surface. With a [haze] state it really blurs what is behind it;
 * without one it falls back to a frosted gradient, which is what cards inside scrolling content use.
 */
fun Modifier.glass(
    shape: Shape,
    haze: HazeState? = null,
    tint: Color = Color.White,
    strength: Float? = null
): Modifier = composed {
    val ui = LocalUi.current
    val p = LocalPalette.current
    val s = strength ?: ui.glass
    var m: Modifier = this.clip(shape)
    if (haze != null) {
        m = m.hazeEffect(
            state = haze,
            style = HazeStyle(
                backgroundColor = p.bgBottom,
                tints = listOf(HazeTint(tint.copy(alpha = 0.05f + 0.13f * s))),
                blurRadius = (14 + 30 * s).dp,
                noiseFactor = 0.035f
            )
        )
    }
    val fill = if (haze != null) 0.06f + 0.06f * s else 0.06f + 0.11f * s
    m.background(Brush.linearGradient(listOf(tint.copy(alpha = fill + 0.05f), tint.copy(alpha = fill * 0.35f))))
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(
                listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.05f), p.accent.copy(alpha = 0.32f))
            ),
            shape = shape
        )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    haze: HazeState? = null,
    tint: Color = Color.White,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier.glass(shape, haze, tint), contentAlignment = contentAlignment, content = content)
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    active: Boolean = false,
    tint: Color? = null,
    haze: HazeState? = null
) {
    val p = LocalPalette.current
    val color by animateColorAsState(
        targetValue = tint ?: if (active) p.accent else p.text,
        animationSpec = tween(220),
        label = "iconTint"
    )
    Box(
        modifier = modifier
            .size(size)
            .bounceClick(onClick = onClick)
            .glass(CircleShape, haze, tint = if (active) p.accent else Color.White),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, tint = color, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun GlassChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null
) {
    val p = LocalPalette.current
    val textColor by animateColorAsState(if (selected) Color(0xFF14100A) else p.text, tween(220), label = "chipText")
    val bg by animateColorAsState(if (selected) p.accent else Color.Transparent, tween(260), label = "chipBg")
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .bounceClick(pressedScale = 0.94f, onClick = onClick)
            .glass(shape, strength = 0.55f)
            .background(bg, shape)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (leading != null) Icon(leading, null, tint = textColor, modifier = Modifier.size(16.dp))
        Text(label, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Primary call-to-action: accent gradient with a coloured glow. */
@Composable
fun AccentButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .bounceClick(enabled = enabled, pressedScale = 0.95f, onClick = onClick)
            .shadow(if (enabled) 18.dp else 0.dp, shape, ambientColor = p.accent, spotColor = p.accent)
            .clip(shape)
            .background(
                if (enabled) Brush.linearGradient(listOf(p.accent, p.accentDeep.copy(alpha = 0.95f)))
                else Brush.linearGradient(listOf(Color(0x33FFFFFF), Color(0x22FFFFFF)))
            )
            .padding(horizontal = 22.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        if (icon != null) Icon(icon, null, tint = Color(0xFF14100A), modifier = Modifier.size(20.dp))
        Text(text, color = if (enabled) Color(0xFF14100A) else p.muted, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    val p = LocalPalette.current
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = p.text, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            if (subtitle != null) Text(subtitle, color = p.muted, fontSize = 11.sp)
        }
        if (action != null && onAction != null) {
            Text(
                action, color = p.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(RoundedCornerShape(50)).bounceClick(pressedScale = 0.9f, onClick = onAction)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

/** Bottom sheet rendered as an in-app overlay (so it can blur the content behind and animate out). */
@Composable
fun GlassSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val haze = LocalHaze.current
    BackHandler(enabled = visible, onBack = onDismiss)
    Box(modifier.fillMaxSize()) {
        AnimatedVisibility(visible, enter = fadeIn(tween(260)), exit = fadeOut(tween(220))) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
            )
        }
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)) { it } + fadeIn(tween(200)),
            exit = slideOutVertically(tween(260)) { it } + fadeOut(tween(220))
        ) {
            val scope = rememberCoroutineScope()
            val offset = remember { Animatable(0f) }
            val p = LocalPalette.current
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp)
                    .heightIn(max = 640.dp)
                    .offsetY { offset.value.roundToInt() }
                    .glass(RoundedCornerShape(34.dp), haze, strength = 0.9f)
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { d -> scope.launch { offset.snapTo((offset.value + d).coerceAtLeast(0f)) } },
                        onDragStopped = {
                            if (offset.value > 180f) onDismiss() else scope.launch { offset.animateTo(0f, spring(0.7f, 400f)) }
                        }
                    )
                    .padding(bottom = 6.dp)
            ) {
                Box(
                    Modifier.padding(top = 10.dp, bottom = 6.dp).align(Alignment.CenterHorizontally)
                        .size(width = 42.dp, height = 5.dp)
                        .clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.35f))
                )
                if (title != null) {
                    Text(
                        title, color = p.text, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp)
                    )
                }
                content()
            }
        }
    }
}

private fun Modifier.offsetY(block: () -> Int): Modifier =
    this.then(Modifier.offset { IntOffset(0, block()) })

/** Centered glass dialog with a springy pop-in. */
@Composable
fun GlassDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val haze = LocalHaze.current
    BackHandler(enabled = visible, onBack = onDismiss)
    Box(modifier.fillMaxSize()) {
        AnimatedVisibility(visible, enter = fadeIn(tween(220)), exit = fadeOut(tween(180))) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
            )
        }
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.Center),
            enter = scaleIn(spring(0.62f, 380f), initialScale = 0.82f) + fadeIn(tween(180)),
            exit = scaleOut(tween(160), targetScale = 0.9f) + fadeOut(tween(140))
        ) {
            Column(
                Modifier
                    .padding(horizontal = 28.dp)
                    .fillMaxWidth()
                    .glass(RoundedCornerShape(32.dp), haze, strength = 0.95f)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
                    .padding(22.dp),
                content = content
            )
        }
    }
}


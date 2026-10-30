package com.aminopy.avatek.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import kotlinx.coroutines.delay

val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
val EmphasizedAccel = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

fun <T> bouncy() = spring<T>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
fun <T> snappy() = spring<T>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)

/** Light haptic tick that respects the user's haptics setting. */
@Composable
fun rememberTick(strong: Boolean = false): () -> Unit {
    val haptic = LocalHapticFeedback.current
    val ui = LocalUi.current
    return {
        if (ui.haptics) {
            haptic.performHapticFeedback(if (strong) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove)
        }
    }
}

/** Clickable with a springy press-scale and haptic tick, without the default ripple. */
fun Modifier.bounceClick(
    enabled: Boolean = true,
    pressedScale: Float = 0.92f,
    role: Role = Role.Button,
    onClick: () -> Unit
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val ui = LocalUi.current
    val tick = rememberTick()
    val scale by animateFloatAsState(
        targetValue = if (pressed && ui.animations) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 520f),
        label = "press"
    )
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = { tick(); onClick() }
        )
}

/** Fade + rise entrance used for the first items of lists and screens. */
fun Modifier.staggerIn(index: Int, maxIndex: Int = 9, stepMs: Int = 55): Modifier = composed {
    val ui = LocalUi.current
    val progress = remember { Animatable(if (ui.animations) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (ui.animations && index <= maxIndex) {
            delay((index * stepMs).toLong())
            progress.animateTo(1f, tween(520, easing = Emphasized))
        } else progress.snapTo(1f)
    }
    this.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 46f
    }
}

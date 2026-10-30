package com.aminopy.avatek.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Living background: three soft light blobs drifting slowly over a dark gradient.
 * [ambient] is the colour of the current cover art, so the whole app glows with the song.
 */
@Composable
fun AuroraBackground(ambient: Color, modifier: Modifier = Modifier.fillMaxSize()) {
    val p = LocalPalette.current
    val ui = LocalUi.current
    val tint by animateColorAsState(
        targetValue = if (ui.ambientColors) ambient else p.secondary,
        animationSpec = tween(1600),
        label = "ambient"
    )
    val phaseA: State<Float>
    val phaseB: State<Float>
    if (ui.animations) {
        val t = rememberInfiniteTransition(label = "aurora")
        phaseA = t.animateFloat(0f, 1f, infiniteRepeatable(tween(28000, easing = LinearEasing), RepeatMode.Restart), label = "a")
        phaseB = t.animateFloat(0f, 1f, infiniteRepeatable(tween(41000, easing = LinearEasing), RepeatMode.Restart), label = "b")
    } else {
        phaseA = remember { mutableFloatStateOf(0.15f) }
        phaseB = remember { mutableFloatStateOf(0.4f) }
    }
    Canvas(modifier) {
        drawRect(Brush.verticalGradient(listOf(p.bgTop, p.bgBottom)))
        val tau = (2 * PI).toFloat()
        val a = phaseA.value * tau
        val b = phaseB.value * tau
        val w = size.width
        val h = size.height
        blob(Offset(w * (0.18f + 0.16f * sin(a)), h * (0.14f + 0.07f * cos(a))), w * 0.95f, p.accent, 0.20f)
        blob(Offset(w * (0.88f + 0.10f * cos(b + 1f)), h * (0.46f + 0.10f * sin(b))), w * 0.85f, tint, 0.30f)
        blob(Offset(w * (0.28f + 0.22f * sin(b + 2f)), h * (0.90f + 0.05f * cos(a))), w * 0.95f, p.secondary, 0.20f)
        // soft vignette so text stays readable
        drawRect(
            Brush.verticalGradient(
                0f to Color.Black.copy(alpha = 0.25f),
                0.3f to Color.Transparent,
                0.75f to Color.Transparent,
                1f to Color.Black.copy(alpha = 0.45f)
            )
        )
    }
}

private fun DrawScope.blob(center: Offset, radius: Float, color: Color, alpha: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), color.copy(alpha = alpha * 0.35f), Color.Transparent),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}

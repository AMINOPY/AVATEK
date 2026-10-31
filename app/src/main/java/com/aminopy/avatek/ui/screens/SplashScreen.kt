package com.aminopy.avatek.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aminopy.avatek.ui.theme.Emphasized
import com.aminopy.avatek.ui.theme.LocalPalette
import com.aminopy.avatek.ui.theme.isWasteland
import kotlinx.coroutines.launch

/** Intro: a gold ring draws itself, the wordmark rises out of it, then [onFinished] fires. */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    if (com.aminopy.avatek.ui.theme.LocalUi.current.accent.isWasteland) {
        com.aminopy.avatek.ui.theme.WastelandSplash(onFinished)
        return
    }
    val p = LocalPalette.current
    val ring = remember { Animatable(0f) }
    val word = remember { Animatable(0f) }
    val tag = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { ring.animateTo(1f, tween(1100, easing = Emphasized)) }
        launch { word.animateTo(1f, tween(900, delayMillis = 500, easing = Emphasized)) }
        launch { tag.animateTo(1f, tween(700, delayMillis = 1000)) }
        kotlinx.coroutines.delay(1900)
        onFinished()
    }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(p.bgTop, p.bgBottom))), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(190.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val sw = 5.dp.toPx()
                    drawCircle(p.accent.copy(alpha = 0.10f * ring.value), size.minDimension * 0.5f)
                    drawArc(
                        Brush.sweepGradient(listOf(p.accentDeep, p.accent, Color.White, p.accent), Offset(size.width / 2f, size.height / 2f)),
                        -90f, 360f * ring.value, false, Offset(sw, sw), Size(size.width - 2 * sw, size.height - 2 * sw),
                        style = Stroke(sw, cap = StrokeCap.Round)
                    )
                }
                Text(
                    "A",
                    style = TextStyle(brush = Brush.linearGradient(listOf(p.accent, Color.White)), fontSize = 84.sp, fontWeight = FontWeight.Black),
                    modifier = Modifier.graphicsLayer { alpha = word.value; scaleX = 0.7f + 0.3f * word.value; scaleY = 0.7f + 0.3f * word.value }
                )
            }
            Text(
                "AVATEK", color = p.text, fontSize = 32.sp, fontWeight = FontWeight.Black, letterSpacing = 9.sp,
                modifier = Modifier.padding(top = 22.dp).graphicsLayer { alpha = word.value; translationY = (1f - word.value) * 40f }
            )
            Text(
                "YOUR MUSIC. YOUR WORLD.", color = p.muted, fontSize = 10.sp, letterSpacing = 3.sp,
                modifier = Modifier.padding(top = 6.dp).graphicsLayer { alpha = tag.value }
            )
        }
    }
}

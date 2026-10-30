package com.aminopy.avatek.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aminopy.avatek.ui.theme.AccentButton
import com.aminopy.avatek.ui.theme.EmphasizedAccel
import com.aminopy.avatek.ui.theme.LocalPalette
import com.aminopy.avatek.ui.theme.LocalUi
import com.aminopy.avatek.ui.theme.glass

/** Expanding sonar rings behind [content]. */
@Composable
private fun SonarRings(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val p = LocalPalette.current
    val ui = LocalUi.current
    val t = rememberInfiniteTransition(label = "sonar")
    val a = t.animateFloat(0f, 1f, infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "a")
    val b = t.animateFloat(0f, 1f, infiniteRepeatable(tween(3000, 1000, LinearEasing)), label = "b")
    val c = t.animateFloat(0f, 1f, infiniteRepeatable(tween(3000, 2000, LinearEasing)), label = "c")
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            if (ui.animations) {
                listOf(a.value, b.value, c.value).forEach { f ->
                    drawCircle(
                        color = p.accent.copy(alpha = 0.5f * (1f - f)),
                        radius = size.minDimension * (0.18f + 0.32f * f),
                        center = Offset(size.width / 2f, size.height / 2f),
                        style = Stroke(2.dp.toPx())
                    )
                }
            }
        }
        content()
    }
}

@Composable
fun PermissionScreen(onGrant: () -> Unit) {
    val p = LocalPalette.current
    Column(
        Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        SonarRings(Modifier.size(260.dp)) {
            Box(Modifier.size(110.dp).glass(CircleShape, strength = 1f), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Headphones, null, tint = p.accent, modifier = Modifier.size(52.dp))
            }
        }
        Text("به AVATEK خوش آمدی", color = p.text, fontSize = 24.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        Text(
            "برای نمایش آهنگ‌های دستگاهت به دسترسی صوتی نیاز داریم. همه‌چیز آفلاین می‌ماند؛ هیچ فایلی آپلود نمی‌شود.",
            color = p.muted, fontSize = 13.sp, lineHeight = 21.sp, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp, bottom = 26.dp)
        )
        AccentButton("اجازه‌ی دسترسی به موسیقی‌ها", onGrant, icon = Icons.Rounded.Lock)
    }
}

@Composable
fun ScanningScreen() {
    val p = LocalPalette.current
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        SonarRings(Modifier.size(260.dp)) {
            Box(
                Modifier.size(70.dp).background(Brush.radialGradient(listOf(p.accent, p.accentDeep)), CircleShape)
            )
        }
        Text("در حال اسکن موسیقی‌های دستگاه…", color = p.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("چند لحظه صبر کن", color = p.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

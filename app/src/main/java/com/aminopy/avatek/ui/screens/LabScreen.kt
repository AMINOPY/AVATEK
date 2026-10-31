@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.aminopy.avatek.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aminopy.avatek.state.AvatekViewModel
import com.aminopy.avatek.ui.AppUi
import com.aminopy.avatek.ui.LocalBottomInset
import com.aminopy.avatek.ui.Sheet
import com.aminopy.avatek.ui.components.ScreenHeader
import com.aminopy.avatek.ui.components.SpeedPicker
import com.aminopy.avatek.ui.fa
import com.aminopy.avatek.ui.faDuration
import com.aminopy.avatek.ui.theme.AccentButton
import com.aminopy.avatek.ui.theme.GlassChip
import com.aminopy.avatek.ui.theme.LocalPalette
import com.aminopy.avatek.ui.theme.SectionTitle
import com.aminopy.avatek.ui.theme.glass
import com.aminopy.avatek.ui.theme.staggerIn
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults

private val Presets = listOf(
    "تخت" to listOf(0f, 0f, 0f, 0f, 0f),
    "بیس" to listOf(0.7f, 0.45f, 0f, -0.1f, 0f),
    "صدای انسان" to listOf(-0.3f, 0f, 0.5f, 0.35f, -0.1f),
    "راک" to listOf(0.5f, 0.2f, -0.2f, 0.3f, 0.5f),
    "کریستالی" to listOf(-0.2f, 0f, 0.1f, 0.5f, 0.7f)
)

@Composable
fun LabScreen(vm: AvatekViewModel, ui: AppUi) {
    val p = LocalPalette.current
    val bottom = LocalBottomInset.current
    val eq = vm.eq
    var confirmReset by remember { mutableStateOf(false) }
    LaunchedEffect(vm.engineReady) { if (vm.engineReady) vm.refreshEqualizer() }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("بازنشانی کامل Audio Engine") },
            text = { Text("اکولایزر، بیس، فضاسازی، تقویت بلندی و سرعت پخش به حالت پیش‌فرض برمی‌گردند. این کار صف پخش و فایل‌های موسیقی را تغییر نمی‌دهد." ) },
            confirmButton = { TextButton(onClick = { confirmReset = false; vm.resetEqualizer() }) { Text("بازنشانی") } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("انصراف") } }
        )
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = bottom)) {
        ScreenHeader("Audio Engine", subtitle = "کنسول پردازش صدا · کنترل زنده و قابل بازنشانی")
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // equalizer
            Column(
                Modifier.fillMaxWidth().staggerIn(0).glass(RoundedCornerShape(30.dp), strength = 0.7f).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("اکولایزر", color = p.text, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Text(if (eq.available) "موتور افکت متصل · تغییرات روی خروجی پخش اعمال می‌شوند" else "برای فعال‌شدن افکت‌ها ابتدا یک آهنگ پخش کن", color = p.muted, fontSize = 11.sp)
                    }
                    Switch(
                        checked = eq.enabled, onCheckedChange = { vm.setEqEnabled(it) }, enabled = eq.available,
                        colors = SwitchDefaults.colors(checkedTrackColor = p.accent, checkedThumbColor = Color(0xFF14100A))
                    )
                }
                if (eq.available && eq.levels.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Presets.forEach { (name, curve) ->
                            GlassChip(name, false, {
                                val n = eq.levels.size
                                for (b in 0 until n) {
                                    val x = if (n == 1) 0f else b * (curve.size - 1f) / (n - 1f)
                                    val lo = x.toInt().coerceIn(0, curve.size - 1)
                                    val hi = (lo + 1).coerceAtMost(curve.size - 1)
                                    val v = curve[lo] + (curve[hi] - curve[lo]) * (x - lo)
                                    val range = if (v >= 0) eq.maxLevel else -eq.minLevel
                                    vm.setEqBand(b, (v * range).toInt())
                                }
                                if (!eq.enabled) vm.setEqEnabled(true)
                            })
                        }
                    }
                    Row(Modifier.fillMaxWidth().height(190.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        eq.levels.forEachIndexed { i, level ->
                            EqBand(
                                level, eq.minLevel, eq.maxLevel, eq.centerFreqsHz.getOrElse(i) { 0 }, eq.enabled
                            ) { vm.setEqBand(i, it) }
                        }
                    }
                }
            }
            // bass & surround
            Column(
                Modifier.fillMaxWidth().staggerIn(1).glass(RoundedCornerShape(30.dp), strength = 0.6f).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EffectSlider("تقویت بیس", eq.bass, eq.available) { vm.setBass(it) }
                EffectSlider("فضاسازی سه‌بعدی", eq.virtualizer, eq.available) { vm.setVirtualizer(it) }
                EffectSlider("تقویت بلندی (Loudness)", eq.loudness, eq.available) { vm.setLoudness(it) }
                Text("تقویت بلندی با پردازش LoudnessEnhancer اندروید انجام می‌شود؛ میزان قابل‌اعمال به سخت‌افزار و مسیر صوتی دستگاه وابسته است.", color = p.muted, fontSize = 10.sp)
                Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.CenterStart) {
                    GlassChip("بازنشانی کامل موتور صدا", false, { confirmReset = true })
                }
            }
            // speed
            Column(Modifier.fillMaxWidth().staggerIn(2).glass(RoundedCornerShape(30.dp), strength = 0.6f).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("سرعت پخش", color = p.text, fontSize = 16.sp, fontWeight = FontWeight.Black)\n                Text("۰٫۵ تا ۲ برابر · بازنشانی کامل، سرعت را به ۱× برمی‌گرداند", color = p.muted, fontSize = 11.sp)
                SpeedPicker(vm)
            }
            // sleep
            Column(Modifier.fillMaxWidth().staggerIn(3).glass(RoundedCornerShape(30.dp), strength = 0.6f).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("تایمر خواب", color = p.text, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Text(vm.sleepRemainingMs?.let { "توقف پخش تا ${faDuration(it)} دیگر" } ?: "پخش بعد از زمان انتخابی متوقف می‌شود.", color = p.muted, fontSize = 12.sp)
                AccentButton(if (vm.sleepRemainingMs != null) "تغییر / لغو" else "تنظیم تایمر", { ui.show(Sheet.Sleep) }, icon = Icons.Rounded.Bedtime)
            }
        }
    }
}

@Composable
private fun EffectSlider(label: String, value: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    val p = LocalPalette.current
    var local by remember(value) { mutableStateOf(value / 1000f) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = p.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.9f))
        Text("${(local * 100).toInt().fa()}٪", color = p.accent, fontSize = 12.sp)
    }
    Slider(
        value = local, onValueChange = { local = it }, onValueChangeFinished = { onChange((local * 1000).toInt()) },
        enabled = enabled,
        colors = SliderDefaults.colors(thumbColor = p.accent, activeTrackColor = p.accent, inactiveTrackColor = Color.White.copy(alpha = 0.15f))
    )
}

/** One vertical equalizer band: drag up/down, tap to set. */
@Composable
private fun EqBand(level: Int, min: Int, max: Int, freqHz: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    val p = LocalPalette.current
    var drag by remember { mutableStateOf<Float?>(null) }
    val range = (max - min).coerceAtLeast(1)
    val frac = drag ?: ((level - min).toFloat() / range).coerceIn(0f, 1f)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${((drag?.let { min + it * range } ?: level.toFloat()) / 100f).toInt().fa()}", color = p.muted, fontSize = 10.sp)
        Box(
            Modifier.weight(1f).size(width = 46.dp, height = 140.dp)
                .pointerInput(range) {
                    detectTapGestures { o -> onChange((min + (1f - o.y / size.height).coerceIn(0f, 1f) * range).toInt()) }
                }
                .pointerInput(range) {
                    detectVerticalDragGestures(
                        onDragStart = { o -> drag = (1f - o.y / size.height).coerceIn(0f, 1f) },
                        onVerticalDrag = { c, _ -> drag = (1f - c.position.y / size.height).coerceIn(0f, 1f) },
                        onDragEnd = { drag?.let { onChange((min + it * range).toInt()) }; drag = null },
                        onDragCancel = { drag = null }
                    )
                }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val w = 8.dp.toPx()
                drawRoundRect(Color.White.copy(alpha = 0.14f), Offset(cx - w / 2, 0f), Size(w, size.height), CornerRadius(w / 2))
                val y = size.height * (1f - frac)
                val c = if (enabled) p.accent else p.muted
                drawRoundRect(
                    Brush.verticalGradient(listOf(c, c.copy(alpha = 0.3f)), startY = y, endY = size.height),
                    Offset(cx - w / 2, y), Size(w, size.height - y), CornerRadius(w / 2)
                )
                drawCircle(c.copy(alpha = 0.25f), 15.dp.toPx(), Offset(cx, y))
                drawCircle(Color.White, 7.dp.toPx(), Offset(cx, y))
            }
        }
        Text(if (freqHz >= 1000) "${(freqHz / 1000).fa()}k" else freqHz.fa(), color = p.text, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

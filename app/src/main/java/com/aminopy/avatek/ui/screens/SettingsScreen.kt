@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.aminopy.avatek.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aminopy.avatek.data.AccentTheme
import com.aminopy.avatek.state.AvatekViewModel
import com.aminopy.avatek.ui.AppUi
import com.aminopy.avatek.ui.LocalBottomInset
import com.aminopy.avatek.ui.components.ScreenHeader
import com.aminopy.avatek.ui.fa
import com.aminopy.avatek.ui.nav.Route
import com.aminopy.avatek.ui.theme.AccentButton
import com.aminopy.avatek.ui.theme.GlassChip
import com.aminopy.avatek.ui.theme.GlassIconButton
import com.aminopy.avatek.ui.theme.LocalPalette
import com.aminopy.avatek.ui.theme.bounceClick
import com.aminopy.avatek.ui.theme.glass
import com.aminopy.avatek.ui.theme.paletteFor
import com.aminopy.avatek.ui.theme.staggerIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.height

@Composable
private fun Section(title: String, index: Int, content: @Composable () -> Unit) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth().staggerIn(index).glass(RoundedCornerShape(30.dp), strength = 0.6f).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(title, color = p.accent, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        content()
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = p.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = p.muted, fontSize = 11.sp, lineHeight = 16.sp)
        }
        Switch(checked, onChange, colors = SwitchDefaults.colors(checkedTrackColor = p.accent, checkedThumbColor = Color(0xFF14100A)))
    }
}

@Composable
fun SettingsScreen(vm: AvatekViewModel, ui: AppUi) {
    val p = LocalPalette.current
    val bottom = LocalBottomInset.current
    val s = vm.settings
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = bottom)) {
        ScreenHeader("تنظیمات", subtitle = "ظاهر و رفتار AVATEK")
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Section("ظاهر", 0) {
                Text("رنگ اصلی", color = p.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    AccentTheme.entries.forEach { t ->
                        val pal = paletteFor(t)
                        val sel = s.accent == t
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.bounceClick(pressedScale = 0.88f) { vm.updateSettings { it.copy(accent = t) } }) {
                            Box(
                                Modifier.size(50.dp)
                                    .shadow(if (sel) 14.dp else 0.dp, CircleShape, ambientColor = pal.accent, spotColor = pal.accent)
                                    .background(Brush.linearGradient(listOf(pal.accent, pal.secondary)), CircleShape)
                                    .border(if (sel) 3.dp else 1.dp, if (sel) Color.White else Color.White.copy(alpha = 0.25f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) { if (sel) Icon(Icons.Rounded.Check, null, tint = Color(0xFF14100A)) }
                            Text(t.label, color = if (sel) p.text else p.muted, fontSize = 9.sp, modifier = Modifier.padding(top = 5.dp), maxLines = 1)
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("شدت شیشه", color = p.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text("${(s.glass * 100).toInt().fa()}٪", color = p.accent, fontSize = 12.sp)
                }
                Slider(
                    s.glass, { v -> vm.updateSettings { it.copy(glass = v) } },
                    colors = SliderDefaults.colors(thumbColor = p.accent, activeTrackColor = p.accent, inactiveTrackColor = Color.White.copy(alpha = 0.15f))
                )
                ToggleRow("انیمیشن‌ها", "پس‌زمینه‌ی زنده، گذارها و چرخش گرامافون. برای صرفه‌جویی در باتری خاموشش کن.", s.animations) { v -> vm.updateSettings { it.copy(animations = v) } }
                ToggleRow("رنگ محیطی آهنگ", "نور پس‌زمینه از رنگ کاور آهنگ در حال پخش می‌گیرد.", s.ambientColors) { v -> vm.updateSettings { it.copy(ambientColors = v) } }
                ToggleRow("نوارهای ریتم", "نوارها از تحلیل زنده‌ی صدای در حال پخش (FFT) حرکت می‌گیرند و با ضرب آهنگ می‌تپند.", s.visualizer) { v -> vm.updateSettings { it.copy(visualizer = v) } }
                ToggleRow("اعلان اختصاصی AVATEK", "اعلان پخش با رنگ تم و برچسب AVATEK. اگر خاموش باشد، اعلان استاندارد آهنگ سیستم نشان داده می‌شود.", s.customNotification) { v -> vm.updateSettings { it.copy(customNotification = v) } }
                ToggleRow("لرزش لمسی", "بازخورد خفیف هنگام لمس دکمه‌ها.", s.haptics) { v -> vm.updateSettings { it.copy(haptics = v) } }
            }
            Section("کتابخانه", 1) {
                Text("نادیده‌گرفتن آهنگ‌های کوتاه‌تر از", color = p.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("برای حذف صدای نوتیفیکیشن و ضبط‌های کوتاه.", color = p.muted, fontSize = 11.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 15, 30, 60, 120).forEach { sec ->
                        GlassChip(if (sec == 0) "همه" else "${sec.fa()} ثانیه", s.minDurationSec == sec, { vm.updateSettings { it.copy(minDurationSec = sec) } })
                    }
                }
                AccentButton("اسکن دوباره‌ی کتابخانه", { vm.scan() })
            }
            Section("پخش و صدا", 2) {
                Text("اکولایزر، بیس، سرعت و تایمر خواب در آزمایشگاه صدا هستند.", color = p.muted, fontSize = 12.sp)
                GlassChip("باز کردن آزمایشگاه صدا", false, { ui.nav.selectTab(Route.Lab) })
            }
            Section("حریم خصوصی", 3) {
                Text("تاریخچه‌ی پخش و جست‌وجو فقط روی همین دستگاه ذخیره می‌شود.", color = p.muted, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassChip("پاک‌کردن جست‌وجوها", false, { vm.clearSearches(); vm.showToast("جست‌وجوها پاک شد") })
                }
            }
            Section("درباره", 4) {
                Row(
                    Modifier.fillMaxWidth().bounceClick(pressedScale = 0.97f) { ui.nav.push(Route.About) },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Rounded.Info, null, tint = p.accent)
                    Text("درباره‌ی AVATEK", color = p.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text("نسخه ۰٫۲٫۰", color = p.muted, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun AboutScreen(vm: AvatekViewModel, ui: AppUi) {
    val p = LocalPalette.current
    val t = rememberInfiniteTransition(label = "about")
    val ring = t.animateFloat(0f, 360f, infiniteRepeatable(tween(14000, easing = LinearEasing)), label = "ring")
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth()) { GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "بازگشت", { ui.nav.pop() }) }
        Box(Modifier.padding(top = 30.dp).size(200.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val c = Offset(size.width / 2f, size.height / 2f)
                rotate(ring.value, c) {
                    drawCircle(Brush.sweepGradient(listOf(Color.Transparent, p.accent, Color.Transparent, p.secondary, Color.Transparent), c), size.minDimension / 2f, c, style = Stroke(5.dp.toPx()))
                }
                drawCircle(p.accent.copy(alpha = 0.12f), size.minDimension * 0.36f, c)
            }
            Text("A", style = TextStyle(brush = Brush.linearGradient(listOf(p.accent, Color.White)), fontSize = 86.sp, fontWeight = FontWeight.Black))
        }
        Text("AVATEK", color = p.text, fontSize = 34.sp, fontWeight = FontWeight.Black, letterSpacing = 8.sp, modifier = Modifier.padding(top = 18.dp))
        Text("YOUR MUSIC. YOUR WORLD.", color = p.muted, fontSize = 11.sp, letterSpacing = 3.sp)
        Text("نسخه ۰٫۲٫۰", color = p.accent, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        Column(
            Modifier.padding(top = 26.dp).fillMaxWidth().glass(RoundedCornerShape(28.dp), strength = 0.7f).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf(
                "پخش‌کننده‌ی آفلاین؛ بدون حساب کاربری و بدون اینترنت",
                "کتابخانه، آلبوم‌ها، هنرمندان، علاقه‌مندی‌ها و پلی‌لیست‌ها",
                "اکولایزر، تقویت بیس و فضاسازی روی خروجی واقعی پلیر",
                "رابط شیشه‌ای فارسی‌محور با انیمیشن و گذارهای روان"
            ).forEach { Text("•  $it", color = p.text, fontSize = 13.sp, lineHeight = 21.sp, textAlign = TextAlign.Start) }
        }
        Text("ساخته‌شده با دقت ♥", color = p.muted, fontSize = 11.sp, modifier = Modifier.padding(top = 24.dp))
        Box(Modifier.height(40.dp))
    }
}

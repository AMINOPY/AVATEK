package com.aminopy.avatek.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aminopy.avatek.state.AvatekViewModel
import com.aminopy.avatek.state.MusicMood
import com.aminopy.avatek.state.MusicIntelligence
import com.aminopy.avatek.ui.AppUi
import com.aminopy.avatek.ui.LocalBottomInset
import com.aminopy.avatek.ui.theme.AccentButton
import com.aminopy.avatek.ui.theme.GlassChip
import com.aminopy.avatek.ui.theme.LocalPalette
import com.aminopy.avatek.ui.theme.glass
import com.aminopy.avatek.ui.components.ScreenHeader

@Composable
fun IntelligenceScreen(vm: AvatekViewModel, ui: AppUi) {
    val p = LocalPalette.current
    val bottom = LocalBottomInset.current
    var mood by remember { mutableStateOf(MusicMood.ENERGY) }
    val dna = remember(vm.tracks, vm.favorites, vm.playCounts) {
        MusicIntelligence.snapshot(vm.tracks, vm.favorites, vm.playCounts)
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = bottom)) {
        ScreenHeader("AVATEK Intelligence", subtitle = "هوشمندی موسیقی · کاملاً آفلاین و روی دستگاه")
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(28.dp), strength = 0.68f).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = p.accent)
                    Text("AVATEK DNA", color = p.text, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
                Text("نمایه‌ی سلیقه از سابقه‌ی واقعی پخش و علاقه‌مندی‌ها ساخته می‌شود؛ هیچ داده‌ای از دستگاه خارج نمی‌شود.", color = p.muted, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Metric("کل پخش", dna.totalPlays.toString())
                    Metric("آهنگ شنیده‌شده", dna.uniqueTracks.toString())
                    Metric("علاقه‌مندی", dna.favoriteCount.toString())
                }
                if (dna.topArtists.isNotEmpty()) {
                    Text("هنرمندان پرتکرار", color = p.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    dna.topArtists.forEach { (name, count) -> Text("$name · $count پخش", color = p.text, fontSize = 12.sp) }
                }
                if (dna.topGenres.isNotEmpty()) {
                    Text("سبک‌های پرتکرار", color = p.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    dna.topGenres.forEach { (name, count) -> Text("$name · $count پخش", color = p.text, fontSize = 12.sp) }
                }
                if (dna.totalPlays == 0) Text("با گوش‌دادن به موسیقی، DNA به‌تدریج از داده‌های واقعی شکل می‌گیرد.", color = p.muted, fontSize = 11.sp)
            }
            Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(28.dp), strength = 0.68f).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.GraphicEq, null, tint = p.accent)
                    Text("Smart DJ", color = p.text, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
                Text("صف پخش هوشمند با اولویت سلیقه، تنوع هنرمند و کاهش تکرار آهنگ‌های اخیر.", color = p.muted, fontSize = 11.sp)
                AccentButton("ساخت صف هوشمند و پخش", {
                    val mix = vm.smartDj(50)
                    if (mix.isNotEmpty()) { vm.playList(mix); ui.openPlayer() } else vm.showToast("ابتدا موسیقی را به کتابخانه اضافه کن")
                }, icon = Icons.Rounded.PlayArrow)
            }
            Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(28.dp), strength = 0.68f).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Spa, null, tint = p.accent)
                    Text("Mood Studio", color = p.text, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
                Text("میکس بر اساس برچسب‌های سبک و اطلاعات متنی آهنگ‌ها ساخته می‌شود؛ تحلیل هوش مصنوعی صوت یا تشخیص خودکار احساس انجام نمی‌شود.", color = p.muted, fontSize = 11.sp)
                MusicMood.entries.forEach { item ->
                    GlassChip(item.title, mood == item, { mood = item })
                }
                Text(mood.hint, color = p.muted, fontSize = 11.sp)
                AccentButton("ساخت میکس این حال‌وهوا", {
                    val mix = vm.moodMix(mood, 50)
                    if (mix.isNotEmpty()) { vm.playList(mix); ui.openPlayer() } else vm.showToast("کتابخانه برای این میکس آهنگی ندارد")
                }, icon = Icons.Rounded.PlayArrow)
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    val p = LocalPalette.current
    Column(Modifier.weight(1f).glass(RoundedCornerShape(16.dp), strength = 0.42f).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = p.accent, fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text(label, color = p.muted, fontSize = 9.sp)
    }
}

package com.aminopy.avatek.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FiberNew
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aminopy.avatek.ui.AppUi
import com.aminopy.avatek.ui.LocalBottomInset
import com.aminopy.avatek.ui.components.ScreenHeader
import com.aminopy.avatek.ui.fa
import com.aminopy.avatek.state.AvatekViewModel
import com.aminopy.avatek.ui.theme.GlassChip
import com.aminopy.avatek.ui.theme.LocalPalette
import com.aminopy.avatek.ui.theme.glass

private data class CrateDefinition(
    val id: String,
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val crateDefinitions = listOf(
    CrateDefinition("favorites", "منتخب‌های من", "آهنگ‌های موردعلاقه؛ مرتب‌شده با توجه به تعداد پخش", Icons.Rounded.Favorite),
    CrateDefinition("unplayed", "هنوز کشف‌نشده", "آهنگ‌هایی که هنوز در AVATEK پخشی برایشان ثبت نشده", Icons.Rounded.AutoAwesome),
    CrateDefinition("recent", "تازه‌واردها", "جدیدترین فایل‌های اضافه‌شده به کتابخانه", Icons.Rounded.FiberNew),
    CrateDefinition("most_played", "همیشه محبوب", "آهنگ‌هایی که بیشترین دفعات پخش را داشته‌اند", Icons.Rounded.History),
    CrateDefinition("rediscover", "دوباره کشف کن", "آهنگ‌های کم‌شنیده‌شده که اخیراً پخش نشده‌اند", Icons.Rounded.Replay)
)

@Composable
fun SmartCrateScreen(vm: AvatekViewModel, ui: AppUi) {
    val p = LocalPalette.current
    val bottom = LocalBottomInset.current
    val counts = remember(vm.tracks, vm.favorites, vm.playCounts, vm.recentIds) {
        crateDefinitions.associate { it.id to vm.smartCrate(it.id).size }
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottom + 22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader("Smart Crate", subtitle = "مجموعه‌های زنده و آفلاین؛ همیشه هماهنگ با کتابخانه")
        }
        item {
            Column(
                Modifier.padding(horizontal = 20.dp).fillMaxWidth()
                    .glass(RoundedCornerShape(24.dp), strength = 0.58f).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.MusicNote, null, tint = p.accent)
                    Text("جعبه‌های هوشمند موسیقی", color = p.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    "این مجموعه‌ها فایل جدیدی کپی نمی‌کنند و پلی‌لیست ثابت هم نیستند؛ هر بار بر اساس آهنگ‌های موجود، علاقه‌مندی‌ها و سابقه‌ی شنیدن دوباره ساخته می‌شوند.",
                    color = p.muted, fontSize = 11.sp, lineHeight = 17.sp
                )
            }
        }
        items(crateDefinitions, key = { it.id }) { crate ->
            val tracks = vm.smartCrate(crate.id)
            Row(
                Modifier.padding(horizontal = 20.dp).fillMaxWidth()
                    .glass(RoundedCornerShape(22.dp), tint = p.accent, strength = 0.34f)
                    .padding(15.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(crate.icon, null, tint = p.accent, modifier = Modifier.padding(3.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(crate.title, color = p.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(crate.description, color = p.muted, fontSize = 10.sp, lineHeight = 14.sp)
                    Text("${(counts[crate.id] ?: 0).fa()} آهنگ", color = p.accent, fontSize = 10.sp)
                }
                GlassChip(
                    "پخش",
                    false,
                    {
                        if (tracks.isEmpty()) vm.showToast("فعلاً آهنگی برای این مجموعه پیدا نشد")
                        else { vm.playList(tracks); ui.openPlayer() }
                    },
                    leading = Icons.Rounded.PlayArrow
                )
            }
        }
    }
}

package com.aminopy.avatek.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aminopy.avatek.data.AudioTrack
import com.aminopy.avatek.state.AvatekViewModel
import com.aminopy.avatek.ui.AppUi
import com.aminopy.avatek.ui.LocalBottomInset
import com.aminopy.avatek.ui.fa
import com.aminopy.avatek.ui.components.ScreenHeader
import com.aminopy.avatek.ui.theme.AccentButton
import com.aminopy.avatek.ui.theme.GlassChip
import com.aminopy.avatek.ui.theme.LocalPalette
import com.aminopy.avatek.ui.theme.glass
import com.aminopy.avatek.ui.theme.staggerIn

@Composable
fun ListeningStatsScreen(vm: AvatekViewModel, ui: AppUi) {
    val p = LocalPalette.current
    val bottom = LocalBottomInset.current
    val counts = vm.playCounts
    val tracksById = remember(vm.tracks) { vm.tracks.associateBy { it.id } }
    val ranked = remember(vm.tracks, counts) {
        vm.tracks.sortedByDescending { counts[it.id] ?: 0 }.filter { (counts[it.id] ?: 0) > 0 }
    }
    val recent = remember(vm.recentIds, tracksById) {
        vm.recentIds.distinct().mapNotNull { tracksById[it] }.take(8)
    }
    val topArtists = remember(vm.tracks, counts) {
        vm.tracks.groupBy { it.artist }.map { (artist, songs) ->
            artist to songs.sumOf { counts[it.id] ?: 0 }
        }.filter { it.second > 0 }.sortedByDescending { it.second }.take(5)
    }
    val totalPlays = counts.values.sum().coerceAtLeast(0)
    val listenedCount = counts.count { it.value > 0 && it.key in tracksById }
    val topTracks = ranked.take(5)
    val maxArtistPlays = topArtists.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = bottom + 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ScreenHeader("نبض شنیدن", subtitle = "آمار موسیقی تو؛ کاملاً آفلاین")
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp).glass(RoundedCornerShape(28.dp), strength = 0.65f).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Headphones, null, tint = p.accent)
                    Text("خلاصه‌ی شنیدن", color = p.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("کل پخش‌های ثبت‌شده", totalPlays.fa(), Icons.Rounded.PlayArrow, Modifier.weight(1f))
                    StatTile("آهنگ‌های شنیده‌شده", listenedCount.fa(), Icons.Rounded.MusicNote, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("علاقه‌مندی‌ها", vm.favorites.size.fa(), Icons.Rounded.Star, Modifier.weight(1f))
                    StatTile("آهنگ‌های کتابخانه", vm.tracks.size.fa(), Icons.Rounded.History, Modifier.weight(1f))
                }
                Text("آمار بر اساس تعداد شروع پخش‌هایی است که AVATEK ثبت کرده؛ زمان شنیدن تخمینی نمایش داده نمی‌شود.", color = p.muted, fontSize = 10.sp, lineHeight = 15.sp)
            }
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp).glass(RoundedCornerShape(26.dp), strength = 0.55f).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("هنرمندان برتر", color = p.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                if (topArtists.isEmpty()) {
                    Text("بعد از شنیدن چند آهنگ، هنرمندان محبوبت اینجا دیده می‌شوند.", color = p.muted, fontSize = 12.sp)
                } else {
                    topArtists.forEachIndexed { index, item ->
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${(index + 1).fa()}. ${item.first}", color = p.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(item.second.fa(), color = p.accent, fontSize = 11.sp)
                            }
                            Box(Modifier.fillMaxWidth().height(6.dp).background(Color.White.copy(alpha = 0.08f), CircleShape)) {
                                Box(Modifier.fillMaxWidth(item.second.toFloat() / maxArtistPlays).height(6.dp).background(p.accent, CircleShape))
                            }
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("پرتکرارترین آهنگ‌ها", color = p.text, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.ArrowUpward, null, tint = p.accent, modifier = Modifier.size(18.dp))
            }
        }
        if (topTracks.isEmpty()) {
            item {
                Text("هنوز پخشی ثبت نشده است. اولین آهنگت را پخش کن!", color = p.muted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp))
            }
        } else {
            items(topTracks, key = { "top-${it.id}" }) { track ->
                RankedTrack(track, counts[track.id] ?: 0, topTracks, vm, ui)
            }
        }
        item {
            Text("اخیراً شنیده‌شده", color = p.text, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp))
        }
        if (recent.isEmpty()) {
            item {
                Text("تاریخچه‌ی شنیدن هنوز خالی است.", color = p.muted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp))
            }
        } else {
            items(recent, key = { "recent-${it.id}" }) { track ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp).glass(RoundedCornerShape(18.dp), strength = 0.42f)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(track.title, color = p.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(track.artist, color = p.muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    GlassChip("پخش", false, { vm.playTrackIn(recent, track); ui.openPlayer() }, leading = Icons.Rounded.PlayArrow)
                }
            }
        }
    }
}

@Composable
private fun StatTile(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Column(modifier.glass(RoundedCornerShape(18.dp), tint = p.accent, strength = 0.32f).padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Icon(icon, null, tint = p.accent, modifier = Modifier.size(18.dp))
        Text(value, color = p.text, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Text(title, color = p.muted, fontSize = 10.sp, lineHeight = 14.sp)
    }
}

@Composable
private fun RankedTrack(track: AudioTrack, plays: Int, list: List<AudioTrack>, vm: AvatekViewModel, ui: AppUi) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp).glass(RoundedCornerShape(18.dp), strength = 0.42f)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(track.title, color = p.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = p.muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text("${plays.fa()} بار", color = p.accent, fontSize = 11.sp, modifier = Modifier.padding(end = 10.dp))
        GlassChip("پخش", false, { vm.playTrackIn(list, track); ui.openPlayer() }, leading = Icons.Rounded.PlayArrow)
    }
}

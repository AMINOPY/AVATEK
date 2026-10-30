package com.aminopy.avatek.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aminopy.avatek.state.AvatekViewModel
import com.aminopy.avatek.ui.AppUi
import com.aminopy.avatek.ui.Sheet
import com.aminopy.avatek.ui.fa
import com.aminopy.avatek.ui.faDuration
import com.aminopy.avatek.ui.nav.Route
import com.aminopy.avatek.ui.theme.AccentButton
import com.aminopy.avatek.ui.theme.Artwork
import com.aminopy.avatek.ui.theme.GlassChip
import com.aminopy.avatek.ui.theme.GlassDialog
import com.aminopy.avatek.ui.theme.GlassSheet
import com.aminopy.avatek.ui.theme.LocalPalette
import com.aminopy.avatek.ui.theme.bounceClick
import com.aminopy.avatek.ui.theme.glass

/** Renders whichever overlay sheet/dialog is requested through [AppUi.show]. */
@Composable
fun SheetHost(vm: AvatekViewModel, ui: AppUi, modifier: Modifier = Modifier) {
    val current = ui.sheet
    // keep the last non-null sheet so the exit animation still has content to draw
    var last by remember { mutableStateOf<Sheet?>(null) }
    if (current != null) last = current
    val shown = last
    Box(modifier.fillMaxSize()) {
        when (shown) {
            is Sheet.Queue -> GlassSheet(current is Sheet.Queue, ui::dismissSheet, title = "صف پخش") { QueueContent(vm, ui) }
            is Sheet.Sleep -> GlassSheet(current is Sheet.Sleep, ui::dismissSheet, title = "تایمر خواب") { SleepContent(vm, ui) }
            is Sheet.Speed -> GlassSheet(current is Sheet.Speed, ui::dismissSheet, title = "سرعت پخش") { SpeedContent(vm, ui) }
            is Sheet.TrackMenu -> GlassSheet(current is Sheet.TrackMenu, ui::dismissSheet) { TrackMenuContent(vm, ui, shown) }
            is Sheet.AddToPlaylist -> GlassSheet(current is Sheet.AddToPlaylist, ui::dismissSheet, title = "افزودن به پلی‌لیست") { AddToPlaylistContent(vm, ui, shown) }
            is Sheet.NewPlaylist -> GlassDialog(current is Sheet.NewPlaylist, ui::dismissSheet) { NameDialogContent("پلی‌لیست جدید", "", "ساخت") { name ->
                if (vm.createPlaylist(name, shown.firstTrackId)) ui.dismissSheet()
            } }
            is Sheet.RenamePlaylist -> GlassDialog(current is Sheet.RenamePlaylist, ui::dismissSheet) { NameDialogContent("تغییر نام پلی‌لیست", shown.name, "ذخیره") { name ->
                vm.renamePlaylist(shown.id, name); ui.dismissSheet()
            } }
            is Sheet.DeletePlaylist -> GlassDialog(current is Sheet.DeletePlaylist, ui::dismissSheet) { ConfirmContent(vm, ui, shown) }
            null -> Unit
        }
    }
}

@Composable
private fun MenuItem(icon: ImageVector, label: String, tint: Color = LocalPalette.current.text, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().bounceClick(pressedScale = 0.97f, onClick = onClick).padding(horizontal = 22.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(icon, null, tint = if (tint == p.text) p.accent else tint, modifier = Modifier.size(22.dp))
        Text(label, color = tint, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun TrackMenuContent(vm: AvatekViewModel, ui: AppUi, sheet: Sheet.TrackMenu) {
    val p = LocalPalette.current
    val t = sheet.track
    Row(Modifier.padding(horizontal = 22.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Artwork(t.albumKey, t.id, t.albumId, Modifier.size(56.dp), RoundedCornerShape(16.dp))
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(t.title, color = p.text, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${t.artist} • ${t.album}", color = p.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    val isFav = t.id in vm.favorites
    MenuItem(Icons.Rounded.SkipNext, "پخش بعدی") { vm.playNext(t); ui.dismissSheet() }
    MenuItem(Icons.Rounded.QueueMusic, "افزودن به انتهای صف") { vm.addToQueue(t); ui.dismissSheet() }
    MenuItem(Icons.Rounded.PlaylistAdd, "افزودن به پلی‌لیست") { ui.show(Sheet.AddToPlaylist(t.id)) }
    MenuItem(if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, if (isFav) "حذف از علاقه‌مندی‌ها" else "افزودن به علاقه‌مندی‌ها") {
        vm.toggleFavorite(t.id); ui.dismissSheet()
    }
    MenuItem(Icons.Rounded.Album, "رفتن به آلبوم") { ui.dismissSheet(); ui.closePlayer(); ui.nav.push(Route.Album(t.albumKey)) }
    MenuItem(Icons.Rounded.Person, "رفتن به هنرمند") { ui.dismissSheet(); ui.closePlayer(); ui.nav.push(Route.Artist(t.artist)) }
    if (sheet.playlistId != null) {
        MenuItem(Icons.Rounded.Delete, "حذف از این پلی‌لیست", tint = p.danger) {
            vm.removeFromPlaylist(sheet.playlistId, t.id); ui.dismissSheet()
        }
    }
}

@Composable
private fun AddToPlaylistContent(vm: AvatekViewModel, ui: AppUi, sheet: Sheet.AddToPlaylist) {
    val p = LocalPalette.current
    MenuItem(Icons.Rounded.AddCircle, "پلی‌لیست جدید…") { ui.show(Sheet.NewPlaylist(sheet.trackId)) }
    if (vm.playlists.isEmpty()) {
        Text("هنوز پلی‌لیستی نداری؛ اولین را بساز.", color = p.muted, fontSize = 13.sp, modifier = Modifier.padding(22.dp))
    } else {
        LazyColumn(Modifier.weight(1f, fill = false)) {
            itemsIndexed(vm.playlists, key = { _, pl -> pl.id }) { _, pl ->
                MenuItem(Icons.Rounded.PlaylistPlay, "${pl.name}  (${pl.trackIds.size.fa()})") {
                    vm.addToPlaylist(pl.id, sheet.trackId); ui.dismissSheet()
                }
            }
        }
    }
}

@Composable
private fun QueueContent(vm: AvatekViewModel, ui: AppUi) {
    val p = LocalPalette.current
    if (vm.queueIds.isEmpty()) {
        Text("صف خالی است.", color = p.muted, modifier = Modifier.padding(22.dp))
        return
    }
    Row(Modifier.padding(horizontal = 22.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("${vm.queueIds.size.fa()} آهنگ", color = p.muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(
            "پاک‌کردن صف", color = p.danger, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.bounceClick(pressedScale = 0.9f) { vm.clearQueue(); ui.dismissSheet() }.padding(8.dp)
        )
    }
    LazyColumn(Modifier.weight(1f, fill = false)) {
        itemsIndexed(vm.queueIds, key = { i, id -> "$i-$id" }) { index, id ->
            val t = vm.trackById(id) ?: return@itemsIndexed
            val current = index == vm.queueIndex
            Row(
                Modifier.fillMaxWidth().bounceClick(pressedScale = 0.98f) { vm.jumpTo(index) }
                    .padding(horizontal = 18.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(46.dp)) {
                    Artwork(t.albumKey, t.id, t.albumId, Modifier.fillMaxSize(), RoundedCornerShape(14.dp))
                }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(t.title, color = if (current) p.accent else p.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${t.artist} • ${faDuration(t.durationMs)}", color = p.muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (current) PlayingBars(Modifier.padding(horizontal = 8.dp), playing = vm.isPlaying)
                else if (index > vm.queueIndex + 1) {
                    Icon(
                        Icons.Rounded.KeyboardArrowUp, "بیاور بعد از آهنگ فعلی", tint = p.muted,
                        modifier = Modifier.size(36.dp).bounceClick(pressedScale = 0.8f) { vm.moveInQueue(index, vm.queueIndex + 1) }.padding(6.dp)
                    )
                }
                Icon(
                    Icons.Rounded.Close, "حذف از صف", tint = p.muted,
                    modifier = Modifier.size(36.dp).bounceClick(pressedScale = 0.8f) { vm.removeFromQueue(index) }.padding(8.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SleepContent(vm: AvatekViewModel, ui: AppUi) {
    val p = LocalPalette.current
    val remaining = vm.sleepRemainingMs
    Text(
        if (remaining != null) "پخش تا ${faDuration(remaining)} دیگر متوقف می‌شود" else "پخش بعد از زمان انتخابی خودکار متوقف می‌شود.",
        color = p.muted, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 22.dp, vertical = 4.dp)
    )
    FlowRow(
        Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        listOf(10, 15, 30, 45, 60, 90).forEach { m ->
            GlassChip("${m.fa()} دقیقه", false, { vm.startSleepTimer(m); ui.dismissSheet() })
        }
        GlassChip("پایان آهنگ", false, { vm.sleepAtEndOfTrack(); ui.dismissSheet() }, leading = Icons.Rounded.Bedtime)
    }
    if (remaining != null) {
        Box(Modifier.fillMaxWidth().padding(bottom = 14.dp), contentAlignment = Alignment.Center) {
            AccentButton("لغو تایمر", { vm.cancelSleep(); ui.dismissSheet() })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpeedPicker(vm: AvatekViewModel, modifier: Modifier = Modifier, onPicked: () -> Unit = {}) {
    FlowRow(
        modifier, horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { s ->
            GlassChip("${s}x".fa(), vm.speed == s, { vm.setSpeed(s); onPicked() })
        }
    }
}

@Composable
private fun SpeedContent(vm: AvatekViewModel, ui: AppUi) {
    SpeedPicker(vm, Modifier.padding(horizontal = 22.dp, vertical = 14.dp)) { ui.dismissSheet() }
}

@Composable
private fun NameDialogContent(title: String, initial: String, action: String, onSubmit: (String) -> Unit) {
    val p = LocalPalette.current
    var text by remember { mutableStateOf(initial) }
    Text(title, color = p.text, fontSize = 19.sp, fontWeight = FontWeight.Black)
    Box(Modifier.padding(vertical = 16.dp)) {
        GlassTextField(text, { text = it }, "نام پلی‌لیست", imeAction = ImeAction.Done, onImeAction = { if (text.isNotBlank()) onSubmit(text) })
    }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        AccentButton(action, { onSubmit(text) }, enabled = text.isNotBlank())
    }
}

@Composable
private fun ConfirmContent(vm: AvatekViewModel, ui: AppUi, sheet: Sheet.DeletePlaylist) {
    val p = LocalPalette.current
    Text("حذف پلی‌لیست", color = p.text, fontSize = 19.sp, fontWeight = FontWeight.Black)
    Text("«${sheet.name}» حذف شود؟ آهنگ‌ها از دستگاه پاک نمی‌شوند.", color = p.muted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 14.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)) {
        GlassChip("انصراف", false, { ui.dismissSheet() })
        AccentButton("حذف", { vm.deletePlaylist(sheet.id); ui.dismissSheet(); if (ui.nav.canPop) ui.nav.pop() })
    }
}

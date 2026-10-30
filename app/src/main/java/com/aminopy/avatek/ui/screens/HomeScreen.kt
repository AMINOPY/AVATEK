package com.aminopy.avatek.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aminopy.avatek.state.AvatekViewModel
import com.aminopy.avatek.state.LibraryStatus
import com.aminopy.avatek.state.SortMode
import com.aminopy.avatek.ui.AppUi
import com.aminopy.avatek.ui.LocalBottomInset
import com.aminopy.avatek.ui.LocalPermissionRequester
import com.aminopy.avatek.ui.components.CoverCard
import com.aminopy.avatek.ui.components.EmptyState
import com.aminopy.avatek.ui.components.TrackRow
import com.aminopy.avatek.ui.components.rememberSpin
import com.aminopy.avatek.ui.fa
import com.aminopy.avatek.ui.faTotal
import com.aminopy.avatek.ui.nav.Route
import com.aminopy.avatek.ui.theme.AccentButton
import com.aminopy.avatek.ui.theme.Artwork
import com.aminopy.avatek.ui.theme.LocalPalette
import com.aminopy.avatek.ui.theme.SectionTitle
import com.aminopy.avatek.ui.theme.bounceClick
import com.aminopy.avatek.ui.theme.glass
import com.aminopy.avatek.ui.theme.staggerIn
import java.util.Calendar

@Composable
fun HomeScreen(vm: AvatekViewModel, ui: AppUi) {
    val p = LocalPalette.current
    val bottom = LocalBottomInset.current
    val requestPermission = LocalPermissionRequester.current

    when (vm.status) {
        LibraryStatus.NeedsPermission -> {
            PermissionScreen(requestPermission)
            return
        }
        LibraryStatus.Scanning -> {
            ScanningScreen()
            return
        }
        LibraryStatus.Empty -> {
            Box(Modifier.fillMaxSize().statusBarsPadding(), contentAlignment = Alignment.Center) {
                EmptyState(
                    Icons.Rounded.MusicNote, "آهنگی پیدا نشد",
                    "فایل‌های صوتی را روی دستگاه بگذار و دوباره اسکن کن. آهنگ‌های کوتاه‌تر از حد تنظیمات نادیده گرفته می‌شوند.",
                    actionLabel = "اسکن دوباره", onAction = { vm.scan() }
                )
            }
            return
        }
        LibraryStatus.Ready -> Unit
    }

    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 5..11 -> "صبح بخیر"
        in 12..16 -> "ظهر بخیر"
        in 17..20 -> "عصر بخیر"
        else -> "شب بخیر"
    }
    val recent = vm.recentlyPlayed()
    val top = vm.mostPlayed(5)
    val added = vm.recentlyAdded(5)
    val sortedAll = vm.sorted(vm.tracks, SortMode.Title)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottom),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp).staggerIn(0),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(greeting, color = p.muted, fontSize = 13.sp)
                    Text(
                        "AVATEK",
                        style = TextStyle(
                            brush = Brush.linearGradient(listOf(p.accent, Color.White, p.accent)),
                            fontSize = 32.sp, fontWeight = FontWeight.Black, letterSpacing = 5.sp
                        )
                    )
                }
                Box(
                    Modifier.size(46.dp).bounceClick { vm.scan() }.glass(CircleShape),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Rounded.Refresh, "اسکن دوباره", tint = p.text, modifier = Modifier.size(22.dp)) }
            }
        }
        item { HeroCard(vm, ui, sortedAll, Modifier.padding(horizontal = 20.dp).staggerIn(1)) }
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp).staggerIn(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatChip("آهنگ", vm.tracks.size.fa(), Modifier.weight(1f))
                StatChip("آلبوم", vm.albums.size.fa(), Modifier.weight(1f))
                StatChip("هنرمند", vm.artists.size.fa(), Modifier.weight(1f))
                StatChip("مدت", faTotal(vm.tracks.sumOf { it.durationMs }).replace(" و ", "\n"), Modifier.weight(1.3f))
            }
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp).staggerIn(3), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickTile(Icons.Rounded.Favorite, "علاقه‌مندی‌ها", "${vm.favorites.size.fa()} آهنگ", Color(0xFFFF4D6D), Modifier.weight(1f)) { ui.nav.push(Route.Favorites) }
                    QuickTile(Icons.Rounded.PlaylistPlay, "پلی‌لیست‌ها", "${vm.playlists.size.fa()} لیست", p.accent, Modifier.weight(1f)) { vm.libraryTab = 3; ui.nav.selectTab(Route.Library) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickTile(Icons.Rounded.Shuffle, "پخش تصادفی", "همه‌ی آهنگ‌ها", p.secondary, Modifier.weight(1f)) { vm.playList(sortedAll, shuffled = true); ui.openPlayer() }
                    QuickTile(Icons.Rounded.GraphicEq, "آزمایشگاه صدا", "اکولایزر و بیس", Color(0xFF5EEAD4), Modifier.weight(1f)) { ui.nav.selectTab(Route.Lab) }
                }
            }
        }
        if (recent.isNotEmpty()) {
            item { SectionTitle("اخیراً پخش‌شده", subtitle = "ادامه‌ی همان‌جا که رها کردی") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(recent, key = { it.id }) { t ->
                        CoverCard(t.title, t.artist, t.albumKey, t.id, t.albumId, { vm.playTrackIn(recent, t) })
                    }
                }
            }
        }
        if (top.isNotEmpty()) {
            item { SectionTitle("پرشنیده‌ترین‌ها", subtitle = "بر اساس تعداد پخش روی همین دستگاه") }
            items(top.size) { i ->
                TrackRow(top[i], top, Modifier.padding(horizontal = 20.dp), rank = i + 1)
            }
        }
        if (vm.albums.isNotEmpty()) {
            item { SectionTitle("آلبوم‌ها", action = "همه", onAction = { vm.libraryTab = 1; ui.nav.selectTab(Route.Library) }) }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(vm.albums.take(16), key = { it.key }) { a ->
                        val first = vm.trackById(a.trackIds.first())
                        CoverCard(a.title, a.artist, a.key, first?.id ?: 0L, first?.albumId ?: 0L, { ui.nav.push(Route.Album(a.key)) })
                    }
                }
            }
        }
        if (vm.artists.isNotEmpty()) {
            item { SectionTitle("هنرمندان", action = "همه", onAction = { vm.libraryTab = 2; ui.nav.selectTab(Route.Library) }) }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(vm.artists.take(16), key = { it.name }) { a ->
                        val first = vm.trackById(a.trackIds.first())
                        Column(
                            Modifier.bounceClick(pressedScale = 0.93f) { ui.nav.push(Route.Artist(a.name)) },
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(Modifier.size(92.dp).glass(CircleShape, strength = 0.5f).padding(3.dp)) {
                                Artwork(a.name, first?.id ?: 0L, first?.albumId ?: 0L, Modifier.fillMaxSize(), CircleShape, 300)
                            }
                            Text(a.name, color = p.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.size(width = 92.dp, height = 18.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
            }
        }
        if (added.isNotEmpty()) {
            item { SectionTitle("تازه اضافه‌شده", subtitle = "جدیدترین فایل‌های دستگاه") }
            items(added.size) { i -> TrackRow(added[i], added, Modifier.padding(horizontal = 20.dp)) }
        }
        item { Spacer(Modifier.height(4.dp)) }
    }
}

@Composable
private fun StatChip(label: String, value: String, modifier: Modifier) {
    val p = LocalPalette.current
    Column(
        modifier.glass(RoundedCornerShape(20.dp), strength = 0.5f).padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = p.accent, fontSize = 15.sp, fontWeight = FontWeight.Black, lineHeight = 18.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text(label, color = p.muted, fontSize = 10.sp)
    }
}

@Composable
private fun QuickTile(icon: ImageVector, title: String, subtitle: String, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        modifier.bounceClick(pressedScale = 0.95f, onClick = onClick).glass(RoundedCornerShape(24.dp), strength = 0.55f).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(42.dp).background4(tint),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp)) }
        Column {
            Text(title, color = p.text, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(subtitle, color = p.muted, fontSize = 10.sp, maxLines = 1)
        }
    }
}

private fun Modifier.background4(tint: Color): Modifier =
    this.then(Modifier.glass(RoundedCornerShape(15.dp), tint = tint, strength = 0.7f))

@Composable
private fun HeroCard(vm: AvatekViewModel, ui: AppUi, all: List<com.aminopy.avatek.data.AudioTrack>, modifier: Modifier) {
    val p = LocalPalette.current
    val current = vm.currentTrack ?: vm.recentlyPlayed(1).firstOrNull()
    val spin = rememberSpin(vm.isPlaying && vm.currentTrack != null, 14000)
    Box(modifier.fillMaxWidth().glass(RoundedCornerShape(32.dp), tint = p.accent, strength = 0.9f)) {
        Canvas(Modifier.matchParentSize()) {
            for (i in 1..5) {
                drawCircle(
                    p.accent.copy(alpha = 0.10f - i * 0.015f),
                    radius = size.height * (0.35f + i * 0.2f),
                    center = Offset(size.width * 0.82f, size.height * 0.5f),
                    style = Stroke(1.5.dp.toPx())
                )
            }
        }
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    if (current != null) (if (vm.currentTrack != null) "در حال پخش" else "ادامه پخش") else "آماده‌ای؟",
                    color = p.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                )
                Text(
                    current?.title ?: "صدای خودت را پیدا کن", color = p.text, fontSize = 20.sp, fontWeight = FontWeight.Black,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 26.sp
                )
                Text(
                    current?.artist ?: "${all.size.fa()} آهنگ آفلاین منتظر توست", color = p.muted, fontSize = 12.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AccentButton(
                        if (vm.currentTrack != null) "باز کردن پلیر" else if (current != null) "ادامه" else "پخش همه",
                        {
                            if (vm.currentTrack != null) ui.openPlayer()
                            else if (current != null) { vm.playTrackIn(all, current); ui.openPlayer() }
                            else { vm.playList(all); ui.openPlayer() }
                        },
                        icon = Icons.Rounded.PlayArrow
                    )
                }
            }
            Box(Modifier.size(118.dp).graphicsLayer { rotationZ = spin.value }.glass(CircleShape, strength = 0.8f).padding(4.dp)) {
                Artwork(current?.albumKey ?: "avatek", current?.id ?: 0L, current?.albumId ?: 0L, Modifier.fillMaxSize(), CircleShape, 400)
            }
        }
    }
}


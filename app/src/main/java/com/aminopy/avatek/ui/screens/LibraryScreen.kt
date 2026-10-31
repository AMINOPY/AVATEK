@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.aminopy.avatek.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aminopy.avatek.data.AudioPlaylist
import com.aminopy.avatek.state.AvatekViewModel
import com.aminopy.avatek.state.LibraryStatus
import com.aminopy.avatek.state.SortMode
import com.aminopy.avatek.ui.AppUi
import com.aminopy.avatek.ui.LocalBottomInset
import com.aminopy.avatek.ui.Sheet
import com.aminopy.avatek.ui.components.CoverCard
import com.aminopy.avatek.ui.components.EmptyState
import com.aminopy.avatek.ui.components.ScreenHeader
import com.aminopy.avatek.ui.components.TrackRow
import com.aminopy.avatek.ui.fa
import com.aminopy.avatek.ui.faDuration
import com.aminopy.avatek.ui.faTotal
import com.aminopy.avatek.ui.nav.Route
import com.aminopy.avatek.ui.theme.AccentButton
import com.aminopy.avatek.ui.theme.Artwork
import com.aminopy.avatek.ui.theme.GeneratedCover
import com.aminopy.avatek.ui.theme.GlassChip
import com.aminopy.avatek.ui.theme.LocalPalette
import com.aminopy.avatek.ui.theme.bounceClick
import com.aminopy.avatek.ui.theme.glass
import com.aminopy.avatek.ui.theme.staggerIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val LibraryTabs = listOf("آهنگ‌ها", "آلبوم‌ها", "هنرمندان", "پوشه‌ها", "سبک‌ها", "لیست‌ها")

@Composable
fun LibraryScreen(vm: AvatekViewModel, ui: AppUi) {
    if (vm.status == LibraryStatus.NeedsPermission || vm.status == LibraryStatus.Scanning) {
        HomeScreen(vm, ui)
        return
    }
    val pager = rememberPagerState(initialPage = vm.libraryTab.coerceIn(0, LibraryTabs.lastIndex)) { LibraryTabs.size }
    val scope = rememberCoroutineScope()
    LaunchedEffect(pager.currentPage) { vm.libraryTab = pager.currentPage }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("کتابخانه", subtitle = "${vm.tracks.size.fa()} آهنگ • ${faTotal(vm.tracks.sumOf { it.durationMs })}")
        SegmentedTabs(
            titles = LibraryTabs,
            position = pager.currentPage + pager.currentPageOffsetFraction,
            onSelect = { scope.launch { pager.animateScrollToPage(it) } },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )
        HorizontalPager(pager, Modifier.weight(1f), beyondViewportPageCount = 1) { page ->
            when (page) {
                0 -> SongsPage(vm, ui)
                1 -> AlbumsPage(vm, ui)
                2 -> ArtistsPage(vm, ui)
                3 -> FoldersPage(vm, ui)
                4 -> GenresPage(vm, ui)
                else -> PlaylistsPage(vm, ui)
            }
        }
    }
}

/** Glass segmented control whose lens follows the pager's finger-tracking position. */
@Composable
fun SegmentedTabs(titles: List<String>, position: Float, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    BoxWithConstraints(modifier.fillMaxWidth().height(46.dp).glass(RoundedCornerShape(23.dp), strength = 0.7f)) {
        val itemPx = with(LocalDensity.current) { (maxWidth / titles.size).toPx() }
        val lens = RoundedCornerShape(19.dp)
        Box(
            Modifier.fillMaxSize().layout { m, c ->
                val w = itemPx.roundToInt()
                val pl = m.measure(Constraints.fixed(w, c.maxHeight))
                layout(c.maxWidth, c.maxHeight) { pl.placeRelative((position * itemPx).roundToInt(), 0) }
            }.padding(4.dp).clip(lens)
                .background(Brush.verticalGradient(listOf(p.accent, p.accentDeep)))
        )
        Row(Modifier.fillMaxSize()) {
            titles.forEachIndexed { i, title ->
                val near = (1f - kotlin.math.abs(position - i)).coerceIn(0f, 1f)
                Box(Modifier.weight(1f).fillMaxHeight().bounceClick(pressedScale = 0.94f) { onSelect(i) }, contentAlignment = Alignment.Center) {
                    Text(
                        title,
                        color = androidx.compose.ui.graphics.lerp(p.muted, Color(0xFF14100A), near),
                        fontSize = 12.sp, fontWeight = if (near > 0.5f) FontWeight.Black else FontWeight.Medium, maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun SongsPage(vm: AvatekViewModel, ui: AppUi) {
    val bottom = LocalBottomInset.current
    val list = remember(vm.tracks, vm.librarySort, vm.playCounts) { vm.sorted(vm.tracks) }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = bottom),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    AccentButton("پخش همه", { vm.playList(list); ui.openPlayer() }, icon = Icons.Rounded.PlayArrow)
                    GlassChip("تصادفی", false, { vm.playList(list, shuffled = true); ui.openPlayer() }, leading = Icons.Rounded.Shuffle)
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SortMode.entries.toList()) { m -> GlassChip(m.label, vm.librarySort == m, { vm.librarySort = m }) }
                }
            }
        }
        itemsIndexed(list, key = { _, t -> t.id }) { i, t -> TrackRow(t, list, Modifier.staggerIn(i)) }
    }
}

@Composable
private fun AlbumsPage(vm: AvatekViewModel, ui: AppUi) {
    val bottom = LocalBottomInset.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val cell = (maxWidth - 40.dp - 14.dp) / 2
        LazyVerticalGrid(
            GridCells.Fixed(2), Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = bottom),
            horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            items(vm.albums, key = { it.key }) { a ->
                val first = vm.trackById(a.trackIds.first())
                CoverCard(
                    a.title, "${a.artist} • ${a.trackIds.size.fa()} آهنگ", a.key, first?.id ?: 0L, first?.albumId ?: 0L,
                    { ui.nav.push(Route.Album(a.key)) }, Modifier.staggerIn(0), size = cell
                )
            }
        }
    }
}

@Composable
private fun ArtistsPage(vm: AvatekViewModel, ui: AppUi) {
    val bottom = LocalBottomInset.current
    val p = LocalPalette.current
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = bottom),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(vm.artists, key = { _, a -> a.name }) { i, a ->
            val first = vm.trackById(a.trackIds.first())
            Row(
                Modifier.fillMaxWidth().staggerIn(i).bounceClick(pressedScale = 0.97f) { ui.nav.push(Route.Artist(a.name)) }
                    .glass(RoundedCornerShape(24.dp), strength = 0.4f).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Artwork(a.name, first?.id ?: 0L, first?.albumId ?: 0L, Modifier.size(58.dp), CircleShape, 240)
                Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                    Text(a.name, color = p.text, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${a.trackIds.size.fa()} آهنگ • ${a.albumCount.fa()} آلبوم", color = p.muted, fontSize = 11.sp)
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, null, tint = p.muted)
            }
        }
    }
}

@Composable
private fun FoldersPage(vm: AvatekViewModel, ui: AppUi) {
    val bottom = LocalBottomInset.current
    val p = LocalPalette.current
    val groups = remember(vm.tracks) { vm.tracks.groupBy { it.folderPath }.toSortedMap() }
    var selected by remember { mutableStateOf<String?>(null) }
    val selectedTracks = selected?.let { groups[it].orEmpty() }.orEmpty()
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = bottom),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (selected != null) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    GlassChip("بازگشت به پوشه‌ها", false, { selected = null })
                    AccentButton("پخش پوشه", { vm.playList(selectedTracks); ui.openPlayer() }, icon = Icons.Rounded.PlayArrow, enabled = selectedTracks.isNotEmpty())
                }
            }
            itemsIndexed(selectedTracks, key = { _, t -> t.id }) { i, t ->
                TrackRow(t, selectedTracks, Modifier.staggerIn(i))
            }
        } else {
            items(groups.entries.toList(), key = { it.key }) { (folder, tracks) ->
                Row(
                    Modifier.fillMaxWidth().bounceClick(pressedScale = 0.97f) { selected = folder }
                        .glass(RoundedCornerShape(22.dp), strength = 0.42f).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Rounded.Folder, null, tint = p.accent, modifier = Modifier.size(28.dp))
                    Column(Modifier.weight(1f)) {
                        Text(folder, color = p.text, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${tracks.size.fa()} آهنگ", color = p.muted, fontSize = 11.sp)
                    }
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, null, tint = p.muted)
                }
            }
            if (groups.isEmpty()) item { EmptyState(Icons.Rounded.Folder, "پوشه‌ای پیدا نشد", "بعد از اسکن کتابخانه، پوشه‌های موسیقی اینجا نمایش داده می‌شوند.") }
        }
    }
}

@Composable
private fun GenresPage(vm: AvatekViewModel, ui: AppUi) {
    val bottom = LocalBottomInset.current
    val p = LocalPalette.current
    val groups = remember(vm.tracks) { vm.tracks.groupBy { it.genre }.toSortedMap() }
    var selected by remember { mutableStateOf<String?>(null) }
    val selectedTracks = selected?.let { groups[it].orEmpty() }.orEmpty()
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = bottom),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (selected != null) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    GlassChip("بازگشت به سبک‌ها", false, { selected = null })
                    AccentButton("پخش سبک", { vm.playList(selectedTracks); ui.openPlayer() }, icon = Icons.Rounded.PlayArrow, enabled = selectedTracks.isNotEmpty())
                }
            }
            itemsIndexed(selectedTracks, key = { _, t -> t.id }) { i, t ->
                TrackRow(t, selectedTracks, Modifier.staggerIn(i))
            }
        } else {
            items(groups.entries.toList(), key = { it.key }) { (genre, tracks) ->
                Row(
                    Modifier.fillMaxWidth().bounceClick(pressedScale = 0.97f) { selected = genre }
                        .glass(RoundedCornerShape(22.dp), strength = 0.42f).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Rounded.Category, null, tint = p.accent, modifier = Modifier.size(28.dp))
                    Column(Modifier.weight(1f)) {
                        Text(genre, color = p.text, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${tracks.size.fa()} آهنگ", color = p.muted, fontSize = 11.sp)
                    }
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, null, tint = p.muted)
                }
            }
            if (groups.isEmpty()) item { EmptyState(Icons.Rounded.Category, "سبکی پیدا نشد", "اطلاعات سبک از نمایه‌ی محلی موسیقی خوانده می‌شود.") }
        }
    }
}

@Composable
private fun PlaylistsPage(vm: AvatekViewModel, ui: AppUi) {
    val bottom = LocalBottomInset.current
    val p = LocalPalette.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val cell = (maxWidth - 40.dp - 14.dp) / 2
        LazyVerticalGrid(
            GridCells.Fixed(2), Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = bottom),
            horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Column(Modifier.bounceClick(pressedScale = 0.95f) { ui.show(Sheet.NewPlaylist()) }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier.size(cell).glass(RoundedCornerShape(24.dp), tint = p.accent, strength = 0.6f),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Rounded.Add, null, tint = p.accent, modifier = Modifier.size(46.dp)) }
                    Text("پلی‌لیست جدید", color = p.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 4.dp))
                }
            }
            items(vm.playlists, key = { it.id }) { pl ->
                Column(Modifier.bounceClick(pressedScale = 0.95f) { ui.nav.push(Route.Playlist(pl.id)) }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlaylistMosaic(vm, pl, Modifier.size(cell))
                    Column(Modifier.padding(horizontal = 4.dp)) {
                        Text(pl.name, color = p.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${pl.trackIds.size.fa()} آهنگ", color = p.muted, fontSize = 11.sp)
                    }
                }
            }
            if (vm.playlists.isEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    EmptyState(
                        Icons.Rounded.Add, "هنوز پلی‌لیستی نداری",
                        "با دکمه‌ی «پلی‌لیست جدید» اولین مجموعه‌ات را بساز و از منوی هر آهنگ به آن اضافه کن."
                    )
                }
            }
        }
    }
}

/** 2×2 collage of the first covers in a playlist; generated cover when empty. */
@Composable
fun PlaylistMosaic(vm: AvatekViewModel, pl: AudioPlaylist, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(24.dp)
    val tracks = remember(pl.trackIds, vm.tracks) { vm.tracksOf(pl.trackIds.take(4)) }
    Box(modifier.glass(shape, strength = 0.4f).padding(3.dp).clip(RoundedCornerShape(21.dp))) {
        if (tracks.size < 4) {
            if (tracks.isEmpty()) GeneratedCover(pl.id)
            else Artwork(tracks[0].albumKey, tracks[0].id, tracks[0].albumId, Modifier.fillMaxSize(), RoundedCornerShape(0.dp), 400)
        } else {
            Column(Modifier.fillMaxSize()) {
                for (r in 0..1) Row(Modifier.weight(1f)) {
                    for (c in 0..1) {
                        val t = tracks[r * 2 + c]
                        Artwork(t.albumKey, t.id, t.albumId, Modifier.weight(1f).fillMaxHeight(), RoundedCornerShape(0.dp), 200)
                    }
                }
            }
        }
    }
}

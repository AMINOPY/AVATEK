package com.aminopy.avatek.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aminopy.avatek.data.AudioTrack
import com.aminopy.avatek.state.AvatekViewModel
import com.aminopy.avatek.ui.AppUi
import com.aminopy.avatek.ui.LocalBottomInset
import com.aminopy.avatek.ui.Sheet
import com.aminopy.avatek.ui.components.EmptyState
import com.aminopy.avatek.ui.components.TrackRow
import com.aminopy.avatek.ui.fa
import com.aminopy.avatek.ui.faTotal
import com.aminopy.avatek.ui.theme.AccentButton
import com.aminopy.avatek.ui.theme.Artwork
import com.aminopy.avatek.ui.theme.GlassChip
import com.aminopy.avatek.ui.theme.GlassIconButton
import com.aminopy.avatek.ui.theme.LocalPalette
import com.aminopy.avatek.ui.theme.glass
import com.aminopy.avatek.ui.theme.staggerIn
import kotlin.math.min

/** What the big cover at the top of a detail page shows. */
private enum class CoverStyle { Square, Circle, Mosaic, Heart }

@Composable
private fun DetailScaffold(
    vm: AvatekViewModel,
    ui: AppUi,
    title: String,
    subtitle: String,
    meta: String,
    seed: String,
    tracks: List<AudioTrack>,
    cover: CoverStyle,
    modifier: Modifier = Modifier,
    playlistId: String? = null,
    actions: @Composable () -> Unit = {},
    emptyTitle: String = "اینجا هنوز آهنگی نیست",
    emptyText: String = ""
) {
    val p = LocalPalette.current
    val bottom = LocalBottomInset.current
    val state = rememberLazyListState()
    val first = tracks.firstOrNull()
    fun scrollPx(): Float = if (state.firstVisibleItemIndex == 0) state.firstVisibleItemScrollOffset.toFloat() else 2000f

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(), state = state,
            contentPadding = PaddingValues(bottom = bottom),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Column(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 64.dp, bottom = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(230.dp).graphicsLayer {
                            val s = scrollPx()
                            val k = 1f - min(s / 900f, 0.3f)
                            scaleX = k; scaleY = k
                            alpha = 1f - (s / 520f).coerceIn(0f, 1f)
                            translationY = s * 0.45f
                        }
                    ) {
                        val shape = if (cover == CoverStyle.Circle) CircleShape else RoundedCornerShape(34.dp)
                        Box(Modifier.fillMaxSize().shadow(36.dp, shape, ambientColor = p.accent, spotColor = p.accent).glass(shape, strength = 0.8f).padding(4.dp)) {
                            val inner = if (cover == CoverStyle.Circle) CircleShape else RoundedCornerShape(30.dp)
                            when (cover) {
                                CoverStyle.Heart -> Box(
                                    Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFFFF4D6D), p.secondary, p.accentDeep)), inner),
                                    contentAlignment = Alignment.Center
                                ) { Icon(Icons.Rounded.Favorite, null, tint = Color.White.copy(alpha = 0.92f), modifier = Modifier.size(96.dp)) }
                                CoverStyle.Mosaic -> {
                                    val items = tracks.take(4)
                                    if (items.size < 4) Artwork(seed, first?.id ?: 0L, first?.albumId ?: 0L, Modifier.fillMaxSize(), inner, 600)
                                    else Column(Modifier.fillMaxSize()) {
                                        for (r in 0..1) Row(Modifier.weight(1f)) {
                                            for (c in 0..1) {
                                                val t = items[r * 2 + c]
                                                Artwork(t.albumKey, t.id, t.albumId, Modifier.weight(1f).fillMaxSize(), RoundedCornerShape(0.dp), 300)
                                            }
                                        }
                                    }
                                }
                                else -> Artwork(seed, first?.id ?: 0L, first?.albumId ?: 0L, Modifier.fillMaxSize(), inner, 700)
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(title, color = p.text, fontSize = 26.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 28.dp))
                    if (subtitle.isNotBlank()) Text(subtitle, color = p.accent, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                    Text(meta, color = p.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        AccentButton("پخش", { vm.playList(tracks); ui.openPlayer() }, icon = Icons.Rounded.PlayArrow, enabled = tracks.isNotEmpty())
                        GlassChip("تصادفی", false, { if (tracks.isNotEmpty()) { vm.playList(tracks, shuffled = true); ui.openPlayer() } }, leading = Icons.Rounded.Shuffle)
                        GlassIconButton(Icons.Rounded.PlaylistAdd, "افزودن به صف", { vm.addAllToQueue(tracks) }, size = 42.dp)
                        actions()
                    }
                }
            }
            if (tracks.isEmpty()) {
                item { EmptyState(Icons.Rounded.MusicOff, emptyTitle, emptyText) }
            }
            itemsIndexed(tracks, key = { i, t -> "$i-${t.id}" }) { i, t ->
                TrackRow(t, tracks, Modifier.padding(horizontal = 20.dp).staggerIn(i), playlistId = playlistId)
            }
        }

        // top bar: back button + title that fades in once the cover has scrolled away
        Row(
            Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "بازگشت", { ui.nav.pop() })
            Text(
                title, color = p.text, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 14.dp).graphicsLayer { alpha = ((scrollPx() - 330f) / 120f).coerceIn(0f, 1f) }
            )
        }
    }
}

@Composable
fun AlbumScreen(vm: AvatekViewModel, ui: AppUi, key: String) {
    val album = vm.albums.firstOrNull { it.key == key }
    if (album == null) { LaunchedEffect(Unit) { ui.nav.pop() }; return }
    val tracks = remember(album, vm.tracks) { vm.tracksOf(album.trackIds) }
    DetailScaffold(
        vm, ui, album.title, album.artist, "${tracks.size.fa()} آهنگ • ${faTotal(album.durationMs)}",
        album.key, tracks, CoverStyle.Square
    )
}

@Composable
fun ArtistScreen(vm: AvatekViewModel, ui: AppUi, name: String) {
    val artist = vm.artists.firstOrNull { it.name == name }
    if (artist == null) { LaunchedEffect(Unit) { ui.nav.pop() }; return }
    val tracks = remember(artist, vm.tracks) { vm.sorted(vm.tracksOf(artist.trackIds), com.aminopy.avatek.state.SortMode.Title) }
    DetailScaffold(
        vm, ui, artist.name, "${artist.albumCount.fa()} آلبوم", "${tracks.size.fa()} آهنگ • ${faTotal(tracks.sumOf { it.durationMs })}",
        artist.name, tracks, CoverStyle.Circle
    )
}

@Composable
fun PlaylistScreen(vm: AvatekViewModel, ui: AppUi, id: String) {
    val playlist = vm.playlists.firstOrNull { it.id == id }
    if (playlist == null) { LaunchedEffect(Unit) { ui.nav.pop() }; return }
    val tracks = remember(playlist, vm.tracks) { vm.tracksOf(playlist.trackIds) }
    DetailScaffold(
        vm, ui, playlist.name, "پلی‌لیست من", "${tracks.size.fa()} آهنگ • ${faTotal(tracks.sumOf { it.durationMs })}",
        playlist.id, tracks, CoverStyle.Mosaic, playlistId = playlist.id,
        actions = {
            GlassIconButton(Icons.Rounded.Edit, "تغییر نام", { ui.show(Sheet.RenamePlaylist(playlist.id, playlist.name)) }, size = 42.dp)
            GlassIconButton(Icons.Rounded.Delete, "حذف", { ui.show(Sheet.DeletePlaylist(playlist.id, playlist.name)) }, size = 42.dp, tint = LocalPalette.current.danger)
        },
        emptyTitle = "پلی‌لیست خالی است",
        emptyText = "از منوی «⋮» هر آهنگ، گزینه‌ی «افزودن به پلی‌لیست» را بزن."
    )
}

@Composable
fun FavoritesScreen(vm: AvatekViewModel, ui: AppUi) {
    val tracks = remember(vm.tracks, vm.favorites) { vm.tracks.filter { it.id in vm.favorites } }
    DetailScaffold(
        vm, ui, "علاقه‌مندی‌ها", "آهنگ‌های دوست‌داشتنی", "${tracks.size.fa()} آهنگ • ${faTotal(tracks.sumOf { it.durationMs })}",
        "favorites", tracks, CoverStyle.Heart,
        emptyTitle = "هنوز قلبی نزدی",
        emptyText = "روی قلب کنار هر آهنگ بزن تا اینجا جمع شوند."
    )
}

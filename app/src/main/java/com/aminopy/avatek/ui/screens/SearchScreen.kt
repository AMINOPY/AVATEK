@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.aminopy.avatek.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aminopy.avatek.state.AvatekViewModel
import com.aminopy.avatek.ui.AppUi
import com.aminopy.avatek.ui.LocalBottomInset
import com.aminopy.avatek.ui.components.CoverCard
import com.aminopy.avatek.ui.components.EmptyState
import com.aminopy.avatek.ui.components.GlassTextField
import com.aminopy.avatek.ui.components.ScreenHeader
import com.aminopy.avatek.ui.components.TrackRow
import com.aminopy.avatek.ui.fa
import com.aminopy.avatek.ui.nav.Route
import com.aminopy.avatek.ui.theme.AccentButton
import com.aminopy.avatek.ui.theme.GlassChip
import com.aminopy.avatek.ui.theme.LocalPalette
import com.aminopy.avatek.ui.theme.SectionTitle
import com.aminopy.avatek.ui.theme.staggerIn

@Composable
fun SearchScreen(vm: AvatekViewModel, ui: AppUi) {
    val p = LocalPalette.current
    val bottom = LocalBottomInset.current
    val focus = LocalFocusManager.current
    val q = vm.searchQuery.trim()
    val tracks = remember(q, vm.tracks) { vm.search(q) }
    val albums = remember(q, vm.albums) { if (q.isEmpty()) emptyList() else vm.albums.filter { it.title.contains(q, true) || it.artist.contains(q, true) } }
    val artists = remember(q, vm.artists) { if (q.isEmpty()) emptyList() else vm.artists.filter { it.name.contains(q, true) } }
    DisposableEffect(Unit) { onDispose { vm.commitSearch(vm.searchQuery) } }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("جست‌وجو", subtitle = "در ${vm.tracks.size.fa()} آهنگ دستگاه")
        GlassTextField(
            vm.searchQuery, { vm.searchQuery = it }, "آهنگ، هنرمند یا آلبوم…",
            Modifier.padding(horizontal = 20.dp), leading = Icons.Rounded.Search,
            onImeAction = { vm.commitSearch(vm.searchQuery); focus.clearFocus() }
        )
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 16.dp, bottom = bottom),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (q.isEmpty()) {
                if (vm.recentSearches.isNotEmpty()) {
                    item { SectionTitle("جست‌وجوهای اخیر", action = "پاک‌کردن", onAction = { vm.clearSearches() }) }
                    item {
                        FlowRow(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            vm.recentSearches.forEach { s -> GlassChip(s, false, { vm.searchQuery = s }) }
                        }
                    }
                }
                if (vm.artists.isNotEmpty()) {
                    item { SectionTitle("هنرمندان پیشنهادی") }
                    item {
                        FlowRow(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            vm.artists.sortedByDescending { it.trackIds.size }.take(10).forEach { a -> GlassChip(a.name, false, { vm.searchQuery = a.name }) }
                        }
                    }
                }
                if (vm.tracks.isNotEmpty()) {
                    item {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 14.dp)) {
                            AccentButton("یک آهنگ شانسی!", { vm.playList(vm.tracks, shuffled = true); ui.openPlayer() }, icon = Icons.Rounded.Casino)
                        }
                    }
                }
            } else if (tracks.isEmpty() && albums.isEmpty() && artists.isEmpty()) {
                item { EmptyState(Icons.Rounded.SearchOff, "نتیجه‌ای پیدا نشد", "چیز دیگری امتحان کن؛ املای هنرمند یا نام آلبوم را هم می‌شود جست‌وجو کرد.") }
            } else {
                if (artists.isNotEmpty()) {
                    item { SectionTitle("هنرمندان") }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(artists, key = { it.name }) { a -> GlassChip("${a.name} (${a.trackIds.size.fa()})", false, { ui.nav.push(Route.Artist(a.name)) }) }
                        }
                    }
                }
                if (albums.isNotEmpty()) {
                    item { SectionTitle("آلبوم‌ها") }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            items(albums, key = { it.key }) { a ->
                                val f = vm.trackById(a.trackIds.first())
                                CoverCard(a.title, a.artist, a.key, f?.id ?: 0L, f?.albumId ?: 0L, { ui.nav.push(Route.Album(a.key)) }, size = 120.dp)
                            }
                        }
                    }
                }
                if (tracks.isNotEmpty()) {
                    item { SectionTitle("آهنگ‌ها", subtitle = "${tracks.size.fa()} نتیجه") }
                    items(tracks.size) { i -> TrackRow(tracks[i], tracks, Modifier.padding(horizontal = 20.dp).staggerIn(i)) }
                }
            }
        }
    }
}

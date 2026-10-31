package com.aminopy.avatek.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aminopy.avatek.data.AudioTrack
import com.aminopy.avatek.ui.LocalAppUi
import com.aminopy.avatek.ui.LocalVm
import com.aminopy.avatek.ui.Sheet
import com.aminopy.avatek.ui.fa
import com.aminopy.avatek.ui.faDuration
import com.aminopy.avatek.ui.theme.Artwork
import com.aminopy.avatek.ui.theme.LocalPalette
import com.aminopy.avatek.ui.theme.bounceClick
import com.aminopy.avatek.ui.theme.glass

/**
 * One glass row of the track list. [queue] is the list the track belongs to – tapping plays that list
 * starting from this track. [playlistId] adds a "remove from playlist" action to the menu.
 */
@Composable
fun TrackRow(
    track: AudioTrack,
    queue: List<AudioTrack>,
    modifier: Modifier = Modifier,
    playlistId: String? = null,
    rank: Int? = null
) {
    val vm = LocalVm.current
    val ui = LocalAppUi.current
    val p = LocalPalette.current
    val isCurrent = vm.currentId == track.id
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier
            .fillMaxWidth()
            .bounceClick(pressedScale = 0.975f) { vm.playTrackIn(queue, track) }
            .glass(shape, strength = if (isCurrent) 0.95f else 0.35f, tint = if (isCurrent) p.accent else Color.White)
            .padding(start = 10.dp, end = 4.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (rank != null) {
            Text(
                rank.fa(), color = if (rank <= 3) p.accent else p.muted, fontSize = 15.sp, fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center, modifier = Modifier.width(30.dp)
            )
        }
        Box(Modifier.size(52.dp)) {
            Artwork(track.albumKey, track.id, track.albumId, Modifier.fillMaxSize(), RoundedCornerShape(16.dp))
            if (isCurrent) {
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) { PlayingBars(playing = vm.isPlaying) }
            }
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                track.title, color = if (isCurrent) p.accent else p.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                "${track.artist}  •  ${faDuration(track.durationMs)}", color = p.muted, fontSize = 11.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        FavoriteButton(active = track.id in vm.favorites, onToggle = { vm.toggleFavorite(track.id) }, size = 40.dp)
        Box(
            Modifier.size(40.dp).bounceClick { ui.show(Sheet.TrackMenu(track, playlistId)) },
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Rounded.MoreVert, "گزینه‌ها", tint = p.muted, modifier = Modifier.size(22.dp)) }
    }
}

package com.aminopy.avatek.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aminopy.avatek.data.AudioTrack
import com.aminopy.avatek.state.AvatekViewModel
import com.aminopy.avatek.ui.nav.Navigator

sealed interface Sheet {
    data object Queue : Sheet
    data object Sleep : Sheet
    data object Speed : Sheet
    data class TrackMenu(val track: AudioTrack, val playlistId: String? = null) : Sheet
    data class AddToPlaylist(val trackId: Long) : Sheet
    data class NewPlaylist(val firstTrackId: Long? = null) : Sheet
    data class RenamePlaylist(val id: String, val name: String) : Sheet
    data class DeletePlaylist(val id: String, val name: String) : Sheet
}

/** UI-level state shared across screens: navigation, full-screen player and overlay sheets. */
class AppUi(val nav: Navigator) {
    var playerOpen by mutableStateOf(false); private set
    var sheet by mutableStateOf<Sheet?>(null); private set

    fun openPlayer() { playerOpen = true }
    fun closePlayer() { playerOpen = false }
    fun show(s: Sheet) { sheet = s }
    fun dismissSheet() { sheet = null }
}

val LocalAppUi = staticCompositionLocalOf<AppUi> { error("AppUi not provided") }
val LocalVm = staticCompositionLocalOf<AvatekViewModel> { error("ViewModel not provided") }

/** Bottom space screens must reserve for the floating navigation bar and mini player. */
val LocalBottomInset = staticCompositionLocalOf<Dp> { 160.dp }

/** Asks the activity to request the audio permission (or rescan when it is already granted). */
val LocalPermissionRequester = staticCompositionLocalOf<() -> Unit> { {} }

package com.aminopy.avatek

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.aminopy.avatek.data.AudioLibraryRepository
import com.aminopy.avatek.data.AudioTrack
import com.aminopy.avatek.data.FavoriteStore
import com.aminopy.avatek.data.PlaylistStore
import com.aminopy.avatek.data.AudioPlaylist
import com.aminopy.avatek.playback.MusicPlaybackService
import com.aminopy.avatek.playback.toMediaItem
import com.aminopy.avatek.ui.theme.AvatekTheme
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private val audioExecutor = Executors.newSingleThreadExecutor()
    private val tracks = mutableStateListOf<AudioTrack>()
    private val currentTitle = mutableStateOf("هیچ آهنگی در حال پخش نیست")
    private val playing = mutableStateOf(false)
    private val libraryMessage = mutableStateOf("برای دیدن آهنگ‌های دستگاه، دسترسی کتابخانه را فعال کن.")
    private val searchQuery = mutableStateOf("")
    private val favoriteIds = mutableStateListOf<Long>()
    private val showFavorites = mutableStateOf(false)
    private val positionMs = mutableStateOf(0L)
    private val durationMs = mutableStateOf(0L)
    private val repeatMode = mutableStateOf(Player.REPEAT_MODE_OFF)
    private val shuffleEnabled = mutableStateOf(false)
    private val playlists = mutableStateListOf<AudioPlaylist>()
    private val showPlaylists = mutableStateOf(false)
    private val showAbout = mutableStateOf(false)
    private val progressHandler = Handler(Looper.getMainLooper())
    private val progressUpdater = object : Runnable {
        override fun run() {
            controller?.let { player ->
                positionMs.value = player.currentPosition.coerceAtLeast(0L)
                durationMs.value = player.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: 0L
            }
            progressHandler.postDelayed(this, 500L)
        }
    }
    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) refreshLibrary() else {
            libraryMessage.value = "بدون مجوز صوتی، امکان نمایش آهنگ‌های دستگاه وجود ندارد."
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            currentTitle.value = mediaItem?.mediaMetadata?.title?.toString()
                ?: "هیچ آهنگی در حال پخش نیست"
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            playing.value = isPlaying
        }

        override fun onRepeatModeChanged(mode: Int) {
            repeatMode.value = mode
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            shuffleEnabled.value = shuffleModeEnabled
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AvatekTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = AvatekColors.Background) {
                    AvatekHome(
                        tracks = tracks,
                        currentTitle = currentTitle.value,
                        isPlaying = playing.value,
                        libraryMessage = libraryMessage.value,
                        searchQuery = searchQuery.value,
                        onSearchChange = { searchQuery.value = it },
                        favoriteIds = favoriteIds.toSet(),
                        showFavorites = showFavorites.value,
                        onShowFavoritesChange = { showFavorites.value = it },
                        onToggleFavorite = { toggleFavorite(it) },
                        onScan = { requestAudioPermission() },
                        onPlay = { index -> playTrack(index) },
                        onTogglePlayback = { togglePlayback() },
                        onPrevious = { skipPrevious() },
                        onNext = { skipNext() },
                        positionMs = positionMs.value,
                        durationMs = durationMs.value,
                        onSeek = { seekTo(it) },
                        repeatMode = repeatMode.value,
                        shuffleEnabled = shuffleEnabled.value,
                        onToggleRepeat = { toggleRepeat() },
                        onToggleShuffle = { toggleShuffle() },
                        playlists = playlists,
                        showPlaylists = showPlaylists.value,
                        onOpenPlaylists = { showPlaylists.value = true },
                        onClosePlaylists = { showPlaylists.value = false },
                        onCreatePlaylist = { name -> createPlaylist(name) },
                        onDeletePlaylist = { id -> deletePlaylist(id) },
                        showAbout = showAbout.value,
                        onOpenAbout = { showAbout.value = true },
                        onCloseAbout = { showAbout.value = false },
                        onOpenSettings = { showAbout.value = true }
                    )
                }
            }
        }
        favoriteIds.addAll(FavoriteStore(this).loadFavoriteIds())
        playlists.addAll(PlaylistStore(this).loadPlaylists())
        if (hasAudioPermission()) refreshLibrary()
    }

    override fun onStart() {
        super.onStart()
        connectPlaybackController()
        progressHandler.post(progressUpdater)
    }

    override fun onStop() {
        progressHandler.removeCallbacks(progressUpdater)
        controller?.removeListener(playerListener)
        controller = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        super.onStop()
    }

    override fun onDestroy() {
        audioExecutor.shutdown()
        super.onDestroy()
    }

    private fun requiredAudioPermission(): String =
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE

    private fun hasAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, requiredAudioPermission()) == PackageManager.PERMISSION_GRANTED

    private fun requestAudioPermission() {
        if (hasAudioPermission()) refreshLibrary()
        else permissionLauncher.launch(requiredAudioPermission())
    }

    private fun toggleFavorite(trackId: Long) {
        if (trackId in favoriteIds) favoriteIds.remove(trackId) else favoriteIds.add(trackId)
        FavoriteStore(this).saveFavoriteIds(favoriteIds.toSet())
    }

    private fun createPlaylist(name: String) {
        val created = PlaylistStore(this).createPlaylist(name) ?: return
        playlists.add(created)
    }

    private fun deletePlaylist(id: String) {
        if (PlaylistStore(this).deletePlaylist(id)) playlists.removeAll { it.id == id }
    }

    private fun refreshLibrary() {
        if (!hasAudioPermission()) return
        libraryMessage.value = "در حال اسکن موسیقی‌های دستگاه…"
        audioExecutor.execute {
            val result = AudioLibraryRepository(contentResolver).loadTracks()
            runOnUiThread {
                tracks.clear()
                tracks.addAll(result)
                libraryMessage.value = if (result.isEmpty()) {
                    "آهنگی پیدا نشد. فایل‌های صوتی را روی دستگاه قرار بده و دوباره اسکن کن."
                } else {
                    "${result.size} آهنگ از حافظهٔ دستگاه پیدا شد."
                }
            }
        }
    }

    private fun connectPlaybackController() {
        if (controller != null || controllerFuture != null) return
        val token = SessionToken(this, ComponentName(this, MusicPlaybackService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        controllerFuture = future
        future.addListener({
            runCatching { future.get() }.onSuccess { mediaController ->
                if (controllerFuture !== future) {
                    MediaController.releaseFuture(future)
                    return@onSuccess
                }
                controller = mediaController
                mediaController.addListener(playerListener)
                currentTitle.value = mediaController.currentMediaItem?.mediaMetadata?.title?.toString()
                    ?: "هیچ آهنگی در حال پخش نیست"
                playing.value = mediaController.isPlaying
                repeatMode.value = mediaController.repeatMode
                shuffleEnabled.value = mediaController.shuffleModeEnabled
            }.onFailure {
                libraryMessage.value = "اتصال موتور پخش برقرار نشد. برنامه را دوباره باز کن."
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun playTrack(index: Int) {
        if (tracks.isEmpty() || index !in tracks.indices) return
        val mediaController = controller
        if (mediaController == null) {
            libraryMessage.value = "موتور پخش در حال آماده‌شدن است؛ چند لحظه دیگر دوباره بزن."
            return
        }
        mediaController.setMediaItems(tracks.map { it.toMediaItem() }, index, 0L)
        mediaController.prepare()
        mediaController.play()
    }

    private fun seekTo(position: Long) {
        controller?.let { player -> if (player.duration > 0L) player.seekTo(position.coerceIn(0L, player.duration)) }
    }

    private fun skipPrevious() {
        controller?.let { player ->
            if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem()
            else if (player.mediaItemCount > 0) player.seekTo(0, 0L)
        }
    }

    private fun skipNext() {
        controller?.let { player -> if (player.hasNextMediaItem()) player.seekToNextMediaItem() }
    }

    private fun toggleRepeat() {
        controller?.let { player ->
            player.repeatMode = when (player.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    private fun toggleShuffle() {
        controller?.let { player -> player.shuffleModeEnabled = !player.shuffleModeEnabled }
    }

    private fun togglePlayback() {
        val mediaController = controller ?: return
        if (mediaController.isPlaying) mediaController.pause()
        else if (mediaController.mediaItemCount > 0) mediaController.play()
        else if (tracks.isNotEmpty()) playTrack(0)
    }
}

private object AvatekColors {
    val Background = Color(0xFF0A0B0D)
    val Surface = Color(0xFF17191E)
    val Gold = Color(0xFFD9B56D)
    val BloodRed = Color(0xFF9E2638)
    val Muted = Color(0xFF9A9DA5)
}

@Composable
private fun AvatekHome(
    tracks: List<AudioTrack>,
    currentTitle: String,
    isPlaying: Boolean,
    libraryMessage: String,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    favoriteIds: Set<Long>,
    showFavorites: Boolean,
    onShowFavoritesChange: (Boolean) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onScan: () -> Unit,
    onPlay: (Int) -> Unit,
    onTogglePlayback: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    repeatMode: Int,
    shuffleEnabled: Boolean,
    onToggleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    playlists: List<AudioPlaylist>,
    showPlaylists: Boolean,
    onOpenPlaylists: () -> Unit,
    onClosePlaylists: () -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    showAbout: Boolean,
    onOpenAbout: () -> Unit,
    onCloseAbout: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF17130F), AvatekColors.Background, AvatekColors.Background)
                )
            )
            .padding(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        var menuExpanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("AVATEK", color = AvatekColors.Gold, fontSize = 28.sp,
                    fontWeight = FontWeight.Black, letterSpacing = 4.sp)
                Text("YOUR MUSIC. YOUR WORLD.", color = AvatekColors.Muted,
                    fontSize = 10.sp, letterSpacing = 2.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "منوی اصلی", tint = AvatekColors.Gold)
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(text = { Text("پلی‌لیست‌های من") }, leadingIcon = { Icon(Icons.Rounded.QueueMusic, null) }, onClick = { menuExpanded = false; onOpenPlaylists() })
                        DropdownMenuItem(text = { Text("همهٔ آهنگ‌ها") }, leadingIcon = { Icon(Icons.Rounded.LibraryMusic, null) }, onClick = { menuExpanded = false; onShowFavoritesChange(false) })
                        DropdownMenuItem(text = { Text("علاقه‌مندی‌ها") }, leadingIcon = { Icon(Icons.Rounded.Favorite, null) }, onClick = { menuExpanded = false; onShowFavoritesChange(true) })
                        DropdownMenuItem(text = { Text("اسکن دوبارهٔ کتابخانه") }, leadingIcon = { Icon(Icons.Rounded.Refresh, null) }, onClick = { menuExpanded = false; onScan() })
                        DropdownMenuItem(text = { Text("تنظیمات") }, leadingIcon = { Icon(Icons.Rounded.Settings, null) }, onClick = { menuExpanded = false; onOpenSettings() })
                        DropdownMenuItem(text = { Text("دربارهٔ AVATEK") }, leadingIcon = { Icon(Icons.Rounded.Info, null) }, onClick = { menuExpanded = false; onOpenAbout() })
                    }
                }
                Box(modifier = Modifier.background(AvatekColors.Surface, RoundedCornerShape(18.dp)).padding(13.dp)) {
                    Icon(Icons.Rounded.Headphones, contentDescription = "Offline music", tint = AvatekColors.Gold)
                }
            }
        }
        if (showPlaylists) PlaylistDialog(
            playlists = playlists,
            onDismiss = onClosePlaylists,
            onCreate = onCreatePlaylist,
            onDelete = onDeletePlaylist
        )
        if (showAbout) AlertDialog(
            onDismissRequest = onCloseAbout,
            title = { Text("AVATEK") },
            text = { Text("پلیر موسیقی آفلاین؛ کتابخانه، علاقه‌مندی‌ها و پلی‌لیست‌ها روی دستگاه شما نگهداری می‌شوند.\n\nنسخهٔ پایهٔ تنظیمات؛ گزینه‌های پیشرفته پس از پیاده‌سازی واقعی اضافه می‌شوند.") },
            confirmButton = { TextButton(onClick = onCloseAbout) { Text("بستن") } }
        )

        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("صدای خودت را پیدا کن", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text("موسیقی‌هایت، بدون اینترنت؛ فقط تو و صدا.", color = AvatekColors.Muted, fontSize = 13.sp)
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = AvatekColors.Surface
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.LibraryMusic, contentDescription = null, tint = AvatekColors.Gold)
                    Text("  کتابخانه موسیقی", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onScan) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "اسکن دوباره", tint = AvatekColors.Gold)
                    }
                }
                Text(libraryMessage, color = AvatekColors.Muted, fontSize = 12.sp, lineHeight = 18.sp)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onToggleShuffle) {
                            Icon(Icons.Rounded.Shuffle, contentDescription = "پخش تصادفی",
                                tint = if (shuffleEnabled) AvatekColors.Gold else AvatekColors.Muted)
                        }
                        IconButton(onClick = onPrevious) {
                            Icon(Icons.Rounded.SkipPrevious, contentDescription = "آهنگ قبلی", tint = AvatekColors.Gold)
                        }
                        IconButton(onClick = onTogglePlayback) {
                            Icon(
                                if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "توقف موقت" else "پخش",
                                tint = AvatekColors.Gold
                            )
                        }
                        IconButton(onClick = onNext) {
                            Icon(Icons.Rounded.SkipNext, contentDescription = "آهنگ بعدی", tint = AvatekColors.Gold)
                        }
                        IconButton(onClick = onToggleRepeat) {
                            Icon(Icons.Rounded.Repeat, contentDescription = "حالت تکرار",
                                tint = if (repeatMode != Player.REPEAT_MODE_OFF) AvatekColors.Gold else AvatekColors.Muted)
                        }
                    }
                    Slider(
                        value = positionMs.toFloat().coerceIn(0f, durationMs.coerceAtLeast(1L).toFloat()),
                        onValueChange = { onSeek(it.toLong()) },
                        valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat(),
                        enabled = durationMs > 0L,
                        colors = SliderDefaults.colors(
                            thumbColor = AvatekColors.Gold,
                            activeTrackColor = AvatekColors.Gold,
                            inactiveTrackColor = AvatekColors.Muted
                        )
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatDuration(positionMs), color = AvatekColors.Muted, fontSize = 10.sp)
                        Text(formatDuration(durationMs), color = AvatekColors.Muted, fontSize = 10.sp)
                    }
                    Text(
                        when (repeatMode) {
                            Player.REPEAT_MODE_ALL -> "تکرار صف"
                            Player.REPEAT_MODE_ONE -> "تکرار همین آهنگ"
                            else -> if (shuffleEnabled) "پخش تصادفی روشن" else "پخش عادی"
                        },
                        color = AvatekColors.Muted,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FeatureTile(Modifier.weight(1f), { Icon(Icons.Rounded.Album, null, tint = AvatekColors.Gold) }, "آلبوم‌ها", "ALBUMS")
            FeatureTile(Modifier.weight(1f), { Icon(Icons.Rounded.GraphicEq, null, tint = AvatekColors.Gold) }, "آزمایشگاه صدا", "SOUND LAB")
        }

        Text("آهنگ‌های دستگاه", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            placeholder = { Text("جست‌وجوی آهنگ، هنرمند یا آلبوم", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = AvatekColors.Gold) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = "پاک‌کردن جست‌وجو", tint = AvatekColors.Muted)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = AvatekColors.Gold,
                unfocusedBorderColor = AvatekColors.Surface,
                focusedContainerColor = AvatekColors.Surface,
                unfocusedContainerColor = AvatekColors.Surface,
                cursorColor = AvatekColors.Gold
            )
        )
        val normalizedQuery = searchQuery.trim()
        val filteredByFavorite = if (showFavorites) tracks.filter { it.id in favoriteIds } else tracks
        val visibleTracks = if (normalizedQuery.isBlank()) filteredByFavorite else filteredByFavorite.filter { track ->
            track.title.contains(normalizedQuery, ignoreCase = true) ||
                track.artist.contains(normalizedQuery, ignoreCase = true) ||
                track.album.contains(normalizedQuery, ignoreCase = true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !showFavorites,
                onClick = { onShowFavoritesChange(false) },
                label = { Text("همهٔ آهنگ‌ها") }
            )
            FilterChip(
                selected = showFavorites,
                onClick = { onShowFavoritesChange(true) },
                label = { Text("علاقه‌مندی‌ها") },
                leadingIcon = { Icon(Icons.Rounded.Favorite, contentDescription = null, tint = AvatekColors.Gold) }
            )
        }
        if (tracks.isEmpty()) {
            Button(
                onClick = onScan,
                colors = ButtonDefaults.buttonColors(containerColor = AvatekColors.BloodRed),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("اجازه دسترسی و اسکن آهنگ‌ها", color = Color.White)
            }
        } else if (visibleTracks.isEmpty()) {
            Text(
                "نتیجه‌ای برای " + normalizedQuery + " پیدا نشد.",
                color = AvatekColors.Muted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f, fill = true),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                itemsIndexed(visibleTracks, key = { _, track -> track.id }) { _, track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AvatekColors.Surface, RoundedCornerShape(15.dp))
                            .clickable { onPlay(tracks.indexOfFirst { it.id == track.id }) }
                            .padding(horizontal = 13.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.LibraryMusic, contentDescription = null, tint = AvatekColors.Gold)
                        Column(modifier = Modifier.weight(1f).padding(start = 11.dp)) {
                            Text(track.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                track.artist + "  •  " + track.album + "  •  " + formatDuration(track.durationMs),
                                color = AvatekColors.Muted,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { onToggleFavorite(track.id) }) {
                            Icon(
                                if (track.id in favoriteIds) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = if (track.id in favoriteIds) "حذف از علاقه‌مندی‌ها" else "افزودن به علاقه‌مندی‌ها",
                                tint = AvatekColors.Gold
                            )
                        }
                        IconButton(onClick = { onPlay(tracks.indexOfFirst { it.id == track.id }) }) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "پخش " + track.title, tint = AvatekColors.Gold)
                        }
                    }
                }
            }
        }

        Text(
            "OFFLINE FIRST  •  DESIGNED FOR ANDROID",
            color = AvatekColors.BloodRed,
            fontSize = 9.sp,
            letterSpacing = 1.3.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}


@Composable
private fun FeatureTile(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String
) {
    Surface(modifier = modifier, shape = RoundedCornerShape(20.dp), color = AvatekColors.Surface) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            icon()
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(subtitle, color = AvatekColors.Muted, fontSize = 9.sp, letterSpacing = 1.sp)
        }
    }
}


@Composable
private fun PlaylistDialog(
    playlists: List<AudioPlaylist>,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    var name by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("پلی‌لیست‌های من") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("نام پلی‌لیست جدید") }
                )
                Button(
                    onClick = {
                        val clean = name.trim()
                        if (clean.isNotEmpty()) {
                            onCreate(clean)
                            name = ""
                        }
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AvatekColors.BloodRed)
                ) { Text("ساخت پلی‌لیست", color = Color.White) }
                if (playlists.isEmpty()) {
                    Text("هنوز پلی‌لیستی نساخته‌ای.", color = AvatekColors.Muted)
                } else {
                    LazyColumn(modifier = Modifier.height(220.dp)) {
                        itemsIndexed(playlists, key = { _, playlist -> playlist.id }) { _, playlist ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.QueueMusic, contentDescription = null, tint = AvatekColors.Gold)
                                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                    Text(playlist.name, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${playlist.trackIds.size} آهنگ", color = AvatekColors.Muted, fontSize = 11.sp)
                                }
                                TextButton(onClick = { onDelete(playlist.id) }) { Text("حذف", color = AvatekColors.BloodRed) }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("تمام") } }
    )
}

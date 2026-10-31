package com.aminopy.avatek.state

import android.app.Application
import android.content.ComponentName
import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.aminopy.avatek.data.AlbumInfo
import com.aminopy.avatek.data.ArtistInfo
import com.aminopy.avatek.data.AudioLibraryRepository
import com.aminopy.avatek.data.AudioPlaylist
import com.aminopy.avatek.data.AudioTrack
import com.aminopy.avatek.data.FavoriteStore
import com.aminopy.avatek.data.HistoryStore
import com.aminopy.avatek.data.PlaylistStore
import com.aminopy.avatek.data.SettingsStore
import com.aminopy.avatek.data.UiSettings
import com.aminopy.avatek.playback.AvatekCommands
import com.aminopy.avatek.playback.EqualizerInfo
import com.aminopy.avatek.playback.MusicPlaybackService
import com.aminopy.avatek.playback.toMediaItem
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

enum class LibraryStatus { NeedsPermission, Scanning, Ready, Empty }
enum class SortMode(val label: String) { Title("نام"), Artist("هنرمند"), Recent("جدیدترین"), Duration("مدت"), MostPlayed("پرشنیده") }

class AvatekViewModel(app: Application) : AndroidViewModel(app) {
    private val favoriteStore = FavoriteStore(app)
    private val playlistStore = PlaylistStore(app)
    private val historyStore = HistoryStore(app)
    private val settingsStore = SettingsStore(app)

    // ---- library -------------------------------------------------------------------------------
    var status by mutableStateOf(LibraryStatus.NeedsPermission); private set
    var tracks by mutableStateOf<List<AudioTrack>>(emptyList()); private set
    var albums by mutableStateOf<List<AlbumInfo>>(emptyList()); private set
    var artists by mutableStateOf<List<ArtistInfo>>(emptyList()); private set
    private var byId: Map<Long, AudioTrack> = emptyMap()

    var favorites by mutableStateOf(favoriteStore.loadFavoriteIds()); private set
    var playlists by mutableStateOf(playlistStore.loadPlaylists()); private set
    var playCounts by mutableStateOf(historyStore.loadPlayCounts()); private set
    var recentIds by mutableStateOf(historyStore.loadRecent()); private set
    var recentSearches by mutableStateOf(historyStore.loadSearches()); private set

    var settings by mutableStateOf(settingsStore.load()); private set
    var searchQuery by mutableStateOf("")
    var librarySort by mutableStateOf(SortMode.Title)
    var libraryTab by mutableStateOf(0)

    // ---- playback ------------------------------------------------------------------------------
    var currentId by mutableStateOf<Long?>(null); private set
    var isPlaying by mutableStateOf(false); private set
    var positionMs by mutableStateOf(0L); private set
    var durationMs by mutableStateOf(0L); private set
    var repeatMode by mutableStateOf(Player.REPEAT_MODE_OFF); private set
    var shuffle by mutableStateOf(false); private set
    var speed by mutableStateOf(1f); private set
    var queueIds by mutableStateOf<List<Long>>(emptyList()); private set
    var queueIndex by mutableStateOf(0); private set
    var engineReady by mutableStateOf(false); private set

    // ---- extras --------------------------------------------------------------------------------
    var sleepRemainingMs by mutableStateOf<Long?>(null); private set
    var eq by mutableStateOf(EqualizerInfo()); private set
    var toast by mutableStateOf<String?>(null); private set
    private var toastJob: Job? = null
    private var sleepJob: Job? = null

    private var controller: MediaController? = null
    private var future: ListenableFuture<MediaController>? = null

    val currentTrack: AudioTrack? get() = currentId?.let { byId[it] }
    fun trackById(id: Long): AudioTrack? = byId[id]
    fun tracksOf(ids: List<Long>): List<AudioTrack> = ids.mapNotNull { byId[it] }

    private val listener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            currentId = mediaItem?.mediaId?.toLongOrNull()
            syncQueue()
            if (mediaItem != null && reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) {
                currentId?.let { recordPlay(it) }
            }
        }
        override fun onIsPlayingChanged(isPlaying: Boolean) { this@AvatekViewModel.isPlaying = isPlaying }
        override fun onRepeatModeChanged(repeatMode: Int) { this@AvatekViewModel.repeatMode = repeatMode }
        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) { shuffle = shuffleModeEnabled }
        override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) = syncQueue()
        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) { speed = playbackParameters.speed }
        override fun onPlaybackStateChanged(playbackState: Int) = syncProgress()
    }

    init {
        connect()
        viewModelScope.launch {
            while (isActive) {
                syncProgress()
                delay(if (isPlaying) 120L else 700L)
            }
        }
    }

    // ---- connection ----------------------------------------------------------------------------
    private fun connect() {
        val app = getApplication<Application>()
        val token = SessionToken(app, ComponentName(app, MusicPlaybackService::class.java))
        val f = MediaController.Builder(app, token).buildAsync()
        future = f
        f.addListener({
            runCatching { f.get() }.onSuccess { c ->
                controller = c
                c.addListener(listener)
                currentId = c.currentMediaItem?.mediaId?.toLongOrNull()
                isPlaying = c.isPlaying
                repeatMode = c.repeatMode
                shuffle = c.shuffleModeEnabled
                speed = c.playbackParameters.speed
                engineReady = true
                syncQueue()
                refreshEqualizer()
            }.onFailure { showToast("اتصال موتور پخش برقرار نشد") }
        }, ContextCompat.getMainExecutor(app))
    }

    override fun onCleared() {
        controller?.removeListener(listener)
        future?.let { MediaController.releaseFuture(it) }
        controller = null
        super.onCleared()
    }

    private fun syncProgress() {
        val c = controller ?: return
        positionMs = c.currentPosition.coerceAtLeast(0L)
        durationMs = c.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: (currentTrack?.durationMs ?: 0L)
    }

    private fun syncQueue() {
        val c = controller ?: return
        queueIds = (0 until c.mediaItemCount).mapNotNull { c.getMediaItemAt(it).mediaId.toLongOrNull() }
        queueIndex = c.currentMediaItemIndex.coerceAtLeast(0)
    }

    // ---- library -------------------------------------------------------------------------------
    fun onPermissionResult(granted: Boolean) {
        if (granted) scan() else status = LibraryStatus.NeedsPermission
    }

    fun scan() {
        status = LibraryStatus.Scanning
        val resolver = getApplication<Application>().contentResolver
        val minMs = settings.minDurationSec * 1000L
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { AudioLibraryRepository(resolver).loadTracks(minMs) }
            applyTracks(result)
            status = if (result.isEmpty()) LibraryStatus.Empty else LibraryStatus.Ready
        }
    }

    private fun applyTracks(list: List<AudioTrack>) {
        tracks = list
        byId = list.associateBy { it.id }
        albums = list.groupBy { it.albumKey }.map { (key, items) ->
            AlbumInfo(key, items.first().album, items.first().artist, items.map { it.id }, items.sumOf { it.durationMs })
        }.sortedBy { it.title.lowercase() }
        artists = list.groupBy { it.artist }.map { (name, items) ->
            ArtistInfo(name, items.map { it.id }, items.map { it.album }.distinct().size)
        }.sortedBy { it.name.lowercase() }
        // drop ids that no longer exist
        recentIds = recentIds.filter { it in byId }
    }

    fun sorted(list: List<AudioTrack>, mode: SortMode = librarySort): List<AudioTrack> = when (mode) {
        SortMode.Title -> list.sortedBy { it.title.lowercase() }
        SortMode.Artist -> list.sortedWith(compareBy({ it.artist.lowercase() }, { it.title.lowercase() }))
        SortMode.Recent -> list.sortedByDescending { it.dateAddedSec }
        SortMode.Duration -> list.sortedByDescending { it.durationMs }
        SortMode.MostPlayed -> list.sortedByDescending { playCounts[it.id] ?: 0 }
    }

    fun search(query: String): List<AudioTrack> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        return tracks.filter {
            it.title.contains(q, true) || it.artist.contains(q, true) || it.album.contains(q, true)
        }
    }

    fun commitSearch(query: String) {
        val q = query.trim()
        if (q.length < 2) return
        recentSearches = (listOf(q) + recentSearches.filterNot { it.equals(q, true) }).take(HistoryStore.MAX_SEARCHES)
        historyStore.saveSearches(recentSearches)
    }

    fun clearSearches() { recentSearches = emptyList(); historyStore.saveSearches(emptyList()) }

    fun mostPlayed(limit: Int = 12): List<AudioTrack> =
        playCounts.entries.sortedByDescending { it.value }.mapNotNull { byId[it.key] }.take(limit)

    fun recentlyPlayed(limit: Int = 12): List<AudioTrack> = recentIds.mapNotNull { byId[it] }.take(limit)
    fun recentlyAdded(limit: Int = 12): List<AudioTrack> = tracks.sortedByDescending { it.dateAddedSec }.take(limit)

    /**
     * Local-first daily mix. It favors liked and frequently played tracks while gently
     * resurfacing recent listening; a day-based seed keeps the order stable for the day.
     */
    fun dailyMix(limit: Int = 30): List<AudioTrack> {
        val daySeed = System.currentTimeMillis() / 86_400_000L
        val recentRank = recentIds.withIndex().associate { it.value to it.index }
        return tracks.sortedByDescending { track ->
            val favoriteBoost = if (track.id in favorites) 48.0 else 0.0
            val playBoost = ((playCounts[track.id] ?: 0).coerceAtMost(30)) * 2.2
            val recentBoost = recentRank[track.id]?.let { (24 - it).coerceAtLeast(0) * 0.8 } ?: 0.0
            val addedDays = if (track.dateAddedSec > 0L) daySeed - track.dateAddedSec / 86_400L else Long.MAX_VALUE
            val freshBoost = if (addedDays in 0..14) (14 - addedDays) * 0.7 else 0.0
            val dailyVariation = Random(daySeed + track.id).nextDouble() * 5.0
            favoriteBoost + playBoost + recentBoost + freshBoost + dailyVariation
        }.take(limit.coerceAtLeast(1))
    }

    /** Discovery mix deliberately prioritizes tracks that have rarely or never been played. */
    fun discoveryMix(limit: Int = 30): List<AudioTrack> {
        val daySeed = System.currentTimeMillis() / 86_400_000L
        return tracks.sortedWith(
            compareBy<AudioTrack> { playCounts[it.id] ?: 0 }
                .thenByDescending { it.dateAddedSec }
                .thenBy { Random(daySeed + it.id).nextInt() }
        ).take(limit.coerceAtLeast(1))
    }

    private fun recordPlay(id: Long) {
        playCounts = playCounts + (id to ((playCounts[id] ?: 0) + 1))
        recentIds = (listOf(id) + recentIds.filter { it != id }).take(HistoryStore.MAX_RECENT)
        viewModelScope.launch(Dispatchers.IO) {
            historyStore.savePlayCounts(playCounts)
            historyStore.saveRecent(recentIds)
        }
    }

    // ---- favourites & playlists ----------------------------------------------------------------
    fun toggleFavorite(id: Long) {
        favorites = if (id in favorites) favorites - id else favorites + id
        favoriteStore.saveFavoriteIds(favorites)
    }

    fun createPlaylist(name: String, firstTrack: Long? = null): Boolean {
        val created = playlistStore.createPlaylist(name)
        if (created == null) { showToast("این نام قبلاً استفاده شده یا خالی است"); return false }
        if (firstTrack != null) playlistStore.addTrack(created.id, firstTrack)
        playlists = playlistStore.loadPlaylists()
        showToast("پلی‌لیست «${created.name}» ساخته شد")
        return true
    }

    fun renamePlaylist(id: String, name: String) {
        if (playlistStore.renamePlaylist(id, name)) playlists = playlistStore.loadPlaylists()
        else showToast("نام معتبر نیست یا تکراری است")
    }

    fun deletePlaylist(id: String) {
        if (playlistStore.deletePlaylist(id)) { playlists = playlistStore.loadPlaylists(); showToast("پلی‌لیست حذف شد") }
    }

    fun addToPlaylist(playlistId: String, trackId: Long) {
        val name = playlists.firstOrNull { it.id == playlistId }?.name.orEmpty()
        if (playlistStore.addTrack(playlistId, trackId)) {
            playlists = playlistStore.loadPlaylists(); showToast("به «$name» اضافه شد")
        } else showToast("این آهنگ از قبل در «$name» است")
    }

    fun removeFromPlaylist(playlistId: String, trackId: Long) {
        if (playlistStore.removeTrack(playlistId, trackId)) playlists = playlistStore.loadPlaylists()
    }

    // ---- transport -----------------------------------------------------------------------------
    fun playList(list: List<AudioTrack>, startIndex: Int = 0, shuffled: Boolean = false) {
        val c = controller
        if (c == null) { showToast("موتور پخش در حال آماده‌شدن است"); return }
        if (list.isEmpty()) return
        val start = if (shuffled) Random.nextInt(list.size) else startIndex.coerceIn(0, list.lastIndex)
        c.shuffleModeEnabled = shuffled
        c.setMediaItems(list.map { it.toMediaItem() }, start, 0L)
        c.prepare()
        c.play()
    }

    fun playTrackIn(list: List<AudioTrack>, track: AudioTrack) {
        if (currentId == track.id && controller?.mediaItemCount ?: 0 > 0) { togglePlay(); return }
        playList(list, list.indexOfFirst { it.id == track.id }.coerceAtLeast(0))
    }

    fun togglePlay() {
        val c = controller ?: return
        when {
            c.isPlaying -> c.pause()
            c.mediaItemCount > 0 -> { if (c.playbackState == Player.STATE_ENDED) c.seekTo(0, 0L); c.play() }
            tracks.isNotEmpty() -> playList(sorted(tracks), 0)
        }
    }

    fun next() { controller?.let { if (it.hasNextMediaItem()) it.seekToNextMediaItem() } }
    fun previous() {
        controller?.let {
            if (it.currentPosition > 3000L) it.seekTo(0L)
            else if (it.hasPreviousMediaItem()) it.seekToPreviousMediaItem()
            else it.seekTo(0L)
        }
    }
    fun seekTo(ms: Long) { controller?.let { if (it.duration > 0L) it.seekTo(ms.coerceIn(0L, it.duration)) } }
    fun cycleRepeat() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }
    fun toggleShuffle() { controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }
    fun setPlaybackSpeed(value: Float) { controller?.setPlaybackParameters(PlaybackParameters(value.coerceIn(0.5f, 2f), 1f)) }

    // ---- queue ---------------------------------------------------------------------------------
    fun playNext(track: AudioTrack) {
        val c = controller ?: return
        if (c.mediaItemCount == 0) { playList(listOf(track)); return }
        c.addMediaItem(c.currentMediaItemIndex + 1, track.toMediaItem())
        showToast("بعدی در صف: ${track.title}")
    }

    fun addToQueue(track: AudioTrack) {
        val c = controller ?: return
        if (c.mediaItemCount == 0) { playList(listOf(track)); return }
        c.addMediaItem(track.toMediaItem())
        showToast("به انتهای صف اضافه شد")
    }

    fun addAllToQueue(list: List<AudioTrack>) {
        val c = controller ?: return
        if (c.mediaItemCount == 0) { playList(list); return }
        c.addMediaItems(list.map { it.toMediaItem() })
        showToast("${list.size} آهنگ به صف اضافه شد")
    }

    fun jumpTo(index: Int) { controller?.let { if (index in 0 until it.mediaItemCount) { it.seekTo(index, 0L); it.play() } } }
    fun removeFromQueue(index: Int) { controller?.let { if (index in 0 until it.mediaItemCount) it.removeMediaItem(index) } }
    fun moveInQueue(from: Int, to: Int) {
        controller?.let { if (from in 0 until it.mediaItemCount && to in 0 until it.mediaItemCount) it.moveMediaItem(from, to) }
    }
    fun clearQueue() { controller?.let { it.stop(); it.clearMediaItems() }; currentId = null; positionMs = 0L }

    // ---- sleep timer ---------------------------------------------------------------------------
    fun startSleepTimer(minutes: Int) = startSleep(minutes * 60_000L)

    fun sleepAtEndOfTrack() {
        val remaining = (durationMs - positionMs).coerceAtLeast(1000L)
        startSleep(remaining)
    }

    private fun startSleep(totalMs: Long) {
        sleepJob?.cancel()
        sleepRemainingMs = totalMs
        showToast("تایمر خواب فعال شد")
        sleepJob = viewModelScope.launch {
            var left = totalMs
            while (left > 0 && isActive) {
                delay(1000L)
                left -= 1000L
                sleepRemainingMs = left.coerceAtLeast(0L)
            }
            if (isActive) {
                controller?.pause()
                sleepRemainingMs = null
                showToast("تایمر خواب: پخش متوقف شد")
            }
        }
    }

    fun cancelSleep() { sleepJob?.cancel(); sleepRemainingMs = null }

    // ---- equalizer -----------------------------------------------------------------------------
    private fun sendEq(action: String, args: Bundle = Bundle.EMPTY) {
        val c = controller ?: return
        val f = c.sendCustomCommand(SessionCommand(action, Bundle.EMPTY), args)
        f.addListener({
            runCatching { f.get() }.onSuccess { r -> eq = parseEq(r.extras) }
        }, ContextCompat.getMainExecutor(getApplication()))
    }

    fun refreshEqualizer() = sendEq(AvatekCommands.EQ_INFO)
    fun setEqEnabled(enabled: Boolean) = sendEq(AvatekCommands.EQ_ENABLED, Bundle().apply { putBoolean(AvatekCommands.KEY_ENABLED, enabled) })
    fun setEqBand(band: Int, level: Int) = sendEq(AvatekCommands.EQ_BAND, Bundle().apply {
        putInt(AvatekCommands.KEY_BAND, band); putInt(AvatekCommands.KEY_LEVEL, level)
    })
    fun setBass(strength: Int) = sendEq(AvatekCommands.BASS, Bundle().apply { putInt(AvatekCommands.KEY_STRENGTH, strength) })
    fun setVirtualizer(strength: Int) = sendEq(AvatekCommands.VIRTUALIZER, Bundle().apply { putInt(AvatekCommands.KEY_STRENGTH, strength) })
    fun resetEqualizer() = sendEq(AvatekCommands.EQ_RESET)

    private fun parseEq(b: Bundle): EqualizerInfo = EqualizerInfo(
        available = b.getBoolean(AvatekCommands.KEY_AVAILABLE),
        enabled = b.getBoolean(AvatekCommands.KEY_ENABLED),
        minLevel = b.getInt(AvatekCommands.KEY_MIN, -1500),
        maxLevel = b.getInt(AvatekCommands.KEY_MAX, 1500),
        centerFreqsHz = b.getIntArray(AvatekCommands.KEY_CENTERS)?.toList().orEmpty(),
        levels = b.getIntArray(AvatekCommands.KEY_LEVELS)?.toList().orEmpty(),
        bass = b.getInt(AvatekCommands.KEY_BASS),
        virtualizer = b.getInt(AvatekCommands.KEY_VIRT)
    )

    // ---- settings & toast ----------------------------------------------------------------------
    fun updateSettings(transform: (UiSettings) -> UiSettings) {
        val old = settings
        settings = transform(old)
        settingsStore.save(settings)
        if (old.minDurationSec != settings.minDurationSec && status != LibraryStatus.NeedsPermission) scan()
    }

    fun showToast(message: String) {
        toast = message
        toastJob?.cancel()
        toastJob = viewModelScope.launch { delay(2300L); toast = null }
    }
}

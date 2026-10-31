package com.aminopy.avatek.state

import android.app.Application
import android.content.ComponentName
import android.net.Uri
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
import com.aminopy.avatek.data.LyricsStore
import com.aminopy.avatek.data.PlaylistStore
import com.aminopy.avatek.data.QueueSnapshot
import com.aminopy.avatek.data.QueueStore
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

data class LyricLine(val timeMs: Long, val text: String)

enum class LibraryStatus { NeedsPermission, Scanning, Ready, Empty }
enum class SortMode(val label: String) { Title("نام"), Artist("هنرمند"), Recent("جدیدترین"), Duration("مدت"), MostPlayed("پرشنیده") }

class AvatekViewModel(app: Application) : AndroidViewModel(app) {
    private val favoriteStore = FavoriteStore(app)
    private val playlistStore = PlaylistStore(app)
    private val historyStore = HistoryStore(app)
    private val settingsStore = SettingsStore(app)
    private val queueStore = QueueStore(app)
    private val lyricsStore = LyricsStore(app)

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
    var lyrics by mutableStateOf<List<LyricLine>>(emptyList()); private set
    var lyricsVisible by mutableStateOf(false); private set
    var eq by mutableStateOf(EqualizerInfo()); private set
    var toast by mutableStateOf<String?>(null); private set
    private var toastJob: Job? = null
    private var sleepJob: Job? = null

    private var controller: MediaController? = null
    private var future: ListenableFuture<MediaController>? = null
    private var tracksLoaded = false
    private var queueRestored = false

    val currentTrack: AudioTrack? get() = currentId?.let { byId[it] }
    fun trackById(id: Long): AudioTrack? = byId[id]
    fun tracksOf(ids: List<Long>): List<AudioTrack> = ids.mapNotNull { byId[it] }

    private val listener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            currentId = mediaItem?.mediaId?.toLongOrNull()
            loadLyrics(currentId)
            syncQueue()
            if (mediaItem != null && reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) {
                currentId?.let { recordPlay(it) }
            }
        }
        override fun onIsPlayingChanged(isPlaying: Boolean) { this@AvatekViewModel.isPlaying = isPlaying; persistQueue() }
        override fun onRepeatModeChanged(repeatMode: Int) { this@AvatekViewModel.repeatMode = repeatMode; persistQueue() }
        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) { shuffle = shuffleModeEnabled; persistQueue() }
        override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) = syncQueue()
        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) { speed = playbackParameters.speed; persistQueue() }
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
        viewModelScope.launch {
            while (isActive) {
                delay(5_000L)
                persistQueue()
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
                loadLyrics(currentId)
                isPlaying = c.isPlaying
                repeatMode = c.repeatMode
                shuffle = c.shuffleModeEnabled
                speed = c.playbackParameters.speed
                engineReady = true
                syncQueue()
                restoreQueueIfReady()
                refreshEqualizer()
            }.onFailure { showToast("اتصال موتور پخش برقرار نشد") }
        }, ContextCompat.getMainExecutor(app))
    }

    override fun onCleared() {
        persistQueue()
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
        persistQueue()
    }

    /** Restores the last local queue only after both Media3 and the MediaStore library are ready. */
    private fun restoreQueueIfReady() {
        val c = controller ?: return
        if (!tracksLoaded || queueRestored) return
        queueRestored = true
        val saved = queueStore.load() ?: return
        val available = saved.trackIds.mapNotNull { id -> byId[id] }
        if (available.isEmpty()) { queueStore.clear(); return }
        val savedCurrentId = saved.trackIds.getOrNull(saved.currentIndex)
        val restoredIndex = available.indexOfFirst { it.id == savedCurrentId }.let { if (it >= 0) it else 0 }
        val restoredPosition = saved.positionMs.coerceAtMost(available[restoredIndex].durationMs.coerceAtLeast(0L))
        c.setMediaItems(available.map { it.toMediaItem() }, restoredIndex, restoredPosition)
        c.repeatMode = saved.repeatMode
        c.shuffleModeEnabled = saved.shuffle
        c.setPlaybackParameters(PlaybackParameters(saved.speed, 1f))
        c.prepare()
        c.playWhenReady = saved.playWhenReady
        currentId = available[restoredIndex].id
        syncQueue()
        showToast("صف پخش قبلی بازیابی شد")
    }

    /** Persist a compact queue snapshot; audio files themselves are never copied. */
    private fun persistQueue() {
        if (!queueRestored) return
        val c = controller ?: return
        if (c.mediaItemCount == 0) { queueStore.clear(); return }
        val ids = (0 until c.mediaItemCount).mapNotNull { c.getMediaItemAt(it).mediaId.toLongOrNull() }
        if (ids.isEmpty()) { queueStore.clear(); return }
        queueStore.save(
            QueueSnapshot(
                trackIds = ids,
                currentIndex = c.currentMediaItemIndex.coerceIn(0, ids.lastIndex),
                positionMs = c.currentPosition.coerceAtLeast(0L),
                repeatMode = c.repeatMode,
                shuffle = c.shuffleModeEnabled,
                speed = c.playbackParameters.speed,
                playWhenReady = c.playWhenReady
            )
        )
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
        tracksLoaded = true
        albums = list.groupBy { it.albumKey }.map { (key, items) ->
            AlbumInfo(key, items.first().album, items.first().artist, items.map { it.id }, items.sumOf { it.durationMs })
        }.sortedBy { it.title.lowercase() }
        artists = list.groupBy { it.artist }.map { (name, items) ->
            ArtistInfo(name, items.map { it.id }, items.map { it.album }.distinct().size)
        }.sortedBy { it.name.lowercase() }
        // drop ids that no longer exist
        recentIds = recentIds.filter { it in byId }
        restoreQueueIfReady()
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
            val freshBoost = if (addedDays in 0L..14L) (14 - addedDays) * 0.7 else 0.0
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
    fun clearQueue() { controller?.let { it.stop(); it.clearMediaItems() }; queueStore.clear(); currentId = null; positionMs = 0L }

    // ---- offline synced lyrics ----------------------------------------------------------------
    fun toggleLyrics() {
        lyricsVisible = !lyricsVisible
        if (lyricsVisible && lyrics.isEmpty()) loadLyrics(currentId)
    }

    fun importLyrics(uri: Uri) {
        val id = currentId
        if (id == null) { showToast("ابتدا یک آهنگ پخش کن"); return }
        val raw = runCatching {
            getApplication<Application>().contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
        }.getOrNull()?.take(LyricsStore.MAX_LRC_CHARS).orEmpty()
        val parsed = parseLrc(raw)
        if (parsed.isEmpty()) { showToast("فایل LRC معتبر یا دارای زمان‌بندی نیست"); return }
        lyricsStore.save(id, raw)
        lyrics = parsed
        lyricsVisible = true
        showToast("متن آهنگ به‌صورت آفلاین ذخیره شد")
    }

    private fun loadLyrics(id: Long?) {
        lyrics = id?.let { lyricsStore.load(it) }?.let(::parseLrc).orEmpty()
    }

    private fun parseLrc(raw: String): List<LyricLine> {
        val timestamp = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]""")
        val offset = Regex("""\[offset:([+-]?\d+)]""").find(raw)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
        return raw.lineSequence().flatMap { line ->
            val matches = timestamp.findAll(line).toList()
            val text = timestamp.replace(line, "").trim()
            if (matches.isEmpty() || text.isBlank()) emptySequence()
            else matches.asSequence().mapNotNull { match ->
                val minutes = match.groupValues[1].toLongOrNull() ?: return@mapNotNull null
                val seconds = match.groupValues[2].toLongOrNull() ?: return@mapNotNull null
                val fraction = match.groupValues.getOrNull(3).orEmpty().padEnd(3, '0').take(3).toLongOrNull() ?: 0L
                LyricLine((minutes * 60_000L + seconds * 1_000L + fraction + offset).coerceAtLeast(0L), text)
            }
        }.sortedBy { it.timeMs }.toList()
    }

    val activeLyric: LyricLine? get() = lyrics.lastOrNull { it.timeMs <= positionMs }
    val nextLyric: LyricLine? get() = activeLyric?.let { active ->
        val index = lyrics.indexOf(active)
        lyrics.getOrNull(index + 1)
    } ?: lyrics.firstOrNull()

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

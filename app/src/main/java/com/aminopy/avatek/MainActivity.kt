package com.aminopy.avatek

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.aminopy.avatek.data.AudioLibraryRepository
import com.aminopy.avatek.data.AudioTrack
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
                        onScan = { requestAudioPermission() },
                        onPlay = { index -> playTrack(index) },
                        onTogglePlayback = { togglePlayback() }
                    )
                }
            }
        }
        if (hasAudioPermission()) refreshLibrary()
    }

    override fun onStart() {
        super.onStart()
        connectPlaybackController()
    }

    override fun onStop() {
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
    onScan: () -> Unit,
    onPlay: (Int) -> Unit,
    onTogglePlayback: () -> Unit
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
            Box(
                modifier = Modifier
                    .background(AvatekColors.Surface, RoundedCornerShape(18.dp))
                    .padding(13.dp)
            ) {
                Icon(Icons.Rounded.Headphones, contentDescription = "Offline music", tint = AvatekColors.Gold)
            }
        }

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(currentTitle, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(if (isPlaying) "در حال پخش" else "پخش‌کنندهٔ محلی", color = AvatekColors.Muted, fontSize = 10.sp)
                    }
                    IconButton(onClick = onTogglePlayback) {
                        Icon(
                            if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "توقف موقت" else "پخش",
                            tint = AvatekColors.Gold
                        )
                    }
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
        if (tracks.isEmpty()) {
            Button(
                onClick = onScan,
                colors = ButtonDefaults.buttonColors(containerColor = AvatekColors.BloodRed),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("اجازه دسترسی و اسکن آهنگ‌ها", color = Color.White)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f, fill = true),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AvatekColors.Surface, RoundedCornerShape(15.dp))
                            .clickable { onPlay(index) }
                            .padding(horizontal = 13.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.LibraryMusic, contentDescription = null, tint = AvatekColors.Gold)
                        Column(modifier = Modifier.weight(1f).padding(start = 11.dp)) {
                            Text(track.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(track.artist, color = AvatekColors.Muted, fontSize = 11.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { onPlay(index) }) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "پخش ${track.title}", tint = AvatekColors.Gold)
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

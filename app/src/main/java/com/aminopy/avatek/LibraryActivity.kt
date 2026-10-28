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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.aminopy.avatek.library.AudioLibraryRepository
import com.aminopy.avatek.library.AudioTrack
import com.aminopy.avatek.playback.MusicPlaybackService
import com.aminopy.avatek.ui.theme.AvatekTheme
import com.google.common.util.concurrent.ListenableFuture

private val Ink = Color(0xFF0A0B0D)
private val Panel = Color(0xFF17191E)
private val Gold = Color(0xFFD9B56D)
private val Red = Color(0xFF9E2638)
private val Muted = Color(0xFF9A9DA5)

class LibraryActivity : ComponentActivity() {
    private val tracks = mutableStateListOf<AudioTrack>()
    private var status by mutableStateOf("برای شروع، دسترسی آهنگ‌های دستگاه را بده.")
    private var controller by mutableStateOf<MediaController?>(null)
    private var future: ListenableFuture<MediaController>? = null
    private var scanning by mutableStateOf(false)

    private fun permission() = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
    private val permissionRequest = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) scan() else status = "مجوز داده نشد؛ بدون آن نمی‌توان آهنگ‌های محلی را فهرست کرد."
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val token = SessionToken(this, ComponentName(this, MusicPlaybackService::class.java))
        val pending = MediaController.Builder(this, token).buildAsync()
        future = pending
        pending.addListener({ runCatching { pending.get() }.onSuccess { controller = it }.onFailure { status = "اتصال موتور پخش ناموفق بود." } }, ContextCompat.getMainExecutor(this))

        setContent {
            AvatekTheme {
                Surface(Modifier.fillMaxSize(), color = Ink) {
                    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("AVATEK", color = Gold, fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
                        Text("YOUR MUSIC. YOUR WORLD.", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
                        Text("کتابخانهٔ موسیقی", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(status, color = Muted, fontSize = 13.sp, lineHeight = 20.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                if (ContextCompat.checkSelfPermission(this@LibraryActivity, permission()) == PackageManager.PERMISSION_GRANTED) scan()
                                else permissionRequest.launch(permission())
                            }, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink)) {
                                Icon(Icons.Rounded.Refresh, null)
                                Text(if (scanning) "  در حال اسکن…" else "  اسکن آهنگ‌ها")
                            }
                            Button(onClick = { controller?.let { if (it.isPlaying) it.pause() else it.play() } }, colors = ButtonDefaults.buttonColors(containerColor = Red)) {
                                Icon(if (controller?.isPlaying == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null)
                                Text(" پخش / مکث")
                            }
                        }
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(tracks, key = { it.id }) { track ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth().clickable { play(track) },
                                    color = Panel,
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Icon(Icons.Rounded.LibraryMusic, null, tint = Gold)
                                        Column(Modifier.weight(1f)) {
                                            Text(track.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                            Text(track.artist, color = Muted, fontSize = 12.sp, maxLines = 1)
                                        }
                                        Icon(Icons.Rounded.PlayArrow, null, tint = Gold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (ContextCompat.checkSelfPermission(this, permission()) == PackageManager.PERMISSION_GRANTED) scan()
    }

    private fun scan() {
        scanning = true
        Thread {
            val result = runCatching { AudioLibraryRepository(applicationContext).loadTracks() }
            runOnUiThread {
                result.onSuccess { found ->
                    tracks.clear()
                    tracks.addAll(found)
                    status = if (found.isEmpty()) "آهنگ محلی پیدا نشد. فایل‌های صوتی دستگاه را بررسی کن." else "${found.size} آهنگ پیدا شد؛ برای پخش روی هر آهنگ بزن."
                }.onFailure { status = "اسکن ناموفق بود؛ مجوز دسترسی را بررسی کن." }
                scanning = false
            }
        }.start()
    }

    private fun play(track: AudioTrack) {
        val player = controller ?: run { status = "موتور پخش هنوز آماده نیست؛ دوباره تلاش کن."; return }
        val queue = tracks.map { audio ->
            MediaItem.Builder().setMediaId(audio.id.toString()).setUri(audio.contentUri)
                .setMediaMetadata(MediaMetadata.Builder().setTitle(audio.title).setArtist(audio.artist).setAlbumTitle(audio.album).build())
                .build()
        }
        player.setMediaItems(queue, tracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0), 0L)
        player.prepare()
        player.play()
    }

    override fun onDestroy() {
        future?.let(MediaController::releaseFuture)
        controller = null
        super.onDestroy()
    }
}

package com.aminopy.avatek.ui.theme

import android.content.ContentUris
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.random.Random

/** Cover art result: the real bitmap when the file has one, plus its dominant tone. */
class ArtEntry(val bitmap: Bitmap?, val average: Color?)

private object ArtCache {
    val cache = LruCache<String, ArtEntry>(96)
}

/** Deterministic colour pair for a seed (album/artist/playlist) – used when there is no embedded art. */
fun seedColors(seed: String): Pair<Color, Color> {
    val h = abs(seed.hashCode())
    val hue1 = (h % 360).toFloat()
    val hue2 = ((h / 7) % 360).toFloat()
    return Color.hsv(hue1, 0.62f, 0.78f) to Color.hsv((hue1 + 40f + (hue2 % 60f)) % 360f, 0.78f, 0.38f)
}

fun seedTone(seed: String): Color = seedColors(seed).first

@Composable
fun rememberArtwork(trackId: Long, albumId: Long, sizePx: Int): ArtEntry? {
    val context = LocalContext.current
    val state = produceState<ArtEntry?>(initialValue = ArtCache.cache.get("$trackId@$sizePx"), trackId, albumId, sizePx) {
        val key = "$trackId@$sizePx"
        val cached = ArtCache.cache.get(key)
        if (cached != null) { value = cached; return@produceState }
        val entry = withContext(Dispatchers.IO) {
            val bmp: Bitmap? = runCatching {
                if (Build.VERSION.SDK_INT >= 29) {
                    val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, trackId)
                    context.contentResolver.loadThumbnail(uri, Size(sizePx, sizePx), null)
                } else if (albumId > 0L) {
                    val uri = ContentUris.withAppendedId(
                        android.net.Uri.parse("content://media/external/audio/albumart"), albumId
                    )
                    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                } else null
            }.getOrNull()
            val avg = bmp?.let {
                runCatching {
                    val px = Bitmap.createScaledBitmap(it, 1, 1, true).getPixel(0, 0)
                    Color(px)
                }.getOrNull()
            }
            ArtEntry(bmp, avg)
        }
        ArtCache.cache.put(key, entry)
        value = entry
    }
    return state.value
}

/**
 * Album/track cover: the embedded art when available, otherwise a generated
 * gradient cover with concentric grooves, unique for each [seed].
 */
@Composable
fun Artwork(
    seed: String,
    trackId: Long,
    albumId: Long,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    sizePx: Int = 256
) {
    val art = rememberArtwork(trackId, albumId, sizePx)
    Box(modifier.clip(shape)) {
        val bmp = art?.bitmap
        if (bmp != null) {
            val image = remember(bmp) { bmp.asImageBitmap() }
            Image(bitmap = image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            GeneratedCover(seed)
        }
    }
}

@Composable
fun GeneratedCover(seed: String, modifier: Modifier = Modifier.fillMaxSize()) {
    val (c1, c2) = remember(seed) { seedColors(seed) }
    Canvas(modifier) {
        drawRect(Brush.linearGradient(listOf(c1, c2), start = Offset.Zero, end = Offset(size.width, size.height)))
        val rnd = Random(seed.hashCode())
        val m = size.minDimension
        repeat(3) {
            val r = m * (0.25f + rnd.nextFloat() * 0.5f)
            drawCircle(
                color = Color.White.copy(alpha = 0.05f + rnd.nextFloat() * 0.07f),
                radius = r,
                center = Offset(size.width * rnd.nextFloat(), size.height * rnd.nextFloat())
            )
        }
        val cx = size.width * 0.5f
        val cy = size.height * 0.5f
        for (i in 1..5) {
            drawCircle(
                color = Color.White.copy(alpha = 0.10f - i * 0.012f),
                radius = m * (0.12f + i * 0.085f),
                center = Offset(cx, cy),
                style = Stroke(width = m * 0.006f)
            )
        }
        drawCircle(Color.Black.copy(alpha = 0.25f), radius = m * 0.075f, center = Offset(cx, cy))
        drawCircle(Color.White.copy(alpha = 0.55f), radius = m * 0.02f, center = Offset(cx, cy))
    }
}

/** The ambient colour (cover average, or seeded tone) for the glow behind the UI. */
@Composable
fun rememberAmbient(trackId: Long?, albumId: Long, seed: String?, fallback: Color): Color {
    if (trackId == null || seed == null) return fallback
    val art = rememberArtwork(trackId, albumId, 64)
    return art?.average?.let { boost(it) } ?: seedTone(seed)
}

private fun boost(c: Color): Color {
    // keep the hue but make sure the glow is bright enough to read on a dark background
    val max = maxOf(c.red, c.green, c.blue).coerceAtLeast(0.05f)
    val k = (0.75f / max).coerceAtMost(3f)
    return Color((c.red * k).coerceIn(0f, 1f), (c.green * k).coerceIn(0f, 1f), (c.blue * k).coerceIn(0f, 1f))
}

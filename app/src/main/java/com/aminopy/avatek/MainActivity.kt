package com.aminopy.avatek

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aminopy.avatek.ui.theme.AvatekTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AvatekTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = AvatekColors.Background) {
                    AvatekHome()
                }
            }
        }
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
private fun AvatekHome() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF17130F), AvatekColors.Background, AvatekColors.Background)
                )
            )
            .padding(horizontal = 22.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "AVATEK",
                    color = AvatekColors.Gold,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp
                )
                Text(
                    text = "YOUR MUSIC. YOUR WORLD.",
                    color = AvatekColors.Muted,
                    fontSize = 10.sp,
                    letterSpacing = 2.sp
                )
            }
            Box(
                modifier = Modifier
                    .background(AvatekColors.Surface, RoundedCornerShape(18.dp))
                    .padding(13.dp)
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Rounded.Headphones,
                    contentDescription = "Offline music",
                    tint = AvatekColors.Gold
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("صدای خودت را پیدا کن", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(
                "موسیقی‌هایت، بدون اینترنت؛ فقط تو و صدا.",
                color = AvatekColors.Muted,
                fontSize = 14.sp
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = AvatekColors.Surface,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Rounded.LibraryMusic,
                        contentDescription = null,
                        tint = AvatekColors.Gold
                    )
                    Text(
                        text = "  کتابخانه موسیقی",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = "هنوز آهنگی بارگذاری نشده است. در مرحله بعد، اسکن امن فایل‌های صوتی دستگاه و پخش پس‌زمینه اضافه می‌شود.",
                    color = AvatekColors.Muted,
                    fontSize = 13.sp,
                    lineHeight = 21.sp
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeatureTile(
                modifier = Modifier.weight(1f),
                icon = { androidx.compose.material3.Icon(Icons.Rounded.Album, contentDescription = null, tint = AvatekColors.Gold) },
                title = "آلبوم‌ها",
                subtitle = "ALBUMS"
            )
            FeatureTile(
                modifier = Modifier.weight(1f),
                icon = { androidx.compose.material3.Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = AvatekColors.Gold) },
                title = "صدا و اکولایزر",
                subtitle = "SOUND LAB"
            )
        }

        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "OFFLINE FIRST  •  DESIGNED FOR ANDROID",
            color = AvatekColors.BloodRed,
            fontSize = 10.sp,
            letterSpacing = 1.5.sp,
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
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = AvatekColors.Surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            icon()
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, color = AvatekColors.Muted, fontSize = 9.sp, letterSpacing = 1.sp)
        }
    }
}

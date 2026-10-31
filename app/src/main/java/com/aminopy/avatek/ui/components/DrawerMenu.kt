package com.aminopy.avatek.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aminopy.avatek.state.AvatekViewModel
import com.aminopy.avatek.ui.AppUi
import com.aminopy.avatek.ui.nav.Route
import com.aminopy.avatek.ui.theme.LocalPalette
import com.aminopy.avatek.ui.theme.glass
import com.aminopy.avatek.ui.theme.bounceClick

@Composable
fun AvatekDrawer(vm: AvatekViewModel, ui: AppUi) {
    val p = LocalPalette.current
    AnimatedVisibility(
        visible = ui.drawerOpen,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.58f)).clickable { ui.closeDrawer() }) {
            AnimatedVisibility(
                visible = ui.drawerOpen,
                modifier = Modifier.align(Alignment.CenterStart),
                enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
            ) {
                Column(
                    Modifier.fillMaxHeight().fillMaxWidth(0.86f)
                        .glass(RoundedCornerShape(topEnd = 30.dp, bottomEnd = 30.dp), tint = p.bgTop, strength = 0.95f)
                        .clickable { }.navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(52.dp).glass(CircleShape, tint = p.accent, strength = 0.8f), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.MusicNote, null, tint = p.accent, modifier = Modifier.size(28.dp))
                        }
                        Column {
                            Text("AVATEK", color = p.text, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                            Text("YOUR MUSIC. YOUR WORLD.", color = p.muted, fontSize = 9.sp, letterSpacing = 1.5.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    DrawerSection("ناوبری")
                    DrawerItem(Icons.Rounded.Home, "خانه", "نمای کلی موسیقی", p.accent) { go(ui, Route.Home) }
                    DrawerItem(Icons.Rounded.LibraryMusic, "کتابخانه", "آهنگ‌ها، آلبوم‌ها و هنرمندان", p.accent) { go(ui, Route.Library) }
                    DrawerItem(Icons.Rounded.Favorite, "علاقه‌مندی‌ها", "مجموعه‌ی منتخب تو", p.accent) { go(ui, Route.Favorites) }
                    DrawerItem(Icons.Rounded.GraphicEq, "جست‌وجو", "پیدا کردن سریع موسیقی", p.accent) { go(ui, Route.Search) }
                    Spacer(Modifier.height(8.dp))
                    DrawerSection("هوشمند و شخصی")
                    DrawerItem(Icons.Rounded.AutoAwesome, "Daily Mix", "ترکیب بر اساس سلیقه و سابقه‌ی پخش", p.accent) {
                        ui.closeDrawer(); vm.playList(vm.dailyMix(40)); ui.openPlayer()
                    }
                    DrawerItem(Icons.Rounded.SurroundSound, "Echo Radio", "رادیوی آفلاین بر پایه‌ی آهنگ فعلی", p.accent) {
                        ui.closeDrawer()
                        val radio = vm.echoRadio(40)
                        if (radio.isNotEmpty()) { vm.playList(radio); ui.openPlayer() } else vm.showToast("برای ساخت رادیو، آهنگی را پخش کن")
                    }
                    DrawerItem(Icons.Rounded.Shuffle, "Discovery Mix", "کشف آهنگ‌های کمتر شنیده‌شده", p.accent) {
                        ui.closeDrawer(); vm.playList(vm.discoveryMix(40)); ui.openPlayer()
                    }
                    DrawerItem(Icons.Rounded.History, "نبض شنیدن", "آمار و تاریخچه‌ی پخش", p.accent) { go(ui, Route.ListeningStats) }
                    DrawerItem(Icons.Rounded.AutoAwesome, "Smart Crate", "مجموعه‌های خودکار با قوانین محلی", p.accent) { go(ui, Route.SmartCrate) }
                    DrawerItem(Icons.Rounded.AutoAwesome, "AVATEK Intelligence", "DNA، دی‌جی هوشمند و Mood Studio", p.accent) { go(ui, Route.Intelligence) }
                    Spacer(Modifier.height(8.dp))
                    DrawerSection("ابزارها")
                    DrawerItem(Icons.Rounded.Equalizer, "Audio Lab", "اکولایزر، بیس و افکت‌های صدا", p.accent) { go(ui, Route.Lab) }
                    DrawerItem(Icons.Rounded.Settings, "تنظیمات", "ظاهر، کتابخانه و پشتیبان‌گیری", p.accent) { go(ui, Route.Settings) }
                    DrawerItem(Icons.Rounded.PlaylistPlay, "صف پخش", "مدیریت آهنگ‌های در انتظار", p.accent) {
                        ui.closeDrawer(); ui.show(com.aminopy.avatek.ui.Sheet.Queue)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("همه‌ی پیشنهادها و آمارها روی دستگاه پردازش می‌شوند.", color = p.muted, fontSize = 10.sp, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
                }
            }
        }
    }
}

private fun go(ui: AppUi, route: Route) {
    ui.closeDrawer()
    if (route in listOf(Route.Favorites, Route.ListeningStats, Route.SmartCrate, Route.Intelligence)) ui.nav.push(route) else ui.nav.selectTab(route)
}

@Composable
private fun DrawerSection(title: String) {
    Text(title, color = LocalPalette.current.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 3.dp))
}

@Composable
private fun DrawerItem(icon: ImageVector, title: String, subtitle: String, tint: Color, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().bounceClick(pressedScale = 0.98f, onClick = onClick)
            .glass(RoundedCornerShape(18.dp), strength = 0.45f).padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(38.dp).glass(RoundedCornerShape(13.dp), tint = tint, strength = 0.65f), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = p.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = p.muted, fontSize = 10.sp, lineHeight = 13.sp)
        }
    }
}

package com.aminopy.avatek.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aminopy.avatek.state.AvatekViewModel
import com.aminopy.avatek.ui.components.FloatingNavBar
import com.aminopy.avatek.ui.components.MiniPlayer
import com.aminopy.avatek.ui.components.SheetHost
import com.aminopy.avatek.ui.components.ToastHost
import com.aminopy.avatek.ui.nav.NavAction
import com.aminopy.avatek.ui.nav.Navigator
import com.aminopy.avatek.ui.nav.Route
import com.aminopy.avatek.ui.screens.AboutScreen
import com.aminopy.avatek.ui.screens.AlbumScreen
import com.aminopy.avatek.ui.screens.ArtistScreen
import com.aminopy.avatek.ui.screens.FavoritesScreen
import com.aminopy.avatek.ui.screens.HomeScreen
import com.aminopy.avatek.ui.screens.LabScreen
import com.aminopy.avatek.ui.screens.LibraryScreen
import com.aminopy.avatek.ui.screens.NowPlayingScreen
import com.aminopy.avatek.ui.screens.PlaylistScreen
import com.aminopy.avatek.ui.screens.SearchScreen
import com.aminopy.avatek.ui.screens.SettingsScreen
import com.aminopy.avatek.ui.screens.SplashScreen
import com.aminopy.avatek.ui.theme.AuroraBackground
import com.aminopy.avatek.ui.theme.AvatekTheme
import com.aminopy.avatek.ui.theme.Emphasized
import com.aminopy.avatek.ui.theme.LocalHaze
import com.aminopy.avatek.ui.theme.LocalPalette
import com.aminopy.avatek.ui.theme.rememberAmbient
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlin.math.sign

@Composable
fun AvatekApp(vm: AvatekViewModel, requestPermission: () -> Unit, requestLyricsImport: () -> Unit = {}) {
    val nav = remember { Navigator() }
    val ui = remember { AppUi(nav, requestLyricsImport) }
    val haze = rememberHazeState()
    var splash by rememberSaveable { mutableStateOf(true) }

    AvatekTheme(vm.settings) {
        val p = LocalPalette.current
        val track = vm.currentTrack
        val ambient = rememberAmbient(track?.id, track?.albumId ?: 0L, track?.albumKey, p.secondary)
        val navBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val bottomInset by animateDpAsState(
            navBar + 12.dp + 70.dp + 16.dp + (if (track != null) 76.dp else 0.dp),
            spring(0.8f, 300f), label = "inset"
        )

        BackHandler(enabled = ui.playerOpen) { ui.closePlayer() }
        BackHandler(enabled = !ui.playerOpen && nav.canPop) { nav.pop() }
        BackHandler(enabled = !ui.playerOpen && !nav.canPop && nav.current != Route.Home) { nav.selectTab(Route.Home) }

        CompositionLocalProvider(
            LocalAppUi provides ui,
            LocalVm provides vm,
            LocalHaze provides haze,
            LocalBottomInset provides bottomInset,
            LocalPermissionRequester provides requestPermission
        ) {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().hazeSource(haze)) {
                    AuroraBackground(ambient)
                    AnimatedContent(
                        targetState = nav.current,
                        transitionSpec = { screenTransition(nav) },
                        label = "nav"
                    ) { route ->
                        when (route) {
                            Route.Home -> HomeScreen(vm, ui)
                            Route.Library -> LibraryScreen(vm, ui)
                            Route.Search -> SearchScreen(vm, ui)
                            Route.Lab -> LabScreen(vm, ui)
                            Route.Settings -> SettingsScreen(vm, ui)
                            Route.Favorites -> FavoritesScreen(vm, ui)
                            Route.About -> AboutScreen(vm, ui)
                            is Route.Album -> AlbumScreen(vm, ui, route.key)
                            is Route.Artist -> ArtistScreen(vm, ui, route.name)
                            is Route.Playlist -> PlaylistScreen(vm, ui, route.id)
                        }
                    }
                    AnimatedVisibility(
                        visible = ui.playerOpen,
                        enter = slideInVertically(spring(0.85f, 300f)) { it } + fadeIn(tween(220)),
                        exit = slideOutVertically(tween(340, easing = Emphasized)) { it } + fadeOut(tween(260))
                    ) { NowPlayingScreen(vm, ui) }
                }

                Column(
                    Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp).navigationBarsPadding().padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MiniPlayer(vm, ui, haze)
                    AnimatedVisibility(
                        visible = !ui.playerOpen,
                        enter = slideInVertically(spring(0.8f, 380f)) { it * 2 } + fadeIn(),
                        exit = slideOutVertically(tween(260)) { it * 2 } + fadeOut(tween(200))
                    ) { FloatingNavBar(nav, haze) }
                }

                SheetHost(vm, ui)
                ToastHost(vm.toast)
                AnimatedVisibility(splash, exit = fadeOut(tween(550)) + scaleOut(tween(550), targetScale = 1.12f)) {
                    SplashScreen { splash = false }
                }
            }
        }
    }
}

private fun AnimatedContentTransitionScope<Route>.screenTransition(nav: Navigator): ContentTransform =
    when (nav.lastAction) {
        NavAction.Push ->
            (slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, spring(0.85f, 380f)) +
                fadeIn(tween(260)) + scaleIn(tween(320), initialScale = 0.92f)) togetherWith
                (fadeOut(tween(220)) + scaleOut(tween(320), targetScale = 0.94f))
        NavAction.Pop ->
            ((fadeIn(tween(260)) + scaleIn(tween(320), initialScale = 0.94f)) togetherWith
                (slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(320, easing = Emphasized)) + fadeOut(tween(240)))
                ).apply { targetContentZIndex = -1f }
        NavAction.Tab -> {
            val dir = sign((nav.tabIndex(targetState) - nav.tabIndex(initialState)).toFloat()).toInt()
            (fadeIn(tween(340, delayMillis = 70)) + slideInHorizontally(tween(420, easing = Emphasized)) { dir * it / 7 } +
                scaleIn(tween(420, easing = Emphasized), initialScale = 0.96f)) togetherWith
                (fadeOut(tween(170)) + slideOutHorizontally(tween(320)) { -dir * it / 7 } + scaleOut(tween(320), targetScale = 0.98f))
        }
    }

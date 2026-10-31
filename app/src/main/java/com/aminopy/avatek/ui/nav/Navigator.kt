package com.aminopy.avatek.ui.nav

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

sealed interface Route {
    data object Home : Route
    data object Library : Route
    data object Search : Route
    data object Lab : Route
    data object Settings : Route
    data object Favorites : Route
    data object About : Route
    data object ListeningStats : Route
    data object SmartCrate : Route
    data object Intelligence : Route
    data class Album(val key: String) : Route
    data class Artist(val name: String) : Route
    data class Playlist(val id: String) : Route
}

val TabRoutes: List<Route> = listOf(Route.Home, Route.Library, Route.Search, Route.Lab, Route.Settings)

enum class NavAction { Tab, Push, Pop }

class Navigator {
    var stack by mutableStateOf<List<Route>>(listOf(Route.Home)); private set
    var lastAction by mutableStateOf(NavAction.Tab); private set
    val current: Route get() = stack.last()
    val depth: Int get() = stack.size
    val canPop: Boolean get() = stack.size > 1

    fun tabIndex(route: Route): Int = TabRoutes.indexOf(route)

    fun selectTab(route: Route) {
        if (stack.size == 1 && stack.first() == route) return
        lastAction = NavAction.Tab
        stack = listOf(route)
    }

    fun push(route: Route) {
        if (current == route) return
        lastAction = NavAction.Push
        stack = stack + route
    }

    fun pop(): Boolean {
        if (!canPop) return false
        lastAction = NavAction.Pop
        stack = stack.dropLast(1)
        return true
    }
}

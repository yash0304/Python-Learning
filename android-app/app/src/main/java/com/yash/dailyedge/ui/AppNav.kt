package com.yash.dailyedge.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yash.dailyedge.data.Track
import com.yash.dailyedge.ui.screens.CalculatorListScreen
import com.yash.dailyedge.ui.screens.CalculatorScreen
import com.yash.dailyedge.ui.screens.LessonScreen
import com.yash.dailyedge.ui.screens.ProfileScreen
import com.yash.dailyedge.ui.screens.TodayScreen
import com.yash.dailyedge.ui.screens.TrackScreen

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("today", "Today", Icons.Filled.WbSunny),
    Tab("track/${Track.AI_EDGE.key}", "Human Edge", Icons.Filled.Psychology),
    Tab("track/${Track.ENGLISH.key}", "English", Icons.Filled.RecordVoiceOver),
    Tab("track/${Track.FINANCE.key}", "Finance", Icons.Filled.CurrencyRupee),
    Tab("profile", "Progress", Icons.Filled.AccountCircle),
)

@Composable
fun AppNav(vm: AppViewModel) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = current == tab.route,
                        onClick = { nav.navigateTab(tab.route) },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, maxLines = 1) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = "today", modifier = Modifier.padding(padding)) {
            composable("today") {
                TodayScreen(vm, openLesson = { nav.navigate("lesson/$it") }, openTrack = { nav.navigateTab("track/${it.key}") })
            }
            Track.entries.forEach { track ->
                composable("track/${track.key}") {
                    TrackScreen(vm, track, openLesson = { nav.navigate("lesson/$it") }, openCalculators = { nav.navigate("calculators") })
                }
            }
            composable("lesson/{id}") { entry ->
                LessonScreen(
                    vm = vm,
                    lessonId = entry.arguments?.getString("id").orEmpty(),
                    onBack = { nav.popBackStack() },
                    openCalculator = { nav.navigate("calculator/$it") },
                )
            }
            composable("calculators") {
                CalculatorListScreen(onBack = { nav.popBackStack() }, open = { nav.navigate("calculator/$it") })
            }
            composable("calculator/{id}") { entry ->
                CalculatorScreen(entry.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() })
            }
            composable("profile") {
                ProfileScreen(vm, openCalculators = { nav.navigate("calculators") })
            }
        }
    }
}

private fun NavHostController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

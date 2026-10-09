package com.yash.dailyedge.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yash.dailyedge.ui.screens.CalculatorListScreen
import com.yash.dailyedge.ui.screens.CalculatorScreen
import com.yash.dailyedge.ui.screens.LearnScreen
import com.yash.dailyedge.ui.screens.LessonScreen
import com.yash.dailyedge.ui.screens.ProfileScreen
import com.yash.dailyedge.ui.screens.TodayScreen
import com.yash.dailyedge.ui.screens.TrackScreen

private data class Tab(val route: String, val label: String, val emoji: String)

private val tabs = listOf(
    Tab("today", "Today", "🏠"),
    Tab("learn", "Learn", "🗺️"),
    Tab("tools", "Tools", "🧰"),
    Tab("me", "Me", "🏆"),
)

@Composable
fun AppNav(vm: AppViewModel) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                tabs.forEach { tab ->
                    val selected = current == tab.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = { nav.navigateTab(tab.route) },
                        icon = { Text(tab.emoji, style = if (selected) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge) },
                        label = { Text(tab.label, fontWeight = if (selected) FontWeight.Black else FontWeight.Medium) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = "today", modifier = Modifier.padding(padding)) {
            composable("today") {
                TodayScreen(vm, openLesson = { nav.navigate("lesson/$it") }, openTrack = { nav.navigate("track/$it") })
            }
            composable("learn") {
                LearnScreen(vm, openTrack = { nav.navigate("track/$it") })
            }
            composable("track/{key}") { entry ->
                TrackScreen(
                    vm = vm,
                    trackKey = entry.arguments?.getString("key").orEmpty(),
                    onBack = { nav.popBackStack() },
                    openLesson = { nav.navigate("lesson/$it") },
                    openTool = { nav.navigate("calculators") },
                )
            }
            composable("lesson/{id}") { entry ->
                LessonScreen(
                    vm = vm,
                    lessonId = entry.arguments?.getString("id").orEmpty(),
                    onBack = { nav.popBackStack() },
                    openCalculator = { nav.navigate("calculator/$it") },
                )
            }
            composable("tools") {
                CalculatorListScreen(onBack = null, open = { nav.navigate("calculator/$it") })
            }
            composable("calculators") {
                CalculatorListScreen(onBack = { nav.popBackStack() }, open = { nav.navigate("calculator/$it") })
            }
            composable("calculator/{id}") { entry ->
                CalculatorScreen(entry.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() })
            }
            composable("me") { ProfileScreen(vm) }
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

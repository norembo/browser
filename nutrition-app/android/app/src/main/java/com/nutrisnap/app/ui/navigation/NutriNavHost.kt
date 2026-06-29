package com.nutrisnap.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nutrisnap.app.feature.camera.ScanScreen
import com.nutrisnap.app.feature.fasting.FastingScreen
import com.nutrisnap.app.feature.insights.InsightsScreen
import com.nutrisnap.app.feature.onboarding.OnboardingScreen
import com.nutrisnap.app.feature.tracking.DiaryScreen
import com.nutrisnap.app.feature.water.WaterScreen

sealed class Route(val path: String, val label: String, val icon: ImageVector?) {
    data object Onboarding : Route("onboarding", "Onboarding", null)
    data object Diary : Route("diary", "Diary", Icons.Filled.RestaurantMenu)
    data object Scan : Route("scan", "Scan", Icons.Filled.CameraAlt)
    data object Fasting : Route("fasting", "Fasting", Icons.Filled.Timer)
    data object Water : Route("water", "Water", Icons.Filled.LocalDrink)
    data object Insights : Route("insights", "Insights", Icons.Filled.Insights)
}

private val bottomTabs = listOf(Route.Diary, Route.Scan, Route.Fasting, Route.Water, Route.Insights)

@Composable
fun NutriNavHost() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination
    val showBar = bottomTabs.any { tab -> current?.hierarchy?.any { it.route == tab.path } == true }

    Scaffold(
        bottomBar = {
            if (showBar) NavigationBar {
                bottomTabs.forEach { tab ->
                    val selected = current?.hierarchy?.any { it.route == tab.path } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            nav.navigate(tab.path) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { tab.icon?.let { Icon(it, contentDescription = tab.label) } },
                        label = { Text(tab.label) },
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            // Start at onboarding; OnboardingScreen routes to Diary once targets exist.
            startDestination = Route.Onboarding.path,
            modifier = Modifier.padding(padding),
        ) {
            composable(Route.Onboarding.path) {
                OnboardingScreen(onDone = {
                    nav.navigate(Route.Diary.path) {
                        popUpTo(Route.Onboarding.path) { inclusive = true }
                    }
                })
            }
            composable(Route.Diary.path) { DiaryScreen(onAddFood = { nav.navigate(Route.Scan.path) }) }
            composable(Route.Scan.path) { ScanScreen(onLogged = { nav.popBackStack() }) }
            composable(Route.Fasting.path) { FastingScreen() }
            composable(Route.Water.path) { WaterScreen() }
            composable(Route.Insights.path) { InsightsScreen() }
        }
    }
}

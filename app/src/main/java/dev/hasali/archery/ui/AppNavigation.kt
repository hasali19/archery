package dev.hasali.archery.ui

import android.content.Intent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import dev.hasali.archery.ActiveSessionService
import dev.hasali.archery.ArcheryApplication
import dev.hasali.archery.ui.scoring.SessionScoringScreen
import dev.hasali.archery.ui.scoring.SessionScoringViewModelFactory
import dev.hasali.archery.ui.sessions.SessionsScreen
import dev.hasali.archery.ui.sessions.SessionsViewModelFactory
import dev.hasali.archery.ui.settings.SettingsScreen
import dev.hasali.archery.ui.settings.SettingsViewModelFactory

private const val ROUTE_SESSIONS = "sessions"
private const val ROUTE_SESSION_SCORING = "session/{sessionId}"
private const val ROUTE_SETTINGS = "settings"
private const val ARG_SESSION_ID = "sessionId"
private const val DEEP_LINK_SESSION = "archery://session/{sessionId}"

private data class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private val topLevelDestinations = listOf(
    TopLevelDestination(ROUTE_SESSIONS, "Sessions", Icons.AutoMirrored.Filled.List),
    TopLevelDestination(ROUTE_SETTINGS, "Settings", Icons.Filled.Settings),
)

@Composable
fun AppNavigation(
    app: ArcheryApplication,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        // Top-level screens each render their own TopAppBar (which already handles the
        // status bar inset), so avoid double-applying it here - only reserve space
        // for the bottom navigation bar itself.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier,
        bottomBar = {
            if (topLevelDestinations.any { it.route == currentRoute }) {
                NavigationBar {
                    topLevelDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ROUTE_SESSIONS,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(ROUTE_SESSIONS) {
                val vm = viewModel<dev.hasali.archery.ui.sessions.SessionsViewModel>(
                    factory = SessionsViewModelFactory(app.sessionRepository),
                )
                SessionsScreen(
                    viewModel = vm,
                    onNavigateToSession = { sessionId ->
                        navController.navigate("session/$sessionId")
                    },
                )
            }

            composable(
                route = ROUTE_SESSION_SCORING,
                arguments = listOf(navArgument(ARG_SESSION_ID) { type = NavType.IntType }),
                deepLinks = listOf(navDeepLink { uriPattern = DEEP_LINK_SESSION }),
            ) { backStackEntry ->
                val sessionId = backStackEntry.arguments!!.getInt(ARG_SESSION_ID)
                val vm = viewModel<dev.hasali.archery.ui.scoring.SessionScoringViewModel>(
                    factory = SessionScoringViewModelFactory(sessionId, app.sessionRepository),
                )

                val context = LocalContext.current
                DisposableEffect(Unit) {
                    val intent = Intent(context, ActiveSessionService::class.java)
                    intent.putExtra("sessionId", sessionId)
                    ContextCompat.startForegroundService(context, intent)
                    onDispose {
                        context.stopService(intent)
                    }
                }

                SessionScoringScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable(ROUTE_SETTINGS) {
                val vm = viewModel<dev.hasali.archery.ui.settings.SettingsViewModel>(
                    factory = SettingsViewModelFactory(app.settingsRepository),
                )
                SettingsScreen(viewModel = vm)
            }
        }
    }
}

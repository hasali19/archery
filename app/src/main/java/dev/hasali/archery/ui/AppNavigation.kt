package dev.hasali.archery.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import dev.hasali.archery.ActiveSessionService
import dev.hasali.archery.ArcheryApplication
import dev.hasali.archery.ui.scoring.SessionScoringScreen
import dev.hasali.archery.ui.scoring.SessionScoringViewModelFactory
import dev.hasali.archery.ui.sessions.SessionsScreen
import dev.hasali.archery.ui.sessions.SessionsViewModelFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private const val ROUTE_SESSIONS = "sessions"
private const val ROUTE_SESSION_SCORING = "session/{sessionId}"
private const val ARG_SESSION_ID = "sessionId"
private const val DEEP_LINK_SESSION = "archery://session/{sessionId}"

@Composable
fun AppNavigation(app: ArcheryApplication) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = ROUTE_SESSIONS) {
        composable(ROUTE_SESSIONS) {
            val vm = viewModel<dev.hasali.archery.ui.sessions.SessionsViewModel>(
                factory = SessionsViewModelFactory(app.sessionRepository),
            )

            val context = LocalContext.current
            val scope = rememberCoroutineScope()

            SessionsScreen(
                viewModel = vm,
                onNavigateToSession = { sessionId ->
                    navController.navigate("session/$sessionId")
                },
                onExportDatabase = { uri: Uri ->
                    scope.launch {
                        try {
                            app.exportDatabaseTo(uri)
                            Toast.makeText(context, "Database exported", Toast.LENGTH_SHORT).show()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to export database", Toast.LENGTH_SHORT).show()
                        }
                    }
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
    }
}

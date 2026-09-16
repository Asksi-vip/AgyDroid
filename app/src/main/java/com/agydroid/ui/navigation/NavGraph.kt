package com.agydroid.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import com.agydroid.ui.auth.AuthScreen
import com.agydroid.ui.auth.AuthViewModel
import com.agydroid.ui.buildmonitor.BuildMonitorScreen
import com.agydroid.ui.chat.ChatScreen
import com.agydroid.ui.fileexplorer.FileExplorerScreen
import com.agydroid.ui.home.CreateProjectScreen
import com.agydroid.ui.home.HomeScreen

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Home : Screen("home")
    object CreateProject : Screen("create_project")
    object Chat : Screen("chat/{projectId}") {
        fun createRoute(projectId: String) = "chat/$projectId"
    }
    object FileExplorer : Screen("files/{projectId}") {
        fun createRoute(projectId: String) = "files/$projectId"
    }
    object BuildMonitor : Screen("build/{projectId}/{runId}") {
        fun createRoute(projectId: String, runId: Long) = "build/$projectId/$runId"
    }
}

@Composable
fun AgyDroidNavGraph() {
    val navController = rememberNavController()

    // Determine start destination based on auth state
    val authViewModel: AuthViewModel = hiltViewModel()
    val isAuthenticated by authViewModel.isAuthenticated.collectAsState(initial = null)

    val startDest = when (isAuthenticated) {
        true -> Screen.Home.route
        false -> Screen.Auth.route
        null -> Screen.Auth.route // Loading
    }

    NavHost(
        navController = navController,
        startDestination = startDest
    ) {
        composable(Screen.Auth.route) {
            AuthScreen(
                onAuthSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onCreateProject = { navController.navigate(Screen.CreateProject.route) },
                onOpenProject = { projectId ->
                    navController.navigate(Screen.Chat.createRoute(projectId))
                },
                onLogout = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.CreateProject.route) {
            CreateProjectScreen(
                onProjectCreated = { projectId ->
                    navController.navigate(Screen.Chat.createRoute(projectId)) {
                        popUpTo(Screen.Home.route)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
            ChatScreen(
                projectId = projectId,
                onNavigateToFiles = { navController.navigate(Screen.FileExplorer.createRoute(projectId)) },
                onNavigateToBuild = { runId ->
                    navController.navigate(Screen.BuildMonitor.createRoute(projectId, runId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.FileExplorer.route,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
            FileExplorerScreen(
                projectId = projectId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.BuildMonitor.route,
            arguments = listOf(
                navArgument("projectId") { type = NavType.StringType },
                navArgument("runId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
            val runId = backStackEntry.arguments?.getLong("runId") ?: return@composable
            BuildMonitorScreen(
                projectId = projectId,
                runId = runId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

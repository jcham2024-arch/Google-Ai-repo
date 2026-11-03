package com.example.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.agents.AgentListScreen
import com.example.ui.chat.ChatScreen
import com.example.ui.chat.PerchanceBrowserScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.workspace.WorkspaceScreen

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Perchance : Screen("perchance", "Perchance Hub", Icons.Default.Language)
    object Chat : Screen("chat", "Narrator Chat", Icons.Default.Chat)
    object Agents : Screen("agents", "Agent Swarm", Icons.Default.SmartToy)
    object Workspace : Screen("workspace", "Workspace", Icons.Default.Code)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val items = listOf(
        Screen.Perchance,
        Screen.Chat,
        Screen.Agents,
        Screen.Workspace,
        Screen.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Chat.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Perchance.route) { PerchanceBrowserScreen() }
            composable(Screen.Chat.route) { ChatScreen() }
            composable(Screen.Agents.route) { AgentListScreen() }
            composable(Screen.Workspace.route) { WorkspaceScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}

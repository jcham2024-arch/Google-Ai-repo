package com.example.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.agents.AgentListScreen
import com.example.ui.chat.ChatScreen
import com.example.ui.control.ControlCenterScreen
import com.example.ui.graph.GraphScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.workspace.WorkspaceScreen

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Chat : Screen("chat/{chatId}", "Chat", Icons.Default.Chat)
    object Graph : Screen("graph", "Graph Tree", Icons.Default.AccountTree)
    object Agents : Screen("agents", "Swarm", Icons.Default.SmartToy)
    object Control : Screen("control", "Control Center", Icons.Default.Security)
    object Workspace : Screen("workspace", "Workspace", Icons.Default.Code)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    companion object {
        fun chatRoute(chatId: String) = "chat/$chatId"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val items = listOf(
        Screen.Chat,
        Screen.Graph,
        Screen.Agents,
        Screen.Control,
        Screen.Workspace,
        Screen.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                items.forEach { screen ->
                    val isSelected = when (screen) {
                        is Screen.Chat -> currentRoute?.startsWith("chat/") == true
                        else -> currentRoute == screen.route
                    }
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title, maxLines = 1) },
                        selected = isSelected,
                        onClick = {
                            val targetRoute = if (screen is Screen.Chat) Screen.chatRoute("default") else screen.route
                            if (currentRoute != targetRoute) {
                                navController.navigate(targetRoute) {
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
            startDestination = Screen.chatRoute("default"),
            modifier = Modifier.padding(padding)
        ) {
            composable(
                route = Screen.Chat.route,
                arguments = listOf(navArgument("chatId") { 
                    type = NavType.StringType
                    defaultValue = "default" 
                })
            ) { backStackEntry ->
                val chatId = backStackEntry.arguments?.getString("chatId") ?: "default"
                ChatScreen(chatId = chatId)
            }
            composable(Screen.Graph.route) { GraphScreen() }
            composable(Screen.Agents.route) { AgentListScreen() }
            composable(Screen.Control.route) { ControlCenterScreen() }
            composable(Screen.Workspace.route) { WorkspaceScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}

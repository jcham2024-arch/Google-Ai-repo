package com.example.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Message
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
import kotlinx.coroutines.launch
import com.example.ui.agents.AgentListScreen
import com.example.ui.chat.ChatScreen
import com.example.ui.conversations.ConversationListScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.workspace.WorkspaceScreen

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Chat : Screen("chat/{chatId}", "Agent Chat", Icons.Default.Chat)
    object Conversations : Screen("conversations", "Conversations", Icons.Default.Message)
    object Agents : Screen("agents", "Agent Swarm", Icons.Default.SmartToy)
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val items = listOf(
        Screen.Chat,
        Screen.Conversations,
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
            composable(Screen.Conversations.route) { ConversationListScreen(
                onConversationClick = { chatId ->
                    navController.navigate(Screen.chatRoute(chatId)) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNewConversation = {
                    val newChatId = java.util.UUID.randomUUID().toString()
                    coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        val database = com.example.data.local.AppDatabase.getDatabase(context)
                        val conversation = com.example.data.model.ChatConversation(
                            id = newChatId,
                            title = "New Conversation",
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        database.apexDao().insertConversation(conversation)
                    }
                    navController.navigate(Screen.chatRoute(newChatId)) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            ) }
            composable(Screen.Agents.route) { AgentListScreen() }
            composable(Screen.Workspace.route) { WorkspaceScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}

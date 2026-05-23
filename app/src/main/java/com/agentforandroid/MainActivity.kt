package com.agentforandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.agentforandroid.ui.screens.ChatScreen
import com.agentforandroid.ui.screens.SettingsScreen
import com.agentforandroid.ui.screens.SkillManageScreen
import com.agentforandroid.ui.theme.AgentForAndroidTheme
import com.agentforandroid.ui.theme.AppPreferences

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode = AppPreferences.getThemeMode(this)
            AgentForAndroidTheme(themeMode = themeMode) {
                MainApp()
            }
        }
    }
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun MainApp() {
    val navController = rememberNavController()

    val isChinese = java.util.Locale.getDefault().language == "zh"
    val items = listOf(
        BottomNavItem("chat", if (isChinese) "对话" else "Chat", Icons.Filled.Home, Icons.Outlined.Home),
        BottomNavItem("skills", if (isChinese) "Skills" else "Skills", Icons.Filled.Star, Icons.Outlined.Star),
        BottomNavItem("settings", if (isChinese) "设置" else "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                items.forEach { item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                if (currentRoute == item.route) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) },
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "chat",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("chat") { ChatScreen() }
            composable("skills") { SkillManageScreen() }
            composable("settings") { SettingsScreen() }
        }
    }
}

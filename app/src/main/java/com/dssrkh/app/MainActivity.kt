package com.dssrkh.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dssrkh.app.data.SessionFactory
import com.dssrkh.app.ui.chat.ChatScreen
import com.dssrkh.app.ui.components.QqGroupDialog
import com.dssrkh.app.ui.memory.MemoryScreen
import com.dssrkh.app.ui.settings.SettingsScreen
import com.dssrkh.app.ui.setup.AgeGateScreen
import com.dssrkh.app.ui.setup.ApiKeyScreen
import com.dssrkh.app.ui.setup.FirstRunScreen
import com.dssrkh.app.ui.theme.DssrkhTheme
import com.dssrkh.app.ui.usage.UsageScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DssrkhTheme {
                val app = application as DssrkhApp
                val nav = rememberNavController()
                val start = when {
                    !app.settings.ageConfirmed -> "age"
                    app.settings.apiKey.isBlank() -> "setup"
                    else -> "boot"
                }

                var showReminder by remember { mutableStateOf(false) }
                var showQqGroup by remember { mutableStateOf(false) }
                val backStackEntry by nav.currentBackStackEntryAsState()

                LaunchedEffect(Unit) {
                    val minutes = app.settings.reminderMinutes
                    if (minutes <= 0) return@LaunchedEffect
                    delay(minutes * 60_000L)
                    showReminder = true
                }
                LaunchedEffect(backStackEntry) {
                    val route = backStackEntry?.destination?.route
                    if (route == "boot" && !app.settings.qqGroupPrompted) {
                        showQqGroup = true
                    }
                }

                if (showReminder) {
                    AlertDialog(
                        onDismissRequest = { showReminder = false },
                        title = { Text("休息一下") },
                        text = { Text("你已经连续使用了一段时间。起身活动一下，喝口水吧。") },
                        confirmButton = {
                            TextButton(onClick = { showReminder = false }) { Text("继续使用") }
                        },
                    )
                }
                if (showQqGroup) {
                    QqGroupDialog(
                        onDismiss = {
                            app.settings.qqGroupPrompted = true
                            showQqGroup = false
                        },
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    NavHost(navController = nav, startDestination = start) {
                        composable("age") {
                            AgeGateScreen(
                                onConfirm = {
                                    val next = if (app.settings.apiKey.isBlank()) "setup" else "boot"
                                    nav.navigate(next) {
                                        popUpTo("age") { inclusive = true }
                                    }
                                },
                            )
                        }
                        composable("setup") {
                            ApiKeyScreen(
                                onDone = {
                                    nav.navigate("boot") {
                                        popUpTo("setup") { inclusive = true }
                                    }
                                },
                            )
                        }
                        composable("boot") {
                            BootScreen(
                                onReady = { sessionId ->
                                    nav.navigate("chat/$sessionId") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                                onFirstRun = {
                                    nav.navigate("first") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                            )
                        }
                        composable("first") {
                            FirstRunScreen(
                                onCreated = { sessionId ->
                                    nav.navigate("chat/$sessionId") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                            )
                        }
                        composable(
                            route = "chat/{sessionId}",
                            arguments = listOf(navArgument("sessionId") { type = NavType.LongType }),
                        ) { entry ->
                            val sessionId = entry.arguments?.getLong("sessionId") ?: return@composable
                            ChatScreen(
                                sessionId = sessionId,
                                onOpenSession = { nextId ->
                                    nav.navigate("chat/$nextId") {
                                        popUpTo("chat/{sessionId}") { inclusive = true }
                                        launchSingleTop = true
                                    }
                                },
                                onOpenMemories = { characterId ->
                                    nav.navigate("memories/$characterId")
                                },
                                onOpenSettings = { nav.navigate("settings") },
                                onFirstRun = {
                                    nav.navigate("first") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                            )
                        }
                        composable(
                            route = "memories/{characterId}",
                            arguments = listOf(navArgument("characterId") { type = NavType.LongType }),
                        ) { entry ->
                            val characterId = entry.arguments?.getLong("characterId") ?: return@composable
                            MemoryScreen(
                                characterId = characterId,
                                onBack = { nav.popBackStack() },
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                onBack = { nav.popBackStack() },
                                onOpenUsage = { nav.navigate("usage") },
                                onOpenSession = { importedId ->
                                    nav.navigate("chat/$importedId") {
                                        popUpTo("chat/{sessionId}") { inclusive = true }
                                    }
                                },
                            )
                        }
                        composable("usage") {
                            UsageScreen(onBack = { nav.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BootScreen(
    onReady: (Long) -> Unit,
    onFirstRun: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as DssrkhApp
    LaunchedEffect(Unit) {
        val characters = app.database.characterDao().observeAll().first()
        if (characters.isEmpty()) {
            onFirstRun()
            return@LaunchedEffect
        }
        val latest = app.database.sessionDao().latestOverall()
        if (latest != null) {
            onReady(latest.id)
            return@LaunchedEffect
        }
        val created = SessionFactory.create(app.database, characters.first())
        onReady(created)
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

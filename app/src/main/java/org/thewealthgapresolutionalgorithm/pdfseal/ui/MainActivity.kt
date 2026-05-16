package org.thewealthgapresolutionalgorithm.pdfseal.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import org.thewealthgapresolutionalgorithm.pdfseal.engine.PdfEngine
import org.thewealthgapresolutionalgorithm.pdfseal.engine.io.AppPrefs
import org.thewealthgapresolutionalgorithm.pdfseal.ui.screens.AboutScreen
import org.thewealthgapresolutionalgorithm.pdfseal.ui.screens.FirstRunScreen
import org.thewealthgapresolutionalgorithm.pdfseal.ui.screens.HomeScreen
import org.thewealthgapresolutionalgorithm.pdfseal.ui.screens.ViewerScreen
import org.thewealthgapresolutionalgorithm.pdfseal.ui.viewer.PdfViewerState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val engine = remember { PdfEngine(applicationContext) }
                val viewerState = remember { PdfViewerState(engine) }
                val appPrefs = remember { AppPrefs(applicationContext) }
                val nav = rememberNavController()
                val scope = rememberCoroutineScope()

                // null = not loaded yet (don't flash either screen), then the
                // stored boolean. The first-launch limits screen blocks the
                // rest of the app until the user taps "I understand".
                val acknowledged by appPrefs.limitsAcknowledged
                    .collectAsState(initial = null)

                when (acknowledged) {
                    null -> Unit
                    false -> FirstRunScreen(
                        onAcknowledge = {
                            scope.launch { appPrefs.setLimitsAcknowledged() }
                        },
                    )
                    true -> NavHost(
                        navController = nav,
                        startDestination = "home",
                    ) {
                        composable("home") {
                            HomeScreen(
                                engine = engine,
                                onOpen = { uri ->
                                    scope.launch { viewerState.open(uri) }
                                    nav.navigate("viewer")
                                },
                                onAbout = { nav.navigate("about") },
                            )
                        }
                        composable("viewer") {
                            ViewerScreen(
                                state = viewerState,
                                onBack = { nav.popBackStack() },
                            )
                        }
                        composable("about") { AboutScreen() }
                    }
                }
            }
        }
    }
}

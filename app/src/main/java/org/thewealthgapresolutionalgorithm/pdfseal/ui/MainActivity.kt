package org.thewealthgapresolutionalgorithm.pdfseal.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import org.thewealthgapresolutionalgorithm.pdfseal.ui.theme.PdfSealTheme
import org.thewealthgapresolutionalgorithm.pdfseal.ui.viewer.PdfViewerState

class MainActivity : ComponentActivity() {

    // PDF passed via ACTION_VIEW ("Open with PDFSeal" from a file app). Held
    // in Compose state so a cold OR warm launch routes straight to the viewer
    // instead of dropping the user on Home (the intent-filter is declared in
    // the manifest, so ignoring the intent was a real bug).
    private var pendingViewUri by mutableStateOf<Uri?>(null)

    private fun viewUriFrom(intent: Intent?): Uri? =
        if (intent?.action == Intent.ACTION_VIEW) intent.data else null

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewUriFrom(intent)?.let { pendingViewUri = it }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingViewUri = viewUriFrom(intent)
        setContent {
            PdfSealTheme {
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
                    true -> {
                        LaunchedEffect(pendingViewUri) {
                            val u = pendingViewUri ?: return@LaunchedEffect
                            pendingViewUri = null
                            viewerState.open(u)
                            nav.navigate("viewer") {
                                launchSingleTop = true
                            }
                        }
                        NavHost(
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
}

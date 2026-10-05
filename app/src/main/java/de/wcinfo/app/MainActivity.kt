package de.wcinfo.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import de.wcinfo.app.data.location.LocationTracker
import de.wcinfo.app.data.model.ToiletFilterSettings
import de.wcinfo.app.data.repository.ToiletRepository
import de.wcinfo.app.ui.navigation.AppNavHost
import de.wcinfo.app.ui.navigation.Screen
import de.wcinfo.app.ui.theme.BackgroundApp
import de.wcinfo.app.ui.theme.WCinfoTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val repository = ToiletRepository()
    private val locationTracker by lazy { LocationTracker(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WCinfoTheme {
                val navController = rememberNavController()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    AppNavHost(navController = navController)
                }

                LaunchedEffect(intent) {
                    handleIntent(intent) { route ->
                        navController.navigate(route)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun handleIntent(intent: Intent?, onNavigate: (String) -> Unit) {
        val uri: Uri = intent?.data ?: return

        // Deep Link 1: wcinfo://urgent-navigate
        if (uri.scheme == "wcinfo" && (uri.host == "urgent-navigate" || uri.path?.contains("urgent-navigate") == true)) {
            val filterSettings = ToiletFilterSettings.fromUri(uri)
            lifecycleScope.launch {
                val location = locationTracker.getCurrentLocation()
                if (location != null) {
                    val result = repository.findEmergencyToilet(location.latitude, location.longitude, filterSettings)
                    result.onSuccess { (toilet, isFallback) ->
                        if (isFallback) {
                            Toast.makeText(this@MainActivity, "Nächste geöffnete (nicht-öffentliche) Toilette gefunden.", Toast.LENGTH_LONG).show()
                        }
                        onNavigate(Screen.Compass.createRoute(toilet.id))
                    }.onFailure {
                        Toast.makeText(this@MainActivity, "Keine geöffnete Toilette im Umkreis gefunden.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Standort nicht verfügbar.", Toast.LENGTH_SHORT).show()
                }
            }
            return
        }

        // Deep Link 2: https://wc-info.org/Toilets/{placeSlug}---{placeId}/{toiletSlug}---{toiletId}
        if (uri.scheme == "https" && uri.host == "wc-info.org" && uri.path?.startsWith("/Toilets") == true) {
            val segments = uri.pathSegments
            val lastSegment = segments.lastOrNull()
            if (lastSegment != null && lastSegment.contains("---")) {
                val parts = lastSegment.split("---")
                val toiletId = parts.lastOrNull()?.toIntOrNull()
                if (toiletId != null) {
                    onNavigate(Screen.Detail.createRoute(toiletId))
                }
            }
        }
    }
}

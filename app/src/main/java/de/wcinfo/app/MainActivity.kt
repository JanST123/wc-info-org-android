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
import de.wcinfo.app.util.DeepLinkResolver
import de.wcinfo.app.util.ResolvedDeepLink
import de.wcinfo.app.data.location.LocationTracker
import de.wcinfo.app.data.model.ToiletFilterSettings
import de.wcinfo.app.data.repository.ToiletRepository
import de.wcinfo.app.ui.navigation.AppNavHost
import de.wcinfo.app.ui.navigation.Screen
import androidx.glance.appwidget.updateAll
import de.wcinfo.app.ui.theme.BackgroundApp
import de.wcinfo.app.ui.theme.WCinfoTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val repository = ToiletRepository()
    private val locationTracker by lazy { LocationTracker(this) }

    private var onNavigateCallback: ((String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                de.wcinfo.app.widget.UrgentToiletWidget().updateAll(applicationContext)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        setContent {
            WCinfoTheme {
                val navController = rememberNavController()
                onNavigateCallback = { route ->
                    try {
                        if (route == "home" || route == Screen.Home.route) {
                            navController.popBackStack(Screen.Home.route, false)
                        } else {
                            navController.navigate(route)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    AppNavHost(navController = navController)
                }

                LaunchedEffect(Unit) {
                    handleIntent(intent)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val uri: Uri = intent?.data ?: return

        lifecycleScope.launch {
            val resolved = DeepLinkResolver.resolveUri(applicationContext, uri) ?: return@launch
            when (resolved) {
                is ResolvedDeepLink.Home -> {
                    onNavigateCallback?.invoke(Screen.Home.route)
                }
                is ResolvedDeepLink.Results -> {
                    val resultsRoute = Screen.Results.createRoute(
                        lat = resolved.lat,
                        lon = resolved.lon,
                        name = resolved.name,
                        toiletId = resolved.toiletId
                    )
                    onNavigateCallback?.invoke(resultsRoute)
                    if (resolved.openDetail && resolved.toiletId != null) {
                        onNavigateCallback?.invoke(Screen.Detail.createRoute(resolved.toiletId))
                    }
                }
                is ResolvedDeepLink.Detail -> {
                    onNavigateCallback?.invoke(Screen.Detail.createRoute(resolved.toiletId))
                }
                is ResolvedDeepLink.DirectRoute -> {
                    onNavigateCallback?.invoke(resolved.route)
                }
                is ResolvedDeepLink.PinWidget -> {
                    val appWidgetManager = getSystemService(android.appwidget.AppWidgetManager::class.java)
                    val myProvider = android.content.ComponentName(this@MainActivity, de.wcinfo.app.widget.UrgentToiletWidgetReceiver::class.java)
                    if (appWidgetManager.isRequestPinAppWidgetSupported) {
                        appWidgetManager.requestPinAppWidget(myProvider, null, null)
                    }
                }
                is ResolvedDeepLink.UrgentNavigate -> {
                    val filterSettings = ToiletFilterSettings.fromUri(resolved.uri)
                    val location = locationTracker.getCurrentLocation()
                    if (location != null) {
                        val result = repository.findEmergencyToilet(location.latitude, location.longitude, filterSettings)
                        result.onSuccess { (toilet, isFallback) ->
                            if (isFallback) {
                                Toast.makeText(this@MainActivity, "Nächste geöffnete (nicht-öffentliche) Toilette gefunden.", Toast.LENGTH_LONG).show()
                            }
                            onNavigateCallback?.invoke(Screen.Compass.createRoute(toilet.id))
                        }.onFailure {
                            Toast.makeText(this@MainActivity, "Keine geöffnete Toilette im Umkreis gefunden.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "Standort nicht verfügbar.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}

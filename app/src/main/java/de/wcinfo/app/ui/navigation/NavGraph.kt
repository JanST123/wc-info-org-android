package de.wcinfo.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import de.wcinfo.app.ui.compass.CompassScreen
import de.wcinfo.app.ui.create.CreateToiletScreen
import de.wcinfo.app.ui.detail.DetailScreen
import de.wcinfo.app.ui.feedback.FeedbackScreen
import de.wcinfo.app.ui.home.HomeScreen
import de.wcinfo.app.ui.results.ResultsScreen
import de.wcinfo.app.ui.update.UpdateToiletScreen
import java.net.URLDecoder

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToResults = { lat, lon, name ->
                    navController.navigate(Screen.Results.createRoute(lat, lon, name))
                }
            )
        }

        composable(
            route = Screen.Results.route,
            arguments = listOf(
                navArgument("lat") { type = NavType.StringType; defaultValue = "52.5200" },
                navArgument("lon") { type = NavType.StringType; defaultValue = "13.4050" },
                navArgument("name") { type = NavType.StringType; defaultValue = "Berlin" },
                navArgument("toiletId") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull() ?: 52.5200
            val lon = backStackEntry.arguments?.getString("lon")?.toDoubleOrNull() ?: 13.4050
            val encodedName = backStackEntry.arguments?.getString("name") ?: "Berlin"
            val name = URLDecoder.decode(encodedName, "UTF-8")
            val toiletId = backStackEntry.arguments?.getString("toiletId")?.toIntOrNull()

            ResultsScreen(
                latitude = lat,
                longitude = lon,
                locationName = name,
                initialToiletId = toiletId,
                onBackClick = { navController.popBackStack() },
                onNavigateToDetail = { id -> navController.navigate(Screen.Detail.createRoute(id)) },
                onNavigateToCompass = { id -> navController.navigate(Screen.Compass.createRoute(id)) },
                onNavigateToCreate = { initialLat, initialLon, resLat, resLon ->
                    navController.navigate(Screen.Create.createRoute(initialLat, initialLon, resLat, resLon))
                }
            )
        }

        composable(
            route = Screen.Detail.route,
            arguments = listOf(navArgument("toiletId") { type = NavType.IntType })
        ) { backStackEntry ->
            val toiletId = backStackEntry.arguments?.getInt("toiletId") ?: 0
            DetailScreen(
                toiletId = toiletId,
                onBackClick = { navController.popBackStack() },
                onNavigateToCompass = { id -> navController.navigate(Screen.Compass.createRoute(id)) },
                onNavigateToUpdate = { id -> navController.navigate(Screen.Update.createRoute(id)) },
                onNavigateToFeedback = { id -> navController.navigate(Screen.Feedback.createRoute(id)) }
            )
        }

        composable(
            route = Screen.Compass.route,
            arguments = listOf(navArgument("toiletId") { type = NavType.IntType })
        ) { backStackEntry ->
            val toiletId = backStackEntry.arguments?.getInt("toiletId") ?: 0
            CompassScreen(
                toiletId = toiletId,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Create.route,
            arguments = listOf(
                navArgument("initialLat") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("initialLon") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("resultLat") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("resultLon") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val initialLat = backStackEntry.arguments?.getString("initialLat")?.toDoubleOrNull()
            val initialLon = backStackEntry.arguments?.getString("initialLon")?.toDoubleOrNull()
            val resultLat = backStackEntry.arguments?.getString("resultLat")?.toDoubleOrNull()
            val resultLon = backStackEntry.arguments?.getString("resultLon")?.toDoubleOrNull()
            CreateToiletScreen(
                initialLat = initialLat,
                initialLon = initialLon,
                resultLat = resultLat,
                resultLon = resultLon,
                onClose = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Update.route,
            arguments = listOf(navArgument("toiletId") { type = NavType.IntType })
        ) { backStackEntry ->
            val toiletId = backStackEntry.arguments?.getInt("toiletId") ?: 0
            UpdateToiletScreen(
                toiletId = toiletId,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Feedback.route,
            arguments = listOf(navArgument("toiletId") { type = NavType.IntType })
        ) { backStackEntry ->
            val toiletId = backStackEntry.arguments?.getInt("toiletId") ?: 0
            FeedbackScreen(
                toiletId = toiletId,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

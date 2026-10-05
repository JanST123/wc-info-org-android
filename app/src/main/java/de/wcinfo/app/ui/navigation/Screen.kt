package de.wcinfo.app.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")

    object Results : Screen("results?lat={lat}&lon={lon}&name={name}&toiletId={toiletId}") {
        fun createRoute(lat: Double, lon: Double, name: String, toiletId: Int? = null): String {
            val encodedName = java.net.URLEncoder.encode(name, "UTF-8")
            return "results?lat=$lat&lon=$lon&name=$encodedName" +
                    if (toiletId != null) "&toiletId=$toiletId" else ""
        }
    }

    object Detail : Screen("detail/{toiletId}") {
        fun createRoute(toiletId: Int): String = "detail/$toiletId"
    }

    object Compass : Screen("compass/{toiletId}") {
        fun createRoute(toiletId: Int): String = "compass/$toiletId"
    }

    object Create : Screen("create?initialLat={initialLat}&initialLon={initialLon}&resultLat={resultLat}&resultLon={resultLon}") {
        fun createRoute(
            initialLat: Double? = null,
            initialLon: Double? = null,
            resultLat: Double? = null,
            resultLon: Double? = null
        ): String {
            val params = mutableListOf<String>()
            if (initialLat != null) params.add("initialLat=$initialLat")
            if (initialLon != null) params.add("initialLon=$initialLon")
            if (resultLat != null) params.add("resultLat=$resultLat")
            if (resultLon != null) params.add("resultLon=$resultLon")
            return if (params.isNotEmpty()) "create?" + params.joinToString("&") else "create"
        }
    }

    object Update : Screen("update/{toiletId}") {
        fun createRoute(toiletId: Int): String = "update/$toiletId"
    }

    object Feedback : Screen("feedback/{toiletId}") {
        fun createRoute(toiletId: Int): String = "feedback/$toiletId"
    }
}

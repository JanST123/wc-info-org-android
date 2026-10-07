package de.wcinfo.app.util

import android.content.Context
import android.location.Geocoder
import android.net.Uri
import android.util.Log
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import de.wcinfo.app.BuildConfig
import de.wcinfo.app.data.repository.ToiletRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.net.URLDecoder
import java.util.Locale
import kotlin.coroutines.resume

sealed class ResolvedDeepLink {
    object Home : ResolvedDeepLink()
    data class Results(
        val lat: Double,
        val lon: Double,
        val name: String,
        val toiletId: Int? = null,
        val openDetail: Boolean = false
    ) : ResolvedDeepLink()
    data class Detail(val toiletId: Int) : ResolvedDeepLink()
    data class DirectRoute(val route: String) : ResolvedDeepLink()
    data class UrgentNavigate(val uri: Uri) : ResolvedDeepLink()
    object PinWidget : ResolvedDeepLink()
}

object DeepLinkResolver {

    private const val TAG = "DeepLinkResolver"

    private fun getPlacesClient(context: Context): PlacesClient? {
        val apiKey = BuildConfig.GOOGLE_API_KEY
        if (apiKey.isBlank()) return null

        if (!Places.isInitialized()) {
            try {
                Places.initialize(context.applicationContext, apiKey)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Google Places: ${e.message}", e)
                return null
            }
        }
        return try {
            Places.createClient(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create PlacesClient: ${e.message}", e)
            null
        }
    }

    suspend fun resolveUri(context: Context, uri: Uri): ResolvedDeepLink? = withContext(Dispatchers.IO) {
        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: ""
        val host = uri.host?.lowercase(Locale.ROOT) ?: ""

        // 1. Custom scheme: wcinfo://
        if (scheme == "wcinfo") {
            if (host == "route") {
                val path = uri.getQueryParameter("path")
                return@withContext if (path != null) ResolvedDeepLink.DirectRoute(path) else null
            }
            if (host == "pin-widget") {
                return@withContext ResolvedDeepLink.PinWidget
            }
            if (host == "urgent-navigate" || uri.path?.contains("urgent-navigate") == true) {
                return@withContext ResolvedDeepLink.UrgentNavigate(uri)
            }
            // Fallback for wcinfo://home or custom route path
            val path = uri.path?.trimStart('/')
            if (!path.isNullOrBlank()) {
                return@withContext ResolvedDeepLink.DirectRoute(path)
            }
            return@withContext ResolvedDeepLink.Home
        }

        // 2. Web deep links: https://wc-info.org / http://wc-info.org / www.wc-info.org
        val isWcInfoHost = host == "wc-info.org" || host == "www.wc-info.org" || host.endsWith(".wc-info.org")
        if (!isWcInfoHost && scheme != "http" && scheme != "https") {
            return@withContext null
        }

        val allSegments = uri.pathSegments.filter { it.isNotBlank() }

        // Remove optional "Toilets" / "toilets" prefix if present
        val segments = if (allSegments.firstOrNull()?.equals("toilets", ignoreCase = true) == true) {
            allSegments.drop(1)
        } else {
            allSegments
        }

        // Case 1: https://wc-info.org -> HomeScreen
        if (segments.isEmpty()) {
            return@withContext ResolvedDeepLink.Home
        }

        val firstSegment = segments[0]

        // Case 2: https://wc-info.org/xyz (Single segment without "---")
        if (segments.size == 1 && !firstSegment.contains("---")) {
            val query = decodeSegmentName(firstSegment)
            if (query.isBlank() || query.equals("home", ignoreCase = true)) {
                return@withContext ResolvedDeepLink.Home
            }
            val resolved = searchFirstPlace(context, query)
            return@withContext if (resolved != null) {
                ResolvedDeepLink.Results(
                    lat = resolved.first,
                    lon = resolved.second,
                    name = resolved.third
                )
            } else {
                ResolvedDeepLink.Home
            }
        }

        // Case 3 & 4: Segments with placeId and optional toiletId
        // First segment: xyz---<place-id>
        val (placeSlugName, placeId) = parseSlugWithId(firstSegment)

        var toiletId: Int? = null
        if (segments.size >= 2) {
            val secondSegment = segments[1]
            val (_, parsedToiletIdStr) = parseSlugWithId(secondSegment)
            toiletId = parsedToiletIdStr?.toIntOrNull()
        }

        // Resolve place coordinates
        var placeLocation: Triple<Double, Double, String>? = null
        if (!placeId.isNullOrBlank()) {
            placeLocation = fetchPlaceDetails(context, placeId, fallbackName = placeSlugName)
        }

        if (placeLocation == null && !placeSlugName.isNullOrBlank()) {
            placeLocation = searchFirstPlace(context, placeSlugName)
        }

        if (placeLocation != null) {
            return@withContext ResolvedDeepLink.Results(
                lat = placeLocation.first,
                lon = placeLocation.second,
                name = placeLocation.third,
                toiletId = toiletId,
                openDetail = (toiletId != null)
            )
        }

        // If place resolution failed but we have a valid toiletId, fetch toilet info directly
        if (toiletId != null) {
            val repo = ToiletRepository()
            val toiletResult = repo.getToilet(toiletId)
            val toilet = toiletResult.getOrNull()
            if (toilet != null) {
                return@withContext ResolvedDeepLink.Results(
                    lat = toilet.lat,
                    lon = toilet.lon,
                    name = toilet.name,
                    toiletId = toiletId,
                    openDetail = true
                )
            }
            return@withContext ResolvedDeepLink.Detail(toiletId)
        }

        ResolvedDeepLink.Home
    }

    private fun decodeSegmentName(segment: String): String {
        return try {
            URLDecoder.decode(segment, "UTF-8").replace("-", " ").replace("_", " ").trim()
        } catch (e: Exception) {
            segment.replace("-", " ").replace("_", " ").trim()
        }
    }

    private fun parseSlugWithId(segment: String): Pair<String?, String?> {
        val decoded = try {
            URLDecoder.decode(segment, "UTF-8")
        } catch (e: Exception) {
            segment
        }

        return if (decoded.contains("---")) {
            val parts = decoded.split("---")
            val id = parts.last().trim().ifBlank { null }
            val nameSlug = parts.dropLast(1).joinToString("---")
            val name = nameSlug.replace("-", " ").replace("_", " ").trim().ifBlank { null }
            Pair(name, id)
        } else {
            Pair(decodeSegmentName(decoded).ifBlank { null }, decoded.trim().ifBlank { null })
        }
    }

    private suspend fun fetchPlaceDetails(
        context: Context,
        placeId: String,
        fallbackName: String?
    ): Triple<Double, Double, String>? = suspendCancellableCoroutine { cont ->
        val client = getPlacesClient(context)
        if (client == null) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }

        val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)
        val request = FetchPlaceRequest.newInstance(placeId, fields)

        client.fetchPlace(request)
            .addOnSuccessListener { response ->
                val place = response.place
                val latLng = place.latLng
                if (latLng != null) {
                    val name = place.name?.takeIf { it.isNotBlank() } ?: fallbackName ?: "Ort"
                    cont.resume(Triple(latLng.latitude, latLng.longitude, name))
                } else {
                    cont.resume(null)
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "fetchPlace failed for $placeId: ${e.message}")
                cont.resume(null)
            }
    }

    private suspend fun searchFirstPlace(
        context: Context,
        query: String
    ): Triple<Double, Double, String>? {
        // Step 1: Try Google Places Autocomplete first
        val client = getPlacesClient(context)
        if (client != null) {
            val placeFromApi = suspendCancellableCoroutine<Triple<Double, Double, String>?> { cont ->
                val request = FindAutocompletePredictionsRequest.builder()
                    .setQuery(query)
                    .build()

                client.findAutocompletePredictions(request)
                    .addOnSuccessListener { response ->
                        val firstMatch = response.autocompletePredictions.firstOrNull()
                        if (firstMatch != null) {
                            val placeName = firstMatch.getPrimaryText(null).toString()
                            val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)
                            val fetchReq = FetchPlaceRequest.newInstance(firstMatch.placeId, fields)
                            client.fetchPlace(fetchReq)
                                .addOnSuccessListener { fetchResp ->
                                    val latLng = fetchResp.place.latLng
                                    if (latLng != null) {
                                        val name = fetchResp.place.name?.takeIf { it.isNotBlank() } ?: placeName
                                        cont.resume(Triple(latLng.latitude, latLng.longitude, name))
                                    } else {
                                        cont.resume(null)
                                    }
                                }
                                .addOnFailureListener {
                                    cont.resume(null)
                                }
                        } else {
                            cont.resume(null)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "findAutocompletePredictions failed for $query: ${e.message}")
                        cont.resume(null)
                    }
            }

            if (placeFromApi != null) {
                return placeFromApi
            }
        }

        // Step 2: Fallback to Android Geocoder
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val results = geocoder.getFromLocationName(query, 1)
                val first = results?.firstOrNull()
                if (first != null) {
                    val name = first.locality ?: first.featureName ?: query
                    Triple(first.latitude, first.longitude, name)
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Geocoder failed for $query: ${e.message}", e)
                null
            }
        }
    }
}

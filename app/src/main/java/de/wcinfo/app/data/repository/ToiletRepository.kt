package de.wcinfo.app.data.repository

import android.location.Location
import de.wcinfo.app.data.api.ApiClient
import de.wcinfo.app.data.api.WCInfoApiService
import de.wcinfo.app.data.model.AddToiletPayload
import de.wcinfo.app.data.model.AddToiletResponse
import de.wcinfo.app.data.model.PlaceResource
import de.wcinfo.app.data.model.SendToiletFeedbackRequest
import de.wcinfo.app.data.model.SendToiletFeedbackResponse
import de.wcinfo.app.data.model.Toilet
import de.wcinfo.app.data.model.ToiletFilterSettings
import de.wcinfo.app.data.model.UpdateToiletPayload
import de.wcinfo.app.data.model.UpdateToiletResponse
import de.wcinfo.app.data.model.UploadPhotoResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class ToiletRepository(
    private val api: WCInfoApiService = ApiClient.apiService
) {

    suspend fun getToilet(id: Int): Result<Toilet> = runCatching {
        api.getToilet(id)
    }

    suspend fun getToiletsNearby(
        latitude: Double,
        longitude: Double,
        distanceKm: Int = 10,
        filters: ToiletFilterSettings? = null
    ): Result<List<Toilet>> = runCatching {
        val filterQuery = filters?.toApiFilterQuery()
        api.getToiletsNearby(latitude, longitude, distanceKm, filterQuery)
    }

    suspend fun getToiletsInBounds(
        south: Double,
        west: Double,
        north: Double,
        east: Double,
        filters: ToiletFilterSettings? = null
    ): Result<List<Toilet>> = runCatching {
        val filterQuery = filters?.toApiFilterQuery()
        api.getToiletsInBounds(south, west, north, east, filterQuery)
    }

    suspend fun fetchNearestPlaces(
        latitude: Double,
        longitude: Double,
        radius: Double? = 200.0,
        limit: Int? = 10
    ): Result<List<PlaceResource>> = runCatching {
        val element = api.getNearestPlaces(latitude, longitude, radius, limit)
        val json = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
        when (element) {
            is kotlinx.serialization.json.JsonObject -> {
                if (element.containsKey("places")) {
                    json.decodeFromJsonElement(de.wcinfo.app.data.model.NearestPlacesResponse.serializer(), element).places
                } else {
                    emptyList()
                }
            }
            is kotlinx.serialization.json.JsonArray -> {
                json.decodeFromJsonElement(
                    kotlinx.serialization.builtins.ListSerializer(PlaceResource.serializer()),
                    element
                )
            }
            else -> emptyList()
        }
    }

    suspend fun addToilet(payload: AddToiletPayload): Result<AddToiletResponse> = runCatching {
        api.addToilet(payload)
    }

    suspend fun updateToilet(id: Int, payload: UpdateToiletPayload): Result<UpdateToiletResponse> = runCatching {
        api.updateToilet(id, payload)
    }

    suspend fun sendFeedback(id: Int, subject: String, message: String?): Result<SendToiletFeedbackResponse> = runCatching {
        api.sendFeedback(id, SendToiletFeedbackRequest(subject, message))
    }

    suspend fun uploadPhoto(toiletId: Int?, photoFile: File): Result<UploadPhotoResponse> = runCatching {
        val requestFile = photoFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", photoFile.name, requestFile)
        val idBody = toiletId?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
        api.uploadPhoto(body, idBody)
    }

    suspend fun deletePhoto(toiletId: Int, filename: String): Result<Unit> = runCatching {
        val response = api.deletePhoto(toiletId, filename)
        if (!response.isSuccessful) {
            error("Fehler beim Löschen des Fotos (${response.code()})")
        }
    }

    /**
     * Emergency Navigation Algorithm with Smart Fallback:
     * 1. Query nearby toilets with initial filters (strictly open and public).
     * 2. Sort by distance ascending.
     * 3. If closest public toilet is <= 500m (or maxPublicDistanceMeters), choose it.
     * 4. Else, search for closer opened non-public toilets (e.g. café, station).
     * 5. If a non-public toilet is closer, return it with fallbackUsed = true.
     */
    suspend fun findEmergencyToilet(
        userLat: Double,
        userLon: Double,
        settings: ToiletFilterSettings = ToiletFilterSettings()
    ): Result<Pair<Toilet, Boolean>> = runCatching {
        val publicSettings = settings.copy(showClosed = false, showNonPublic = false)
        val publicList = api.getToiletsNearby(
            userLat, userLon, 10, publicSettings.toApiFilterQuery()
        ).sortedBy { distBetween(userLat, userLon, it.lat, it.lon) }

        val closestPublic = publicList.firstOrNull {
            !it.placeOpeningHours.isNullOrEmpty() || it.accessibleOutsideOpeningTimes
        }

        val publicDistanceMeters = if (closestPublic != null) {
            distBetween(userLat, userLon, closestPublic.lat, closestPublic.lon)
        } else {
            Float.MAX_VALUE
        }

        if (settings.allowNonPublicFallback && publicDistanceMeters > settings.maxPublicDistanceMeters) {
            val nonPublicSettings = settings.copy(showClosed = false, showNonPublic = true)
            val fallbackList = api.getToiletsNearby(
                userLat, userLon, 10, nonPublicSettings.toApiFilterQuery()
            ).sortedBy { distBetween(userLat, userLon, it.lat, it.lon) }

            val closestFallback = fallbackList.firstOrNull {
                !it.placeOpeningHours.isNullOrEmpty() || it.accessibleOutsideOpeningTimes
            }

            if (closestFallback != null) {
                val fallbackDist = distBetween(userLat, userLon, closestFallback.lat, closestFallback.lon)
                if (fallbackDist < publicDistanceMeters) {
                    return@runCatching Pair(closestFallback, true)
                }
            }
        }

        if (closestPublic != null) {
            Pair(closestPublic, false)
        } else if (publicList.isNotEmpty()) {
            Pair(publicList.first(), false)
        } else {
            error("No toilets found within 10 km")
        }
    }

    private fun distBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }
}

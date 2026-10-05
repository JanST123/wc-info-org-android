package de.wcinfo.app.data.api

import de.wcinfo.app.data.model.AddToiletPayload
import de.wcinfo.app.data.model.AddToiletResponse
import de.wcinfo.app.data.model.PlaceResource
import de.wcinfo.app.data.model.SendToiletFeedbackRequest
import de.wcinfo.app.data.model.SendToiletFeedbackResponse
import de.wcinfo.app.data.model.Toilet
import de.wcinfo.app.data.model.UpdateToiletPayload
import de.wcinfo.app.data.model.UpdateToiletResponse
import de.wcinfo.app.data.model.UploadPhotoResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface WCInfoApiService {

    @GET("toilet/{id}")
    suspend fun getToilet(
        @Path("id") id: Int
    ): Toilet

    @GET("toilets/nearby/{latitude}/{longitude}")
    suspend fun getToiletsNearby(
        @Path("latitude") latitude: Double,
        @Path("longitude") longitude: Double,
        @Query("distance") distanceKm: Int = 10,
        @Query("filter") filter: String? = null
    ): List<Toilet>

    @GET("toilets/bounds/{south}/{west}/{north}/{east}")
    suspend fun getToiletsInBounds(
        @Path("south") south: Double,
        @Path("west") west: Double,
        @Path("north") north: Double,
        @Path("east") east: Double,
        @Query("filter") filter: String? = null
    ): List<Toilet>

    @GET("places/nearest/{latitude}/{longitude}")
    suspend fun getNearestPlaces(
        @Path("latitude") latitude: Double,
        @Path("longitude") longitude: Double,
        @Query("radius") radius: Double? = null,
        @Query("limit") limit: Int? = null
    ): kotlinx.serialization.json.JsonElement

    @POST("toilet/add")
    suspend fun addToilet(
        @Body payload: AddToiletPayload
    ): AddToiletResponse

    @PATCH("toilet/{id}/update")
    suspend fun updateToilet(
        @Path("id") id: Int,
        @Body payload: UpdateToiletPayload
    ): UpdateToiletResponse

    @POST("toilet/feedback/{id}")
    suspend fun sendFeedback(
        @Path("id") id: Int,
        @Body request: SendToiletFeedbackRequest
    ): SendToiletFeedbackResponse

    @Multipart
    @POST("upload")
    suspend fun uploadPhoto(
        @Part file: MultipartBody.Part,
        @Part("toilet_id") toiletId: RequestBody?,
        @Part("exif") exifJson: RequestBody? = null
    ): UploadPhotoResponse

    @DELETE("deletePhoto/{toiletId}/{filename}")
    suspend fun deletePhoto(
        @Path("toiletId") toiletId: Int,
        @Path("filename") filename: String,
        @Query("soft") soft: Int? = null
    ): Response<ResponseBody>

    @GET("health")
    suspend fun checkHealth(): Response<ResponseBody>
}

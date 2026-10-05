package de.wcinfo.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AddToiletPayload(
    val name: String? = null,
    val owner: String? = null,
    val lat: Double,
    val lon: Double,
    @SerialName("place_id")
    val placeId: String? = null,
    @SerialName("is_unisex")
    val isUnisex: Boolean? = null,
    @SerialName("is_gender_separated")
    val isGenderSeparated: Boolean? = null,
    @SerialName("has_wheelchair_access")
    val hasWheelchairAccess: Boolean? = null,
    @SerialName("has_changing_table")
    val hasChangingTable: Boolean? = null,
    @SerialName("accessible_outside_opening_times")
    val accessibleOutsideOpeningTimes: Boolean? = null,
    @SerialName("public_accessible")
    val publicAccessible: Boolean? = null,
    @SerialName("place_opening_hours")
    val placeOpeningHours: List<GooglePlacesPeriod>? = null,
    val address: String? = null,
    val website: String? = null,
    val comment: String? = null,
    @SerialName("euro_key")
    val euroKey: String? = null,
    @SerialName("storage_space")
    val storageSpace: String? = null,
    val status: String? = "active"
)

@Serializable
data class AddToiletResponse(
    val success: Boolean = true,
    val id: Int
)

@Serializable
data class UpdateToiletPayload(
    val name: String? = null,
    val owner: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    @SerialName("place_id")
    val placeId: String? = null,
    @SerialName("is_unisex")
    val isUnisex: Boolean? = null,
    @SerialName("is_gender_separated")
    val isGenderSeparated: Boolean? = null,
    @SerialName("has_wheelchair_access")
    val hasWheelchairAccess: Boolean? = null,
    @SerialName("has_changing_table")
    val hasChangingTable: Boolean? = null,
    @SerialName("accessible_outside_opening_times")
    val accessibleOutsideOpeningTimes: Boolean? = null,
    @SerialName("public_accessible")
    val publicAccessible: Boolean? = null,
    @SerialName("place_opening_hours")
    val placeOpeningHours: List<GooglePlacesPeriod>? = null,
    val address: String? = null,
    val website: String? = null,
    val comment: String? = null,
    @SerialName("euro_key")
    val euroKey: String? = null,
    @SerialName("storage_space")
    val storageSpace: String? = null,
    val status: String? = null
)

@Serializable
data class UpdateToiletResponse(
    val success: Boolean = true,
    val id: Int
)

@Serializable
data class SendToiletFeedbackRequest(
    val subject: String,
    val message: String? = null
)

@Serializable
data class SendToiletFeedbackResponse(
    val status: String = "success",
    val message: String = ""
)

@Serializable
data class DisplayNameText(
    val text: String? = null,
    val languageCode: String? = null
)

@Serializable
data class PlaceLocation(
    val latitude: Double? = null,
    val longitude: Double? = null
)

@Serializable
data class NearestPlacesResponse(
    val status: String? = null,
    val places: List<PlaceResource> = emptyList()
)

@Serializable
data class RegularOpeningHours(
    @SerialName("open_now") val openNow: Boolean? = null,
    val periods: List<GooglePlacesPeriod>? = null,
    val weekdayDescriptions: List<String>? = null
)

@Serializable
data class PlaceResource(
    val id: String? = null,
    @SerialName("place_id") val placeId: String? = null,
    val displayName: DisplayNameText? = null,
    val name: String? = null,
    val location: PlaceLocation? = null,
    val formattedAddress: String? = null,
    @SerialName("formatted_address") val formattedAddressSnake: String? = null,
    val address: String? = null,
    val websiteUri: String? = null,
    val website: String? = null,
    val regularOpeningHours: RegularOpeningHours? = null,
    @SerialName("opening_hours") val openingHours: RegularOpeningHours? = null,
    @SerialName("place_opening_hours") val placeOpeningHours: List<GooglePlacesPeriod>? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val distance: Double? = null,
    val types: List<String>? = null
) {
    val resolvedPlaceId: String?
        get() = id ?: placeId

    val resolvedName: String?
        get() = displayName?.text?.takeIf { it.isNotBlank() }
            ?: name?.takeIf { it.isNotBlank() && !it.startsWith("places/") }

    val resolvedAddress: String?
        get() = formattedAddress ?: formattedAddressSnake ?: address

    val resolvedWebsite: String?
        get() = websiteUri ?: website

    val resolvedLat: Double?
        get() = location?.latitude ?: lat

    val resolvedLon: Double?
        get() = location?.longitude ?: lon

    val resolvedOpeningHours: List<GooglePlacesPeriod>?
        get() = regularOpeningHours?.periods ?: openingHours?.periods ?: placeOpeningHours

    fun toNearbyPlaceOption(): NearbyPlaceOption? {
        val pId = resolvedPlaceId ?: return null
        val pName = resolvedName ?: return null
        return NearbyPlaceOption(id = pId, name = pName, secondaryText = resolvedAddress)
    }

    fun toPlaceDetails(): PlaceDetails? {
        val pId = resolvedPlaceId ?: return null
        val latVal = resolvedLat
        val lonVal = resolvedLon
        return PlaceDetails(
            placeId = pId,
            name = resolvedName,
            formattedAddress = resolvedAddress,
            website = resolvedWebsite,
            latitude = latVal,
            longitude = lonVal,
            openingHours = resolvedOpeningHours
        )
    }
}

data class NearbyPlaceOption(
    val id: String,
    val name: String,
    val secondaryText: String? = null
)

data class PlaceDetails(
    val placeId: String,
    val name: String?,
    val formattedAddress: String?,
    val website: String?,
    val latitude: Double?,
    val longitude: Double?,
    val openingHours: List<GooglePlacesPeriod>?
)

@Serializable
data class UploadPhotoResponse(
    val success: Boolean = true,
    @SerialName("toilet_id")
    val toiletId: Int = 0,
    val filename: String = "",
    @SerialName("image_url")
    val imageUrl: String = "",
    @SerialName("thumb_url")
    val thumbUrl: String = ""
)

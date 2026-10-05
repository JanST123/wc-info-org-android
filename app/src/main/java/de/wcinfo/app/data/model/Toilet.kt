package de.wcinfo.app.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import java.text.Normalizer
import java.util.Locale

object FlexibleBooleanSerializer : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleBoolean", PrimitiveKind.BOOLEAN)

    override fun deserialize(decoder: Decoder): Boolean {
        return if (decoder is JsonDecoder) {
            val element = decoder.decodeJsonElement()
            if (element is JsonPrimitive) {
                element.booleanOrNull ?: (element.intOrNull != null && element.intOrNull != 0) ||
                        element.content.equals("true", ignoreCase = true) ||
                        element.content.equals("yes", ignoreCase = true) ||
                        element.content == "1"
            } else {
                false
            }
        } else {
            decoder.decodeBoolean()
        }
    }

    override fun serialize(encoder: Encoder, value: Boolean) {
        encoder.encodeBoolean(value)
    }
}

object FlexibleNullableBooleanSerializer : KSerializer<Boolean?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleNullableBoolean", PrimitiveKind.BOOLEAN)

    override fun deserialize(decoder: Decoder): Boolean? {
        return if (decoder is JsonDecoder) {
            val element = decoder.decodeJsonElement()
            if (element is JsonPrimitive) {
                if (element.content.isEmpty() || element.content == "null") return null
                element.booleanOrNull ?: (if (element.intOrNull != null) element.intOrNull != 0 else null)
                ?: if (element.content.equals("true", ignoreCase = true) || element.content.equals("yes", ignoreCase = true) || element.content == "1") true
                else if (element.content.equals("false", ignoreCase = true) || element.content.equals("no", ignoreCase = true) || element.content == "0") false
                else null
            } else {
                null
            }
        } else {
            decoder.decodeBoolean()
        }
    }

    override fun serialize(encoder: Encoder, value: Boolean?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeBoolean(value)
        }
    }
}

@Serializable
data class Toilet(
    val id: Int,
    val name: String = "",
    val owner: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    @SerialName("place_id")
    val placeId: String? = null,
    val status: String = "active",
    @Serializable(with = FlexibleBooleanSerializer::class)
    @SerialName("is_qualified")
    val isQualified: Boolean = false,
    @Serializable(with = FlexibleBooleanSerializer::class)
    @SerialName("is_unisex")
    val isUnisex: Boolean = false,
    @Serializable(with = FlexibleBooleanSerializer::class)
    @SerialName("is_gender_separated")
    val isGenderSeparated: Boolean = false,
    @Serializable(with = FlexibleBooleanSerializer::class)
    @SerialName("has_wheelchair_access")
    val hasWheelchairAccess: Boolean = false,
    @Serializable(with = FlexibleBooleanSerializer::class)
    @SerialName("has_changing_table")
    val hasChangingTable: Boolean = false,
    @Serializable(with = FlexibleBooleanSerializer::class)
    @SerialName("accessible_outside_opening_times")
    val accessibleOutsideOpeningTimes: Boolean = false,
    @Serializable(with = FlexibleBooleanSerializer::class)
    @SerialName("public_accessible")
    val publicAccessible: Boolean = true,
    @Serializable(with = FlexibleBooleanSerializer::class)
    @SerialName("temporary_closed")
    val temporaryClosed: Boolean = false,
    @Serializable(with = FlexibleNullableBooleanSerializer::class)
    @SerialName("is_open")
    val isOpen: Boolean? = null,
    val source: String? = null,
    val address: String? = null,
    val website: String? = null,
    val comment: String? = null,
    @SerialName("euro_key")
    val euroKey: String? = null,
    @SerialName("storage_space")
    val storageSpace: String? = null,
    val distance: Double? = null,
    val photos: List<ToiletPhoto> = emptyList(),
    @SerialName("open_timestamp")
    val openTimestamp: String? = null,
    @SerialName("close_timestamp")
    val closeTimestamp: String? = null,
    @SerialName("place_opening_hours")
    val placeOpeningHours: List<GooglePlacesPeriod>? = null,
    val updated: String? = null
) {
    val displayName: String
        get() = name.ifBlank { owner.ifBlank { "Toilette" } }

    val isOpen24HoursEveryDay: Boolean
        get() = placeOpeningHours?.isOpen24HoursEveryDay() == true

    val hasEuroKey: Boolean
        get() = euroKey.equals("yes", ignoreCase = true) ||
                euroKey.equals("true", ignoreCase = true) ||
                euroKey == "1"

    val shareUrl: String
        get() {
            val placeSegment = if (!placeId.isNullOrBlank()) {
                val placeName = owner.ifBlank { name }
                val slug = placeName.toUrlSlug().ifBlank { "Nearby" }
                "$slug---$placeId"
            } else {
                "Nearby---NEARBY"
            }

            val toiletNameOrOwner = owner.ifBlank { name }
            val toiletSlug = toiletNameOrOwner.toUrlSlug().ifBlank { "toilet" }
            val toiletSegment = "$toiletSlug---$id"

            return "https://wc-info.org/Toilets/$placeSegment/$toiletSegment"
        }
}

fun String.toUrlSlug(): String {
    var text = this.trim()
    if (text.isEmpty()) return ""

    val replacements = listOf(
        "ä" to "ae", "ö" to "oe", "ü" to "ue",
        "Ä" to "Ae", "Ö" to "Oe", "Ü" to "Ue",
        "ß" to "ss"
    )
    for ((target, replacement) in replacements) {
        text = text.replace(target, replacement)
    }

    text = Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")

    val result = StringBuilder()
    var lastWasHyphen = false

    for (ch in text) {
        if (ch.isLetterOrDigit()) {
            result.append(ch)
            lastWasHyphen = false
        } else if (!lastWasHyphen) {
            result.append('-')
            lastWasHyphen = true
        }
    }

    return result.toString().trim('-').lowercase(Locale.ROOT)
}

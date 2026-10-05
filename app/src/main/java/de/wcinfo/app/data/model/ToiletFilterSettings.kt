package de.wcinfo.app.data.model

import android.content.Context
import android.net.Uri
import de.wcinfo.app.R
import kotlinx.serialization.Serializable

@Serializable
data class ToiletFilterSettings(
    val showClosed: Boolean = false,
    val showNonPublic: Boolean = false,
    val showNonWheelchairAccessible: Boolean = true,
    val showWithoutChangingTable: Boolean = true,
    val showWithoutGenderSeparation: Boolean = true,
    val showWithoutEuroKey: Boolean = true,
    val allowNonPublicFallback: Boolean = true,
    val maxPublicDistanceMeters: Int = 500
) {
    fun toApiFilterQuery(): String? {
        val parts = mutableListOf<String>()
        if (!showClosed) {
            parts.add("is_open:true")
        }
        if (!showNonPublic) {
            parts.add("public_accessible:true")
        }
        if (!showNonWheelchairAccessible) {
            parts.add("has_wheelchair_access:true")
        }
        if (!showWithoutChangingTable) {
            parts.add("has_changing_table:true")
        }
        if (!showWithoutGenderSeparation) {
            parts.add("is_gender_separated:true")
        }
        if (!showWithoutEuroKey) {
            parts.add("euro_key:yes")
        }
        return if (parts.isEmpty()) null else parts.joinToString(",")
    }

    val allSwitchesOn: Boolean
        get() = showClosed && showNonPublic && showNonWheelchairAccessible &&
                showWithoutChangingTable && showWithoutGenderSeparation && showWithoutEuroKey

    val hasActiveFilters: Boolean
        get() = !allSwitchesOn

    val isDefault: Boolean
        get() = !showClosed && !showNonPublic && showNonWheelchairAccessible &&
                showWithoutChangingTable && showWithoutGenderSeparation && showWithoutEuroKey

    fun getSummaryText(context: Context): String {
        return getSummaryText { resId -> context.getString(resId) }
    }

    fun getSummaryText(resolveString: (Int) -> String): String {
        if (allSwitchesOn) {
            return resolveString(R.string.filter_all)
        }

        val adjectives = mutableListOf<String>()
        if (!showClosed) {
            adjectives.add(resolveString(R.string.filter_desc_opened))
        }
        if (!showNonPublic) {
            adjectives.add(resolveString(R.string.filter_desc_public))
        }
        if (!showNonWheelchairAccessible) {
            adjectives.add(resolveString(R.string.filter_desc_wheelchair))
        }
        if (!showWithoutGenderSeparation) {
            adjectives.add(resolveString(R.string.filter_desc_gender_separated))
        }

        val postModifiers = mutableListOf<String>()
        if (!showWithoutChangingTable) {
            postModifiers.add(resolveString(R.string.filter_desc_changing_table))
        }
        if (!showWithoutEuroKey) {
            postModifiers.add(resolveString(R.string.filter_desc_euro_key))
        }

        val prefix = resolveString(R.string.filter_summary_showing_prefix)
        val toiletsWord = resolveString(R.string.filter_summary_toilets)

        val adjPart = if (adjectives.isNotEmpty()) " " + adjectives.joinToString(", ") else ""
        val mainPart = "$prefix$adjPart $toiletsWord"

        return if (postModifiers.isNotEmpty()) {
            "$mainPart " + postModifiers.joinToString(", ")
        } else {
            mainPart
        }
    }

    fun toDeepLinkUri(): Uri {
        return Uri.parse(
            "wcinfo://urgent-navigate" +
            "?showClosed=$showClosed" +
            "&showNonPublic=$showNonPublic" +
            "&allowNonPublicFallback=$allowNonPublicFallback" +
            "&maxPublicDistanceMeters=$maxPublicDistanceMeters" +
            "&showNonWheelchairAccessible=$showNonWheelchairAccessible" +
            "&showWithoutChangingTable=$showWithoutChangingTable" +
            "&showWithoutGenderSeparation=$showWithoutGenderSeparation" +
            "&showWithoutEuroKey=$showWithoutEuroKey"
        )
    }

    companion object {
        fun fromUri(uri: Uri): ToiletFilterSettings {
            return ToiletFilterSettings(
                showClosed = uri.getQueryParameter("showClosed")?.toBooleanStrictOrNull() ?: false,
                showNonPublic = uri.getQueryParameter("showNonPublic")?.toBooleanStrictOrNull() ?: false,
                allowNonPublicFallback = uri.getQueryParameter("allowNonPublicFallback")?.toBooleanStrictOrNull() ?: true,
                maxPublicDistanceMeters = uri.getQueryParameter("maxPublicDistanceMeters")?.toIntOrNull() ?: 500,
                showNonWheelchairAccessible = uri.getQueryParameter("showNonWheelchairAccessible")?.toBooleanStrictOrNull() ?: true,
                showWithoutChangingTable = uri.getQueryParameter("showWithoutChangingTable")?.toBooleanStrictOrNull() ?: true,
                showWithoutGenderSeparation = uri.getQueryParameter("showWithoutGenderSeparation")?.toBooleanStrictOrNull() ?: true,
                showWithoutEuroKey = uri.getQueryParameter("showWithoutEuroKey")?.toBooleanStrictOrNull() ?: true
            )
        }
    }
}

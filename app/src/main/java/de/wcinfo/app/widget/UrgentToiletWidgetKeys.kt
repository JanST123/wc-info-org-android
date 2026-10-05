package de.wcinfo.app.widget

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import de.wcinfo.app.R
import de.wcinfo.app.data.model.ToiletFilterSettings

object UrgentToiletWidgetKeys {
    val SHOW_CLOSED = booleanPreferencesKey("showClosed")
    val SHOW_NON_PUBLIC = booleanPreferencesKey("showNonPublic")
    val ALLOW_NON_PUBLIC_FALLBACK = booleanPreferencesKey("allowNonPublicFallback")
    val MAX_PUBLIC_DISTANCE_METERS = intPreferencesKey("maxPublicDistanceMeters")
    val SHOW_NON_WHEELCHAIR_ACCESSIBLE = booleanPreferencesKey("showNonWheelchairAccessible")
    val SHOW_WITHOUT_CHANGING_TABLE = booleanPreferencesKey("showWithoutChangingTable")
    val SHOW_WITHOUT_GENDER_SEPARATION = booleanPreferencesKey("showWithoutGenderSeparation")
    val SHOW_WITHOUT_EURO_KEY = booleanPreferencesKey("showWithoutEuroKey")
}

data class UrgentWidgetConfig(
    val showClosed: Boolean = false,
    val showNonPublic: Boolean = true,
    val allowNonPublicFallback: Boolean = true,
    val maxPublicDistanceMeters: Int = 500,
    val showNonWheelchairAccessible: Boolean = true,
    val showWithoutChangingTable: Boolean = true,
    val showWithoutGenderSeparation: Boolean = true,
    val showWithoutEuroKey: Boolean = true
) {
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

    fun toToiletFilterSettings(): ToiletFilterSettings {
        return ToiletFilterSettings(
            showClosed = showClosed,
            showNonPublic = showNonPublic,
            allowNonPublicFallback = allowNonPublicFallback,
            maxPublicDistanceMeters = maxPublicDistanceMeters,
            showNonWheelchairAccessible = showNonWheelchairAccessible,
            showWithoutChangingTable = showWithoutChangingTable,
            showWithoutGenderSeparation = showWithoutGenderSeparation,
            showWithoutEuroKey = showWithoutEuroKey
        )
    }

    fun getActiveFilterTags(context: Context? = null): List<String> {
        val tags = mutableListOf<String>()
        if (!showClosed) {
            tags.add(context?.getString(R.string.widget_tag_open) ?: "Open")
        }
        if (!showNonPublic) {
            if (allowNonPublicFallback) {
                tags.add(
                    context?.getString(R.string.widget_tag_public_fallback, maxPublicDistanceMeters)
                        ?: "Public (>${maxPublicDistanceMeters}m fallback)"
                )
            } else {
                tags.add(context?.getString(R.string.widget_tag_only_public) ?: "Public only")
            }
        }
        if (!showNonWheelchairAccessible) {
            tags.add(context?.getString(R.string.widget_tag_wheelchair) ?: "Wheelchair accessible")
        }
        if (!showWithoutChangingTable) {
            tags.add(context?.getString(R.string.widget_tag_changing_table) ?: "Changing table")
        }
        if (!showWithoutGenderSeparation) {
            tags.add(context?.getString(R.string.widget_tag_separated) ?: "Separated")
        }
        if (!showWithoutEuroKey) {
            tags.add(context?.getString(R.string.widget_tag_euro_key) ?: "Euro-key")
        }
        return tags
    }

    companion object {
        fun fromPreferences(prefs: Preferences): UrgentWidgetConfig {
            return UrgentWidgetConfig(
                showClosed = prefs[UrgentToiletWidgetKeys.SHOW_CLOSED] ?: false,
                showNonPublic = prefs[UrgentToiletWidgetKeys.SHOW_NON_PUBLIC] ?: true,
                allowNonPublicFallback = prefs[UrgentToiletWidgetKeys.ALLOW_NON_PUBLIC_FALLBACK] ?: true,
                maxPublicDistanceMeters = prefs[UrgentToiletWidgetKeys.MAX_PUBLIC_DISTANCE_METERS] ?: 500,
                showNonWheelchairAccessible = prefs[UrgentToiletWidgetKeys.SHOW_NON_WHEELCHAIR_ACCESSIBLE] ?: true,
                showWithoutChangingTable = prefs[UrgentToiletWidgetKeys.SHOW_WITHOUT_CHANGING_TABLE] ?: true,
                showWithoutGenderSeparation = prefs[UrgentToiletWidgetKeys.SHOW_WITHOUT_GENDER_SEPARATION] ?: true,
                showWithoutEuroKey = prefs[UrgentToiletWidgetKeys.SHOW_WITHOUT_EURO_KEY] ?: true
            )
        }
    }
}

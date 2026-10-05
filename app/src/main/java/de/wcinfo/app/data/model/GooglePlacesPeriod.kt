package de.wcinfo.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GooglePlacesPoint(
    val day: Int = 0,
    val hour: Int = 0,
    val minute: Int = 0
) {
    val formattedTime: String
        get() = "%02d:%02d".format(hour, minute)

    val dayNameShort: String
        get() = when (day) {
            0 -> "So"
            1 -> "Mo"
            2 -> "Di"
            3 -> "Mi"
            4 -> "Do"
            5 -> "Fr"
            6 -> "Sa"
            else -> ""
        }
}

@Serializable
data class GooglePlacesPeriod(
    val open: GooglePlacesPoint,
    val close: GooglePlacesPoint? = null
)

fun List<GooglePlacesPeriod>.isOpen24HoursEveryDay(): Boolean {
    if (size == 1) {
        val first = first()
        if (first.open.day == 0 && first.open.hour == 0 && first.open.minute == 0 && first.close == null) {
            return true
        }
    }
    if (size == 7) {
        val all24h = all { period ->
            period.open.hour == 0 && period.open.minute == 0 &&
                    (period.close == null || (period.close.hour == 23 && period.close.minute >= 59) || period.close.hour == 0)
        }
        if (all24h) return true
    }
    return false
}

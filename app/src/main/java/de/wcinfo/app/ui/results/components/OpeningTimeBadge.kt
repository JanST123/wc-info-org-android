package de.wcinfo.app.ui.results.components

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import de.wcinfo.app.R
import de.wcinfo.app.data.model.Toilet
import de.wcinfo.app.ui.theme.PrimaryPurple
import de.wcinfo.app.ui.theme.StatusClosed
import de.wcinfo.app.ui.theme.StatusOpen
import de.wcinfo.app.ui.theme.StatusUrgent
import java.time.Instant
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

data class OpeningStatusInfo(
    val title: String,
    val subtitle: String?,
    val color: Color
)

fun calculateOpeningStatus(context: Context, toilet: Toilet): OpeningStatusInfo {
    return calculateOpeningStatus(
        toilet = toilet,
        resolveString = { context.getString(it) },
        resolveStringArgs = { id, args -> context.getString(id, *args) }
    )
}

fun calculateOpeningStatus(
    toilet: Toilet,
    resolveString: (Int) -> String,
    resolveStringArgs: (Int, Array<out Any>) -> String = { id, args -> String.format(resolveString(id), *args) }
): OpeningStatusInfo {
    if (toilet.temporaryClosed) {
        return OpeningStatusInfo(resolveString(R.string.open_status_temporarily_closed), null, StatusUrgent)
    }

    if (toilet.placeOpeningHours.isNullOrEmpty()) {
        return OpeningStatusInfo(resolveString(R.string.open_status_no_times), null, StatusClosed)
    }

    if (toilet.isOpen24HoursEveryDay) {
        return OpeningStatusInfo(resolveString(R.string.open_status_open_24h), null, StatusOpen)
    }

    // Direct open boolean flag
    val isOpen = toilet.isOpen

    // Parse timestamps if present
    val now = Instant.now()
    var closeInstant: Instant? = null
    var openInstant: Instant? = null

    try {
        toilet.closeTimestamp?.let { closeInstant = Instant.parse(it) }
    } catch (_: DateTimeParseException) {}

    try {
        toilet.openTimestamp?.let { openInstant = Instant.parse(it) }
    } catch (_: DateTimeParseException) {}

    if (isOpen == true) {
        if (closeInstant != null && closeInstant!!.isAfter(now)) {
            val minutesUntilClose = ChronoUnit.MINUTES.between(now, closeInstant)
            val hoursUntilClose = ChronoUnit.HOURS.between(now, closeInstant)

            return if (minutesUntilClose < 30) {
                val timeStr = resolveStringArgs(R.string.time_minutes, arrayOf(minutesUntilClose))
                OpeningStatusInfo(
                    title = resolveString(R.string.open_status_open),
                    subtitle = resolveStringArgs(R.string.open_status_closes_in, arrayOf(timeStr)),
                    color = StatusUrgent
                )
            } else if (hoursUntilClose in 1..24) {
                val remainingMin = minutesUntilClose % 60
                val timeStr = if (remainingMin > 0) {
                    resolveStringArgs(R.string.time_hours_minutes, arrayOf(hoursUntilClose, remainingMin))
                } else {
                    resolveStringArgs(R.string.time_hours, arrayOf(hoursUntilClose))
                }
                OpeningStatusInfo(
                    title = resolveString(R.string.open_status_open),
                    subtitle = resolveStringArgs(R.string.open_status_closes_in, arrayOf(timeStr)),
                    color = StatusOpen
                )
            } else {
                OpeningStatusInfo(
                    title = resolveString(R.string.open_status_open_now),
                    subtitle = null,
                    color = StatusOpen
                )
            }
        }
        return OpeningStatusInfo(resolveString(R.string.open_status_open_now), null, StatusOpen)
    } else if (isOpen == false) {
        if (toilet.accessibleOutsideOpeningTimes) {
            return OpeningStatusInfo(
                title = resolveString(R.string.open_status_closed),
                subtitle = resolveString(R.string.open_status_accessible_outside),
                color = StatusClosed
            )
        }
        if (openInstant != null && openInstant!!.isAfter(now)) {
            val minutesUntilOpen = ChronoUnit.MINUTES.between(now, openInstant)
            val hoursUntilOpen = ChronoUnit.HOURS.between(now, openInstant)
            val timeStr = if (hoursUntilOpen > 0) {
                val remainingMin = minutesUntilOpen % 60
                if (remainingMin > 0) {
                    resolveStringArgs(R.string.time_hours_minutes, arrayOf(hoursUntilOpen, remainingMin))
                } else {
                    resolveStringArgs(R.string.time_hours, arrayOf(hoursUntilOpen))
                }
            } else {
                resolveStringArgs(R.string.time_minutes, arrayOf(minutesUntilOpen))
            }
            return OpeningStatusInfo(
                title = resolveString(R.string.open_status_closed),
                subtitle = resolveStringArgs(R.string.open_status_opens_in, arrayOf(timeStr)),
                color = PrimaryPurple
            )
        }
        return OpeningStatusInfo(resolveString(R.string.open_status_closed), null, StatusClosed)
    }

    if (toilet.accessibleOutsideOpeningTimes) {
        return OpeningStatusInfo(
            title = resolveString(R.string.open_status_closed),
            subtitle = resolveString(R.string.open_status_accessible_outside),
            color = StatusClosed
        )
    }

    return OpeningStatusInfo(resolveString(R.string.open_status_check_times), null, StatusClosed)
}

@Composable
fun OpeningTimeBadge(
    toilet: Toilet,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.End
) {
    val context = LocalContext.current
    val status = calculateOpeningStatus(context, toilet)

    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment
    ) {
        Text(
            text = status.title,
            color = status.color,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        if (!status.subtitle.isNullOrBlank()) {
            Text(
                text = status.subtitle,
                color = if (status.color == StatusUrgent) StatusUrgent else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

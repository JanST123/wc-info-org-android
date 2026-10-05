package de.wcinfo.app.ui.results.components

import de.wcinfo.app.R
import de.wcinfo.app.data.model.GooglePlacesPeriod
import de.wcinfo.app.data.model.GooglePlacesPoint
import de.wcinfo.app.data.model.Toilet
import de.wcinfo.app.ui.theme.PrimaryPurple
import de.wcinfo.app.ui.theme.StatusClosed
import de.wcinfo.app.ui.theme.StatusOpen
import de.wcinfo.app.ui.theme.StatusUrgent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpeningTimeBadgeTest {

    private val stringResolver: (Int) -> String = { resId ->
        when (resId) {
            R.string.open_status_temporarily_closed -> "Temporarily closed"
            R.string.open_status_open_24h -> "Open 24 hours"
            R.string.open_status_open -> "Open"
            R.string.open_status_open_now -> "Open now"
            R.string.open_status_closed -> "Closed"
            R.string.open_status_closes_in -> "closes in %1\$s"
            R.string.open_status_opens_in -> "opens in %1\$s"
            R.string.open_status_accessible_outside -> "Accessible outside opening hours"
            R.string.open_status_no_times -> "No opening times"
            R.string.open_status_check_times -> "Check hours"
            R.string.time_hours_minutes -> "%1\$d h %2\$d min"
            R.string.time_hours -> "%1\$d h"
            R.string.time_minutes -> "%1\$d min"
            else -> ""
        }
    }

    private val stringArgsResolver: (Int, Array<out Any>) -> String = { resId, args ->
        val format = stringResolver(resId)
        String.format(format, *args)
    }

    @Test
    fun testNullPlaceOpeningHoursReturnsNoOpeningTimes() {
        val toilet = Toilet(
            id = 1,
            placeOpeningHours = null,
            accessibleOutsideOpeningTimes = false
        )
        val status = calculateOpeningStatus(toilet, stringResolver, stringArgsResolver)
        assertEquals("No opening times", status.title)
        assertNull(status.subtitle)
        assertEquals(StatusClosed, status.color)
    }

    @Test
    fun testEmptyPlaceOpeningHoursReturnsNoOpeningTimes() {
        val toilet = Toilet(
            id = 1,
            placeOpeningHours = emptyList(),
            accessibleOutsideOpeningTimes = false
        )
        val status = calculateOpeningStatus(toilet, stringResolver, stringArgsResolver)
        assertEquals("No opening times", status.title)
        assertNull(status.subtitle)
        assertEquals(StatusClosed, status.color)
    }

    @Test
    fun testNullOpeningHoursWithAccessibleOutsideReturnsNoOpeningTimes() {
        val toilet = Toilet(
            id = 1,
            placeOpeningHours = null,
            accessibleOutsideOpeningTimes = true
        )
        val status = calculateOpeningStatus(toilet, stringResolver, stringArgsResolver)
        assertEquals("No opening times", status.title)
        assertNull(status.subtitle)
        assertEquals(StatusClosed, status.color)
    }

    @Test
    fun testTemporarilyClosedOverridesEverything() {
        val toilet = Toilet(
            id = 1,
            temporaryClosed = true,
            placeOpeningHours = listOf(
                GooglePlacesPeriod(open = GooglePlacesPoint(day = 0, hour = 0, minute = 0))
            ),
            isOpen = true
        )
        val status = calculateOpeningStatus(toilet, stringResolver, stringArgsResolver)
        assertEquals("Temporarily closed", status.title)
        assertNull(status.subtitle)
        assertEquals(StatusUrgent, status.color)
    }

    @Test
    fun testClosedAndAccessibleOutsideOpeningTimesReturnsClosedWithSubtitle() {
        val samplePeriods = listOf(
            GooglePlacesPeriod(
                open = GooglePlacesPoint(day = 1, hour = 8, minute = 0),
                close = GooglePlacesPoint(day = 1, hour = 18, minute = 0)
            )
        )
        val toilet = Toilet(
            id = 1,
            placeOpeningHours = samplePeriods,
            isOpen = false,
            accessibleOutsideOpeningTimes = true
        )
        val status = calculateOpeningStatus(toilet, stringResolver, stringArgsResolver)
        assertEquals("Closed", status.title)
        assertEquals("Accessible outside opening hours", status.subtitle)
        assertEquals(StatusClosed, status.color)
    }

    @Test
    fun testClosedWithoutAccessibleOutsideReturnsClosed() {
        val samplePeriods = listOf(
            GooglePlacesPeriod(
                open = GooglePlacesPoint(day = 1, hour = 8, minute = 0),
                close = GooglePlacesPoint(day = 1, hour = 18, minute = 0)
            )
        )
        val toilet = Toilet(
            id = 1,
            placeOpeningHours = samplePeriods,
            isOpen = false,
            accessibleOutsideOpeningTimes = false
        )
        val status = calculateOpeningStatus(toilet, stringResolver, stringArgsResolver)
        assertEquals("Closed", status.title)
        assertNull(status.subtitle)
        assertEquals(StatusClosed, status.color)
    }

    @Test
    fun testOpenNowWithoutCloseTimestampReturnsOpenNow() {
        val samplePeriods = listOf(
            GooglePlacesPeriod(
                open = GooglePlacesPoint(day = 1, hour = 8, minute = 0),
                close = GooglePlacesPoint(day = 1, hour = 18, minute = 0)
            )
        )
        val toilet = Toilet(
            id = 1,
            placeOpeningHours = samplePeriods,
            isOpen = true
        )
        val status = calculateOpeningStatus(toilet, stringResolver, stringArgsResolver)
        assertEquals("Open now", status.title)
        assertNull(status.subtitle)
        assertEquals(StatusOpen, status.color)
    }
}

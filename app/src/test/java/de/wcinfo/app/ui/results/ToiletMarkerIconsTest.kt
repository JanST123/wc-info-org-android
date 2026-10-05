package de.wcinfo.app.ui.results

import de.wcinfo.app.R
import org.junit.Assert.assertEquals
import org.junit.Test

class ToiletMarkerIconsTest {

    @Test
    fun testWheelchairAccessMarkerSelectedWhenOpen() {
        val resId = ToiletMarkerIcons.getMarkerDrawableRes(
            hasWheelchairAccess = true,
            isGenderSeparated = false,
            temporaryClosed = false
        )
        assertEquals(R.drawable.ic_marker_wheelchair, resId)
    }

    @Test
    fun testWheelchairAccessTakesPrecedenceOverGenderSeparation() {
        val resId = ToiletMarkerIcons.getMarkerDrawableRes(
            hasWheelchairAccess = true,
            isGenderSeparated = true,
            temporaryClosed = false
        )
        assertEquals(R.drawable.ic_marker_wheelchair, resId)
    }

    @Test
    fun testGenderSeparatedMarkerSelectedWhenOpenAndNoWheelchair() {
        val resId = ToiletMarkerIcons.getMarkerDrawableRes(
            hasWheelchairAccess = false,
            isGenderSeparated = true,
            temporaryClosed = false
        )
        assertEquals(R.drawable.ic_marker_restroom, resId)
    }

    @Test
    fun testToiletPaperMarkerSelectedWhenNeitherWheelchairNorGenderSeparated() {
        val resId = ToiletMarkerIcons.getMarkerDrawableRes(
            hasWheelchairAccess = false,
            isGenderSeparated = false,
            temporaryClosed = false
        )
        assertEquals(R.drawable.ic_marker_toilet_paper, resId)
    }

    @Test
    fun testClosedWheelchairMarkerSelectedWhenTemporaryClosed() {
        val resId = ToiletMarkerIcons.getMarkerDrawableRes(
            hasWheelchairAccess = true,
            isGenderSeparated = false,
            temporaryClosed = true
        )
        assertEquals(R.drawable.ic_marker_wheelchair_closed, resId)
    }

    @Test
    fun testClosedRestroomMarkerSelectedWhenTemporaryClosed() {
        val resId = ToiletMarkerIcons.getMarkerDrawableRes(
            hasWheelchairAccess = false,
            isGenderSeparated = true,
            temporaryClosed = true
        )
        assertEquals(R.drawable.ic_marker_restroom_closed, resId)
    }

    @Test
    fun testClosedToiletPaperMarkerSelectedWhenTemporaryClosed() {
        val resId = ToiletMarkerIcons.getMarkerDrawableRes(
            hasWheelchairAccess = false,
            isGenderSeparated = false,
            temporaryClosed = true
        )
        assertEquals(R.drawable.ic_marker_toilet_paper_closed, resId)
    }
}

package de.wcinfo.app.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UrgentToiletWidgetTest {

    @Test
    fun testDefaultConfigFilterTags() {
        // Defaults: showClosed=false, showNonPublic=true, others=true
        val config = UrgentWidgetConfig()
        val tags = config.getActiveFilterTags()
        // Only "Open" since showClosed is false by default and showNonPublic is true
        assertEquals(listOf("Open"), tags)
    }

    @Test
    fun testStrictPublicFilterTags() {
        val config = UrgentWidgetConfig(
            showClosed = false,
            showNonPublic = false,
            allowNonPublicFallback = false,
            showNonWheelchairAccessible = false,
            showWithoutChangingTable = false
        )
        val tags = config.getActiveFilterTags()
        assertTrue(tags.contains("Open"))
        assertTrue(tags.contains("Public only"))
        assertTrue(tags.contains("Wheelchair accessible"))
        assertTrue(tags.contains("Changing table"))
    }

    @Test
    fun testFallbackPublicFilterTags() {
        val config = UrgentWidgetConfig(
            showClosed = false,
            showNonPublic = false,
            allowNonPublicFallback = true,
            maxPublicDistanceMeters = 500
        )
        val tags = config.getActiveFilterTags()
        assertTrue(tags.contains("Public (>500m fallback)"))
    }

    @Test
    fun testAllAllowedFilterTagsEmpty() {
        val config = UrgentWidgetConfig(
            showClosed = true,
            showNonPublic = true,
            showNonWheelchairAccessible = true,
            showWithoutChangingTable = true,
            showWithoutGenderSeparation = true,
            showWithoutEuroKey = true
        )
        val tags = config.getActiveFilterTags()
        assertTrue(tags.isEmpty())
    }
}

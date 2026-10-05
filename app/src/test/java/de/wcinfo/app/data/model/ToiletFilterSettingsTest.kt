package de.wcinfo.app.data.model

import de.wcinfo.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToiletFilterSettingsTest {

    private val stringResolver: (Int) -> String = { resId ->
        when (resId) {
            R.string.filter_all -> "Show all toilets"
            R.string.filter_summary_showing_prefix -> "Showing"
            R.string.filter_summary_toilets -> "toilets"
            R.string.filter_desc_opened -> "opened"
            R.string.filter_desc_public -> "public accessible"
            R.string.filter_desc_wheelchair -> "wheelchair accessible"
            R.string.filter_desc_gender_separated -> "gender-separated"
            R.string.filter_desc_changing_table -> "with changing table"
            R.string.filter_desc_euro_key -> "with Euro-key"
            else -> ""
        }
    }

    private val germanStringResolver: (Int) -> String = { resId ->
        when (resId) {
            R.string.filter_all -> "Alle Toiletten anzeigen"
            R.string.filter_summary_showing_prefix -> "Zeige"
            R.string.filter_summary_toilets -> "Toiletten"
            R.string.filter_desc_opened -> "geöffnete"
            R.string.filter_desc_public -> "öffentlich zugängliche"
            R.string.filter_desc_wheelchair -> "rollstuhlgerechte"
            R.string.filter_desc_gender_separated -> "geschlechtergetrennte"
            R.string.filter_desc_changing_table -> "mit Wickeltisch"
            R.string.filter_desc_euro_key -> "mit Euroschlüssel"
            else -> ""
        }
    }

    @Test
    fun testAllSwitchesOnReturnsShowAllToilets() {
        val settings = ToiletFilterSettings(
            showClosed = true,
            showNonPublic = true,
            showNonWheelchairAccessible = true,
            showWithoutChangingTable = true,
            showWithoutGenderSeparation = true,
            showWithoutEuroKey = true
        )
        assertTrue(settings.allSwitchesOn)
        assertFalse(settings.hasActiveFilters)
        assertEquals("Show all toilets", settings.getSummaryText(stringResolver))
        assertEquals("Alle Toiletten anzeigen", settings.getSummaryText(germanStringResolver))
    }

    @Test
    fun testOnlyClosedOffReturnsShowingOpenedToilets() {
        val settings = ToiletFilterSettings(
            showClosed = false,
            showNonPublic = true,
            showNonWheelchairAccessible = true,
            showWithoutChangingTable = true,
            showWithoutGenderSeparation = true,
            showWithoutEuroKey = true
        )
        assertFalse(settings.allSwitchesOn)
        assertTrue(settings.hasActiveFilters)
        assertEquals("Showing opened toilets", settings.getSummaryText(stringResolver))
        assertEquals("Zeige geöffnete Toiletten", settings.getSummaryText(germanStringResolver))
    }

    @Test
    fun testClosedAndNonPublicOffReturnsShowingOpenedPublicAccessibleToilets() {
        val settings = ToiletFilterSettings(
            showClosed = false,
            showNonPublic = false,
            showNonWheelchairAccessible = true,
            showWithoutChangingTable = true,
            showWithoutGenderSeparation = true,
            showWithoutEuroKey = true
        )
        assertFalse(settings.allSwitchesOn)
        assertTrue(settings.hasActiveFilters)
        assertEquals("Showing opened, public accessible toilets", settings.getSummaryText(stringResolver))
        assertEquals("Zeige geöffnete, öffentlich zugängliche Toiletten", settings.getSummaryText(germanStringResolver))
    }

    @Test
    fun testAllSwitchesOffReturnsAllDescriptions() {
        val settings = ToiletFilterSettings(
            showClosed = false,
            showNonPublic = false,
            showNonWheelchairAccessible = false,
            showWithoutChangingTable = false,
            showWithoutGenderSeparation = false,
            showWithoutEuroKey = false
        )
        assertFalse(settings.allSwitchesOn)
        assertTrue(settings.hasActiveFilters)
        assertEquals(
            "Showing opened, public accessible, wheelchair accessible, gender-separated toilets with changing table, with Euro-key",
            settings.getSummaryText(stringResolver)
        )
        assertEquals(
            "Zeige geöffnete, öffentlich zugängliche, rollstuhlgerechte, geschlechtergetrennte Toiletten mit Wickeltisch, mit Euroschlüssel",
            settings.getSummaryText(germanStringResolver)
        )
    }

    @Test
    fun testDefaultSettings() {
        val settings = ToiletFilterSettings()
        assertTrue(settings.isDefault)
        assertFalse(settings.showClosed)
        assertFalse(settings.showNonPublic)
        assertTrue(settings.showNonWheelchairAccessible)
        assertTrue(settings.showWithoutChangingTable)
        assertTrue(settings.showWithoutGenderSeparation)
        assertTrue(settings.showWithoutEuroKey)
        assertEquals("is_open:true,public_accessible:true", settings.toApiFilterQuery())
    }

    @Test
    fun testCustomFilterQuery() {
        val settings = ToiletFilterSettings(
            showClosed = false,
            showNonPublic = false,
            showNonWheelchairAccessible = false,
            showWithoutChangingTable = false,
            showWithoutGenderSeparation = false,
            showWithoutEuroKey = false
        )
        assertEquals(
            "is_open:true,public_accessible:true,has_wheelchair_access:true,has_changing_table:true,is_gender_separated:true,euro_key:yes",
            settings.toApiFilterQuery()
        )
    }
}

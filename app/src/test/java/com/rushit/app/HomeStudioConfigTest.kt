package com.rushit.app

import com.rushit.app.ui.studio.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class HomeStudioConfigTest {
    @Test fun roundTripKeepsIndependentSections() {
        val config = HomeStudioConfig(top = TopBladeConfig(festival = Festival.PARTY, heading = "Party"),
            bottom = BottomSectionConfig(showCategories = false, groceryTitle = "Featured"))
        assertEquals(config, HomeStudioCodec.decode(HomeStudioCodec.encode(config)))
        assertFalse(HomeStudioCodec.decode(HomeStudioCodec.encode(config)).bottom.showCategories)
    }
    @Test fun corruptAndFutureVersionFallBackToOriginal() {
        assertEquals(HomeStudioConfig(), HomeStudioCodec.decode("not json"))
        assertEquals(HomeStudioConfig(), HomeStudioCodec.decode("{\"version\":2}"))
        assertEquals(HomeStudioConfig(), HomeStudioCodec.decode(null))
    }
    @Test fun unsupportedFestivalAndExtremeValuesAreSafe() {
        val config = HomeStudioCodec.decode("""{"version":1,"top":{"festival":"UNKNOWN","duration":50000},"bottom":{}}""")
        assertEquals(Festival.NORMAL, config.top.festival)
        assertEquals(1000, config.top.durationMs)
        assertEquals(BottomSectionConfig(), config.bottom)
    }
    @Test fun changingFestivalDoesNotChangeBottom() {
        val bottom = BottomSectionConfig(showModes = false, groceryTitle = "Local")
        val draft = HomeStudioConfig(bottom = bottom)
        assertEquals(bottom, draft.copy(top = draft.top.copy(festival = Festival.DIWALI)).bottom)
    }
}

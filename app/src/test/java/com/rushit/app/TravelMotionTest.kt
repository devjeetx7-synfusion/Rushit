package com.rushit.app

import com.rushit.app.domain.RiderGeometry
import com.rushit.app.domain.TravelParallax
import com.rushit.app.domain.parallaxOffset
import com.rushit.app.domain.travelSpeedFactor
import com.rushit.app.domain.wheelDegrees
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

class TravelMotionTest {
    @Test fun `one wheel circumference rolls exactly one turn`() {
        val radius = 20f
        assertEquals(360f, wheelDegrees(2f * PI.toFloat() * radius, radius), 0.01f)
        assertEquals(0f, wheelDegrees(0f, radius), 0.001f)
        assertEquals(0f, wheelDegrees(100f, 0f), 0.001f)
    }

    @Test fun `hub pivots stay inside the supplied rider artwork`() {
        val w = RiderGeometry.SOURCE_WIDTH
        val h = RiderGeometry.SOURCE_HEIGHT
        val (rx, ry) = RiderGeometry.rearHub(w, h)
        val (fx, fy) = RiderGeometry.frontHub(w, h)
        assertEquals(RiderGeometry.REAR_HUB_X, rx, 0.001f)
        assertEquals(RiderGeometry.REAR_HUB_Y, ry, 0.001f)
        assertEquals(RiderGeometry.FRONT_HUB_X, fx, 0.001f)
        assertEquals(RiderGeometry.FRONT_HUB_Y, fy, 0.001f)
        assertTrue(rx < fx)
        assertTrue(RiderGeometry.wheelRadius(w) > 0f)
    }

    @Test fun `parallax layers move at different restrained speeds`() {
        val d = 100f
        assertTrue(parallaxOffset(d, TravelParallax.SKYLINE) < parallaxOffset(d, TravelParallax.TREES))
        assertTrue(parallaxOffset(d, TravelParallax.TREES) < parallaxOffset(d, TravelParallax.ROAD))
        assertEquals(d, parallaxOffset(d, TravelParallax.ROAD), 0.001f)
    }

    @Test fun `almost there slows without stopping the loop`() {
        assertTrue(travelSpeedFactor(almostThere = true) < travelSpeedFactor(almostThere = false))
        assertTrue(travelSpeedFactor(almostThere = true) > 0f)
    }
}

package com.rushit.app.domain

import kotlin.math.PI

/**
 * Pure geometry for the layered rider/scooter travel scene.
 *
 * The supplied rider artwork is 349 × 302 px. Wheel hubs are centred at these source coordinates;
 * keeping them in one place lets the renderer and unit tests agree on pivots and rolling distance.
 */
object RiderGeometry {
    const val SOURCE_WIDTH = 349f
    const val SOURCE_HEIGHT = 302f

    const val REAR_HUB_X = 62f
    const val REAR_HUB_Y = 257f
    const val FRONT_HUB_X = 303f
    const val FRONT_HUB_Y = 258f

    /** Tyre radius in source pixels; the visible hub is smaller but rolls on the tyre radius. */
    const val WHEEL_RADIUS = 44f

    fun rearHub(drawWidth: Float, drawHeight: Float): Pair<Float, Float> =
        REAR_HUB_X / SOURCE_WIDTH * drawWidth to REAR_HUB_Y / SOURCE_HEIGHT * drawHeight

    fun frontHub(drawWidth: Float, drawHeight: Float): Pair<Float, Float> =
        FRONT_HUB_X / SOURCE_WIDTH * drawWidth to FRONT_HUB_Y / SOURCE_HEIGHT * drawHeight

    fun wheelRadius(drawWidth: Float): Float = WHEEL_RADIUS / SOURCE_WIDTH * drawWidth
}

/** Degrees a wheel must turn after rolling [distance] pixels without slipping. */
fun wheelDegrees(distance: Float, wheelRadius: Float): Float {
    if (wheelRadius <= 0f) return 0f
    return (distance / (2f * PI.toFloat() * wheelRadius)) * 360f
}

/** Background layers move left (opposite the rider) at restrained parallax speeds. */
fun parallaxOffset(distance: Float, factor: Float): Float = distance * factor

object TravelParallax {
    const val SKYLINE = 0.18f
    const val TREES = 0.45f
    const val ROAD = 1.0f
}

/** Almost There keeps the same travel loop but eases into a slower approach. */
fun travelSpeedFactor(almostThere: Boolean): Float = if (almostThere) 0.45f else 1f

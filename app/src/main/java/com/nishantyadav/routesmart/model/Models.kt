package com.nishantyadav.routesmart.model

import org.osmdroid.util.GeoPoint

/**
 * RouteInfo — holds all data returned from OSRM for a single route.
 */
data class RouteInfo(
    val points       : List<GeoPoint>,  // decoded polyline points
    val distanceText : String,           // e.g. "12.3 km"
    val durationText : String,           // e.g. "28 min"
    val distanceM    : Double,           // raw meters
    val durationSec  : Double            // raw seconds
)

/**
 * NearbyPlace — a Point of Interest from Overpass API.
 */
data class NearbyPlace(
    val name             : String,
    val type             : String,   // "restaurant", "cafe", "shop (...)", etc.
    val lat              : Double,
    val lon              : Double,
    val distanceFromRoute: Float = 0f   // meters from nearest route point
) {
    /** Human-readable distance string shown in suggestion cards */
    fun distanceLabel(): String {
        return if (distanceFromRoute >= 1000) {
            "%.1f km from route".format(distanceFromRoute / 1000)
        } else {
            "${distanceFromRoute.toInt()} m from route"
        }
    }

    /** Emoji icon based on type */
    fun emoji(): String = when {
        type.contains("restaurant") || type.contains("food") -> "🍽️"
        type.contains("cafe") || type.contains("coffee")     -> "☕"
        type.contains("fast_food")                           -> "🍔"
        type.contains("bakery")                              -> "🥐"
        type.contains("bar")                                 -> "🍺"
        type.contains("shop")                                -> "🛍️"
        type.contains("supermarket")                         -> "🛒"
        else                                                 -> "📍"
    }
}

package com.nishantyadav.routesmart.model

data class Route(
    val source: String = "",
    val destination: String = "",
    val distance: String = "",
    val duration: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
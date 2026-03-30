package com.nishantyadav.routesmart.api

import com.nishantyadav.routesmart.model.RouteInfo
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.net.URL

/**
 * OsrmApi — Fetches driving routes using the free OSRM demo server.
 * Returns decoded polyline points + distance/duration text.
 *
 * ⚠️ Must be called from a background thread (IO dispatcher).
 * ⚠️ For production, host your own OSRM server (osrm-backend).
 */
object OsrmApi {

    private const val BASE_URL = "https://router.project-osrm.org/route/v1/driving"

    /**
     * Gets a route between two GeoPoints.
     * Returns RouteInfo with decoded points, distance, and duration.
     */
    fun getRoute(start: GeoPoint, end: GeoPoint): RouteInfo? {
        return try {
            val url = "$BASE_URL/${start.longitude},${start.latitude};${end.longitude},${end.latitude}" +
                    "?overview=full&geometries=polyline6"

            val connection = URL(url).openConnection().apply {
                setRequestProperty("User-Agent", "RouteSmart-App/1.0")
                connectTimeout = 10000
                readTimeout    = 10000
            }

            val response = connection.getInputStream().bufferedReader().readText()
            val json     = JSONObject(response)

            if (json.getString("code") != "Ok") return null

            val route    = json.getJSONArray("routes").getJSONObject(0)
            val geometry = route.getString("geometry")   // encoded polyline6
            val distance = route.getDouble("distance")   // meters
            val duration = route.getDouble("duration")   // seconds

            val points = decodePolyline6(geometry)

            RouteInfo(
                points       = points,
                distanceText = formatDistance(distance),
                durationText = formatDuration(duration),
                distanceM    = distance,
                durationSec  = duration
            )
        } catch (e: Exception) {
            android.util.Log.e("OsrmApi", "Route fetch failed: ${e.message}")
            null
        }
    }

    // ────────────────────────────────────────────────
    //  Polyline6 Decoder
    // ────────────────────────────────────────────────

    /**
     * Decodes a Google/OSRM encoded polyline (precision 6) into a list of GeoPoints.
     * Precision 6 uses 1e-6 instead of 1e-5.
     */
    private fun decodePolyline6(encoded: String): List<GeoPoint> {
        val points = mutableListOf<GeoPoint>()
        var index  = 0
        var lat    = 0
        var lng    = 0

        while (index < encoded.length) {
            // Decode latitude delta
            var result = 0
            var shift  = 0
            var b: Int
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            lat += if ((result and 1) != 0) (result shr 1).inv() else result shr 1

            // Decode longitude delta
            result = 0
            shift  = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            lng += if ((result and 1) != 0) (result shr 1).inv() else result shr 1

            points.add(GeoPoint(lat / 1_000_000.0, lng / 1_000_000.0))
        }
        return points
    }

    // ────────────────────────────────────────────────
    //  Formatting Helpers
    // ────────────────────────────────────────────────

    private fun formatDistance(meters: Double): String {
        return if (meters >= 1000) {
            "%.1f km".format(meters / 1000)
        } else {
            "${meters.toInt()} m"
        }
    }

    private fun formatDuration(seconds: Double): String {
        val mins  = (seconds / 60).toInt()
        val hours = mins / 60
        val remMins = mins % 60
        return if (hours > 0) "${hours}h ${remMins}m" else "${mins} min"
    }
}

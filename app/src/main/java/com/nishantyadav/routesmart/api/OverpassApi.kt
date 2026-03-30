package com.nishantyadav.routesmart.api

import com.nishantyadav.routesmart.model.NearbyPlace
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

/**
 * OverpassApi — Fetches nearby Points of Interest from OpenStreetMap.
 * Uses the free Overpass API (overpass-api.de).
 *
 * ⚠️ Must be called from a background thread (IO dispatcher).
 * ⚠️ Rate-limit: don't hammer this; use debouncing in the UI.
 */
object OverpassApi {

    private const val BASE_URL = "https://overpass-api.de/api/interpreter"

    /**
     * Fetch nearby restaurants, cafes, shops within [radiusMeters].
     * Returns a list of NearbyPlace objects.
     */
    fun fetchNearbyPOIs(
        lat: Double,
        lon: Double,
        radiusMeters: Int = 1000
    ): List<NearbyPlace> {
        return try {
            // Overpass QL query:
            // Fetches nodes tagged as amenity (food/cafe) or shop within radius
            val query = """
                [out:json][timeout:15];
                (
                  node["amenity"~"restaurant|cafe|fast_food|food_court|bar|bakery"](around:$radiusMeters,$lat,$lon);
                  node["shop"~"convenience|supermarket|mall|clothes|electronics"](around:$radiusMeters,$lat,$lon);
                );
                out body 40;
            """.trimIndent()

            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "$BASE_URL?data=$encodedQuery"

            val connection = URL(url).openConnection().apply {
                setRequestProperty("User-Agent", "RouteSmart-App/1.0")
                connectTimeout = 15000
                readTimeout    = 15000
            }

            val response = connection.getInputStream().bufferedReader().readText()
            parseOverpassResponse(response)

        } catch (e: Exception) {
            android.util.Log.e("OverpassApi", "Overpass fetch failed: ${e.message}")
            emptyList()
        }
    }

    // ────────────────────────────────────────────────
    //  JSON Parser
    // ────────────────────────────────────────────────

    private fun parseOverpassResponse(json: String): List<NearbyPlace> {
        val places = mutableListOf<NearbyPlace>()
        val root   = JSONObject(json)
        val elements = root.getJSONArray("elements")

        for (i in 0 until elements.length()) {
            val el   = elements.getJSONObject(i)
            val tags = el.optJSONObject("tags") ?: continue

            val lat  = el.optDouble("lat", Double.NaN)
            val lon  = el.optDouble("lon", Double.NaN)
            if (lat.isNaN() || lon.isNaN()) continue

            // Get the best available name
            val name = tags.optString("name", "").ifBlank {
                tags.optString("brand", "Unnamed Place")
            }

            // Determine type
            val amenity = tags.optString("amenity", "")
            val shop    = tags.optString("shop", "")
            val type    = when {
                amenity.isNotBlank() -> amenity
                shop.isNotBlank()    -> "shop ($shop)"
                else                 -> "place"
            }

            places.add(
                NearbyPlace(
                    name             = name,
                    type             = type,
                    lat              = lat,
                    lon              = lon,
                    distanceFromRoute = 0f   // filled later in MapActivity
                )
            )
        }

        return places
    }
}

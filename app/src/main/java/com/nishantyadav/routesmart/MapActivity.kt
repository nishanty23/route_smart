package com.nishantyadav.routesmart

import android.Manifest
import android.animation.ObjectAnimator
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.nishantyadav.routesmart.api.NominatimApi
import com.nishantyadav.routesmart.api.OverpassApi
import com.nishantyadav.routesmart.api.OsrmApi
import com.nishantyadav.routesmart.model.NearbyPlace
import com.nishantyadav.routesmart.model.RouteInfo
import com.nishantyadav.routesmart.ui.PlaceSuggestionsAdapter
import com.google.android.gms.location.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.*
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.*
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import okhttp3.*
import java.io.IOException
import kotlin.math.*

// ── MapActivity: The main screen for navigation, tracking, and nearby places ──
class MapActivity : AppCompatActivity() {

    // ── Views ──
    private lateinit var mapView: MapView
    private lateinit var binding: com.example.routesmart.databinding.ActivityMapBinding

    // ── Location ──
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var currentLocation: GeoPoint? = null
    private var lastBearing: Float = 0f

    // ── Overlays ──
    private var routeOverlay: Polyline? = null
    private var userMarker: Marker? = null
    private var destinationMarker: Marker? = null
    private val poiMarkers = mutableListOf<Marker>()

    // ── State ──
    private var isNavigating = false
    private var currentRoute: List<GeoPoint> = emptyList()

    // ── Bottom Sheet ──
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    // ── Suggestions Adapter ──
    private lateinit var suggestionsAdapter: PlaceSuggestionsAdapter

    // ── HTTP Client ──
    private val httpClient = OkHttpClient()

    // ── Coroutine scope ──
    private val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 1001
        private const val TAG = "MapActivity"
        private const val DEFAULT_ZOOM = 15.0
    }

    // ────────────────────────────────────────────────
    //  Lifecycle
    // ────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // osmdroid needs this before setContentView
        Configuration.getInstance().load(this, getSharedPreferences("osm_pref", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName

        binding = com.example.routesmart.databinding.ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupMap()
        setupBottomSheet()
        setupSuggestionsRecycler()
        setupSearchInputs()
        setupButtons()
        setupLocationClient()
        requestLocationPermission()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        stopLocationUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel()
        mapView.onDetach()
    }

    // ────────────────────────────────────────────────
    //  Map Setup
    // ────────────────────────────────────────────────

    private fun setupMap() {
        mapView = binding.mapView

        // Show loader while tiles load
        showMapLoader(true)

        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(DEFAULT_ZOOM)

        // Enable rotation gesture
        val rotationGesture = RotationGestureOverlay(mapView)
        rotationGesture.isEnabled = true
        mapView.overlays.add(rotationGesture)

        // Default center (India)
        mapView.controller.setCenter(GeoPoint(20.5937, 78.9629))

        // Hide loader once tiles are ready
        mapView.addMapListener(object : org.osmdroid.events.MapListener {
            override fun onScroll(event: org.osmdroid.events.ScrollEvent?): Boolean = false
            override fun onZoom(event: org.osmdroid.events.ZoomEvent?): Boolean {
                showMapLoader(false)
                return false
            }
        })

        // Fallback: hide loader after 2 seconds
        Handler(Looper.getMainLooper()).postDelayed({ showMapLoader(false) }, 2000)
    }

    // ────────────────────────────────────────────────
    //  Bottom Sheet
    // ────────────────────────────────────────────────

    private fun setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheetBehavior.peekHeight = 220

        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {}
            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })
    }

    // ────────────────────────────────────────────────
    //  Suggestions RecyclerView
    // ────────────────────────────────────────────────

    private fun setupSuggestionsRecycler() {
        suggestionsAdapter = PlaceSuggestionsAdapter { place ->
            // When a suggestion is tapped, highlight it on map
            highlightPlaceOnMap(place)
        }
        binding.suggestionsRecycler.adapter = suggestionsAdapter
    }

    // ────────────────────────────────────────────────
    //  Search Inputs
    // ────────────────────────────────────────────────

    private fun setupSearchInputs() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateRouteButtonState()
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        binding.etStart.addTextChangedListener(watcher)
        binding.etDestination.addTextChangedListener(watcher)
    }

    private fun updateRouteButtonState() {
        val hasStart = binding.etStart.text.isNotBlank()
        val hasDest = binding.etDestination.text.isNotBlank()
        binding.btnGetRoute.isEnabled = hasStart && hasDest
        binding.btnGetRoute.alpha = if (hasStart && hasDest) 1f else 0.5f
    }

    // ────────────────────────────────────────────────
    //  Buttons
    // ────────────────────────────────────────────────

    private fun setupButtons() {
        updateRouteButtonState()

        binding.btnGetRoute.setOnClickListener {
            val start = binding.etStart.text.toString().trim()
            val destination = binding.etDestination.text.toString().trim()
            if (start.isNotEmpty() && destination.isNotEmpty()) {
                geocodeAndRoute(start, destination)
            }
        }

        binding.btnMyLocation.setOnClickListener {
            currentLocation?.let {
                mapView.controller.animateTo(it)
                mapView.controller.setZoom(16.0)
            } ?: Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show()
        }

        binding.btnStartNavigation.setOnClickListener {
            if (!isNavigating) startNavigation() else stopNavigation()
        }

        binding.btnNearby.setOnClickListener {
            currentLocation?.let { loc ->
                fetchNearbyPlaces(loc.latitude, loc.longitude)
            } ?: Toast.makeText(this, "Getting your location…", Toast.LENGTH_SHORT).show()
        }
    }

    // ────────────────────────────────────────────────
    //  Geocoding → Routing
    // ────────────────────────────────────────────────

    private fun geocodeAndRoute(startQuery: String, destQuery: String) {
        showRoutingLoader(true)
        clearMapOverlays()

        activityScope.launch {
            try {
                // Geocode both locations in parallel
                val startDeferred = async(Dispatchers.IO) { NominatimApi.geocode(startQuery) }
                val destDeferred  = async(Dispatchers.IO) { NominatimApi.geocode(destQuery)  }

                val startPoint = startDeferred.await()
                val destPoint  = destDeferred.await()

                if (startPoint == null) {
                    showError("Could not find \"$startQuery\". Try a more specific name.")
                    showRoutingLoader(false)
                    return@launch
                }
                if (destPoint == null) {
                    showError("Could not find \"$destQuery\". Try a more specific name.")
                    showRoutingLoader(false)
                    return@launch
                }

                // Fetch route from OSRM
                val routeInfo = withContext(Dispatchers.IO) {
                    OsrmApi.getRoute(startPoint, destPoint)
                }

                if (routeInfo == null) {
                    showError("Route not found. Check your inputs.")
                    showRoutingLoader(false)
                    return@launch
                }

                // Draw on map
                drawRoute(routeInfo, startPoint, destPoint)
                showRouteInfo(routeInfo)

                // Fetch suggestions along route
                fetchSuggestionsAlongRoute(routeInfo.points)

                showRoutingLoader(false)

            } catch (e: Exception) {
                Log.e(TAG, "Route error: ${e.message}")
                showError("Network error. Please check your connection.")
                showRoutingLoader(false)
            }
        }
    }

    // ────────────────────────────────────────────────
    //  Draw Route on Map
    // ────────────────────────────────────────────────

    private fun drawRoute(routeInfo: RouteInfo, start: GeoPoint, dest: GeoPoint) {
        currentRoute = routeInfo.points

        // Draw the polyline
        routeOverlay = Polyline().apply {
            setPoints(routeInfo.points)
            outlinePaint.color = ContextCompat.getColor(this@MapActivity, R.color.route_blue)
            outlinePaint.strokeWidth = 14f
            outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
            outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
        }
        mapView.overlays.add(routeOverlay)

        // Start marker
        addMapMarker(start, "Start", R.drawable.ic_marker_start)

        // Destination marker
        destinationMarker = addMapMarker(dest, "Destination", R.drawable.ic_marker_end)

        // Zoom to fit route
        val bounds = org.osmdroid.util.BoundingBox.fromGeoPoints(routeInfo.points)
        mapView.zoomToBoundingBox(bounds.increaseByScale(1.2f), true)

        mapView.invalidate()
    }

    // ────────────────────────────────────────────────
    //  Route Info in Bottom Sheet
    // ────────────────────────────────────────────────

    private fun showRouteInfo(routeInfo: RouteInfo) {
        binding.tvDistance.text = routeInfo.distanceText
        binding.tvDuration.text = routeInfo.durationText
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        binding.btnStartNavigation.visibility = View.VISIBLE
    }

    // ────────────────────────────────────────────────
    //  Nearby Places (Overpass API)
    // ────────────────────────────────────────────────

    private fun fetchNearbyPlaces(lat: Double, lon: Double) {
        showNearbyLoader(true)
        activityScope.launch {
            try {
                val places = withContext(Dispatchers.IO) {
                    OverpassApi.fetchNearbyPOIs(lat, lon, radiusMeters = 1000)
                }
                clearPOIMarkers()
                places.forEach { place -> addPOIMarker(place) }
                mapView.invalidate()

                if (places.isEmpty()) {
                    Toast.makeText(this@MapActivity, "No nearby places found", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MapActivity, "${places.size} places found nearby", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Overpass error: ${e.message}")
                showError("Failed to load nearby places")
            } finally {
                showNearbyLoader(false)
            }
        }
    }

    // ────────────────────────────────────────────────
    //  Smart Suggestions Along Route
    // ────────────────────────────────────────────────

    private fun fetchSuggestionsAlongRoute(routePoints: List<GeoPoint>) {
        if (routePoints.isEmpty()) return

        activityScope.launch {
            try {
                // Sample a few points along the route (every ~500m equivalent)
                val sampledPoints = sampleRoutePoints(routePoints, maxSamples = 5)

                val allPlaces = mutableListOf<NearbyPlace>()
                for (point in sampledPoints) {
                    val places = withContext(Dispatchers.IO) {
                        OverpassApi.fetchNearbyPOIs(point.latitude, point.longitude, radiusMeters = 300)
                    }
                    allPlaces.addAll(places)
                }

                // Deduplicate by name
                val unique = allPlaces.distinctBy { it.name }.take(20)

                // Calculate distance from route for each place
                val withDistance = unique.map { place ->
                    val distFromRoute = minDistanceFromRoute(
                        GeoPoint(place.lat, place.lon), routePoints
                    )
                    place.copy(distanceFromRoute = distFromRoute)
                }.sortedBy { it.distanceFromRoute }

                // Show in RecyclerView
                suggestionsAdapter.submitList(withDistance)

                if (withDistance.isNotEmpty()) {
                    binding.suggestionsLabel.visibility = View.VISIBLE
                    binding.suggestionsRecycler.visibility = View.VISIBLE
                }

            } catch (e: Exception) {
                Log.e(TAG, "Suggestions error: ${e.message}")
            }
        }
    }

    /** Sample N evenly-spaced points from the route */
    private fun sampleRoutePoints(points: List<GeoPoint>, maxSamples: Int): List<GeoPoint> {
        if (points.size <= maxSamples) return points
        val step = points.size / maxSamples
        return (0 until maxSamples).map { i -> points[i * step] }
    }

    /** Minimum distance (meters) from a point to any segment on the route */
    private fun minDistanceFromRoute(point: GeoPoint, route: List<GeoPoint>): Float {
        return route.minOf { rp ->
            val result = FloatArray(1)
            Location.distanceBetween(point.latitude, point.longitude, rp.latitude, rp.longitude, result)
            result[0]
        }
    }

    // ────────────────────────────────────────────────
    //  Highlight Place on Map
    // ────────────────────────────────────────────────

    private fun highlightPlaceOnMap(place: NearbyPlace) {
        val point = GeoPoint(place.lat, place.lon)
        mapView.controller.animateTo(point)
        mapView.controller.setZoom(17.0)

        // Find and pulse the marker
        poiMarkers.firstOrNull { marker ->
            marker.title == place.name
        }?.let { marker ->
            // Simple bounce animation on icon
            ObjectAnimator.ofFloat(marker.icon as? View, "scaleX", 1f, 1.4f, 1f).apply {
                duration = 400
                start()
            }
        }
        mapView.invalidate()
    }

    // ────────────────────────────────────────────────
    //  POI Markers
    // ────────────────────────────────────────────────

    private fun addPOIMarker(place: NearbyPlace) {
        val marker = Marker(mapView).apply {
            position = GeoPoint(place.lat, place.lon)
            title    = place.name
            snippet  = place.type.replaceFirstChar { it.uppercase() }
            icon     = getPoiIcon(place.type)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        poiMarkers.add(marker)
        mapView.overlays.add(marker)
    }

    private fun clearPOIMarkers() {
        poiMarkers.forEach { mapView.overlays.remove(it) }
        poiMarkers.clear()
    }

    private fun getPoiIcon(type: String): android.graphics.drawable.Drawable {
        val iconRes = when {
            type.contains("restaurant") || type.contains("food") -> R.drawable.ic_poi_food
            type.contains("cafe") || type.contains("coffee")     -> R.drawable.ic_poi_cafe
            type.contains("shop") || type.contains("store")      -> R.drawable.ic_poi_shop
            else -> R.drawable.ic_poi_default
        }
        return ContextCompat.getDrawable(this, iconRes)
            ?: ContextCompat.getDrawable(this, R.drawable.ic_poi_default)!!
    }

    // ────────────────────────────────────────────────
    //  Generic Marker Helper
    // ────────────────────────────────────────────────

    private fun addMapMarker(point: GeoPoint, label: String, iconRes: Int): Marker {
        return Marker(mapView).apply {
            position = point
            title    = label
            icon     = ContextCompat.getDrawable(this@MapActivity, iconRes)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            mapView.overlays.add(this)
        }
    }

    // ────────────────────────────────────────────────
    //  Live Navigation (GPS Arrow)
    // ────────────────────────────────────────────────

    private fun setupLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    onNewLocation(location)
                }
            }
        }
    }

    private fun startLocationUpdates() {
        if (!hasLocationPermission()) return

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1500L)
            .setMinUpdateDistanceMeters(3f)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission denied: ${e.message}")
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    /** Called for every GPS fix */
    private fun onNewLocation(location: Location) {
        val newPoint = GeoPoint(location.latitude, location.longitude)
        val bearing  = location.bearing

        currentLocation = newPoint

        if (isNavigating) {
            updateNavigationArrow(newPoint, bearing)
            // Keep map centered on user
            mapView.controller.animateTo(newPoint)
        }

        // Auto-fill "Start" if empty
        if (binding.etStart.text.isBlank()) {
            binding.etStart.setText("My Location")
        }
    }

    // ────────────────────────────────────────────────
    //  Navigation Arrow (moving, rotating marker)
    // ────────────────────────────────────────────────

    private fun updateNavigationArrow(position: GeoPoint, bearing: Float) {
        if (userMarker == null) {
            userMarker = Marker(mapView).apply {
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                mapView.overlays.add(this)
            }
        }

        userMarker?.apply {
            this.position = position
            this.icon     = createRotatedArrow(bearing)
            this.rotation = 0f  // rotation already baked into bitmap
        }

        lastBearing = bearing
        mapView.invalidate()
    }

    /** Creates a rotated arrow bitmap for the navigation marker */
    private fun createRotatedArrow(bearing: Float): android.graphics.drawable.BitmapDrawable {
        val original = BitmapFactory.decodeResource(resources, R.drawable.ic_navigation_arrow)
        val size = 80
        val scaled = Bitmap.createScaledBitmap(original, size, size, true)

        val matrix = Matrix().apply { postRotate(bearing, size / 2f, size / 2f) }
        val rotated = Bitmap.createBitmap(scaled, 0, 0, size, size, matrix, true)

        return android.graphics.drawable.BitmapDrawable(resources, rotated)
    }

    // ────────────────────────────────────────────────
    //  Start / Stop Navigation
    // ────────────────────────────────────────────────

    private fun startNavigation() {
        isNavigating = true
        binding.btnStartNavigation.text = "Stop Navigation"
        binding.btnStartNavigation.setBackgroundColor(ContextCompat.getColor(this, R.color.error_red))
        startLocationUpdates()
        Toast.makeText(this, "Navigation started", Toast.LENGTH_SHORT).show()
    }

    private fun stopNavigation() {
        isNavigating = false
        binding.btnStartNavigation.text = "Start Navigation"
        binding.btnStartNavigation.setBackgroundColor(ContextCompat.getColor(this, R.color.route_blue))
        stopLocationUpdates()
        userMarker?.let { mapView.overlays.remove(it) }
        userMarker = null
        mapView.invalidate()
    }

    // ────────────────────────────────────────────────
    //  Map Cleanup
    // ────────────────────────────────────────────────

    private fun clearMapOverlays() {
        routeOverlay?.let { mapView.overlays.remove(it) }
        routeOverlay = null
        destinationMarker?.let { mapView.overlays.remove(it) }
        destinationMarker = null
        clearPOIMarkers()
        currentRoute = emptyList()
        suggestionsAdapter.submitList(emptyList())
        binding.suggestionsLabel.visibility = View.GONE
        binding.suggestionsRecycler.visibility = View.GONE
        mapView.invalidate()
    }

    // ────────────────────────────────────────────────
    //  UI State Helpers
    // ────────────────────────────────────────────────

    private fun showMapLoader(show: Boolean) {
        binding.mapLoader.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showRoutingLoader(show: Boolean) {
        binding.routingLoader.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnGetRoute.isEnabled   = !show
    }

    private fun showNearbyLoader(show: Boolean) {
        binding.nearbyLoader.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnNearby.isEnabled     = !show
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    // ────────────────────────────────────────────────
    //  Permissions
    // ────────────────────────────────────────────────

    private fun requestLocationPermission() {
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
        } else {
            onPermissionGranted()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST &&
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            onPermissionGranted()
        } else {
            Toast.makeText(this, "Location permission needed for live tracking", Toast.LENGTH_LONG).show()
        }
    }

    private fun onPermissionGranted() {
        // Get last known location to center map
        if (hasLocationPermission()) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    currentLocation = GeoPoint(it.latitude, it.longitude)
                    mapView.controller.animateTo(currentLocation)
                    mapView.controller.setZoom(DEFAULT_ZOOM)
                }
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    }
}

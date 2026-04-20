package com.nishantyadav.routesmart

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.nishantyadav.routesmart.api.NominatimApi
import com.nishantyadav.routesmart.api.OsrmApi
import com.nishantyadav.routesmart.api.OverpassApi
import com.nishantyadav.routesmart.databinding.ActivityMapBinding
import com.nishantyadav.routesmart.model.NearbyPlace
import com.nishantyadav.routesmart.model.RouteInfo
import com.nishantyadav.routesmart.ui.PlaceSuggestionsAdapter
import kotlinx.coroutines.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import kotlin.math.abs
import kotlin.math.max

class MapActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var binding: ActivityMapBinding

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var currentLocation: GeoPoint? = null
    private var currentLocationRaw: Location? = null
    private var lastBearing: Float = 0f

    private var routeOverlay: Polyline? = null
    private var userMarker: Marker? = null
    private var destinationMarker: Marker? = null
    private val poiMarkers = mutableListOf<Marker>()

    private var isNavigating = false
    private var currentRoute: List<GeoPoint> = emptyList()

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var suggestionsAdapter: PlaceSuggestionsAdapter

    private val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var lastRecommendationFetchLocation: Location? = null
    private var liveNearbyPlaces: List<NearbyPlace> = emptyList()

    private val voiceLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val matches = result.data
                    ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    ?: arrayListOf()

                val spokenText = matches.firstOrNull().orEmpty()
                if (spokenText.isNotBlank()) {
                    handleAssistantQuery(spokenText, fromVoice = true)
                } else {
                    Toast.makeText(this, "No voice input detected", Toast.LENGTH_SHORT).show()
                }
            }
        }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 1001
        private const val TAG = "MapActivity"
        private const val DEFAULT_ZOOM = 15.0
        private const val LIVE_RECOMMENDATION_RADIUS_M = 15000
        private const val LIVE_RECOMMENDATION_REFRESH_DISTANCE_M = 500f
        private const val ROUTE_SAMPLE_COUNT = 8
        private const val ROUTE_POI_RADIUS_M = 1500
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val chatbotBtn = findViewById<ImageButton>(R.id.btnChatbot)

        chatbotBtn?.let {
            ObjectAnimator.ofFloat(it, "scaleX", 1f, 1.1f, 1f).apply {
                duration = 1200
                repeatCount = ValueAnimator.INFINITE
                start()
            }

            ObjectAnimator.ofFloat(it, "scaleY", 1f, 1.1f, 1f).apply {
                duration = 1200
                repeatCount = ValueAnimator.INFINITE
                start()
            }
        }

// Pulse animation
        ObjectAnimator.ofFloat(chatbotBtn, "scaleX", 1f, 1.1f, 1f).apply {
            duration = 1200
            repeatCount = ValueAnimator.INFINITE
            start()
        }

        ObjectAnimator.ofFloat(chatbotBtn, "scaleY", 1f, 1.1f, 1f).apply {
            duration = 1200
            repeatCount = ValueAnimator.INFINITE
            start()
        }

        Configuration.getInstance().load(this, getSharedPreferences("osm_pref", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName

        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupMap()
        setupBottomSheet()
        setupSuggestionsRecycler()
        setupSearchInputs()
        setupButtons()
        setupOptionalAssistantButtons()
        setupLocationClient()
        requestLocationPermission()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        if (hasLocationPermission()) {
            startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        stopLocationUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel()
        stopLocationUpdates()
        mapView.onDetach()
    }

    private fun setupMap() {
        mapView = binding.mapView

        showMapLoader(true)

        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(DEFAULT_ZOOM)

        val rotationGesture = RotationGestureOverlay(mapView)
        rotationGesture.isEnabled = true
        mapView.overlays.add(rotationGesture)

        mapView.controller.setCenter(GeoPoint(20.5937, 78.9629))

        mapView.addMapListener(object : org.osmdroid.events.MapListener {
            override fun onScroll(event: org.osmdroid.events.ScrollEvent?): Boolean = false
            override fun onZoom(event: org.osmdroid.events.ZoomEvent?): Boolean {
                showMapLoader(false)
                return false
            }
        })

        Handler(Looper.getMainLooper()).postDelayed({ showMapLoader(false) }, 2000)
    }

    private fun setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheetBehavior.peekHeight = 220
    }

    private fun setupSuggestionsRecycler() {
        suggestionsAdapter = PlaceSuggestionsAdapter { place ->
            highlightPlaceOnMap(place)
        }
        binding.suggestionsRecycler.adapter = suggestionsAdapter
    }

    private fun setupSearchInputs() {
        val watcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateRouteButtonState()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
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
                fetchLiveRecommendations(loc.latitude, loc.longitude, force = true)
            } ?: Toast.makeText(this, "Getting your location…", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupOptionalAssistantButtons() {
        findViewByOptionalId<View>("btnVoiceAssistant")?.setOnClickListener {
            startVoiceAssistant()
        }

        findViewByOptionalId<View>("btnChatbot")?.setOnClickListener {
            showChatbotDialog()
        }
    }

    private fun geocodeAndRoute(startQuery: String, destQuery: String) {
        showRoutingLoader(true)
        clearMapOverlays()

        activityScope.launch {
            try {
                val startDeferred = async(Dispatchers.IO) { NominatimApi.geocode(startQuery) }
                val destDeferred = async(Dispatchers.IO) { NominatimApi.geocode(destQuery) }

                val startPoint = startDeferred.await()
                val destPoint = destDeferred.await()

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

                val routeInfo = withContext(Dispatchers.IO) {
                    OsrmApi.getRoute(startPoint, destPoint)
                }

                if (routeInfo == null) {
                    showError("Route not found. Check your inputs.")
                    showRoutingLoader(false)
                    return@launch
                }

                drawRoute(routeInfo, startPoint, destPoint)
                showRouteInfo(routeInfo)
                fetchSuggestionsAlongRoute(routeInfo.points)

                currentLocation?.let { loc ->
                    fetchLiveRecommendations(loc.latitude, loc.longitude, force = true)
                }

                showRoutingLoader(false)

            } catch (e: Exception) {
                Log.e(TAG, "Route error: ${e.message}", e)
                showError("Network error. Please check your connection.")
                showRoutingLoader(false)
            }
        }
    }

    private fun drawRoute(routeInfo: RouteInfo, start: GeoPoint, dest: GeoPoint) {
        currentRoute = routeInfo.points

        routeOverlay = Polyline().apply {
            setPoints(routeInfo.points)
            outlinePaint.color = ContextCompat.getColor(this@MapActivity, R.color.route_blue)
            outlinePaint.strokeWidth = 14f
            outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
            outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
        }
        mapView.overlays.add(routeOverlay)

        addMapMarker(start, "Start", R.drawable.ic_marker_start)
        destinationMarker = addMapMarker(dest, "Destination", R.drawable.ic_marker_end)

        val bounds = BoundingBox.fromGeoPoints(routeInfo.points)
        mapView.zoomToBoundingBox(bounds.increaseByScale(1.2f), true)
        mapView.invalidate()
    }

    private fun showRouteInfo(routeInfo: RouteInfo) {
        binding.tvDistance.text = routeInfo.distanceText
        binding.tvDuration.text = routeInfo.durationText
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        binding.btnStartNavigation.visibility = View.VISIBLE
    }

    private fun fetchLiveRecommendations(lat: Double, lon: Double, force: Boolean = false) {
        showNearbyLoader(true)

        activityScope.launch {
            try {
                val current = Location("live").apply {
                    latitude = lat
                    longitude = lon
                }

                val shouldFetch = force ||
                        lastRecommendationFetchLocation == null ||
                        current.distanceTo(lastRecommendationFetchLocation!!) >= LIVE_RECOMMENDATION_REFRESH_DISTANCE_M

                if (!shouldFetch) {
                    showNearbyLoader(false)
                    return@launch
                }

                lastRecommendationFetchLocation = current

                val places = withContext(Dispatchers.IO) {
                    OverpassApi.fetchNearbyPOIs(
                        lat = lat,
                        lon = lon,
                        radiusMeters = LIVE_RECOMMENDATION_RADIUS_M
                    )
                }

                val ranked = rankNearbyPlaces(places, currentRoute, lat, lon)
                    .distinctBy { "${it.name}_${it.lat}_${it.lon}" }
                    .take(20)

                liveNearbyPlaces = ranked

                clearPOIMarkers()
                ranked.forEach { addPOIMarker(it) }
                suggestionsAdapter.submitList(ranked)

                binding.suggestionsLabel.visibility =
                    if (ranked.isNotEmpty()) View.VISIBLE else View.GONE
                binding.suggestionsRecycler.visibility =
                    if (ranked.isNotEmpty()) View.VISIBLE else View.GONE

                updateSmartSuggestion(ranked)

                if (ranked.isEmpty() && force) {
                    Toast.makeText(this@MapActivity, "No nearby places found", Toast.LENGTH_SHORT).show()
                }

                mapView.invalidate()

            } catch (e: Exception) {
                Log.e(TAG, "Live recommendations error: ${e.message}", e)
                if (force) showError("Failed to load nearby recommendations")
            } finally {
                showNearbyLoader(false)
            }
        }
    }

    private fun fetchSuggestionsAlongRoute(routePoints: List<GeoPoint>) {
        if (routePoints.isEmpty()) return

        activityScope.launch {
            try {
                val sampledPoints = sampleRoutePoints(routePoints, ROUTE_SAMPLE_COUNT)

                val allPlaces = mutableListOf<NearbyPlace>()

                for (point in sampledPoints) {
                    val places = withContext(Dispatchers.IO) {
                        OverpassApi.fetchNearbyPOIs(
                            point.latitude,
                            point.longitude,
                            radiusMeters = ROUTE_POI_RADIUS_M
                        )
                    }
                    allPlaces.addAll(places)
                }

                val unique = allPlaces.distinctBy { "${it.name}_${it.lat}_${it.lon}" }

                val withDistance = unique.map { place ->
                    val distFromRoute = minDistanceFromRoute(
                        GeoPoint(place.lat, place.lon),
                        routePoints
                    )
                    place.copy(distanceFromRoute = distFromRoute)
                }

                val ranked = rankRoutePlaces(withDistance).take(20)

                suggestionsAdapter.submitList(ranked)

                if (ranked.isNotEmpty()) {
                    binding.suggestionsLabel.visibility = View.VISIBLE
                    binding.suggestionsRecycler.visibility = View.VISIBLE
                }

                updateSmartSuggestion(ranked)

            } catch (e: Exception) {
                Log.e(TAG, "Suggestions error: ${e.message}", e)
            }
        }
    }

    private fun rankNearbyPlaces(
        places: List<NearbyPlace>,
        routePoints: List<GeoPoint>,
        userLat: Double,
        userLon: Double
    ): List<NearbyPlace> {
        return places.map { place ->
            val result = FloatArray(1)
            Location.distanceBetween(userLat, userLon, place.lat, place.lon, result)
            val distanceFromUser = result[0]

            val distFromRoute = if (routePoints.isNotEmpty()) {
                minDistanceFromRoute(GeoPoint(place.lat, place.lon), routePoints)
            } else {
                distanceFromUser
            }

            place.copy(distanceFromRoute = distFromRoute)
        }.sortedWith(
            compareBy<NearbyPlace> {
                smartCategoryPriority(it.type)
            }.thenBy {
                distanceFromCurrentLocation(it)
            }.thenBy {
                it.distanceFromRoute
            }
        )
    }

    private fun rankRoutePlaces(places: List<NearbyPlace>): List<NearbyPlace> {
        return places.sortedWith(
            compareBy<NearbyPlace> {
                smartCategoryPriority(it.type)
            }.thenBy {
                it.distanceFromRoute
            }
        )
    }

    private fun smartCategoryPriority(type: String): Int {
        val t = type.lowercase()
        return when {
            t.contains("hospital") || t.contains("supermarket") || t.contains("shop") || t.contains("hotel") || t.contains("cafe") || t.contains("coffee") || t.contains("restaurant") || t.contains("fast_food") || t.contains("atm") || t.contains("police") || t.contains("pharmacy") || t.contains("fuel") || t.contains("charging") -> 1
            //t.contains("pharmacy") -> 2
            //t.contains("police") -> 3
            //t.contains("fuel") || t.contains("charging") -> 4
            //t.contains("atm") -> 5
            //t.contains("restaurant") || t.contains("fast_food") -> 6
            //t.contains("cafe") || t.contains("coffee") -> 7
            //t.contains("hotel") -> 8
            //t.contains("supermarket") || t.contains("shop") -> 9
            else -> 2
        }
    }

    private fun updateSmartSuggestion(places: List<NearbyPlace>) {
        val text = buildSmartSuggestionText(places) ?: return

        findViewByOptionalId<TextView>("tvSmartSuggestion")?.text = text

        // fallback toast only when smart suggestion TextView isn't added yet
        if (findViewByOptionalId<TextView>("tvSmartSuggestion") == null) {
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildSmartSuggestionText(places: List<NearbyPlace>): String? {
        if (places.isEmpty()) return null

        val top = places.first()
        val distanceLabel = distanceFromCurrentLocation(top).toFriendlyDistance()

        return when {
            top.type.contains("fuel", true) ->
                "Smart stop: Fuel station nearby, about $distanceLabel away."
            top.type.contains("hospital", true) ->
                "Safety stop: Hospital available nearby, about $distanceLabel away."
            top.type.contains("pharmacy", true) ->
                "Helpful stop: Pharmacy nearby, about $distanceLabel away."
            top.type.contains("atm", true) ->
                "Convenience stop: ATM available nearby, about $distanceLabel away."
            top.type.contains("cafe", true) ->
                "Break suggestion: Café nearby, about $distanceLabel away."
            top.type.contains("restaurant", true) || top.type.contains("food", true) ->
                "Meal stop: Food place nearby, about $distanceLabel away."
            else ->
                "Suggested stop: ${top.name} is nearby, about $distanceLabel away."
        }
    }

    private fun sampleRoutePoints(points: List<GeoPoint>, maxSamples: Int): List<GeoPoint> {
        if (points.isEmpty()) return emptyList()
        if (points.size <= maxSamples) return points

        val step = max(1, points.size / maxSamples)
        val sampled = mutableListOf<GeoPoint>()

        var index = 0
        while (index < points.size) {
            sampled.add(points[index])
            index += step
        }

        if (sampled.last() != points.last()) {
            sampled.add(points.last())
        }

        return sampled.distinct()
    }

    private fun minDistanceFromRoute(point: GeoPoint, route: List<GeoPoint>): Float {
        return route.minOf { routePoint ->
            val result = FloatArray(1)
            Location.distanceBetween(
                point.latitude,
                point.longitude,
                routePoint.latitude,
                routePoint.longitude,
                result
            )
            result[0]
        }
    }

    private fun highlightPlaceOnMap(place: NearbyPlace) {
        val point = GeoPoint(place.lat, place.lon)
        mapView.controller.animateTo(point)
        mapView.controller.setZoom(17.0)
        mapView.invalidate()
    }

    private fun addPOIMarker(place: NearbyPlace) {
        val marker = Marker(mapView).apply {
            position = GeoPoint(place.lat, place.lon)
            title = place.name
            snippet = buildMarkerSnippet(place)
            icon = getPoiIcon(place.type)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }

        poiMarkers.add(marker)
        mapView.overlays.add(marker)
    }

    private fun buildMarkerSnippet(place: NearbyPlace): String {
        val desc = buildPlaceDescription(place)
        val distance = distanceFromCurrentLocation(place).toFriendlyDistance()
        return "${place.type.replaceFirstChar { it.uppercase() }} • $distance\n$desc"
    }

    private fun buildPlaceDescription(place: NearbyPlace): String {
        val t = place.type.lowercase()
        return when {
            t.contains("hospital") -> "Medical help and emergency support available here."
            t.contains("pharmacy") -> "Useful for medicines and first-aid needs."
            t.contains("police") -> "Safety and emergency assistance point."
            t.contains("fuel") -> "Good stop for fuel during travel."
            t.contains("charging") -> "Useful charging stop for electric vehicles."
            t.contains("atm") -> "Cash withdrawal point nearby."
            t.contains("restaurant") -> "Good option for meals during the trip."
            t.contains("fast_food") -> "Quick bite stop for faster travel breaks."
            t.contains("cafe") -> "Nice place for a tea or coffee break."
            t.contains("supermarket") || t.contains("shop") -> "Useful for snacks and travel essentials."
            t.contains("hotel") -> "Stay option for longer journeys."
            else -> "Recommended nearby stop for your route."
        }
    }

    private fun clearPOIMarkers() {
        poiMarkers.forEach { mapView.overlays.remove(it) }
        poiMarkers.clear()
    }

    private fun getPoiIcon(type: String): android.graphics.drawable.Drawable {
        val lower = type.lowercase()

        val iconRes = when {
            lower.contains("restaurant") || lower.contains("food") || lower.contains("fast_food") -> R.drawable.ic_poi_food
            lower.contains("cafe") || lower.contains("coffee") -> R.drawable.ic_poi_cafe
            lower.contains("shop") || lower.contains("supermarket") || lower.contains("store") -> R.drawable.ic_poi_shop
            else -> R.drawable.ic_poi_default
        }

        return ContextCompat.getDrawable(this, iconRes)
            ?: ContextCompat.getDrawable(this, R.drawable.ic_poi_default)!!
    }

    private fun addMapMarker(point: GeoPoint, label: String, iconRes: Int): Marker {
        return Marker(mapView).apply {
            position = point
            title = label
            icon = ContextCompat.getDrawable(this@MapActivity, iconRes)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            mapView.overlays.add(this)
        }
    }

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

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L
        )
            .setMinUpdateIntervalMillis(3000L)
            .setMinUpdateDistanceMeters(20f)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission denied: ${e.message}", e)
        }
    }

    private fun stopLocationUpdates() {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (_: Exception) {
        }
    }

    private fun onNewLocation(location: Location) {
        val newPoint = GeoPoint(location.latitude, location.longitude)
        val bearing = location.bearing

        currentLocation = newPoint
        currentLocationRaw = location

        if (isNavigating) {
            updateNavigationArrow(newPoint, bearing)
            mapView.controller.animateTo(newPoint)
        }

        if (binding.etStart.text.isBlank()) {
            binding.etStart.setText("My Location")
        }

        // keep live recommendations updated while moving
        fetchLiveRecommendations(location.latitude, location.longitude, force = false)
    }

    private fun updateNavigationArrow(position: GeoPoint, bearing: Float) {
        if (userMarker == null) {
            userMarker = Marker(mapView).apply {
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                mapView.overlays.add(this)
            }
        }

        userMarker?.apply {
            this.position = position
            this.icon = createRotatedArrow(bearing)
            this.rotation = 0f
        }

        lastBearing = bearing
        mapView.invalidate()
    }

    private fun createRotatedArrow(bearing: Float): android.graphics.drawable.BitmapDrawable {
        val original = BitmapFactory.decodeResource(resources, R.drawable.img_2)
        val size = 80
        val scaled = Bitmap.createScaledBitmap(original, size, size, true)

        val matrix = Matrix().apply {
            postRotate(bearing, size / 2f, size / 2f)
        }

        val rotated = Bitmap.createBitmap(scaled, 0, 0, size, size, matrix, true)
        return android.graphics.drawable.BitmapDrawable(resources, rotated)
    }

    private fun startNavigation() {
        isNavigating = true
        binding.btnStartNavigation.text = "Stop Navigation"
        binding.btnStartNavigation.setBackgroundColor(
            ContextCompat.getColor(this, R.color.error_red)
        )
        startLocationUpdates()
        Toast.makeText(this, "Navigation started", Toast.LENGTH_SHORT).show()
    }

    private fun stopNavigation() {
        isNavigating = false
        binding.btnStartNavigation.text = "Start Navigation"
        binding.btnStartNavigation.setBackgroundColor(
            ContextCompat.getColor(this, R.color.route_blue)
        )
        stopLocationUpdates()
        userMarker?.let { mapView.overlays.remove(it) }
        userMarker = null
        mapView.invalidate()
    }

    private fun clearMapOverlays() {
        routeOverlay?.let { mapView.overlays.remove(it) }
        routeOverlay = null

        destinationMarker?.let { mapView.overlays.remove(it) }
        destinationMarker = null

        clearPOIMarkers()
        currentRoute = emptyList()
        liveNearbyPlaces = emptyList()

        suggestionsAdapter.submitList(emptyList())
        binding.suggestionsLabel.visibility = View.GONE
        binding.suggestionsRecycler.visibility = View.GONE

        mapView.invalidate()
    }

    private fun startVoiceAssistant() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask about nearby places or safety")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            voiceLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Voice recognition not available on this device", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showChatbotDialog() {
        val input = EditText(this).apply {
            hint = "Ask about hospitals, cafes, ATMs, fuel, safety..."
            setPadding(40, 30, 40, 30)
        }

        AlertDialog.Builder(this)
            .setTitle("RouteSmart Assistant")
            .setView(input)
            .setPositiveButton("Ask") { _, _ ->
                val query = input.text.toString().trim()
                if (query.isNotBlank()) {
                    handleAssistantQuery(query, fromVoice = false)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun handleAssistantQuery(query: String, fromVoice: Boolean) {
        val lower = query.lowercase()

        val response = when {

            lower.contains("hospital") ->
                chatbotPlaceResponse("hospital", "hospital")

            lower.contains("pharmacy") || lower.contains("medicine") ->
                chatbotPlaceResponse("pharmacy", "pharmacy")

            lower.contains("police") || lower.contains("safety") -> {
                val place = chatbotPlaceResponse("police", "police")
                val tip = "Safety tip: Stay on main roads, keep phone charged, and share location."
                "$place\n\n$tip"
            }

            lower.contains("fuel") || lower.contains("petrol") || lower.contains("gas") ->
                chatbotPlaceResponse("fuel", "fuel station")

            lower.contains("charging") || lower.contains("ev") ->
                chatbotPlaceResponse("charging", "charging station")

            lower.contains("atm") || lower.contains("cash") ->
                chatbotPlaceResponse("atm", "ATM")

            lower.contains("restaurant") || lower.contains("food") ->
                chatbotPlaceResponse("restaurant", "restaurant")

            lower.contains("cafe") || lower.contains("coffee") || lower.contains("tea") ->
                chatbotPlaceResponse("cafe", "cafe")

            lower.contains("hotel") || lower.contains("stay") ->
                chatbotPlaceResponse("hotel", "hotel")

            lower.contains("shop") || lower.contains("market") ->
                chatbotPlaceResponse("shop", "shop")

            lower.contains("near me") || lower.contains("nearby") ->
                generalNearbyResponse()

            else ->
                "I can help with hospitals, fuel, ATMs, cafes, food, hotels, shops, and safety."
        }

        AlertDialog.Builder(this)
            .setTitle(if (fromVoice) "Voice Assistant" else "RouteSmart Assistant")
            .setMessage(response)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun generalNearbyResponse(): String {
        if (liveNearbyPlaces.isEmpty()) {
            return "I do not have fresh nearby recommendations yet. Please wait for GPS or tap Nearby."
        }

        val top = liveNearbyPlaces.take(5)
        val lines = top.mapIndexed { index, place ->
            "${index + 1}. ${place.name} (${place.type}) - ${distanceFromCurrentLocation(place).toFriendlyDistance()}"
        }

        return "Top nearby recommendations:\n\n" + lines.joinToString("\n")
    }

    private fun distanceFromCurrentLocation(place: NearbyPlace): Float {
        val current = currentLocationRaw ?: return place.distanceFromRoute
        val result = FloatArray(1)
        Location.distanceBetween(
            current.latitude,
            current.longitude,
            place.lat,
            place.lon,
            result
        )
        return result[0]
    }

    private fun Float.toFriendlyDistance(): String {
        return if (this >= 1000f) {
            String.format("%.1f km", this / 1000f)
        } else {
            "${this.toInt()} m"
        }
    }

    private fun showMapLoader(show: Boolean) {
        binding.mapLoader.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showRoutingLoader(show: Boolean) {
        binding.routingLoader.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnGetRoute.isEnabled = !show
    }

    private fun showNearbyLoader(show: Boolean) {
        binding.nearbyLoader.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnNearby.isEnabled = !show
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun requestLocationPermission() {
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST
            )
        } else {
            onPermissionGranted()
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            onPermissionGranted()
        } else {
            Toast.makeText(this, "Location permission needed for live tracking", Toast.LENGTH_LONG).show()
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun onPermissionGranted() {
        if (hasLocationPermission()) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    currentLocation = GeoPoint(it.latitude, it.longitude)
                    currentLocationRaw = it
                    mapView.controller.animateTo(currentLocation)
                    mapView.controller.setZoom(DEFAULT_ZOOM)

                    startLocationUpdates()
                    fetchLiveRecommendations(it.latitude, it.longitude, force = true)
                }
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun <T : View> findViewByOptionalId(idName: String): T? {
        val id = resources.getIdentifier(idName, "id", packageName)
        if (id == 0) return null
        @Suppress("UNCHECKED_CAST")
        return findViewById<View>(id) as? T
    }

    private fun chatbotPlaceResponse(keyword: String, humanLabel: String): String {

        if (liveNearbyPlaces.isEmpty()) {
            return "I don't have nearby data yet. Please tap 'Nearby' or wait for GPS."
        }

        val key = keyword.lowercase()

        val filtered = liveNearbyPlaces.filter { place ->
            val type = place.type.lowercase()
            val name = place.name.lowercase()

            when (key) {
                "hospital" -> type.contains("hospital")
                "pharmacy" -> type.contains("pharmacy")
                "police" -> type.contains("police")
                "fuel" -> type.contains("fuel") || type.contains("petrol") || type.contains("gas")
                "charging" -> type.contains("charging")
                "atm" -> type.contains("atm")
                "restaurant" -> type.contains("restaurant") || type.contains("food")
                "cafe" -> type.contains("cafe") || type.contains("coffee")
                "hotel" -> type.contains("hotel")
                "shop" -> type.contains("shop") || type.contains("supermarket")
                else -> type.contains(key) || name.contains(key)
            }
        }

        val match = filtered.minByOrNull { distanceFromCurrentLocation(it) }

        return if (match != null) {
            highlightPlaceOnMap(match)

            "Nearest $humanLabel: ${match.name}\n" +
                    "Distance: ${distanceFromCurrentLocation(match).toFriendlyDistance()}\n" +
                    "${buildPlaceDescription(match)}"
        } else {
            "No nearby $humanLabel found. Try moving or tap Nearby again."
        }
    }
}

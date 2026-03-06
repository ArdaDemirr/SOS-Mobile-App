package com.example.sos

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.*
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow
import org.osmdroid.views.overlay.mylocation.*
import android.graphics.Color as AndroidColor
import java.text.DecimalFormat
import java.util.LinkedList
import kotlin.math.abs
import kotlin.math.floor

@Composable
fun MapScreen(onBack: () -> Unit) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val prefs = context.getSharedPreferences("TacticalMapPrefs", Context.MODE_PRIVATE)

    // 1. Configuration
    Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", 0))
    Configuration.getInstance().userAgentValue = "SurvivalMesh/1.0"

    // --- STATE ---
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var hasPermission by remember { mutableStateOf(false) }

    // Dialog & Marker State
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var targetGeoPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var markerNameInput by remember { mutableStateOf("") }
    var selectedMarker by remember { mutableStateOf<Marker?>(null) }

    // Trail State
    var isTrailActive by remember { mutableStateOf(prefs.getBoolean("trail_active", true)) }
    val trailPoints = remember { mutableStateListOf<GeoPoint>() }

    // Permissions
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasPermission = it }

    LaunchedEffect(Unit) {
        hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    // --- HELPERS ---

    // 1. ADD MARKER
    fun addMarker(map: MapView, pos: GeoPoint, title: String) {
        val marker = Marker(map).apply {
            position = pos
            this.title = title
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

            // Independent InfoWindow
            infoWindow = MarkerInfoWindow(org.osmdroid.library.R.layout.bonuspack_bubble, map)

            // SINGLE CLICK: Toggle Text
            setOnMarkerClickListener { m, _ ->
                if (m.isInfoWindowShown) m.closeInfoWindow()
                else {
                    // InfoWindow.closeAll(map) // Uncomment if you want only one open at a time
                    m.showInfoWindow()
                }
                true
            }
        }
        map.overlays.add(marker)
        map.invalidate()
    }

    // 2. SAVE FUNCTIONS
    fun saveMarkers() {
        mapView ?: return
        val markers = mapView!!.overlays.filterIsInstance<Marker>()
        val string = markers.joinToString(";") { "${it.title}|${it.position.latitude}|${it.position.longitude}" }
        prefs.edit().putString("saved_markers", string).apply()
    }

    fun saveTrail() {
        val string = trailPoints.joinToString("|") { "${it.latitude},${it.longitude}" }
        prefs.edit().putString("saved_trail", string).apply()
    }

    // 3. LOAD DATA
    LaunchedEffect(mapView) {
        if (mapView != null) {
            val savedMarkers = prefs.getString("saved_markers", "") ?: ""
            if (savedMarkers.isNotEmpty()) {
                savedMarkers.split(";").forEach {
                    val p = it.split("|")
                    if (p.size == 3) addMarker(mapView!!, GeoPoint(p[1].toDouble(), p[2].toDouble()), p[0])
                }
            }
            val savedTrail = prefs.getString("saved_trail", "") ?: ""
            if (savedTrail.isNotEmpty()) {
                savedTrail.split("|").forEach {
                    val p = it.split(",")
                    if (p.size == 2) trailPoints.add(GeoPoint(p[0].toDouble(), p[1].toDouble()))
                }
            }
        }
    }

    // MAIN LAYOUT
    Box(
        Modifier
            .fillMaxSize()
            .background(PipBlack) // Uses Global Black
            .systemBarsPadding()
    ) {

        // 4. MAP VIEW
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(19.0)

                    // A. Grid
                    overlays.add(BezelGridOverlay())

                    // B. Touch Receiver (HITBOX FIX)
                    overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                            InfoWindow.closeAll(this@apply)
                            return true
                        }

                        override fun longPressHelper(p: GeoPoint?): Boolean {
                            if (p == null) return false
                            val tap = Point()
                            projection.toPixels(p, tap)

                            // HIT BOX LOGIC
                            val hit = overlays.filterIsInstance<Marker>().find { m ->
                                val mPt = Point()
                                projection.toPixels(m.position, mPt)
                                val xDiff = abs(tap.x - mPt.x)
                                val yDiff = mPt.y - tap.y

                                // 80px wide, extends -20px below to +150px above
                                val hitX = xDiff < 80
                                val hitY = yDiff > -20 && yDiff < 150
                                hitX && hitY
                            }

                            if (hit != null) {
                                selectedMarker = hit
                                markerNameInput = hit.title
                                showEditDialog = true
                            } else {
                                targetGeoPoint = p
                                markerNameInput = ""
                                showAddDialog = true
                            }
                            return true
                        }
                    }))

                    // C. Trail Layer
                    val polyline = Polyline().apply {
                        outlinePaint.color = AndroidColor.GREEN
                        outlinePaint.strokeWidth = 5f
                    }
                    overlays.add(polyline)

                    mapView = this
                }
            }
        )

        // Sync Trail
        LaunchedEffect(trailPoints.size) {
            mapView?.let { map ->
                val poly = map.overlays.filterIsInstance<Polyline>().firstOrNull()
                poly?.setPoints(trailPoints)
                map.invalidate()
            }
        }

        // Lifecycle
        DisposableEffect(lifecycleOwner) {
            val obs = LifecycleEventObserver { _, e ->
                if (e == Lifecycle.Event.ON_RESUME) mapView?.onResume()
                if (e == Lifecycle.Event.ON_PAUSE) mapView?.onPause()
            }
            lifecycleOwner.lifecycle.addObserver(obs)
            onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
        }

        // GPS Logic
        LaunchedEffect(hasPermission, mapView) {
            if (hasPermission && mapView != null) {
                val provider = GpsMyLocationProvider(context)
                provider.locationUpdateMinDistance = 0f
                provider.locationUpdateMinTime = 0L

                val myLoc = object : MyLocationNewOverlay(provider, mapView) {
                    val history = LinkedList<Location>()
                    override fun onLocationChanged(l: Location?, s: IMyLocationProvider?) {
                        if (l == null) return
                        history.add(l)
                        if (history.size > 3) history.removeFirst()
                        var sumLat = 0.0; var sumLon = 0.0
                        history.forEach { sumLat += it.latitude; sumLon += it.longitude }
                        val smooth = Location("S").apply {
                            latitude = sumLat / history.size
                            longitude = sumLon / history.size
                            bearing = l.bearing
                            accuracy = l.accuracy
                        }
                        super.onLocationChanged(smooth, s)

                        if (isTrailActive) {
                            val pt = GeoPoint(smooth.latitude, smooth.longitude)
                            if (trailPoints.isEmpty() || pt.distanceToAsDouble(trailPoints.last()) > 5.0) {
                                trailPoints.add(pt)
                                saveTrail()
                            }
                        }
                    }
                }
                myLoc.enableMyLocation()
                myLoc.enableFollowLocation()
                myLoc.isDrawAccuracyEnabled = false
                mapView!!.overlays.add(myLoc)
            }
        }

        // --- UI OVERLAYS (CONTROLS) ---

        Column(
            Modifier
                .align(Alignment.TopStart)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Trail Toggle (Uses PipAmber)
            Box(
                Modifier
                    .background(PipBlack.copy(0.7f))
                    .border(1.dp, if (isTrailActive) PipGreen else PipRed)
                    .clickable {
                        isTrailActive = !isTrailActive
                        prefs.edit().putBoolean("trail_active", isTrailActive).apply()
                    }
                    .padding(8.dp)
            ) {
                Text(
                    text = "TRAIL: ${if(isTrailActive) "ON" else "OFF"}",
                    color = if (isTrailActive) PipGreen else PipRed,
                    fontWeight = FontWeight.Bold
                )
            }

            // 2. Clear Trail
            Box(
                Modifier
                    .background(PipBlack.copy(0.7f))
                    .border(1.dp, PipRed)
                    .clickable {
                        trailPoints.clear()
                        saveTrail()
                        mapView?.invalidate()
                    }
                    .padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Refresh, null, tint = PipRed, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("CLEAR TRAIL", color = PipRed, fontWeight = FontWeight.Bold)
                }
            }

            // 3. Clear All Markers (Uses PipRed)
            Box(
                Modifier
                    .background(PipBlack.copy(0.7f))
                    .border(1.dp, PipRed)
                    .clickable {
                        mapView?.overlays?.removeAll { it is Marker }
                        mapView?.invalidate()
                        saveMarkers()
                    }
                    .padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Delete, null, tint = PipRed, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("DEL ALL", color = PipRed, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Recenter
        Box(Modifier.align(Alignment.TopEnd).padding(20.dp).size(50.dp)
            .background(PipBlack.copy(.7f), CircleShape).border(2.dp, PipAmber, CircleShape)
            .clickable {
                val loc = mapView?.overlays?.filterIsInstance<MyLocationNewOverlay>()?.firstOrNull()?.myLocation
                if (loc != null) {
                    mapView?.controller?.animateTo(loc)
                    mapView?.overlays?.filterIsInstance<MyLocationNewOverlay>()?.firstOrNull()?.enableFollowLocation()
                }
            },
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Default.Home, null, tint = PipAmber) }

        // Back
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(60.dp)
            .background(PipAmber).clickable { onBack() },
            contentAlignment = Alignment.Center
        ) { Text("< Geri <", color = PipBlack, fontWeight = FontWeight.Bold) }

        // --- DIALOGS ---

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                containerColor = PipBlack,
                title = { Text("NEW WAYPOINT", color = PipAmber) },
                text = {
                    OutlinedTextField(
                        value = markerNameInput,
                        onValueChange = { markerNameInput = it },
                        label = { Text("TITLE", color = PipAmber) },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = PipAmber,
                            unfocusedTextColor = PipAmber,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = PipAmber,
                            unfocusedIndicatorColor = PipAmber,
                            focusedLabelColor = PipAmber,
                            unfocusedLabelColor = PipAmber
                        )
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (targetGeoPoint != null && markerNameInput.isNotBlank()) {
                            addMarker(mapView!!, targetGeoPoint!!, markerNameInput)
                            saveMarkers()
                        }
                        showAddDialog = false
                    }, colors = ButtonDefaults.buttonColors(containerColor = PipAmber)) { Text("ADD", color = PipBlack) }
                }
            )
        }

        if (showEditDialog && selectedMarker != null) {
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                containerColor = PipBlack,
                title = { Text("EDIT WAYPOINT", color = PipAmber) },
                text = {
                    OutlinedTextField(
                        value = markerNameInput,
                        onValueChange = { markerNameInput = it },
                        label = { Text("TITLE", color = PipAmber) },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = PipAmber,
                            unfocusedTextColor = PipAmber,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = PipAmber,
                            unfocusedIndicatorColor = PipAmber,
                            focusedLabelColor = PipAmber,
                            unfocusedLabelColor = PipAmber
                        )
                    )
                },
                confirmButton = {
                    Row {
                        Button(onClick = {
                            selectedMarker?.title = markerNameInput
                            selectedMarker?.showInfoWindow()
                            saveMarkers()
                            showEditDialog = false
                        }, colors = ButtonDefaults.buttonColors(containerColor = PipAmber)) { Text("UPDATE", color = PipBlack) }

                        Spacer(Modifier.width(8.dp))

                        // FORCE DELETE BUTTON
                        Button(
                            colors = ButtonDefaults.buttonColors(containerColor = PipRed),
                            onClick = {
                                selectedMarker?.closeInfoWindow()
                                if (mapView != null && selectedMarker != null) {
                                    mapView!!.overlays.remove(selectedMarker)
                                }
                                mapView?.invalidate()
                                saveMarkers()
                                selectedMarker = null
                                showEditDialog = false
                            }
                        ) {
                            Icon(Icons.Default.Delete, null, tint = PipBlack)
                        }
                    }
                }
            )
        }
    }
}

// --- HELPER CLASSES ---

object InfoWindow {
    fun closeAll(map: MapView) = map.overlays.filterIsInstance<Marker>().forEach { it.closeInfoWindow() }
}

class BezelGridOverlay : Overlay() {
    private val paint = Paint().apply { color = AndroidColor.BLACK; strokeWidth = 2f }
    private val text = Paint().apply { color = AndroidColor.BLACK; textSize = 35f; isFakeBoldText = true; typeface = android.graphics.Typeface.MONOSPACE }
    private val df = DecimalFormat("#.#####")

    override fun draw(c: Canvas, map: MapView, shadow: Boolean) {
        if (shadow) return
        val p = map.projection
        val b = p.boundingBox
        val r = p.intrinsicScreenRect
        var step = 1.0
        while (b.latitudeSpan / step < 3) step /= 10
        while (b.latitudeSpan / step > 8) step *= 10

        var lon = floor(b.lonWest / step) * step
        while (lon < b.lonEast + step) {
            val pt = Point()
            p.toPixels(GeoPoint(b.centerLatitude, lon), pt)
            if (pt.x >= 0 && pt.x <= r.width()) {
                c.drawLine(pt.x.toFloat(), 0f, pt.x.toFloat(), r.height().toFloat(), paint)
                c.drawText(df.format(lon), pt.x + 5f, 180f, text)
            }
            lon += step
        }

        var lat = floor(b.latSouth / step) * step
        while (lat < b.latNorth + step) {
            val pt = Point()
            p.toPixels(GeoPoint(lat, b.centerLongitude), pt)
            if (pt.y >= 0 && pt.y <= r.height()) {
                c.drawLine(0f, pt.y.toFloat(), r.width().toFloat(), pt.y.toFloat(), paint)
                c.drawText(df.format(lat), 10f, pt.y - 10f, text)
            }
            lat += step
        }
    }
}
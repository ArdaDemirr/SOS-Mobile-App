package com.example.sos.Navigation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Typeface
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.sos.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.text.DecimalFormat
import kotlin.math.abs
import kotlin.math.floor
import android.graphics.Color as AndroidColor
import androidx.compose.runtime.*
import androidx.compose.ui.text.TextStyle
import androidx.preference.PreferenceManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.sos.PipAmber
import com.example.sos.PipBlack
import com.example.sos.PipGreen
import com.example.sos.PipRed
import com.example.sos.RetrofitInstance
import org.osmdroid.config.Configuration
import org.osmdroid.library.R
import kotlin.collections.forEach

// --- LOCAL DATA ---
data class TacticalWaypoint(
    var id: Long = -1L, // Tracks the server database ID
    var title: String,
    var description: String,
    val point: GeoPoint,
    val isPublic: Boolean
)

// --- NETWORK PAYLOADS ---
data class WaypointRequestPayload(
    val senderId: String,
    val title: String,
    val latitude: Double,
    val longitude: Double,
    val description: String
)

data class WaypointResponsePayload(
    val id: Long,
    val senderId: String,
    val title: String,
    val latitude: Double,
    val longitude: Double,
    val description: String,
    val timestamp: String
)

enum class MapFrequency { PRIVATE, PUBLIC }

@Composable
fun MapScreen(onBack: () -> Unit) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val prefs = context.getSharedPreferences("TacticalMapPrefs", Context.MODE_PRIVATE)
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context).dogtagDao() }

    // --- STATE ---
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var hasPermission by remember { mutableStateOf(false) }
    var currentFrequency by remember { mutableStateOf(MapFrequency.PRIVATE) }
    var senderUuid by remember { mutableStateOf("UNKNOWN") }

    // Dialog & Marker State
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var targetGeoPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var markerNameInput by remember { mutableStateOf("") }
    var markerDescInput by remember { mutableStateOf("") }
    var markerIsPublic by remember { mutableStateOf(false) }
    var selectedMarker by remember { mutableStateOf<Marker?>(null) }

    val savedWaypoints = remember { mutableStateListOf<TacticalWaypoint>() }
    var isTrailActive by remember { mutableStateOf(prefs.getBoolean("trail_active", true)) }
    val trailPoints = remember { mutableStateListOf<GeoPoint>() }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasPermission = it }

    // --- INITIALIZATION ---
    LaunchedEffect(Unit) {
        // Fix 403 Forbidden Error
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        Configuration.getInstance().userAgentValue = "SOS-Tactical-Mesh-Arda"

        hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)

        val dogtag = withContext(Dispatchers.IO) { db.getDogtag() }
        senderUuid = dogtag?.userUuid ?: "UNKNOWN"
    }

    // --- LIFECYCLE MANAGEMENT (Prevents Freezing) ---
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView?.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)

        // Grab UUID silently for the server payload
        val dogtag = withContext(Dispatchers.IO) { db.getDogtag() }
        senderUuid = dogtag?.userUuid ?: "UNKNOWN"
    }

    // --- CORE LOGIC ---

    fun saveLocalWaypoints() {
        val string = savedWaypoints.joinToString(";") {
            val safeTitle = it.title.replace("|", "").replace(";", "")
            val safeDesc = it.description.replace("|", "").replace(";", "")
            "${it.id}|$safeTitle|$safeDesc|${it.point.latitude}|${it.point.longitude}|${it.isPublic}"
        }
        prefs.edit().putString("saved_waypoints_v4", string).apply()
    }

    fun saveTrail() {
        val string = trailPoints.joinToString("|") { "${it.latitude},${it.longitude}" }
        prefs.edit().putString("saved_trail", string).apply()
    }

    fun refreshMarkers() {
        mapView?.let { map ->
            val markersToRemove = map.overlays.filterIsInstance<Marker>()
            map.overlays.removeAll(markersToRemove)

            val visibleWaypoints = savedWaypoints.filter {
                if (currentFrequency == MapFrequency.PRIVATE) !it.isPublic else it.isPublic
            }

            visibleWaypoints.forEach { wp ->
                val marker = Marker(map).apply {
                    position = wp.point
                    title = wp.title
                    snippet = wp.description
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    infoWindow = MarkerInfoWindow(R.layout.bonuspack_bubble, map)

                    setOnMarkerClickListener { m, _ ->
                        if (m.isInfoWindowShown) m.closeInfoWindow() else m.showInfoWindow()
                        true
                    }
                }
                map.overlays.add(marker)
            }
            map.invalidate()
        }
    }

    // PULL DOWN PUBLIC MARKERS FROM SERVER
    fun fetchPublicMesh() {
        scope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitInstance.api.getAllWaypoints()
                if (response.isSuccessful) {
                    val serverWaypoints = response.body() ?: emptyList()
                    withContext(Dispatchers.Main) {
                        savedWaypoints.removeAll { it.isPublic }
                        serverWaypoints.forEach { wp ->
                            savedWaypoints.add(
                                TacticalWaypoint(
                                    id = wp.id, // Save the server ID locally
                                    title = wp.title,
                                    description = wp.description,
                                    point = GeoPoint(wp.latitude, wp.longitude),
                                    isPublic = true
                                )
                            )
                        }
                        refreshMarkers()
                    }
                }
            } catch (e: Exception) {
                println("Tactical Sync Error: Could not reach Public Mesh. ${e.message}")
            }
        }
    }

    LaunchedEffect(mapView) {
        if (mapView != null) {
            val savedData = prefs.getString("saved_waypoints_v4", "") ?: ""
            if (savedData.isNotEmpty()) {
                savedData.split(";").forEach {
                    val p = it.split("|")
                    if (p.size >= 6) {
                        savedWaypoints.add(TacticalWaypoint(p[0].toLong(), p[1], p[2], GeoPoint(p[3].toDouble(), p[4].toDouble()), p[5].toBoolean()))
                    }
                }
            }
            val loadedTrail = prefs.getString("saved_trail", "") ?: ""
            if (loadedTrail.isNotEmpty()) {
                loadedTrail.split("|").forEach {
                    val p = it.split(",")
                    if (p.size == 2) trailPoints.add(GeoPoint(p[0].toDouble(), p[1].toDouble()))
                }
            }
            refreshMarkers()
        }
    }

    // Re-render and Fetch when tab changes
    LaunchedEffect(currentFrequency) {
        if (currentFrequency == MapFrequency.PUBLIC) {
            fetchPublicMesh()
        }
        refreshMarkers()
    }

    // --- MAIN UI ---
    Box(Modifier.fillMaxSize().background(PipBlack).systemBarsPadding()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(19.0)
                    overlays.add(BezelGridOverlay())

                    overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                            InfoWindow.closeAll(this@apply)
                            return true
                        }
                        override fun longPressHelper(p: GeoPoint?): Boolean {
                            if (p == null) return false
                            val tap = Point()
                            projection.toPixels(p, tap)

                            val hit = overlays.filterIsInstance<Marker>().find { m ->
                                val mPt = Point()
                                projection.toPixels(m.position, mPt)
                                val xDiff = abs(tap.x - mPt.x)
                                val yDiff = mPt.y - tap.y
                                xDiff < 80 && yDiff > -20 && yDiff < 150
                            }

                            if (hit != null) {
                                selectedMarker = hit
                                markerNameInput = hit.title
                                val match = savedWaypoints.find { it.title == hit.title && it.point.latitude == hit.position.latitude }
                                markerDescInput = match?.description ?: ""
                                markerIsPublic = match?.isPublic ?: false
                                showEditDialog = true
                            } else {
                                targetGeoPoint = p
                                markerNameInput = ""
                                markerDescInput = ""
                                markerIsPublic = currentFrequency == MapFrequency.PUBLIC
                                showAddDialog = true
                            }
                            return true
                        }
                    }))

                    val polyline = Polyline().apply {
                        outlinePaint.color = AndroidColor.GREEN
                        outlinePaint.strokeWidth = 6f
                    }
                    overlays.add(polyline)
                    mapView = this
                }
            }
        )

        LaunchedEffect(trailPoints.size) {
            mapView?.let { map ->
                val poly = map.overlays.filterIsInstance<Polyline>().firstOrNull()
                poly?.setPoints(trailPoints)
                map.invalidate()
            }
        }

        LaunchedEffect(hasPermission, mapView) {
            if (hasPermission && mapView != null) {
                val provider = GpsMyLocationProvider(context)
                provider.locationUpdateMinDistance = 1f
                provider.locationUpdateMinTime = 1000L

                val myLoc = object : MyLocationNewOverlay(provider, mapView) {
                    override fun onLocationChanged(l: Location?, s: IMyLocationProvider?) {
                        super.onLocationChanged(l, s)
                        if (l == null) return
                        if (isTrailActive) {
                            val pt = GeoPoint(l.latitude, l.longitude)
                            if (trailPoints.isEmpty() || pt.distanceToAsDouble(trailPoints.last()) > 1.5) {
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

        // --- OVERLAY CONTROLS ---
        Column(Modifier.fillMaxSize()) {

            // FREQUENCY TABS
            Row(modifier = Modifier.fillMaxWidth().background(PipBlack.copy(alpha = 0.8f)).padding(top = 16.dp, bottom = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Box(modifier = Modifier.weight(1f).padding(horizontal = 8.dp).border(2.dp, if (currentFrequency == MapFrequency.PRIVATE) PipAmber else PipBlack, RoundedCornerShape(4.dp)).clickable { currentFrequency = MapFrequency.PRIVATE }.padding(12.dp), contentAlignment = Alignment.Center) {
                    Text("PRIVATE", color = if (currentFrequency == MapFrequency.PRIVATE) PipAmber else PipAmber.copy(0.4f), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Box(modifier = Modifier.weight(1f).padding(horizontal = 8.dp).border(2.dp, if (currentFrequency == MapFrequency.PUBLIC) PipAmber else PipBlack, RoundedCornerShape(4.dp)).clickable { currentFrequency = MapFrequency.PUBLIC }.padding(12.dp), contentAlignment = Alignment.Center) {
                    Text("PUBLIC MESH", color = if (currentFrequency == MapFrequency.PUBLIC) PipAmber else PipAmber.copy(0.4f), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }

            // SIDE CONTROLS
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.Companion.background(PipBlack.copy(0.7f)).border(1.dp, if (isTrailActive) PipGreen else PipRed).clickable {
                    isTrailActive = !isTrailActive
                    prefs.edit().putBoolean("trail_active", isTrailActive).apply()
                }.padding(8.dp)) {
                    Text("TRAIL: ${if(isTrailActive) "REC" else "OFF"}", color = if (isTrailActive) PipGreen else PipRed, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Box(Modifier.Companion.background(PipBlack.copy(0.7f)).border(1.dp, PipRed).clickable {
                    trailPoints.clear()
                    saveTrail()
                    mapView?.invalidate()
                }.padding(8.dp)) {
                    Text("WIPE TRAIL", color = PipRed, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.weight(1f))

            // Recenter
            Box(Modifier.align(Alignment.End).padding(20.dp).size(50.dp).background(PipBlack.copy(.7f), CircleShape).border(2.dp,
                PipAmber, CircleShape)
                .clickable {
                    val loc = mapView?.overlays?.filterIsInstance<MyLocationNewOverlay>()?.firstOrNull()?.myLocation
                    if (loc != null) {
                        mapView?.controller?.animateTo(loc)
                        mapView?.overlays?.filterIsInstance<MyLocationNewOverlay>()?.firstOrNull()?.enableFollowLocation()
                    }
                }, contentAlignment = Alignment.Center) { Icon(Icons.Default.Home, null, tint = PipAmber) }

            // Back
            Box(Modifier.fillMaxWidth().height(60.dp).background(PipAmber).clickable { onBack() }, contentAlignment = Alignment.Center) {
                Text("< OFFLINE SYSTEM <", color = PipBlack, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }

        // --- DIALOGS ---

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                containerColor = PipBlack,
                title = { Text("NEW WAYPOINT", color = PipAmber, fontFamily = FontFamily.Monospace) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = markerNameInput, onValueChange = { markerNameInput = it },
                            label = { Text("TITLE", color = PipAmber) },
                            textStyle = TextStyle(color = PipAmber, fontFamily = FontFamily.Monospace),
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = PipAmber, unfocusedIndicatorColor = PipAmber, focusedLabelColor = PipAmber, unfocusedLabelColor = PipAmber)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = markerDescInput, onValueChange = { markerDescInput = it },
                            label = { Text("DESCRIPTION", color = PipAmber) },
                            textStyle = TextStyle(color = PipAmber, fontFamily = FontFamily.Monospace),
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = PipAmber, unfocusedIndicatorColor = PipAmber, focusedLabelColor = PipAmber, unfocusedLabelColor = PipAmber)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { markerIsPublic = !markerIsPublic }) {
                            Checkbox(checked = markerIsPublic, onCheckedChange = { markerIsPublic = it }, colors = CheckboxDefaults.colors(checkedColor = PipAmber, checkmarkColor = PipBlack, uncheckedColor = PipAmber))
                            Text("TRANSMIT TO PUBLIC MESH", color = PipAmber, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (targetGeoPoint != null && markerNameInput.isNotBlank()) {
                            val newWp = TacticalWaypoint(
                                id = -1L,
                                title = markerNameInput,
                                description = markerDescInput,
                                point = targetGeoPoint!!,
                                isPublic = markerIsPublic
                            )
                            savedWaypoints.add(newWp)
                            saveLocalWaypoints()
                            refreshMarkers()

                            // 1. PUSH TO SERVER IF PUBLIC
                            if (markerIsPublic) {
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val payload = WaypointRequestPayload(
                                            senderId = senderUuid,
                                            title = markerNameInput,
                                            latitude = targetGeoPoint!!.latitude,
                                            longitude = targetGeoPoint!!.longitude,
                                            description = markerDescInput
                                        )
                                        val response = RetrofitInstance.api.createWaypoint(payload)
                                        if (response.isSuccessful) {
                                            // Save the Server's ID back to the local device so we can Delete/Update it later!
                                            val serverId = response.body()?.id
                                            if (serverId != null) {
                                                val index = savedWaypoints.indexOf(newWp)
                                                if (index != -1) {
                                                    savedWaypoints[index].id = serverId
                                                    saveLocalWaypoints()
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        println("Tactical Sync Error: Failed to push waypoint.")
                                    }
                                }
                            }
                        }
                        showAddDialog = false
                    }, colors = ButtonDefaults.buttonColors(containerColor = PipAmber)) { Text("PLOT", color = PipBlack, fontFamily = FontFamily.Monospace) }
                }
            )
        }

        if (showEditDialog && selectedMarker != null) {
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                containerColor = PipBlack,
                title = { Text("EDIT WAYPOINT", color = PipAmber, fontFamily = FontFamily.Monospace) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = markerNameInput, onValueChange = { markerNameInput = it },
                            label = { Text("TITLE", color = PipAmber) },
                            textStyle = TextStyle(color = PipAmber, fontFamily = FontFamily.Monospace),
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = PipAmber, unfocusedIndicatorColor = PipAmber, focusedLabelColor = PipAmber, unfocusedLabelColor = PipAmber)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = markerDescInput, onValueChange = { markerDescInput = it },
                            label = { Text("DESCRIPTION", color = PipAmber) },
                            textStyle = TextStyle(color = PipAmber, fontFamily = FontFamily.Monospace),
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = PipAmber, unfocusedIndicatorColor = PipAmber, focusedLabelColor = PipAmber, unfocusedLabelColor = PipAmber)
                        )
                    }
                },
                confirmButton = {
                    Row {
                        // --- 2. THE UPDATE BUTTON ---
                        Button(onClick = {
                            val index = savedWaypoints.indexOfFirst { it.title == selectedMarker?.title && it.point.latitude == selectedMarker?.position?.latitude }
                            if (index != -1) {
                                // Update Locally
                                savedWaypoints[index].title = markerNameInput
                                savedWaypoints[index].description = markerDescInput
                                saveLocalWaypoints()
                                refreshMarkers()

                                val wpToUpdate = savedWaypoints[index]

                                // Push Update to Server
                                if (wpToUpdate.isPublic && wpToUpdate.id != -1L) {
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            val payload = WaypointRequestPayload(
                                                senderId = senderUuid,
                                                title = markerNameInput,
                                                latitude = wpToUpdate.point.latitude,
                                                longitude = wpToUpdate.point.longitude,
                                                description = markerDescInput
                                            )
                                            val response = RetrofitInstance.api.updateWaypoint(wpToUpdate.id, senderUuid, payload)
                                            if (!response.isSuccessful) {
                                                println("Tactical Sync Error: Update rejected. Code: ${response.code()}")
                                            }
                                        } catch (e: Exception) {
                                            println("Tactical Sync Error: Failed to transmit update.")
                                        }
                                    }
                                }
                            }
                            showEditDialog = false
                        }, colors = ButtonDefaults.buttonColors(containerColor = PipAmber)) { Text("UPDATE", color = PipBlack, fontFamily = FontFamily.Monospace) }

                        Spacer(Modifier.width(8.dp))

                        // --- 3. THE DELETE BUTTON ---
                        Button(colors = ButtonDefaults.buttonColors(containerColor = PipRed), onClick = {
                            val index = savedWaypoints.indexOfFirst { it.title == selectedMarker?.title && it.point.latitude == selectedMarker?.position?.latitude }
                            if (index != -1) {
                                val wpToDelete = savedWaypoints[index]

                                // Tell Server to Delete
                                if (wpToDelete.isPublic && wpToDelete.id != -1L) {
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            RetrofitInstance.api.deleteWaypoint(wpToDelete.id, senderUuid)
                                        } catch (e: Exception) {
                                            println("Tactical Sync Error: Failed to send kill command.")
                                        }
                                    }
                                }

                                // Delete Locally
                                savedWaypoints.removeAt(index)
                                saveLocalWaypoints()
                                refreshMarkers()
                            }
                            selectedMarker = null
                            showEditDialog = false
                        }) { Icon(Icons.Default.Delete, null, tint = PipBlack) }
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
    private val text = Paint().apply { color = AndroidColor.BLACK; textSize = 35f; isFakeBoldText = true; typeface = Typeface.MONOSPACE }
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
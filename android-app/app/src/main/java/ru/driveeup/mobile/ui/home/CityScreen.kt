package ru.driveeup.mobile.ui.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.net.URLEncoder
import java.net.HttpURLConnection
import java.net.URL

data class PlaceHit(
    val display: String,
    val lat: Double,
    val lon: Double,
    val distanceKm: Double? = null
)

@Composable
fun CityScreen(
    driveCoin: Long,
    onOpenMenu: () -> Unit,
    onOpenDriveUp: () -> Unit
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    Configuration.getInstance().load(appContext, appContext.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))

    var from by remember { mutableStateOf("") }
    var to by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var activeField by remember { mutableStateOf<String?>(null) }
    var fullSearchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var locationGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    val results = remember { mutableStateListOf<PlaceHit>() }
    var fromPlace by remember { mutableStateOf<PlaceHit?>(null) }
    var toPlace by remember { mutableStateOf<PlaceHit?>(null) }
    var userGeoPoint by remember { mutableStateOf<GeoPoint?>(null) }
    val mapRef = remember { mutableStateOf<MapView?>(null) }
    val overlaysRef = remember { mutableStateListOf<org.osmdroid.views.overlay.Overlay>() }
    val locationOverlayRef = remember { mutableStateOf<MyLocationNewOverlay?>(null) }
    val uiScope = rememberCoroutineScope()

    suspend fun reverseGeocode(lat: Double, lon: Double): String {
        return withContext(Dispatchers.IO) {
            runCatching {
                val url = "https://nominatim.openstreetmap.org/reverse?format=json&lat=$lat&lon=$lon"
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "DriveUP-Android/1.0")
                    connectTimeout = 7000
                    readTimeout = 7000
                }
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                JSONObject(text).optString("display_name")
            }.getOrDefault("")
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        locationGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (locationGranted) {
            enableUserLocation(mapRef.value, locationOverlayRef)
            val p = getLastKnownGeoPoint(context)
            if (p != null) {
                userGeoPoint = p
                uiScope.launch {
                    val addr = reverseGeocode(p.latitude, p.longitude)
                    from = if (addr.isNotBlank()) addr else "Моё местоположение"
                    fromPlace = PlaceHit(
                        display = from,
                        lat = p.latitude,
                        lon = p.longitude,
                        distanceKm = 0.0
                    )
                }
            }
        }
    }

    suspend fun searchPlaces(query: String) {
        if (query.isBlank()) {
            results.clear()
            return
        }
        val items = withContext(Dispatchers.IO) {
            runCatching {
                val url = "https://nominatim.openstreetmap.org/search?format=json&q=${URLEncoder.encode(query, "UTF-8")}&limit=8&addressdetails=1"
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "DriveUP-Android/1.0")
                    connectTimeout = 7000
                    readTimeout = 7000
                }
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val arr = JSONArray(text)
                val referencePoint = userGeoPoint ?: fromPlace?.let { GeoPoint(it.lat, it.lon) }
                val list = buildList {
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val lat = o.optString("lat").toDoubleOrNull() ?: 0.0
                        val lon = o.optString("lon").toDoubleOrNull() ?: 0.0
                        val dist = referencePoint?.let { haversineKm(it.latitude, it.longitude, lat, lon) }
                        add(
                            PlaceHit(
                                display = o.optString("display_name"),
                                lat = lat,
                                lon = lon,
                                distanceKm = dist
                            )
                        )
                    }
                }
                if (referencePoint != null) list.sortedBy { it.distanceKm ?: Double.MAX_VALUE } else list
            }.getOrDefault(emptyList())
        }
        results.clear()
        results.addAll(items)
    }

    LaunchedEffect(searchQuery, activeField, fullSearchOpen) {
        if (!fullSearchOpen) {
            results.clear()
            return@LaunchedEffect
        }
        when (activeField) {
            "from", "to" -> searchPlaces(searchQuery)
            else -> results.clear()
        }
    }

    LaunchedEffect(Unit) {
        if (!locationGranted) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            if (from.isBlank()) {
                // lastKnown может быть пустым сразу после старта, поэтому делаем несколько попыток.
                repeat(6) {
                    val p = getLastKnownGeoPoint(context)
                    if (p != null) {
                        userGeoPoint = p
                        val addr = reverseGeocode(p.latitude, p.longitude)
                        from = if (addr.isNotBlank()) addr else "Моё местоположение"
                        fromPlace = PlaceHit(from, p.latitude, p.longitude, 0.0)
                        return@LaunchedEffect
                    }
                    delay(800)
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Box(modifier = Modifier.fillMaxWidth().weight(3f)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(12.0)
                        controller.setCenter(GeoPoint(55.751244, 37.618423))
                        mapRef.value = this
                        if (locationGranted) {
                            enableUserLocation(this, locationOverlayRef)
                        } else {
                            centerByLastKnownLocation(context, this)
                        }
                    }
                }
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(42.dp).clickable(onClick = onOpenMenu),
                    shape = CircleShape,
                    color = Color.White
                ) {
                    Icon(Icons.Default.Menu, contentDescription = null, tint = Color.Gray, modifier = Modifier.padding(10.dp))
                }
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    modifier = Modifier.clickable(onClick = onOpenDriveUp)
                ) {
                    Text(
                        text = "$driveCoin DriveCoin",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .size(46.dp)
                    .clickable {
                        if (locationGranted) {
                            val map = mapRef.value
                            val overlay = locationOverlayRef.value
                            if (map != null) {
                                if (overlay == null) {
                                    enableUserLocation(map, locationOverlayRef)
                                } else {
                                    val p = overlay.myLocation
                                    if (p != null) {
                                        map.controller.animateTo(p)
                                        map.controller.setZoom(17.0)
                                    } else {
                                        overlay.enableFollowLocation()
                                    }
                                }
                            }
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                shape = CircleShape,
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFD9D9D9))
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = "GPS",
                    tint = Color(0xFF5A7BA5),
                    modifier = Modifier.padding(11.dp)
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().weight(2f),
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            activeField = "from"
                            searchQuery = from
                            fullSearchOpen = true
                        },
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFBFC8B8))
                ) {
                    Text(
                        text = if (from.isBlank()) "Откуда" else from,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                        color = if (from.isBlank()) Color(0xFF8A8A8A) else Color(0xFF1F1F1F)
                    )
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            activeField = "to"
                            searchQuery = to
                            fullSearchOpen = true
                        },
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFBFC8B8))
                ) {
                    Text(
                        text = if (to.isBlank()) "Куда" else to,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                        color = if (to.isBlank()) Color(0xFF8A8A8A) else Color(0xFF1F1F1F)
                    )
                }
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Предложите цену") },
                    singleLine = true
                )
                Button(
                    onClick = {
                        val a = fromPlace
                        val b = toPlace
                        if (a != null && b != null) {
                            drawRoute(mapRef.value, overlaysRef, a, b)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Заказать") }
            }
        }
        if (fullSearchOpen) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White),
                color = Color.White
            ) {
                Column(Modifier.fillMaxSize().padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(40.dp)
                                .clickable {
                                    fullSearchOpen = false
                                    activeField = null
                                    searchQuery = ""
                                    results.clear()
                                },
                            shape = CircleShape,
                            color = Color(0xFFF0F0F0)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть", modifier = Modifier.padding(10.dp), tint = Color.Gray)
                        }
                    }
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(if (activeField == "from") "Поиск места отправления" else "Поиск места назначения") },
                        singleLine = true
                    )
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                        items(results) { hit ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (activeField == "from") {
                                            from = hit.display
                                            fromPlace = hit
                                        } else if (activeField == "to") {
                                            to = hit.display
                                            toPlace = hit
                                        }
                                        fullSearchOpen = false
                                        activeField = null
                                        searchQuery = ""
                                        results.clear()
                                    }
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    hit.display,
                                    color = Color(0xFF303030),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    formatDistance(hit.distanceKm),
                                    color = Color(0xFF8A8A8A),
                                    modifier = Modifier.padding(start = 10.dp)
                                )
                            }
                            Divider()
                        }
                    }
                }
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            locationOverlayRef.value?.disableMyLocation()
            mapRef.value?.onDetach()
            overlaysRef.clear()
        }
    }
}

private fun getLastKnownGeoPoint(context: Context): GeoPoint? {
    val hasLocationPerm =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (!hasLocationPerm) return null
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
    for (provider in providers) {
        val loc = runCatching { lm.getLastKnownLocation(provider) }.getOrNull() ?: continue
        return GeoPoint(loc.latitude, loc.longitude)
    }
    return null
}

private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
        Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return r * c
}

private fun formatDistance(km: Double?): String {
    if (km == null) return ""
    return if (km < 1.0) "${(km * 1000).toInt()} м" else String.format("%.1f км", km)
}

private fun enableUserLocation(map: MapView?, locationOverlayRef: androidx.compose.runtime.MutableState<MyLocationNewOverlay?>) {
    if (map == null) return
    if (locationOverlayRef.value == null) {
        val overlay = MyLocationNewOverlay(GpsMyLocationProvider(map.context), map)
        overlay.enableMyLocation()
        overlay.enableFollowLocation()
        map.overlays.add(overlay)
        locationOverlayRef.value = overlay
    }
    val p = locationOverlayRef.value?.myLocation
    if (p != null) {
        map.controller.animateTo(p)
        map.controller.setZoom(17.0)
    }
    map.invalidate()
}

private fun centerByLastKnownLocation(context: Context, map: MapView?) {
    if (map == null) return
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
    for (provider in providers) {
        val loc = runCatching { lm.getLastKnownLocation(provider) }.getOrNull() ?: continue
        map.controller.setCenter(GeoPoint(loc.latitude, loc.longitude))
        map.controller.setZoom(15.5)
        return
    }
}

private fun drawRoute(
    map: MapView?,
    overlaysRef: MutableList<org.osmdroid.views.overlay.Overlay>,
    from: PlaceHit,
    to: PlaceHit
) {
    if (map == null) return
    CoroutineScope(Dispatchers.IO).launch {
        runCatching {
            val url = "https://router.project-osrm.org/route/v1/driving/${from.lon},${from.lat};${to.lon},${to.lat}?overview=full&geometries=geojson"
            val text = URL(url).openStream().bufferedReader().use { it.readText() }
            val json = JSONObject(text)
            val coords = json.getJSONArray("routes")
                .getJSONObject(0)
                .getJSONObject("geometry")
                .getJSONArray("coordinates")

            withContext(Dispatchers.Main) {
                overlaysRef.forEach { map.overlays.remove(it) }
                overlaysRef.clear()

                val markerA = Marker(map).apply { position = GeoPoint(from.lat, from.lon) }
                val markerB = Marker(map).apply { position = GeoPoint(to.lat, to.lon) }
                map.overlays.add(markerA)
                map.overlays.add(markerB)
                overlaysRef.add(markerA)
                overlaysRef.add(markerB)

                val polyline = Polyline().apply {
                    outlinePaint.color = android.graphics.Color.parseColor("#5abf2a")
                    outlinePaint.strokeWidth = 8f
                    setPoints((0 until coords.length()).map { idx ->
                        val c = coords.getJSONArray(idx)
                        GeoPoint(c.getDouble(1), c.getDouble(0))
                    })
                }
                map.overlays.add(polyline)
                overlaysRef.add(polyline)
                map.invalidate()
                map.controller.animateTo(GeoPoint(from.lat, from.lon))
            }
        }
    }
}

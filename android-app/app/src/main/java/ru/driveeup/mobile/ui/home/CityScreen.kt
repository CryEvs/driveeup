package ru.driveeup.mobile.ui.home

import android.content.Context
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
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.net.URLEncoder
import java.net.URL

data class PlaceHit(val display: String, val lat: Double, val lon: Double)

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
    val results = remember { mutableStateListOf<PlaceHit>() }
    var fromPlace by remember { mutableStateOf<PlaceHit?>(null) }
    var toPlace by remember { mutableStateOf<PlaceHit?>(null) }
    val mapRef = remember { mutableStateOf<MapView?>(null) }
    val overlaysRef = remember { mutableStateListOf<org.osmdroid.views.overlay.Overlay>() }

    suspend fun searchPlaces(query: String) {
        if (query.isBlank()) {
            results.clear()
            return
        }
        val items = withContext(Dispatchers.IO) {
            runCatching {
                val url = "https://nominatim.openstreetmap.org/search?format=json&q=${URLEncoder.encode(query, "UTF-8")}&limit=8"
                val text = URL(url).openStream().bufferedReader().use { it.readText() }
                val arr = JSONArray(text)
                buildList {
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        add(
                            PlaceHit(
                                display = o.optString("display_name"),
                                lat = o.optString("lat").toDoubleOrNull() ?: 0.0,
                                lon = o.optString("lon").toDoubleOrNull() ?: 0.0
                            )
                        )
                    }
                }
            }.getOrDefault(emptyList())
        }
        results.clear()
        results.addAll(items)
    }

    LaunchedEffect(from, to, activeField) {
        when (activeField) {
            "from" -> searchPlaces(from)
            "to" -> searchPlaces(to)
            else -> results.clear()
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
        }

        Card(
            modifier = Modifier.fillMaxWidth().weight(2f),
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = from,
                    onValueChange = { from = it; activeField = "from" },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Откуда") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = to,
                    onValueChange = { to = it; activeField = "to" },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Куда") },
                    singleLine = true
                )
                if (results.isNotEmpty()) {
                    LazyColumn(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                        items(results) { hit ->
                            Text(
                                hit.display,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (activeField == "from") {
                                            from = hit.display
                                            fromPlace = hit
                                        } else {
                                            to = hit.display
                                            toPlace = hit
                                        }
                                        activeField = null
                                    }
                                    .padding(8.dp),
                                color = Color(0xFF303030)
                            )
                            Divider()
                        }
                    }
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
    }
    DisposableEffect(Unit) {
        onDispose {
            mapRef.value?.onDetach()
            overlaysRef.clear()
        }
    }
}

private fun drawRoute(
    map: MapView?,
    overlaysRef: MutableList<org.osmdroid.views.overlay.Overlay>,
    from: PlaceHit,
    to: PlaceHit
) {
    if (map == null) return
    kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        runCatching {
            val url = "https://router.project-osrm.org/route/v1/driving/${from.lon},${from.lat};${to.lon},${to.lat}?overview=full&geometries=geojson"
            val text = URL(url).openStream().bufferedReader().use { it.readText() }
            val json = JSONObject(text)
            val coords = json.getJSONArray("routes")
                .getJSONObject(0)
                .getJSONObject("geometry")
                .getJSONArray("coordinates")

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
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

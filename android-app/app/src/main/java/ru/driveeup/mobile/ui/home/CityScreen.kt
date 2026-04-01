package ru.driveeup.mobile.ui.home

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URLEncoder
import java.net.URL

data class PlaceHit(val display: String, val lat: Double, val lon: Double)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CityScreen(
    driveCoin: Long,
    onOpenMenu: () -> Unit,
    onOpenDriveUp: () -> Unit
) {
    var from by remember { mutableStateOf("") }
    var to by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var activeField by remember { mutableStateOf<String?>(null) }
    val results = remember { mutableStateListOf<PlaceHit>() }
    var fromPlace by remember { mutableStateOf<PlaceHit?>(null) }
    var toPlace by remember { mutableStateOf<PlaceHit?>(null) }
    val webRef = remember { mutableStateOf<WebView?>(null) }

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
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        webViewClient = WebViewClient()
                        loadDataWithBaseURL(
                            "https://driveeup.ru",
                            cityMapHtml(),
                            "text/html",
                            "utf-8",
                            null
                        )
                        webRef.value = this
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
                            webRef.value?.evaluateJavascript(
                                "window.driveeupRoute(${a.lat},${a.lon},${b.lat},${b.lon});",
                                null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Заказать") }
            }
        }
    }
}

private fun cityMapHtml(): String = """
<!doctype html><html><head>
<meta name='viewport' content='width=device-width,initial-scale=1'/>
<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>
<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>
<style>html,body,#map{height:100%;margin:0;} </style>
</head><body><div id='map'></div>
<script>
const map = L.map('map').setView([55.751244, 37.618423], 12);
L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19}).addTo(map);
let routeLine = null; let markerA = null; let markerB = null;
window.driveeupRoute = async function(aLat,aLon,bLat,bLon){
  if(markerA) map.removeLayer(markerA); if(markerB) map.removeLayer(markerB); if(routeLine) map.removeLayer(routeLine);
  markerA = L.marker([aLat,aLon]).addTo(map);
  markerB = L.marker([bLat,bLon]).addTo(map);
  const url = `https://router.project-osrm.org/route/v1/driving/${aLon},${aLat};${bLon},${bLat}?overview=full&geometries=geojson`;
  const res = await fetch(url); const data = await res.json();
  const coords = (data.routes && data.routes[0] && data.routes[0].geometry && data.routes[0].geometry.coordinates) || [];
  routeLine = L.polyline(coords.map(x=>[x[1],x[0]]), {color:'#5abf2a', weight:6}).addTo(map);
  map.fitBounds(routeLine.getBounds(), {padding:[24,24]});
}
</script></body></html>
""".trimIndent()

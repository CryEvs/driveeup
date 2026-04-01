package ru.driveeup.mobile.ui.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.net.URL

/** Результат OSRM: точки, длительность (с), длина (м). */
data class OsrmRouteResult(
    val points: List<GeoPoint>,
    val durationSec: Double,
    val distanceM: Double
)

suspend fun fetchOsrmRoute(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double
): OsrmRouteResult? = withContext(Dispatchers.IO) {
    runCatching {
        val url =
            "https://router.project-osrm.org/route/v1/driving/$lon1,$lat1;$lon2,$lat2?overview=full&geometries=geojson"
        val text = URL(url).openStream().bufferedReader().use { it.readText() }
        val json = JSONObject(text)
        val route = json.getJSONArray("routes").getJSONObject(0)
        val durationSec = route.getDouble("duration")
        val distanceM = route.getDouble("distance")
        val coords = route.getJSONObject("geometry").getJSONArray("coordinates")
        val points = (0 until coords.length()).map { idx ->
            val c = coords.getJSONArray(idx)
            GeoPoint(c.getDouble(1), c.getDouble(0))
        }
        OsrmRouteResult(points, durationSec, distanceM)
    }.getOrNull()
}

/**
 * Два сегмента: водитель → A (голубой), A → B (тёмно-фирменный).
 * Если [driverLat]/[driverLon] null — рисуется только A→B одним цветом (фирменный).
 */
fun drawDriverTwoLegRoute(
    context: Context,
    map: MapView?,
    overlaysRef: MutableList<org.osmdroid.views.overlay.Overlay>,
    driverLat: Double?,
    driverLon: Double?,
    aLat: Double,
    aLon: Double,
    bLat: Double,
    bLon: Double,
    colorDriverToA: Int,
    colorAToB: Int,
    colorMarkerA: Int,
    colorMarkerB: Int,
    colorMarkerDriver: Int
) {
    if (map == null) return
    CoroutineScope(Dispatchers.IO).launch {
        val toA = if (driverLat != null && driverLon != null) {
            fetchOsrmRoute(driverLat, driverLon, aLat, aLon)
        } else null
        val aToB = fetchOsrmRoute(aLat, aLon, bLat, bLon) ?: return@launch

        withContext(Dispatchers.Main) {
            overlaysRef.forEach { map.overlays.remove(it) }
            overlaysRef.clear()

            val iconA = dotDrawable(context, colorMarkerA)
            val iconB = dotDrawable(context, colorMarkerB)
            val iconD = dotDrawable(context, colorMarkerDriver)

            if (toA != null && toA.points.size >= 2) {
                val poly1 = Polyline().apply {
                    outlinePaint.color = colorDriverToA
                    outlinePaint.strokeWidth = 9f
                    setPoints(toA.points)
                }
                map.overlays.add(poly1)
                overlaysRef.add(poly1)
                if (driverLat != null && driverLon != null) {
                    val mD = Marker(map).apply {
                        position = GeoPoint(driverLat, driverLon)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        icon = iconD
                    }
                    map.overlays.add(mD)
                    overlaysRef.add(mD)
                }
            }

            val poly2 = Polyline().apply {
                outlinePaint.color = colorAToB
                outlinePaint.strokeWidth = 10f
                setPoints(aToB.points)
            }
            map.overlays.add(poly2)
            overlaysRef.add(poly2)

            val markerA = Marker(map).apply {
                position = GeoPoint(aLat, aLon)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = iconA
                title = "A"
            }
            val markerB = Marker(map).apply {
                position = GeoPoint(bLat, bLon)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = iconB
                title = "B"
            }
            map.overlays.add(markerA)
            map.overlays.add(markerB)
            overlaysRef.add(markerA)
            overlaysRef.add(markerB)

            map.invalidate()
            val cLat = (aLat + bLat) / 2
            val cLon = (aLon + bLon) / 2
            map.controller.setZoom(12.0)
            map.controller.setCenter(GeoPoint(cLat, cLon))
        }
    }
}

fun formatOsrmDuration(sec: Double): String {
    val m = (sec / 60.0).toInt().coerceAtLeast(1)
    return if (m >= 60) "${m / 60} ч ${m % 60} мин" else "~$m мин"
}

fun formatOsrmDistance(meters: Double): String {
    return if (meters < 1000) "${meters.toInt()} м" else String.format("%.1f км", meters / 1000.0)
}

/** Light blue route + markers (A light blue, B brand dark green). */
fun drawRouteAb(
    context: Context,
    map: MapView?,
    overlaysRef: MutableList<org.osmdroid.views.overlay.Overlay>,
    from: PlaceHit,
    to: PlaceHit,
    routeColorArgb: Int,
    markerAColorArgb: Int,
    markerBColorArgb: Int
) {
    if (map == null) return
    CoroutineScope(Dispatchers.IO).launch {
        runCatching {
            val url =
                "https://router.project-osrm.org/route/v1/driving/${from.lon},${from.lat};${to.lon},${to.lat}?overview=full&geometries=geojson"
            val text = URL(url).openStream().bufferedReader().use { it.readText() }
            val json = JSONObject(text)
            val coords = json.getJSONArray("routes")
                .getJSONObject(0)
                .getJSONObject("geometry")
                .getJSONArray("coordinates")

            withContext(Dispatchers.Main) {
                overlaysRef.forEach { map.overlays.remove(it) }
                overlaysRef.clear()

                val iconA = dotDrawable(context, markerAColorArgb)
                val iconB = dotDrawable(context, markerBColorArgb)
                val markerA = Marker(map).apply {
                    position = GeoPoint(from.lat, from.lon)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    icon = iconA
                }
                val markerB = Marker(map).apply {
                    position = GeoPoint(to.lat, to.lon)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    icon = iconB
                }
                map.overlays.add(markerA)
                map.overlays.add(markerB)
                overlaysRef.add(markerA)
                overlaysRef.add(markerB)

                val polyline = Polyline().apply {
                    outlinePaint.color = routeColorArgb
                    outlinePaint.strokeWidth = 10f
                    setPoints((0 until coords.length()).map { idx ->
                        val c = coords.getJSONArray(idx)
                        GeoPoint(c.getDouble(1), c.getDouble(0))
                    })
                }
                map.overlays.add(polyline)
                overlaysRef.add(polyline)
                map.invalidate()
                val centerLat = (from.lat + to.lat) / 2
                val centerLon = (from.lon + to.lon) / 2
                map.controller.animateTo(GeoPoint(centerLat, centerLon))
            }
        }
    }
}

private fun dotDrawable(ctx: Context, colorArgb: Int): BitmapDrawable {
    val d = 18f * ctx.resources.displayMetrics.density
    val size = d.toInt().coerceAtLeast(12)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colorArgb }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2f, p)
    val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * ctx.resources.displayMetrics.density
        color = 0xFFFFFFFF.toInt()
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2f, border)
    return BitmapDrawable(ctx.resources, bmp)
}

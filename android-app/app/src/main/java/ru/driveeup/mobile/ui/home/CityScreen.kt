package ru.driveeup.mobile.ui.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import ru.driveeup.mobile.data.RideRepository
import ru.driveeup.mobile.domain.RideOrder
import ru.driveeup.mobile.domain.User
import ru.driveeup.mobile.domain.UserRole
import ru.driveeup.mobile.R
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

private val FieldFillGray = Color(0xFFE8E8E8)
private val BrandGreen = Color(0xFF96EA28)

private val RouteLineBlue = 0xFF29B6F6.toInt()
private val MarkerPickUp = 0xFF29B6F6.toInt()
private val MarkerDropOff = 0xFF5CB018.toInt()

data class PlaceHit(
    val display: String,
    val lat: Double,
    val lon: Double,
    val distanceKm: Double? = null
)

@Composable
fun CityScreen(
    driveCoin: Long,
    token: String,
    user: User,
    onOpenMenu: () -> Unit,
    onOpenDriveUp: () -> Unit
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    Configuration.getInstance().load(appContext, appContext.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))

    val rideRepo = remember { RideRepository() }
    val uiScope = rememberCoroutineScope()

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

    var activeRide by remember { mutableStateOf<RideOrder?>(null) }
    var rateStars by remember { mutableStateOf(0) }
    var orderingBusy by remember { mutableStateOf(false) }
    /** После «Отмена» в оценке не показываем снова, пока придёт другая поездка. */
    var dismissedRatingRideId by remember { mutableStateOf<Long?>(null) }

    suspend fun reverseGeocode(lat: Double, lon: Double): String {
        return withContext(Dispatchers.IO) {
            runCatching {
                val url = "https://nominatim.openstreetmap.org/reverse?format=json&lat=$lat&lon=$lon&accept-language=ru"
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "DriveUP-Android/1.0 (contact: support@driveeup.ru)")
                    setRequestProperty("Accept-Language", "ru,en")
                    connectTimeout = 10000
                    readTimeout = 10000
                }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code !in 200..299) return@runCatching ""
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
                val url =
                    "https://nominatim.openstreetmap.org/search?format=json&q=${URLEncoder.encode(query, "UTF-8")}&limit=8&addressdetails=1&accept-language=ru"
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "DriveUP-Android/1.0 (contact: support@driveeup.ru)")
                    setRequestProperty("Accept-Language", "ru,en")
                    connectTimeout = 10000
                    readTimeout = 10000
                }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code !in 200..299) return@runCatching emptyList()
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
                repeat(8) {
                    val p = getLastKnownGeoPoint(context)
                    if (p != null) {
                        userGeoPoint = p
                        val addr = reverseGeocode(p.latitude, p.longitude)
                        from = if (addr.isNotBlank()) addr else "Моё местоположение"
                        fromPlace = PlaceHit(from, p.latitude, p.longitude, 0.0)
                        return@LaunchedEffect
                    }
                    delay(600)
                }
            }
        }
    }

    LaunchedEffect(fromPlace, toPlace) {
        val a = fromPlace
        val b = toPlace
        if (a != null && b != null) {
            drawRouteAb(
                context,
                mapRef.value,
                overlaysRef,
                a,
                b,
                RouteLineBlue,
                MarkerPickUp,
                MarkerDropOff
            )
        }
    }

    LaunchedEffect(token, user.role) {
        if (token.isBlank() || user.role != UserRole.PASSENGER) return@LaunchedEffect
        while (true) {
            delay(3000)
            runCatching {
                activeRide = rideRepo.passengerActive(token)
            }
        }
    }

    val activeRideSnapshot = activeRide
    val needsPassengerRating =
        activeRideSnapshot != null &&
            activeRideSnapshot.status == "completed" &&
            activeRideSnapshot.driver != null &&
            activeRideSnapshot.driverRating == null &&
            activeRideSnapshot.id != dismissedRatingRideId

    val tripBlocksOrderForm = activeRideSnapshot != null && (
        activeRideSnapshot.status in listOf("searching", "accepted", "at_pickup", "in_trip") ||
            needsPassengerRating
        )

    Box(Modifier.fillMaxSize().background(Color.White)) {
        Column(Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().weight(if (tripBlocksOrderForm) 1f else 3f)) {
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

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onOpenMenu) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Меню",
                                tint = Color(0xFF1D2A08),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        val tier = loyaltyTier(user)
                        Row(
                            modifier = Modifier.clickable(onClick = onOpenDriveUp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_coin),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "$driveCoin",
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1D2A08)
                            )
                            Image(
                                painter = painterResource(loyaltyTierIconRes(tier)),
                                contentDescription = loyaltyTierLabel(tier),
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = loyaltyTierLabel(tier),
                                fontSize = 13.sp,
                                color = Color(0xFF1D2A08)
                            )
                        }
                    }

                    val rideTop = activeRide
                    if (rideTop != null) {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                            when (rideTop.status) {
                                "accepted" -> PassengerEtaBanner(minutes = rideTop.driverEtaMinutes ?: 3)
                                "at_pickup" -> {
                                    if (!rideTop.passengerExiting) {
                                        PassengerAtPickupCombined(
                                            onExitClick = {
                                                uiScope.launch {
                                                    runCatching {
                                                        rideRepo.passengerExit(token, rideTop.id)
                                                        activeRide = rideRepo.passengerActive(token)
                                                    }
                                                }
                                            }
                                        )
                                    } else {
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color.White,
                                            shadowElevation = 4.dp
                                        ) {
                                            Text(
                                                "Вы выходите к водителю",
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF1D2A08)
                                            )
                                        }
                                    }
                                }
                                "in_trip" -> PassengerInTripBanner()
                                else -> {}
                            }
                        }
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

            if (!tripBlocksOrderForm) {
            Card(
                modifier = Modifier.fillMaxWidth().weight(2f),
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(FieldFillGray)
                            .clickable {
                                activeField = "from"
                                searchQuery = from
                                fullSearchOpen = true
                            }
                    ) {
                        Text(
                            text = if (from.isBlank()) "Откуда" else from,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp)
                                .align(Alignment.CenterStart),
                            color = if (from.isBlank()) Color(0xFF8A8A8A) else Color(0xFF1F1F1F),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(FieldFillGray)
                            .clickable {
                                activeField = "to"
                                searchQuery = to
                                fullSearchOpen = true
                            }
                    ) {
                        Text(
                            text = if (to.isBlank()) "Куда" else to,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp)
                                .align(Alignment.CenterStart),
                            color = if (to.isBlank()) Color(0xFF8A8A8A) else Color(0xFF1F1F1F),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextField(
                        value = price,
                        onValueChange = { v -> price = v.filter { it.isDigit() } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        placeholder = { Text("Предложите цену", color = Color(0xFF8A8A8A)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = FieldFillGray,
                            unfocusedContainerColor = FieldFillGray,
                            disabledContainerColor = FieldFillGray,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            focusedTextColor = Color(0xFF1F1F1F),
                            unfocusedTextColor = Color(0xFF1F1F1F)
                        )
                    )
                    Button(
                        onClick = {
                            val a = fromPlace
                            val b = toPlace
                            val rub = price.toIntOrNull()
                            if (a == null || b == null || rub == null || rub <= 0) return@Button
                            if (token.isBlank()) return@Button
                            orderingBusy = true
                            uiScope.launch {
                                runCatching {
                                    activeRide = rideRepo.createRide(
                                        token = token,
                                        fromLat = a.lat,
                                        fromLon = a.lon,
                                        fromAddress = a.display,
                                        toLat = b.lat,
                                        toLon = b.lon,
                                        toAddress = b.display,
                                        priceRub = rub
                                    )
                                }
                                orderingBusy = false
                            }
                        },
                        enabled = !orderingBusy && activeRide == null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandGreen,
                            contentColor = Color(0xFF1D2A08)
                        ),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Text("Заказать", fontWeight = FontWeight.Bold)
                    }
                }
            }
            }
        }

        val ride = activeRide
        if (ride != null) {
            when (ride.status) {
                "searching" -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                        PassengerSearchingBottomSheet(
                            priceRub = ride.displayPriceRub,
                            onCancelSearch = {
                                uiScope.launch {
                                    runCatching {
                                        rideRepo.cancelPassenger(token, ride.id)
                                        activeRide = null
                                    }
                                }
                            }
                        )
                    }
                }
                "accepted", "at_pickup" -> {
                    if (ride.driver != null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                            Column {
                                PassengerDriverInfoSheet(
                                    ride = ride,
                                    onCancelTrip = {
                                        uiScope.launch {
                                            runCatching {
                                                rideRepo.cancelPassenger(token, ride.id)
                                                activeRide = null
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                "completed" -> {
                    if ((ride.driverRating == null) && ride.driver != null && ride.id != dismissedRatingRideId) {
                        PassengerRateFullScreen(
                            driverName = ride.driver.firstName.ifBlank { ride.driver.email },
                            selectedStars = rateStars,
                            onStar = { rateStars = it },
                            onDone = {
                                if (rateStars > 0) {
                                    uiScope.launch {
                                        runCatching {
                                            rideRepo.rate(token, ride.id, rateStars, "driver")
                                            activeRide = null
                                            dismissedRatingRideId = null
                                            rateStars = 0
                                        }
                                    }
                                }
                            },
                            onCancel = {
                                dismissedRatingRideId = ride.id
                                rateStars = 0
                            }
                        )
                    }
                }
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
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        placeholder = {
                            Text(
                                if (activeField == "from") "Поиск места отправления" else "Поиск места назначения",
                                color = Color(0xFF8A8A8A)
                            )
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = FieldFillGray,
                            unfocusedContainerColor = FieldFillGray,
                            disabledContainerColor = FieldFillGray,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        )
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

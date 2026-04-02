package ru.driveeup.mobile.ui.home

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import ru.driveeup.mobile.data.RideRepository
import ru.driveeup.mobile.domain.RideOrder
import ru.driveeup.mobile.domain.User
import kotlin.math.max

private val Brand = Color(0xFF96EA28)
private val BrandDark = Color(0xFF5CB018)
private val SaladOutline = Color(0xFFB8E986)
private val BlueRoute = Color(0xFF29B6F6)

@Composable
fun DriverCityScreen(
    driveCoin: Double,
    token: String,
    @Suppress("UNUSED_PARAMETER") user: User,
    onOpenMenu: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onOpenDriveUp: () -> Unit
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(appContext, appContext.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
    }

    val scope = rememberCoroutineScope()
    val repo = remember { RideRepository() }

    var freeForOrders by remember { mutableStateOf(true) }
    var feed by remember { mutableStateOf<List<RideOrder>>(emptyList()) }
    var activeTrip by remember { mutableStateOf<RideOrder?>(null) }
    var offerRide by remember { mutableStateOf<RideOrder?>(null) }
    var offerProgress by remember { mutableStateOf(1f) }
    /** Заказы, для которых таймер/прогресс уже отработал или пропущены вручную — при повторном открытии из ленты без таймера. */
    var offersWithoutCountdownIds by remember { mutableStateOf(setOf<Long>()) }
    /** Чтобы не открывать снова тот же верхний заказ после закрытия окна. */
    var previousFeedTopId by remember { mutableStateOf<Long?>(null) }
    var showOrderOptions by remember { mutableStateOf(false) }
    var showEtaPick by remember { mutableStateOf(false) }
    var pendingRideId by remember { mutableStateOf<Long?>(null) }
    var driverTab by remember { mutableStateOf(0) }
    var rateStars by remember { mutableStateOf(0) }
    var showRateSheet by remember { mutableStateOf(false) }
    var lastCompletedRideId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(token) {
        while (true) {
            delay(2500)
            runCatching {
                val a = repo.driverActive(token)
                activeTrip = a
                if (a == null && freeForOrders) {
                    feed = repo.driverFeed(token)
                }
            }
        }
    }

    LaunchedEffect(feed, freeForOrders, activeTrip, offerRide) {
        if (!freeForOrders || activeTrip != null || offerRide != null) return@LaunchedEffect
        val top = feed.firstOrNull() ?: return@LaunchedEffect
        if (previousFeedTopId != top.id) {
            previousFeedTopId = top.id
            offerRide = top
        }
    }

    LaunchedEffect(offerRide?.id, freeForOrders, showEtaPick) {
        val o = offerRide ?: return@LaunchedEffect
        if (!freeForOrders || showEtaPick) return@LaunchedEffect
        if (o.id in offersWithoutCountdownIds) return@LaunchedEffect
        offerProgress = 1f
        repeat(80) {
            delay(100)
            offerProgress = max(0f, 1f - (it + 1) / 80f)
        }
        runCatching { repo.skip(token, o.id) }
        offersWithoutCountdownIds = offersWithoutCountdownIds + o.id
        offerRide = null
    }

    if (showOrderOptions) {
        Surface(Modifier.fillMaxSize(), color = Color.White) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        Modifier
                            .size(40.dp)
                            .clickable { showOrderOptions = false },
                        shape = CircleShape,
                        color = Color(0xFFF0F0F0)
                    ) {
                        Text("<", Modifier.padding(10.dp), color = Color.Gray)
                    }
                }
                Divider(color = Color(0xFFD7D7D7))
                Text(
                    "Опции заказов",
                    Modifier.padding(16.dp),
                    color = Color(0xFF8A8A8A)
                )
                Text(
                    "Здесь будут радиус поиска, типы заказов и уведомления.",
                    Modifier.padding(horizontal = 16.dp),
                    color = Color(0xFF6C6C6C),
                    fontSize = 14.sp
                )
            }
        }
        return
    }

    Box(Modifier.fillMaxSize().background(Color.White)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(Modifier.size(42.dp).clickable(onClick = onOpenMenu), shape = CircleShape, color = Color.White) {
                    Icon(Icons.Default.Menu, null, tint = Color.Gray, modifier = Modifier.padding(10.dp))
                }
                DriverAvailabilityToggle(
                    free = freeForOrders,
                    onToggle = { freeForOrders = !freeForOrders }
                )
                Surface(
                    Modifier
                        .size(42.dp)
                        .clickable { showOrderOptions = true },
                    shape = CircleShape,
                    color = Color(0xFFF5F5F5)
                ) {
                    Text("🔑", modifier = Modifier.padding(8.dp), fontSize = 18.sp)
                }
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (driverTab) {
                    0 -> DriverFeedTab(
                        feed = feed,
                        freeForOrders = freeForOrders,
                        onItemClick = {
                            offerRide = it
                            previousFeedTopId = it.id
                        }
                    )
                    1 -> DriverIncomeTab()
                    2 -> DriverPriorityTab()
                    3 -> DriverPaymentTab()
                }
            }

            DriverBottomTabs(selected = driverTab, onSelect = { driverTab = it })
        }

        val trip = activeTrip
        if (trip != null && trip.status != "completed") {
            Surface(Modifier.fillMaxSize(), color = Color.White) {
                DriverActiveTripOverlay(
                    ride = trip,
                    token = token,
                    repo = repo,
                    onCancelled = { activeTrip = null },
                    onCompleted = { endedId ->
                        lastCompletedRideId = endedId
                        activeTrip = null
                        showRateSheet = true
                    },
                    scope = scope
                )
            }
        }

        if (offerRide != null && freeForOrders && activeTrip == null) {
            val o = offerRide!!
            val showOfferCountdown = o.id !in offersWithoutCountdownIds
            Box(Modifier.fillMaxSize()) {
                Surface(
                    Modifier
                        .fillMaxSize()
                        .background(Color(0x66000000))
                        .clickable { },
                    color = Color.Transparent
                ) {}
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    color = Color.White,
                    tonalElevation = 10.dp
                ) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text("Новый заказ", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1D2A08))
                        Spacer(Modifier.height(8.dp))
                        if (showOfferCountdown) {
                            LinearProgressIndicator(
                                progress = { offerProgress },
                                modifier = Modifier.fillMaxWidth(),
                                color = BrandDark,
                                trackColor = Color(0xFFE0E0E0)
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                        DriverNewOrderMapCard(ride = o)
                        Spacer(Modifier.height(12.dp))
                        val p = o.passenger
                        Row {
                            Column(Modifier.weight(0.28f)) {
                                Surface(
                                    modifier = Modifier.size(48.dp),
                                    shape = CircleShape,
                                    color = Color(0xFFEAEAEA)
                                ) {}
                                Text(p?.firstName ?: "—", fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("★ ${p?.ratingAvg ?: 5.0} (${p?.ridesCount ?: 0})", fontSize = 11.sp, color = Color(0xFFFFC107))
                            }
                            Column(Modifier.weight(0.72f).padding(start = 8.dp)) {
                                Text(shortAddr(o.fromAddress), fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(shortAddr(o.toAddress), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text("${o.displayPriceRub} Р", color = BrandDark, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.weight(1f, fill = true))
                        Button(
                            onClick = {
                                offersWithoutCountdownIds = offersWithoutCountdownIds + o.id
                                pendingRideId = o.id
                                showEtaPick = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Brand)
                        ) {
                            Text("Принять за ${o.displayPriceRub} Р")
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Предложите свою цену:", color = Color(0xFF6C6C6C), fontSize = 13.sp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(50, 100, 150).forEach { add ->
                                Button(
                                    onClick = {
                                        scope.launch {
                                            runCatching {
                                                repo.counterOffer(token, o.id, o.displayPriceRub + add)
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Brand),
                                    contentPadding = PaddingValues(4.dp)
                                ) {
                                    Text("${o.displayPriceRub + add} Р", fontSize = 11.sp)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    offersWithoutCountdownIds = offersWithoutCountdownIds + o.id
                                    runCatching { repo.skip(token, o.id) }
                                    offerRide = null
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0), contentColor = Color(0xFF333333))
                        ) { Text("Пропустить") }
                    }
                }
            }
        }

        if (showEtaPick && pendingRideId != null) {
            Surface(Modifier.fillMaxSize().background(Color(0x88000000)), color = Color.Transparent) {}
            Surface(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(280.dp),
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
                color = Color.White
            ) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Через сколько вы приедете?", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val id = pendingRideId ?: return@Button
                            scope.launch {
                                runCatching {
                                    repo.accept(token, id, null, 3)
                                    offerRide = null
                                    showEtaPick = false
                                    pendingRideId = null
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Brand)
                    ) { Text("3 мин") }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            showEtaPick = false
                            pendingRideId = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0), contentColor = Color(0xFF333333))
                    ) { Text("Отмена") }
                }
            }
        }

        if (showRateSheet) {
            Box(Modifier.fillMaxSize().background(Color(0x99000000))) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .heightIn(min = 260.dp),
                    shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
                    color = Color.White,
                    shadowElevation = 10.dp
                ) {
                    Column(
                        Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("Оцените заказ", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (i in 1..5) {
                                Text(
                                    "★",
                                    fontSize = 36.sp,
                                    color = if (i <= rateStars) Color(0xFFFFC107) else Color(0xFFE0E0E0),
                                    modifier = Modifier.clickable {
                                        rateStars = i
                                    }
                                )
                            }
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    val id = lastCompletedRideId
                                    if (id != null && rateStars > 0) {
                                        runCatching { repo.rate(token, id, rateStars, "passenger") }
                                    }
                                    showRateSheet = false
                                    rateStars = 0
                                    lastCompletedRideId = null
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0), contentColor = Color(0xFF333333))
                        ) { Text("Пропустить") }
                    }
                }
            }
        }
    }
}

@Composable
private fun DriverNewOrderMapCard(ride: RideOrder) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(appContext, appContext.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
    }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    val overlays = remember(ride.id) { mutableStateListOf<org.osmdroid.views.overlay.Overlay>() }
    var labelToA by remember { mutableStateOf<String?>(null) }
    var labelAB by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(mapView, ride.id) {
        val m = mapView ?: return@LaunchedEffect
        val d = getLastKnownGeoPoint(context)
        val rToA = if (d != null) {
            fetchOsrmRoute(d.latitude, d.longitude, ride.fromLat, ride.fromLon)
        } else null
        val rAB = fetchOsrmRoute(ride.fromLat, ride.fromLon, ride.toLat, ride.toLon)
        labelToA = rToA?.let { "${formatOsrmDuration(it.durationSec)} · ${formatOsrmDistance(it.distanceM)}" }
            ?: if (d == null) "Включите геолокацию" else "—"
        labelAB = rAB?.let { "${formatOsrmDuration(it.durationSec)} · ${formatOsrmDistance(it.distanceM)}" } ?: "—"
        drawDriverTwoLegRoute(
            context = context,
            map = m,
            overlaysRef = overlays,
            driverLat = d?.latitude,
            driverLon = d?.longitude,
            aLat = ride.fromLat,
            aLon = ride.fromLon,
            bLat = ride.toLat,
            bLon = ride.toLon,
            colorDriverToA = 0xFF29B6F6.toInt(),
            colorAToB = 0xFF5CB018.toInt(),
            colorMarkerA = 0xFF29B6F6.toInt(),
            colorMarkerB = 0xFF5CB018.toInt(),
            colorMarkerDriver = 0xFF42A5F5.toInt()
        )
    }

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text("До точки A", fontSize = 11.sp, color = Color(0xFF7E8580))
                Text(
                    labelToA ?: "…",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1D2A08),
                    maxLines = 2
                )
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text("От A до B", fontSize = 11.sp, color = Color(0xFF7E8580))
                Text(
                    labelAB ?: "…",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1D2A08),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFE8E8E8))
        ) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(12.0)
                        mapView = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    DisposableEffect(ride.id) {
        onDispose {
            mapView?.onDetach()
            mapView = null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DriverAvailabilityToggle(free: Boolean, onToggle: () -> Unit) {
    val border = if (free) SaladOutline else Color(0xFFE53935)
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(2.dp, border),
        color = Color.White
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (free) {
                Text("Свободен", color = Color(0xFF1D2A08), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE53935),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Text(
                        "Занят",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DriverFeedTab(
    feed: List<RideOrder>,
    freeForOrders: Boolean,
    onItemClick: (RideOrder) -> Unit
) {
    if (!freeForOrders) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Режим «Занят» — заказы скрыты", color = Color.Gray)
        }
        return
    }
    if (feed.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Brand)
                Spacer(Modifier.height(12.dp))
                Text("Поиск заказов", color = Color(0xFF8A8A8A))
            }
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(feed) { item ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onItemClick(item) }
                    .padding(12.dp)
            ) {
                Column(Modifier.weight(0.25f)) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        color = Color(0xFFEAEAEA)
                    ) {}
                    Text(item.passenger?.firstName ?: "—", fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("★ ${item.passenger?.ratingAvg ?: 5.0} (${item.passenger?.ridesCount ?: 0})", fontSize = 11.sp, color = Color(0xFFFFC107))
                    Text("Только что", fontSize = 11.sp, color = Color(0xFF8A8A8A))
                }
                Column(Modifier.weight(0.75f)) {
                    Text(shortAddr(item.fromAddress), fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 2)
                    Text(shortAddr(item.toAddress), fontSize = 13.sp, maxLines = 2)
                    Text("${item.displayPriceRub} Р", color = BrandDark, fontWeight = FontWeight.Bold)
                }
            }
            Divider()
        }
    }
}

@Composable
private fun DriverIncomeTab() {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Доходы", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF1D2A08))
        Spacer(Modifier.height(16.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FDF2)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Сегодня", color = Color(0xFF7E8580), fontSize = 13.sp)
                Text("— ₽", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = BrandDark)
            }
        }
        Spacer(Modifier.height(12.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("За неделю", color = Color(0xFF7E8580), fontSize = 13.sp)
                Text("— ₽", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Подробная статистика появится после накопления поездок.",
            fontSize = 13.sp,
            color = Color(0xFF8A8A8A)
        )
    }
}

@Composable
private fun DriverPriorityTab() {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Приоритет", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF1D2A08))
        Spacer(Modifier.height(12.dp))
        Text(
            "Заказы с высоким приоритетом и бонусом к цене будут отображаться здесь. Скоро можно будет настроить фильтры по расстоянию и рейтингу пассажира.",
            fontSize = 14.sp,
            color = Color(0xFF4E4E4E),
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun DriverPaymentTab() {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Оплата", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF1D2A08))
        Spacer(Modifier.height(12.dp))
        Text(
            "Способы получения выплат и реквизиты можно будет привязать здесь (карта, СБП). Пока все расчёты проходят в приложении по факту поездки.",
            fontSize = 14.sp,
            color = Color(0xFF4E4E4E),
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun DriverBottomTabs(selected: Int, onSelect: (Int) -> Unit) {
    val labels = listOf("Лента", "Доходы", "Приоритет", "Оплата")
    val icons = listOf("☰", "₽", "★", "👛")
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8F8F8))
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        labels.indices.forEach { i ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(i) }
                    .padding(4.dp)
            ) {
                Text(icons[i], fontSize = 18.sp)
                Text(
                    labels[i],
                    fontSize = 10.sp,
                    color = if (selected == i) BrandDark else Color.Gray,
                    fontWeight = if (selected == i) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun DriverActiveTripOverlay(
    ride: RideOrder,
    token: String,
    repo: RideRepository,
    onCancelled: () -> Unit,
    onCompleted: (Long) -> Unit,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(appContext, appContext.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
    }

    var local by remember(ride.id) { mutableStateOf(ride) }
    LaunchedEffect(ride.id, ride.status, ride.passengerExiting) {
        local = ride
    }

    var mapView by remember { mutableStateOf<MapView?>(null) }
    val overlays = remember(ride.id) { mutableStateListOf<org.osmdroid.views.overlay.Overlay>() }

    var etaRemainingSec by remember(ride.id, ride.status, ride.driverEtaMinutes) {
        mutableStateOf(((ride.driverEtaMinutes ?: 3) * 60).coerceAtLeast(0))
    }
    LaunchedEffect(ride.id, ride.status, ride.driverEtaMinutes) {
        if (ride.status != "accepted") return@LaunchedEffect
        etaRemainingSec = ((ride.driverEtaMinutes ?: 3) * 60).coerceAtLeast(0)
        while (etaRemainingSec > 0 && isActive) {
            delay(1000)
            etaRemainingSec--
        }
    }

    var pickupWaitSec by remember { mutableStateOf(0) }
    LaunchedEffect(local.status, local.id) {
        if (local.status == "at_pickup") {
            pickupWaitSec = 300
            while (pickupWaitSec > 0 && isActive) {
                delay(1000)
                pickupWaitSec--
            }
        } else {
            pickupWaitSec = 0
        }
    }

    LaunchedEffect(mapView, local.id, local.status, local.fromLat, local.toLat) {
        val m = mapView ?: return@LaunchedEffect
        while (isActive) {
            val d = getLastKnownGeoPoint(context)
            drawDriverTwoLegRoute(
                context = context,
                map = m,
                overlaysRef = overlays,
                driverLat = d?.latitude,
                driverLon = d?.longitude,
                aLat = local.fromLat,
                aLon = local.fromLon,
                bLat = local.toLat,
                bLon = local.toLon,
                colorDriverToA = 0xFF29B6F6.toInt(),
                colorAToB = 0xFF5CB018.toInt(),
                colorMarkerA = 0xFF29B6F6.toInt(),
                colorMarkerB = 0xFF5CB018.toInt(),
                colorMarkerDriver = 0xFF42A5F5.toInt()
            )
            delay(3000)
        }
    }

    val bottomSheetMinHeight = when (local.status) {
        "accepted" -> 272.dp
        "at_pickup" -> 228.dp
        else -> 212.dp
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(12.0)
                    mapView = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (local.status == "accepted") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Text(
                        "До заказчика: ${formatMmSs(etaRemainingSec)}",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = Color(0xFF1D2A08)
                    )
                }
            }
            if (local.passengerExiting) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Text(
                        "Пассажир выходит к вам",
                        modifier = Modifier.padding(14.dp),
                        color = Color(0xFF1D2A08),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }
            if (local.status == "at_pickup" && pickupWaitSec > 0) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    val mm = pickupWaitSec / 60
                    val ss = pickupWaitSec % 60
                    Text(
                        "Ожидание пассажира: ${String.format("%d:%02d", mm, ss)}",
                        modifier = Modifier.padding(14.dp),
                        color = Color(0xFF6C6C6C),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(min = bottomSheetMinHeight),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = Color.White,
            shadowElevation = 10.dp
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Отменить поездку",
                        color = Color(0xFFE53935),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            scope.launch {
                                runCatching {
                                    repo.cancelDriver(token, local.id)
                                    onCancelled()
                                }
                            }
                        }
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(shortAddr(local.fromAddress), fontWeight = FontWeight.Bold)
                Text(shortAddr(local.toAddress), color = Color.Gray, fontSize = 13.sp)
                Text("${local.displayPriceRub} Р", color = BrandDark, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                when (local.status) {
                    "accepted" -> Button(
                        onClick = {
                            scope.launch {
                                runCatching {
                                    local = repo.arrived(token, local.id)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BlueRoute)
                    ) { Text("Я на месте") }
                    "at_pickup" -> Button(
                        onClick = {
                            scope.launch {
                                runCatching {
                                    local = repo.startTrip(token, local.id)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Brand)
                    ) { Text("Начать поездку") }
                    "in_trip" -> Button(
                        onClick = {
                            scope.launch {
                                runCatching {
                                    repo.complete(token, local.id)
                                    onCompleted(local.id)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Brand)
                    ) { Text("Завершить поездку") }
                }
            }
        }

        DisposableEffect(ride.id) {
            onDispose {
                mapView?.onDetach()
                mapView = null
            }
        }
    }
}

private fun formatMmSs(totalSec: Int): String {
    val s = totalSec.coerceAtLeast(0)
    val m = s / 60
    val r = s % 60
    return String.format("%d:%02d", m, r)
}

private fun shortAddr(s: String): String {
    val t = s.split(",").map { it.trim() }.filter { it.isNotBlank() }
    return t.take(3).joinToString(", ").ifBlank { s.take(80) }
}

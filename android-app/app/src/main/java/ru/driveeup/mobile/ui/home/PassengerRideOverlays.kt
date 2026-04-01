package ru.driveeup.mobile.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.driveeup.mobile.domain.RideOrder

@Composable
fun PassengerSearchingBottomSheet(priceRub: Int) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.35f),
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        color = Color.White,
        tonalElevation = 6.dp
    ) {
        Column(
            Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Ищем водителей...",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF1D2A08)
            )
            Spacer(Modifier.height(12.dp))
            Text("Ваша цена", color = Color(0xFF7E8580), fontSize = 14.sp)
            Text(
                "$priceRub Р",
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = Color(0xFF1D2A08)
            )
        }
    }
}

@Composable
fun PassengerEtaBanner(minutes: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Text(
            "Водитель будет через ~$minutes мин",
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1D2A08)
        )
    }
}

@Composable
fun PassengerWaitingDriverBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Text(
            "Водитель ожидает",
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1D2A08)
        )
    }
}

@Composable
fun PassengerInTripBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Text(
            "В пути",
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1D2A08)
        )
    }
}

@Composable
fun PassengerDriverInfoSheet(
    ride: RideOrder,
    onCancelTrip: () -> Unit
) {
    val d = ride.driver ?: return
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.35f),
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        color = Color.White,
        tonalElevation = 6.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    "Отменить поездку",
                    color = Color(0xFFE53935),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(onClick = onCancelTrip)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                d.vehicleModel ?: "Автомобиль",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
            Text(
                d.vehiclePlate ?: "—",
                color = Color(0xFF6C6C6C),
                fontSize = 15.sp
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = Color(0xFFEAEAEA), modifier = Modifier.size(48.dp)) {}
                Column(Modifier.padding(start = 12.dp)) {
                    Text(d.firstName.ifBlank { d.email }, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("★", color = Color(0xFFFFC107), fontSize = 14.sp)
                        Text(
                            " ${d.ratingAvg} (${d.ridesCount})",
                            color = Color(0xFF6C6C6C),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PassengerRateFullScreen(
    driverName: String,
    selectedStars: Int,
    onStar: (Int) -> Unit,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(Modifier.fillMaxSize(), color = Color.White) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Оцените заказ", fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Spacer(Modifier.height(24.dp))
            Surface(shape = CircleShape, color = Color(0xFFEAEAEA), modifier = Modifier.size(72.dp)) {}
            Spacer(Modifier.height(12.dp))
            Text(driverName, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (i in 1..5) {
                    val filled = i <= selectedStars
                    Text(
                        "★",
                        fontSize = 36.sp,
                        color = if (filled) Color(0xFFFFC107) else Color(0xFFE0E0E0),
                        modifier = Modifier
                            .clickable { onStar(i) }
                            .padding(4.dp)
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0), contentColor = Color(0xFF333333))
            ) { Text("Готово") }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0), contentColor = Color(0xFF333333))
            ) { Text("Отмена") }
        }
    }
}

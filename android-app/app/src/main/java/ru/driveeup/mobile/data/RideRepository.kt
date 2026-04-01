package ru.driveeup.mobile.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import ru.driveeup.mobile.domain.RideOrder
import ru.driveeup.mobile.domain.RideUserBrief
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class RideRepository {
    private val apiBase = "https://driveeup.ru/api"

    suspend fun createRide(
        token: String,
        fromLat: Double,
        fromLon: Double,
        fromAddress: String,
        toLat: Double,
        toLon: Double,
        toAddress: String,
        priceRub: Int
    ): RideOrder = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("fromLat", fromLat)
            .put("fromLon", fromLon)
            .put("fromAddress", fromAddress)
            .put("toLat", toLat)
            .put("toLon", toLon)
            .put("toAddress", toAddress)
            .put("priceRub", priceRub)
        val json = postJson("/rides", body.toString(), token)
        parseRide(json)
    }

    suspend fun passengerActive(token: String): RideOrder? = withContext(Dispatchers.IO) {
        val json = getJson("/rides/passenger/active", token)
        val ride = json.optJSONObject("ride") ?: return@withContext null
        parseRide(ride)
    }

    suspend fun driverActive(token: String): RideOrder? = withContext(Dispatchers.IO) {
        val json = getJson("/rides/driver/active", token)
        val ride = json.optJSONObject("ride") ?: return@withContext null
        parseRide(ride)
    }

    suspend fun driverFeed(token: String): List<RideOrder> = withContext(Dispatchers.IO) {
        val json = getJson("/rides/driver/feed", token)
        val arr = json.optJSONArray("orders") ?: return@withContext emptyList()
        (0 until arr.length()).map { parseRide(arr.getJSONObject(it)) }
    }

    suspend fun getRide(token: String, id: Long): RideOrder = withContext(Dispatchers.IO) {
        parseRide(getJson("/rides/$id", token))
    }

    suspend fun skip(token: String, id: Long) = withContext(Dispatchers.IO) {
        postJson("/rides/$id/skip", "{}", token)
    }

    suspend fun counterOffer(token: String, id: Long, priceRub: Int): RideOrder = withContext(Dispatchers.IO) {
        val body = JSONObject().put("priceRub", priceRub)
        parseRide(postJson("/rides/$id/counter", body.toString(), token))
    }

    suspend fun accept(token: String, id: Long, agreedPriceRub: Int?, etaMinutes: Int): RideOrder =
        withContext(Dispatchers.IO) {
            val body = JSONObject().put("etaMinutes", etaMinutes)
            if (agreedPriceRub != null) body.put("agreedPriceRub", agreedPriceRub)
            parseRide(postJson("/rides/$id/accept", body.toString(), token))
        }

    suspend fun arrived(token: String, id: Long): RideOrder = withContext(Dispatchers.IO) {
        parseRide(postJson("/rides/$id/arrived", "{}", token))
    }

    suspend fun startTrip(token: String, id: Long): RideOrder = withContext(Dispatchers.IO) {
        parseRide(postJson("/rides/$id/start-trip", "{}", token))
    }

    suspend fun complete(token: String, id: Long): RideOrder = withContext(Dispatchers.IO) {
        parseRide(postJson("/rides/$id/complete", "{}", token))
    }

    suspend fun passengerExit(token: String, id: Long): RideOrder = withContext(Dispatchers.IO) {
        parseRide(postJson("/rides/$id/passenger-exit", "{}", token))
    }

    suspend fun cancelPassenger(token: String, id: Long) = withContext(Dispatchers.IO) {
        postJson("/rides/$id/cancel-passenger", "{}", token)
    }

    suspend fun cancelDriver(token: String, id: Long) = withContext(Dispatchers.IO) {
        postJson("/rides/$id/cancel-driver", "{}", token)
    }

    suspend fun rate(token: String, id: Long, stars: Int, target: String) = withContext(Dispatchers.IO) {
        val body = JSONObject().put("stars", stars).put("target", target)
        postJson("/rides/$id/rate", body.toString(), token)
    }

    private fun getJson(path: String, token: String): JSONObject = requestJson("GET", path, null, token)

    private fun postJson(path: String, body: String, token: String): JSONObject =
        requestJson("POST", path, body, token)

    private fun requestJson(method: String, path: String, body: String?, token: String): JSONObject {
        val connection = (URL("$apiBase$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15000
            readTimeout = 15000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $token")
            doInput = true
            if (body != null) doOutput = true
        }
        try {
            if (body != null) {
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(body) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
            val json = if (responseText.isNotBlank()) JSONObject(responseText) else JSONObject()
            if (status !in 200..299) {
                throw IllegalStateException(json.optString("error", "Ошибка API: HTTP $status"))
            }
            return json
        } finally {
            connection.disconnect()
        }
    }

    private fun parseRide(json: JSONObject): RideOrder {
        fun userBrief(o: JSONObject?): RideUserBrief? {
            if (o == null) return null
            return RideUserBrief(
                id = o.optLong("id"),
                firstName = o.optString("firstName"),
                lastName = o.optString("lastName"),
                email = o.optString("email"),
                avatarUrl = o.optString("avatarUrl").ifBlank { null },
                ratingAvg = o.optDouble("ratingAvg", 5.0),
                ridesCount = o.optLong("ridesCount"),
                vehicleModel = o.optString("vehicleModel").ifBlank { null },
                vehiclePlate = o.optString("vehiclePlate").ifBlank { null }
            )
        }
        return RideOrder(
            id = json.optLong("id"),
            passengerId = json.optLong("passengerId"),
            driverId = if (json.isNull("driverId")) null else json.optLong("driverId"),
            fromLat = json.optDouble("fromLat"),
            fromLon = json.optDouble("fromLon"),
            fromAddress = json.optString("fromAddress"),
            toLat = json.optDouble("toLat"),
            toLon = json.optDouble("toLon"),
            toAddress = json.optString("toAddress"),
            priceRub = json.optInt("priceRub"),
            agreedPriceRub = if (json.isNull("agreedPriceRub")) null else json.optInt("agreedPriceRub"),
            displayPriceRub = json.optInt("displayPriceRub"),
            status = json.optString("status"),
            driverEtaMinutes = if (json.isNull("driverEtaMinutes")) null else json.optInt("driverEtaMinutes"),
            passengerExiting = json.optBoolean("passengerExiting"),
            passengerRating = if (json.isNull("passengerRating")) null else json.optInt("passengerRating"),
            driverRating = if (json.isNull("driverRating")) null else json.optInt("driverRating"),
            createdAt = json.optString("createdAt").ifBlank { null },
            passenger = userBrief(json.optJSONObject("passenger")),
            driver = userBrief(json.optJSONObject("driver"))
        )
    }
}

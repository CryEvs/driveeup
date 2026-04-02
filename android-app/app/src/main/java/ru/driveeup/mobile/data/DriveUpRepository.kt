package ru.driveeup.mobile.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import ru.driveeup.mobile.domain.DriveUpContent
import ru.driveeup.mobile.domain.DriveUpStoreItem
import ru.driveeup.mobile.domain.DriveUpTaskItem
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class DriveUpRepository {
    private val apiBase = "https://driveeup.ru/api"

    suspend fun content(token: String): DriveUpContent = withContext(Dispatchers.IO) {
        val json = getJson("/driveup/content", token)
        parseContent(json)
    }

    suspend fun storeItems(token: String): List<DriveUpStoreItem> = withContext(Dispatchers.IO) {
        val arr = getJsonArray("/driveup/store/items", token)
        (0 until arr.length()).map { parseStoreItem(arr.getJSONObject(it)) }
    }

    suspend fun tasks(token: String): List<DriveUpTaskItem> = withContext(Dispatchers.IO) {
        val arr = getJsonArray("/driveup/tasks", token)
        (0 until arr.length()).map { parseTask(arr.getJSONObject(it)) }
    }

    private fun getJson(path: String, token: String): JSONObject = requestJson("GET", path, null, token)
    private fun getJsonArray(path: String, token: String): org.json.JSONArray = requestJsonArray("GET", path, null, token)

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

    private fun requestJsonArray(method: String, path: String, body: String?, token: String): org.json.JSONArray {
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
            if (status !in 200..299) {
                val json = if (responseText.isNotBlank()) JSONObject(responseText) else JSONObject()
                throw IllegalStateException(json.optString("error", "Ошибка API: HTTP $status"))
            }
            return if (responseText.isBlank()) org.json.JSONArray() else org.json.JSONArray(responseText)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseContent(json: JSONObject): DriveUpContent {
        val itemsArr = json.optJSONArray("storeItems") ?: org.json.JSONArray()
        val tasksArr = json.optJSONArray("tasks") ?: org.json.JSONArray()
        val items = (0 until itemsArr.length()).map { parseStoreItem(itemsArr.getJSONObject(it)) }
        val tasks = (0 until tasksArr.length()).map { parseTask(tasksArr.getJSONObject(it)) }
        return DriveUpContent(
            loyaltyTier = json.optString("loyaltyTier", "BRONZE"),
            driveCoin = json.optLong("driveCoin", 0L),
            ridesCount = json.optLong("ridesCount", 0L),
            nextRideBenefitForTier = json.optString("nextRideBenefitForTier", ""),
            storeItems = items,
            tasks = tasks
        )
    }

    private fun parseStoreItem(json: JSONObject): DriveUpStoreItem = DriveUpStoreItem(
        id = json.optLong("id"),
        name = json.optString("name"),
        iconUrl = json.optString("iconUrl").ifBlank { null },
        shortDescription = json.optString("shortDescription"),
        allowedTier = json.optString("allowedTier", "ANY"),
        description = json.optString("description"),
        usageTerms = json.optString("usageTerms"),
        validityText = json.optString("validityText"),
        priceDriveCoin = json.optLong("priceDriveCoin"),
        isAvailableForCurrentTier = json.optBoolean("isAvailableForCurrentTier", true),
        sortOrder = json.optInt("sortOrder", 0)
    )

    private fun parseTask(json: JSONObject): DriveUpTaskItem = DriveUpTaskItem(
        id = json.optLong("id"),
        title = json.optString("title"),
        description = json.optString("description"),
        rewardDriveCoin = json.optLong("rewardDriveCoin"),
        sortOrder = json.optInt("sortOrder", 0)
    )
}


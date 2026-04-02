package ru.driveeup.mobile.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import ru.driveeup.mobile.domain.DriveUpContent
import ru.driveeup.mobile.domain.DriveUpNotification
import ru.driveeup.mobile.domain.DriveUpStoreItem
import ru.driveeup.mobile.domain.AchievementItem
import ru.driveeup.mobile.domain.DriveUpTaskItem
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray

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

    suspend fun purchaseStoreItem(token: String, itemId: Long): JSONObject = withContext(Dispatchers.IO) {
        requestJson("POST", "/driveup/store/items/$itemId/purchase", "{}", token)
    }

    suspend fun notifications(token: String): List<DriveUpNotification> = withContext(Dispatchers.IO) {
        val arr = getJsonArray("/driveup/notifications", token)
        (0 until arr.length()).map { parseNotification(arr.getJSONObject(it)) }
    }

    suspend fun battlePassCurrent(token: String): JSONObject = withContext(Dispatchers.IO) {
        getJson("/battle-pass/current", token)
    }

    suspend fun claimBattlePassGift(token: String, levelId: Long): JSONObject = withContext(Dispatchers.IO) {
        requestJson("POST", "/battle-pass/levels/$levelId/claim-gift", "{}", token)
    }

    suspend fun achievementsList(token: String): List<AchievementItem> = withContext(Dispatchers.IO) {
        val arr = getJsonArray("/achievements", token)
        (0 until arr.length()).map { parseAchievement(arr.getJSONObject(it)) }
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
        val itemsArr = json.optJSONArray("storeItems") ?: JSONArray()
        val tasksArr = json.optJSONArray("tasks") ?: JSONArray()
        val descriptionsObj = json.optJSONObject("loyaltyLevelDescriptions") ?: JSONObject()
        val thresholdsObj = json.optJSONObject("loyaltyRidesThresholds") ?: JSONObject()
        val items = (0 until itemsArr.length()).map { parseStoreItem(itemsArr.getJSONObject(it)) }
        val tasks = (0 until tasksArr.length()).map { parseTask(tasksArr.getJSONObject(it)) }
        return DriveUpContent(
            loyaltyTier = json.optString("loyaltyTier", "BRONZE"),
            driveCoin = json.optDouble("driveCoin", 0.0),
            ridesCount = json.optLong("ridesCount", 0L),
            nextRideBenefitForTier = json.optString("nextRideBenefitForTier", ""),
            nextRideStoreItemName = json.optString("nextRideStoreItemName")
                .takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) },
            loyaltyLevelDescriptions = parseStringMap(descriptionsObj),
            loyaltyRidesThresholds = parseLongMap(thresholdsObj),
            storeItems = items,
            tasks = tasks
        )
    }

    private fun parseStringMap(json: JSONObject): Map<String, String> {
        val result = linkedMapOf<String, String>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            result[key] = json.optString(key, "")
        }
        return result
    }

    private fun parseLongMap(json: JSONObject): Map<String, Long> {
        val result = linkedMapOf<String, Long>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            result[key] = json.optLong(key, 0L)
        }
        return result
    }

    private fun parseStoreItem(json: JSONObject): DriveUpStoreItem = DriveUpStoreItem(
        id = json.optLong("id"),
        name = json.optString("name"),
        iconUrl = json.optString("iconUrl").takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) },
        shortDescription = json.optString("shortDescription"),
        allowedTier = json.optString("allowedTier", "ANY"),
        itemType = json.optString("itemType", "DISCOUNT"),
        discountPercent = if (json.has("discountPercent") && !json.isNull("discountPercent")) json.optInt("discountPercent") else null,
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

    private fun parseAchievement(json: JSONObject): AchievementItem {
        val rawIcon = json.optString("iconUrl").takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }
            ?: json.optString("icon_url").takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }
        return AchievementItem(
            id = json.optLong("id"),
            title = json.optString("title"),
            description = json.optString("description"),
            iconUrl = resolvePublicAssetUrl(rawIcon),
        )
    }

    /** Абсолютный URL для картинок (относительные пути относительно сайта, не только /api). */
    private fun resolvePublicAssetUrl(raw: String?): String? {
        val s = raw?.trim()?.takeIf { it.isNotBlank() } ?: return null
        if (s.startsWith("http://", ignoreCase = true) || s.startsWith("https://", ignoreCase = true)) {
            return s
        }
        val origin = apiBase.trimEnd('/').removeSuffix("/api")
        return if (s.startsWith("/")) origin + s else "$origin/$s"
    }

    private fun parseNotification(json: JSONObject): DriveUpNotification = DriveUpNotification(
        id = json.optLong("id"),
        type = json.optString("type"),
        title = json.optString("title"),
        body = json.optString("body"),
        createdAt = json.optString("createdAt").ifBlank { null }
    )
}


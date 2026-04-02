package ru.driveeup.mobile.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import ru.driveeup.mobile.domain.AuthResult
import ru.driveeup.mobile.domain.User
import ru.driveeup.mobile.domain.UserRole
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class AuthRepository {
    private val apiBase = "https://driveeup.ru/api"

    suspend fun login(email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("email", email)
            .put("password", password)

        val response = postJson("/auth/login", payload.toString(), token = null)
        val accessToken = response.optString("accessToken")
        if (accessToken.isBlank()) throw IllegalStateException(response.optString("error", "Ошибка входа"))
        val user = parseUser(response.getJSONObject("user"))
        AuthResult(accessToken, user)
    }

    suspend fun register(email: String, password: String, role: UserRole): AuthResult = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("email", email)
            .put("password", password)
            .put("role", role.name)

        val response = postJson("/auth/register", payload.toString(), token = null)
        val accessToken = response.optString("accessToken")
        if (accessToken.isBlank()) throw IllegalStateException(response.optString("error", "Ошибка регистрации"))
        val user = parseUser(response.getJSONObject("user"))
        AuthResult(accessToken, user)
    }

    suspend fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String
    ): AuthResult = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("firstName", firstName)
            .put("lastName", lastName)
            .put("email", email)
            .put("password", password)
            .put("role", UserRole.PASSENGER.name)

        val response = postJson("/auth/register", payload.toString(), token = null)
        val accessToken = response.optString("accessToken")
        if (accessToken.isBlank()) throw IllegalStateException(response.optString("error", "Ошибка регистрации"))
        val user = parseUser(response.getJSONObject("user"))
        AuthResult(accessToken, user)
    }

    suspend fun me(token: String): User = withContext(Dispatchers.IO) {
        val response = getJson("/auth/me", token)
        parseUser(response)
    }

    suspend fun updateAvatar(token: String, avatarUrl: String): User = withContext(Dispatchers.IO) {
        val payload = JSONObject().put("avatarUrl", avatarUrl)
        val response = putJson("/auth/avatar", payload.toString(), token)
        parseUser(response)
    }

    suspend fun updateProfile(
        token: String,
        firstName: String,
        lastName: String,
        email: String,
        city: String
    ): User = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("firstName", firstName)
            .put("lastName", lastName)
            .put("email", email)
            .put("city", city)
        val response = putJson("/auth/profile", payload.toString(), token)
        parseUser(response)
    }

    suspend fun setRole(token: String, role: UserRole): User = withContext(Dispatchers.IO) {
        val payload = JSONObject().put("role", role.name)
        val response = putJson("/auth/role", payload.toString(), token)
        parseUser(response)
    }

    private fun getJson(path: String, token: String?): JSONObject {
        return requestJson("GET", path, body = null, token = token)
    }

    private fun postJson(path: String, body: String, token: String?): JSONObject {
        return requestJson("POST", path, body = body, token = token)
    }

    private fun putJson(path: String, body: String, token: String?): JSONObject {
        return requestJson("PUT", path, body = body, token = token)
    }

    private fun requestJson(method: String, path: String, body: String?, token: String?): JSONObject {
        val connection = (URL("$apiBase$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10000
            readTimeout = 10000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json")
            if (!token.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $token")
            }
            doInput = true
            if (body != null) {
                doOutput = true
            }
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

    private fun parseUser(json: JSONObject): User {
        return User(
            id = json.optLong("id"),
            email = json.optString("email"),
            firstName = json.optString("firstName"),
            lastName = json.optString("lastName"),
            city = json.optString("city"),
            role = runCatching { UserRole.valueOf(json.optString("role", UserRole.PASSENGER.name)) }
                .getOrDefault(UserRole.PASSENGER),
            driveCoin = if (json.has("driveCoin")) json.optLong("driveCoin") else json.optLong("driveeCoin"),
            totalDriveCoin = json.optLong("totalDriveCoin"),
            premium = json.optBoolean("premium"),
            avatarUrl = json.optString("avatarUrl").ifBlank { null },
            ratingAvg = json.optDouble("ratingAvg", 5.0),
            ridesCount = json.optLong("ridesCount"),
            vehicleModel = json.optString("vehicleModel").ifBlank { null },
            vehiclePlate = json.optString("vehiclePlate").ifBlank { null }
        )
    }
}

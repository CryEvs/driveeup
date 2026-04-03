package ru.driveeup.mobile.data

import android.util.Log

/**
 * Иконки достижений на сервере (Laravel):
 * - загрузка: POST /api/admin/achievements/icon → в БД пишется `icon_url`;
 * - отдача файла: GET /api/achievements/icons/{path} → файл из `storage/app/public/achievement-icons/{path}`;
 * - в JSON списка: поле `iconUrl` (camelCase), абсолютный https URL.
 *
 * Старые ссылки `/storage/achievement-icons/...` в приложении приводим к `/api/achievements/icons/...`:
 * иначе фронтовой nginx часто отдаёт `index.html` (React), а не файл — Coil получает HTML вместо PNG.
 */
object AchievementIconUrl {
    private const val TAG = "AchievementIconUrl"

    const val DEFAULT_API_BASE = "https://driveeup.ru/api"

    private val allowedPathPrefixes = arrayOf(
        "/api/achievements/icons/",
        "/storage/achievement-icons/",
        "/storage/",
    )

    /**
     * Приводит значение из JSON к абсолютному https URL или возвращает null, если ссылка небезопасна/битая.
     */
    fun normalize(raw: String?, apiBase: String = DEFAULT_API_BASE): String? {
        val s =
            raw
                ?.trim()
                ?.removePrefix("\uFEFF")
                ?.trim()
                ?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                ?: return null

        if (s.contains("javascript:", ignoreCase = true) || s.contains("data:text/html", ignoreCase = true)) {
            Log.w(TAG, "отклонён небезопасный URL")
            return null
        }

        val siteOrigin = siteOriginFromApiBase(apiBase)

        var url = when {
            s.startsWith("//") -> "https:$s"
            s.startsWith("http://", ignoreCase = true) || s.startsWith("https://", ignoreCase = true) -> s
            s.startsWith("/") -> siteOrigin + s
            else -> "$siteOrigin/${s.trimStart('/')}"
        }

        url = upgradeHttpToHttpsIfApiUsesHttps(url, apiBase)
        url = rewriteLocalhostToSiteOrigin(url, siteOrigin)
        url = rewriteAchievementStorageUrlToApi(url, siteOrigin)

        val uri = try {
            java.net.URI(url)
        } catch (e: Exception) {
            Log.w(TAG, "некорректный URL после нормализации: $url", e)
            return null
        }

        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") {
            Log.w(TAG, "нужна схема http(s): $url")
            return null
        }

        if (scheme == "http" && apiBase.startsWith("https://", ignoreCase = true)) {
            Log.w(TAG, "остался http при https API — отклонено: $url")
            return null
        }

        val path = uri.path ?: return null
        if (!isAllowedIconPath(path)) {
            Log.w(TAG, "неожиданный path (всё равно пробуем загрузить): $path")
        }

        // Убираем ?query и #fragment — для статики они часто ломают совпадение пути (404), в JSON иногда мусор.
        return stripQueryAndFragment(url)
    }

    /** Удаляет `?…` и `#…` — Coil/OkHttp иначе могут запросить другой ресурс, чем в браузере по «чистому» URL. */
    private fun stripQueryAndFragment(url: String): String {
        val cutHash = url.indexOf('#').takeIf { it >= 0 } ?: url.length
        val noHash = url.substring(0, cutHash)
        val q = noHash.indexOf('?')
        return if (q >= 0) noHash.substring(0, q) else noHash
    }

    private fun siteOriginFromApiBase(apiBase: String): String {
        return apiBase.trimEnd('/').removeSuffix("/api")
    }

    private fun upgradeHttpToHttpsIfApiUsesHttps(url: String, apiBase: String): String {
        if (!apiBase.startsWith("https://", ignoreCase = true)) return url
        if (!url.startsWith("http://", ignoreCase = true)) return url
        return "https://" + url.substring(7)
    }

    private fun rewriteLocalhostToSiteOrigin(fullUrl: String, siteOrigin: String): String {
        return try {
            val uri = java.net.URI(fullUrl)
            val host = uri.host?.lowercase() ?: return fullUrl
            val bad =
                host == "localhost" ||
                    host == "127.0.0.1" ||
                    host == "10.0.2.2" ||
                    host.endsWith(".localhost")
            val path = uri.rawPath ?: return fullUrl
            val query = uri.rawQuery?.let { "?$it" } ?: ""
            if (bad && (path.startsWith("/api/") || path.startsWith("/storage/"))) {
                siteOrigin.trimEnd('/') + path + query
            } else {
                fullUrl
            }
        } catch (_: Exception) {
            fullUrl
        }
    }

    private fun isAllowedIconPath(path: String): Boolean {
        return allowedPathPrefixes.any { path.startsWith(it) }
    }

    /**
     * `/storage/achievement-icons/x` на проде часто попадает под SPA (отдаётся HTML) — грузим только через Laravel API.
     */
    private fun rewriteAchievementStorageUrlToApi(fullUrl: String, siteOrigin: String): String {
        return try {
            val uri = java.net.URI(fullUrl)
            val path = uri.path ?: return fullUrl
            val storagePrefix = "/storage/achievement-icons/"
            if (!path.startsWith(storagePrefix)) {
                return fullUrl
            }
            val name = path.removePrefix(storagePrefix).trimStart('/')
            if (name.isEmpty() || name.contains('/')) {
                return fullUrl
            }
            val origin = siteOrigin.trimEnd('/')
            "$origin/api/achievements/icons/$name"
        } catch (_: Exception) {
            fullUrl
        }
    }
}

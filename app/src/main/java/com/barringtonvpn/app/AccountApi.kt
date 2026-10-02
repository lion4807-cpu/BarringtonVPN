package com.barringtonvpn.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

data class AccountInfo(
    val ok: Boolean,
    val username: String,
    val keyName: String,
    val expiresAt: String,
    val daysLeft: Int,
    val isBanned: Boolean,
    val isExpired: Boolean,
    val trafficUsedGb: Double,
    val trafficLimitGb: Double,
    val devicesActive: Int,
    val devicesLimit: Int,
    val subJsonUrl: String
)

object AccountApi {
    // Базовый URL эндпоинта статуса аккаунта на сервере BarringtonVPN.
    private const val BASE_URL = "https://barrgo.fun/api/account/"

    suspend fun fetch(token: String): Result<AccountInfo> = withContext(Dispatchers.IO) {
        try {
            val cleanToken = token.trim()
            val url = URL(BASE_URL + cleanToken)
            val conn = url.openConnection() as HttpsURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.requestMethod = "GET"

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""

            if (code !in 200..299) {
                return@withContext Result.failure(Exception("HTTP $code: $text"))
            }

            val j = JSONObject(text)
            val info = AccountInfo(
                ok = j.optBoolean("ok", false),
                username = j.optString("username", "?"),
                keyName = j.optString("key_name", "?"),
                expiresAt = j.optString("expires_at", "?"),
                daysLeft = j.optInt("days_left", 0),
                isBanned = j.optBoolean("is_banned", false),
                isExpired = j.optBoolean("is_expired", false),
                trafficUsedGb = j.optDouble("traffic_used_gb", 0.0),
                trafficLimitGb = j.optDouble("traffic_limit_gb", 0.0),
                devicesActive = j.optInt("devices_active", 0),
                devicesLimit = j.optInt("devices_limit", 0),
                subJsonUrl = j.optString("sub_json_url", "")
            )
            Result.success(info)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

package com.barringtonvpn.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

data class ServerProfile(
    val index: Int,
    val protocol: String,
    val address: String,
    val port: Int,
    val security: String,
    val network: String,
    val flag: String,
    val label: String
)

object SubscriptionApi {
    // Та же подписка, что уже используется в Happ (JSON-формат Xray).
    private const val BASE_URL = "https://barrgo.fun/FN3mkzIeIl/"

    suspend fun fetch(token: String): Result<List<ServerProfile>> = withContext(Dispatchers.IO) {
        try {
            val url = URL(BASE_URL + token.trim())
            val conn = url.openConnection() as HttpsURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.requestMethod = "GET"

            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) return@withContext Result.failure(Exception("HTTP $code"))

            val arr = JSONArray(body)
            val result = mutableListOf<ServerProfile>()

            for (i in 0 until arr.length()) {
                val profile = arr.optJSONObject(i) ?: continue
                val outbounds = profile.optJSONArray("outbounds") ?: continue

                var picked: JSONObject? = null
                for (j in 0 until outbounds.length()) {
                    val ob = outbounds.optJSONObject(j) ?: continue
                    if (ob.optString("tag", "") == "proxy") { picked = ob; break }
                }
                if (picked == null) {
                    for (j in 0 until outbounds.length()) {
                        val ob = outbounds.optJSONObject(j) ?: continue
                        val proto = ob.optString("protocol", "")
                        if (proto != "freedom" && proto != "blackhole" && proto != "dns") { picked = ob; break }
                    }
                }
                val finalPicked = picked ?: continue

                val protocol = finalPicked.optString("protocol", "?")
                val settings = finalPicked.optJSONObject("settings") ?: JSONObject()
                val address = settings.optString("address", "?")
                val port = settings.optInt("port", 0)
                val streamSettings = finalPicked.optJSONObject("streamSettings") ?: JSONObject()
                val security = streamSettings.optString("security", "none")
                val network = streamSettings.optString("network", "tcp")

                val lower = address.lowercase()
                val flag = when {
                    "fr" in lower -> "\uD83C\uDDEB\uD83C\uDDF7"
                    "ch" in lower || "cz" in lower -> "\uD83C\uDDE8\uD83C\uDDFF"
                    "nl" in lower -> "\uD83C\uDDF3\uD83C\uDDF1"
                    "de" in lower -> "\uD83C\uDDE9\uD83C\uDDEA"
                    else -> "\uD83C\uDF10"
                }
                val label = address.substringBefore(".")
                result.add(ServerProfile(i, protocol, address, port, security, network, flag, label))
            }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

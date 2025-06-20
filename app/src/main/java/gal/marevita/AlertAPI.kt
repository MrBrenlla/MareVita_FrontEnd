package gal.marevita

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
data class Alert(
    val id: String? = null,
    val name: String,
    val owner: String? = null,
    val relatedCapture: String? = null,
    val gpsLocation: GpsLocation? = null,
    val location: String? = null,
    val baits: List<String>,
    val fish: List<Fish>,
    @SerialName("weatherConditions")
    val weatherConditions: List<WeatherCondition>,
    val activated: List<Period> = emptyList()
) {
    fun fishStringList(): String {
        if (fish.isEmpty()) return ""
        return fish.joinToString(", ", transform = { it.name }).replaceLastCommaWith(" e")
    }

    fun baitStringList(): String {
        if (baits.isEmpty()) return ""
        return baits.joinToString(", ").replaceLastCommaWith(" e")
    }

    private fun String.replaceLastCommaWith(replacement: String): String {
        val lastComma = this.lastIndexOf(", ")
        return if (lastComma != -1) {
            this.substring(0, lastComma) + replacement + this.substring(lastComma + 1)
        } else this
    }
}

class AlertAPI {

    private val client = OkHttpClient()
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    private val baseUrl = "${AppConfig.BASE_URL}/alert"

    fun sendAlert(alert: Alert, callback: (Boolean, String?) -> Unit) {
        val jsonBody = json.encodeToString(alert)
        val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

        var request: Request

        if (alert.id == null) {
            request = Request.Builder()
                .url("$baseUrl/new")
                .addHeader("Authorization", "Bearer ${AppConfig.token}")
                .post(requestBody)
                .build()
        } else {
            request = Request.Builder()
                .url("$baseUrl/${alert.id}")
                .addHeader("Authorization", "Bearer ${AppConfig.token}")
                .put(requestBody)
                .build()
        }

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                callback(false, e.message)
            }

            override fun onResponse(call: okhttp3.Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        callback(true, it.body?.string())
                    } else {
                        callback(false, "Error: ${it.code} - ${it.message}")
                    }
                }
            }
        })
    }

    fun getAlert(alertId: String, callback: (Boolean, Alert?, String?) -> Unit) {
        val url = "$baseUrl/$alertId"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${AppConfig.token}")
            .get()
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                callback(false, null, e.message)
            }

            override fun onResponse(call: okhttp3.Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        val bodyString = it.body?.string()
                        try {
                            val alert = json.decodeFromString<Alert>(bodyString ?: "")
                            callback(true, alert, null)
                        } catch (e: Exception) {
                            callback(false, null, "Parse error: ${e.message}")
                        }
                    } else {
                        callback(false, null, "Error: ${it.code} - ${it.message}")
                    }
                }
            }
        })
    }

    fun getAlerts(callback: (Boolean, List<Alert>?, String?) -> Unit) {
        val url = baseUrl

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${AppConfig.token}")
            .get()
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                callback(false, null, e.message)
            }

            override fun onResponse(call: okhttp3.Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        val bodyString = it.body?.string()
                        try {
                            val alerts = json.decodeFromString<List<Alert>>(bodyString ?: "")
                            callback(true, alerts, null)
                        } catch (e: Exception) {
                            callback(false, null, "Parse error: ${e.message}")
                        }
                    } else {
                        callback(false, null, "Error: ${it.code} - ${it.message}")
                    }
                }
            }
        })
    }

    fun deleteAlert(alertId: String, callback: (Boolean, String?) -> Unit) {
        val url = "$baseUrl/$alertId"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${AppConfig.token}")
            .delete()
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                callback(false, e.message)
            }

            override fun onResponse(call: okhttp3.Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        callback(true, it.body?.string())
                    } else {
                        callback(false, "Error: ${it.code} - ${it.message}")
                    }
                }
            }
        })
    }
}

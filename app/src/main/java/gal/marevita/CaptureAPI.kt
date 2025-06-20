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
import java.time.ZonedDateTime

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
data class GpsLocation(
    val latitude: Double,
    val longitude: Double
)

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
data class Fish(
    val name: String,
    val quantity: Int? = null
)

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
data class WeatherCondition(
    val value: Double,
    val name: String,
    val error: Double? = null
)

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
data class Capture(
    val security: Int? = null,
    val dateTime: String? = null,
    val gpsLocation: GpsLocation? = null,
    val imageCaption: String? = null,
    val images: List<String> = emptyList(),
    val baits: List<String> = emptyList(),
    val fish: List<Fish> = emptyList(),
    val likes: List<String> = emptyList(),
    @SerialName("weatherConditions")
    val weatherConditions: List<WeatherCondition> = emptyList(),
    val id: String? = null,
    val owner: String? = null,
    val location: String? = null
) {
    fun fishStringList(): String {
        if (fish.isEmpty()) return ""

        var aux = fish[0].name
        for (i in 1 until fish.size - 1) {
            aux += ", ${fish[i].name}"
        }
        if (fish.size > 1) aux += " e ${fish[fish.size - 1].name}"
        return aux
    }

    fun fishNumberedStringList(): String {
        if (fish.isEmpty()) return ""

        var fish = fish.filter { it.quantity != null }

        var aux = fish[0].quantity.toString() + " " + fish[0].name
        for (i in 1 until fish.size - 1) {
            aux += ", ${fish[i].quantity} ${fish[i].name}"
        }
        if (fish.size > 1) aux += " e ${fish[fish.size - 1].quantity} ${fish[fish.size - 1].name}"
        return aux
    }

    fun baitStringList(): String {
        if (baits.isEmpty()) return ""

        var aux = baits[0]
        for (i in 1 until baits.size - 1) {
            aux += ", ${baits[i]}"
        }
        if (baits.size > 1) aux += " e ${baits[baits.size - 1]}"
        return aux
    }
}

private val url = AppConfig.BASE_URL

class CaptureAPI {

    private val client = OkHttpClient()
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    fun sendCapture(capture: Capture, callback: (Boolean, String?) -> Unit) {
        val url = url + "/capture/new"

        val jsonBody = json.encodeToString(capture)

        val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${AppConfig.token}")
            .post(requestBody)
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

    fun getCapture(captureId: String, callback: (Boolean, Capture?, String?) -> Unit) {
        val url = "$url/capture/$captureId"

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
                        if (bodyString != null) {
                            try {
                                val capture = json.decodeFromString<Capture>(bodyString)
                                callback(true, capture, null)
                            } catch (e: Exception) {
                                callback(false, null, "Parse error: ${e.message}")
                            }
                        } else {
                            callback(false, null, "Empty response body")
                        }
                    } else {
                        callback(false, null, "Error: ${it.code} - ${it.message}")
                    }
                }
            }
        })
    }

    fun likeCapture(captureId: String, callback: (Boolean, Capture?, String?) -> Unit) {
        val url = "$url/capture/$captureId/like"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${AppConfig.token}")
            .put(ByteArray(0).toRequestBody())
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                callback(false, null, e.message)
            }

            override fun onResponse(call: okhttp3.Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        val bodyString = it.body?.string()
                        if (bodyString != null) {
                            try {
                                val capture = json.decodeFromString<Capture>(bodyString)
                                callback(true, capture, null)
                            } catch (e: Exception) {
                                callback(false, null, "Parse error: ${e.message}")
                            }
                        } else {
                            callback(false, null, "Empty response body")
                        }
                    } else {
                        callback(false, null, "Error: ${it.code} - ${it.message}")
                    }
                }
            }
        })
    }

    fun getCaptures(endpoit: String, callback: (Boolean, List<Capture>?, String?) -> Unit) {

        var url = url + endpoit

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
                        if (bodyString != null) {
                            try {
                                val captures = json.decodeFromString<List<Capture>>(bodyString)
                                    .sortedByDescending { ZonedDateTime.parse(it.dateTime) }
                                callback(true, captures, null)
                            } catch (e: Exception) {
                                callback(false, null, "Parse error: ${e.message}")
                            }
                        } else {
                            callback(false, null, "Empty response body")
                        }
                    } else {
                        callback(false, null, "Error: ${it.code} - ${it.message}")
                    }
                }
            }
        })
    }

    fun deleteCapture(captureId: String, callback: (Boolean, String?) -> Unit) {
        val url = "$url/capture/$captureId"

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


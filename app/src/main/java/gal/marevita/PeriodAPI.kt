package gal.marevita

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
data class Period(
    val alert: Alert,
    val startDate: String,
    val endDate: String
)

class PeriodAPI {

    private val client = OkHttpClient()
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    private val baseUrl = "${AppConfig.BASE_URL}/alert"

    fun getPeriods(alertaId: String? = null, callback: (Boolean, List<Period>?, String?) -> Unit) {
        var url = baseUrl

        if (alertaId != null) url += "/$alertaId/check"
        else url += "/check"

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
                            val periods = json.decodeFromString<List<Period>>(bodyString ?: "")
                            callback(true, periods, null)
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
}

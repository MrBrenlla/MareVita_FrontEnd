package gal.marevita

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

private const val baseUrl = AppConfig.BASE_URL

data class GrupedFishList(
    val name: String,
    val times: Int,
    val fishes: List<FishCount>
)

@Serializable
data class CaptureInfo(
    val id: String,
    val dateTime: String
)

@Serializable
data class LocationStat(
    val name: String,
    val times: Int,
    val fishes: List<FishCount>
) {
    fun toGrupedFishList(): GrupedFishList {
        return GrupedFishList(
            name = name,
            times = times,
            fishes = fishes
        )
    }
}

@Serializable
data class FishCount(
    val name: String,
    val number: Int
)

@Serializable
data class BaitStat(
    val name: String,
    val times: Int,
    val fishes: List<FishCount>
) {
    fun toGrupedFishList(): GrupedFishList {
        return GrupedFishList(
            name = name,
            times = times,
            fishes = fishes
        )
    }
}

@Serializable
data class Statistics(
    val totalLocations: Int,
    val totalCaptures: Int,
    val totalFishCount: Int,
    val biggerCapture: CaptureInfo,
    val diverseCapture: CaptureInfo,
    val likedCapture: CaptureInfo,
    val locations: List<LocationStat>,
    val baits: List<BaitStat>,
    val fishes: List<FishCount>
)

class StatisticsAPI {

    private val client = OkHttpClient()
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    /**
     * Recupera as estatísticas dun usuario dado o seu nome
     */
    fun getStatistics(userName: String, callback: (Boolean, Statistics?, String?) -> Unit) {
        val url = "$baseUrl/statistics/$userName"

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
                                val statistics = json.decodeFromString<Statistics>(bodyString)
                                callback(true, statistics, null)
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
}

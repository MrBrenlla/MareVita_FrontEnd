package gal.marevita

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
data class Social(
    val friends: List<String>,
    val friendPetitionsReceived: List<String>,
    val friendPetitionsSent: List<String>
)

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
data class SearchResult(
    val userList: List<String>
)

class SocialAPI {

    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    private val baseUrl = AppConfig.BASE_URL

    private fun buildRequest(url: String, method: String): Request {
        val builder = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${AppConfig.token}")
        return when (method) {
            "GET" -> builder.get().build()
            "PUT" -> builder.put(ByteArray(0).toRequestBody()).build()
            else -> throw IllegalArgumentException("Unsupported HTTP method: $method")
        }
    }

    fun getSocial(callback: (Boolean, Social?, String?) -> Unit) {
        val request = buildRequest("$baseUrl/user/social", "GET")
        client.newCall(request).enqueue(handleResponse(callback))
    }

    fun sendFriendPetition(userName: String, callback: (Boolean, Social?, String?) -> Unit) {
        val request = buildRequest("$baseUrl/user/$userName/add", "PUT")
        client.newCall(request).enqueue(handleResponse(callback))
    }

    fun acceptFriendPetition(userName: String, callback: (Boolean, Social?, String?) -> Unit) {
        val request = buildRequest("$baseUrl/user/$userName/accept", "PUT")
        client.newCall(request).enqueue(handleResponse(callback))
    }

    fun declineFriendPetition(userName: String, callback: (Boolean, Social?, String?) -> Unit) {
        val request = buildRequest("$baseUrl/user/$userName/decline", "PUT")
        client.newCall(request).enqueue(handleResponse(callback))
    }

    fun removeFriend(userName: String, callback: (Boolean, Social?, String?) -> Unit) {
        val request = buildRequest("$baseUrl/user/$userName/remove", "PUT")
        client.newCall(request).enqueue(handleResponse(callback))
    }

    fun searchUser(keyword: String, callback: (Boolean, List<String>?, String?) -> Unit) {
        val request = buildRequest("$baseUrl/user/search/$keyword", "GET")
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false, null, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = it.body?.string()
                    if (it.isSuccessful && body != null) {
                        try {
                            val result = json.decodeFromString<SearchResult>(body)
                            callback(true, result.userList, null)
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

    private fun handleResponse(callback: (Boolean, Social?, String?) -> Unit): Callback {
        return object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false, null, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = it.body?.string()
                    if (it.isSuccessful && body != null) {
                        try {
                            val social = json.decodeFromString<Social>(body)
                            callback(true, social, null)
                        } catch (e: Exception) {
                            callback(false, null, "Parse error: ${e.message}")
                        }
                    } else {
                        callback(false, null, "Error: ${it.code} - ${it.message}")
                    }
                }
            }
        }
    }
}

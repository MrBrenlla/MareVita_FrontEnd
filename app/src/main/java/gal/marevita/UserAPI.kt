package gal.marevita

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException


@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
data class UserData(
    val userName: String,
    val name: String,
    val email: String,
    val friendsCount: Int? = null
)

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
data class PassChange(
    val oldPass: String,
    val newPass: String
)

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
data class Error400(
    val badValue: String
)

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
data class Error409(
    val conflict: String
)

class UserAPI {

    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val mediaType = "application/json".toMediaType()
    private val baseUrl = AppConfig.BASE_URL

    fun updateUser(userData: UserData, callback: (Int?, UserData?, String?) -> Unit) {
        val url = "$baseUrl/user/update"
        val requestBody = json.encodeToString(userData).toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${AppConfig.token}")
            .put(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null,  null, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = it.body?.string()
                    if (it.isSuccessful && body != null) {
                        val updatedUser = json.decodeFromString<UserData>(body)
                        callback(it.code, updatedUser, null)
                    } else {
                        if(it.code == 400){
                            val error = json.decodeFromString<Error400>(body!!)
                            callback(it.code, null, error.badValue)

                        }else if(it.code == 409){
                            val error = json.decodeFromString<Error409>(body!!)
                            callback(it.code, null, error.conflict)
                        }else
                        callback(it.code, null, it.message)
                    }
                }
            }
        })
    }

    fun updatePassword(change: PassChange, callback: (Int?, String?) -> Unit) {
        val url = "$baseUrl/user/update/password"
        val requestBody = json.encodeToString(change).toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${AppConfig.token}")
            .put(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    callback(it.code, it.message)
                }
            }
        })
    }

    fun getUser(userName: String, callback: (Boolean, UserData?, String?) -> Unit) {
        val url = "$baseUrl/user/$userName"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${AppConfig.token}")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false, null, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = it.body?.string()
                    if (it.isSuccessful && body != null) {
                        val user = json.decodeFromString<UserData>(body)
                        callback(true, user, null)
                    } else {
                        callback(false, null, "Error: ${it.code} - ${it.message}")
                    }
                }
            }
        })
    }
}
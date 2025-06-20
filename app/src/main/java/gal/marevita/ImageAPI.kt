package gal.marevita

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.BufferedSink
import okio.source
import java.io.IOException

class ImageAPI(private val context: Context) {

    private val url = AppConfig.BASE_URL
    private val client = OkHttpClient()

    fun sendFile(uri: Uri, endpoint: String, put : Boolean = false, callback: (Boolean,String?) -> Unit) {

        val endpointUrl = url + endpoint

        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
        val fileName = getFileName(uri)

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                fileName,
                object : RequestBody() {
                    override fun contentType(): MediaType? = mimeType.toMediaTypeOrNull()

                    override fun writeTo(sink: BufferedSink) {
                        contentResolver.openInputStream(uri)?.use { inputStream ->
                            sink.writeAll(inputStream.source())
                        } ?: throw IOException("Non se puido abrir o InputStream para o URI.")
                    }
                }
            )
            .build()

        var request: Request

        if (put)
            request = Request.Builder()
                .url(endpointUrl)
                .addHeader("Authorization", "Bearer ${AppConfig.token}")
                .put(requestBody)
                .build()
        else
            request = Request.Builder()
                .url(endpointUrl)
                .addHeader("Authorization", "Bearer ${AppConfig.token}")
                .post(requestBody)
                .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false,null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        callback(false,null)
                    } else {
                        val responseBody = it.body?.string()
                        callback(true,responseBody)
                    }
                }
            }
        })
    }

    fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex("_display_name")
                    if (index != -1) {
                        result = it.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: "file"
    }

    fun getImage(endpoint: String, callback: (Boolean, Bitmap?, String?) -> Unit) {

        val url = url + endpoint

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
                    if (it.isSuccessful) {
                        val bytes = it.body?.bytes()
                        if (bytes != null) {
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                            val resizedBitmap = resizeBitmap(bitmap, 800, 800)

                            callback(true, resizedBitmap, null)

                        } else {
                            callback(false, null, "Empty image response")
                        }
                    } else {
                        callback(false, null, "Error: ${it.code} - ${it.message}")
                    }
                }
            }
        })
    }

    fun resizeBitmap(source: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = source.width
        val height = source.height

        val aspectRatio = width.toFloat() / height.toFloat()

        val newWidth: Int
        val newHeight: Int

        if (width > height) {
            newWidth = maxWidth
            newHeight = (maxWidth / aspectRatio).toInt()
        } else {
            newHeight = maxHeight
            newWidth = (maxHeight * aspectRatio).toInt()
        }

        return Bitmap.createScaledBitmap(source, newWidth, newHeight, true)
    }
}
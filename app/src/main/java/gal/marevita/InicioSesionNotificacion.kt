package gal.marevita

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import gal.marevita.databinding.CargaInicioSesionBinding
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class InicioSesionNotificacion : AppCompatActivity() {

    private lateinit var binding: CargaInicioSesionBinding
    val url = AppConfig.BASE_URL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CargaInicioSesionBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onStart() {
        super.onStart()
        var (user, pass) = PreferencesManager.getInstance(this).getLastSession()

        if (user != null && pass != null) signInWithEmailPassword(user, pass)
        else {
            val intent = Intent(this@InicioSesionNotificacion, InicioActivity::class.java)
            intent.putExtra("target", "alertas")
            startActivity(intent)
            finish()
            Toast.makeText(
                this@InicioSesionNotificacion,
                "Error co inicio de sesión.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun signInWithEmailPassword(
        userNameOrEmail: String,
        password: String
    ) {
        val client = OkHttpClient()
        val endpoint = "$url/user/login"
        val jsonBody = JSONObject().apply {
            put("userNameOrEmail", userNameOrEmail)
            put("password", password)
        }

        val requestBody = jsonBody.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(endpoint)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!isFinishing && !isDestroyed)
                    runOnUiThread {
                        Log.e("LOGIN ", "Erro na solicitude ", e)
                        Toast.makeText(
                            baseContext,
                            "Erro de conexión: ${e.message}.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBodyString = response.body?.string() ?: ""
                    val statusCode = response.code

                    runOnUiThread {
                        if (isFinishing || isDestroyed) return@runOnUiThread

                        if (!response.isSuccessful || statusCode !in 200..299) {
                            redirectToLogin()
                            return@runOnUiThread
                        }

                        try {
                            val respostaJson = JSONObject(responseBodyString)
                            val token = respostaJson.optString("token", null)

                            if (token != null) {
                                AppConfig.token = token
                                val intent = Intent(
                                    this@InicioSesionNotificacion,
                                    MenuActivity::class.java
                                )
                                intent.putExtra("target", "alertas")
                                startActivity(intent)
                                finish()
                                Toast.makeText(
                                    this@InicioSesionNotificacion,
                                    "Sesión iniciada.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                redirectToLogin()
                            }
                        } catch (e: Exception) {
                            Log.e("LOGIN", "Erro ao parsear a resposta", e)
                            redirectToLogin()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("LOGIN", "Erro ao ler a resposta", e)
                    if (!isFinishing && !isDestroyed) {
                        runOnUiThread {
                            Toast.makeText(
                                baseContext,
                                "Erro de procesamento: ${e.message}.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } finally {
                    response.close()
                }
            }
        })
    }

    private fun redirectToLogin() {
        val intent = Intent(this@InicioSesionNotificacion, InicioActivity::class.java)
        intent.putExtra("target", "alertas")
        startActivity(intent)
        finish()
        Toast.makeText(
            this@InicioSesionNotificacion,
            "Error co inicio de sesión.",
            Toast.LENGTH_SHORT
        ).show()
    }
}
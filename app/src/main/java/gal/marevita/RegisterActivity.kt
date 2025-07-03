package gal.marevita

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import gal.marevita.databinding.RegisterBinding
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class RegisterActivity : AppCompatActivity() {


    private lateinit var binding: RegisterBinding

    val url = AppConfig.BASE_URL


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = RegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.register.setOnClickListener {
            register()
        }

    }

    override fun onStart() {
        super.onStart()
    }

    fun register() {

        val client = OkHttpClient()

        val endpoint = "$url/user/register"

        val jsonBody = JSONObject().apply {
            put("userName", binding.usernameText.text)
            put("name", binding.nameText.text)
            put("email", binding.emailText.text)
            put("password", binding.passText.text)
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
                            "Erro de conexión: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    try {

                        val respostaJson = JSONObject(it.body?.string() ?: "")

                        if (!isFinishing && !isDestroyed)
                            runOnUiThread {
                                binding.error.text = null
                                binding.usernameLayout.error = null
                                binding.emailLayout.error = null
                                binding.passLayout.error = null

                                if (!it.isSuccessful) {
                                    if (it.code == 400) {

                                        val error = respostaJson.optString("badValue", null)

                                        if (error == "Email")
                                            binding.emailLayout.error =
                                                "O email non ten un formato correcto."
                                        else if (error == "UserName")
                                            binding.usernameLayout.error =
                                                "O nome de usuario non pode conter @."
                                        else if (error == "Password")
                                            binding.passLayout.error =
                                                "A contrasinal debe de ter 8 caracteres, incluindo 1 maiuscula, 1 minúscula e 1 número."
                                        else
                                            binding.error.text = "Algún valor incorrecto."

                                    } else if (it.code == 409) {

                                        val error = respostaJson.optString("conflict", null)

                                        if (error == "Email")
                                            binding.emailLayout.error = "O email xe está en uso."
                                        else if (error == "UserName")
                                            binding.usernameLayout.error =
                                                "O nome de usuario xe está en uso."
                                        else
                                            binding.error.text = "Algún valor en conflicto."
                                    }
                                    return@runOnUiThread
                                }


                                PreferencesManager.getInstance(this@RegisterActivity)
                                    .saveLastSession(
                                        binding.usernameText.text.toString(),
                                        binding.passText.text.toString()
                                    )

                                finish()
                                Toast.makeText(
                                    this@RegisterActivity,
                                    "Usuario creado",
                                    Toast.LENGTH_SHORT
                                ).show()

                            }
                    } catch (e: Exception) {
                        Log.e("REGISTER ", "Erro na resposta ", e)
                    }
                }
            }
        })
    }

}
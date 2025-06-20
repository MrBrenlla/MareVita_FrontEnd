package gal.marevita


import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import gal.marevita.databinding.InicioBinding
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class InicioActivity : AppCompatActivity() {

    private lateinit var binding: InicioBinding

    val url = AppConfig.BASE_URL


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = InicioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.logIn.setOnClickListener {
            val email = binding.userText.text.toString()
            val password = binding.passText.text.toString()
            signInWithEmailPassword(email, password)
        }

        binding.register.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }


    }

    override fun onStart() {
        super.onStart()
        var (user,pass) = PreferencesManager.getInstance(this).getLastSession()

        if(user!=null) {
            binding.userText.setText(user)
            if( pass!=null) signInWithEmailPassword(user,pass,true)
        }
        binding.error.text = ""
    }


    private fun signInWithEmailPassword(
        userNameOrEmail: String,
        password: String,
        autoSignIn : Boolean = false
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
                if (!isFinishing && !isDestroyed && !autoSignIn)
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
                response.use {
                    try {
                        if (!it.isSuccessful) {
                            if (!isFinishing && !isDestroyed && !autoSignIn)
                                runOnUiThread {
                                    binding.error.text = "Usuario ou contrasinal incorrecta."
                                }
                            return
                        }

                        val respostaJson = JSONObject(it.body?.string() ?: "")
                        val token = respostaJson.optString("token", null)

                        if (token != null) {
                            PreferencesManager.getInstance(this@InicioActivity)
                                .saveLastSession(userNameOrEmail, password)
                            AppConfig.token = token

                            if (!isFinishing && !isDestroyed)
                                runOnUiThread {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
                                    }

                                    val workRequest = PeriodicWorkRequestBuilder<NotificadorWorker>(2, TimeUnit.DAYS)
                                        .build()

                                    WorkManager.getInstance(this@InicioActivity).enqueueUniquePeriodicWork(
                                        "check_every_two_days",
                                        ExistingPeriodicWorkPolicy.REPLACE,
                                        workRequest
                                    )

                                    val target = intent.getStringExtra("target")
                                    val intent =
                                        Intent(this@InicioActivity, MenuActivity::class.java)
                                    if(target!=null) intent.putExtra("target",target)

                                    startActivity(intent)
                                    finish()
                                    Toast.makeText(
                                        this@InicioActivity,
                                        "Inicio de sesión correcto.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                        } else {
                            if (!isFinishing && !isDestroyed && !autoSignIn)
                                runOnUiThread {
                                    Toast.makeText(
                                        baseContext,
                                        "Token non devolto.",
                                        Toast.LENGTH_LONG
                                    )
                                        .show()
                                }
                        }
                    } catch (e: Exception) {
                        Log.e("LOGIN ", "Erro na resposta ", e)
                    }
                }
            }
        })
    }
}

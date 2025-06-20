package gal.marevita

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class NotificadorWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {

        signInAndCheckAlerts()

        return Result.success()
    }

    private fun signInAndCheckAlerts() {

        val preferencesManager = PreferencesManager.getInstance(applicationContext)

        val (userNameOrEmail,password)= preferencesManager.getLastSession()
        val client = OkHttpClient()

        val endpoint = "${AppConfig.BASE_URL}/user/login"

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
            override fun onFailure(call: Call, e: IOException) {}

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    try {
                        if (!it.isSuccessful) return

                        val respostaJson = JSONObject(it.body?.string() ?: "")
                        val token = respostaJson.optString("token", null)

                        if (token != null) {
                            AppConfig.token = token
                            checkAlerts()
                        }
                    } catch (e: Exception) {
                        Log.e("LOGIN ", "Erro na resposta ", e)
                    }
                }
            }
        })
    }

    private fun checkAlerts(){
        PeriodAPI().getPeriods{ success, periods, error ->
            if (success) {
                if(periods!=null && periods.isNotEmpty()){
                    var actives = mutableListOf<String>()
                    periods.forEach {
                        if(!actives.contains(it.alert.id)) actives.add(it.alert.name)
                    }
                    if(actives.isNotEmpty()) {
                        var msg = ""
                        if(actives.size==1) msg = "Hai 1 alerta activa nos próximos 7 días."
                        else msg = "Hai ${actives.size} alertas activas nos próximos 7 días."

                        val notificationManager =
                            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                        val channelId = "MareVita_chanel"
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val channel = NotificationChannel(
                                channelId,
                                "Notificacións MareVita",
                                NotificationManager.IMPORTANCE_DEFAULT
                            )
                            notificationManager.createNotificationChannel(channel)
                        }

                        val intent = Intent(applicationContext, InicioSesionNotificacion::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }

                        val pendingIntent = PendingIntent.getActivity(
                            applicationContext,
                            0,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                        val notification = NotificationCompat.Builder(applicationContext, channelId)
                            .setContentTitle("A cana estache esperando!")
                            .setContentText(msg)
                            .setSmallIcon(R.drawable.logo)
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true)
                            .build()

                        notificationManager.notify(1, notification)

                    }
                }
            } else {
                println("error: $error")
            }
        }
    }

}
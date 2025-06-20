package gal.marevita

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.net.Uri
import android.view.LayoutInflater
import android.widget.Toast
import gal.marevita.ConditionsTranslator.getMesurementUnit
import gal.marevita.ConditionsTranslator.translateGalego
import gal.marevita.databinding.PopUpAlertaBinding
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class AlertaPopUp(private val context: Context, private val callback: () -> Unit) {

    private lateinit var binding: PopUpAlertaBinding



    fun fromId(alertaId: String) {
        AlertAPI().getAlert(alertaId) { success, alert, message ->
            if (context is Activity && !context.isFinishing && !context.isDestroyed)
                context.runOnUiThread {
                    if (!success) {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    } else if (alert != null) {
                        show(alert)
                    }
                }

        }
    }

    fun show(alert: Alert) {
        binding = PopUpAlertaBinding.inflate(LayoutInflater.from(context))

        var loc = alert.location
        if (alert.gpsLocation != null) {
            val latitude = alert.gpsLocation.latitude
            val longitude = alert.gpsLocation.longitude

            if (loc == null) {
                loc = "$latitude, $longitude"
            } else {
                loc += " ($latitude, $longitude)"
            }

            binding.tvAlertLocation.apply {
                setTextColor(context.getColor(R.color.dark_blue))
                paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
            }

            binding.tvAlertLocation.setOnClickListener {
                val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.setPackage("com.google.android.apps.maps")

                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                } else {
                    Toast.makeText(context, "Google Maps non está instalado", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
        binding.tvAlertLocation.text = loc ?: "N/A"

        binding.tvFish.text = alert.fishStringList().ifEmpty { "N/A" }

        binding.tvBaits.text = alert.baitStringList().ifEmpty { "N/A" }

        if (alert.weatherConditions.isEmpty()) {
            binding.tvAlertWeatherConditions.text = "N/A"
        } else {
            val weatherStr = alert.weatherConditions.joinToString("\n") { wc ->
                "${translateGalego(wc.name)}: ${wc.value} ±${wc.error} ${getMesurementUnit(wc.name)}"
            }
            binding.tvAlertWeatherConditions.text = weatherStr
        }

        if (alert.activated.isEmpty()) {
            binding.tvPredictions.text = "Nada na próxima semana"
        } else {
            val periodsSTR = alert.activated.joinToString("\n") { p ->
                var start = ZonedDateTime.parse(p.startDate)
                    .withZoneSameInstant(ZoneId.systemDefault())
                var end = ZonedDateTime.parse(p.endDate)
                    .withZoneSameInstant(ZoneId.systemDefault())
                val formatter =
                    DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm", Locale("gl"))
                "${start.format(formatter)} - ${end.format(formatter)}"
            }
            binding.tvPredictions.text = periodsSTR
        }


    val dialog = AlertDialog.Builder(context)
            .setTitle(alert.name)
            .setView(binding.root)
            .setPositiveButton("Pechar", null)
            .create()

        if (alert.relatedCapture != null) {
            binding.relatedCapture.apply {
                setTextColor(context.getColor(R.color.dark_blue))
                paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
            }

            binding.relatedCapture.setOnClickListener {
                CapturaPopUp(context).fromId(alert.relatedCapture)
            }
        }

        binding.removeAlertButton.setOnClickListener {
            AlertAPI().deleteAlert(alert.id!!) { success, message ->
                if (context is Activity && !context.isFinishing && !context.isDestroyed)
                    context.runOnUiThread {
                        if (success) {
                            Toast.makeText(context, "Alerta eliminada", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            callback()
                        } else {
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    }

            }
        }

        binding.editAlertButton.setOnClickListener {
            val intent = Intent(context, NovaAlertaActivity::class.java)
            intent.putExtra("alertId", alert.id)
            context.startActivity(intent)
            dialog.dismiss()
            callback()
        }

        dialog.show()
    }
}

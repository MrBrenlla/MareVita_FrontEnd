package gal.marevita

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import gal.marevita.ConditionsTranslator.getMesurementUnit
import gal.marevita.ConditionsTranslator.translateGalego
import gal.marevita.databinding.PopUpCapturaBinding
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class CapturaPopUp(private val context: Context, private val callback: (Capture?) -> Unit = {}) {

    private lateinit var binding: PopUpCapturaBinding

    fun fromId(capturaId: String, buttons: Int = 0) {
        CaptureAPI().getCapture(capturaId) { bool, capture, message ->
            if (context is Activity && !context.isFinishing && !context.isDestroyed)
                context.runOnUiThread {
                    if (!bool) {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT)
                    } else {
                        if (capture != null) show(capture, buttons)
                    }
                }
        }
    }

    fun show(capture: Capture, buttons: Int = 0) {
        val binding = PopUpCapturaBinding.inflate(android.view.LayoutInflater.from(context))

        when (capture.security) {
            null -> binding.tvSecurity.text = "N/A"
            0 -> binding.tvSecurity.text = "Pública"
            1 -> binding.tvSecurity.text = "Só amigos"
            2 -> binding.tvSecurity.text = "Privada"
            else -> binding.tvSecurity.text = "N/A"
        }

        binding.tvOwner.text = capture.owner ?: "N/A"
        binding.tvlikes.text = capture.likes.size.toString()

        if (capture.owner != null) {
            binding.tvOwner.apply {
                setTextColor(context.getColor(R.color.dark_blue))
                paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
            }
            binding.tvOwner.setOnClickListener {
                val intent = Intent(context, PerfilActivity::class.java)
                intent.putExtra("userName", capture.owner)
                startActivity(context, intent, null)
            }
        }


        binding.tvImageCaption.text = capture.imageCaption ?: "N/A"

        var loc = capture.location
        if (capture.gpsLocation != null) {
            if (loc == null) loc =
                capture.gpsLocation.latitude.toString() + "," + capture.gpsLocation.longitude.toString()
            else loc += " (" + capture.gpsLocation.latitude.toString() + "," + capture.gpsLocation.longitude.toString() + ")"

            val latitude = capture.gpsLocation.latitude
            val longitude = capture.gpsLocation.longitude

            binding.tvLocation.apply {
                setTextColor(context.getColor(R.color.dark_blue))
                paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
            }

            binding.tvLocation.setOnClickListener {
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

        binding.tvLocation.text = loc ?: "N/A"


        if (capture.dateTime == null) binding.tvDateTime.text = "N/A"
        else {
            var dateTime = ZonedDateTime.parse(capture.dateTime)
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm (z)", Locale("gl"))
            binding.tvDateTime.text = dateTime.format(formatter)
        }
        binding.tvFish.text = capture.fishNumberedStringList().ifEmpty { "N/A" }

        binding.tvBaits.text = capture.baitStringList().ifEmpty { "N/A" }

        if (capture.weatherConditions.isEmpty()) {
            binding.tvWeatherConditions.text = "N/A"
        } else {
            val weatherStr = capture.weatherConditions.joinToString("\n") { wc ->
                "${translateGalego(wc.name)}: ${wc.value} ${getMesurementUnit(wc.name)}".trim()
            }
            binding.tvWeatherConditions.text = weatherStr
        }

        val imageApi = ImageAPI(context)

        capture.images.forEach { imageId ->
            imageApi.getImage("/images/" + capture.owner + "/" + imageId) { b, image, message ->
                if (context is Activity && !context.isFinishing && !context.isDestroyed)
                    context.runOnUiThread {
                        if (b) {

                            val marginInDp = 5
                            val scale = context.resources.displayMetrics.density
                            val marginInPx = (marginInDp * scale + 0.5f).toInt()

                            val layoutParams = ViewGroup.MarginLayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                            layoutParams.setMargins(
                                marginInPx,
                                marginInPx,
                                marginInPx,
                                marginInPx
                            )

                            val imageView = ImageView(context).apply {
                                this.layoutParams = layoutParams
                                scaleType = ImageView.ScaleType.FIT_CENTER
                                adjustViewBounds = true
                            }
                            imageView.setImageBitmap(image)

                            binding.tvImages.addView(imageView)
                        }
                    }
            }

        }

        var dialogTemp = AlertDialog.Builder(context)
            .setTitle("Información da captura")
            .setView(binding.root)

        dialogTemp = if (buttons == 2) dialogTemp.setNegativeButton("Pechar", null)
            .setPositiveButton("Desligar", null)
        else if (buttons == 1) dialogTemp.setNegativeButton("Cancelar", null)
            .setPositiveButton("Seleccionar", null)
        else dialogTemp.setPositiveButton("Pechar", null)

        val dialog = dialogTemp.create()



        dialog.setOnShowListener {
            var button = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            button.setOnClickListener {
                callback(null)
                dialog.dismiss()
            }

            button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                callback(capture)
                dialog.dismiss()
            }
        }


        if (buttons == 0) {

            binding.removeButton.setOnClickListener {
                CaptureAPI().deleteCapture(capture.id!!) { bool, message ->
                    if (context is Activity && !context.isFinishing && !context.isDestroyed)
                        context.runOnUiThread {
                            if (bool) {
                                Toast.makeText(context, "Captura eliminada", Toast.LENGTH_SHORT)
                                    .show()
                                callback(null)
                                dialog.dismiss()
                            } else {
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            }

        } else binding.removeButton.visibility = View.GONE

        if (buttons == 0 || buttons == 4) {
            binding.newAlertButton.setOnClickListener {
                val intent = Intent(context, NovaAlertaActivity::class.java)
                intent.putExtra("capture", capture.id)
                context.startActivity(intent)
            }
        } else binding.newAlertButton.visibility = View.GONE



        dialog.show()
    }
}
package gal.marevita.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.widget.Toast
import gal.marevita.CapturaPopUp
import gal.marevita.Capture
import gal.marevita.CaptureAPI
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class CaptureSelectorPopUp(private val context: Context) {

    private val api = CaptureAPI()

    fun showCapturePopup(callback: (Capture) -> Unit) {
        api.getCaptures("/capture") { success, captures, error ->
            if (success && captures != null) {
                if (context is Activity && !context.isFinishing && !context.isDestroyed)
                    context.runOnUiThread {
                        if (captures.isEmpty()) {
                            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                        } else {
                            showPopup(captures, callback)
                        }
                    }
            }

        }
    }

    private fun showPopup(captures: List<Capture>, callback: (Capture) -> Unit) {

        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("gl"))

        val items = captures.map { capture ->
            val date = try {
                val dateTime = ZonedDateTime.parse(capture.dateTime)
                dateTime.format(formatter)
            } catch (e: Exception) {
                "data descoñecida"
            }
            "${capture.location ?: "Localización descoñecida"} ($date)"
        }.toTypedArray()

        var dialog: AlertDialog? = null

        val builder = AlertDialog.Builder(context)
            .setTitle("Selecciona unha captura")
            .setItems(items) { _, which ->
                val selected = captures[which]
                if (selected.id != null)
                    CapturaPopUp(context) { c ->
                        if (c != null) {
                            callback(c)
                            dialog?.dismiss()
                        } else {
                            showPopup(captures, callback)
                        }
                    }.fromId(selected.id, 1)
            }
            .setNegativeButton("Cancelar", null)

        dialog = builder.create()
        dialog.show()
    }
}

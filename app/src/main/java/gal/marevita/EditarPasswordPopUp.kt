package gal.marevita

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import gal.marevita.NovaCapturaActivity


class EditarPasswordPopUp(
    val context: Context,
) {



    fun setDialogoEditarPassword() {
        val vista = LayoutInflater.from(context).inflate(R.layout.editar_password, null)

        val oldPassView = vista.findViewById<TextInputEditText>(R.id.oldPassText)
        val newPassView = vista.findViewById<TextInputEditText>(R.id.newPassText)
        val oldPassLayout = vista.findViewById<TextInputLayout>(R.id.oldPassLayout)
        val newPassLayout = vista.findViewById<TextInputLayout>(R.id.newPassLayout)

        val dialog = AlertDialog.Builder(context)
            .setTitle("Editar Usuario")
            .setView(vista)
            .setPositiveButton("Gardar", null)
            .setNegativeButton("Cancelar", null)
            .create()


        dialog.setOnShowListener {
            var button = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            button.setOnClickListener {
                dialog.dismiss()
            }

            button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {

                val oldPass = oldPassView.text.toString()
                val newPass = newPassView.text.toString()
                val passChange = PassChange(oldPass, newPass)

                UserAPI().updatePassword(passChange) { code, message ->
                    if (context is Activity && !context.isFinishing && !context.isDestroyed)
                        context.runOnUiThread {
                            oldPassLayout.error = null
                            newPassLayout.error = null
                            if (code == 200) {
                                Toast.makeText(
                                    context,
                                    "Contrsinal actualizada correctamente",
                                    Toast.LENGTH_SHORT
                                ).show()
                                val preference = PreferencesManager.getInstance(context)
                                val user = preference.getUsername()
                                preference.saveLastSession(user, newPass)
                                dialog.dismiss()
                            } else if (code == 400) {
                                newPassLayout.error = "A nova contrasinal debe de ter 8 caracteres, incluindo 1 maiuscula, 1 minúscula e 1 número."
                            } else if (code == 401) {
                                oldPassLayout.error = "Contrasinal incorrecta."
                            } else
                                Toast.makeText(
                                    context,
                                    "Erro ao actualizar a contrasinal",
                                    Toast.LENGTH_SHORT
                                ).show()
                        }
                }
            }
        }

        dialog.show()
    }

}
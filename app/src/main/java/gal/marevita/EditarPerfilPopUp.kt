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


class EditarPerfilPopUp(
    val context: Context,
    val profilePic: Bitmap?,
    val imagePicker: ImagePicker
) {

    var uriPic: Uri? = null

    private lateinit var usuario: UserData

    fun fromUserName(userName: String, callback: (UserData?, Uri?) -> Unit) {
        UserAPI().getUser(userName) { bool, user, message ->
            if (context is Activity && !context.isFinishing && !context.isDestroyed)
                context.runOnUiThread {
                    if (!bool) {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT)
                    } else {
                        if (user != null) {
                            usuario = user
                            setDialogoEditarUsuario(callback)
                        }
                    }
                }
        }
    }


    private fun setDialogoEditarUsuario(callback: (UserData?, Uri?) -> Unit) {
        val vista = LayoutInflater.from(context).inflate(R.layout.editar_perfil, null)

        val useNameView = vista.findViewById<TextInputEditText>(R.id.usernameText)
        val nameView = vista.findViewById<TextInputEditText>(R.id.nameText)
        val emailView = vista.findViewById<TextInputEditText>(R.id.emailText)
        val userNameLayaout = vista.findViewById<TextInputLayout>(R.id.usernameLayout)
        val nameLayout = vista.findViewById<TextInputLayout>(R.id.nameLayout)
        val emailLayout = vista.findViewById<TextInputLayout>(R.id.emailLayout)

        val imagePerfil = vista.findViewById<ImageView>(R.id.imagePerfil)
        val btnCambiarFoto = vista.findViewById<Button>(R.id.btnCambiarFoto)
        val updating = vista.findViewById<ProgressBar>(R.id.updating)

        setProfilePic(imagePerfil, btnCambiarFoto)

        useNameView.setText(usuario.userName)
        nameView.setText(usuario.name)
        emailView.setText(usuario.email)

        btnCambiarFoto.setOnClickListener {
            imagePicker.startImagePicker { uri ->
                imagePerfil.visibility = View.GONE
                updating.visibility = View.VISIBLE
                ImageAPI(context).sendFile(uri, "/profile/pic", true) { succes, _ ->
                    if (context is Activity && !context.isFinishing && !context.isDestroyed)
                        context.runOnUiThread {
                            if (succes) {
                                uriPic = uri
                                imagePerfil.setImageURI(uri)
                                btnCambiarFoto.text = "Cambiar foto de perfil"
                            } else {
                                Toast.makeText(
                                    context,
                                    "Erro ao actualizar a foto de perfil",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            imagePerfil.visibility = View.VISIBLE
                            updating.visibility = View.GONE
                        }
                }
            }

        }

        val dialog = AlertDialog.Builder(context)
            .setTitle("Editar Usuario")
            .setView(vista)
            .setPositiveButton("Gardar", null)
            .setNegativeButton("Cancelar", null)
            .create()


        dialog.setOnShowListener {
            var button = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            button.setOnClickListener {
                callback(null,uriPic)
                dialog.dismiss()
            }
        }


        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {

                val userName = useNameView.text.toString()
                val name = nameView.text.toString()
                val email = emailView.text.toString()
                usuario = usuario.copy(userName = userName, name = name, email = email)

                UserAPI().updateUser(usuario){code, user, message ->
                    if (context is Activity && !context.isFinishing && !context.isDestroyed)
                        context.runOnUiThread {
                            userNameLayaout.error = null
                            emailLayout.error = null
                            if (code == 200 && user != null) {
                                Toast.makeText(
                                    context,
                                    "Perfil actualizado correctamente",
                                    Toast.LENGTH_SHORT
                                ).show()
                                val preference = PreferencesManager.getInstance(context)
                                val (_, pass) = preference.getLastSession()
                                preference.saveLastSession(usuario.userName, pass?: "")
                                callback(user, uriPic)
                                dialog.dismiss()
                            }
                            else if(code == 400){
                                if(message == "email") emailLayout.error = "Email inválido"
                                else if(message == "userName") userNameLayaout.error = "Nome de usuario inválido, non pode conter @"
                                else Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                            else if(code == 409){
                                if(message == "userName") emailLayout.error = "Nome de usuario en uso"
                                else if(message == "email") userNameLayaout.error = "Email asociado a outro usuario"
                                else Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }else
                                Toast.makeText(context, "Erro ao actualizar o perfil", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }

        dialog.show()


    }

    private fun setProfilePic(view: ImageView, btn : Button) {
        if (profilePic != null) {
            view.visibility = View.VISIBLE
            view.setImageBitmap(profilePic)
            btn.text = "Cambiar foto de perfil"
        } else {
            ImageAPI(context).getImage("/profile/pic/${usuario.userName}") { success, pic, message ->
                if (context is Activity && !context.isFinishing && !context.isDestroyed)
                    context.runOnUiThread {
                        if (success && pic != null) {
                            view.visibility = View.VISIBLE
                            view.setImageBitmap(pic)
                            btn.text = "Cambiar foto de perfil"
                        }
                    }
            }
        }
    }

}
package gal.marevita

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat

class SearchPopUp(val context: Context) {

    val images = mutableMapOf<String, Bitmap>()
    private var notShow = mutableListOf<String>(PreferencesManager.getInstance(context).getUsername())
    private lateinit var dialog: AlertDialog

    fun mostrarBuscadorUsuarios() {

        val view = LayoutInflater.from(context).inflate(R.layout.buscador, null)
        val inputSearch = view.findViewById<EditText>(R.id.inputSearch)
        val buttonSearch = view.findViewById<ImageButton>(R.id.buttonSearch)
        val userListLayout = view.findViewById<LinearLayout>(R.id.userList)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        dialog = AlertDialog.Builder(context)
            .setTitle("Buscar usuario")
            .setView(view)
            .setNegativeButton("Pechar") { dialog, _ -> dialog.dismiss() }
            .create()

        dialog.show()

        buttonSearch.setOnClickListener {
            val keyword = inputSearch.text.toString().trim()
            if (keyword.isEmpty()) {
                Toast.makeText(context, "Introduce un nome", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            userListLayout.removeAllViews()
            userListLayout.visibility = View.GONE

            SocialAPI().searchUser(keyword) { success, users, error ->
                if (!context.isValidContext()) return@searchUser

                (context as Activity).runOnUiThread {
                    progressBar.visibility = View.GONE
                    userListLayout.visibility = View.VISIBLE
                    if (success && users != null) {
                        val filtered = users.filterNot { notShow.contains(it) }
                        if (filtered.isEmpty()) {
                            val emptyView = TextView(context).apply {
                                text = "Non se atoparon usuarios"
                                textSize = 15f
                                setTextColor(ContextCompat.getColor(context, R.color.shadow_grey))
                                setPadding(16, 32, 16, 32)
                                gravity =
                                    Gravity.CENTER_HORIZONTAL
                                textAlignment =
                                    View.TEXT_ALIGNMENT_CENTER
                            }
                            userListLayout.addView(emptyView)
                        } else {
                            filtered.forEach { addUserView(userListLayout, it) }
                        }
                    } else {
                        Toast.makeText(context, error ?: "Erro na busca", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        progressBar.visibility = View.VISIBLE
        SocialAPI().getSocial { b, social, message ->
            if (!context.isValidContext()) return@getSocial

            (context as Activity).runOnUiThread {
                progressBar.visibility = View.GONE

                if (!b || social == null) {
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }

                notShow.apply {
                    addAll(social.friends)
                    addAll(social.friendPetitionsReceived)
                    addAll(social.friendPetitionsSent)
                }
            }
        }
    }

    private fun addUserView(container: LinearLayout, username: String) {
        val inflater = LayoutInflater.from(context)
        val vista = inflater.inflate(R.layout.lista_usuarios, container, false)

        vista.findViewById<TextView>(R.id.userName).text = username

        val imageView = vista.findViewById<ImageView>(R.id.profilePic)
        if (images.containsKey(username)) {
            imageView.setImageBitmap(images[username])
        } else {
            ImageAPI(context).getImage("/profile/pic/$username") { success, image, _ ->
                if (success && image != null) {
                    (context as Activity).runOnUiThread {
                        images[username] = image
                        imageView.setImageBitmap(image)
                    }
                }
            }
        }

        vista.findViewById<ImageView>(R.id.action2).visibility = View.GONE
        val addButton = vista.findViewById<ImageView>(R.id.action1)
        addButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.user_add))

        addButton.setOnClickListener {
            SocialAPI().sendFriendPetition(username) { success, _, message ->
                (context as Activity).runOnUiThread {
                    if (success) {
                        notShow.add(username)
                        container.removeView(vista)
                        Toast.makeText(context, "Solicitude enviada", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        container.addView(vista)
    }
}

fun Context.isValidContext(): Boolean {
    return when (this) {
        is Activity -> !isFinishing && !isDestroyed
        else -> true
    }
}
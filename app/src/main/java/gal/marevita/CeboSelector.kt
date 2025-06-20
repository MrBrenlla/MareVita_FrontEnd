package gal.marevita

import android.app.AlertDialog
import android.content.Context
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

object CeboSelector {

    private val url = AppConfig.BASE_URL

    interface Callback {
        fun onCebosSeleccionados(seleccionados: List<String>)
    }

    fun mostrar(
        context: Context,
        cebos: List<String>,
        seleccionados: List<String>,
        callback: Callback
    ) {
        val copia = ArrayList(seleccionados)

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 30, 40, 10)
        }

        val gridLayout = GridLayout(context).apply {
            columnCount = 2
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val checkBoxes = mutableListOf<CheckBox>()
        cebos.forEach { cebo ->
            val checkBox = CheckBox(context).apply {
                text = cebo
                isChecked = copia.contains(cebo)

                setSingleLine(false)
                maxLines = 3
                ellipsize = null
            }

            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setGravity(Gravity.FILL_HORIZONTAL)
            }
            checkBox.layoutParams = params

            if (checkBox.isChecked) copia.remove(cebo)
            checkBoxes.add(checkBox)
            gridLayout.addView(checkBox)
        }

        // Se o número de elementos é impar, engade unha vista baleira para manter o deseño simétrico
        if (cebos.size % 2 != 0) {
            gridLayout.addView(View(context))
        }

        val input = EditText(context).apply {
            hint = "Outros, separados por comas"
            inputType = InputType.TYPE_CLASS_TEXT
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setText(stringOfList(copia))
        }

        val innerLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            addView(gridLayout)
            addView(input)
        }

        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                600
            )
            addView(innerLayout)
        }

        layout.addView(scrollView)

        AlertDialog.Builder(context)
            .setTitle("Selecciona os cebos")
            .setView(layout)
            .setPositiveButton("Aceptar") { dialog, _ ->
                val escollidos = checkBoxes.filter { it.isChecked }.map { it.text.toString() }
                val adicionais = input.text.toString()
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                callback.onCebosSeleccionados((escollidos + adicionais).distinct())
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }


    fun getBaits(callback: (List<String>) -> Unit) {
        val client = OkHttpClient()
        val endpoint = "$url/capture/bait"

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer ${AppConfig.token}")
            .get()
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(emptyList())
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    try {
                        if (!it.isSuccessful) {
                            callback(emptyList())
                            return
                        }

                        val respostaJson = JSONObject(it.body?.string() ?: "")
                        val baits = respostaJson.optJSONArray("list")
                        val list = mutableListOf<String>()

                        for (i in 0 until (baits?.length() ?: 0)) {
                            val item = baits?.optString(i)
                            if (!item.isNullOrEmpty()) {
                                list.add(item)
                            }
                        }

                        callback(list.sorted())
                    } catch (e: Exception) {
                        Log.e("CEBOSELECTOR", "Erro na resposta", e)
                        callback(emptyList())
                    }
                }
            }
        })
    }

    fun stringOfList(list: List<String>): String {
        if (list.isEmpty()) return ""
        var aux = list[0]
        for (i in 1 until list.size) aux += ", " + list[i]
        return aux
    }
}

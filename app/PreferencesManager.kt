package com.example.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.MutableLiveData
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager private constructor(private val context: Context) {

    private object PreferencesKeys {
        val textSize = stringPreferencesKey("textSize")
        val previousTextSize = stringPreferencesKey("previousTextSize")
        val darkMode = stringPreferencesKey("darkMode")
    }

    val textSizePreferenceLiveData = MutableLiveData<String>()
    /*
    * Se inicializa el LiveData en "medium" la primera vez que se inicia la app.
    * Es necesario para que notifique a los observadores cuando se inicializan.
    * De esta forma el observador notifica en cuanto se suscribe y por tanto se inicializa el texto.
    * El valor de LiveData solo se emplea como disparador para el observador.
    * En ningún caso se emplea para obtener el tamaño del texto.
    */
    init {
        textSizePreferenceLiveData.value = "medium"
    }

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("MyApp", Context.MODE_PRIVATE)

    fun saveLastSession(userId: String, authToken: String) {
        sharedPreferences.edit().apply {
            putString("USER_ID", userId)
            putString("AUTH_TOKEN", authToken)
            apply()
        }
    }

    fun getLastSession(): Pair<String?, String?> {
        val userId = sharedPreferences.getString("USER_ID", null)
        val authToken = sharedPreferences.getString("AUTH_TOKEN", null)
        return Pair(userId, authToken)
    }

    suspend fun getTextSizePreference(): String {
        return context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.textSize] ?: "medium"
        }.first()
    }

    suspend fun getPreviousTextSizePreference(): String {
        return context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.previousTextSize] ?: "medium"
        }.first()
    }

    suspend fun getColorModePreference(): String {
        return context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.darkMode] ?: "system"
        }.first()
    }

    // Se guarda el tamaño del texto actual y el anterior para poder calcular el factor de cambio
    suspend fun saveTextSizePreference(value: String) {
        val currentTextSize = getTextSizePreference()
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.previousTextSize] = currentTextSize
            preferences[PreferencesKeys.textSize] = value
        }
        Log.d("Lifecycle", "currentTextSize: $currentTextSize, value: $value")
        textSizePreferenceLiveData.value = value
    }

    suspend fun saveDarkModePreference(value: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.darkMode] = value
        }
        applyDarkModePreference(value)
    }

    // Se aplica la primera vez que se inicializa la actividad/fragmento
    fun applyTextSizePreference(allTextView: List<TextView>, textSize: String) {
        val size = textSizeFactor(textSize)
        updateTextViewsSize(allTextView, size)
    }

    // Se aplica la primera vez que se inicializa el elemento
    fun applyTextSizePreference(textView: TextView, textSize: String) {
        val size = textSizeFactor(textSize)
        updateTextViewSize(textView, size)
    }

    // Función auxiliar para calcular el factor inicial de cambio de tamaño del texto
    private fun textSizeFactor(textSize: String): Float {
        return when (textSize) {
            "small" -> 0.8f
            "medium" -> 1.0f
            "big" -> 1.2f
            else -> 1.0f
        }
    }

    // Se aplica cuando se cambia el tamaño del texto
    fun applyTextSizePreference(allTextView: List<TextView>, textSize: String, previousTextSize: String) {
        val size = when (textSize) {
            "small" -> {
                when (previousTextSize) {
                    // 1.0 / 1.25 = 0.8
                    "medium" -> 0.8f
                    // 1.2 / 1.5 = 0.8
                    "big" -> 1.0f/1.5f
                    // Ya es el tamaño pequeño
                    else -> 1.0f
                }
            }
            "medium" -> {
                when (previousTextSize) {
                    // 0.8 * 1.25 = 1.0
                    "small" -> 1.25f
                    "big" -> 1.0f/1.2f
                    // Ya es el tamaño medio
                    else -> 1.0f
                }
            }
            "big" -> {
                when (previousTextSize) {
                    // 0.8 * 1.25 * 1.2 = 1.2
                    "small" -> 1.5f
                    "medium" -> 1.2f
                    // Ya es el tamaño grande
                    else -> 1.0f
                }
            }
            else -> 1.0f
        }
        updateTextViewsSize(allTextView, size)
    }

    fun applyDarkModePreference(darkMode: String) {
        when (darkMode) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "night" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "system" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    // Se aplica la primera vez que se inicializa la app
    suspend fun applyPreferences(allTextView: List<TextView>) {
        applyTextSizePreference(allTextView, getTextSizePreference())
        applyDarkModePreference(getColorModePreference())
    }

    private fun updateTextViewsSize(allTextView: List<TextView>, factor: Float) {
        allTextView.forEach { textView ->
            val originalTextSize = textView.textSize
            val newTextSize = originalTextSize * factor
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, newTextSize)
        }
    }

    private fun updateTextViewSize(textView: TextView, factor: Float) {
        val originalTextSize = textView.textSize
        val newTextSize = originalTextSize * factor
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, newTextSize)
    }

    fun getAllTextViews(view: ViewGroup): List<TextView> {
        val textViewList = mutableListOf<TextView>()
        for (i in 0 until view.childCount) {
            val child = view.getChildAt(i)
            if (child is TextView) {
                textViewList.add(child)
            } else if (child is TabLayout) {
                for (j in 0 until child.tabCount) {
                    val tab = child.getTabAt(j)
                    Log.d("TabLayout", "tab: $tab")
                    val tabView = tab?.customView
                    Log.d("TabLayout", "tabView: $tabView")
                    val textView = tabView?.findViewById<TextView>(R.id.tab_item_text)
                    Log.d("TabLayout", "textView: $textView")
                    textViewList.add(textView?:continue)
                }
            } else if (child is ViewGroup) {
                textViewList.addAll(getAllTextViews(child))
            }
        }
        return textViewList
    }

    // Singleton para crear una única instancia de PreferencesManager
    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: PreferencesManager? = null
        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferencesManager(context).also { INSTANCE = it }
            }
        }
    }

}
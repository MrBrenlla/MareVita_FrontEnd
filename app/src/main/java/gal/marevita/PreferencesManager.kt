package gal.marevita

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager private constructor(private val context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("MarVita", Context.MODE_PRIVATE)

    fun saveLastSession(userName: String, password: String) {
        sharedPreferences.edit().apply {
            putString("USERNAME", userName)
            putString("PASSWORD", password)
            apply()
        }
    }

    fun getLastSession(): Pair<String?, String?> {
        val userName = sharedPreferences.getString("USERNAME", null)
        val password = sharedPreferences.getString("PASSWORD", null)
        return Pair(userName, password)
    }

    fun getUsername(): String {
        return sharedPreferences.getString("USERNAME", null) ?: ""
    }

    fun removePassword(){
        sharedPreferences.edit().apply {
            remove("PASSWORD")
            apply()
        }
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
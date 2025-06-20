package gal.marevita


import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.menu)
        preferencesManager = PreferencesManager.getInstance(this)

        // Iniciar la actividad de registro al abrir la aplicación
        startActivity(Intent(this, InicioActivity::class.java))

        // Finalizar la actividad actual para que no se pueda volver atrás a MainActivity
        finish()
    }
}

package gal.marevita

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import gal.marevita.databinding.PerfilActivityBinding

class PerfilActivity() : AppCompatActivity() {

    private lateinit var binding: PerfilActivityBinding

    val url = AppConfig.BASE_URL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = PerfilActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
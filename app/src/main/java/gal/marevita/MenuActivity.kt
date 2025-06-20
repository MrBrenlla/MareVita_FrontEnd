package gal.marevita

import android.content.Intent
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import gal.marevita.databinding.MenuBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class MenuActivity : AppCompatActivity() {

    private lateinit var binding: MenuBinding

    private lateinit var executor: Executor

    val url = AppConfig.BASE_URL

    var actualFragment = "feed"

    val imagePicker = ImagePicker(this)

    private var waitAction=false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = MenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        executor = ContextCompat.getMainExecutor(this)


        val screenHeight = Resources.getSystem().displayMetrics.heightPixels

        binding.capture.setOnClickListener {

            binding.anzuelo.visibility = View.VISIBLE

            binding.anzuelo.animate()
                .translationY(screenHeight * 1.1f)
                .setDuration(800)
                .withEndAction {
                    waitAction=true
                    startActivity(Intent(this, NovaCapturaActivity::class.java))
                    overridePendingTransition(0, 0)
                    lifecycleScope.launch {
                        resetAnzuelo(-screenHeight*1.2f)
                    }
                }
                .start()
        }

        binding.maps.setOnClickListener {
            startActivity(Intent(this, MapaActivity::class.java))
        }

        binding.home.setOnClickListener {
            if (actualFragment == "feed") return@setOnClickListener
            val fragment = FeedFragment()
            supportFragmentManager.beginTransaction()
                .replace(binding.fragmentContainerView.id, fragment)
                .commit()
            actualFragment = "feed"
        }

        binding.historic.setOnClickListener {
            if (actualFragment == "historic") return@setOnClickListener
            val fragment = HistoricoEAlertasFragment()
            supportFragmentManager.beginTransaction()
                .replace(binding.fragmentContainerView.id, fragment)
                .commit()
            actualFragment = "historic"
        }

        binding.profile.setOnClickListener {
            if (actualFragment == "profile") return@setOnClickListener
            val fragment = PerfilFragment(imagePicker)
            supportFragmentManager.beginTransaction()
                .replace(binding.fragmentContainerView.id, fragment)
                .commit()
            actualFragment = "profile"
        }

        var fragment:Fragment = FeedFragment()

        when(intent.getStringExtra("target")) {
            "alertas" ->{fragment = HistoricoEAlertasFragment("alertas")
                actualFragment = "historic"}
        }
            supportFragmentManager.beginTransaction()
                .replace(binding.fragmentContainerView.id, fragment)
                .commit()

    }

    private suspend fun resetAnzuelo(pos: Float) {
        delay(1200)
        waitAction=false
        binding.anzuelo.y = pos
    }

    fun endThis(callback: () -> Unit){
        if (!waitAction) callback()
        else lifecycleScope.launch {
            endLate(callback)
        }
    }

    private suspend fun endLate(callback: () -> Unit){
        delay(1200)
        callback()
    }

    override fun onStart() {
        super.onStart()
    }

}
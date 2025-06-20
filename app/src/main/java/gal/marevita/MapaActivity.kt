package gal.marevita

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import gal.marevita.databinding.MapasBinding

class MapaActivity : AppCompatActivity() {

    private lateinit var binding: MapasBinding

    private lateinit var drawerLayout: DrawerLayout

    private var actualFragment = "latestCaptures"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = MapasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        drawerLayout = binding.drawerLayout

        val fragment = UltimasCapturasFragment()
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment)
            .commit()

        setMenu()
    }


    private fun setMenu() {
        binding.menuButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        binding.menu.setNavigationItemSelectedListener { menuItem ->
            if (!isFinishing && !isDestroyed)
                runOnUiThread {
                    when (menuItem.itemId) {
                        R.id.weather -> return@runOnUiThread
                        R.id.temp -> {
                            if (actualFragment == "temp") return@runOnUiThread
                            val fragment = WindyFragment("temp")
                            supportFragmentManager.beginTransaction()
                                .replace(binding.fragmentContainer.id, fragment).commit()
                            actualFragment = "temp"
                        }

                        R.id.humidity -> {
                            if (actualFragment == "humidity") return@runOnUiThread
                            val fragment = WindyFragment("rh") //layer "rh" = humidade
                            supportFragmentManager.beginTransaction()
                                .replace(binding.fragmentContainer.id, fragment).commit()
                            actualFragment = "humidity"
                        }

                        R.id.wind -> {
                            if (actualFragment == "wind") return@runOnUiThread
                            val fragment = WindyFragment("wind")
                            supportFragmentManager.beginTransaction()
                                .replace(binding.fragmentContainer.id, fragment).commit()
                            actualFragment = "wind"
                        }

                        R.id.pressure -> {
                            if (actualFragment == "pressure") return@runOnUiThread
                            val fragment = WindyFragment("pressure")
                            supportFragmentManager.beginTransaction()
                                .replace(binding.fragmentContainer.id, fragment).commit()
                            actualFragment = "pressure"
                        }

                        R.id.rain -> {
                            if (actualFragment == "rain") return@runOnUiThread
                            val fragment = WindyFragment("rain")
                            supportFragmentManager.beginTransaction()
                                .replace(binding.fragmentContainer.id, fragment).commit()
                            actualFragment = "rain"
                        }

                        R.id.clouds -> {
                            if (actualFragment == "clouds") return@runOnUiThread
                            val fragment = WindyFragment("clouds")
                            supportFragmentManager.beginTransaction()
                                .replace(binding.fragmentContainer.id, fragment).commit()
                            actualFragment = "clouds"
                        }

                        R.id.uvindex -> {
                            if (actualFragment == "uvindex") return@runOnUiThread
                            val fragment = WindyFragment("uvindex")
                            supportFragmentManager.beginTransaction()
                                .replace(binding.fragmentContainer.id, fragment).commit()
                            actualFragment = "uvindex"
                        }

                        R.id.waves -> {
                            if (actualFragment == "waves") return@runOnUiThread
                            val fragment = WindyFragment("waves")
                            supportFragmentManager.beginTransaction()
                                .replace(binding.fragmentContainer.id, fragment).commit()
                            actualFragment = "waves"
                        }

                        R.id.currents -> {
                            if (actualFragment == "currents") return@runOnUiThread
                            val fragment = WindyFragment("currents")
                            supportFragmentManager.beginTransaction()
                                .replace(binding.fragmentContainer.id, fragment).commit()
                            actualFragment = "currents"
                        }

                        R.id.sst -> {
                            if (actualFragment == "sst") return@runOnUiThread
                            val fragment = WindyFragment("sst")
                            supportFragmentManager.beginTransaction()
                                .replace(binding.fragmentContainer.id, fragment).commit()
                            actualFragment = "sst"
                        }

                        R.id.latestCaptures -> {
                            if (actualFragment == "latestCaptures") return@runOnUiThread
                            val fragment = UltimasCapturasFragment()
                            supportFragmentManager.beginTransaction()
                                .replace(binding.fragmentContainer.id, fragment).commit()
                            actualFragment = "latestCaptures"
                        }
                    }

                    fun Int.toPx(): Int = (this * resources.displayMetrics.density).toInt()

                    if (actualFragment == "latestCaptures") {
                        val params =
                            binding.menuButton.layoutParams as ConstraintLayout.LayoutParams
                        params.bottomMargin = 30.toPx()
                        binding.menuButton.layoutParams = params
                    } else {
                        val params =
                            binding.menuButton.layoutParams as ConstraintLayout.LayoutParams
                        params.bottomMargin = 70.toPx()
                        binding.menuButton.layoutParams = params
                    }

                    drawerLayout.closeDrawer(GravityCompat.END)
                }

            true
        }
    }

}
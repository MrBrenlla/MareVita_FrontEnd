package gal.marevita

import android.app.Activity
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputLayout
import gal.marevita.CeboSelector.stringOfList
import gal.marevita.ConditionsTranslator.allDefaultConditions
import gal.marevita.ConditionsTranslator.getMesurementUnit
import gal.marevita.ConditionsTranslator.translateGalego
import gal.marevita.databinding.NovaAlertaBinding
import gal.marevita.ui.CaptureSelectorPopUp
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class NovaAlertaActivity : AppCompatActivity() {

    private lateinit var binding: NovaAlertaBinding

    private lateinit var locationPickerLauncher: ActivityResultLauncher<Intent>

    var gps: GpsLocation? = null

    private lateinit var allBaits: List<String>
    var baits = listOf<String>()
    private lateinit var allFish: List<String>
    var fish = listOf<String>()

    var relatedCapture: String? = null
    var alertId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = NovaAlertaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        alertId = intent.getStringExtra("alertId")
        relatedCapture = intent.getStringExtra("capture")

        CeboSelector.getBaits { l -> allBaits = l }
        PeixeSelector.getFish { l -> allFish = l }

        setMap()

        binding.baitsButton.setOnClickListener {
            CeboSelector.mostrar(this, allBaits, baits, object : CeboSelector.Callback {
                override fun onCebosSeleccionados(seleccionados: List<String>) {
                    baits = ArrayList<String>(seleccionados)
                    binding.baitsText.setText(stringOfList(baits))
                }
            })
        }

        binding.fishButton.setOnClickListener {
            PeixeSelector.mostrar(this, allFish, fish, object : PeixeSelector.Callback {
                override fun onPeixesSeleccionados(seleccionados: List<String>) {
                    fish = ArrayList<String>(seleccionados)
                    binding.fishText.setText(stringOfList(fish))
                }
            })
        }

        binding.relatedCaptureText.apply {
            setTextColor(context.getColor(R.color.dark_blue))
            paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
        }
        binding.relatedCaptureButton.setOnClickListener {
            if (!isFinishing && !isDestroyed) runOnUiThread {
                CaptureSelectorPopUp(this@NovaAlertaActivity).showCapturePopup() { capture ->
                    if (!isFinishing && !isDestroyed)
                        runOnUiThread {
                            setValues(capture)
                            relatedCapture = capture.id
                        }
                }
            }
        }

        binding.save.setOnClickListener {
            saveAlert()
        }

        if (alertId != null) initValues(alertId!!)
        else if (relatedCapture != null) setValues(relatedCapture!!)
        else setConditions()

    }

    fun setMap() {
        locationPickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val lat = data?.getDoubleExtra("lat", 0.0)
                val lng = data?.getDoubleExtra("lng", 0.0)

                if (lat == null || lng == null) {
                    Toast.makeText(
                        baseContext,
                        "Erro ao coller a localización, volva intentalo",
                        Toast.LENGTH_LONG
                    ).show()
                    return@registerForActivityResult
                }
                gps = GpsLocation(lat, lng)

                binding.locationText.setText("Lat: $lat, Lng: $lng")
            }
        }

        binding.locationButton.setOnClickListener {
            val intent = Intent(this, SelectLocationActivity::class.java)
            locationPickerLauncher.launch(intent)
        }
    }

    fun setConditions() {
        val container = binding.conditions
        val inflater = LayoutInflater.from(this)

        allDefaultConditions.forEach { c ->
            val vista = inflater.inflate(R.layout.condicion_alerta, container, false)
            vista.findViewById<TextView>(R.id.conditionName).text = "${translateGalego(c)}:"
            vista.tag = c
            vista.findViewById<TextInputLayout>(R.id.valueLayout).suffixText = getMesurementUnit(c)
            container.addView(vista)
            vista.findViewById<SwitchMaterial>(R.id.active)
                .setOnCheckedChangeListener { _, isChecked ->
                    val valueText = vista.findViewById<EditText>(R.id.value)
                    val errorText = vista.findViewById<EditText>(R.id.error)
                    if (isChecked) {
                        valueText.isEnabled = true
                        errorText.isEnabled = true
                    } else {
                        valueText.isEnabled = false
                        errorText.isEnabled = false
                    }
                }
        }
    }

    fun getConditions(): Pair<List<WeatherCondition>, Boolean> {
        val conditions = ArrayList<WeatherCondition>()

        val container = binding.conditions
        var valid = true

        container.children.forEach { vista ->
            if (vista.findViewById<SwitchMaterial>(R.id.active).isChecked) {
                val conditionName = vista.tag.toString()
                val valueView = vista.findViewById<TextInputLayout>(R.id.valueLayout)
                val errorView = vista.findViewById<TextInputLayout>(R.id.errorLayout)
                val value = vista.findViewById<EditText>(R.id.value).text.toString()
                val error = vista.findViewById<EditText>(R.id.error).text.toString()

                if (value.isEmpty()) {
                    valueView.error = "Valor inválido"
                    valid = false
                } else valueView.error = null
                if (error.isEmpty()) {
                    errorView.error = "Valor inválido"
                    valid = false
                } else errorView.error = null

                if (valid) conditions.add(
                    WeatherCondition(
                        value.toDouble(),
                        conditionName,
                        error.toDouble()
                    )
                )
            }
        }

        return Pair(conditions, valid)
    }

    fun getAlertFishes(): List<Fish> {
        val fishList = ArrayList<Fish>()
        fish.forEach { f -> fishList.add(Fish(f)) }
        return fishList
    }

    fun initValues(alertId: String) {
        AlertAPI().getAlert(alertId) { success, a, response ->
            if (!isFinishing && !isDestroyed)
                runOnUiThread {
                    if (success && a != null) {
                        initValues(a)
                        relatedCapture = a.id
                    } else {
                        Toast.makeText(
                            baseContext,
                            "Erro de conexión: ${response}.",
                            Toast.LENGTH_LONG
                        )
                    }
                }
        }
    }

    private fun initValues(alert: Alert) {
        binding.title.text = "Editar alerta"

        binding.nameText.setText(alert.name)
        gps = alert.gpsLocation
        gps?.let { binding.locationText.setText("Lat: ${it.latitude}, Lng: ${it.latitude}") }
        fish = alert.fish.map { it.name }.toList()
        binding.fishText.setText(stringOfList(fish))
        baits = alert.baits
        binding.baitsText.setText(stringOfList(baits))

        relatedCapture = alert.relatedCapture

        if (relatedCapture != null) {

            CaptureAPI().getCapture(relatedCapture!!) { success, c, response ->
                if (!isFinishing && !isDestroyed)
                    runOnUiThread {
                        if (c == null) return@runOnUiThread
                        var dateTime = ZonedDateTime.parse(c.dateTime)
                        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("gl"))
                        val date = dateTime.format(formatter)
                        binding.relatedCaptureText.setText(alert.location + " (" + date + ")")
                        binding.relatedCaptureText.setOnClickListener {
                            CapturaPopUp(this@NovaAlertaActivity) { c ->
                                if (c != null) {
                                    binding.relatedCaptureText.setOnClickListener {}
                                    binding.relatedCaptureText.setText(null)
                                    relatedCapture = null
                                }
                            }.fromId(relatedCapture!!, 2)
                        }
                    }
            }
        }
        setConditions()

        alert.weatherConditions.forEach { wc ->
            val conditionView = binding.conditions.findViewWithTag<View>(wc.name)
            if (conditionView != null) {
                val valueText = conditionView.findViewById<EditText>(R.id.value)
                valueText.setText(wc.value.toString())
                valueText.isEnabled = true
                val errorText = conditionView.findViewById<EditText>(R.id.error)
                errorText.setText(wc.error.toString())
                errorText.isEnabled = true
                conditionView.findViewById<SwitchMaterial>(R.id.active).isChecked = true
            }
        }
    }

    fun setValues(capturaId: String) {
        CaptureAPI().getCapture(capturaId) { success, c, response ->
            if (!isFinishing && !isDestroyed)
                runOnUiThread {
                    if (success && c != null) {
                        setValues(c)
                        relatedCapture = c.id
                    } else {
                        Toast.makeText(
                            baseContext,
                            "Erro de conexión: ${response}.",
                            Toast.LENGTH_LONG
                        )
                    }
                }
        }
    }

    private fun setValues(capture: Capture) {
        gps = capture.gpsLocation
        gps?.let { binding.locationText.setText("Lat: ${it.latitude}, Lng: ${it.latitude}") }
        fish = capture.fish.map { it.name }.toList()
        binding.fishText.setText(stringOfList(fish))
        baits = capture.baits
        binding.baitsText.setText(stringOfList(baits))
        var dateTime = ZonedDateTime.parse(capture.dateTime)
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("gl"))
        val date = dateTime.format(formatter)
        binding.relatedCaptureText.setText(capture.location + " (" + date + ")")
        binding.relatedCaptureText.setOnClickListener {
            CapturaPopUp(this@NovaAlertaActivity) { c ->
                if (c != null) {
                    binding.relatedCaptureText.setOnClickListener {}
                    binding.relatedCaptureText.setText(null)
                    relatedCapture = null
                }
            }.fromId(relatedCapture!!, 2)
        }

        binding.conditions.removeAllViews()
        setConditions()

        capture.weatherConditions.forEach { wc ->
            val conditionView = binding.conditions.findViewWithTag<View>(wc.name)
            if (conditionView != null) {
                val valueText = conditionView.findViewById<EditText>(R.id.value)
                valueText.setText(wc.value.toString())
                valueText.isEnabled = true
                conditionView.findViewById<EditText>(R.id.error).isEnabled = true
                conditionView.findViewById<SwitchMaterial>(R.id.active).isChecked = true
            }
        }
    }

    fun saveAlert() {


        binding.nameLayout.error = null
        binding.locationLayout.error = null
        binding.baitsLayout.error = null
        binding.fishLayout.error = null
        binding.relatedCaptureLayout.error = null

        val (conditions, valid) = getConditions()
        if (!valid) return

        if (binding.nameText.text.toString().isEmpty()) {
            binding.nameLayout.error = "Nome inválido"
            binding.error.text = "É obrigatorio indicar nome."
            return
        }

        if (gps == null) {
            binding.locationLayout.error = "Localización inválida"
            binding.error.text = "É obrigatorio indicar localización."
            return
        }

        var alert: Alert

        if (alertId != null)
            alert = Alert(
                id = alertId,
                name = binding.nameText.text.toString(),
                gpsLocation = gps,
                fish = getAlertFishes(),
                baits = baits,
                weatherConditions = conditions,
                relatedCapture = relatedCapture
            )
        else alert = Alert(
            name = binding.nameText.text.toString(),
            gpsLocation = gps,
            fish = getAlertFishes(),
            baits = baits,
            weatherConditions = conditions,
            relatedCapture = relatedCapture
        )

        AlertAPI().sendAlert(alert) { success, response ->
            if (!isFinishing && !isDestroyed)
                runOnUiThread {
                    if (success) {
                        Toast.makeText(baseContext, "Alerta gardada", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        Toast.makeText(
                            baseContext,
                            "Erro de conexión: ${response}.",
                            Toast.LENGTH_LONG
                        )
                            .show()
                    }
                }
        }
    }
}
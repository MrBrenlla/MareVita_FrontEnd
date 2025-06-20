package gal.marevita

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import gal.marevita.CeboSelector.stringOfList
import gal.marevita.databinding.NovaCapturaBinding
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import androidx.core.graphics.drawable.toDrawable

class NovaCapturaActivity : AppCompatActivity() {

    private lateinit var binding: NovaCapturaBinding

    private lateinit var locationPickerLauncher: ActivityResultLauncher<Intent>

    private lateinit var imagePicker: ImagePicker

    var gps: GpsLocation? = null

    private lateinit var allBaits: List<String>
    var baits = listOf<String>()
    private lateinit var allFish: List<String>
    var fish = listOf<String>()

    var images = ArrayList<String>()
    var imagesCharging = 0

    var date: LocalDate? = null
    var time: LocalTime? = null

    var security = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = NovaCapturaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val screenHeight = Resources.getSystem().displayMetrics.heightPixels

        binding.anzuelo.translationY = screenHeight * 1.2f

        binding.scroll.translationY = screenHeight * 1.1f

        binding.anzuelo.animate()
            .translationY(0f)
            .setDuration(800)
            .start()

        binding.scroll.animate()
            .translationY(0f)
            .setDuration(800)
            .start()



        CeboSelector.getBaits { l -> allBaits = l }
        PeixeSelector.getFish { l -> allFish = l }

        setMap()
        setDateTime()
        setSecurityButons()
        setImages()

        binding.fishesScroll.viewTreeObserver.addOnGlobalLayoutListener {
            val maxHeight = 270 * Resources.getSystem().displayMetrics.density
            if (binding.fishes.height > maxHeight) {
                binding.fishesScroll.layoutParams.height = maxHeight.toInt()
                binding.fishesScroll.requestLayout()
            } else {
                binding.fishesScroll.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                binding.fishesScroll.requestLayout()
            }
        }

        binding.photosScroll.viewTreeObserver.addOnGlobalLayoutListener {
            val maxHeight = 270 * Resources.getSystem().displayMetrics.density
            if (binding.photos.height > maxHeight) {
                binding.photosScroll.layoutParams.height = maxHeight.toInt()
                binding.photosScroll.requestLayout()
            } else {
                binding.photosScroll.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                binding.photosScroll.requestLayout()
            }
        }

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
                    fishList()
                }
            })
        }

        binding.save.setOnClickListener {
            saveCapture()
        }

    }

    override fun onStart() {
        super.onStart()
    }

    fun fishList() {

        val copy = ArrayList<String>(fish)

        val container = binding.fishes
        val inflater = LayoutInflater.from(this)

        val toRemove = ArrayList<View>()

        container.children.forEach { fl ->
            val name = fl.findViewById<TextView>(R.id.nomePeixe).text
            if (copy.contains(name)) copy.remove(name)
            else toRemove.add(fl)
        }

        toRemove.map { v -> container.removeView(v) }

        copy.forEach { f ->
            val vista = inflater.inflate(R.layout.lista_peixes, container, false)
            vista.findViewById<TextView>(R.id.nomePeixe).text = f
            container.addView(vista)
        }

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

    fun updateToCurrentDateTime() {
        date = LocalDate.now()
        time = LocalTime.now()
        val currentDate = date?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        val currentTime = time?.format(DateTimeFormatter.ofPattern("HH:mm"))
        binding.date.setText(currentDate)
        binding.time.setText(currentTime)
    }

    fun setDateTime() {

        val calendar = Calendar.getInstance()


        binding.date.setOnClickListener {
            if (binding.date.isEnabled) {
                val datePicker =
                    MaterialDatePicker.Builder.datePicker()
                        .setTitleText("Escolle unha data")
                        .setTheme(R.style.MaterialCalendarTheme)
                        .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                        .build()

                datePicker.addOnPositiveButtonClickListener { selection ->
                    val instant = Instant.ofEpochMilli(selection)
                    val zoneId = ZoneId.systemDefault()
                    val selectedDate = LocalDateTime.ofInstant(instant, zoneId).toLocalDate()

                    binding.date.setText(
                        "%02d/%02d/%04d".format(
                            selectedDate.dayOfMonth,
                            selectedDate.monthValue,
                            selectedDate.year
                        )
                    )
                    date = selectedDate
                }

                datePicker.show(supportFragmentManager, "MATERIAL_DATE_PICKER")
            }
        }

        binding.time.setOnClickListener {
            if (binding.time.isEnabled) {
                val timePicker = TimePickerDialog(
                    this,
                    R.style.MaterialCalendarTheme,
                    { _, hour, minute ->
                        binding.time.setText("%02d:%02d".format(hour, minute))
                        time = LocalTime.of(hour, minute)
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                )
                timePicker.show()
            }
        }

        binding.now.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                updateToCurrentDateTime()
                binding.date.isEnabled = false
                binding.time.isEnabled = false
            } else {
                binding.date.isEnabled = true
                binding.time.isEnabled = true
                binding.date.text.clear()
                binding.time.text.clear()
            }
        }
    }

    fun setSecurityButons() {

        binding.publicButton.isChecked = true

        binding.publicButton.setOnClickListener {
            security = 0
            binding.publicButton.isChecked = true
            binding.friendsButton.isChecked = false
            binding.privateButton.isChecked = false
        }

        binding.friendsButton.setOnClickListener {
            security = 1
            binding.publicButton.isChecked = false
            binding.friendsButton.isChecked = true
            binding.privateButton.isChecked = false
        }

        binding.privateButton.setOnClickListener {
            security = 2
            binding.publicButton.isChecked = false
            binding.friendsButton.isChecked = false
            binding.privateButton.isChecked = true
        }
    }

    fun setImages() {
        val container = binding.photos
        val inflater = LayoutInflater.from(this)

        imagePicker = ImagePicker(this) { uri ->
            val vista = inflater.inflate(R.layout.image_upload, container, false)
            vista.findViewById<ImageView>(R.id.image).setImageURI(uri)
            container.addView(vista)
            imagesCharging++

            val uploader = ImageAPI(this@NovaCapturaActivity)

            uploader.sendFile(uri, "/images/new") {_,result ->
                if (!isFinishing && !isDestroyed)
                    runOnUiThread {
                        vista.findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
                        imagesCharging--

                        if (result == null) {
                            vista.findViewById<ImageView>(R.id.cross).visibility = View.VISIBLE
                        } else {
                            images.add(result)
                        }

                        vista.setOnClickListener {
                            container.removeView(vista)
                            images.remove(result)
                        }
                    }
            }
        }

        binding.photoButton.setOnClickListener {
            if (container.childCount < 8)
                imagePicker.startImagePicker()
            else
                Toast.makeText(this@NovaCapturaActivity, "Máximo de 6 imaxes", Toast.LENGTH_SHORT)
                    .show()
        }
    }

    fun getFishCuantities(): List<Fish> {
        var list = ArrayList<Fish>()

        binding.fishes.children.forEach { v ->
            val name = v.findViewById<TextView>(R.id.nomePeixe).text.toString()
            val n =
                v.findViewById<TextInputEditText>(R.id.cantidadeText).text.toString().toIntOrNull()
            if (n != null && n > 0) list.add(Fish(name, n))
        }

        return list
    }

    fun saveCapture() {

        val cuantityFish = getFishCuantities()

        binding.dateLayout.error = null
        binding.timeLayout.error = null
        binding.locationLayout.error = null
        binding.baitsLayout.error = null
        binding.fishesScroll.setBackgroundResource(R.color.shadow_grey20)

        if (gps == null) {
            binding.locationLayout.error = "Localización inválida"
            binding.error.text = "É obrigatorio indicar localización."
            return
        }

        if (date == null) {
            binding.dateLayout.error = "Data inválida"
            binding.error.text = "É obrigatorio indicar data."
            return
        }

        if (time == null) {
            binding.timeLayout.error = "Hora inválida"
            binding.error.text = "É obrigatorio indicar hora."
            return
        }

        if (baits.isEmpty()) {
            binding.baitsLayout.error = "Sin cebos"
            binding.error.text = "É obrigatorio indicar os cebos usados."
            return
        }

        if (cuantityFish.isEmpty()) {
            binding.fishesScroll.setBackgroundResource(R.color.red20)
            binding.error.text =
                "É obrigatorio indicar os peixes pescado. Débese ter en conta que se a cantidade é 0 non se teñen en conta."
            return
        }

        var dateTime: ZonedDateTime = ZonedDateTime.of(date, time, ZoneId.systemDefault())

        val capture = Capture(
            security = security,
            dateTime = dateTime.toString(),
            gpsLocation = gps!!,
            imageCaption = binding.comentText.text.toString(),
            images = images,
            baits = baits,
            fish = cuantityFish
        )

        CaptureAPI().sendCapture(capture) { success, response ->
            if (!isFinishing && !isDestroyed)
                runOnUiThread {
                    if (success) {
                        Toast.makeText(baseContext, "Captura gardada", Toast.LENGTH_LONG).show()
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

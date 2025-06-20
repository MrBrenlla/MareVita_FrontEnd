package gal.marevita

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import gal.marevita.databinding.CapturasPerfilBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale


class CapturasPerfilFragment(val userName: String) : Fragment() {

    private lateinit var binding: CapturasPerfilBinding

    private var images = mutableMapOf<String, Bitmap>()

    private var showingGroup = false
    private lateinit var backCallback: OnBackPressedCallback

    private var job: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = CapturasPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setFilter()

        binding.filter.setOnClickListener {
            if (!isAdded) return@setOnClickListener
            activity?.runOnUiThread {
                binding.filterArrow.visibility = View.GONE
                binding.filter.visibility = View.GONE
                CaptureAPI().getCaptures("/capture/user/$userName") { success, captures, response ->
                    if (success && captures != null) {
                        job?.cancel()
                        job = lifecycleScope.launch {
                            setGroups(binding.dropdownMenu.text.toString(), captures)
                        }
                    } else {
                        Log.e("CapturasPerfilFragment", "Erro ao obter as capturas: $response")
                        if (!isAdded) return@getCaptures
                        requireActivity().runOnUiThread {
                            Toast.makeText(
                                requireContext(),
                                "Erro de conexión: ${response}.",
                                Toast.LENGTH_LONG
                            )
                        }
                    }
                }
            }
        }

        backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (showingGroup) {
                    binding.filterArrow.visibility = View.GONE
                    binding.filter.visibility = View.GONE
                    CaptureAPI().getCaptures("/capture/user/$userName") { success, captures, response ->

                        if (success && captures != null) {
                            showingGroup = false
                            job?.cancel()
                            job = lifecycleScope.launch {
                                setGroups(binding.dropdownMenu.text.toString(), captures)
                            }
                        } else {
                            if (!isAdded) return@getCaptures
                            requireActivity().runOnUiThread {
                                Toast.makeText(
                                    requireContext(),
                                    "Erro de conexión: $response",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                } else if (binding.dropdownMenu.text.toString() != "Todos") {
                    binding.dropdownMenu.setText("Todos", false)

                    CaptureAPI().getCaptures("/capture/user/$userName") { success, captures, response ->
                        if (success && captures != null) {
                            job?.cancel()
                            job = lifecycleScope.launch {
                                setGroups("Todos", captures)
                            }
                        } else {
                            if (!isAdded) return@getCaptures
                            requireActivity().runOnUiThread {
                                Toast.makeText(
                                    requireContext(),
                                    "Erro de conexión: ${response}.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback)

    }

    private fun setFilter() {
        val options = mutableListOf("Todos", "Peixe", "Lugar", "Cebo", "Mes")
        if (userName == PreferencesManager.getInstance(requireContext()).getUsername()) options.add(
            "Privacidade"
        )
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, options)

        binding.dropdownMenu.setAdapter(adapter)


        binding.dropdownMenu.setOnItemClickListener { _, _, position, _ ->
            CaptureAPI().getCaptures("/capture/user/$userName") { success, captures, response ->

                if (success && captures != null) {
                    job?.cancel()
                    job = lifecycleScope.launch {
                        setGroups(options[position], captures)
                    }
                } else {
                    if (!isAdded) return@getCaptures
                    requireActivity().runOnUiThread {
                        Toast.makeText(
                            requireContext(),
                            "Erro de conexión: ${response}.",
                            Toast.LENGTH_LONG
                        )
                    }
                }

            }
        }

        binding.dropdownMenu.setText("Todos", false)

        CaptureAPI().getCaptures("/capture/user/$userName") { success, captures, response ->

            if (success && captures != null) {
                job?.cancel()
                job=lifecycleScope.launch {
                    setGroups("Todos", captures)
                }
            } else {
                if (!isAdded) return@getCaptures
                requireActivity().runOnUiThread {
                    Toast.makeText(
                        requireContext(), "Erro de conexión: ${response}.", Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

    }

    private suspend fun setGroups(option: String, captures: List<Capture>) {

        var groups = HashMap<String, List<Capture>>()

        binding.filterArrow.visibility = View.GONE
        binding.filter.visibility = View.GONE

        showingGroup = false

        if (option == "Todos") return setIndividuals(captures)

        captures.forEach { c ->
            when (option) {
                "Peixe" -> {
                    c.fish.forEach { f ->
                        if (groups.containsKey(f.name)) {
                            groups[f.name] = groups[f.name]!!.plus(c)
                        } else {
                            groups[f.name] = listOf(c)
                        }
                    }
                }

                "Lugar" -> {
                    if (c.location != null)
                        if (groups.containsKey(c.location)) {
                            groups[c.location] = groups[c.location]!!.plus(c)
                        } else {
                            groups[c.location] = listOf(c)
                        }
                }

                "Cebo" -> {
                    c.baits.forEach { b ->
                        if (groups.containsKey(b)) {
                            groups[b] = groups[b]!!.plus(c)
                        } else {
                            groups[b] = listOf(c)
                        }
                    }
                }

                "Mes" -> {
                    var dateTime = ZonedDateTime.parse(c.dateTime)
                    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("gl"))
                    val month = dateTime.format(formatter)
                    if (groups.containsKey(month)) {
                        groups[month] = groups[month]!!.plus(c)
                    } else {
                        groups[month] = listOf(c)
                    }
                }

                "Privacidade" -> {
                    val p = when (c.security) {
                        null -> "N/A"
                        0 -> "Pública"
                        1 -> "Só amigos"
                        2 -> "Privada"
                        else -> "N/A"
                    }
                    if (groups.containsKey(p)) {
                        groups[p] = groups[p]!!.plus(c)
                    } else {
                        groups[p] = listOf(c)
                    }
                }
            }
        }

        var sortedGroups = groups.toList()

        if(option == "Mes") sortedGroups = sortedGroups.sortedByDescending {
            val formatter = DateTimeFormatter.ofPattern("MMMM yyyy dd", Locale("gl"))
            LocalDate.parse("${it.first} 01", formatter)
        }
        else sortedGroups = sortedGroups.sortedBy { it.first }

        val container = binding.capturesList
        val inflater = LayoutInflater.from(activity)
        container.removeAllViews()

        for(i in 0 until 3) inflater.inflate(R.layout.grid_space, container, true)

        var i = -1
        sortedGroups.forEach { (group, captures) ->
            i++

            if(i<12) delay(300)
            else if(i<60) delay(50)

            val groupView = inflater.inflate(R.layout.grupo_capturas, container, false)

            groupView.findViewById<TextView>(R.id.filter).text = group

            var firstImage = true

            for (c in captures) {
                if (c.images.isNotEmpty()) {
                    if (firstImage) {
                        if (!images.containsKey(c.images[0]))
                            ImageAPI(requireContext()).getImage("/images/${c.owner}/${c.images[0]}") { success, image, response ->
                                if (!isAdded || image == null || !success) return@getImage
                                images[c.images[0]] = image
                                activity?.runOnUiThread {
                                    groupView.findViewById<ImageView>(R.id.image1)
                                        .setImageBitmap(image)
                                }
                            }
                        else groupView.findViewById<ImageView>(R.id.image1)
                            .setImageBitmap(images[c.images[0]])
                        firstImage = false
                    } else {
                        if (!images.containsKey(c.images[0]))
                            ImageAPI(requireContext()).getImage("/images/${c.owner}/${c.images[0]}") { success, image, response ->
                                if (!isAdded) return@getImage
                                if (success && image != null) {
                                    images[c.images[0]] = image
                                    activity?.runOnUiThread {
                                        groupView.findViewById<ImageView>(R.id.image2)
                                            .setImageBitmap(image)
                                    }
                                }
                            }
                        else groupView.findViewById<ImageView>(R.id.image2)
                            .setImageBitmap(images[c.images[0]])
                        break
                    }
                }
            }

            groupView.setOnClickListener {
                if (!isAdded) return@setOnClickListener
                activity?.runOnUiThread {
                    binding.filterArrow.visibility = View.VISIBLE
                    binding.filter.visibility = View.VISIBLE
                    binding.filter.text = group
                    showingGroup = true
                    job?.cancel()
                    job=lifecycleScope.launch {
                        setIndividuals(captures)
                    }
                }
            }

            if(!isAdded)return
            val anim = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
            groupView.startAnimation(anim)

            container.addView(groupView)
        }


    }

    private suspend fun setIndividuals(captures: List<Capture>) {
        val container = binding.capturesList
        val inflater = LayoutInflater.from(activity)
        container.removeAllViews()

        for(i in 0 until 3) inflater.inflate(R.layout.grid_space, container, true)

        var i = -1
        captures.sortedByDescending { ZonedDateTime.parse(it.dateTime) }
            .forEach { c ->

                i++

                if (i < 12) delay(300)
                else if (i < 60) delay(50)

                val individualView =
                    inflater.inflate(R.layout.captura_individual_perfil, container, false)

                var dateTime = ZonedDateTime.parse(c.dateTime)
                val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("gl"))
                val date = dateTime.format(formatter)

                individualView.findViewById<TextView>(R.id.date).text = date

                if (c.images.isNotEmpty()) {
                    if (!images.containsKey(c.images[0]))
                        ImageAPI(requireContext()).getImage("/images/${c.owner}/${c.images[0]}") { success, image, response ->
                            if (!isAdded || image == null || !success) return@getImage
                            activity?.runOnUiThread {
                                if (!isAdded) return@runOnUiThread
                                individualView.findViewById<ImageView>(R.id.image)
                                    .setImageBitmap(image)
                            }
                        }
                    else individualView.findViewById<ImageView>(R.id.image)
                        .setImageBitmap(images[c.images[0]])
                }

                individualView.setOnClickListener {
                    val capturesJson =
                        Json.encodeToString(ListSerializer(Capture.serializer()), captures)
                    val intent = Intent(requireContext(), FeedFiltradoActivity::class.java)
                    intent.putExtra("captures_json", capturesJson)
                    intent.putExtra("target", c.id ?: "")
                    startActivity(intent)
                }

                if (!isAdded) return
                val anim = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
                individualView.startAnimation(anim)

                container.addView(individualView)
            }


    }
}
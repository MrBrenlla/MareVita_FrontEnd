package gal.marevita

import android.content.res.Resources
import android.graphics.Paint
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import gal.marevita.databinding.EstatisticasPerfilBinding
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class EstatisticasPerfilFragment(private val userName: String) : Fragment() {

    private lateinit var binding: EstatisticasPerfilBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = EstatisticasPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        StatisticsAPI().getStatistics(userName) { success, statistics, response ->
            if (!isAdded) return@getStatistics
            activity?.runOnUiThread {
                if (success && statistics != null) {
                    setStatistics(statistics)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Erro de conexión: ${response}.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        binding.fishScroll.viewTreeObserver.addOnGlobalLayoutListener {
            val maxHeight = 270 * Resources.getSystem().displayMetrics.density
            if (binding.fishes.height > maxHeight) {
                binding.fishScroll.layoutParams.height = maxHeight.toInt()
                binding.fishScroll.requestLayout()
            } else {
                binding.fishScroll.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                binding.fishScroll.requestLayout()
            }
        }

        binding.baitsScroll.viewTreeObserver.addOnGlobalLayoutListener {
            val maxHeight = 270 * Resources.getSystem().displayMetrics.density
            if (binding.baits.height > maxHeight) {
                binding.baitsScroll.layoutParams.height = maxHeight.toInt()
                binding.baitsScroll.requestLayout()
            } else {
                binding.baitsScroll.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                binding.baitsScroll.requestLayout()
            }
        }

        binding.locationsScroll.viewTreeObserver.addOnGlobalLayoutListener {
            val maxHeight = 270 * Resources.getSystem().displayMetrics.density
            if (binding.locations.height > maxHeight) {
                binding.locationsScroll.layoutParams.height = maxHeight.toInt()
                binding.locationsScroll.requestLayout()
            } else {
                binding.locationsScroll.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                binding.locationsScroll.requestLayout()
            }
        }

    }


    private fun setStatistics(statistics: Statistics) {
        binding.totalCaptures.text = statistics.totalCaptures.toString()
        binding.totalFish.text = statistics.totalFishCount.toString()
        binding.totalLocations.text = statistics.totalLocations.toString()


        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("gl"))

        CaptureAPI().getCapture(statistics.biggerCapture.id) { success, capture, response ->
            if (!isAdded) return@getCapture
            activity?.runOnUiThread {
                if (success && capture != null) {
                    var dateTime = ZonedDateTime.parse(capture.dateTime)
                    binding.biggerCapture.text =
                        "${capture.location} (${dateTime.format(formatter)})"
                    binding.biggerCapture.setOnClickListener {
                        CapturaPopUp(requireContext()).show(capture, 4)
                    }
                    binding.biggerCapture.apply {
                        setTextColor(context.getColor(R.color.dark_blue))
                        paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Erro de conexión: ${response}.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        CaptureAPI().getCapture(statistics.diverseCapture.id) { success, capture, response ->
            if (!isAdded) return@getCapture
            activity?.runOnUiThread {
                if (success && capture != null) {
                    var dateTime = ZonedDateTime.parse(capture.dateTime)
                    binding.diverseCapture.text =
                        "${capture.location} (${dateTime.format(formatter)})"
                    binding.diverseCapture.setOnClickListener {
                        CapturaPopUp(requireContext()).show(capture, 4)
                    }
                    binding.diverseCapture.apply {
                        setTextColor(context.getColor(R.color.dark_blue))
                        paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Erro de conexión: ${response}.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        CaptureAPI().getCapture(statistics.likedCapture.id) { success, capture, response ->
            if (!isAdded) return@getCapture
            activity?.runOnUiThread {
                if (success && capture != null) {
                    var dateTime = ZonedDateTime.parse(capture.dateTime)
                    binding.likedCapture.text =
                        "${capture.location} (${dateTime.format(formatter)})"
                    binding.likedCapture.setOnClickListener {
                        CapturaPopUp(requireContext()).show(capture, 4)
                    }
                    binding.likedCapture.apply {
                        setTextColor(context.getColor(R.color.dark_blue))
                        paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Erro de conexión: ${response}.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        fillFishList(
            binding.fishes,
            statistics.fishes
        )
        fillList(
            binding.baits,
            statistics.baits.map { b -> b.toGrupedFishList() }.toList(),
            "usado"
        )
        fillList(
            binding.locations,
            statistics.locations.map { l -> l.toGrupedFishList() }.toList(),
            "visitado"
        )

    }

    private fun fillList(list: LinearLayout, groups: List<GrupedFishList>, union: String) {
        val inflater = LayoutInflater.from(activity)

        for (group in groups.sortedByDescending { it.times }) {
            var groupView = inflater.inflate(R.layout.lista_stats_container, list, false)

            val nomeTextView = groupView.findViewById<TextView>(R.id.nome)

            val text = "${group.name} ($union ${group.times} veces)"
            val spannable = SpannableString(text)


            val start = group.name.length
            val end = text.length

            val color = ContextCompat.getColor(requireContext(), R.color.light_grey)
            spannable.setSpan(
                ForegroundColorSpan(color),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            nomeTextView.text = spannable
            val container = groupView.findViewById<LinearLayout>(R.id.container)
            fillFishList(container, group.fishes)

            val buton = groupView.findViewById<ImageButton>(R.id.toggle)
            buton.setOnClickListener {
                if (container.visibility == View.GONE) {
                    container.visibility = View.VISIBLE
                    buton.setImageResource(R.drawable.up)
                    groupView.setBackgroundColor(requireContext().getColor(R.color.light_blue))
                } else {
                    container.visibility = View.GONE
                    buton.setImageResource(R.drawable.down)
                    groupView.setBackgroundColor(requireContext().getColor(R.color.transparent))
                }
            }

            list.addView(groupView)
        }
    }

    private fun fillFishList(container: LinearLayout, fishes: List<FishCount>) {
        val inflater = LayoutInflater.from(activity)

        for (fish in fishes.sortedByDescending { it.number }) {
            var row = inflater.inflate(R.layout.lista_stats_peixes, container, false)

            row.findViewById<TextView>(R.id.nomePeixe).text = fish.name
            row.findViewById<TextView>(R.id.quantity).text = fish.number.toString()

            container.addView(row)
        }
    }
}


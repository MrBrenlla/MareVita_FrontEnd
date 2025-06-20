package gal.marevita

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import gal.marevita.databinding.HistoricoBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

class HistoricoFragment(
) : Fragment() {

    private lateinit var binding: HistoricoBinding

    private var images = mutableMapOf<String, Bitmap>()

    private var job: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = HistoricoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        refreshList()
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    override fun onPause() {
        super.onPause()
        (requireActivity() as MenuActivity).endThis {
            job?.cancel()
            job = null
            binding.capturesList.removeAllViews()
        }

    }

    fun refreshList() {
        binding.capturesList.removeAllViews()

        CaptureAPI().getCaptures("/capture") { b, list, message ->
            if (!isAdded) return@getCaptures
            activity?.runOnUiThread {
                if (!b) {
                    Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
                    Log.e("HistoricoFragment", message ?: "")
                }

                if (list == null) return@runOnUiThread
                job?.cancel()
                binding.capturesList.removeAllViews()
                job = lifecycleScope.launch {
                    fillList(list)
                }
            }
        }

    }

    private suspend fun fillList(list: List<Capture>){

        val container = binding.capturesList
        val inflater = LayoutInflater.from(activity)

        val aux =list.sortedByDescending { capture ->
            capture.dateTime?.let {
                try {
                    ZonedDateTime.parse(it)
                } catch (e: Exception) {
                    ZonedDateTime.parse("1970-01-01T00:00:00Z")
                }
            } ?: ZonedDateTime.parse("1970-01-01T00:00:00Z")
        }
        for( c in aux){

            delay(400)

            val vista = inflater.inflate(R.layout.lista_capturas, container, false)
            vista.findViewById<TextView>(R.id.nomePeixe).text = c.fishStringList()
            vista.findViewById<TextView>(R.id.location).text = c.location
            vista.findViewById<TextView>(R.id.date).text =
                ZonedDateTime.parse(c.dateTime).toLocalDate().toString()

            if (!c.images.isEmpty()) {

                if (images.containsKey(c.images[0])) vista.findViewById<ImageView>(R.id.image)
                    .setImageBitmap(images[c.images[0]])

                ImageAPI(requireContext()).getImage("/images/" + c.owner + "/" + c.images[0]) { success, image, message ->
                    if (!isAdded) return@getImage
                    activity?.runOnUiThread {
                        if (success && image != null) {
                            images[c.images[0]] = image
                            vista.findViewById<ImageView>(R.id.image).setImageBitmap(image)
                        }
                    }
                }
            }

            if (!isAdded) return
            val anim = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
            vista.startAnimation(anim)

            container.addView(vista)

            vista.setOnClickListener {
                CapturaPopUp(requireContext()) { c ->
                    if (c == null) refreshList()
                }.fromId(c.id!!)
            }
        }
    }

}

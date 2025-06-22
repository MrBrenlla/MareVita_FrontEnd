package gal.marevita

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import gal.marevita.databinding.AlertasBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AlertasFragment(
) : Fragment() {

    private lateinit var binding: AlertasBinding


    private var job: Job? = null

    private var filtered = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = AlertasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.newAlertButton.setOnClickListener {
            val intent = Intent(requireContext(), NovaAlertaActivity::class.java)
            requireContext().startActivity(intent)
        }

        binding.filter.setOnClickListener {
            if (filtered) {
                binding.filter.setImageResource(R.drawable.filter)
                binding.alertsList.children.forEach { v ->
                    if (v.tag == "inactive") v.visibility = View.VISIBLE
                }
                filtered = false
            }else{
                binding.filter.setImageResource(R.drawable.not_filter)
                binding.alertsList.children.forEach { v ->
                    if (v.tag == "inactive") v.visibility = View.GONE
                }
                filtered = true
            }
        }

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
            binding.alertsList.removeAllViews()
        }
    }

    fun refreshList() {
        binding.alertsList.removeAllViews()

        AlertAPI().getAlerts() { b, list, message ->
            if (!isAdded) return@getAlerts
            activity?.runOnUiThread {
                if (!b) {
                    Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
                    Log.e("AlertFragment", message ?: "")
                    return@runOnUiThread
                }

                if (list == null) return@runOnUiThread
                job?.cancel()
                binding.alertsList.removeAllViews()
                job = lifecycleScope.launch {
                    fillList(list)
                }
            }
        }
    }

    private suspend fun fillList(list: List<Alert>){

        val container = binding.alertsList
        val inflater = LayoutInflater.from(activity)

        list.sortedByDescending { alert -> alert.name ?: "" }
            .forEach { a ->

                delay(400)

                val vista = inflater.inflate(R.layout.lista_alertas, container, false)
                vista.findViewById<TextView>(R.id.nomeAlerta).text = a.name
                vista.findViewById<TextView>(R.id.nomePeixe).text = a.fishStringList()
                vista.findViewById<TextView>(R.id.location).text = a.location
                if (a.activated.isNotEmpty()) vista.findViewById<ImageView>(R.id.active).visibility = View.VISIBLE
                else vista.tag = "inactive"

                if(!isAdded)return
                val anim = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
                vista.startAnimation(anim)


                container.addView(vista)

                vista.setOnClickListener {
                    AlertaPopUp(requireContext()) {
                        refreshList()
                    }.fromId(a.id!!)
                }
            }
    }
}

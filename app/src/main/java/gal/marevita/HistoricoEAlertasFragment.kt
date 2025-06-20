package gal.marevita

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import gal.marevita.databinding.HistoricoEAlertasBinding

class HistoricoEAlertasPagerAdapter(
    fragment: Fragment,
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HistoricoFragment()
            1 -> AlertasFragment()
            else -> throw IllegalArgumentException("Fragmento non válido")
        }
    }
}


class HistoricoEAlertasFragment(
    val target: String? = null
) : Fragment() {

    private var _binding: HistoricoEAlertasBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = HistoricoEAlertasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        val adapter = HistoricoEAlertasPagerAdapter(this)
        binding.viewPager.adapter = adapter

        when(target) {
            "alertas" -> {binding.viewPager.currentItem = 1
                binding.alertsButton.isChecked = true}
            else -> binding.historicButton.isChecked = true
        }

        binding.historicButton.setOnClickListener {
            if (binding.historicButton.isChecked) return@setOnClickListener
            binding.viewPager.currentItem = 0
            binding.historicButton.isChecked = true
            binding.alertsButton.isChecked = false
        }

        binding.alertsButton.setOnClickListener {
            if (binding.alertsButton.isChecked) return@setOnClickListener
            binding.viewPager.currentItem = 1
            binding.historicButton.isChecked = false
            binding.alertsButton.isChecked = true
        }

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.historicButton.isChecked = position == 0
                binding.alertsButton.isChecked = position == 1
            }
        })


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

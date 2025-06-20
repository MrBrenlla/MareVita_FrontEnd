package gal.marevita

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import gal.marevita.databinding.SocialBinding

class SocialPagerAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> AmigosFragment()
            1 -> PeticionsFragment()
            else -> throw IllegalArgumentException("Fragmento non válido")
        }
    }
}


class SocialActivity : AppCompatActivity() {

    private lateinit var binding: SocialBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SocialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPager()
        setupListeners()
    }

    private fun setupViewPager() {
        val adapter = SocialPagerAdapter(this)
        binding.viewPager.adapter = adapter

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.historicButton.isChecked = (position == 0)
                binding.alertsButton.isChecked = (position == 1)
            }
        })
    }

    private fun setupListeners() {
        binding.historicButton.setOnClickListener {
            if (!binding.historicButton.isChecked) {
                binding.viewPager.currentItem = 0
            }
        }

        binding.alertsButton.setOnClickListener {
            if (!binding.alertsButton.isChecked) {
                binding.viewPager.currentItem = 1
            }
        }

        binding.historicButton.isChecked = true
        binding.alertsButton.isChecked = false
    }
}
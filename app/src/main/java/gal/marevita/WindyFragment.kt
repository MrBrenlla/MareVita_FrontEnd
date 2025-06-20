package gal.marevita

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import gal.marevita.databinding.WindyBinding

class WindyFragment(val layer: String) : Fragment() {

    private lateinit var binding: WindyBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = WindyBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.windy.settings.javaScriptEnabled = true
        if (layer == "pressure") binding.windy.loadUrl("https://embed.windy.com/embed2.html?lat=42&lon=-8&detailLat=42&detailLon=-8&width=650&height=450&zoom=7&type=map&overlay=$layer&pressure=true&menu=hidden")
        else binding.windy.loadUrl("https://embed.windy.com/embed2.html?lat=42&lon=-8&detailLat=42&detailLon=-8&width=650&height=450&zoom=7&type=map&overlay=$layer&menu=hidden&zoomControl=0")

    }


}
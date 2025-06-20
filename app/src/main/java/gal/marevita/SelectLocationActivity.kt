package gal.marevita

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import gal.marevita.databinding.SeleccionLocalizacionBinding

class SelectLocationActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: SeleccionLocalizacionBinding
    private lateinit var map: GoogleMap
    private var selectedLatLng: LatLng? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SeleccionLocalizacionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.btnConfirm.setOnClickListener {
            selectedLatLng?.let {
                val resultIntent = Intent().apply {
                    putExtra("lat", it.latitude)
                    putExtra("lng", it.longitude)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            }
            if (selectedLatLng == null)
                Toast.makeText(baseContext, "Debe seleccionar unha ubicación", Toast.LENGTH_LONG)
                    .show()
        }

        binding.btnUbicacionActual.setOnClickListener {
            getCurrentLocationAndMark()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Desactiva o botón de localización por defecto (círculo azul)
        map.uiSettings.isMyLocationButtonEnabled = false

        // Habilita o botón de localización personalizado
        enableMyLocation()

        map.setOnMapClickListener { latLng ->
            selectedLatLng = latLng
            map.clear()
            map.addMarker(MarkerOptions().position(latLng))
        }
    }


    private fun enableMyLocation() {
        if (hasLocationPermission()) {
            try {
                map.isMyLocationEnabled = true
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        } else {
            requestLocationPermission()
        }
    }

    private fun getCurrentLocationAndMark() {
        if (!hasLocationPermission()) {
            requestLocationPermission()
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    val latLng = LatLng(it.latitude, it.longitude)
                    selectedLatLng = latLng
                    map.clear()
                    map.addMarker(MarkerOptions().position(latLng).title("Ubicación actual"))
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation()
            }
        }
    }
}

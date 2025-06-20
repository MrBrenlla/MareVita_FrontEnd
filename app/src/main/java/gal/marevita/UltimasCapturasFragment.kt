package gal.marevita


import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager
import gal.marevita.databinding.UltimasCapturasBinding
import java.time.ZonedDateTime

class CaptureClusterItem(
    private val position: LatLng,
    private val title: String,
    private val color: Int
) : ClusterItem {
    override fun getPosition() = position
    override fun getTitle() = title
    override fun getSnippet() = null

    override fun getZIndex(): Float {
        return 0f
    }

    fun getColor() = color
}

class UltimasCapturasFragment() : Fragment(), OnMapReadyCallback {

    private lateinit var binding: UltimasCapturasBinding
    private lateinit var mMap: GoogleMap
    private lateinit var clusterManager: ClusterManager<CaptureClusterItem>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = UltimasCapturasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapFragment =
            childFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        clusterManager = ClusterManager(requireContext(), mMap)
        clusterManager.algorithm = CenteredClusterAlgorithm<CaptureClusterItem>(
            10000
        )

        clusterManager.renderer = CustomClusterRenderer(requireContext(), mMap, clusterManager)

        clusterManager.setOnClusterClickListener { cluster ->
            val builder = LatLngBounds.Builder()
            cluster.items.forEach { builder.include(it.position) }
            try {
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100))
            } catch (e: Exception) {
                e.printStackTrace()
            }
            true
        }

        clusterManager.setOnClusterItemClickListener { item ->
            if (!isAdded) return@setOnClusterItemClickListener false
            activity?.runOnUiThread {
                CapturaPopUp(requireContext()).fromId(item.title, 4)
            }
            true
        }

        mMap.setOnCameraIdleListener(clusterManager)
        mMap.setOnMarkerClickListener(clusterManager)

        CaptureAPI().getCaptures("/latest-captures") { success, captures, error ->
            if (!isAdded) return@getCaptures
            activity?.runOnUiThread {
                if (!success || captures == null) {
                    Toast.makeText(
                        requireContext(),
                        "Erro: ${error ?: "Descoñecido"}",
                        Toast.LENGTH_LONG
                    ).show()
                    return@runOnUiThread
                }

                clusterManager.clearItems()
                captures.forEach { c ->
                    c.gpsLocation?.let { loc ->
                        val item = CaptureClusterItem(
                            position = LatLng(loc.latitude, loc.longitude),
                            title = c.id ?: "",
                            color = calcColor(c.dateTime ?: "")
                        )
                        clusterManager.addItem(item)
                    }
                }
                clusterManager.cluster()
            }
        }

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(42.5751, -7.9056), 7f))
    }


    private fun calcColor(date: String): Int {
        val maxAgeHours = 7L * 24L
        val now = ZonedDateTime.now()
        val ageHours = java.time.Duration.between(ZonedDateTime.parse(date), now).toHours()
            .coerceAtLeast(0).coerceAtMost(maxAgeHours)

        val fraction = ageHours.toFloat() / maxAgeHours

        val startColor = ContextCompat.getColor(requireContext(), R.color.full_red)
        val middleColor = ContextCompat.getColor(requireContext(), R.color.orange)
        val endColor = ContextCompat.getColor(requireContext(), R.color.green)

        return if (fraction < 0.5f) {
            val localFraction = fraction / 0.5f
            interpolateColor(startColor, middleColor, localFraction)
        } else {
            val localFraction = (fraction - 0.5f) / 0.5f
            interpolateColor(middleColor, endColor, localFraction)
        }
    }

    private fun interpolateColor(colorStart: Int, colorEnd: Int, fraction: Float): Int {
        val r = Color.red(colorStart) + ((Color.red(colorEnd) - Color.red(colorStart)) * fraction)
        val g =
            Color.green(colorStart) + ((Color.green(colorEnd) - Color.green(colorStart)) * fraction)
        val b =
            Color.blue(colorStart) + ((Color.blue(colorEnd) - Color.blue(colorStart)) * fraction)
        return Color.rgb(r.toInt(), g.toInt(), b.toInt())
    }

    private fun createColoredMarkerIcon(color: Int, sizePx: Int = 100): BitmapDescriptor {
        val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.marker)!!.mutate()
        drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN)

        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(canvas)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}
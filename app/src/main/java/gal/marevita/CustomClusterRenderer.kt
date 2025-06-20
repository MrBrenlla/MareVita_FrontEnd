package gal.marevita

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer

class CustomClusterRenderer(
    val context: Context,
    val map: GoogleMap,
    val clusterManager: ClusterManager<CaptureClusterItem>
) : DefaultClusterRenderer<CaptureClusterItem>(context, map, clusterManager) {

    override fun onBeforeClusterItemRendered(
        item: CaptureClusterItem,
        markerOptions: MarkerOptions
    ) {
        val icon = createColoredMarkerIcon(item.getColor())
        markerOptions.icon(icon).title(item.title)
    }

    override fun onClusterUpdated(cluster: Cluster<CaptureClusterItem>, marker: Marker) {
        val averageColor = calculateAverageColor(cluster.items.map { it.getColor() })
        val icon = createClusterIcon(averageColor, cluster.size)
        marker.setIcon(icon)
        marker.tag = null
    }

    override fun onBeforeClusterRendered(
        cluster: Cluster<CaptureClusterItem>,
        markerOptions: MarkerOptions
    ) {
        val averageColor = calculateAverageColor(cluster.items.map { it.getColor() })

        val icon = createClusterIcon(averageColor, cluster.size)

        markerOptions.icon(icon).anchor(0.5f, 0.5f)
    }

    private fun calculateAverageColor(colors: List<Int>): Int {
        if (colors.isEmpty()) return Color.BLACK

        var alpha = 0
        var red = 0
        var green = 0
        var blue = 0

        for (color in colors) {
            alpha += Color.alpha(color)
            red += Color.red(color)
            green += Color.green(color)
            blue += Color.blue(color)
        }

        val size = colors.size
        return Color.argb(
            alpha / size,
            red / size,
            green / size,
            blue / size
        )
    }

    private fun createClusterIcon(color: Int, count: Int, sizePx: Int = 100): BitmapDescriptor {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = color
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, paint)

        paint.color = Color.WHITE
        paint.textSize = sizePx * 0.3f
        paint.textAlign = Paint.Align.CENTER
        val yPos = sizePx / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(count.toString(), sizePx / 2f, yPos, paint)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }


    private fun createColoredMarkerIcon(color: Int, sizePx: Int = 100): BitmapDescriptor {
        val drawable = ContextCompat.getDrawable(context, R.drawable.marker)!!.mutate()
        drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN)

        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(canvas)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    override fun shouldRenderAsCluster(cluster: Cluster<CaptureClusterItem>): Boolean {
        return cluster.size >= 2
    }

}

package gal.marevita

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

class ImageCarouselAdapter(
    private val context: Context,
    private val images: List<String>,
    private val userNamer: String,
) : RecyclerView.Adapter<ImageCarouselAdapter.ImageViewHolder>() {

    private val aspectRatios = mutableMapOf<Int, Float>()

    var onImageLoaded: ((position: Int) -> Unit)? = null

    inner class ImageViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val imageView = LayoutInflater.from(parent.context)
            .inflate(R.layout.image_carrusel, parent, false) as ImageView
        imageView.adjustViewBounds = true
        imageView.scaleType = ImageView.ScaleType.FIT_START
        return ImageViewHolder(imageView)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val endpoint = "/images/$userNamer/${images[position]}"

        onImageLoaded?.invoke(position)

        Glide.with(holder.imageView.context)
            .asGif()
            .load(R.drawable.waiting_fish)
            .into(object : CustomTarget<GifDrawable>() {
                override fun onResourceReady(
                    resource: GifDrawable,
                    transition: Transition<in GifDrawable>?
                ) {
                    holder.imageView.setImageDrawable(resource)
                    resource.start()

                    val position = holder.bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val w = resource.intrinsicWidth
                        val h = resource.intrinsicHeight
                        if (w > 0 && h > 0) {
                            aspectRatios[position] = w.toFloat() / h.toFloat()
                        }
                        onImageLoaded?.invoke(position)
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // Limpar recursos se é necesario
                }
            })


        ImageAPI(context).getImage(endpoint) { success, image, message ->
            if (context is Activity && !context.isFinishing && !context.isDestroyed)
                context.runOnUiThread {
                    if (!success) {
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        Log.e("ImageCarouselAdapter", message ?: "")
                    } else {
                        if (!context.isFinishing && !context.isDestroyed) {
                            Glide.with(holder.imageView.context)
                                .load(image)
                                .into(object : CustomTarget<Drawable>() {
                                    override fun onResourceReady(
                                        resource: Drawable,
                                        transition: Transition<in Drawable>?
                                    ) {
                                        holder.imageView.setImageDrawable(resource)

                                        val currentPosition = holder.bindingAdapterPosition
                                        if (currentPosition != RecyclerView.NO_POSITION) {
                                            val w = resource.intrinsicWidth
                                            val h = resource.intrinsicHeight
                                            if (w > 0 && h > 0) {
                                                aspectRatios[currentPosition] =
                                                    w.toFloat() / h.toFloat()
                                                onImageLoaded?.invoke(currentPosition)
                                            }
                                        }
                                    }

                                    override fun onLoadCleared(placeholder: Drawable?) {}
                                })
                        }
                    }
                }
        }
    }

    override fun getItemCount(): Int = images.size

    fun getAspectRatio(position: Int): Float? = aspectRatios[position]
}
package gal.marevita

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator
import gal.marevita.databinding.FeedFiltradoBinding
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class FeedFiltradoActivity : AppCompatActivity() {

    private lateinit var binding: FeedFiltradoBinding
    val url = AppConfig.BASE_URL
    private var profilePics: MutableMap<String, Pair<Bitmap?, Boolean>> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FeedFiltradoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val capturesJson = intent.getStringExtra("captures_json")
        val captures = capturesJson?.let {
            Json.decodeFromString(ListSerializer(Capture.serializer()), it)
        }.orEmpty()

        val target = intent.getStringExtra("target") ?: ""

        setFeed(captures, target)
    }

    fun setFeed(captures: List<Capture>, target: String) {
        val container = binding.scrollContent
        val inflater = LayoutInflater.from(this@FeedFiltradoActivity)
        container.removeAllViews()

        var targetView: View? = null

        var imagesToLoad = 0
        var imagesLoaded = 0

        for (c in captures) {
            if (c.id == target) break
            if (c.images.isNotEmpty() && c.owner != null) {
                imagesToLoad++
            }
        }

        fun checkAllImagesLoaded() {
            if (imagesLoaded >= imagesToLoad && targetView != null) {
                binding.scroll.post {
                    binding.scroll.smoothScrollTo(0, targetView!!.top)
                }
            }
        }

        for (capture in captures) {
            val post = inflater.inflate(R.layout.captura_feed, container, false)

            if (capture.id == target) {
                targetView = post
            }

            if (targetView == null) {
                setPost(post, capture) {
                    imagesLoaded++
                    checkAllImagesLoaded()
                }
            } else setPost(post, capture)

            container.addView(post)
        }

        if (imagesToLoad == 0 && targetView != null) {
            binding.scroll.post {
                binding.scroll.smoothScrollTo(0, targetView!!.top)
            }
        }
    }

    private fun setPost(post: View, capture: Capture, onFullyLoaded: (() -> Unit)? = null) {
        if (capture.images.isNotEmpty() && capture.owner != null) {
            setImageCarousel(post, capture.images, capture.owner, onFullyLoaded)
        } else {
            post.findViewById<ViewPager2>(R.id.imageCarousel).visibility = View.GONE
            post.findViewById<DotsIndicator>(R.id.dotsIndicator).visibility = View.GONE
        }

        post.findViewById<TextView>(R.id.userName).text = capture.owner
        post.findViewById<TextView>(R.id.userName).setOnClickListener {
            val intent = Intent(this@FeedFiltradoActivity, PerfilActivity::class.java)
            intent.putExtra("userName", capture.owner)
            startActivity(intent)
        }
        post.findViewById<ImageView>(R.id.profilePic).setOnClickListener {
            val intent = Intent(this@FeedFiltradoActivity, PerfilActivity::class.java)
            intent.putExtra("userName", capture.owner)
            startActivity(intent)
        }
        if (capture.owner != null) setProfilePic(post, capture.owner)
        post.findViewById<TextView>(R.id.coment).text = capture.imageCaption
        post.findViewById<TextView>(R.id.location).text = capture.location

        post.findViewById<TextView>(R.id.date).text = capture.dateTime?.let {
            val dateTime = ZonedDateTime.parse(it)
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm (z)", Locale("gl"))
            dateTime.format(formatter)
        } ?: "N/A"

        post.findViewById<ImageView>(R.id.info).setOnClickListener {
            if (!isFinishing && !isDestroyed)
                runOnUiThread {
                    if (capture.id != null)
                        CapturaPopUp(this@FeedFiltradoActivity).fromId(capture.id, 4)
                    else
                        Toast.makeText(
                            this@FeedFiltradoActivity,
                            "Información non dispoñible",
                            Toast.LENGTH_SHORT
                        ).show()
                }
        }

        setLikes(post, capture)
        post.findViewById<ImageView>(R.id.like).setOnClickListener {
            CaptureAPI().likeCapture(capture.id!!) { b, newCapture, message ->
                if (!isFinishing && !isDestroyed)
                    runOnUiThread {
                        if (!b) {
                            Toast.makeText(this@FeedFiltradoActivity, message, Toast.LENGTH_LONG)
                                .show()
                            Log.e("Feed", message ?: "")
                            return@runOnUiThread
                        }
                        if (newCapture == null) return@runOnUiThread
                        setLikes(post, newCapture)
                    }
            }
        }
    }

    private fun setImageCarousel(
        post: View,
        images: List<String>,
        userName: String,
        onLoaded: (() -> Unit)? = null
    ) {
        val carousel = post.findViewById<ViewPager2>(R.id.imageCarousel)
        val dotsIndicator = post.findViewById<DotsIndicator>(R.id.dotsIndicator)

        val adapter = ImageCarouselAdapter(this@FeedFiltradoActivity, images, userName)

        fun adjustCarouselHeight(position: Int) {
            val aspectRatio = adapter.getAspectRatio(position)
            if (aspectRatio != null && carousel.width > 0) {
                val newHeight = (carousel.width / aspectRatio).toInt()
                val params = carousel.layoutParams
                if (params.height != newHeight) {
                    params.height = newHeight
                    carousel.layoutParams = params
                    dotsIndicator.setViewPager2(carousel)
                }
            }
        }

        adapter.onImageLoaded = { position ->
            adjustCarouselHeight(position)
            if (position == 0) {
                onLoaded?.invoke()
            }
        }

        carousel.adapter = adapter
        dotsIndicator.setViewPager2(carousel)

        carousel.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                adjustCarouselHeight(position)
            }
        })

        carousel.post {
            if (images.isNotEmpty()) {
                adjustCarouselHeight(0)
                if (adapter.getAspectRatio(0) == null) {
                    carousel.postDelayed({
                        adjustCarouselHeight(0)
                    }, 300)
                }
            }
        }
    }

    private fun setProfilePic(post: View, userName: String) {
        val (pic, searched) = profilePics[userName] ?: Pair(null, false)
        if (!searched) {
            ImageAPI(this@FeedFiltradoActivity).getImage("/profile/pic/$userName") { b, image, _ ->
                if (!isFinishing && !isDestroyed)
                    runOnUiThread {
                        if (b && image != null) {
                            profilePics[userName] = Pair(image, true)
                            post.findViewById<ImageView>(R.id.profilePic).setImageBitmap(image)
                        } else {
                            profilePics[userName] = Pair(null, false)
                        }
                    }
            }
        } else if (pic != null) {
            post.findViewById<ImageView>(R.id.profilePic).setImageBitmap(pic)
        }
    }

    private fun setLikes(post: View, capture: Capture) {
        val likes = capture.likes
        post.findViewById<TextView>(R.id.likeNum).text = likes.size.toString()
        val like = post.findViewById<ImageView>(R.id.like)
        val username = PreferencesManager.getInstance(this@FeedFiltradoActivity).getUsername()
        if (likes.contains(username)) {
            like.setImageResource(R.drawable.heart)
        } else {
            like.setImageResource(R.drawable.heartoutline)
        }
    }
}

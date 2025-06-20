package gal.marevita

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator
import gal.marevita.databinding.FeedBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class FeedFragment(
) : Fragment() {

    private lateinit var binding: FeedBinding

    private var profilePics: MutableMap<String, Pair<Bitmap?, Boolean>> = mutableMapOf()

    private var isBarVisible = true
    private var lastScrollY = 0
    private var topBarHeight = 0
    private var currentAnimator: ValueAnimator? = null

    private var lastAnimationTime = 0L
    private val animationDebounceMs = 100L
    private val minDyToTrigger = 10

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setTopBar()
        binding.social.setOnClickListener {
            val intent = Intent(requireContext(), SocialActivity::class.java)
            startActivity(intent)
        }

    }

    override fun onResume() {
        super.onResume()
        setFeed()
    }

    override fun onPause() {
        super.onPause()
        (requireActivity() as MenuActivity).endThis {binding.scrollContent.removeAllViews()}
    }


    fun setTopBar() {
        binding.topBar.post {
            topBarHeight = binding.topBar.height
        }

        binding.scroll.viewTreeObserver.addOnScrollChangedListener {
            val currentScrollY = binding.scroll.scrollY
            val dy = currentScrollY - lastScrollY

            val now = System.currentTimeMillis()

            if (currentAnimator?.isRunning == true) {
                lastScrollY = currentScrollY
                return@addOnScrollChangedListener
            }

            if (kotlin.math.abs(dy) > minDyToTrigger && (now - lastAnimationTime) > animationDebounceMs) {
                if (dy > 0 && isBarVisible) {
                    animateTopBar(false)
                    lastAnimationTime = now
                } else if (dy < 0 && !isBarVisible) {
                    animateTopBar(true)
                    lastAnimationTime = now
                }
            }

            lastScrollY = currentScrollY
        }
    }

    private fun animateTopBar(show: Boolean) {
        if (isBarVisible == show) return

        currentAnimator?.cancel()

        val startHeight = if (show) 1 else topBarHeight
        val endHeight = if (show) topBarHeight else 1

        if (show) {
            binding.topBar.visibility = View.VISIBLE
        }

        currentAnimator = ValueAnimator.ofInt(startHeight, endHeight).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            addUpdateListener { valueAnimator ->
                val value = valueAnimator.animatedValue as Int
                val lp = binding.topBar.layoutParams
                lp.height = value
                binding.topBar.layoutParams = lp
            }
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationEnd(animation: Animator) {
                    if (!show) {
                        binding.topBar.visibility = View.GONE
                    }
                }

                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })
            start()
        }

        isBarVisible = show
    }

    fun setFeed() {

        profilePics = mutableMapOf()

        CaptureAPI().getCaptures("/capture/friends") { b, list, message ->
            if (!isAdded) return@getCaptures
            activity?.runOnUiThread {
                if (!b) {
                    Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
                    Log.e("Feed", message ?: "")
                    return@runOnUiThread
                }
                binding.scrollContent.removeAllViews()
                if (list == null) return@runOnUiThread

                lifecycleScope.launch {
                    setPosts(list)
                }

            }
        }
    }

    private suspend fun setPosts(list: List<Capture>) {
        val container = binding.scrollContent
        val inflater = LayoutInflater.from(activity)

        for (capture in list) {

            delay(400)

            val post = inflater.inflate(R.layout.captura_feed, container, false)
            if (capture.images.isNotEmpty() && capture.owner != null) setImageCarousel(
                post,
                capture.images,
                capture.owner
            )
            else {
                post.findViewById<ViewPager2>(R.id.imageCarousel).visibility = View.GONE
                post.findViewById<DotsIndicator>(R.id.dotsIndicator).visibility = View.GONE
            }
            post.findViewById<TextView>(R.id.userName).text = capture.owner
            post.findViewById<TextView>(R.id.userName).setOnClickListener {
                val intent = Intent(context, PerfilActivity::class.java)
                intent.putExtra("userName", capture.owner)
                startActivity(intent)
            }
            post.findViewById<ImageView>(R.id.profilePic).setOnClickListener {
                val intent = Intent(context, PerfilActivity::class.java)
                intent.putExtra("userName", capture.owner)
                startActivity(intent)
            }
            if (capture.owner != null) setProfilePic(post, capture.owner)
            if (capture.imageCaption != null) post.findViewById<TextView>(R.id.coment).text =
                capture.imageCaption
            else post.findViewById<TextView>(R.id.coment).visibility = View.GONE
            post.findViewById<TextView>(R.id.location).text = capture.location

            if (capture.dateTime == null) post.findViewById<TextView>(R.id.date).text = "N/A"
            else {
                var dateTime = ZonedDateTime.parse(capture.dateTime)
                val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm (z)", Locale("gl"))
                post.findViewById<TextView>(R.id.date).text = dateTime.format(formatter)
            }

            post.findViewById<ImageView>(R.id.info).setOnClickListener {
                if (!isAdded) return@setOnClickListener
                activity?.runOnUiThread {
                    if (capture.id != null) CapturaPopUp(requireContext()).fromId(capture.id, 4)
                    else Toast.makeText(context, "Información non dispoñible", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            setLikes(post, capture)
            post.findViewById<ImageView>(R.id.like).setOnClickListener {
                CaptureAPI().likeCapture(capture.id!!) { b, capture, message ->
                    if (!isAdded) return@likeCapture
                    activity?.runOnUiThread {
                        if (!b) {
                            Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
                            Log.e("Feed", message ?: "")
                            return@runOnUiThread
                        }
                        if (capture == null) return@runOnUiThread
                        setLikes(post, capture)
                    }
                }
            }

            if(!isAdded)return
            val anim = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
            post.startAnimation(anim)

            container.addView(post)
        }
    }


    private fun setImageCarousel(post: View, images: List<String>, userName: String) {
        val carousel = post.findViewById<ViewPager2>(R.id.imageCarousel)
        val dotsIndicator = post.findViewById<DotsIndicator>(R.id.dotsIndicator)

        val adapter = ImageCarouselAdapter(requireContext(), images, userName)

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
            if (position == carousel.currentItem) {
                adjustCarouselHeight(position)
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
        var (pic, searched) = profilePics[userName] ?: Pair(null, false)
        if (!searched) {
            ImageAPI(requireContext()).getImage("/profile/pic/$userName") { b, pic, message ->
                if (!isAdded) return@getImage
                activity?.runOnUiThread {
                    if (!b) {
                        profilePics[userName] = Pair(null, false)
                    }
                    if (pic != null) {
                        profilePics[userName] = Pair(pic, true)
                        post.findViewById<ImageView>(R.id.profilePic).setImageBitmap(pic)
                    } else {
                        profilePics[userName] = Pair(null, false)
                    }
                }
            }
        } else if (pic != null) post.findViewById<ImageView>(R.id.profilePic).setImageBitmap(pic)

    }

    fun setLikes(post: View, capture: Capture) {
        val likes = capture.likes
        post.findViewById<TextView>(R.id.likeNum).text = likes.size.toString()

        val like = post.findViewById<ImageView>(R.id.like)
        if (likes.contains(PreferencesManager.getInstance(requireContext()).getUsername())) {
            like.setImageResource(R.drawable.heart)
        } else {
            like.setImageResource(R.drawable.heartoutline)
        }
    }

}
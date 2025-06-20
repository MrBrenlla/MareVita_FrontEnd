package gal.marevita

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import gal.marevita.databinding.AmigosBinding

class AmigosFragment : Fragment() {

    private lateinit var binding: AmigosBinding

    private var images = mutableMapOf<String, Bitmap>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = AmigosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.addFriendButton.setOnClickListener {
            SearchPopUp(requireContext()).mostrarBuscadorUsuarios()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    override fun onPause() {
        super.onPause()
        binding.friends.removeAllViews()
    }

    fun refreshList() {

        SocialAPI().getSocial { b, social, message ->
            if (!isAdded) return@getSocial
            activity?.runOnUiThread {
                if (!b) {
                    Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
                    Log.e("AmigosFragment", message ?: "")
                    return@runOnUiThread
                }

                val inflater = LayoutInflater.from(activity)

                if (social == null) return@runOnUiThread

                social.friends.sorted().forEach { f ->
                    add(binding.friends, inflater, f)
                }

            }
        }
    }

    private fun add(container: LinearLayout, inflater: LayoutInflater, friend: String) {

        val vista = inflater.inflate(R.layout.lista_usuarios, container, false)
        vista.findViewById<TextView>(R.id.userName).text = friend

        if (images.containsKey(friend)) vista.findViewById<ImageView>(R.id.profilePic)
            .setImageBitmap(images[friend])
        else ImageAPI(requireContext()).getImage("/profile/pic/" + friend) { success, image, message ->
            if (!isAdded) return@getImage
            activity?.runOnUiThread {
                if (success && image != null) {
                    images[friend] = image
                    vista.findViewById<ImageView>(R.id.profilePic).setImageBitmap(image)
                }
            }
        }

        vista.findViewById<ImageView>(R.id.action2).visibility = View.GONE

        val button = vista.findViewById<ImageView>(R.id.action1)
        button.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.user_remove))
        button.setOnClickListener {
            SocialAPI().removeFriend(friend) { b, _, message ->
                if (!isAdded) return@removeFriend
                activity?.runOnUiThread {
                    if (!b) {
                        Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
                        Log.e("AmigosFragment", message ?: "")
                        return@runOnUiThread
                    }
                    container.removeView(vista)
                }
            }
        }
        container.addView(vista)
    }
}

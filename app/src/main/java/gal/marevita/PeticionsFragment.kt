package gal.marevita

import android.content.Intent
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
import androidx.lifecycle.lifecycleScope
import gal.marevita.databinding.PeticionsBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PeticionsFragment : Fragment() {

    private lateinit var binding: PeticionsBinding

    private var images = mutableMapOf<String, Bitmap>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = PeticionsBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onResume() {
        super.onResume()
        refreshList()
    }

    override fun onPause() {
        super.onPause()
        lifecycleScope.launch {
            delay(200)
            binding.receivedPetitions.removeAllViews()
            binding.sendedPetitions.removeAllViews()        }

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

                social.friendPetitionsReceived.sorted().forEach { f ->
                    add(binding.receivedPetitions, inflater, f, true)
                }

                social.friendPetitionsSent.sorted().forEach { f ->
                    add(binding.sendedPetitions, inflater, f, false)
                }

            }
        }
    }

    private fun add(container: LinearLayout, inflater: LayoutInflater, friend: String, buttons : Boolean) {

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

        vista.findViewById<TextView>(R.id.userName).setOnClickListener {
            val intent = Intent(context, PerfilActivity::class.java)
            intent.putExtra("userName", friend)
            startActivity(intent)
        }
        vista.findViewById<ImageView>(R.id.profilePic).setOnClickListener {
            val intent = Intent(context, PerfilActivity::class.java)
            intent.putExtra("userName", friend)
            startActivity(intent)
        }

        if (!buttons) {
            vista.findViewById<ImageView>(R.id.action1).visibility = View.GONE
            vista.findViewById<ImageView>(R.id.action2).visibility = View.GONE
        }

        else {
            vista.findViewById<ImageView>(R.id.action1).setOnClickListener {
                SocialAPI().acceptFriendPetition(friend) { b, _, message ->
                    if (!isAdded) return@acceptFriendPetition
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
            vista.findViewById<ImageView>(R.id.action2).setOnClickListener {
                SocialAPI().declineFriendPetition(friend) { b, _, message ->
                    if (!isAdded) return@declineFriendPetition
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
        }
        container.addView(vista)
    }
}

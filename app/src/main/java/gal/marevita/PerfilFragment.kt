package gal.marevita

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import androidx.work.WorkManager
import gal.marevita.InicioActivity
import gal.marevita.databinding.PerfilBinding

class ProfilePagerAdapter(
    fragment: Fragment,
    private val userName: String
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> CapturasPerfilFragment(userName)
            1 -> EstatisticasPerfilFragment(userName)
            else -> throw IllegalArgumentException("Fragmento non válido")
        }
    }
}


class PerfilFragment(
    val imagePicker: ImagePicker? = null
) : Fragment() {

    private lateinit var binding: PerfilBinding

    private lateinit var userName: String

    private var pic: Bitmap? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        userName =
            requireActivity().intent.getStringExtra("userName") ?: PreferencesManager.getInstance(
                requireContext()
            ).getUsername()
        binding = PerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setProfile()

        val adapter = ProfilePagerAdapter(this, userName)
        binding.viewPager.adapter = adapter

        binding.capturesButton.setOnClickListener {
            if (binding.capturesButton.isChecked) return@setOnClickListener
            binding.viewPager.currentItem = 0
            binding.capturesButton.isChecked = true
            binding.statisticsButton.isChecked = false
        }

        binding.statisticsButton.setOnClickListener {
            if (binding.statisticsButton.isChecked) return@setOnClickListener
            binding.viewPager.currentItem = 1
            binding.capturesButton.isChecked = false
            binding.statisticsButton.isChecked = true
        }

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.capturesButton.isChecked = position == 0
                binding.statisticsButton.isChecked = position == 1
            }
        })

    }

    private fun setProfile() {
        setProfilePic()

        UserAPI().getUser(userName) { success, user, message ->
            if (!isAdded) return@getUser
            activity?.runOnUiThread {
                if (success && user != null) {
                    binding.userName.text = user.userName
                    binding.friends.text = user.friendsCount.toString()
                } else {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        if (userName == PreferencesManager.getInstance(requireContext())
                .getUsername()
        ) {
            binding.socialButton.visibility = View.GONE
            if(imagePicker!=null){
                binding.settings.visibility = View.VISIBLE
                setMenu()
            }
        } else SocialAPI().getSocial { success, social, message ->
            if (!isAdded) return@getSocial
            activity?.runOnUiThread {
                if (success && social != null) {
                    if (social.friends.contains(userName)) {
                        binding.socialButton.setImageResource(R.drawable.user_remove)
                        binding.socialButton.setOnClickListener { removeFriend() }
                    } else if (social.friendPetitionsReceived.contains(userName)) {
                        binding.socialButton.setImageResource(R.drawable.user_accept)
                        binding.socialButton.setOnClickListener { acceptFriendPetition() }
                    } else if (social.friendPetitionsSent.contains(userName)) {
                        binding.socialButton.setImageResource(R.drawable.user_add)
                        binding.socialButton.imageTintList = ColorStateList.valueOf(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.shadow_grey
                            )
                        )
                        binding.socialButton.backgroundTintList = ColorStateList.valueOf(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.shadow_grey20
                            )
                        )
                        binding.socialButton.setOnClickListener {
                            Toast.makeText(
                                requireContext(),
                                "Petición xa enviada",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        binding.socialButton.setImageResource(R.drawable.user_add)
                        binding.socialButton.setOnClickListener { addFriend() }
                    }
                }
            }
        }
    }

    private fun addFriend() {
        SocialAPI().sendFriendPetition(userName) { success, social, message ->
            if (!isAdded) return@sendFriendPetition
            activity?.runOnUiThread {
                if (success && social != null) {
                    binding.socialButton.isEnabled = false
                } else {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun removeFriend() {
        SocialAPI().removeFriend(userName) { success, social, message ->
            if (!isAdded) return@removeFriend
            activity?.runOnUiThread {
                if (success && social != null) {
                    binding.socialButton.setImageResource(R.drawable.user_add)
                    binding.socialButton.setOnClickListener { addFriend() }
                    binding.friends.text = (binding.friends.text.toString().toInt() - 1).toString()
                } else {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun acceptFriendPetition() {
        SocialAPI().acceptFriendPetition(userName) { success, social, message ->
            if (!isAdded) return@acceptFriendPetition
            activity?.runOnUiThread {
                if (success && social != null) {
                    binding.socialButton.setImageResource(R.drawable.user_remove)
                    binding.socialButton.setOnClickListener { removeFriend() }
                    binding.friends.text = (binding.friends.text.toString().toInt() + 1).toString()
                } else {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun setProfilePic() {
        ImageAPI(requireContext()).getImage("/profile/pic/$userName") { success, pic, message ->
            if (!isAdded) return@getImage
            if (success && pic != null)
                activity?.runOnUiThread {
                    this.pic = pic
                    binding.profilePic.setImageBitmap(pic)
                }
        }
    }

    private fun setMenu() {

        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

        binding.settings.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.END)
        }

        binding.menu.setNavigationItemSelectedListener { menuItem ->

            when (menuItem.itemId) {
                R.id.editProfile -> {
                    if (imagePicker == null) return@setNavigationItemSelectedListener true
                    EditarPerfilPopUp(requireContext(), pic, imagePicker).fromUserName(userName) { user, uri ->
                        if (user != null) {
                            binding.userName.text = user.userName
                            userName = user.userName
                            binding.viewPager.adapter = ProfilePagerAdapter(this@PerfilFragment, userName)
                        }
                        if (uri != null) {
                            binding.profilePic.setImageURI(uri)
                        }
                    }
                }
                R.id.changePassword -> {
                    EditarPasswordPopUp(requireContext()).setDialogoEditarPassword()
                }
                R.id.logout -> {
                    val preferences = PreferencesManager.getInstance(requireContext())
                    preferences.removePassword()
                    AppConfig.token = ""
                    val intent = Intent(requireContext(), InicioActivity::class.java)
                    startActivity(intent)
                    WorkManager.getInstance(requireContext()).cancelUniqueWork("check_every_two_days")
                    requireActivity().finish()
                }
            }

            binding.drawerLayout.closeDrawer(GravityCompat.END)
            true
        }
    }

}
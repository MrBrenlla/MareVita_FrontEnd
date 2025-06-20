package gal.marevita

import android.Manifest
import android.app.AlertDialog
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import java.io.File

class ImagePicker(
    private val activity: ComponentActivity,
    private var imagePickedCallback: (Uri) -> Unit = {}
) {


    private lateinit var photoUri: Uri

    private enum class Mode { CAMERA, GALLERY }

    private var pendingMode: Mode? = null

    private val permissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val mode = pendingMode
        pendingMode = null

        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            Toast.makeText(activity, "Permisos denegados", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }

        when (mode) {
            Mode.CAMERA -> launchCamera()
            Mode.GALLERY -> openGallery()
            null -> {}
        }
    }

    private val pickImageLauncher = activity.registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { imagePickedCallback(it) }
    }

    private val takePictureLauncher = activity.registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            imagePickedCallback(photoUri)
        }
    }

    fun startImagePicker(callback: ((Uri) -> Unit)? = null){
        if(callback != null) imagePickedCallback = callback
        val options = arrayOf("Sacar foto", "Seleccionar da galería")
        AlertDialog.Builder(activity)
            .setTitle("Elixe unha opción")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermission()
                    1 -> checkGalleryPermission()
                }
            }
            .show()
    }

    private fun checkCameraPermission() {
        pendingMode = Mode.CAMERA
        permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
    }

    private fun checkGalleryPermission() {
        pendingMode = Mode.GALLERY
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES))
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
        }
    }

    private fun openGallery() {
        pickImageLauncher.launch("image/*")
    }

    private fun launchCamera() {
        val photoFile = File.createTempFile("photo_", ".jpg", activity.cacheDir)
        photoUri = FileProvider.getUriForFile(
            activity,
            "${activity.packageName}.fileprovider",
            photoFile
        )
        takePictureLauncher.launch(photoUri)
    }
}
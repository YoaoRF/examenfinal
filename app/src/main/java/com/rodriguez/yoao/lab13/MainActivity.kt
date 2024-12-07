package com.rodriguez.yoao.lab13


import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.google.common.util.concurrent.ListenableFuture
import com.rodriguez.yoao.lab13.R.*
import com.rodriguez.yoao.lab13.R.id.*
import com.rodriguez.yoao.lab13.databinding.ActivityGalleryBinding
import com.rodriguez.yoao.lab13.databinding.ActivityMainBinding
import com.rodriguez.yoao.lab13.databinding.ListItemImgBinding
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


@SuppressLint("MissingInflatedId")
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraSelector: CameraSelector
    private var imageCapture: ImageCapture? = null
    private lateinit var imgCaptureExecutor: ExecutorService

    private val cameraPermissionResult =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { permissionGranted ->
            if (permissionGranted) {
                startCamera()
            } else {
                Snackbar.make(
                    binding.root, "The camera permission is necessary",
                    Snackbar.LENGTH_INDEFINITE
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraProviderFuture = ProcessCameraProvider.getInstance(context = this)
        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        imgCaptureExecutor = Executors.newSingleThreadExecutor()

        cameraPermissionResult.launch(android.Manifest.permission.CAMERA)

        binding.imgCaptureBtn.setOnClickListener { it: View ->
            takePhoto()
        }

        binding.switchBtn.setOnClickListener { it: View ->
            cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            startCamera()
        }

        binding.galleryBtn.setOnClickListener { it: View ->
            val intent = Intent(packageContext = this, GalleryActivity::class.java)
            startActivity(intent)
        }
    }

    private fun Intent(packageContext: MainActivity, java: Class<GalleryActivity>): Intent? {
        TODO("Not yet implemented")
    }


    private fun startCamera() {
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(binding.preview.surfaceProvider)
        }

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            imageCapture = ImageCapture.Builder().build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner = this,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Log.d(TAG, "Use case binding failed")
            }
        }, ContextCompat.getMainExecutor(this))
    }


    private fun takePhoto() {
        imageCapture?.let { it: ImageCapture ->
            val fileName = "JPEG_${System.currentTimeMillis()}"
            val file = File(externalMediaDirs[0], fileName)
            val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()

            it.takePicture(
                outputFileOptions,
                imgCaptureExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        Log.i(TAG, "The image has been saved in ${file.toUri()}")
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Toast.makeText(
                            binding.root.context, "Error taking photo",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.d(TAG, "Error taking photo: $exception")
                    }
                }
            )
        }
    }


    companion object {
        const val TAG = "MainActivity"
    }

    private fun enableEdgeToEdge() {
        // Configurar ajustes para un diseño de bordes (si aplica)
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }
}


class GalleryAdapter(private val fileArray: Array<File>) :
    RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

    class ViewHolder(private val binding: ListItemImgBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(file: File) {
            Glide.with(binding.root).load(file).into(binding.localImg)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(
            ListItemImgBinding.inflate(
                layoutInflater, parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(fileArray[position])
    }

    override fun getItemCount(): Int {
        return fileArray.size
    }
}
class GalleryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGalleryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val directory = File(externalMediaDirs[0].absolutePath)
        val files = directory.listFiles() as Array<File>

        val adapter = GalleryAdapter(files.reversedArray())
        binding.viewPager.adapter = adapter
    }
}

package com.sheev.sheev_vision

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.Gravity
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import com.sheev.sheev_vision.databinding.ActivityMainBinding
import com.sheev.sheev_vision.detection.LandmarkOverlayView
import com.sheev.sheev_vision.detection.ObjectDetectorProcessor
import com.sheev.sheev_vision.udp.UdpSocketListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : ComponentActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var udpListener: UdpSocketListener
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var landmarkOverlayView: LandmarkOverlayView
    // private lateinit var broadcastMsgAdapter: ArrayAdapter<String>

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
            startCamera()
        }

    private fun requestPermissions() {
        activityResultLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
        )
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        requestPermissions()

        cameraExecutor = Executors.newSingleThreadExecutor()

        // broadcastMsgAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, messages)
        // val listView: ListView = findViewById(R.id.broadcast_msg_view)
        // listView.adapter = broadcastMsgAdapter

        landmarkOverlayView = LandmarkOverlayView(this)
        val layoutParams = CoordinatorLayout.LayoutParams(720, 1280)
        layoutParams.gravity = Gravity.CENTER
        landmarkOverlayView.layoutParams = layoutParams
        view.addView(landmarkOverlayView)

        // 👂 Listen for UDP broadcasts
        startListening()

        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewContainer.surfaceProvider)
                }

            // 📸 Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            // 🕵️‍♀️ Setup object detection
            val options = PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                .build()

            // 🔎 Set image analyzer
            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(Size(720, 1280))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(
                        cameraExecutor,
                        ObjectDetectorProcessor(
                            options,
                            landmarkOverlayView,
                            Size(binding.previewContainer.width, binding.previewContainer.height)
                        )
                    )
                }

            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )
                camera.cameraControl.setLinearZoom(0f)
            } catch (exc: Exception) {
                Log.e(TAG, "Camera binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun startListening() {
        udpListener = UdpSocketListener(58266)

        // Create coroutine to handle network task
        lifecycleScope.launch {
            while (true) {
                val m = withContext(Dispatchers.IO) {
                    udpListener.receive()
                }

                // Automatically back on main thread
                if (m != null) {
                    Log.d(TAG, "new UDP message: $m")
                }
                // broadcastMsgAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        udpListener.stopListening()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "SheevVision"
    }
}
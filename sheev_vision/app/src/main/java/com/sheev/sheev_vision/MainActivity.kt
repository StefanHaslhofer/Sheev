package com.sheev.sheev_vision

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.sheev.sheev_vision.databinding.ActivityMainBinding
import com.sheev.sheev_vision.detection.BoundingBoxView
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
    private lateinit var boundingBoxView: BoundingBoxView
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

        boundingBoxView = BoundingBoxView(this)
        view.addView(boundingBoxView)

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
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewContainer.surfaceProvider)
                }

            // 📸 Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            // 🕵️‍♀️ Setup object detection
            val options = ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                .enableClassification()
                .build()

            // 🔎 Set image analyzer
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(
                        cameraExecutor,
                        ObjectDetectorProcessor(options, boundingBoxView)
                    )
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )
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
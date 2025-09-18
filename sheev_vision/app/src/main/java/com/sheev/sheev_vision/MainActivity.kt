package com.sheev.sheev_vision

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.ListView
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
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import com.sheev.sheev_vision.databinding.ActivityMainBinding
import com.sheev.sheev_vision.detection.ActionBorderOverlayView
import com.sheev.sheev_vision.detection.ActionBorderOverlayView.ActionBorder
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
    private lateinit var actionBorderOverlayView: ActionBorderOverlayView

    private lateinit var broadcastMsgAdapter: ArrayAdapter<String>
    private lateinit var usbManager: UsbManager
    private var usbDevice: UsbDevice? = null
    private var usbSerialDevice: UsbSerialDevice? = null
    private var usbConnection: UsbDeviceConnection? = null

    private var messages = mutableListOf<String>()


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

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        requestPermissions()

        usbManager = getSystemService(USB_SERVICE) as UsbManager
        val filter = IntentFilter()
        filter.addAction(ACTION_USB_PERMISSION)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED)
        registerReceiver(broadcastReceiver, filter)
        initUsbConnection()

        cameraExecutor = Executors.newSingleThreadExecutor()

        val layoutParams = CoordinatorLayout.LayoutParams(720, 1280)
        layoutParams.gravity = Gravity.CENTER

        landmarkOverlayView = LandmarkOverlayView(this)
        landmarkOverlayView.layoutParams = layoutParams
        view.addView(landmarkOverlayView)

        actionBorderOverlayView = ActionBorderOverlayView(this)
        actionBorderOverlayView.layoutParams = layoutParams
        view.addView(actionBorderOverlayView)

        // 👂 Listen for UDP broadcasts
        // startListening()

        startCamera()

        broadcastMsgAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, messages)
        val listView: ListView = findViewById(R.id.broadcast_msg_view)
        listView.adapter = broadcastMsgAdapter
    }

    fun sendData(input: String) {
        if (usbSerialDevice != null) {
            lifecycleScope.launch {
                usbSerialDevice?.write(input.toByteArray())
            }
        }
    }

    // Source: https://github.com/appsinthesky/Kotlin-Serial-Usb
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {

            messages.add(intent?.action!!)
            broadcastMsgAdapter.notifyDataSetChanged()

            if (intent?.action!! == ACTION_USB_PERMISSION) {
                val granted = intent.extras!!.getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED)
                if (granted) {
                    usbConnection = usbManager.openDevice(usbDevice)
                    usbSerialDevice =
                        UsbSerialDevice.createUsbSerialDevice(usbDevice, usbConnection)
                    // 🪛 Configure usb serial device
                    if (usbSerialDevice != null) {
                        if (usbSerialDevice!!.open()) {
                            usbSerialDevice!!.setBaudRate(9600)
                            usbSerialDevice!!.setDataBits(UsbSerialInterface.DATA_BITS_8)
                            usbSerialDevice!!.setStopBits(UsbSerialInterface.STOP_BITS_1)
                            usbSerialDevice!!.setParity(UsbSerialInterface.PARITY_NONE)
                            usbSerialDevice!!.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF)
                        } else {
                            Log.d(TAG, "serial port not open")
                            messages.add("serial port not open")
                            broadcastMsgAdapter.notifyDataSetChanged()
                        }
                    } else {
                        Log.d(TAG, "port is null")
                        messages.add("port is null")
                        broadcastMsgAdapter.notifyDataSetChanged()
                    }
                } else {
                    Log.d(TAG, "serial permission not granted")
                    messages.add("serial permission not granted")
                    broadcastMsgAdapter.notifyDataSetChanged()
                }
            } else if (intent.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
                initUsbConnection()
            } else if (intent.action == UsbManager.ACTION_USB_DEVICE_DETACHED) {
                disconnectUsbConnection()
            }
        }
    }

    @SuppressLint("MutableImplicitPendingIntent")
    private fun initUsbConnection() {
        val usbDevices: HashMap<String, UsbDevice>? = usbManager.deviceList

        if (!usbDevices?.isEmpty()!!) {
            Log.d(TAG, "USB devices recognized")
            messages.add("Usb Devices recognized")
            broadcastMsgAdapter.notifyDataSetChanged()

            var keep = true
            usbDevices.forEach { e ->
                usbDevice = e.value
                val deviceVendorId: Int? = usbDevice?.vendorId
                Log.d(TAG, "vendorId: ${deviceVendorId}")
                messages.add("vendorId: ${deviceVendorId}")
                broadcastMsgAdapter.notifyDataSetChanged()

                if (deviceVendorId != null) {
                    val intent: PendingIntent =
                        PendingIntent.getBroadcast(
                            this, 0, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_MUTABLE
                        )
                    usbManager.requestPermission(usbDevice, intent)
                    keep = false
                    Log.d(TAG, "connection successful")
                    messages.add("connection successful")
                    broadcastMsgAdapter.notifyDataSetChanged()
                } else {
                    usbConnection = null
                    usbDevice = null
                    Log.d(TAG, "unable to connect")
                    messages.add("unable to connect")
                    broadcastMsgAdapter.notifyDataSetChanged()
                }

                if (!keep) {
                    return
                }
            }
        } else {
            Log.d(TAG, "no usb device connected")
        }
    }

    private fun disconnectUsbConnection() {
        usbSerialDevice?.close()
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

            initActionBorders(
                binding.previewContainer.height.toFloat(),
                binding.previewContainer.width.toFloat()
            )

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
                            actionBorderOverlayView,
                            ::sendData
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

    /**
     * ✏️ Init action borders in view
     */
    private fun initActionBorders(prevHeight: Float, prevWidth: Float) {
        val leftBorder = ActionBorder(
            0f,
            prevHeight / 5 * 4,
            prevWidth,
            prevHeight / 5 * 4
        )

        val rightBorder =
            ActionBorder(
                0f,
                prevHeight / 5,
                prevWidth,
                prevHeight / 5
            )

        val leftInnerBorder = ActionBorder(
            0f,
            prevHeight / 5 * 3,
            prevWidth,
            prevHeight / 5 * 3
        )

        val rightInnerBorder =
            ActionBorder(
                0f,
                prevHeight / 5 * 2,
                prevWidth,
                prevHeight / 5 * 2
            )

        actionBorderOverlayView.setBorders(
            leftBorder,
            rightBorder,
            leftInnerBorder,
            rightInnerBorder
        )
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
        private const val ACTION_USB_PERMISSION = "permission"
    }
}
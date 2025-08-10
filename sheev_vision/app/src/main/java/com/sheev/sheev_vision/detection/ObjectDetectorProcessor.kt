package com.sheev.sheev_vision.detection

import android.graphics.Color
import android.media.Image
import android.util.Log
import android.util.Size
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetectorOptionsBase
import com.google.mlkit.vision.pose.PoseLandmark

class ObjectDetectorProcessor(
    options: PoseDetectorOptionsBase,
    private val landmarkOverlayView: LandmarkOverlayView,
    private val previewSize: Size
) : ImageAnalysis.Analyzer {

    private val poseDetector = PoseDetection.getClient(options)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image

        if (mediaImage != null) {

            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            poseDetector.process(image)
                .addOnSuccessListener { result ->
                    Log.d(TAG, "image processing successful")

                    processDetectedObjects(result, mediaImage)
                    imageProxy.close()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, e.toString())
                    imageProxy.close()
                }

        }
    }

    private fun processDetectedObjects(pose: Pose, mediaImage: Image) {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)

        val landmarks = if (rightShoulder != null && leftShoulder != null) {
            listOf(
                LandmarkOverlayView.PoseLandmark(
                    rightShoulder.position.x * previewSize.width.toFloat() / mediaImage.height,
                    rightShoulder.position.y * previewSize.height.toFloat() / mediaImage.width,
                    Color.RED,
                    "RS",
                    3.0f,
                    1
                ),
                LandmarkOverlayView.PoseLandmark(
                    leftShoulder.position.x * previewSize.width.toFloat() / mediaImage.height,
                    leftShoulder.position.y * previewSize.height.toFloat() / mediaImage.width,
                    Color.RED,
                    "LS",
                    3.0f,
                    2
                )
            )
        } else {
            emptyList()
        }

        landmarkOverlayView.setPoseLandemarks(landmarks)
    }

    companion object {
        private const val TAG = "ObjectDetectorProcessor"
    }
}
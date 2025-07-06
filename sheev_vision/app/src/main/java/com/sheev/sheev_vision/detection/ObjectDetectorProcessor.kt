package com.sheev.sheev_vision.detection

import android.graphics.Color
import android.graphics.Matrix
import android.media.Image
import android.util.Log
import android.util.Size
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetectorOptionsBase

class ObjectDetectorProcessor(
    options: ObjectDetectorOptionsBase,
    private val boundingBoxView: BoundingBoxView,
    private val displaySize: Size
) : ImageAnalysis.Analyzer {

    private val objectDetector = ObjectDetection.getClient(options)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image

        if (mediaImage != null) {
            val scaleMatrix = getInputImageScale(mediaImage)

            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees,
                scaleMatrix
            )

            objectDetector.process(image)
                .addOnSuccessListener { detectedObjects ->
                    Log.d(TAG, "image processing successful")

                    processDetectedObjects(detectedObjects, mediaImage)
                    imageProxy.close()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, e.toString())
                    imageProxy.close()
                }

        }
    }

    private fun getInputImageScale(img: Image): Matrix {
        // Calculate scale factors
        val scaleX = displaySize.width.toFloat() / img.height
        val scaleY = displaySize.height.toFloat() / img.width

        // Create transformation matrix
        return Matrix().apply {
            postScale(scaleX, scaleY)
        }
    }

    private fun processDetectedObjects(detectedObjects: List<DetectedObject>, mediaImage: Image) {
        val boundingBoxes = detectedObjects.map { detectedObject ->
            val boundingBox = detectedObject.boundingBox
            val trackingId = detectedObject.trackingId
            for (label in detectedObject.labels) {
                val text = label.text
                val index = label.index
                val confidence = label.confidence
            }
            BoundingBoxView.BoundingBox(
                detectedObject.boundingBox,
                Color.RED,
                "test",
                3.0f,
                detectedObject.trackingId
            )
        }.toMutableList()

        boundingBoxView.setBoundingBoxes(boundingBoxes)
    }

    companion object {
        private const val TAG = "ObjectDetectorProcessor"
    }
}
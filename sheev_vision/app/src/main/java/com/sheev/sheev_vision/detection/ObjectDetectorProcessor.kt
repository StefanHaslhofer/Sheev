package com.sheev.sheev_vision.detection

import android.util.Log
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
    private val boundingBoxView: BoundingBoxView
) : ImageAnalysis.Analyzer {

    private val objectDetector = ObjectDetection.getClient(options)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            objectDetector.process(image)
                .addOnSuccessListener { detectedObjects ->
                    Log.d(TAG, "image processing successful")

                    processDetectedObjects(detectedObjects)
                    imageProxy.close()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, e.toString())
                    imageProxy.close()
                }

        }
    }

    private fun processDetectedObjects(detectedObjects: List<DetectedObject>) {
        val boundingBoxes = detectedObjects.map { detectedObject ->
            val boundingBox = detectedObject.boundingBox
            val trackingId = detectedObject.trackingId
            for (label in detectedObject.labels) {
                val text = label.text
                val index = label.index
                val confidence = label.confidence
            }
            BoundingBoxView.BoundingBox(detectedObject.boundingBox, "test")
        }

        boundingBoxView.setBoundingBoxes(boundingBoxes)
    }

    companion object {
        private const val TAG = "ObjectDetectorProcessor"
    }
}
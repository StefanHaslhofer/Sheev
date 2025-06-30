package com.sheev.sheev_vision.detection

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetectorOptionsBase

class ObjectDetectorProcessor(options: ObjectDetectorOptionsBase) : ImageAnalysis.Analyzer {

    private val objectDetector = ObjectDetection.getClient(options)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            detectInImage(image)
        }
    }

    private fun detectInImage(image: InputImage) {
        objectDetector.process(image)
            .addOnSuccessListener { detectedObjects ->
                for (detectedObject in detectedObjects) {
                    val boundingBox = detectedObject.boundingBox
                    val trackingId = detectedObject.trackingId
                    for (label in detectedObject.labels) {
                        val text = label.text
                        val index = label.index
                        val confidence = label.confidence
                        print(text)
                    }
                }
            }
    }

    companion object {
        private const val TAG = "ObjectDetectorProcessor"
    }
}
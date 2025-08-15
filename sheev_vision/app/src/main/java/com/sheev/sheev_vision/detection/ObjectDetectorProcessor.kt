package com.sheev.sheev_vision.detection

import android.graphics.Color
import android.media.Image
import android.util.Log
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
    private val actionBorderOverlayView: ActionBorderOverlayView,
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

                    processBody(result, mediaImage)
                    imageProxy.close()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, e.toString())
                    imageProxy.close()
                }

        }
    }

    /**
     * paint body parts in landmarkOverlayView
     */
    private fun processBody(pose: Pose, mediaImage: Image) {
        // head
        val nose = pose.getPoseLandmark(PoseLandmark.NOSE)
        // upper body
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)
        // lower body
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)

        val landmarks =
            if (nose != null && rightShoulder != null && leftShoulder != null &&
                leftElbow != null && rightElbow != null && leftWrist != null &&
                rightWrist != null && leftHip != null && rightHip != null && leftKnee != null &&
                rightKnee != null && leftAnkle != null && rightAnkle != null
            ) {
                listOf(
                    LandmarkOverlayView.PoseLandmark(
                        nose.position.x, nose.position.y, Color.RED, "NS", 3.0f, 1
                    ),
                    LandmarkOverlayView.PoseLandmark(
                        leftShoulder.position.x, leftShoulder.position.y, Color.RED, "LS", 3.0f, 2
                    ),
                    LandmarkOverlayView.PoseLandmark(
                        rightShoulder.position.x, rightShoulder.position.y, Color.RED, "RS", 3.0f, 3
                    ),
                    LandmarkOverlayView.PoseLandmark(
                        leftElbow.position.x, leftElbow.position.y, Color.YELLOW, "LE", 3.0f, 4
                    ),
                    LandmarkOverlayView.PoseLandmark(
                        rightElbow.position.x, rightElbow.position.y, Color.YELLOW, "RE", 3.0f, 5
                    ),
                    LandmarkOverlayView.PoseLandmark(
                        leftWrist.position.x, leftWrist.position.y, Color.GREEN, "LW", 3.0f, 6
                    ),
                    LandmarkOverlayView.PoseLandmark(
                        rightWrist.position.x, rightWrist.position.y, Color.GREEN, "RW", 3.0f, 7
                    ),
                    LandmarkOverlayView.PoseLandmark(
                        leftHip.position.x, leftHip.position.y, Color.RED, "LH", 3.0f, 8
                    ),
                    LandmarkOverlayView.PoseLandmark(
                        rightHip.position.x, rightHip.position.y, Color.RED, "RH", 3.0f, 9
                    ),
                    LandmarkOverlayView.PoseLandmark(
                        leftKnee.position.x, leftKnee.position.y, Color.YELLOW, "LK", 3.0f, 10
                    ),
                    LandmarkOverlayView.PoseLandmark(
                        rightKnee.position.x, rightKnee.position.y, Color.YELLOW, "LK", 3.0f, 11
                    ),
                    LandmarkOverlayView.PoseLandmark(
                        leftAnkle.position.x, leftAnkle.position.y, Color.GREEN, "LA", 3.0f, 12
                    ),
                    LandmarkOverlayView.PoseLandmark(
                        rightAnkle.position.x, rightAnkle.position.y, Color.GREEN, "RA", 3.0f, 13
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
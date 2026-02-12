package com.example.mindarc.ui.components

import android.util.Size
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.nativeCanvas
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark

@Composable
fun PoseOverlay(
    modifier: Modifier = Modifier,
    pose: Pose?,
    imageSize: Size,
    repCount: Int,
    depthPercentage: Int,
    feedback: String
) {
    Canvas(modifier = modifier) {
        if (pose == null) return@Canvas

        val canvasWidth = size.width
        val canvasHeight = size.height

        val scaleX = canvasWidth / imageSize.width
        val scaleY = canvasHeight / imageSize.height
        val scaleFactor = maxOf(scaleX, scaleY)

        val dx = (canvasWidth - imageSize.width * scaleFactor) / 2
        val dy = (canvasHeight - imageSize.height * scaleFactor) / 2

        fun mapPoint(x: Float, y: Float): Offset {
             val flippedX = imageSize.width - x
             return Offset(flippedX * scaleFactor + dx, y * scaleFactor + dy)
        }

        val connections = listOf(
            Pair(PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER),
            Pair(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW),
            Pair(PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST),
            Pair(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW),
            Pair(PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST),
            Pair(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP),
            Pair(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP),
            Pair(PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP),
            Pair(PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE),
            Pair(PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE),
            Pair(PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE),
            Pair(PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE)
        )

        connections.forEach { (startType, endType) ->
            val start = pose.getPoseLandmark(startType)
            val end = pose.getPoseLandmark(endType)
            if (start != null && end != null) {
                drawLine(
                    color = Color.Cyan.copy(alpha = 0.6f),
                    start = mapPoint(start.position.x, start.position.y),
                    end = mapPoint(end.position.x, end.position.y),
                    strokeWidth = 6f
                )
            }
        }

        pose.allPoseLandmarks.forEach { landmark ->
            drawCircle(
                color = if (landmark.inFrameLikelihood > 0.5f) Color.Green else Color.Red.copy(alpha = 0.5f),
                radius = 8f,
                center = mapPoint(landmark.position.x, landmark.position.y)
            )
        }

        drawContext.canvas.nativeCanvas.apply {
            val paint = Paint().asFrameworkPaint().apply {
                color = android.graphics.Color.WHITE
                textSize = 48f
                isFakeBoldText = true
                setShadowLayer(8f, 0f, 0f, android.graphics.Color.BLACK)
            }

            val nose = pose.getPoseLandmark(PoseLandmark.NOSE)
            if (nose != null) {
                val pt = mapPoint(nose.position.x, nose.position.y)
                drawText("Reps: $repCount", pt.x - 100, pt.y - 100, paint)
                drawText("Depth: $depthPercentage%", pt.x - 100, pt.y - 50, paint)

                val feedbackPaint = Paint().asFrameworkPaint().apply {
                    color = if (feedback.contains("Good", true)) android.graphics.Color.GREEN else android.graphics.Color.YELLOW
                    textSize = 40f
                    setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
                }
                drawText(feedback, pt.x - 100, pt.y - 150, feedbackPaint)
            }
        }
    }
}

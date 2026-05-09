package com.example.mindarc.ui.components

import android.util.Size
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import com.example.mindarc.ui.theme.Success

@Composable
fun PoseOverlay(
    modifier: Modifier = Modifier,
    pose: Pose?,
    imageSize: Size,
    repCount: Int,
    depthPercentage: Int,
    feedback: String,
    primaryLabel: String = "reps",
    secondaryLabel: String = "depth",
    showSecondary: Boolean = true
) {
    // Theme-driven palette (computed in composable context; Canvas lambda is not @Composable).
    val skeleton = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.95f)
    val skeletonGlow = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.9f)
    val jointHigh = MaterialTheme.colorScheme.secondary.copy(alpha = 0.95f)
    val jointLow = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
    val textBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.90f)
    val accent = MaterialTheme.colorScheme.tertiary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val success = Success
    val onSurfaceArgb = MaterialTheme.colorScheme.onSurface.toArgb()

    // Hoist constant landmark connections and Android Paint objects to avoid per-frame allocations.
    val connections = remember {
        listOf(
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
            Pair(PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE),
        )
    }

    // Android Paint is mutable; we set text sizes/shadows once and only update colors in the draw pass.
    val repPaint = remember {
        android.graphics.Paint().apply {
            textSize = 36f
            isFakeBoldText = true
            setShadowLayer(6f, 0f, 2f, android.graphics.Color.BLACK)
        }
    }
    val labelPaint = remember {
        android.graphics.Paint().apply {
            textSize = 22f
            setShadowLayer(4f, 0f, 1f, android.graphics.Color.BLACK)
        }
    }
    val accentPaint = remember {
        android.graphics.Paint().apply {
            textSize = 20f
            setShadowLayer(4f, 0f, 1f, android.graphics.Color.BLACK)
        }
    }
    val feedbackPaint = remember {
        android.graphics.Paint().apply {
            textSize = 24f
            isFakeBoldText = true
            setShadowLayer(4f, 0f, 1f, android.graphics.Color.BLACK)
        }
    }

    val jointGlowRadius = 14f
    val jointRadius = 7f
    val glowWidth = 14f
    val strokeWidth = 5f

    val jointHighOuter = jointHigh.copy(alpha = 0.35f)
    val jointLowOuter = jointLow.copy(alpha = 0.35f)
    val jointHighInner = jointHigh.copy(alpha = 0.9f)
    val jointLowInner = jointLow.copy(alpha = 0.9f)
    val jointWhite = Color.White.copy(alpha = 0.9f)

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

        // ---- Bones: glow layer then main stroke (round cap/join) ----
        connections.forEach { (startType, endType) ->
            val start = pose.getPoseLandmark(startType)
            val end = pose.getPoseLandmark(endType)
            if (start != null && end != null) {
                val p1 = mapPoint(start.position.x, start.position.y)
                val p2 = mapPoint(end.position.x, end.position.y)
                // Avoid allocating Path objects each frame; draw the same “bone” as 2 lines.
                drawLine(
                    color = skeletonGlow.copy(alpha = 0.25f),
                    start = p1,
                    end = p2,
                    strokeWidth = glowWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = skeleton.copy(alpha = 0.95f),
                    start = p1,
                    end = p2,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
        }

        // ---- Joints: outer glow + inner fill (premium look) ----
        pose.allPoseLandmarks.forEach { landmark ->
            val jointCenter = mapPoint(landmark.position.x, landmark.position.y)
            val confident = landmark.inFrameLikelihood > 0.5f
            val outer = if (confident) jointHighOuter else jointLowOuter
            val inner = if (confident) jointHighInner else jointLowInner
            drawCircle(
                color = outer,
                radius = jointGlowRadius,
                center = jointCenter
            )
            drawCircle(
                color = inner,
                radius = jointRadius,
                center = jointCenter
            )
            drawCircle(
                color = jointWhite,
                radius = jointRadius * 0.5f,
                center = jointCenter
            )
        }

        // ---- Premium HUD: rounded card + typography ----
        val nose = pose.getPoseLandmark(PoseLandmark.NOSE)
        if (nose != null) {
            val pt = mapPoint(nose.position.x, nose.position.y)
            val hudLeft = (pt.x - 120f).coerceIn(20f, canvasWidth - 260f)
            val hudTop = (pt.y - 140f).coerceIn(20f, canvasHeight - 120f)
            val hudWidth = 240f
            val hudHeight = 100f
            val cornerRadius = CornerRadius(20f)
            drawRoundRect(
                color = textBg,
                topLeft = Offset(hudLeft, hudTop),
                size = androidx.compose.ui.geometry.Size(hudWidth, hudHeight),
                cornerRadius = cornerRadius
            )
            drawRoundRect(
                color = skeletonGlow.copy(alpha = 0.2f),
                topLeft = Offset(hudLeft, hudTop),
                size = androidx.compose.ui.geometry.Size(hudWidth, hudHeight),
                cornerRadius = cornerRadius,
                style = Stroke(width = 1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            drawContext.canvas.nativeCanvas.apply {
                // Update colors only; paint instances themselves are reused across frames.
                repPaint.color = onSurfaceArgb
                labelPaint.color = muted.toArgb()
                accentPaint.color = accent.toArgb()

                val isGood = feedback.contains("Good", true) || feedback.contains("Great", true)
                feedbackPaint.color = if (isGood) success.toArgb() else onSurfaceArgb

                drawText("$repCount", hudLeft + 24f, hudTop + 42f, repPaint)
                drawText(primaryLabel, hudLeft + 24f, hudTop + 68f, labelPaint)
                if (showSecondary) {
                    drawText("$depthPercentage%", hudLeft + 130f, hudTop + 42f, accentPaint)
                    drawText(secondaryLabel, hudLeft + 130f, hudTop + 68f, labelPaint)
                }
                drawText(feedback, hudLeft + 24f, hudTop + 92f, feedbackPaint)
            }
        }
    }
}

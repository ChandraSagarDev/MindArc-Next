package com.example.mindarc.domain

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.*

class PoseAnalyzer {

    data class PushUpMetrics(
        val elbowAngle: Float?,
        val repCount: Int,
        val depthPercentage: Int,
        val feedback: String,
        val isHorizontal: Boolean,
        val confidence: Float
    )

    data class SquatMetrics(
        val kneeAngle: Float?,
        val repCount: Int,
        val depthPercentage: Int,
        val feedback: String,
        val isUpright: Boolean,
        val confidence: Float
    )

    data class PlankHoldMetrics(
        val isCorrect: Boolean,
        val stabilityPercentage: Int,
        val feedback: String,
        val confidence: Float
    )

    // ========================================================================
    // 4-Stage State Machine for rep counting
    // Must traverse: UP -> GOING_DOWN -> AT_BOTTOM -> GOING_UP -> UP (= 1 rep)
    // ========================================================================
    private enum class RepPhase {
        UP,          // Standing / arms extended
        GOING_DOWN,  // Actively descending
        AT_BOTTOM,   // Reached full depth
        GOING_UP     // Actively ascending
    }

    // --- Pushup state ---
    private var pushupRepCount = 0
    private var pushupPhase = RepPhase.UP
    private var pushupFramesInPhase = 0

    // --- Squat state ---
    private var squatRepCount = 0
    private var squatPhase = RepPhase.UP
    private var squatFramesInPhase = 0

    // ========================================================================
    // Thresholds — tuned with hysteresis (enter vs exit offsets)
    // Informed by fitness-trainer-pose-estimation-master angle ranges
    // ========================================================================

    // Pushup elbow angle thresholds — lenient so reps register reliably
    private val PUSHUP_DOWN_ENTER = 100f      // bend to ~100° or below = bottom
    private val PUSHUP_DOWN_EXIT = 108f       // rise above this to leave bottom
    private val PUSHUP_UP_ENTER = 142f        // arms extended enough = top (was 155, too strict)
    private val PUSHUP_UP_EXIT = 125f         // drop below this = start descent

    // Squat secondary: hip angle (shoulder-hip-knee) for form feedback only (not used for count)
    private val SQUAT_MAX_HIP_ANGLE = 145f

    // ========================================================================
    // SQUAT: Displacement-based (hip + head vertical only, no joint angles)
    // ========================================================================
    private val SQUAT_EMA_ALPHA = 0.25f
    private val SQUAT_DEPTH_REF_FRACTION = 0.45f       // ref = imageHeight * this for scale-invariance
    private val SQUAT_BOTTOM_DEPTH_THRESHOLD = 0.22f  // normalized depth to count as "at bottom"
    private val SQUAT_BOTTOM_EXIT_THRESHOLD = 0.18f   // above this = left bottom
    private val SQUAT_UP_DEPTH_THRESHOLD = 0.06f      // below this = standing
    private val SQUAT_VELOCITY_DOWN_THRESHOLD = -0.008f  // must move down to enter GOING_DOWN
    private val SQUAT_VELOCITY_UP_THRESHOLD = 0.006f     // must move up to leave AT_BOTTOM
    private val SQUAT_MIN_BOTTOM_HOLD_MS = 200L
    private val SQUAT_MIN_DEPTH_RANGE_FOR_REP = 0.12f   // half-rep rejection: min depth range in rep
    private val SQUAT_BASELINE_STABLE_FRAMES = 5
    private val SQUAT_REP_COOLDOWN_MS = 400L

    private var squatBaselineHipY: Float? = null
    private var squatBaselineHeadY: Float? = null
    private var squatBaselineStableFrames: Int = 0
    private var squatSmoothedDepth: Float = 0f
    private var squatPrevDepth: Float = 0f
    private var squatVelocity: Float = 0f
    private var squatBottomEnterTimeMs: Long = 0L
    private var squatMinDepthInRep: Float = 1f
    private var squatMaxDepthInRep: Float = 0f

    // ========================================================================
    // Consecutive frame confirmation — 1 frame so smoothed angle can register
    // (smoothing often prevents 2 consecutive frames hitting threshold)
    // ========================================================================
    private val MIN_FRAMES_TO_CONFIRM = 1

    // Pushup confirmation counters
    private var pushupConsecDown = 0
    private var pushupConsecUp = 0
    private var pushupConsecGoingDown = 0
    private var pushupConsecGoingUp = 0

    // Squat confirmation counters
    private var squatConsecDown = 0
    private var squatConsecUp = 0
    private var squatConsecGoingDown = 0
    private var squatConsecGoingUp = 0

    // --- Forearm plank (front view): upper body only (no hip/knee/ankle) ---
    private val PLANK_Y_SPREAD_THRESHOLD = 0.14f    // normalized (shoulders + elbows vertical spread) — relaxed so timer counts
    private val PLANK_SHOULDER_LEVEL_THRESHOLD = 0.06f // |leftShoulderY - rightShoulderY| normalized
    private val PLANK_ELBOW_X_DIFF_THRESHOLD = 0.28f // |elbowMidX - shoulderMidX| normalized — elbows can be wider/narrower
    private val PLANK_MIN_CONSEC_FRAMES = 3         // fewer frames so "correct" registers quickly

    // --- Forearm plank (side / profile view): shoulders appear narrow in X; use shoulder–hip line + one arm ---
    /** If shoulder line is narrower than this (fraction of image width), treat as side/profile view. */
    private val PLANK_SIDE_SHOULDER_X_SPAN_MAX = 0.24f
    /** Shoulder and hip should be nearly level (horizontal trunk) in profile. */
    private val PLANK_SIDE_TRUNK_Y_THRESHOLD = 0.14f
    /** Elbow should be clearly below shoulder (forearms on floor) but not absurdly far. */
    private val PLANK_SIDE_ELBOW_DROP_MIN = 0.02f
    private val PLANK_SIDE_ELBOW_DROP_MAX = 0.42f
    /** In profile, elbow should stack under the shoulder in X. */
    private val PLANK_SIDE_ELBOW_X_ALIGN_MAX = 0.26f
    /** Forearm on ground: wrist and elbow at similar height. */
    private val PLANK_SIDE_FOREARM_Y_THRESHOLD = 0.11f

    private var plankConsecCorrectFrames = 0

    // ========================================================================
    // Median filter (window of 5) + EMA smoothing
    // ========================================================================
    private val MEDIAN_WINDOW = 5
    private val pushupAngleBuffer = ArrayDeque<Float>(MEDIAN_WINDOW)
    private var smoothedPushupAngle: Float? = null
    private val EMA_ALPHA = 0.35f

    // Velocity tracking (pushup: degrees per frame)
    private var prevPushupAngle: Float? = null
    private var pushupVelocity: Float = 0f

    // ========================================================================
    // Rep cooldown
    // ========================================================================
    private var lastPushupRepTime = 0L
    private var lastSquatRepTime = 0L
    private val REP_COOLDOWN_MS = 450L  // shorter so consecutive reps count reliably

    // ========================================================================
    // Confidence
    // ========================================================================
    private val MIN_LANDMARK_CONFIDENCE = 0.65f  // was 0.7; slightly more inclusive

    // Peak tracking (pushup: angle; squat uses depth in own state)
    private var pushupMinAngleInRep: Float = 180f
    private var pushupMaxAngleInRep: Float = 0f

    // Minimum range of motion (pushup: angle; squat: depth range in state)
    private val PUSHUP_MIN_ROM = 20f

    // ========================================================================
    // 3-Signal depth (professional): Shoulder Drop + Nose Drop + Elbow Expansion
    // ========================================================================
    private var pushupShoulderTopY: Float? = null
    private var pushupNoseTopY: Float? = null
    private var pushupElbowRatioTop: Float? = null
    private var pushupBaselineStableFrames: Int = 0
    private val PUSHUP_BASELINE_FRAMES = 5
    private val DEPTH_BOTTOM_THRESHOLD = 0.5f   // depthScore >= this = at bottom
    private val DEPTH_SHOULDER_DROP_NORM_SCALE = 0.12f  // ~12% body height = full signal
    private val DEPTH_NOSE_DROP_NORM_SCALE = 0.10f
    private val DEPTH_ELBOW_EXPANSION_SCALE = 0.25f     // ratio increase for full signal

    // ========================================================================
    // PUSHUP ANALYSIS — elbow angle + 3-signal depth (shoulder, nose, elbow width)
    // ========================================================================
    fun analyzePushUpPose(pose: Pose, imageWidth: Int, imageHeight: Int): PushUpMetrics {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)
        val nose = pose.getPoseLandmark(PoseLandmark.NOSE)

        val leftArmConfident = (leftShoulder?.inFrameLikelihood ?: 0f) >= MIN_LANDMARK_CONFIDENCE &&
                (leftElbow?.inFrameLikelihood ?: 0f) >= MIN_LANDMARK_CONFIDENCE &&
                (leftWrist?.inFrameLikelihood ?: 0f) >= MIN_LANDMARK_CONFIDENCE
        val rightArmConfident = (rightShoulder?.inFrameLikelihood ?: 0f) >= MIN_LANDMARK_CONFIDENCE &&
                (rightElbow?.inFrameLikelihood ?: 0f) >= MIN_LANDMARK_CONFIDENCE &&
                (rightWrist?.inFrameLikelihood ?: 0f) >= MIN_LANDMARK_CONFIDENCE

        val leftAngle = if (leftArmConfident) calculateAngle(leftShoulder, leftElbow, leftWrist) else null
        val rightAngle = if (rightArmConfident) calculateAngle(rightShoulder, rightElbow, rightWrist) else null
        val rawAngle = when {
            leftAngle != null && rightAngle != null -> (leftAngle + rightAngle) / 2f
            leftAngle != null -> leftAngle
            rightAngle != null -> rightAngle
            else -> null
        }

        val currentAngle = applySmoothing(rawAngle, pushupAngleBuffer, smoothedPushupAngle)
        if (currentAngle != null) smoothedPushupAngle = currentAngle

        if (currentAngle != null) {
            pushupVelocity = (prevPushupAngle?.let { currentAngle - it } ?: 0f)
            prevPushupAngle = currentAngle
        }

        val shoulder = leftShoulder ?: rightShoulder
        val hasValidPose = leftArmConfident || rightArmConfident

        // --- 3-Signal depth: shoulder drop, nose drop, elbow width expansion ---
        val depthScore = computePushupDepthScore(
            leftShoulder, rightShoulder, leftElbow, rightElbow, nose,
            imageHeight, currentAngle, hasValidPose
        )

        val now = System.currentTimeMillis()
        if (currentAngle != null) {
            updatePushupStateMachine(currentAngle, now, depthScore)
        }

        // Depth % from combined signal when available, else fallback to angle-based
        val depthPercentage = if (depthScore >= 0f) {
            (depthScore * 100f).toInt().coerceIn(0, 100)
        } else if (currentAngle != null) {
            val progress = (PUSHUP_UP_ENTER - currentAngle) / (PUSHUP_UP_ENTER - PUSHUP_DOWN_ENTER)
            (progress * 100).toInt().coerceIn(0, 100)
        } else 0

        val feedback = generatePushupFeedback(currentAngle, hasValidPose)

        return PushUpMetrics(
            elbowAngle = currentAngle,
            repCount = pushupRepCount,
            depthPercentage = depthPercentage,
            feedback = feedback,
            isHorizontal = hasValidPose,
            confidence = shoulder?.inFrameLikelihood ?: 0f
        )
    }

    /**
     * Combines 3 signals for professional-level pushup depth:
     * 1. Shoulder drop (torso moving down)
     * 2. Nose drop (head moving down)
     * 3. Elbow width expansion (elbows flare when bending, front view)
     * Returns depth score in [0, 1], or -1f if not enough data.
     */
    private fun computePushupDepthScore(
        leftShoulder: PoseLandmark?,
        rightShoulder: PoseLandmark?,
        leftElbow: PoseLandmark?,
        rightElbow: PoseLandmark?,
        nose: PoseLandmark?,
        imageHeight: Int,
        currentAngle: Float?,
        hasValidPose: Boolean
    ): Float {
        if (!hasValidPose || imageHeight <= 0) return -1f

        val midShoulderY = when {
            leftShoulder != null && rightShoulder != null ->
                (leftShoulder.position.y + rightShoulder.position.y) / 2f
            leftShoulder != null -> leftShoulder.position.y
            rightShoulder != null -> rightShoulder.position.y
            else -> return -1f
        }

        val shoulderWidth = if (leftShoulder != null && rightShoulder != null)
            abs(rightShoulder.position.x - leftShoulder.position.x) else 1f
        val elbowWidth = if (leftElbow != null && rightElbow != null)
            abs(rightElbow.position.x - leftElbow.position.x) else 0f
        val elbowRatio = if (shoulderWidth > 1f) elbowWidth / shoulderWidth else 0f
        val noseY = nose?.position?.y

        // Update baseline when stable at top (arms extended)
        if (currentAngle != null && currentAngle >= PUSHUP_UP_ENTER - 8f) {
            pushupBaselineStableFrames++
            if (pushupBaselineStableFrames >= PUSHUP_BASELINE_FRAMES && pushupShoulderTopY == null) {
                pushupShoulderTopY = midShoulderY
                noseY?.let { pushupNoseTopY = it }
                pushupElbowRatioTop = elbowRatio
            }
        } else {
            pushupBaselineStableFrames = 0
        }

        val refHeight = imageHeight * 0.5f
        if (refHeight <= 0f) return -1f

        // 1. Shoulder drop (positive = down in image coords)
        val shoulderDrop = pushupShoulderTopY?.let { midShoulderY - it } ?: 0f
        val shoulderDropNorm = (shoulderDrop / refHeight / DEPTH_SHOULDER_DROP_NORM_SCALE).coerceIn(0f, 1f)

        // 2. Nose drop
        val noseDrop = if (pushupNoseTopY != null && noseY != null) (noseY - pushupNoseTopY!!) else 0f
        val noseDropNorm = (noseDrop / refHeight / DEPTH_NOSE_DROP_NORM_SCALE).coerceIn(0f, 1f)

        // 3. Elbow width expansion (ratio increases when elbows go out)
        val expansion = pushupElbowRatioTop?.let { (elbowRatio - it).coerceAtLeast(0f) } ?: 0f
        val elbowExpansionNorm = (expansion / DEPTH_ELBOW_EXPANSION_SCALE).coerceIn(0f, 1f)

        if (pushupShoulderTopY == null) return -1f

        // Combined: weighted average (all signals contribute)
        return (0.4f * shoulderDropNorm + 0.3f * noseDropNorm + 0.3f * elbowExpansionNorm).coerceIn(0f, 1f)
    }

    private fun updatePushupStateMachine(
        angle: Float,
        now: Long,
        depthScore: Float = -1f
    ) {
        val atBottomByAngle = angle <= PUSHUP_DOWN_ENTER
        val atBottomByDepth = depthScore >= DEPTH_BOTTOM_THRESHOLD
        val atBottom = atBottomByAngle || atBottomByDepth

        when (pushupPhase) {
            RepPhase.UP -> {
                if (angle < PUSHUP_UP_EXIT) {
                    pushupConsecGoingDown++
                    if (pushupConsecGoingDown >= MIN_FRAMES_TO_CONFIRM) {
                        pushupPhase = RepPhase.GOING_DOWN
                        pushupFramesInPhase = 0
                        pushupMinAngleInRep = angle
                        pushupMaxAngleInRep = angle
                        pushupConsecGoingDown = 0
                    }
                } else {
                    pushupConsecGoingDown = 0
                }
            }

            RepPhase.GOING_DOWN -> {
                pushupFramesInPhase++
                pushupMinAngleInRep = min(pushupMinAngleInRep, angle)
                pushupMaxAngleInRep = max(pushupMaxAngleInRep, angle)

                // Bottom: either angle-based OR 3-signal depth (shoulder + nose + elbow)
                if (atBottom) {
                    pushupConsecDown++
                    if (pushupConsecDown >= MIN_FRAMES_TO_CONFIRM) {
                        pushupPhase = RepPhase.AT_BOTTOM
                        pushupFramesInPhase = 0
                        pushupConsecDown = 0
                    }
                } else {
                    pushupConsecDown = 0
                }

                if (angle > PUSHUP_UP_ENTER) {
                    pushupPhase = RepPhase.UP
                    pushupConsecDown = 0
                }
            }

            RepPhase.AT_BOTTOM -> {
                pushupFramesInPhase++
                pushupMinAngleInRep = min(pushupMinAngleInRep, angle)

                // Looking for the start of an ascent
                // IMPROVED: Also removed strict velocity requirement here;
                // use a gentler check — any upward movement or angle above exit threshold
                if (angle > PUSHUP_DOWN_EXIT) {
                    pushupConsecGoingUp++
                    if (pushupConsecGoingUp >= MIN_FRAMES_TO_CONFIRM) {
                        pushupPhase = RepPhase.GOING_UP
                        pushupFramesInPhase = 0
                        pushupConsecGoingUp = 0
                    }
                } else {
                    pushupConsecGoingUp = 0
                }
            }

            RepPhase.GOING_UP -> {
                pushupFramesInPhase++
                pushupMaxAngleInRep = max(pushupMaxAngleInRep, angle)

                // Check if reached the top
                if (angle >= PUSHUP_UP_ENTER) {
                    pushupConsecUp++
                    if (pushupConsecUp >= MIN_FRAMES_TO_CONFIRM) {
                        // Validate the rep: ROM + cooldown only
                        // IMPROVED: Form checks are now informational feedback only,
                        // not blocking. This prevents rejecting valid reps due to
                        // camera angle or minor form deviations.
                        val rom = pushupMaxAngleInRep - pushupMinAngleInRep
                        val cooldownPassed = (now - lastPushupRepTime) >= REP_COOLDOWN_MS

                        if (rom >= PUSHUP_MIN_ROM && cooldownPassed) {
                            pushupRepCount++
                            lastPushupRepTime = now
                            // Refresh baseline so next rep uses current "top" as reference
                            pushupShoulderTopY = null
                            pushupNoseTopY = null
                            pushupElbowRatioTop = null
                            pushupBaselineStableFrames = 0
                        }

                        pushupPhase = RepPhase.UP
                        pushupFramesInPhase = 0
                        pushupConsecUp = 0
                        pushupMinAngleInRep = 180f
                        pushupMaxAngleInRep = 0f
                    }
                } else {
                    pushupConsecUp = 0
                }

                // If angle drops back below DOWN threshold, we went back down
                if (angle < PUSHUP_DOWN_ENTER) {
                    pushupPhase = RepPhase.AT_BOTTOM
                    pushupConsecUp = 0
                }
            }
        }
    }

    private fun generatePushupFeedback(
        angle: Float?,
        hasValidPose: Boolean
    ): String {
        if (angle == null) return "Position yourself in front of the camera"
        if (!hasValidPose) return "Make sure your arms and shoulders are visible"

        return when (pushupPhase) {
            RepPhase.UP -> "Go down slowly"
            RepPhase.GOING_DOWN -> "Keep going down..."
            RepPhase.AT_BOTTOM -> "Good depth! Push up now"
            RepPhase.GOING_UP -> "Push! Almost there..."
        }
    }

    // ========================================================================
    // SQUAT ANALYSIS — Hip & head vertical displacement only (no joint angles)
    // ========================================================================
    //
    // ALGORITHM DESIGN
    // ----------------
    // 1. Signal: Normalized vertical displacement from standing.
    //    - Hip Y and head Y (image coords; +Y = lower). Baseline = standing pose.
    //    - Raw depth = (hipDrop + headDrop) / 2, normalized by imageHeight so the same
    //      thresholds work at different camera distances (scale-invariant).
    // 2. Dynamic baseline: When depth stays low for SQUAT_BASELINE_STABLE_FRAMES,
    //    we set baseline hip Y and head Y. Re-calibrated after each rep so drift is minimal.
    // 3. Exponential smoothing: EMA on raw depth to reduce jitter (SQUAT_EMA_ALPHA).
    // 4. Velocity: (smoothedDepth - prevDepth) per frame. Used to require intentional
    //    movement: e.g. negative velocity to enter GOING_DOWN, positive to leave AT_BOTTOM.
    // 5. Minimum bottom hold (0.2 s): Transition AT_BOTTOM -> GOING_UP only after
    //    squatBottomEnterTimeMs + 200 ms. Prevents counting half-reps that bounce.
    // 6. Half-rep rejection: Count rep only if (squatMaxDepthInRep - squatMinDepthInRep)
    //    >= SQUAT_MIN_DEPTH_RANGE_FOR_REP and bottom hold was satisfied.
    // 7. State machine: UP -> GOING_DOWN (depth + velocity) -> AT_BOTTOM (depth)
    //    -> [hold 0.2s] -> GOING_UP (velocity) -> UP (depth); validate rep at UP.
    //
    // RESEARCH-LEVEL EXTENSIONS: see docs/SQUAT_DETECTION_RESEARCH_SUGGESTIONS.md
    //
    fun analyzeSquatPose(pose: Pose, imageWidth: Int, imageHeight: Int): SquatMetrics {
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        val nose = pose.getPoseLandmark(PoseLandmark.NOSE)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)

        val hipY = when {
            leftHip != null && rightHip != null -> (leftHip.position.y + rightHip.position.y) / 2f
            leftHip != null -> leftHip.position.y
            rightHip != null -> rightHip.position.y
            else -> null
        }
        val headY = nose?.position?.y
        val shoulder = leftShoulder ?: rightShoulder
        val ankle = leftAnkle ?: rightAnkle

        val hasValidPose = hipY != null && (leftHip?.inFrameLikelihood ?: 0f) >= MIN_LANDMARK_CONFIDENCE

        if (!hasValidPose || imageHeight <= 0) {
            return SquatMetrics(
                kneeAngle = null,
                repCount = squatRepCount,
                depthPercentage = 0,
                feedback = "Position your full body in frame",
                isUpright = false,
                confidence = 0f
            )
        }

        val refHeight = imageHeight * SQUAT_DEPTH_REF_FRACTION

        // ---- Dynamic baseline (standing = low depth) ----
        val rawDepth = computeSquatRawDepth(hipY, headY, refHeight)
        if (rawDepth != null) {
            updateSquatBaseline(hipY, headY, rawDepth)
        }

        val depthForSmoothed = rawDepth ?: squatSmoothedDepth
        squatPrevDepth = squatSmoothedDepth
        squatSmoothedDepth = SQUAT_EMA_ALPHA * depthForSmoothed + (1f - SQUAT_EMA_ALPHA) * squatSmoothedDepth
        squatVelocity = squatSmoothedDepth - squatPrevDepth

        val isUpright = shoulder != null && ankle != null &&
                abs(shoulder.position.y - ankle.position.y) > (imageHeight * 0.4f)
        val hipHingeAngle = calculateBestSideAngle(
            leftShoulder, leftHip, leftKnee,
            rightShoulder, rightHip, rightKnee
        )
        val isHipHinging = hipHingeAngle != null && hipHingeAngle <= SQUAT_MAX_HIP_ANGLE
        val kneesCaving = detectKneesCaving(leftKnee, leftAnkle, rightKnee, rightAnkle)
        val leaningForward = detectForwardLean(shoulder, leftHip ?: rightHip, imageWidth)

        val now = System.currentTimeMillis()
        updateSquatStateMachineDisplacement(squatSmoothedDepth, squatVelocity, now)

        val depthPercentage = (squatSmoothedDepth * 100f).toInt().coerceIn(0, 100)
        val feedback = generateSquatFeedback(
            squatSmoothedDepth,
            isHipHinging, isUpright, kneesCaving, leaningForward
        )

        return SquatMetrics(
            kneeAngle = null,
            repCount = squatRepCount,
            depthPercentage = depthPercentage,
            feedback = feedback,
            isUpright = isUpright,
            confidence = shoulder?.inFrameLikelihood ?: 0f
        )
    }

    /** Raw normalized depth from hip/head displacement (no angles). Scale-invariant. */
    private fun computeSquatRawDepth(hipY: Float?, headY: Float?, refHeight: Float): Float? {
        if (hipY == null || refHeight <= 0f) return null
        if (squatBaselineHipY == null) {
            squatBaselineHipY = hipY
            squatBaselineHeadY = headY
            return 0f
        }
        val hipDrop = hipY - squatBaselineHipY!!
        val headDrop = if (squatBaselineHeadY != null && headY != null) headY - squatBaselineHeadY!! else hipDrop
        val combinedDrop = (hipDrop + headDrop) / 2f
        return (combinedDrop / refHeight).coerceIn(0f, 1f)
    }

    private fun updateSquatBaseline(hipY: Float, headY: Float?, rawDepth: Float) {
        if (rawDepth < SQUAT_UP_DEPTH_THRESHOLD) {
            squatBaselineStableFrames++
            if (squatBaselineStableFrames >= SQUAT_BASELINE_STABLE_FRAMES) {
                squatBaselineHipY = squatBaselineHipY?.let { it * 0.9f + hipY * 0.1f } ?: hipY
                headY?.let { squatBaselineHeadY = squatBaselineHeadY?.let { h -> h * 0.9f + it * 0.1f } ?: it }
            }
        } else {
            squatBaselineStableFrames = 0
        }
    }

    /**
     * State machine for squats: displacement + velocity + minimum bottom hold + half-rep rejection.
     * No joint angle math.
     */
    private fun updateSquatStateMachineDisplacement(depth: Float, velocity: Float, now: Long) {
        val atBottom = depth >= SQUAT_BOTTOM_DEPTH_THRESHOLD
        val leftBottom = depth <= SQUAT_BOTTOM_EXIT_THRESHOLD
        val atTop = depth <= SQUAT_UP_DEPTH_THRESHOLD
        val bottomHoldSatisfied = squatBottomEnterTimeMs > 0L &&
                (now - squatBottomEnterTimeMs) >= SQUAT_MIN_BOTTOM_HOLD_MS

        when (squatPhase) {
            RepPhase.UP -> {
                if (depth > SQUAT_UP_DEPTH_THRESHOLD && velocity < SQUAT_VELOCITY_DOWN_THRESHOLD) {
                    squatConsecGoingDown++
                    if (squatConsecGoingDown >= MIN_FRAMES_TO_CONFIRM) {
                        squatPhase = RepPhase.GOING_DOWN
                        squatFramesInPhase = 0
                        squatMinDepthInRep = depth
                        squatMaxDepthInRep = depth
                        squatConsecGoingDown = 0
                    }
                } else {
                    squatConsecGoingDown = 0
                }
            }

            RepPhase.GOING_DOWN -> {
                squatFramesInPhase++
                squatMinDepthInRep = min(squatMinDepthInRep, depth)
                squatMaxDepthInRep = max(squatMaxDepthInRep, depth)
                if (atBottom) {
                    squatConsecDown++
                    if (squatConsecDown >= MIN_FRAMES_TO_CONFIRM) {
                        squatPhase = RepPhase.AT_BOTTOM
                        squatFramesInPhase = 0
                        squatBottomEnterTimeMs = now
                        squatConsecDown = 0
                    }
                } else {
                    squatConsecDown = 0
                }
                if (atTop) {
                    squatPhase = RepPhase.UP
                    squatConsecDown = 0
                }
            }

            RepPhase.AT_BOTTOM -> {
                squatFramesInPhase++
                squatMinDepthInRep = min(squatMinDepthInRep, depth)
                if (leftBottom && velocity > SQUAT_VELOCITY_UP_THRESHOLD && bottomHoldSatisfied) {
                    squatConsecGoingUp++
                    if (squatConsecGoingUp >= MIN_FRAMES_TO_CONFIRM) {
                        squatPhase = RepPhase.GOING_UP
                        squatFramesInPhase = 0
                        squatConsecGoingUp = 0
                    }
                } else {
                    squatConsecGoingUp = 0
                }
            }

            RepPhase.GOING_UP -> {
                squatFramesInPhase++
                squatMaxDepthInRep = max(squatMaxDepthInRep, depth)
                if (atTop) {
                    squatConsecUp++
                    if (squatConsecUp >= MIN_FRAMES_TO_CONFIRM) {
                        val depthRange = squatMaxDepthInRep - squatMinDepthInRep
                        val cooldownPassed = (now - lastSquatRepTime) >= SQUAT_REP_COOLDOWN_MS
                        val fullRep = depthRange >= SQUAT_MIN_DEPTH_RANGE_FOR_REP && bottomHoldSatisfied
                        if (fullRep && cooldownPassed) {
                            squatRepCount++
                            lastSquatRepTime = now
                        }
                        squatPhase = RepPhase.UP
                        squatFramesInPhase = 0
                        squatConsecUp = 0
                        squatMinDepthInRep = 1f
                        squatMaxDepthInRep = 0f
                        squatBottomEnterTimeMs = 0L
                    }
                } else {
                    squatConsecUp = 0
                }
                if (atBottom) {
                    squatPhase = RepPhase.AT_BOTTOM
                    squatConsecUp = 0
                }
            }
        }
    }

    /**
     * Detects if knees are caving inward during squats.
     * Adapted from fitness-trainer reference: knees_caving rule.
     * Compares knee X-position to ankle X-position.
     */
    private fun detectKneesCaving(
        leftKnee: PoseLandmark?,
        leftAnkle: PoseLandmark?,
        rightKnee: PoseLandmark?,
        rightAnkle: PoseLandmark?
    ): Boolean {
        // Check left side: knee should not be significantly inside ankle
        val leftCaving = if (leftKnee != null && leftAnkle != null) {
            leftKnee.position.x < leftAnkle.position.x - 20f
        } else false

        // Check right side: knee should not be significantly inside ankle
        val rightCaving = if (rightKnee != null && rightAnkle != null) {
            rightKnee.position.x > rightAnkle.position.x + 20f
        } else false

        return leftCaving || rightCaving
    }

    /**
     * Detects if the user is leaning too far forward during squats.
     * Adapted from fitness-trainer reference: leaning_forward rule.
     */
    private fun detectForwardLean(
        shoulder: PoseLandmark?,
        hip: PoseLandmark?,
        imageWidth: Int
    ): Boolean {
        if (shoulder == null || hip == null) return false
        // Shoulder X should not be much further forward than hip X
        val threshold = imageWidth * 0.08f
        return abs(shoulder.position.x - hip.position.x) > threshold &&
                shoulder.position.y > hip.position.y  // shoulder below hip means leaning
    }

    private fun generateSquatFeedback(
        depth: Float,
        isHipHinging: Boolean,
        isUpright: Boolean,
        kneesCaving: Boolean,
        leaningForward: Boolean
    ): String {
        val formTip = when {
            !isUpright -> " (tip: step back for full body view)"
            kneesCaving -> " (tip: keep knees over toes)"
            leaningForward -> " (tip: keep chest up)"
            else -> ""
        }
        return when (squatPhase) {
            RepPhase.UP -> "Squat down slowly$formTip"
            RepPhase.GOING_DOWN -> "Keep going down...$formTip"
            RepPhase.AT_BOTTOM -> "Good depth! Hold, then stand up"
            RepPhase.GOING_UP -> "Drive up! Almost there..."
        }
    }

    // ========================================================================
    // SHARED UTILITIES
    // ========================================================================

    /**
     * Applies a median filter (removes spike outliers) followed by
     * exponential moving average (smooths remaining noise).
     */
    private fun applySmoothing(
        rawAngle: Float?,
        buffer: ArrayDeque<Float>,
        prevSmoothed: Float?
    ): Float? {
        if (rawAngle == null) return prevSmoothed

        // --- Median filter ---
        buffer.addLast(rawAngle)
        if (buffer.size > MEDIAN_WINDOW) {
            buffer.removeFirst()
        }
        val median = if (buffer.size >= 3) {
            val sorted = buffer.toList().sorted()
            sorted[sorted.size / 2]
        } else {
            rawAngle
        }

        // --- EMA on median-filtered value ---
        return if (prevSmoothed != null) {
            EMA_ALPHA * median + (1 - EMA_ALPHA) * prevSmoothed
        } else {
            median
        }
    }

    /**
     * Calculates 3D angle at point p2 (vertex) formed by p1-p2-p3.
     */
    private fun calculateAngle(p1: PoseLandmark?, p2: PoseLandmark?, p3: PoseLandmark?): Float? {
        if (p1 == null || p2 == null || p3 == null) return null
        val p1_3D = p1.position3D
        val p2_3D = p2.position3D
        val p3_3D = p3.position3D
        val v1 = floatArrayOf(p1_3D.x - p2_3D.x, p1_3D.y - p2_3D.y, p1_3D.z - p2_3D.z)
        val v2 = floatArrayOf(p3_3D.x - p2_3D.x, p3_3D.y - p2_3D.y, p3_3D.z - p2_3D.z)
        val dot = v1[0] * v2[0] + v1[1] * v2[1] + v1[2] * v2[2]
        val m1 = sqrt(v1[0] * v1[0] + v1[1] * v1[1] + v1[2] * v1[2])
        val m2 = sqrt(v2[0] * v2[0] + v2[1] * v2[1] + v2[2] * v2[2])
        return if (m1 == 0f || m2 == 0f) null
        else (acos((dot / (m1 * m2)).coerceIn(-1f, 1f)) * 180f / PI).toFloat()
    }

    /**
     * Picks the best (most confident) side's angle from left/right landmark triples.
     * Used for secondary angle validation (hip angle, etc.)
     */
    private fun calculateBestSideAngle(
        leftP1: PoseLandmark?, leftP2: PoseLandmark?, leftP3: PoseLandmark?,
        rightP1: PoseLandmark?, rightP2: PoseLandmark?, rightP3: PoseLandmark?
    ): Float? {
        val leftConf = minOf(
            leftP1?.inFrameLikelihood ?: 0f,
            leftP2?.inFrameLikelihood ?: 0f,
            leftP3?.inFrameLikelihood ?: 0f
        )
        val rightConf = minOf(
            rightP1?.inFrameLikelihood ?: 0f,
            rightP2?.inFrameLikelihood ?: 0f,
            rightP3?.inFrameLikelihood ?: 0f
        )

        val leftAngle = if (leftConf >= MIN_LANDMARK_CONFIDENCE) {
            calculateAngle(leftP1, leftP2, leftP3)
        } else null

        val rightAngle = if (rightConf >= MIN_LANDMARK_CONFIDENCE) {
            calculateAngle(rightP1, rightP2, rightP3)
        } else null

        return when {
            leftAngle != null && rightAngle != null -> (leftAngle + rightAngle) / 2f
            leftAngle != null -> leftAngle
            rightAngle != null -> rightAngle
            else -> null
        }
    }

    fun resetReps() {
        // Pushup state
        pushupRepCount = 0
        pushupPhase = RepPhase.UP
        pushupFramesInPhase = 0
        pushupConsecDown = 0
        pushupConsecUp = 0
        pushupConsecGoingDown = 0
        pushupConsecGoingUp = 0
        smoothedPushupAngle = null
        prevPushupAngle = null
        pushupVelocity = 0f
        lastPushupRepTime = 0L
        pushupAngleBuffer.clear()
        pushupMinAngleInRep = 180f
        pushupMaxAngleInRep = 0f
        // 3-signal depth baseline (re-established at next top)
        pushupShoulderTopY = null
        pushupNoseTopY = null
        pushupElbowRatioTop = null
        pushupBaselineStableFrames = 0

        // Squat state
        squatRepCount = 0
        squatPhase = RepPhase.UP
        squatFramesInPhase = 0
        squatConsecDown = 0
        squatConsecUp = 0
        squatConsecGoingDown = 0
        squatConsecGoingUp = 0
        lastSquatRepTime = 0L
        squatBaselineHipY = null
        squatBaselineHeadY = null
        squatBaselineStableFrames = 0
        squatSmoothedDepth = 0f
        squatPrevDepth = 0f
        squatVelocity = 0f
        squatBottomEnterTimeMs = 0L
        squatMinDepthInRep = 1f
        squatMaxDepthInRep = 0f

        // Plank state
        plankConsecCorrectFrames = 0
    }

    fun analyzePlankHoldPose(pose: Pose, imageWidth: Int, imageHeight: Int): PlankHoldMetrics {
        if (imageWidth <= 0 || imageHeight <= 0) {
            return PlankHoldMetrics(
                isCorrect = false,
                stabilityPercentage = 0,
                feedback = "Camera is not ready yet",
                confidence = 0f
            )
        }

        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)

        fun avgY(
            l: PoseLandmark?,
            r: PoseLandmark?
        ): Float? {
            val lc = (l?.inFrameLikelihood ?: 0f) >= MIN_LANDMARK_CONFIDENCE
            val rc = (r?.inFrameLikelihood ?: 0f) >= MIN_LANDMARK_CONFIDENCE
            return when {
                lc && rc -> (l!!.position.y + r!!.position.y) / 2f
                lc -> l!!.position.y
                rc -> r!!.position.y
                else -> null
            }
        }

        fun avgX(
            l: PoseLandmark?,
            r: PoseLandmark?
        ): Float? {
            val lc = (l?.inFrameLikelihood ?: 0f) >= MIN_LANDMARK_CONFIDENCE
            val rc = (r?.inFrameLikelihood ?: 0f) >= MIN_LANDMARK_CONFIDENCE
            return when {
                lc && rc -> (l!!.position.x + r!!.position.x) / 2f
                lc -> l!!.position.x
                rc -> r!!.position.x
                else -> null
            }
        }

        val shouldersY = avgY(leftShoulder, rightShoulder)
        val elbowsY = avgY(leftElbow, rightElbow)
        val elbowsX = avgX(leftElbow, rightElbow)
        val shouldersX = avgX(leftShoulder, rightShoulder)

        val hasValidPose = shouldersY != null &&
                elbowsY != null &&
                elbowsX != null &&
                shouldersX != null

        val confidence = listOfNotNull(
            leftShoulder?.inFrameLikelihood,
            rightShoulder?.inFrameLikelihood,
            leftElbow?.inFrameLikelihood,
            rightElbow?.inFrameLikelihood
        ).minOrNull() ?: 0f

        val plankMinConfidence = 0.5f  // lower than MIN_LANDMARK_CONFIDENCE so plank still registers in marginal conditions
        if (!hasValidPose || confidence < plankMinConfidence) {
            return PlankHoldMetrics(
                isCorrect = false,
                stabilityPercentage = 0,
                feedback = "Get shoulders and elbows in frame",
                confidence = confidence
            )
        }

        val refHeight = imageHeight * 0.6f
        if (refHeight <= 0f) {
            return PlankHoldMetrics(
                isCorrect = false,
                stabilityPercentage = 0,
                feedback = "Keep the camera steady",
                confidence = confidence
            )
        }

        // ---- Detect side / profile view: shoulder line is narrow in X (vs front-facing) ----
        val shoulderSpanXNorm = if (
            (leftShoulder?.inFrameLikelihood ?: 0f) >= MIN_LANDMARK_CONFIDENCE &&
            (rightShoulder?.inFrameLikelihood ?: 0f) >= MIN_LANDMARK_CONFIDENCE
        ) {
            abs(leftShoulder!!.position.x - rightShoulder!!.position.x) / imageWidth
        } else {
            1f // treat as not side; use front logic
        }

        val hipsY = avgY(leftHip, rightHip)
        val hipsOkForSide = hipsY != null &&
                ((leftHip?.inFrameLikelihood ?: 0f) >= plankMinConfidence ||
                        (rightHip?.inFrameLikelihood ?: 0f) >= plankMinConfidence)

        val useSideViewPlank = shoulderSpanXNorm <= PLANK_SIDE_SHOULDER_X_SPAN_MAX && hipsOkForSide

        if (useSideViewPlank) {
            return analyzePlankHoldPoseSideView(
                imageWidth = imageWidth,
                refHeight = refHeight,
                plankMinConfidence = plankMinConfidence,
                baseConfidence = confidence,
                leftShoulder = leftShoulder,
                rightShoulder = rightShoulder,
                leftElbow = leftElbow,
                rightElbow = rightElbow,
                leftHip = leftHip,
                rightHip = rightHip,
                leftWrist = leftWrist,
                rightWrist = rightWrist
            )
        }

        // ---- Front view: vertical spread between shoulders and elbows (upper body) ----
        val yVals = listOfNotNull(shouldersY, elbowsY)
        val minY = yVals.minOrNull() ?: shouldersY!!
        val maxY = yVals.maxOrNull() ?: shouldersY!!

        val ySpreadNorm = (maxY - minY) / refHeight

        val shouldersLevelNorm = if (
            (leftShoulder?.inFrameLikelihood ?: 0f) >= MIN_LANDMARK_CONFIDENCE &&
            (rightShoulder?.inFrameLikelihood ?: 0f) >= MIN_LANDMARK_CONFIDENCE
        ) {
            val lY = leftShoulder?.position?.y
            val rY = rightShoulder?.position?.y
            if (lY != null && rY != null) abs(lY - rY) / refHeight else 0f
        } else {
            0f
        }

        val elbowUnderNorm = abs(elbowsX!! - shouldersX!!) / imageWidth

        val penalty = maxOf(
            ySpreadNorm / PLANK_Y_SPREAD_THRESHOLD,
            shouldersLevelNorm / PLANK_SHOULDER_LEVEL_THRESHOLD,
            elbowUnderNorm / PLANK_ELBOW_X_DIFF_THRESHOLD
        )

        val stability = (1f - penalty).coerceIn(0f, 1f)
        val stabilityPercentage = (stability * 100f).toInt().coerceIn(0, 100)

        val strictCorrect = ySpreadNorm <= PLANK_Y_SPREAD_THRESHOLD &&
                shouldersLevelNorm <= PLANK_SHOULDER_LEVEL_THRESHOLD &&
                elbowUnderNorm <= PLANK_ELBOW_X_DIFF_THRESHOLD
        val isCorrectNow = strictCorrect || stabilityPercentage >= 50

        if (isCorrectNow) {
            plankConsecCorrectFrames++
        } else {
            plankConsecCorrectFrames = 0
        }

        val isCorrectConfirmed = plankConsecCorrectFrames >= PLANK_MIN_CONSEC_FRAMES

        val feedback = when {
            ySpreadNorm > PLANK_Y_SPREAD_THRESHOLD -> "Keep a straight line (no sagging)"
            shouldersLevelNorm > PLANK_SHOULDER_LEVEL_THRESHOLD -> "Keep shoulders level"
            elbowUnderNorm > PLANK_ELBOW_X_DIFF_THRESHOLD -> "Place elbows under shoulders"
            !isCorrectConfirmed -> "Hold steady..."
            else -> "Good plank! Hold steady"
        }

        return PlankHoldMetrics(
            isCorrect = isCorrectConfirmed,
            stabilityPercentage = stabilityPercentage,
            feedback = feedback,
            confidence = confidence
        )
    }

    /**
     * Forearm plank when the camera sees the user in profile: shoulder line is narrow in X.
     * Uses the more visible arm + shoulder–hip alignment (horizontal trunk) and forearm on floor.
     */
    private fun analyzePlankHoldPoseSideView(
        imageWidth: Int,
        refHeight: Float,
        plankMinConfidence: Float,
        baseConfidence: Float,
        leftShoulder: PoseLandmark?,
        rightShoulder: PoseLandmark?,
        leftElbow: PoseLandmark?,
        rightElbow: PoseLandmark?,
        leftHip: PoseLandmark?,
        rightHip: PoseLandmark?,
        leftWrist: PoseLandmark?,
        rightWrist: PoseLandmark?
    ): PlankHoldMetrics {
        fun chainConfidence(vararg lm: PoseLandmark?): Float {
            val vals = lm.mapNotNull { it?.inFrameLikelihood }
            return if (vals.isEmpty()) 0f else vals.minOrNull()!!
        }

        val leftChain = chainConfidence(leftShoulder, leftElbow, leftHip, leftWrist)
        val rightChain = chainConfidence(rightShoulder, rightElbow, rightHip, rightWrist)

        val useLeft = when {
            leftChain >= plankMinConfidence && rightChain < plankMinConfidence -> true
            rightChain >= plankMinConfidence && leftChain < plankMinConfidence -> false
            leftChain >= rightChain -> true
            else -> false
        }

        val shoulder = if (useLeft) leftShoulder else rightShoulder
        val elbow = if (useLeft) leftElbow else rightElbow
        val hip = if (useLeft) leftHip else rightHip
        val wrist = if (useLeft) leftWrist else rightWrist

        val sideConfidence = minOf(
            baseConfidence,
            listOfNotNull(
                shoulder?.inFrameLikelihood,
                elbow?.inFrameLikelihood,
                hip?.inFrameLikelihood
            ).minOrNull() ?: 0f
        )

        if (shoulder == null || elbow == null || hip == null ||
            shoulder.inFrameLikelihood < plankMinConfidence ||
            elbow.inFrameLikelihood < plankMinConfidence ||
            hip.inFrameLikelihood < plankMinConfidence
        ) {
            return PlankHoldMetrics(
                isCorrect = false,
                stabilityPercentage = 0,
                feedback = "Show your side — shoulders, hips, and arms in frame",
                confidence = sideConfidence
            )
        }

        val sY = shoulder.position.y
        val eY = elbow.position.y
        val hY = hip.position.y
        val sX = shoulder.position.x
        val eX = elbow.position.x

        // Horizontal trunk: shoulder and hip at similar height in profile
        val trunkDeltaNorm = abs(sY - hY) / refHeight

        // Forearms on ground: elbow below shoulder; wrist optional (helps when visible)
        val elbowDropNorm = (eY - sY) / refHeight
        val forearmYNorm = if (wrist != null && wrist.inFrameLikelihood >= plankMinConfidence) {
            abs(eY - wrist.position.y) / refHeight
        } else {
            0f
        }

        val elbowXDeltaNorm = abs(eX - sX) / imageWidth

        // Penalties (same spirit as front view)
        val trunkPenalty = trunkDeltaNorm / PLANK_SIDE_TRUNK_Y_THRESHOLD
        val elbowDropPenalty = when {
            elbowDropNorm < PLANK_SIDE_ELBOW_DROP_MIN ->
                (PLANK_SIDE_ELBOW_DROP_MIN - elbowDropNorm) / PLANK_SIDE_ELBOW_DROP_MIN.coerceAtLeast(1e-3f)
            elbowDropNorm > PLANK_SIDE_ELBOW_DROP_MAX ->
                (elbowDropNorm - PLANK_SIDE_ELBOW_DROP_MAX) / 0.25f // normalize excess drop
            else -> 0f
        }
        val xPenalty = elbowXDeltaNorm / PLANK_SIDE_ELBOW_X_ALIGN_MAX
        val forearmPenalty = if (forearmYNorm > 0f) {
            forearmYNorm / PLANK_SIDE_FOREARM_Y_THRESHOLD
        } else {
            0f
        }

        val penalty = maxOf(trunkPenalty, elbowDropPenalty, xPenalty, forearmPenalty)
        val stability = (1f - penalty).coerceIn(0f, 1f)
        val stabilityPercentage = (stability * 100f).toInt().coerceIn(0, 100)

        val strictTrunk = trunkDeltaNorm <= PLANK_SIDE_TRUNK_Y_THRESHOLD
        val strictElbowDrop = elbowDropNorm in PLANK_SIDE_ELBOW_DROP_MIN..PLANK_SIDE_ELBOW_DROP_MAX
        val strictX = elbowXDeltaNorm <= PLANK_SIDE_ELBOW_X_ALIGN_MAX
        val strictForearm = forearmYNorm == 0f || forearmYNorm <= PLANK_SIDE_FOREARM_Y_THRESHOLD

        val strictCorrect = strictTrunk && strictElbowDrop && strictX && strictForearm
        val isCorrectNow = strictCorrect || stabilityPercentage >= 50

        if (isCorrectNow) {
            plankConsecCorrectFrames++
        } else {
            plankConsecCorrectFrames = 0
        }

        val isCorrectConfirmed = plankConsecCorrectFrames >= PLANK_MIN_CONSEC_FRAMES

        val feedback = when {
            trunkDeltaNorm > PLANK_SIDE_TRUNK_Y_THRESHOLD ->
                "Straighten your body — hips level with shoulders"
            elbowDropNorm < PLANK_SIDE_ELBOW_DROP_MIN ->
                "Lower onto forearms — elbows under shoulders"
            elbowDropNorm > PLANK_SIDE_ELBOW_DROP_MAX ->
                "Raise hips slightly — keep a straight line"
            elbowXDeltaNorm > PLANK_SIDE_ELBOW_X_ALIGN_MAX ->
                "Stack elbows under shoulders (side view)"
            forearmYNorm > PLANK_SIDE_FOREARM_Y_THRESHOLD ->
                "Rest forearms flat on the floor"
            !isCorrectConfirmed -> "Hold steady..."
            else -> "Good plank! Hold steady"
        }

        return PlankHoldMetrics(
            isCorrect = isCorrectConfirmed,
            stabilityPercentage = stabilityPercentage,
            feedback = feedback,
            confidence = sideConfidence
        )
    }
}

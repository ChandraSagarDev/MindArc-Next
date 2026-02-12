package com.example.mindarc.domain

import kotlin.math.pow
import kotlin.math.sqrt

/**
 * A simple K-Nearest Neighbor classifier for Pose states.
 */
class KNearestNeighborClassifier(
    private val k: Int = 3
) {

    enum class PoseState {
        UP,
        DOWN,
        UNKNOWN
    }

    data class Sample(
        val features: FloatArray,
        val state: PoseState
    )

    private val trainingData = mutableListOf<Sample>()

    init {
        // Initialize with some synthetic data for UP and DOWN states
        // Features: [Elbow Angle (deg), Shoulder-Hip Vertical Diff (normalized)]
        
        // UP State: High elbow angle (~160-180), High vertical diff (shoulders above hips significantly if inclined)
        // or just rely heavily on elbow angle for pushups.
        
        // Let's assume normalized features:
        // 0: Elbow Angle (normalized 0..1, where 1.0 is 180 degrees)
        // 1: Shoulder relative height (0..1)
        
        // UP samples
        addSample(floatArrayOf(0.95f, 0.8f), PoseState.UP) // 171 deg
        addSample(floatArrayOf(0.90f, 0.8f), PoseState.UP) // 162 deg
        addSample(floatArrayOf(1.00f, 0.85f), PoseState.UP) // 180 deg
        
        // DOWN samples
        addSample(floatArrayOf(0.40f, 0.4f), PoseState.DOWN) // ~70 deg
        addSample(floatArrayOf(0.35f, 0.35f), PoseState.DOWN) // ~60 deg
        addSample(floatArrayOf(0.50f, 0.45f), PoseState.DOWN) // ~90 deg
    }
    
    fun addSample(features: FloatArray, state: PoseState) {
        trainingData.add(Sample(features, state))
    }

    fun classify(features: FloatArray): ClassificationResult {
        if (trainingData.isEmpty()) return ClassificationResult(PoseState.UNKNOWN, 0f)

        val distances = trainingData.map { sample ->
            val dist = euclideanDistance(features, sample.features)
            Pair(dist, sample.state)
        }.sortedBy { it.first }

        val nearest = distances.take(k)
        
        // weighted voting or simple majority
        val counts = nearest.groupingBy { it.second }.eachCount()
        val bestState = counts.maxByOrNull { it.value }?.key ?: PoseState.UNKNOWN
        
        // Calculate a "confidence" based on how many of the K neighbors agreed
        val confidence = (counts[bestState] ?: 0).toFloat() / k
        
        return ClassificationResult(bestState, confidence)
    }

    private fun euclideanDistance(f1: FloatArray, f2: FloatArray): Float {
        var sum = 0f
        for (i in f1.indices) {
            val diff = f1[i] - f2.getOrElse(i) { 0f }
            sum += diff.pow(2)
        }
        return sqrt(sum)
    }
    
    data class ClassificationResult(
        val state: PoseState,
        val confidence: Float
    )
}

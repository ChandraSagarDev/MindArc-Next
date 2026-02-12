package com.example.mindarc.domain

class PushUpRepCounter {
    
    private var currentState = KNearestNeighborClassifier.PoseState.UP
    private var repCount = 0
    private var previousState = KNearestNeighborClassifier.PoseState.UP
    
    // Threshold to confirm state change (debounce)
    private var consecutiveFrames = 0
    private val frameThreshold = 3

    fun processState(newState: KNearestNeighborClassifier.PoseState): Int {
        if (newState == KNearestNeighborClassifier.PoseState.UNKNOWN) return repCount
        
        if (newState == currentState) {
            consecutiveFrames++
        } else {
            consecutiveFrames = 0
            // If we have enough consistent frames, switch state
            // OR if new state is very confident (handled by caller passing refined state)
            
            // For responsiveness, we might want lower threshold or no debounce if confidence is high.
            // But simple transition logic for now:
            
            // Wait, logic above is slightly wrong for debouncing.
            // Let's just switch immediately for responsiveness and rely on the KNN stability.
            // A simple state machine: UP -> DOWN -> UP = +1
            
            if (previousState == KNearestNeighborClassifier.PoseState.DOWN && newState == KNearestNeighborClassifier.PoseState.UP) {
                repCount++
            }
            
            previousState = currentState
            currentState = newState
        }
        
        return repCount
    }
    
    fun reset() {
        repCount = 0
        currentState = KNearestNeighborClassifier.PoseState.UP
        previousState = KNearestNeighborClassifier.PoseState.UP
    }
    
    fun getCount(): Int = repCount
    fun getCurrentState(): KNearestNeighborClassifier.PoseState = currentState
}

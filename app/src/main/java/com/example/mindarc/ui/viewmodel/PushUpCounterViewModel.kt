package com.example.mindarc.ui.viewmodel

import android.util.Size
import androidx.lifecycle.ViewModel
import com.example.mindarc.domain.PoseAnalyzer
import com.google.mlkit.vision.pose.Pose
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class PushUpCounterState(
    val count: Int = 0,
    val formFeedback: String = "Position yourself in front of the camera",
    val isDetecting: Boolean = false,
    val depthPercentage: Int = 0,
    val isGoodForm: Boolean = false,
    val currentPose: Pose? = null,
    val imageSize: Size = Size(0, 0)
)

@HiltViewModel
class PushUpCounterViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow(PushUpCounterState())
    val state: StateFlow<PushUpCounterState> = _state.asStateFlow()

    fun updateMetrics(metrics: PoseAnalyzer.PushUpMetrics, pose: Pose?, size: Size) {
        _state.value = _state.value.copy(
            count = metrics.repCount,
            formFeedback = metrics.feedback,
            isDetecting = metrics.confidence > 0.5f,
            depthPercentage = metrics.depthPercentage,
            isGoodForm = metrics.isHorizontal,
            currentPose = pose,
            imageSize = size
        )
    }

    fun resetCount() {
        // Note: The actual repCount is currently held in the PoseAnalyzer instance 
        // within the PoseDetectionProcessor. In a full implementation, you might 
        // want to call processor.poseAnalyzer.resetReps()
        _state.value = PushUpCounterState()
    }
}

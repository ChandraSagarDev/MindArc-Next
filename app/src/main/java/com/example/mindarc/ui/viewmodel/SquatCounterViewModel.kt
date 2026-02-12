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

data class SquatCounterState(
    val count: Int = 0,
    val formFeedback: String = "Position yourself in front of the camera",
    val isDetecting: Boolean = false,
    val depthPercentage: Int = 0,
    val isGoodForm: Boolean = false,
    val currentPose: Pose? = null,
    val imageSize: Size = Size(0, 0)
)

@HiltViewModel
class SquatCounterViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow(SquatCounterState())
    val state: StateFlow<SquatCounterState> = _state.asStateFlow()

    fun updateMetrics(metrics: PoseAnalyzer.SquatMetrics, pose: Pose?, size: Size) {
        _state.value = _state.value.copy(
            count = metrics.repCount,
            formFeedback = metrics.feedback,
            isDetecting = metrics.confidence > 0.5f,
            depthPercentage = metrics.depthPercentage,
            isGoodForm = metrics.isUpright,
            currentPose = pose,
            imageSize = size
        )
    }

    fun resetCount() {
        _state.value = SquatCounterState()
    }
}

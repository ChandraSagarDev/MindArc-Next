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

data class PlankHoldCounterState(
    val isCorrect: Boolean = false,
    val stabilityPercentage: Int = 0,
    val formFeedback: String = "Position your forearms and torso in front of the camera",
    val currentPose: Pose? = null,
    val imageSize: Size = Size(0, 0)
)

@HiltViewModel
class PlankHoldCounterViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow(PlankHoldCounterState())
    val state: StateFlow<PlankHoldCounterState> = _state.asStateFlow()

    fun updateMetrics(metrics: PoseAnalyzer.PlankHoldMetrics, pose: Pose?, size: Size) {
        _state.value = _state.value.copy(
            isCorrect = metrics.isCorrect,
            stabilityPercentage = metrics.stabilityPercentage,
            formFeedback = metrics.feedback,
            currentPose = pose,
            imageSize = size
        )
    }

    fun reset() {
        _state.value = PlankHoldCounterState()
    }
}


package com.example.mindarc.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindarc.data.model.ActivityRecord
import com.example.mindarc.data.model.ActivityType
import com.example.mindarc.data.model.QuizQuestion
import com.example.mindarc.data.model.ReadingContent
import com.example.mindarc.data.model.ReadingReflection
import com.example.mindarc.data.repository.MindArcRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReadingViewModel @Inject constructor(private val repository: MindArcRepository) : ViewModel() {

    private val _readingContent = MutableStateFlow<ReadingContent?>(null)
    val readingContent: StateFlow<ReadingContent?> = _readingContent.asStateFlow()

    private val _quizQuestions = MutableStateFlow<List<QuizQuestion>>(emptyList())
    val quizQuestions: StateFlow<List<QuizQuestion>> = _quizQuestions.asStateFlow()

    init {
        loadRandomReadingContent()
    }

    private fun loadRandomReadingContent() {
        viewModelScope.launch {
            val content = repository.getRandomReadingContent()
            _readingContent.value = content
            content?.let {
                _quizQuestions.value = repository.getQuestionsForContent(it.id)
            }
        }
    }

    fun saveAppProvidedReading(
        durationMinutes: Int,
        isQuizPerfect: Boolean,
        hasLeftApp: Boolean
    ) {
        viewModelScope.launch {
            val effectivePerfect = isQuizPerfect && !hasLeftApp
            val unlockDuration = repository.calculateReadingUnlockDuration(durationMinutes, effectivePerfect)
            var points = repository.calculateReadingPoints(durationMinutes, effectivePerfect)

            // If the user left the app mid-session, void the Perfect Score bonus and apply the focus penalty.
            if (hasLeftApp) {
                points = (points * 0.7f).toInt()
            }

            val activityRecord = ActivityRecord(
                activityType = ActivityType.READING_APP_PROVIDED,
                pointsEarned = points,
                unlockDurationMinutes = unlockDuration,
                readingContentId = _readingContent.value?.id
            )
            val activityId = repository.insertActivity(activityRecord)
            repository.updateProgressAfterActivity(
                activity = activityRecord,
                actualReadingTime = durationMinutes,
                appProvidedLeftApp = hasLeftApp,
                appProvidedQuizPerfect = isQuizPerfect
            )
            // Unlock only when user explicitly redeems points (spendPointsToUnlock)
        }
    }

    fun saveUserProvidedReading(durationMinutes: Int, reflection: String, userReadingTitle: String? = null) {
        viewModelScope.launch {
            val points = repository.calculateReadingPoints(durationMinutes)
            val unlockDuration = repository.calculateReadingUnlockDuration(durationMinutes)
            val activityRecord = ActivityRecord(
                activityType = ActivityType.READING_USER_PROVIDED,
                pointsEarned = points,
                unlockDurationMinutes = unlockDuration,
                userReadingTitle = userReadingTitle ?: "User Provided Reading"
            )
            val activityId = repository.insertActivity(activityRecord)
            val reflectionObj = ReadingReflection(
                activityRecordId = activityId,
                question = "What did you read?",
                answer = reflection
            )
            repository.insertReflection(reflectionObj)
            repository.updateProgressAfterActivity(activityRecord)
            // Unlock only when user explicitly redeems points (spendPointsToUnlock)
        }
    }
}


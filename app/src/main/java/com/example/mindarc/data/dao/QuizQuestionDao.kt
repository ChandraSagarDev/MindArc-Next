package com.example.mindarc.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.mindarc.data.model.QuizQuestion
import kotlinx.coroutines.flow.Flow

@Dao
interface QuizQuestionDao {
    @Query("SELECT * FROM quiz_questions WHERE readingContentId = :contentId")
    suspend fun getQuestionsForContent(contentId: Long): List<QuizQuestion>

    @Query("SELECT * FROM quiz_questions WHERE readingContentId = :contentId ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomQuestionsForContent(contentId: Long, limit: Int = 3): List<QuizQuestion>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: QuizQuestion)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuizQuestion>)
}


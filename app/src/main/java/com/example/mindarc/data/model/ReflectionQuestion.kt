package com.example.mindarc.data.model

data class ReflectionQuestion(
    val question: String,
    val expectedKeywords: List<String> // Keywords that should appear in user's answer
)


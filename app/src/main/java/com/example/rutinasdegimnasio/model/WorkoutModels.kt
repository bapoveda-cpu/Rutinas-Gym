package com.example.rutinasdegimnasio.model

data class Exercise(
    val name: String,
    val description: String,
    val reps: String
)

data class ExerciseCategory(
    val id: Int,
    val title: String,
    val level: String,
    val exercises: List<Exercise>
)

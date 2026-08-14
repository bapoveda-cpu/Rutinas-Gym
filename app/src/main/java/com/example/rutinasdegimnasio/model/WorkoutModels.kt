package com.example.rutinasdegimnasio.model

data class Exercise(
    val id: Int = 0,
    val name: String,
    val description: String,
    val reps: String,
    val imageUri: String? = null // Guarda la ruta de la imagen
)

data class ExerciseCategory(
    val id: Int,
    val title: String,
    val level: String,
    val exercises: List<Exercise>
)

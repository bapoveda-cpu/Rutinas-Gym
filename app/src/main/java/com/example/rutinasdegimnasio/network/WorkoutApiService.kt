package com.example.rutinasdegimnasio.network

import com.example.rutinasdegimnasio.model.ExerciseCategory
import retrofit2.http.GET

interface WorkoutApiService {
    // Aquí se pone la ruta final de tu API, por ejemplo "workouts"
    @GET("workouts")
    suspend fun getWorkouts(): List<ExerciseCategory>
}

package com.example.rutinasdegimnasio.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    // Aquí pondrán la URL real cuando tengan el servidor listo
     private const val BASE_URL = "https://api.tu-servidor-gym.com/"

    val api: WorkoutApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WorkoutApiService::class.java)
    }
}

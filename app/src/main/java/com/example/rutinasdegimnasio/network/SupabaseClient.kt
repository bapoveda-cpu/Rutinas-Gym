package com.example.rutinasdegimnasio.network

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth

object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = "https://umulzdkgqbtwysaqhbrz.supabase.co",
        supabaseKey = "sb_secret_BNadfd1gGwe_e_zmXhIWpg_To9xO9EK"
    ) {
        // Instalamos el módulo de base de datos
        install(Postgrest)
        // Instalamos el módulo de autenticación (¡Esto es lo que faltaba!)
        install(Auth)
    }
}

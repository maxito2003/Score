package com.example.clubhome.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
object SupabaseClientManager {

    // Reemplazar con la URL y la ANON KEY de tu proyecto en Supabase
    private const val SUPABASE_URL = "https://ksrqhboslyakdyribqzr.supabase.co"
    private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImtzcnFoYm9zbHlha2R5cmlicXpyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTAyMTM0MDQsImV4cCI6MjEwNTc4OTQwNH0.H0UG--nHAXeEsKynh6jrbpzkGteIWYNl-GceKluVTwk"

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_KEY
        ) {
            install(Postgrest)
            install(Storage)
            install(Auth)
            install(Realtime)
        }
    }
}

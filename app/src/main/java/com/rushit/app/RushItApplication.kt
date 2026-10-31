package com.rushit.app

import android.app.Application
import com.rushit.app.data.auth.SupabaseAuthRepository
import com.rushit.app.data.order.LocalOrderRepository
import com.rushit.app.data.repository.SampleCatalogRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest

class RushItApplication : Application() {
    
    val supabaseClient: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
        }
    }

    val authRepository by lazy { SupabaseAuthRepository(this, supabaseClient) }
    // Replace this binding, not the screens, once the real schema and RLS are configured.
    val catalogRepository by lazy { SampleCatalogRepository() }
    // Orders are stored on this device only until an order backend exists.
    val orderRepository by lazy { LocalOrderRepository(this) }
}

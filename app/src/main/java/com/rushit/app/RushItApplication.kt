package com.rushit.app

import android.app.Application
import com.rushit.app.data.auth.SupabaseAuthRepository
import com.rushit.app.data.repository.SampleCatalogRepository

class RushItApplication : Application() {
    val authRepository by lazy { SupabaseAuthRepository(this, BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY) }
    // Replace this binding, not the screens, once the real schema and RLS are configured.
    val catalogRepository by lazy { SampleCatalogRepository() }
}

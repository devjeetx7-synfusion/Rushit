package com.rushit.app

import android.app.Application
import com.rushit.app.data.auth.AuthGateway
import com.rushit.app.data.auth.AuthRepository
import com.rushit.app.data.auth.EncryptedSessionManager
import com.rushit.app.data.auth.SecureSessionStore
import com.rushit.app.data.auth.SupabaseAuthGateway
import com.rushit.app.data.auth.SupabaseAuthRepository
import com.rushit.app.data.auth.UnconfiguredAuthGateway
import com.rushit.app.data.order.LocalOrderRepository
import com.rushit.app.data.repository.SampleCatalogRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest

class RushItApplication : Application() {

    /** True when the build was given a Supabase project to talk to. */
    val isSupabaseConfigured: Boolean
        get() = BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    val supabaseClient: SupabaseClient by lazy {
        createSupabaseClient(
            // Placeholders keep an unconfigured build from throwing during construction; the
            // unconfigured gateway below means no request is ever made with them.
            supabaseUrl = BuildConfig.SUPABASE_URL.ifBlank { UNCONFIGURED_URL },
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY.ifBlank { UNCONFIGURED_KEY },
        ) {
            defaultSerializer = SupabaseAuthGateway.serializer
            install(Auth) {
                // The SDK owns the session lifecycle (load, refresh, rotate, expire). RushIt only
                // changes where the serialized session lives: an Android Keystore encrypted store
                // instead of plaintext SharedPreferences.
                sessionManager = EncryptedSessionManager(SecureSessionStore(this@RushItApplication))
                autoLoadFromStorage = true
                autoSaveToStorage = true
                alwaysAutoRefresh = true
                // Pause auto-refresh while the app is backgrounded and resume on return.
                enableLifecycleCallbacks = true
            }
            install(Postgrest)
            // Used for the account-deletion Edge Function; the service-role key stays server-side.
            install(Functions)
        }
    }

    val authGateway: AuthGateway by lazy {
        if (isSupabaseConfigured) {
            SupabaseAuthGateway(supabaseClient)
        } else {
            UnconfiguredAuthGateway(SupabaseAuthRepository.Messages.SUPABASE_UNCONFIGURED)
        }
    }

    val authRepository: AuthRepository by lazy { SupabaseAuthRepository(authGateway) }

    // Replace this binding, not the screens, once the real schema and RLS are configured.
    val catalogRepository by lazy { SampleCatalogRepository() }

    // Orders are stored on this device only until an order backend exists.
    val orderRepository by lazy { LocalOrderRepository(this) }

    private companion object {
        const val UNCONFIGURED_URL = "https://rushit-not-configured.supabase.co"
        const val UNCONFIGURED_KEY = "not-configured"
    }
}

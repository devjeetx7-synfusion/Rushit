package com.rushit.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.rushit.app.data.auth.AuthGateway
import com.rushit.app.data.auth.AuthRepository
import com.rushit.app.data.auth.BackendTokenBridge
import com.rushit.app.data.auth.FirebaseAuthRepository
import com.rushit.app.data.auth.ProfileGateway
import com.rushit.app.data.auth.SupabaseProfileGateway
import com.rushit.app.data.auth.firebase.FirebaseAuthGateway
import com.rushit.app.data.auth.google.ForegroundActivityTracker
import com.rushit.app.data.auth.google.GoogleSignInClient
import com.rushit.app.data.auth.google.GoogleSignInCoordinator
import com.rushit.app.data.auth.supabase.SupabaseIdentityBridge
import com.rushit.app.data.order.LocalOrderRepository
import com.rushit.app.data.repository.SampleCatalogRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.MemorySessionManager
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest

/**
 * Process-wide wiring.
 *
 * Identity is Firebase Authentication; the database, storage and business data stay on Supabase.
 * The two are joined by [SupabaseIdentityBridge], which hands the current Firebase ID token to the
 * Supabase client so Row Level Security can authorize every request.
 */
class RushItApplication : Application() {

    /** True when the build was given a Supabase project to talk to. */
    val isSupabaseConfigured: Boolean
        get() = BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    /** True when this build was produced with a `google-services.json`. */
    val isFirebaseConfigured: Boolean
        get() = BuildConfig.FIREBASE_CONFIGURED

    /**
     * The Firebase Authentication entry point, or null when the build has no Firebase configuration.
     *
     * A null instance is not an error at start-up: the app reports its own unconfigured state on the
     * login screen instead of crashing. CI builds without `google-services.json` therefore still run.
     */
    val firebaseAuth: FirebaseAuth? by lazy {
        if (!isFirebaseConfigured) {
            null
        } else {
            // The Gradle plugin normally initializes the default app through FirebaseInitProvider;
            // calling initializeApp explicitly is idempotent and covers a plugin applied late.
            runCatching {
                FirebaseApp.initializeApp(this)
                FirebaseAuth.getInstance()
            }.getOrNull()
        }
    }

    val supabaseClient: SupabaseClient by lazy {
        createSupabaseClient(
            // Placeholders keep an unconfigured build from throwing during construction; the
            // unconfigured profile gateway below means no request is ever made with them.
            supabaseUrl = BuildConfig.SUPABASE_URL.ifBlank { UNCONFIGURED_URL },
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY.ifBlank { UNCONFIGURED_KEY },
        ) {
            defaultSerializer = SupabaseProfileGateway.serializer
            install(Auth) {
                // The Supabase Auth plugin is installed for exactly one reason: it is the only place
                // this SDK version reads the bearer token from. Supabase Auth itself is never used —
                // there is no sign-in, sign-up, refresh or OAuth call anywhere in the app. These
                // settings pin that down: nothing is loaded, saved, refreshed or persisted, and the
                // session exists only in memory while a Firebase token is imported into it.
                autoLoadFromStorage = false
                autoSaveToStorage = false
                alwaysAutoRefresh = false
                enableLifecycleCallbacks = false
                sessionManager = MemorySessionManager()
            }
            install(Postgrest)
        }
    }

    /** Hands the current Firebase ID token to the Supabase client (see [SupabaseIdentityBridge]). */
    val backendTokens: BackendTokenBridge by lazy { SupabaseIdentityBridge(supabaseClient.auth) }

    val profileGateway: ProfileGateway by lazy {
        SupabaseProfileGateway(if (isSupabaseConfigured) supabaseClient else null)
    }

    val foregroundActivityTracker = ForegroundActivityTracker()

    val authGateway: AuthGateway by lazy {
        FirebaseAuthGateway(
            firebaseAuth = firebaseAuth,
            activityProvider = { foregroundActivityTracker.activity },
        )
    }

    val authRepository: AuthRepository by lazy {
        FirebaseAuthRepository(
            auth = authGateway,
            profiles = profileGateway,
            tokens = backendTokens,
        )
    }

    /** One request owner shared by every recreated MainActivity in this process. */
    val googleSignIn: GoogleSignInClient by lazy {
        GoogleSignInCoordinator(
            activityProvider = { foregroundActivityTracker.activity },
            webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID,
        )
    }

    // Replace this binding, not the screens, once the real schema and RLS are configured.
    val catalogRepository by lazy { SampleCatalogRepository() }

    // Orders are stored on this device only until an order backend exists.
    val orderRepository by lazy { LocalOrderRepository(this) }

    private companion object {
        const val UNCONFIGURED_URL = "https://rushit-not-configured.supabase.co"
        const val UNCONFIGURED_KEY = "not-configured"
    }
}

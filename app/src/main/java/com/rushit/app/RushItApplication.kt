package com.rushit.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.rushit.app.data.auth.AuthGateway
import com.rushit.app.data.auth.AuthRepository
import com.rushit.app.data.auth.FirebaseAuthRepository
import com.rushit.app.data.auth.firebase.FirebaseAuthGateway
import com.rushit.app.data.auth.google.ForegroundActivityTracker
import com.rushit.app.data.auth.google.GoogleSignInClient
import com.rushit.app.data.auth.google.GoogleSignInCoordinator
import com.rushit.app.data.order.LocalOrderRepository
import com.rushit.app.data.repository.SampleCatalogRepository

/**
 * Process-wide wiring for RushIt Android Application.
 *
 * Firebase Authentication is configured for Phone OTP and Google Sign-In.
 */
class RushItApplication : Application() {

    val isFirebaseConfigured: Boolean
        get() = BuildConfig.FIREBASE_CONFIGURED

    val firebaseAuth: FirebaseAuth? by lazy {
        if (!isFirebaseConfigured) {
            null
        } else {
            runCatching {
                FirebaseApp.initializeApp(this)
                FirebaseAuth.getInstance()
            }.getOrNull()
        }
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
            context = this,
        )
    }

    val googleSignIn: GoogleSignInClient by lazy {
        GoogleSignInCoordinator(
            activityProvider = { foregroundActivityTracker.activity },
            webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID,
        )
    }

    val catalogRepository by lazy { SampleCatalogRepository() }

    val orderRepository by lazy { LocalOrderRepository(this) }
}

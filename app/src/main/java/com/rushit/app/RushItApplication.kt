package com.rushit.app

import android.app.Application
import com.rushit.app.data.auth.AuthRepository
import com.rushit.app.data.auth.LocalAuthRepository
import com.rushit.app.data.auth.google.GoogleSignInClient
import com.rushit.app.data.auth.google.LocalGoogleSignInClient
import com.rushit.app.data.order.LocalOrderRepository
import com.rushit.app.data.repository.SampleCatalogRepository

/**
 * Process-wide wiring for RushIt Android Application.
 *
 * All external backend infrastructure (Firebase Authentication and Supabase database/storage)
 * has been completely removed.
 *
 * Browsing, catalog, local cart, wishlist, and demo orders operate honestly on-device.
 */
class RushItApplication : Application() {

    val authRepository: AuthRepository by lazy {
        LocalAuthRepository()
    }

    val googleSignIn: GoogleSignInClient by lazy {
        LocalGoogleSignInClient()
    }

    val catalogRepository by lazy { SampleCatalogRepository() }

    val orderRepository by lazy { LocalOrderRepository(this) }
}

package com.rushit.app

import com.rushit.app.data.auth.AuthRepository
import com.rushit.app.data.auth.AuthResult
import com.rushit.app.data.auth.SessionResult
import com.rushit.app.ui.viewmodel.AuthEvent
import com.rushit.app.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun `failed OTP request stays on login and reports error`() = runTest {
        val repo = FakeAuth().apply { requestResult = AuthResult.Failure("SMS unavailable") }
        val vm = AuthViewModel(repo)
        advanceUntilIdle()
        vm.updatePhone("9876543210")
        vm.requestOtp()
        advanceUntilIdle()
        assertEquals("SMS unavailable", vm.state.value.error)
        assertFalse(vm.state.value.authenticated)
        assertFalse(vm.state.value.busy)
        assertEquals(1, repo.requests)
    }

    @Test fun `only successful request opens OTP and verification opens home`() = runTest {
        val repo = FakeAuth()
        val vm = AuthViewModel(repo)
        advanceUntilIdle()
        vm.updatePhone("9876543210")
        vm.requestOtp()
        advanceUntilIdle()
        assertEquals(AuthEvent.OpenOtp, vm.events.first())
        assertFalse(vm.state.value.authenticated)
        vm.updateOtp("123456")
        vm.verifyOtp()
        advanceUntilIdle()
        assertEquals(AuthEvent.OpenHome, vm.events.first())
        assertTrue(vm.state.value.authenticated)
    }

    @Test fun `in-flight OTP requests cannot be duplicated`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repo = FakeAuth().apply { waitForRequest = gate }
        val vm = AuthViewModel(repo)
        advanceUntilIdle()
        vm.updatePhone("9876543210")
        vm.requestOtp()
        vm.requestOtp()
        assertTrue(vm.state.value.busy)
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, repo.requests)
    }

    @Test fun `sample browsing never authenticates a user`() = runTest {
        val vm = AuthViewModel(FakeAuth())
        advanceUntilIdle()
        vm.browseSampleStore()
        advanceUntilIdle()
        assertEquals(AuthEvent.OpenHome, vm.events.first())
        assertTrue(vm.state.value.demo)
        assertFalse(vm.state.value.authenticated)
    }

    @Test fun `failed verification stays unauthenticated`() = runTest {
        val repo = FakeAuth().apply { verifyResult = AuthResult.Failure("Invalid code") }
        val vm = AuthViewModel(repo)
        advanceUntilIdle()
        vm.updatePhone("9876543210")
        vm.requestOtp()
        advanceUntilIdle()
        assertEquals(AuthEvent.OpenOtp, vm.events.first())
        vm.updateOtp("123456")
        vm.verifyOtp()
        advanceUntilIdle()
        assertEquals("Invalid code", vm.state.value.error)
        assertFalse(vm.state.value.authenticated)
    }

    @Test fun `restored session routes as authenticated`() = runTest {
        val vm = AuthViewModel(FakeAuth().apply { session = SessionResult.Authenticated })
        advanceUntilIdle()
        assertFalse(vm.state.value.initializing)
        assertTrue(vm.state.value.authenticated)
    }

    private class FakeAuth : AuthRepository {
        var requestResult: AuthResult = AuthResult.Success
        var verifyResult: AuthResult = AuthResult.Success
        var session: SessionResult = SessionResult.Unauthenticated()
        var waitForRequest: CompletableDeferred<Unit>? = null
        var requests = 0
        override suspend fun restoreSession() = session
        override suspend fun requestOtp(phone: String): AuthResult { requests++; waitForRequest?.await(); return requestResult }
        override suspend fun verifyOtp(phone: String, token: String): AuthResult = verifyResult
        override suspend fun signOut(): AuthResult = AuthResult.Success
    }
}

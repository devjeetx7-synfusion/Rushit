package com.rushit.app

import com.rushit.app.data.auth.AuthResponse
import com.rushit.app.data.auth.AuthResult
import com.rushit.app.data.auth.AuthTransport
import com.rushit.app.data.auth.SessionResult
import com.rushit.app.data.auth.SessionStore
import com.rushit.app.data.auth.StoredSession
import com.rushit.app.data.auth.SupabaseAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class SupabaseAuthRepositoryTest {
    private data class Request(val method: String, val path: String, val body: JSONObject?, val token: String?)
    private class MemoryStore(var session: StoredSession? = null) : SessionStore {
        override fun write(session: StoredSession) { this.session = session }
        override fun read() = session
        override fun clear() { session = null }
    }
    private class FakeTransport(var reply: (Request) -> AuthResponse) : AuthTransport {
        val requests = mutableListOf<Request>()
        override fun request(method: String, path: String, body: JSONObject?, token: String?): AuthResponse {
            val request = Request(method, path, body, token)
            requests += request
            return reply(request)
        }
    }
    private fun repository(store: MemoryStore, transport: FakeTransport) =
        SupabaseAuthRepository(store, "https://example.test", "public-test-key", transport)

    @Test fun `unconfigured authentication returns an error without making a request`() = runBlocking {
        val transport = FakeTransport { error("should not be called") }
        val result = SupabaseAuthRepository(MemoryStore(), "", "", transport).requestOtp("+919876543210")
        assertTrue(result is AuthResult.Failure)
        assertTrue(transport.requests.isEmpty())
    }

    @Test fun `OTP request uses phone endpoint and reports rate limits`() = runBlocking {
        val transport = FakeTransport { AuthResponse(200, "{}") }
        val repo = repository(MemoryStore(), transport)
        assertEquals(AuthResult.Success, repo.requestOtp("+919876543210"))
        val request = transport.requests.single()
        assertEquals("POST", request.method)
        assertEquals("/otp", request.path)
        assertEquals("+919876543210", request.body?.getString("phone"))
        assertEquals(true, request.body?.getBoolean("create_user"))
        transport.reply = { AuthResponse(429, "{}") }
        val failure = repo.requestOtp("+919876543210") as AuthResult.Failure
        assertTrue(failure.message.contains("Too many"))
    }

    @Test fun `verification saves a session only after the server accepts the code`() = runBlocking {
        val store = MemoryStore()
        val transport = FakeTransport { AuthResponse(400, "{}") }
        val repo = repository(store, transport)
        assertTrue(repo.verifyOtp("+919876543210", "123456") is AuthResult.Failure)
        assertNull(store.session)
        transport.reply = { AuthResponse(200, "{}") }
        assertTrue(repo.verifyOtp("+919876543210", "123456") is AuthResult.Failure)
        assertNull(store.session)
        transport.reply = { AuthResponse(200, """{"access_token":"access","refresh_token":"refresh"}""") }
        assertEquals(AuthResult.Success, repo.verifyOtp("+919876543210", "123456"))
        assertEquals(StoredSession("access", "refresh"), store.session)
        assertEquals("/verify", transport.requests.last().path)
        assertEquals("sms", transport.requests.last().body?.getString("type"))
        assertEquals("123456", transport.requests.last().body?.getString("token"))
    }

    @Test fun `expired access token is refreshed and securely replaced`() = runBlocking {
        val store = MemoryStore(StoredSession("old-access", "old-refresh"))
        val transport = FakeTransport { request ->
            if (request.path == "/user") AuthResponse(401, "{}")
            else AuthResponse(200, """{"access_token":"new-access","refresh_token":"new-refresh"}""")
        }
        assertEquals(SessionResult.Authenticated, repository(store, transport).restoreSession())
        assertEquals(StoredSession("new-access", "new-refresh"), store.session)
        assertEquals("old-access", transport.requests.first().token)
        assertEquals("old-refresh", transport.requests.last().body?.getString("refresh_token"))
    }

    @Test fun `transient restore failure retains session but expired refresh clears it`() = runBlocking {
        val store = MemoryStore(StoredSession("access", "refresh"))
        val transport = FakeTransport { throw IOException("offline") }
        val repo = repository(store, transport)
        assertTrue(repo.restoreSession() is SessionResult.Unauthenticated)
        assertNotNull(store.session)
        transport.reply = { if (it.path == "/user") AuthResponse(401, "{}") else AuthResponse(400, "{}") }
        assertTrue(repo.restoreSession() is SessionResult.Unauthenticated)
        assertNull(store.session)
    }

    @Test fun `cancelled restore cannot write a late refresh result`() = runBlocking {
        val old = StoredSession("old-access", "old-refresh")
        val store = MemoryStore(old)
        val refreshStarted = CountDownLatch(1)
        val release = CountDownLatch(1)
        val transport = FakeTransport { request ->
            if (request.path == "/user") AuthResponse(401, "{}") else {
                refreshStarted.countDown()
                check(release.await(3, TimeUnit.SECONDS))
                AuthResponse(200, """{"access_token":"late-access","refresh_token":"late-refresh"}""")
            }
        }
        val job = launch(Dispatchers.Default) { repository(store, transport).restoreSession() }
        assertTrue("refresh did not start", refreshStarted.await(3, TimeUnit.SECONDS))
        job.cancel()
        release.countDown()
        job.cancelAndJoin()
        assertEquals(old, store.session)
    }

    @Test fun `failed remote signout clears local session and reports revocation failure`() = runBlocking {
        val store = MemoryStore(StoredSession("access", "refresh"))
        val transport = FakeTransport { AuthResponse(503, "{}") }
        val result = repository(store, transport).signOut()
        assertTrue(result is AuthResult.Failure)
        assertNull(store.session)
        assertEquals("/logout", transport.requests.single().path)
        assertEquals("access", transport.requests.single().token)
    }
}

package com.rushit.app

import com.rushit.app.data.auth.EncryptedSessionManager
import com.rushit.app.data.auth.InMemorySessionStore
import io.github.jan.supabase.gotrue.user.UserInfo
import io.github.jan.supabase.gotrue.user.UserSession
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Session persistence through the SDK's own lifecycle.
 *
 * RushIt keeps the Supabase SDK in charge of refresh and expiry, and only changes where the
 * serialized session is kept. These tests cover the round-trip, the "corrupt payload means no
 * session" fallback, and that no plaintext token is left behind.
 */
class EncryptedSessionManagerTest {

    private fun session(
        accessToken: String = "access-token",
        refreshToken: String = "refresh-token",
    ) = UserSession(
        accessToken = accessToken,
        refreshToken = refreshToken,
        expiresIn = 3600L,
        tokenType = "bearer",
        user = UserInfo(
            aud = "authenticated",
            id = TestConfig.USER_A,
            email = "shopper@example.com",
        ),
    )

    @Test
    fun `a saved session is restored exactly, including its expiry`() = runTest {
        val store = InMemorySessionStore()
        val manager = EncryptedSessionManager(store)
        val saved = session()

        manager.saveSession(saved)
        val loaded = manager.loadSession()

        assertEquals(saved, loaded)
        assertEquals(saved.expiresAt, loaded?.expiresAt)
        assertEquals(TestConfig.USER_A, loaded?.user?.id)
    }

    @Test
    fun `the manager hands the store exactly one opaque payload to encrypt`() = runTest {
        // SecureSessionStore is the encryption boundary: it wraps whatever it is given in AES/GCM with
        // a non-exportable Android Keystore key, so no refresh token reaches SharedPreferences in
        // plaintext. That step needs a device Keystore and is not exercised on the JVM; what is
        // testable here is that the manager writes one payload and reads back the same session.
        val store = InMemorySessionStore()
        val manager = EncryptedSessionManager(store)
        // expiresAt is stamped when the session is created, so the very same instance has to be
        // compared against the restored one.
        val saved = session()

        manager.saveSession(saved)
        val payload = store.payload

        assertNotNull(payload)
        assertTrue("the payload must be the serialized session", payload!!.startsWith("{"))
        assertEquals(saved, manager.loadSession())
    }

    @Test
    fun `saving twice keeps only the newest session`() = runTest {
        val store = InMemorySessionStore()
        val manager = EncryptedSessionManager(store)

        manager.saveSession(session(refreshToken = "first"))
        manager.saveSession(session(refreshToken = "second"))

        assertEquals("second", manager.loadSession()?.refreshToken)
    }

    @Test
    fun `a corrupt payload is treated as no session instead of crashing`() = runTest {
        val store = InMemorySessionStore().apply { payload = "not a serialized session" }
        val manager = EncryptedSessionManager(store)

        assertNull(manager.loadSession())
    }

    @Test
    fun `a truncated payload is treated as no session`() = runTest {
        val store = InMemorySessionStore()
        val manager = EncryptedSessionManager(store)
        manager.saveSession(session())
        store.payload = store.payload!!.take(20)

        assertNull(manager.loadSession())
    }

    @Test
    fun `deleting a session removes it`() = runTest {
        val store = InMemorySessionStore()
        val manager = EncryptedSessionManager(store)
        manager.saveSession(session())

        manager.deleteSession()

        assertNull(manager.loadSession())
        assertNull(store.payload)
    }

    @Test
    fun `loading from an empty store returns null`() = runTest {
        assertNull(EncryptedSessionManager(InMemorySessionStore()).loadSession())
    }

    @Test
    fun `a session without a user object still round-trips`() = runTest {
        val store = InMemorySessionStore()
        val manager = EncryptedSessionManager(store)
        val anonymous = UserSession(
            accessToken = "access",
            refreshToken = "refresh",
            expiresIn = 60L,
            tokenType = "bearer",
            user = null,
        )

        manager.saveSession(anonymous)

        assertEquals(anonymous, manager.loadSession())
        assertNull(manager.loadSession()?.user)
    }

    @Test
    fun `the session serializer encodes defaults so expiry survives a restart`() {
        // encodeDefaults=false would drop UserSession.expiresAt and every restored session would look
        // already expired, silently logging everybody out at start-up.
        val encoded = EncryptedSessionManager.sessionJson.encodeToString(
            UserSession.serializer(),
            session(),
        )

        assertTrue(encoded.contains("expiresAt"))
        assertTrue(encoded.contains("access_token"))
        assertTrue(encoded.contains("refresh_token"))
    }

    @Test
    fun `the store contract the SDK relies on is write, read and clear`() {
        // Compile-time and behavioural guard: SecureSessionStore implements this same contract, and
        // session persistence breaks at runtime only if the contract drifts.
        val store: com.rushit.app.data.auth.SessionStore = InMemorySessionStore()

        store.write("payload")
        assertEquals("payload", store.read())
        store.clear()
        assertNull(store.read())
    }
}

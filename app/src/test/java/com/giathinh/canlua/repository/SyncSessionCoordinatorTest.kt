package com.giathinh.canlua.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** T05 — phiên bất đổi chủ: generation tăng khi đổi uid / logout. */
class SyncSessionCoordinatorTest {

    private val auth = mockk<FirebaseAuth>(relaxed = true)

    private fun setUser(uid: String?) {
        every { auth.currentUser } returns uid?.let {
            mockk<FirebaseUser> { every { this@mockk.uid } returns it }
        }
    }

    @Test
    fun `currentSession null when signed out`() {
        setUser(null)
        val coordinator = SyncSessionCoordinator(auth)
        assertNull(coordinator.currentSession())
    }

    @Test
    fun `session valid for same uid and generation`() {
        setUser("A")
        val coordinator = SyncSessionCoordinator(auth)
        val session = coordinator.currentSession()
        assertNotNull(session)
        assertTrue(coordinator.isValid(session!!))
    }

    @Test
    fun `switching uid invalidates previous session`() {
        setUser("A")
        val coordinator = SyncSessionCoordinator(auth)
        val sessionA = coordinator.currentSession()!!
        coordinator.onAuthChanged("B")
        setUser("B")
        assertFalse("Session A phải hết hiệu lực khi sang B", coordinator.isValid(sessionA))
        val sessionB = coordinator.currentSession()!!
        assertTrue(coordinator.isValid(sessionB))
    }

    @Test
    fun `invalidate on logout invalidates running session`() {
        setUser("A")
        val coordinator = SyncSessionCoordinator(auth)
        val session = coordinator.currentSession()!!
        coordinator.invalidate()
        assertFalse(coordinator.isValid(session))
    }

    @Test
    fun `same uid does not bump generation`() {
        setUser("A")
        val coordinator = SyncSessionCoordinator(auth)
        val session = coordinator.currentSession()!!
        coordinator.onAuthChanged("A")
        assertTrue("Token refresh cùng uid không đổi generation", coordinator.isValid(session))
    }
}
package com.giathinh.canlua.repository

import com.giathinh.canlua.data.firestore.FirestoreCard
import com.giathinh.canlua.data.model.Card
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * Tests cho [PullDedupResolver] — bảo vệ chống bug duplicate phiếu khi
 * logout/login trên cùng máy.
 *
 * Bug history (đã fix):
 *  - User push 5 phiếu lên cloud ở app version cũ (chưa có firestoreId field).
 *  - Update lên v13 → migration thêm firestoreId nullable, default NULL.
 *  - Logout → login → pull check `getByFirestoreId` → null cho mọi local card.
 *  - → Insert 5 phiếu mới → DB có 10 phiếu trùng nội dung.
 *
 * Fix: composite key fallback (ownerUid + date + name + totalWeight) khi
 * firestoreId không match.
 */
class PullDedupResolverTest {

    private val owner = "user-A"

    private fun fsCard(
        id: String = "fs-1",
        name: String = "Nông dân X",
        date: Long = 1_700_000_000_000L,
        totalWeight: Double = 1000.0,
        lastModified: Long = 1_700_000_000_000L
    ) = FirestoreCard(
        id = id,
        name = name,
        date = Date(date),
        totalWeight = totalWeight,
        userId = owner,
        lastModifiedMs = lastModified
    )

    private fun localCard(
        id: Long = 1L,
        name: String = "Nông dân X",
        date: Long = 1_700_000_000_000L,
        totalWeight: Double = 1000.0,
        firestoreId: String? = null,
        lastModified: Long = 1_700_000_000_000L
    ) = Card(
        id = id,
        ownerUid = owner,
        name = name,
        date = Date(date),
        totalWeight = totalWeight,
        firestoreId = firestoreId,
        lastModifiedMs = lastModified
    )

    @Test
    fun `firestoreId match and cloud newer - update local`() = runBlocking {
        val fs = fsCard(id = "fs-1", lastModified = 2000L)
        val local = localCard(id = 5L, firestoreId = "fs-1", lastModified = 1000L)

        val result = PullDedupResolver.resolve(fs, byFirestoreId = local) { null }

        assertTrue(result is PullDedupResolver.Action.Update)
        val update = result as PullDedupResolver.Action.Update
        assertEquals(5L, update.localId)
        assertEquals(PullDedupResolver.Reason.FIRESTORE_ID_MATCH_NEWER, update.reason)
    }

    @Test
    fun `firestoreId match but local newer - skip overwrite`() = runBlocking {
        val fs = fsCard(id = "fs-1", lastModified = 1000L)
        val local = localCard(id = 5L, firestoreId = "fs-1", lastModified = 2000L)

        val result = PullDedupResolver.resolve(fs, byFirestoreId = local) { null }

        assertTrue(result is PullDedupResolver.Action.Skip)
        assertEquals(5L, (result as PullDedupResolver.Action.Skip).localId)
    }

    @Test
    fun `firestoreId match with same lastModified - skip duplicate write`() = runBlocking {
        val fs = fsCard(id = "fs-1", lastModified = 1500L)
        val local = localCard(id = 5L, firestoreId = "fs-1", lastModified = 1500L)

        val result = PullDedupResolver.resolve(fs, byFirestoreId = local) { null }

        assertTrue(result is PullDedupResolver.Action.Skip)
    }

    @Test
    fun `no firestoreId match but composite match - update orphan and stamp fsId`() = runBlocking {
        val fs = fsCard(id = "fs-1", name = "Nông dân X", totalWeight = 1000.0)
        val orphan = localCard(id = 7L, firestoreId = null, name = "Nông dân X", totalWeight = 1000.0)

        val result = PullDedupResolver.resolve(fs, byFirestoreId = null) { orphan }

        assertTrue(result is PullDedupResolver.Action.Update)
        val update = result as PullDedupResolver.Action.Update
        assertEquals(7L, update.localId)
        assertEquals(PullDedupResolver.Reason.COMPOSITE_KEY_MATCH, update.reason)
    }

    @Test
    fun `no match anywhere - insert as new card`() = runBlocking {
        val fs = fsCard(id = "fs-new")

        val result = PullDedupResolver.resolve(fs, byFirestoreId = null) { null }

        assertTrue(result is PullDedupResolver.Action.Insert)
        assertEquals("fs-new", (result as PullDedupResolver.Action.Insert).fsCard.id)
    }

    @Test
    fun `blank firestoreId - skip without checks`() = runBlocking {
        var compositeLookupCalled = false
        val fs = fsCard(id = "")

        val result = PullDedupResolver.resolve(
            fs,
            byFirestoreId = null
        ) {
            compositeLookupCalled = true
            null
        }

        assertTrue(result is PullDedupResolver.Action.Skip)
        assertEquals(false, compositeLookupCalled)
    }

    @Test
    fun `regression - logout login does not duplicate cards synced before v13`() = runBlocking {
        val fs = fsCard(id = "fs-old", name = "A", totalWeight = 500.0)
        val orphan = localCard(id = 99L, firestoreId = null, name = "A", totalWeight = 500.0)

        val first = PullDedupResolver.resolve(fs, byFirestoreId = null) { orphan }
        assertTrue("Lần 1 phải UPDATE orphan, không insert", first is PullDedupResolver.Action.Update)

        val stamped = orphan.copy(firestoreId = "fs-old", lastModifiedMs = fs.lastModifiedMs)
        val second = PullDedupResolver.resolve(fs, byFirestoreId = stamped) { null }
        assertTrue("Lần 2 phải SKIP, không insert", second is PullDedupResolver.Action.Skip)
    }
}

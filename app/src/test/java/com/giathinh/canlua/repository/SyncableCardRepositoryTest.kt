package com.giathinh.canlua.repository

import androidx.room.withTransaction
import com.giathinh.canlua.data.dao.CardDao
import com.giathinh.canlua.data.dao.CardSyncStateDao
import com.giathinh.canlua.data.dao.DeletedCardDao
import com.giathinh.canlua.data.dao.MutationReceiptDao
import com.giathinh.canlua.data.dao.SyncOutboxDao
import com.giathinh.canlua.data.dao.TransactionDao
import com.giathinh.canlua.data.dao.WeightEntryDao
import com.giathinh.canlua.data.database.AppDatabase
import com.giathinh.canlua.data.model.Card
import com.giathinh.canlua.data.model.CardSyncState
import com.giathinh.canlua.data.model.DeletedCard
import com.giathinh.canlua.data.model.MutationReceipt
import com.giathinh.canlua.data.model.Transaction
import com.giathinh.canlua.data.model.TransactionType
import com.giathinh.canlua.data.model.WeightEntry
import com.giathinh.canlua.data.model.serialize
import com.giathinh.canlua.domain.CardError
import com.giathinh.canlua.domain.CardMutationResult
import com.giathinh.canlua.util.CccdCrypto
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date

class SyncableCardRepositoryTest {

    private val db = mockk<AppDatabase>(relaxed = true)
    private val cardDao = mockk<CardDao>(relaxed = true)
    private val weightEntryDao = mockk<WeightEntryDao>(relaxed = true)
    private val transactionDao = mockk<TransactionDao>(relaxed = true)
    private val deletedCardDao = mockk<DeletedCardDao>(relaxed = true)
    private val syncStateDao = mockk<CardSyncStateDao>(relaxed = true)
    private val receiptDao = mockk<MutationReceiptDao>(relaxed = true)
    private val outboxDao = mockk<SyncOutboxDao>(relaxed = true)
    private val authManager = mockk<AuthManager>(relaxed = true)
    private val syncManager = mockk<SyncManager>(relaxed = true)
    private val firebaseUser = mockk<FirebaseUser> { every { uid } returns TEST_UID }

    private lateinit var cardRepository: CardRepository
    private lateinit var syncableRepository: SyncableCardRepository

    @Before
    fun setUp() {
        mockkStatic("androidx.room.RoomDatabaseKt")
        coEvery { db.withTransaction<Any?>(any()) } coAnswers {
            @Suppress("UNCHECKED_CAST")
            val block = secondArg<suspend () -> Any?>()
            block()
        }
        every { authManager.currentUser } returns firebaseUser
        coEvery { syncStateDao.getBySyncId(any()) } returns null
        coEvery { receiptDao.get(any(), any()) } returns null
        coEvery { weightEntryDao.getWeightEntriesByCardIdSync(any()) } returns emptyList()
        coEvery { transactionDao.getTotalPaidAmountByCardId(any()) } returns 0.0
        coEvery { transactionDao.getTotalDepositAmountByCardId(any()) } returns 0.0

        cardRepository = CardRepository(
            db = db,
            cardDao = cardDao,
            weightEntryDao = weightEntryDao,
            transactionDao = transactionDao,
            deletedCardDao = deletedCardDao,
            authManager = authManager,
            syncStateDao = syncStateDao,
            receiptDao = receiptDao,
            outboxDao = outboxDao
        )
        syncableRepository = SyncableCardRepository(cardRepository, syncManager)
    }

    @After
    fun tearDown() {
        unmockkStatic("androidx.room.RoomDatabaseKt")
    }

    private fun sampleCard(
        id: Long = 10L,
        isLocked: Boolean = false,
        revision: Long = 1L,
        firestoreId: String? = "fs-10",
        syncId: String = "sync-10"
    ) = Card(
        id = id,
        ownerUid = TEST_UID,
        name = "Chú Ba",
        cccd = CccdCrypto.encrypt("082099111222", TEST_UID),
        date = Date(10_000L),
        pricePerKg = 8_000.0,
        isLocked = isLocked,
        firestoreId = firestoreId,
        revision = revision,
        syncId = syncId
    )

    @Test
    fun `createCardWithDeposit persists card, deposit, sync state, receipt and triggers sync`() = runTest {
        val insertedSlot = slot<Card>()
        coEvery { cardDao.insertCard(capture(insertedSlot)) } returns 10L
        coEvery { cardDao.getCardById(10L, TEST_UID) } answers {
            insertedSlot.captured.copy(id = 10L)
        }

        val input = Card(name = "Bác Tư", cccd = "082099111222", date = Date(10_000L), pricePerKg = 7_500.0)
        val result = syncableRepository.createCardWithDeposit(input, depositAmount = 2_000_000.0, operationId = "op-create-1")

        assertTrue(result is CardMutationResult.Success)
        val created = (result as CardMutationResult.Success).card
        assertEquals(10L, created.id)
        assertEquals("082099111222", created.cccd)
        assertTrue(insertedSlot.captured.cccd?.startsWith("v2:") == true)
        coVerify(exactly = 1) {
            transactionDao.insertTransaction(match { it.type == TransactionType.DEPOSIT && it.amount == 2_000_000.0 })
        }
        coVerify(exactly = 1) {
            syncStateDao.insert(
                match {
                    it.ownerUid == TEST_UID &&
                        it.localRevision == 1L &&
                        it.protocolState == CardSyncState.STATE_DIRTY
                }
            )
        }
        coVerify(exactly = 1) {
            receiptDao.insert(match { it.operationId == "op-create-1" && it.createdCardId == 10L })
        }
        coVerify(exactly = 1) { syncManager.refreshPendingSyncState() }
        verify(exactly = 1) { syncManager.scheduleImmediateSync() }
    }

    @Test
    fun `createCardWithDeposit idempotency replay returns cached success and conflict blocks sync`() = runTest {
        val existingCard = sampleCard(id = 10L).copy(name = "Bác Tư", pricePerKg = 7_500.0)
        coEvery { receiptDao.get(TEST_UID, "op-1") } returns MutationReceipt(
            ownerUid = TEST_UID,
            operationId = "op-1",
            commandHash = "create|Bác Tư|2000000.0|7500.0",
            createdCardId = 10L
        )
        coEvery { cardDao.getCardById(10L, TEST_UID) } returns existingCard

        val replay = syncableRepository.createCardWithDeposit(
            Card(name = "Bác Tư", date = Date(10_000L), pricePerKg = 7_500.0),
            depositAmount = 2_000_000.0,
            operationId = "op-1"
        )
        assertTrue(replay is CardMutationResult.Success)
        coVerify(exactly = 0) { cardDao.insertCard(any()) }

        // Same operationId with different payload -> Conflict, NO sync triggered
        val conflict = syncableRepository.createCardWithDeposit(
            Card(name = "Bác Tư", date = Date(10_000L), pricePerKg = 7_500.0),
            depositAmount = 500_000.0,
            operationId = "op-1"
        )
        assertEquals(CardMutationResult.Failure(CardError.Conflict), conflict)
        verify(exactly = 1) { syncManager.scheduleImmediateSync() } // Only from replay, NOT conflict
    }

    @Test
    fun `createCardWithDeposit invalid input returns Failure without triggering sync`() = runTest {
        val emptyNameResult = syncableRepository.createCardWithDeposit(
            Card(name = "  ", date = Date(0L)),
            depositAmount = 0.0,
            operationId = "op-bad-1"
        )
        val negativeDepositResult = syncableRepository.createCardWithDeposit(
            Card(name = "Hợp lệ", date = Date(0L)),
            depositAmount = -100.0,
            operationId = "op-bad-2"
        )

        assertTrue((emptyNameResult as CardMutationResult.Failure).error is CardError.InvalidInput)
        assertTrue((negativeDepositResult as CardMutationResult.Failure).error is CardError.InvalidInput)
        coVerify(exactly = 0) { cardDao.insertCard(any()) }
        coVerify(exactly = 0) { syncManager.refreshPendingSyncState() }
        verify(exactly = 0) { syncManager.scheduleImmediateSync() }
    }

    @Test
    fun `addPayment persists payment on unlocked card and blocks locked or conflicting requests`() = runTest {
        val unlocked = sampleCard(id = 10L, isLocked = false, revision = 1L, syncId = "sync-10")
        coEvery { cardDao.getCardById(10L, TEST_UID) } returns unlocked
        coEvery { syncStateDao.getBySyncId("sync-10") } returns CardSyncState("sync-10", TEST_UID, localRevision = 1L)

        val ok = syncableRepository.addPayment(10L, amount = 1_500_000.0, description = "Đợt 1", operationId = "pay-1")
        assertTrue(ok is CardMutationResult.Success)
        coVerify(exactly = 1) {
            transactionDao.insertTransaction(match { it.type == TransactionType.PAYMENT && it.amount == 1_500_000.0 })
        }
        coVerify(exactly = 1) { cardDao.bumpRevision(10L, TEST_UID, 2L, any()) }
        coVerify(exactly = 1) {
            syncStateDao.update(match { it.localRevision == 2L && it.protocolState == CardSyncState.STATE_DIRTY })
        }
        verify(exactly = 1) { syncManager.scheduleImmediateSync() }

        // Locked card -> Failure(Locked), NO additional sync scheduled
        coEvery { cardDao.getCardById(10L, TEST_UID) } returns unlocked.copy(isLocked = true)
        val lockedRes = syncableRepository.addPayment(10L, amount = 500_000.0, description = null, operationId = "pay-2")
        assertEquals(CardMutationResult.Failure(CardError.Locked), lockedRes)

        // Conflicting operationId -> Failure(Conflict), NO additional sync scheduled
        coEvery { receiptDao.get(TEST_UID, "pay-1") } returns MutationReceipt(
            ownerUid = TEST_UID,
            operationId = "pay-1",
            commandHash = "pay|10|1500000.0|Đợt 1",
            createdCardId = 10L
        )
        val conflictRes = syncableRepository.addPayment(10L, amount = 999_000.0, description = "Đợt 1", operationId = "pay-1")
        assertEquals(CardMutationResult.Failure(CardError.Conflict), conflictRes)
        verify(exactly = 1) { syncManager.scheduleImmediateSync() }
    }

    @Test
    fun `setCardLocked enforces CAS revision and only schedules sync on success`() = runTest {
        val current = sampleCard(id = 10L, isLocked = false, revision = 3L, syncId = "sync-10")
        coEvery { cardDao.getCardById(10L, TEST_UID) } returns current

        val stale = syncableRepository.setCardLocked(10L, expectedRevision = 2L, locked = true)
        assertEquals(CardMutationResult.Failure(CardError.StaleRevision), stale)
        coVerify(exactly = 0) { cardDao.updateCard(any()) }
        verify(exactly = 0) { syncManager.scheduleImmediateSync() }

        val success = syncableRepository.setCardLocked(10L, expectedRevision = 3L, locked = true)
        assertTrue(success is CardMutationResult.Success)
        coVerify(exactly = 1) { cardDao.updateCard(match { it.isLocked && it.revision == 4L }) }
        coVerify(exactly = 1) {
            syncStateDao.insert(match { it.localRevision == 4L && it.protocolState == CardSyncState.STATE_DIRTY })
        }
        verify(exactly = 1) { syncManager.scheduleImmediateSync() }
    }

    @Test
    fun `addWeightEntryAndRecalc triggers sync when valid and skips sync when null`() = runTest {
        val locked = sampleCard(id = 20L, isLocked = true)
        coEvery { cardDao.getCardById(20L, TEST_UID) } returns locked

        assertNull(syncableRepository.addWeightEntryAndRecalc(20L, 50.0))
        verify(exactly = 0) { syncManager.scheduleImmediateSync() }

        val unlocked = sampleCard(id = 21L, isLocked = false, revision = 1L)
        coEvery { cardDao.getCardById(21L, TEST_UID) } returns unlocked
        coEvery { weightEntryDao.getMaxOrdinal(21L) } returns 0
        coEvery { weightEntryDao.getWeightEntriesByCardIdSync(21L) } returns listOf(
            WeightEntry(id = 1L, cardId = 21L, weight = 50.5, ordinal = 1)
        )

        val bagCount = syncableRepository.addWeightEntryAndRecalc(21L, 50.5)
        assertEquals(1, bagCount)
        coVerify(exactly = 1) { syncManager.refreshPendingSyncState() }
        verify(exactly = 1) { syncManager.scheduleImmediateSync() }
    }

    @Test
    fun `deleteCard writes pending cloud tombstone and restoreFromTombstone recreates dirty card`() = runTest {
        val syncedCard = sampleCard(id = 30L, firestoreId = "fs-30", revision = 2L)
        syncableRepository.deleteCard(syncedCard)

        val tombSlot = slot<DeletedCard>()
        coVerify(exactly = 1) { deletedCardDao.insert(capture(tombSlot)) }
        assertEquals("fs-30", tombSlot.captured.firestoreId)
        assertEquals(false, tombSlot.captured.cloudDeleted)
        coVerify(exactly = 1) { cardDao.deleteCard(syncedCard) }
        coVerify(exactly = 1) { syncManager.refreshPendingSyncState() }
        verify(exactly = 1) { syncManager.scheduleImmediateSync() }

        // Test tombstone restoration atomicity & clean sync identity reset on underlying CardRepository
        val storedTomb = DeletedCard(
            id = 5L,
            ownerUid = TEST_UID,
            firestoreId = "fs-30",
            localId = 30L,
            cardJson = syncedCard.copy(cccd = "082099111222").serialize(),
            name = syncedCard.name,
            traderName = syncedCard.traderName,
            totalWeight = 0.0,
            totalAmount = 0.0,
            cardDate = syncedCard.date.time,
            seasonLabel = "",
            riceVariety = ""
        )
        coEvery { deletedCardDao.getById(5L) } returns storedTomb
        val restoredSlot = slot<Card>()
        coEvery { cardDao.insertCard(capture(restoredSlot)) } returns 31L

        val restoredId = cardRepository.restoreFromTombstone(5L)
        assertEquals(31L, restoredId)
        assertNull(restoredSlot.captured.firestoreId)
        assertEquals(1L, restoredSlot.captured.revision)
        assertTrue(restoredSlot.captured.cccd?.startsWith("v2:") == true)
        assertEquals("082099111222", CccdCrypto.decrypt(restoredSlot.captured.cccd, TEST_UID))
        coVerify(exactly = 1) { deletedCardDao.purge(5L) }
    }

    @Test
    fun `patchCard and editCardFinancials trigger sync only on valid mutations`() = runTest {
        val unlocked = sampleCard(id = 40L, isLocked = false, revision = 1L)
        coEvery { cardDao.getCardById(40L, TEST_UID) } returns unlocked

        val invalidMoisture = syncableRepository.editCardFinancials(
            unlocked.copy(moisturePercent = 150.0),
            operationId = "edit-bad"
        )
        assertTrue((invalidMoisture as CardMutationResult.Failure).error is CardError.InvalidInput)
        verify(exactly = 0) { syncManager.scheduleImmediateSync() }

        val validPatch = syncableRepository.patchCard(40L, expectedRevision = 1L, recalculate = false) {
            it.copy(name = "Chú Ba Mới")
        }
        assertTrue(validPatch is CardMutationResult.Success)
        coVerify(exactly = 1) { cardDao.updateCard(match { it.name == "Chú Ba Mới" && it.revision == 2L }) }
        verify(exactly = 1) { syncManager.scheduleImmediateSync() }
    }

    private companion object {
        const val TEST_UID = "user-123"
    }
}

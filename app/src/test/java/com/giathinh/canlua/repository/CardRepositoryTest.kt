package com.giathinh.canlua.repository

import com.giathinh.canlua.data.dao.CardDao
import com.giathinh.canlua.data.dao.TransactionDao
import com.giathinh.canlua.data.dao.WeightEntryDao
import com.giathinh.canlua.data.dao.DeletedCardDao
import com.giathinh.canlua.data.database.AppDatabase
import com.giathinh.canlua.data.model.Card
import com.giathinh.canlua.data.model.WeightEntry
import com.giathinh.canlua.data.model.Transaction
import com.giathinh.canlua.data.model.TransactionType
import com.giathinh.canlua.util.CccdCrypto
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import androidx.room.withTransaction
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date

class CardRepositoryTest {

    private val cardDao        = mockk<CardDao>(relaxed = true)
    private val weightEntryDao = mockk<WeightEntryDao>(relaxed = true)
    private val transactionDao = mockk<TransactionDao>(relaxed = true)
    private val deletedCardDao = mockk<DeletedCardDao>(relaxed = true)
    private val authManager    = mockk<AuthManager>(relaxed = true)
    private val db             = mockk<AppDatabase>(relaxed = true)

    private val firebaseUser = mockk<FirebaseUser> {
        every { uid } returns "user-123"
    }

    private val repository = CardRepository(
        db             = db,
        cardDao        = cardDao,
        weightEntryDao = weightEntryDao,
        transactionDao = transactionDao,
        deletedCardDao = deletedCardDao,
        authManager    = authManager
    )

    @Before
    fun setUp() {
        // withTransaction là suspend extension top-level trong androidx.room.
        // Dùng mockkStatic để stub: execute lambda ngay lập tức trong test coroutine.
        mockkStatic("androidx.room.RoomDatabaseKt")
        coEvery { db.withTransaction<Any?>(any()) } coAnswers {
            @Suppress("UNCHECKED_CAST")
            val block = secondArg<suspend () -> Any?>()
            block()
        }
    }

    @After
    fun tearDown() {
        unmockkStatic("androidx.room.RoomDatabaseKt")
    }

    @Test
    fun `insertCard should stamp ownerUid, lastModifiedMs and encrypt cccd`() = runTest {
        every { authManager.currentUser } returns firebaseUser
        
        val inputCard = Card(
            id = 0L,
            name = "Farmer A",
            cccd = "123456789",
            date = Date(1000L),
            ownerUid = "",
            lastModifiedMs = 0L
        )

        val cardSlot = slot<Card>()
        coEvery { cardDao.insertCard(capture(cardSlot)) } returns 42L

        val resultId = repository.insertCard(inputCard)

        assertEquals(42L, resultId)
        assertTrue(cardSlot.isCaptured)
        val captured = cardSlot.captured
        assertEquals("user-123", captured.ownerUid)
        assertTrue(captured.lastModifiedMs > 0L)
        // Verify CCCD is encrypted
        assertEquals(CccdCrypto.encrypt("123456789"), captured.cccd)
    }

    @Test
    fun `updateCard should update lastModifiedMs, encrypt cccd and preserve original transaction date`() = runTest {
        every { authManager.currentUser } returns firebaseUser
        
        val originalDate = Date(50000L)
        val inputCard = Card(
            id = 100L,
            name = "Farmer A",
            cccd = "123456789",
            date = originalDate,
            ownerUid = "user-123",
            lastModifiedMs = 10L
        )

        val cardSlot = slot<Card>()
        coEvery { cardDao.updateCard(capture(cardSlot)) } returns Unit

        repository.updateCard(inputCard)

        assertTrue(cardSlot.isCaptured)
        val captured = cardSlot.captured
        assertEquals(100L, captured.id)
        // Verify original date is preserved and NOT overwritten with System.currentTimeMillis()
        assertEquals(originalDate.time, captured.date.time)
        assertTrue(captured.lastModifiedMs > 10L)
        assertEquals(CccdCrypto.encrypt("123456789"), captured.cccd)
    }

    @Test
    fun `updateCardCalculations should update aggregates and preserve original transaction date`() = runTest {
        every { authManager.currentUser } returns firebaseUser

        val originalDate = Date(70000L)
        val originalCard = Card(
            id = 200L,
            name = "Farmer B",
            cccd = "987654321",
            date = originalDate,
            ownerUid = "user-123",
            lastModifiedMs = 10L,
            pricePerKg = 8000.0,
            moisturePercent = 15.0,
            bagWeight = 0.5      // 2 bao × 0.5 kg = 1.0 kg bì tổng
        )

        coEvery { cardDao.getCardById(200L, "user-123") } returns originalCard

        // 2 dòng cân: 100.5 + 99.5 = 200.0 kg thô
        val weightEntries = listOf(
            WeightEntry(id = 1L, cardId = 200L, weight = 100.5, bagWeight = 0.0, impurityWeight = 0.0, netWeight = 0.0, timestamp = 1000L),
            WeightEntry(id = 2L, cardId = 200L, weight = 99.5,  bagWeight = 0.0, impurityWeight = 0.0, netWeight = 0.0, timestamp = 2000L)
        )
        coEvery { weightEntryDao.getWeightEntriesByCardIdSync(200L) } returns weightEntries

        // RiceSettlement đọc paid/deposit trực tiếp từ transactionDao
        coEvery { transactionDao.getTotalPaidAmountByCardId(200L) }    returns 50000.0
        coEvery { transactionDao.getTotalDepositAmountByCardId(200L) } returns 10000.0

        val cardSlot = slot<Card>()
        coEvery { cardDao.updateCard(capture(cardSlot)) } returns Unit

        repository.updateCardCalculations(200L)

        // Entries phải được cập nhật bì/tạp chất/netWeight
        coVerify { weightEntryDao.updateWeightEntries(any()) }

        assertTrue(cardSlot.isCaptured)
        val captured = cardSlot.captured
        assertEquals(200L, captured.id)
        // Date gốc không bị ghi đè
        assertEquals(originalDate.time, captured.date.time)

        // totalWeight = tổng thô = 200.0 kg
        assertEquals(200.0, captured.totalWeight, 0.01)

        // RiceSettlement:
        //   raw = [1005, 995] units, totalRaw = 2000
        //   bag = 10 units tổng (2 × 5), chia [5, 5]
        //   gross = [1000, 990] units, totalGross = 1990
        //   moisture 15% → factor = 85/86 → totalNet = round(1990 × 85/86) = 1967
        //   netWeight = 196.7 kg
        assertEquals(196.7, captured.netWeight, 0.01)
        assertEquals(2, captured.bagCount)

        // amount = 1967 × 8000 / 10 = 1_573_600
        // remaining = 1_573_600 - 50_000 - 10_000 = 1_513_600
        assertEquals(1_573_600.0, captured.totalAmount, 1.0)
        assertEquals(1_513_600.0, captured.remainingAmount, 1.0)
        assertEquals(0.0, captured.overpaidAmount, 0.01)
    }
}

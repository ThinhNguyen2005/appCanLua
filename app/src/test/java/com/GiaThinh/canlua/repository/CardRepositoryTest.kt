package com.giathinh.canlua.repository

import com.giathinh.canlua.data.dao.CardDao
import com.giathinh.canlua.data.dao.TransactionDao
import com.giathinh.canlua.data.dao.WeightEntryDao
import com.giathinh.canlua.data.dao.DeletedCardDao
import com.giathinh.canlua.data.model.Card
import com.giathinh.canlua.data.model.WeightEntry
import com.giathinh.canlua.data.model.Transaction
import com.giathinh.canlua.data.model.TransactionType
import com.giathinh.canlua.util.CccdCrypto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class CardRepositoryTest {

    private val cardDao = mockk<CardDao>(relaxed = true)
    private val weightEntryDao = mockk<WeightEntryDao>(relaxed = true)
    private val transactionDao = mockk<TransactionDao>(relaxed = true)
    private val deletedCardDao = mockk<DeletedCardDao>(relaxed = true)
    private val cccdCrypto = mockk<CccdCrypto>()

    init {
        every { cccdCrypto.encrypt(any()) } answers { firstArg<String?>()?.let { "encrypted:$it" } }
    }

    private val repository = CardRepository(
        cardDao = cardDao,
        weightEntryDao = weightEntryDao,
        transactionDao = transactionDao,
        deletedCardDao = deletedCardDao,
        cccdCrypto = cccdCrypto
    )

    @Test
    fun `insertCard should stamp ownerUid, lastModifiedMs and encrypt cccd`() = runTest {
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
        assertEquals("", captured.ownerUid)
        assertTrue(captured.lastModifiedMs > 0L)
        // Verify CCCD is encrypted
        assertEquals("encrypted:123456789", captured.cccd)
    }

    @Test
    fun `updateCard should update lastModifiedMs, encrypt cccd and preserve original transaction date`() = runTest {
        val originalDate = Date(50000L)
        val inputCard = Card(
            id = 100L,
            name = "Farmer A",
            cccd = "123456789",
            date = originalDate,
            ownerUid = "",
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
        assertEquals("encrypted:123456789", captured.cccd)
    }

    @Test
    fun `updateCardCalculations should update aggregates and preserve original transaction date`() = runTest {
        val originalDate = Date(70000L)
        val originalCard = Card(
            id = 200L,
            name = "Farmer B",
            cccd = "987654321",
            date = originalDate,
            ownerUid = "",
            lastModifiedMs = 10L,
            pricePerKg = 8000.0,
            moisturePercent = 15.0,
            bagWeight = 0.5
        )

        // Mock database operations
        coEvery { cardDao.getCardById(200L, "") } returns originalCard
        
        // Mock weight entries
        val weightEntries = listOf(
            WeightEntry(id = 1L, cardId = 200L, weight = 100.5, bagWeight = 0.0, impurityWeight = 0.0, netWeight = 0.0, timestamp = 1000L),
            WeightEntry(id = 2L, cardId = 200L, weight = 99.5, bagWeight = 0.0, impurityWeight = 0.0, netWeight = 0.0, timestamp = 2000L)
        )
        coEvery { weightEntryDao.getWeightEntriesByCardIdSync(200L) } returns weightEntries
        
        // Mock totals from DAO (used by calculateCardTotals)
        coEvery { weightEntryDao.getTotalRawWeightByCardId(200L) } returns 200.0
        coEvery { weightEntryDao.getTotalNetWeightByCardId(200L) } returns 199.0
        coEvery { weightEntryDao.getBagCountByCardId(200L) } returns 2
        coEvery { transactionDao.getTotalPaidAmountByCardId(200L) } returns 50000.0
        coEvery { transactionDao.getTotalDepositAmountByCardId(200L) } returns 10000.0

        val cardSlot = slot<Card>()
        coEvery { cardDao.updateCard(capture(cardSlot)) } returns Unit

        repository.updateCardCalculations(200L)

        // Check if entries update is triggered
        coVerify { weightEntryDao.updateWeightEntries(any()) }

        // Check if cardDao.updateCard is called with preserved date
        assertTrue(cardSlot.isCaptured)
        val captured = cardSlot.captured
        assertEquals(200L, captured.id)
        // Verify original date is preserved
        assertEquals(originalDate.time, captured.date.time)
        assertEquals(200.0, captured.totalWeight, 0.01)
        // Net weight: raw (200) - bag (2*0.5=1.0) = 199.0 gross.
        // moisture = 15.0 -> net = 199.0 * (100 - 15) / (100 - 14) = 199.0 * 85 / 86 = 196.686 -> rounds to 196.7
        assertEquals(196.7, captured.netWeight, 0.01)
        assertEquals(2, captured.bagCount)
    }
}

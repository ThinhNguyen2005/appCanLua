package com.GiaThinh.canlua.repository

import android.content.Context
import com.GiaThinh.canlua.data.dao.CardDao
import com.GiaThinh.canlua.data.dao.TransactionDao
import com.GiaThinh.canlua.data.dao.WeightEntryDao
import com.GiaThinh.canlua.data.model.Card
import com.GiaThinh.canlua.data.model.DeletedCard
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncManagerTest {

    private val firestoreRepository = mockk<FirestoreRepository>(relaxed = true)
    private val cardRepository = mockk<CardRepository>(relaxed = true)
    private val cardDao = mockk<CardDao>(relaxed = true)
    private val weightEntryDao = mockk<WeightEntryDao>(relaxed = true)
    private val transactionDao = mockk<TransactionDao>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)
    private val auth = mockk<FirebaseAuth>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)

    private val syncManager = spyk(
        SyncManager(
            firestoreRepository = firestoreRepository,
            cardRepository = cardRepository,
            cardDao = cardDao,
            weightEntryDao = weightEntryDao,
            transactionDao = transactionDao,
            context = context,
            auth = auth,
            settingsRepository = settingsRepository
        )
    )

    @Test
    fun `refreshPendingSyncState should return true when there are unsynced cards`() = runTest {
        val mockUser = mockk<FirebaseUser> {
            every { uid } returns "user-123"
        }
        every { auth.currentUser } returns mockUser
        coEvery { cardDao.countUnsyncedCards("user-123") } returns 1
        coEvery { weightEntryDao.countUnsyncedWeightEntries() } returns 0
        coEvery { transactionDao.countUnsyncedTransactions() } returns 0
        coEvery { cardRepository.getPendingCloudDeletes("user-123") } returns emptyList()

        val hasPending = syncManager.refreshPendingSyncState()
        assertTrue(hasPending)
        assertTrue(syncManager.hasPendingSyncData.value)
    }

    @Test
    fun `refreshPendingSyncState should return true when there are pending cloud deletes`() = runTest {
        val mockUser = mockk<FirebaseUser> {
            every { uid } returns "user-123"
        }
        every { auth.currentUser } returns mockUser
        coEvery { cardDao.countUnsyncedCards("user-123") } returns 0
        coEvery { weightEntryDao.countUnsyncedWeightEntries() } returns 0
        coEvery { transactionDao.countUnsyncedTransactions() } returns 0
        coEvery { cardRepository.getPendingCloudDeletes("user-123") } returns listOf(
            DeletedCard(
                id = 1L,
                ownerUid = "user-123",
                firestoreId = "fs-1",
                localId = 10L,
                cardJson = "{}",
                name = "Farmer A",
                traderName = "Trader B",
                totalWeight = 100.0,
                totalAmount = 800000.0,
                cardDate = 1000L,
                seasonLabel = "Spring 2026",
                riceVariety = "OM5451"
            )
        )

        val hasPending = syncManager.refreshPendingSyncState()
        assertTrue(hasPending)
    }

    @Test
    fun `refreshPendingSyncState should return false when everything is synced`() = runTest {
        val mockUser = mockk<FirebaseUser> {
            every { uid } returns "user-123"
        }
        every { auth.currentUser } returns mockUser
        coEvery { cardDao.countUnsyncedCards("user-123") } returns 0
        coEvery { weightEntryDao.countUnsyncedWeightEntries() } returns 0
        coEvery { transactionDao.countUnsyncedTransactions() } returns 0
        coEvery { cardRepository.getPendingCloudDeletes("user-123") } returns emptyList()

        val hasPending = syncManager.refreshPendingSyncState()
        assertFalse(hasPending)
        assertFalse(syncManager.hasPendingSyncData.value)
    }

    @Test
    fun `syncAll should fail when currentUser is null`() = runTest {
        every { auth.currentUser } returns null

        val result = syncManager.syncAll()
        assertTrue(result.isFailure)
        assertTrue(syncManager.syncStatus.value is SyncStatus.Error)
    }

    @Test
    fun `syncAll should fail when canSync is false`() = runTest {
        val mockUser = mockk<FirebaseUser> {
            every { uid } returns "user-123"
        }
        every { auth.currentUser } returns mockUser
        every { syncManager.canSync() } returns false

        val result = syncManager.syncAll()
        assertTrue(result.isFailure)
        assertTrue(syncManager.syncStatus.value is SyncStatus.Error)
    }
}

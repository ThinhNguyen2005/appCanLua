package com.giathinh.canlua.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.giathinh.canlua.data.dao.CardDao
import com.giathinh.canlua.data.dao.TransactionDao
import com.giathinh.canlua.data.dao.WeightEntryDao
import com.giathinh.canlua.data.firestore.FirestoreCard
import com.giathinh.canlua.data.firestore.FirestoreTransaction
import com.giathinh.canlua.data.firestore.FirestoreWeightEntry
import com.giathinh.canlua.data.model.Card
import com.giathinh.canlua.data.model.DeletedCard
import com.giathinh.canlua.data.model.Transaction
import com.giathinh.canlua.data.model.TransactionType
import com.giathinh.canlua.data.model.WeightEntry
import com.giathinh.canlua.util.CccdCrypto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger

class SyncManagerTest {

    private val firestoreRepository = mockk<FirestoreRepository>(relaxed = true)
    private val cardRepository = mockk<CardRepository>(relaxed = true)
    private val cardDao = mockk<CardDao>(relaxed = true)
    private val weightEntryDao = mockk<WeightEntryDao>(relaxed = true)
    private val transactionDao = mockk<TransactionDao>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)
    private val connectivityManager = mockk<ConnectivityManager>(relaxed = true)
    private val auth = mockk<FirebaseAuth>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val mockUser = mockk<FirebaseUser>(relaxed = true) {
        every { uid } returns TEST_UID
        every { isAnonymous } returns false
    }

    private lateinit var sessionCoordinator: SyncSessionCoordinator
    private lateinit var syncManager: SyncManager

    @Before
    fun setUp() {
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { auth.currentUser } returns mockUser
        every { settingsRepository.isSyncOnlyWifi() } returns false
        setNetworkState(hasActiveNetwork = true, wifiOrUnmetered = true)

        coEvery { firestoreRepository.getAllCards() } returns Result.success(emptyList())
        coEvery { firestoreRepository.getAllWeightEntries() } returns Result.success(emptyList())
        coEvery { firestoreRepository.getAllTransactions() } returns Result.success(emptyList())
        coEvery { firestoreRepository.deleteWeightEntry(any()) } returns Result.success(Unit)
        coEvery { firestoreRepository.deleteTransaction(any()) } returns Result.success(Unit)
        coEvery { firestoreRepository.deleteCard(any()) } returns Result.success(Unit)
        coEvery { cardRepository.getAllCards() } returns flowOf(emptyList())
        coEvery { cardRepository.getPendingCloudDeletes(any()) } returns emptyList()
        coEvery { cardRepository.isCardTombstoned(any(), any()) } returns false

        sessionCoordinator = SyncSessionCoordinator(auth)
        syncManager = SyncManager(
            firestoreRepository = firestoreRepository,
            cardRepository = cardRepository,
            cardDao = cardDao,
            weightEntryDao = weightEntryDao,
            transactionDao = transactionDao,
            context = context,
            auth = auth,
            settingsRepository = settingsRepository,
            sessionCoordinator = sessionCoordinator
        )
    }

    private fun setNetworkState(
        hasActiveNetwork: Boolean,
        internet: Boolean = true,
        validated: Boolean = true,
        wifiOrUnmetered: Boolean = true
    ) {
        if (!hasActiveNetwork) {
            every { connectivityManager.activeNetwork } returns null
            every { connectivityManager.getNetworkCapabilities(any()) } returns null
            return
        }
        val network = mockk<Network>()
        val capabilities = mockk<NetworkCapabilities> {
            every { hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns internet
            every { hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns validated
            every { hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) } returns wifiOrUnmetered
            every { hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns wifiOrUnmetered
        }
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
    }

    private fun sampleTombstone(
        id: Long = 1L,
        firestoreId: String? = "fs-del-1"
    ) = DeletedCard(
        id = id,
        ownerUid = TEST_UID,
        firestoreId = firestoreId,
        localId = 10L,
        cardJson = "{}",
        name = "Farmer A",
        traderName = "Trader B",
        totalWeight = 100.0,
        totalAmount = 800_000.0,
        cardDate = 1_000L,
        seasonLabel = "Spring 2026",
        riceVariety = "OM5451"
    )

    @Test
    fun `canSync respects network validation and wifi-only preference`() {
        data class Case(
            val hasNetwork: Boolean,
            val validated: Boolean,
            val wifi: Boolean,
            val syncOnlyWifi: Boolean,
            val expected: Boolean
        )

        val cases = listOf(
            Case(hasNetwork = false, validated = false, wifi = false, syncOnlyWifi = false, expected = false),
            Case(hasNetwork = true, validated = false, wifi = true, syncOnlyWifi = false, expected = false),
            Case(hasNetwork = true, validated = true, wifi = false, syncOnlyWifi = true, expected = false),
            Case(hasNetwork = true, validated = true, wifi = false, syncOnlyWifi = false, expected = true),
            Case(hasNetwork = true, validated = true, wifi = true, syncOnlyWifi = true, expected = true)
        )

        cases.forEachIndexed { index, c ->
            every { settingsRepository.isSyncOnlyWifi() } returns c.syncOnlyWifi
            setNetworkState(
                hasActiveNetwork = c.hasNetwork,
                internet = c.hasNetwork,
                validated = c.validated,
                wifiOrUnmetered = c.wifi
            )
            assertEquals("Failed case #$index: $c", c.expected, syncManager.canSync())
        }
    }

    @Test
    fun `refreshPendingSyncState detects unsynced local data or tombstones across all sources`() = runTest {
        every { auth.currentUser } returns null
        assertFalse(syncManager.refreshPendingSyncState())

        every { auth.currentUser } returns mockUser
        data class Case(
            val cards: Int,
            val entries: Int,
            val txs: Int,
            val tombstones: Int,
            val expected: Boolean
        )

        val cases = listOf(
            Case(cards = 0, entries = 0, txs = 0, tombstones = 0, expected = false),
            Case(cards = 1, entries = 0, txs = 0, tombstones = 0, expected = true),
            Case(cards = 0, entries = 2, txs = 0, tombstones = 0, expected = true),
            Case(cards = 0, entries = 0, txs = 1, tombstones = 0, expected = true),
            Case(cards = 0, entries = 0, txs = 0, tombstones = 1, expected = true)
        )

        cases.forEachIndexed { index, c ->
            coEvery { cardDao.countUnsyncedCards(TEST_UID) } returns c.cards
            coEvery { weightEntryDao.countUnsyncedWeightEntries() } returns c.entries
            coEvery { transactionDao.countUnsyncedTransactions() } returns c.txs
            val pending = if (c.tombstones > 0) listOf(sampleTombstone()) else emptyList()
            coEvery { cardRepository.getPendingCloudDeletes(TEST_UID) } returns pending

            val result = syncManager.refreshPendingSyncState()
            assertEquals("Case #$index failed", c.expected, result)
            assertEquals("StateFlow mismatch at #$index", c.expected, syncManager.hasPendingSyncData.value)
        }
    }

    @Test
    fun `syncAll fails fast when signed out or network condition is unmet`() = runTest {
        every { auth.currentUser } returns null
        val signedOutResult = syncManager.syncAll()
        assertTrue(signedOutResult.isFailure)
        assertTrue(syncManager.syncStatus.value is SyncStatus.Error)

        every { auth.currentUser } returns mockUser
        every { settingsRepository.isSyncOnlyWifi() } returns true
        setNetworkState(hasActiveNetwork = true, wifiOrUnmetered = false)

        val noWifiResult = syncManager.syncAll()
        assertTrue(noWifiResult.isFailure)
        val status = syncManager.syncStatus.value as SyncStatus.Error
        assertTrue(status.message.contains("Wi-Fi"))
        coVerify(exactly = 0) { firestoreRepository.getAllCards() }
    }

    @Test
    fun `syncAll splits dirty cards across batches when combined ops exceed 400`() = runTest {
        val card1 = Card(id = 1L, ownerUid = TEST_UID, name = "Phiếu 1", date = Date(1_000L), lastModifiedMs = 100L)
        val card2 = Card(id = 2L, ownerUid = TEST_UID, name = "Phiếu 2", date = Date(2_000L), lastModifiedMs = 200L)
        val entries1 = (1..250).map { WeightEntry(id = it.toLong(), cardId = 1L, weight = 50.0) }
        val entries2 = (251..450).map { WeightEntry(id = it.toLong(), cardId = 2L, weight = 52.0) }

        coEvery { cardRepository.getAllCards() } returns flowOf(listOf(card1, card2))
        every { cardRepository.getWeightEntriesByCardId(1L) } returns flowOf(entries1)
        every { cardRepository.getWeightEntriesByCardId(2L) } returns flowOf(entries2)
        every { cardRepository.getTransactionsByCardId(any()) } returns flowOf(emptyList())

        val cardIdSeq = AtomicInteger(0)
        val entryIdSeq = AtomicInteger(0)
        every { firestoreRepository.generateCardId() } answers { "fs-card-${cardIdSeq.incrementAndGet()}" }
        every { firestoreRepository.generateWeightEntryId() } answers { "fs-entry-${entryIdSeq.incrementAndGet()}" }

        val capturedBatches = mutableListOf<Pair<Int, Int>>()
        coEvery {
            firestoreRepository.executeBatchSync(any(), any(), any(), any(), any())
        } answers {
            val batchCards = firstArg<List<FirestoreCard>>()
            val batchEntries = secondArg<List<FirestoreWeightEntry>>()
            capturedBatches.add(batchCards.size to batchEntries.size)
            Result.success(Unit)
        }

        val result = syncManager.syncAll()

        assertTrue(result.isSuccess)
        assertEquals(SyncStatus.Success, syncManager.syncStatus.value)
        assertNotNull(syncManager.lastSyncTime.value)
        assertEquals(listOf(1 to 250, 1 to 200), capturedBatches)
        coVerify(exactly = 2) { cardDao.updateFirestoreId(any(), any()) }
        coVerify(exactly = 450) { weightEntryDao.updateFirestoreId(any(), any()) }
    }

    @Test
    fun `syncAll deletes orphaned remote children removed locally on dirty card`() = runTest {
        val localCard = Card(
            id = 10L,
            ownerUid = TEST_UID,
            firestoreId = "fs-10",
            name = "Phiếu A",
            date = Date(1_000L),
            lastModifiedMs = 5_000L
        )
        val localEntry = WeightEntry(id = 1L, cardId = 10L, firestoreId = "entry-keep", weight = 48.5)
        val localTx = Transaction(id = 2L, cardId = 10L, firestoreId = "tx-keep", amount = 500_000.0, type = TransactionType.DEPOSIT)

        coEvery { firestoreRepository.getAllCards() } returns Result.success(
            listOf(FirestoreCard(id = "fs-10", name = "Phiếu A", lastModifiedMs = 1_000L))
        )
        coEvery { cardRepository.getAllCards() } returns flowOf(listOf(localCard))
        every { cardRepository.getWeightEntriesByCardId(10L) } returns flowOf(listOf(localEntry))
        every { cardRepository.getTransactionsByCardId(10L) } returns flowOf(listOf(localTx))
        coEvery { firestoreRepository.getWeightEntriesByCardId("fs-10") } returns Result.success(
            listOf(
                FirestoreWeightEntry(id = "entry-keep", cardId = "fs-10"),
                FirestoreWeightEntry(id = "entry-orphan", cardId = "fs-10")
            )
        )
        coEvery { firestoreRepository.getTransactionsByCardId("fs-10") } returns Result.success(
            listOf(
                FirestoreTransaction(id = "tx-keep", cardId = "fs-10", type = "DEPOSIT"),
                FirestoreTransaction(id = "tx-orphan", cardId = "fs-10", type = "PAYMENT")
            )
        )
        coEvery { firestoreRepository.executeBatchSync(any(), any(), any(), any(), any()) } returns Result.success(Unit)

        val result = syncManager.syncAll()

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { firestoreRepository.deleteWeightEntry("entry-orphan") }
        coVerify(exactly = 0) { firestoreRepository.deleteWeightEntry("entry-keep") }
        coVerify(exactly = 1) { firestoreRepository.deleteTransaction("tx-orphan") }
        coVerify(exactly = 0) { firestoreRepository.deleteTransaction("tx-keep") }
    }

    @Test
    fun `syncAll handles total failure, partial failure, and stale session`() = runTest {
        val card1 = Card(id = 1L, ownerUid = TEST_UID, name = "Phiếu 1", date = Date(1_000L), lastModifiedMs = 100L)
        val card2 = Card(id = 2L, ownerUid = TEST_UID, name = "Phiếu 2", date = Date(2_000L), lastModifiedMs = 200L)
        every { cardRepository.getTransactionsByCardId(any()) } returns flowOf(emptyList())
        every { firestoreRepository.generateCardId() } returnsMany listOf("fs-1", "fs-2", "fs-3", "fs-4")
        val entrySeq = AtomicInteger(0)
        every { firestoreRepository.generateWeightEntryId() } answers { "e-${entrySeq.incrementAndGet()}" }

        // Case 1: All cards fail
        coEvery { cardRepository.getAllCards() } returns flowOf(listOf(card1))
        every { cardRepository.getWeightEntriesByCardId(1L) } returns flowOf(emptyList())
        coEvery { firestoreRepository.executeBatchSync(any(), any(), any(), any(), any()) } returns
            Result.failure(RuntimeException("Network error"))

        val allFailed = syncManager.syncAll()
        assertTrue(allFailed.isFailure)
        assertEquals(
            SyncStatus.Error("Không thể đồng bộ dữ liệu. Vui lòng thử lại."),
            syncManager.syncStatus.value
        )

        // Case 2: Partial failure (batch 1 succeeds, batch 2 fails -> SyncStatus.Success)
        val entries1 = (1..250).map { WeightEntry(id = it.toLong(), cardId = 1L, weight = 50.0) }
        val entries2 = (251..450).map { WeightEntry(id = it.toLong(), cardId = 2L, weight = 50.0) }
        coEvery { cardRepository.getAllCards() } returns flowOf(listOf(card1, card2))
        every { cardRepository.getWeightEntriesByCardId(1L) } returns flowOf(entries1)
        every { cardRepository.getWeightEntriesByCardId(2L) } returns flowOf(entries2)
        coEvery { firestoreRepository.executeBatchSync(any(), any(), any(), any(), any()) } returnsMany listOf(
            Result.success(Unit),
            Result.failure(RuntimeException("Second batch failed"))
        )

        val partial = syncManager.syncAll()
        assertTrue(partial.isSuccess)
        assertEquals(SyncStatus.Success, syncManager.syncStatus.value)

        // Case 3: Stale session detected before pull phase
        coEvery { cardRepository.getAllCards() } returns flowOf(emptyList())
        coEvery { firestoreRepository.getAllCards() } coAnswers {
            sessionCoordinator.onAuthChanged("other-uid")
            Result.success(emptyList())
        }
        val staleResult = syncManager.syncAll()
        assertTrue(staleResult.isFailure)
        assertEquals(SyncStatus.Error("Phiên đăng nhập đã thay đổi"), syncManager.syncStatus.value)
    }

    @Test
    fun `pullAllForCurrentUser acks tombstone only when all cloud deletes succeed`() = runTest {
        val tomb = sampleTombstone(id = 99L, firestoreId = "fs-del-99")
        coEvery { cardRepository.getPendingCloudDeletes(TEST_UID) } returns listOf(tomb)
        coEvery { firestoreRepository.getAllWeightEntries() } returns Result.success(
            listOf(FirestoreWeightEntry(id = "e-99", cardId = "fs-del-99"))
        )
        coEvery { firestoreRepository.getAllTransactions() } returns Result.success(
            listOf(FirestoreTransaction(id = "t-99", cardId = "fs-del-99", type = "PAYMENT"))
        )

        // Failure case: deleteCard fails -> tombstone MUST NOT be acked
        coEvery { firestoreRepository.deleteCard("fs-del-99") } returns Result.failure(RuntimeException("Denied"))
        syncManager.pullAllForCurrentUser()
        coVerify(exactly = 0) { cardRepository.markTombstoneCloudDeleted(99L) }

        // Success case on retry: all deletes succeed -> tombstone IS acked once
        coEvery { firestoreRepository.deleteCard("fs-del-99") } returns Result.success(Unit)
        val result = syncManager.pullAllForCurrentUser()
        assertTrue(result.isSuccess)
        coVerify(exactly = 2) { firestoreRepository.deleteWeightEntry("e-99") }
        coVerify(exactly = 2) { firestoreRepository.deleteTransaction("t-99") }
        coVerify(exactly = 2) { firestoreRepository.deleteCard("fs-del-99") }
        coVerify(exactly = 1) { cardRepository.markTombstoneCloudDeleted(99L) }
    }

    @Test
    fun `pullAllForCurrentUser resolves Insert, Update, Skip, and Tombstone with CCCD encryption`() = runTest {
        val remoteInsert = FirestoreCard(id = "fs-new", name = "Mới", cccd = "082099123456", date = Date(10_000L), lastModifiedMs = 5_000L)
        val remoteUpdate = FirestoreCard(id = "fs-upd", name = "Cloud mới", date = Date(20_000L), lastModifiedMs = 8_000L)
        val remoteSkip = FirestoreCard(id = "fs-skip", name = "Cloud cũ", date = Date(30_000L), lastModifiedMs = 2_000L)
        val remoteTomb = FirestoreCard(id = "fs-tomb", name = "Đã xóa", date = Date(40_000L), lastModifiedMs = 9_000L)

        coEvery { firestoreRepository.getAllCards() } returns Result.success(
            listOf(remoteInsert, remoteUpdate, remoteSkip, remoteTomb)
        )
        coEvery { cardRepository.isCardTombstoned(TEST_UID, "fs-tomb") } returns true
        coEvery { cardDao.getByFirestoreId("fs-new") } returns null
        coEvery { cardDao.findOrphanFirestoreMatch(any(), any(), any(), any()) } returns null
        coEvery { cardDao.getByFirestoreId("fs-upd") } returns Card(
            id = 20L, ownerUid = TEST_UID, firestoreId = "fs-upd", name = "Local cũ", date = Date(20_000L), lastModifiedMs = 4_000L
        )
        coEvery { cardDao.getByFirestoreId("fs-skip") } returns Card(
            id = 30L, ownerUid = TEST_UID, firestoreId = "fs-skip", name = "Local mới", date = Date(30_000L), lastModifiedMs = 7_000L
        )
        val insertedCardSlot = slot<Card>()
        coEvery { cardDao.insertCard(capture(insertedCardSlot)) } returns 11L

        val result = syncManager.pullAllForCurrentUser()

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { cardDao.insertCard(any()) }
        assertTrue(insertedCardSlot.captured.cccd?.startsWith("v2:") == true)
        assertEquals("082099123456", CccdCrypto.decrypt(insertedCardSlot.captured.cccd, TEST_UID))
        coVerify(exactly = 1) { cardDao.updateCard(match { it.id == 20L && it.name == "Cloud mới" }) }
        coVerify(exactly = 0) { cardDao.updateCard(match { it.id == 30L }) }
    }

    @Test
    fun `pullAllForCurrentUser replaces sorted children only for cloud-winning cards`() = runTest {
        val remoteInsert = FirestoreCard(id = "fs-new", name = "Mới", date = Date(10_000L), lastModifiedMs = 5_000L)
        val remoteSkip = FirestoreCard(id = "fs-skip", name = "Cũ", date = Date(30_000L), lastModifiedMs = 2_000L)
        coEvery { firestoreRepository.getAllCards() } returns Result.success(listOf(remoteInsert, remoteSkip))
        coEvery { cardDao.getByFirestoreId("fs-new") } returns null
        coEvery { cardDao.findOrphanFirestoreMatch(any(), any(), any(), any()) } returns null
        coEvery { cardDao.getByFirestoreId("fs-skip") } returns Card(
            id = 30L, ownerUid = TEST_UID, firestoreId = "fs-skip", name = "Local mới", date = Date(30_000L), lastModifiedMs = 7_000L
        )
        coEvery { cardDao.insertCard(any()) } returns 11L
        coEvery { firestoreRepository.getAllWeightEntries() } returns Result.success(
            listOf(
                FirestoreWeightEntry(id = "en-2", cardId = "fs-new", weight = 52.0, timestamp = 200L),
                FirestoreWeightEntry(id = "en-1", cardId = "fs-new", weight = 50.0, timestamp = 100L),
                FirestoreWeightEntry(id = "en-skip", cardId = "fs-skip", weight = 40.0, timestamp = 100L)
            )
        )
        coEvery { firestoreRepository.getAllTransactions() } returns Result.success(
            listOf(FirestoreTransaction(id = "tx-new", cardId = "fs-new", amount = 2_000_000.0, type = "DEPOSIT"))
        )

        val result = syncManager.pullAllForCurrentUser()

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            cardRepository.replaceChildrenAtomic(
                11L,
                match { list -> list.map { it.firestoreId } == listOf("en-1", "en-2") && list[0].ordinal == 0 },
                match { txs -> txs.size == 1 && txs[0].firestoreId == "tx-new" }
            )
        }
        coVerify(exactly = 0) { cardRepository.replaceChildrenAtomic(30L, any(), any()) }
        coVerify(exactly = 1) { cardRepository.updateCardCalculations(11L) }
        coVerify(exactly = 0) { cardRepository.updateCardCalculations(30L) }
    }

    private companion object {
        const val TEST_UID = "user-123"
    }
}

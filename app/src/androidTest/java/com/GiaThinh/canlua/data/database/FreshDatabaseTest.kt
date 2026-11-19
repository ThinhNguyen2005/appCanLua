package com.giathinh.canlua.data.database

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.giathinh.canlua.data.model.Card
import com.giathinh.canlua.data.model.CardSyncState
import com.giathinh.canlua.data.model.MutationReceipt
import com.giathinh.canlua.data.model.WeightEntry
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

/**
 * T03 — Mở Room v20 mới (in-memory) để xác nhận schema entity/index nhất quán nội
 * bộ và các DAO mới hoạt động. Room tự validate schema khi mở → bắt lệch index/cột.
 */
@RunWith(AndroidJUnit4::class)
class FreshDatabaseTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun freshSchema_opensAndExercisesNewDaos() = runTest {
        val cardId = db.cardDao().insertCard(
            Card(name = "Test", date = Date(0), ownerUid = "uidA", syncId = "card-sync-1")
        )
        db.weightEntryDao().insertWeightEntry(
            WeightEntry(cardId = cardId, weight = 50.0, syncId = "w1", ordinal = 0, timestamp = 100)
        )
        db.weightEntryDao().insertWeightEntry(
            WeightEntry(cardId = cardId, weight = 60.0, syncId = "w2", ordinal = 1, timestamp = 100)
        )

        val entries = db.weightEntryDao().getWeightEntriesByCardIdSync(cardId)
        assertEquals(listOf("w1", "w2"), entries.map { it.syncId })
        assertEquals(1, db.weightEntryDao().getMaxOrdinal(cardId))

        db.cardSyncStateDao().insert(CardSyncState(syncId = "card-sync-1", ownerUid = "uidA"))
        assertEquals(CardSyncState.STATE_LEGACY, db.cardSyncStateDao().getBySyncId("card-sync-1")?.protocolState)

        db.mutationReceiptDao().insert(MutationReceipt(ownerUid = "uidA", operationId = "op1", commandHash = "h"))
        assertTrue(db.mutationReceiptDao().get("uidA", "op1") != null)

        // Dòng cân theo thứ tự ổn định (timestamp trùng → id tăng dần)
        assertEquals(entries.sortedBy { it.id }.map { it.id }, entries.map { it.id })
    }
}
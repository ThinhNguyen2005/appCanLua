package com.giathinh.canlua.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.giathinh.canlua.data.model.Card
import com.giathinh.canlua.data.model.CardWithDetails
import com.giathinh.canlua.data.model.Transaction
import com.giathinh.canlua.data.model.TransactionType
import com.giathinh.canlua.data.model.WeightEntry
import com.giathinh.canlua.repository.CardRepository
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Date

/**
 * Unit tests cho [BackupManager] — kiểm tra tính đúng đắn của logic Sao lưu & Khôi phục
 * tối ưu với Room @Relation và Transaction an toàn.
 */
class BackupManagerTest {

    @Test
    fun `exportData serializes user cards to JSON correctly using single relation query`() = runBlocking {
        val context = mockk<Context>()
        val contentResolver = mockk<ContentResolver>()
        val uri = mockk<Uri>()
        val cardRepository = mockk<CardRepository>()

        val date = Date(1782838800000L) // Thời điểm cố định để test
        val card1 = Card(id = 1, ownerUid = "user1", name = "Phiếu 1", date = date)
        val entry1 = WeightEntry(id = 10, cardId = 1, weight = 50.5, netWeight = 50.0)
        val tx1 = Transaction(id = 20, cardId = 1, amount = 10000.0, type = TransactionType.PAYMENT, date = date)

        val detail1 = CardWithDetails(card = card1, entries = listOf(entry1), transactions = listOf(tx1))

        // Xác nhận dùng 1 query duy nhất qua getCardsWithDetails
        coEvery { cardRepository.getCardsWithDetails("user1") } returns listOf(detail1)

        val outputStream = ByteArrayOutputStream()
        every { context.contentResolver } returns contentResolver
        every { contentResolver.openOutputStream(uri) } returns outputStream

        val result = BackupManager.exportData(context, uri, "user1", cardRepository)
        assertTrue(result.isSuccess)

        val json = outputStream.toString("UTF-8")
        assertTrue(json.contains("\"name\":\"Phiếu 1\""))
        assertTrue(json.contains("\"weight\":50.5"))
        assertTrue(json.contains("\"amount\":10000.0"))

        coVerify(exactly = 1) { cardRepository.getCardsWithDetails("user1") }
    }

    @Test
    fun `importData parses JSON and delegates to atomic batch transaction`() = runBlocking {
        val context = mockk<Context>()
        val contentResolver = mockk<ContentResolver>()
        val uri = mockk<Uri>()
        val cardRepository = mockk<CardRepository>(relaxed = true)

        val json = """
            {
              "version": 1,
              "backupDate": 1782838800000,
              "data": [
                {
                  "card": {
                    "id": 1,
                    "ownerUid": "oldUser",
                    "name": "Phiếu cũ",
                    "date": "2026-06-14T20:00:00.000+0700",
                    "totalWeight": 100.0
                  },
                  "entries": [
                    {
                      "id": 10,
                      "cardId": 1,
                      "weight": 50.0
                    }
                  ],
                  "transactions": [
                    {
                      "id": 20,
                      "cardId": 1,
                      "amount": 5000.0,
                      "type": "PAYMENT",
                      "date": "2026-06-14T20:00:00.000+0700"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val inputStream = ByteArrayInputStream(json.toByteArray(Charsets.UTF_8))
        every { context.contentResolver } returns contentResolver
        every { contentResolver.openInputStream(uri) } returns inputStream

        coEvery { cardRepository.importBackupBatch("newUser", any()) } returns 1

        val result = BackupManager.importData(context, uri, "newUser", cardRepository)
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())

        // Xác nhận toàn bộ batch được chuyển sang importBackupBatch để bọc trong 1 Transaction duy nhất
        coVerify(exactly = 1) {
            cardRepository.importBackupBatch("newUser", match { wrappers ->
                wrappers.size == 1 &&
                wrappers[0].card.name == "Phiếu cũ" &&
                wrappers[0].entries.size == 1 &&
                wrappers[0].transactions.size == 1
            })
        }
    }
}

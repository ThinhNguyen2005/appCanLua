package com.giathinh.canlua.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.giathinh.canlua.data.model.Card
import com.giathinh.canlua.data.model.Transaction
import com.giathinh.canlua.data.model.TransactionType
import com.giathinh.canlua.data.model.WeightEntry
import com.giathinh.canlua.repository.CardRepository
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Date

/**
 * Unit tests cho [BackupManager] — kiểm tra tính đúng đắn của logic Sao lưu & Khôi phục.
 */
class BackupManagerTest {

    @Test
    fun `exportData serializes user cards to JSON correctly`() = runBlocking {
        val context = mockk<Context>()
        val contentResolver = mockk<ContentResolver>()
        val uri = mockk<Uri>()
        val cardRepository = mockk<CardRepository>()

        val date = Date(1782838800000L) // Thời điểm cố định để test
        val card1 = Card(id = 1, ownerUid = "user1", name = "Phiếu 1", date = date)
        val entry1 = WeightEntry(id = 10, cardId = 1, weight = 50.5, netWeight = 50.0)
        val tx1 = Transaction(id = 20, cardId = 1, amount = 10000.0, type = TransactionType.PAYMENT, date = date)

        every { cardRepository.getAllCards() } returns flowOf(listOf(card1))
        every { cardRepository.getWeightEntriesByCardId(1) } returns flowOf(listOf(entry1))
        every { cardRepository.getTransactionsByCardId(1) } returns flowOf(listOf(tx1))

        val outputStream = ByteArrayOutputStream()
        every { context.contentResolver } returns contentResolver
        every { contentResolver.openOutputStream(uri) } returns outputStream

        val result = BackupManager.exportData(context, uri, "user1", cardRepository)
        assertTrue(result.isSuccess)

        val json = outputStream.toString("UTF-8")
        assertTrue(json.contains("\"name\":\"Phiếu 1\""))
        assertTrue(json.contains("\"weight\":50.5"))
        assertTrue(json.contains("\"amount\":10000.0"))
    }

    @Test
    fun `importData parses JSON and inserts copies in correct order`() = runBlocking {
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

        coEvery { cardRepository.insertCard(any()) } returns 999L // Mock ID phiếu mới

        val result = BackupManager.importData(context, uri, "newUser", cardRepository)
        if (result.isFailure) {
            println("IMPORT_FAILURE_EXCEPTION:")
            result.exceptionOrNull()?.printStackTrace()
        }
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())

        // Xác nhận card được chèn với id = 0 (tự sinh) và ownerUid mới
        coVerify {
            cardRepository.insertCard(match {
                it.id == 0L && it.ownerUid == "newUser" && it.name == "Phiếu cũ"
            })
        }

        // Xác nhận entry được chèn với id = 0 (tự sinh) và cardId mới (999)
        coVerify {
            cardRepository.insertWeightEntry(match {
                it.id == 0L && it.cardId == 999L && it.weight == 50.0
            })
        }

        // Xác nhận transaction được chèn với id = 0 (tự sinh) và cardId mới (999)
        coVerify {
            cardRepository.insertTransaction(match {
                it.id == 0L && it.cardId == 999L && it.amount == 5000.0
            })
        }

        // Xác nhận kích hoạt cập nhật tính toán lại tổng số
        coVerify {
            cardRepository.updateCardCalculations(999L)
        }
    }
}

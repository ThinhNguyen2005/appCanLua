package com.GiaThinh.canlua.util

import android.content.Context
import android.net.Uri
import com.GiaThinh.canlua.data.model.Card
import com.GiaThinh.canlua.data.model.Transaction
import com.GiaThinh.canlua.data.model.TransactionType
import com.GiaThinh.canlua.data.model.WeightEntry
import com.GiaThinh.canlua.repository.CardRepository
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

/**
 * Tiện ích Sao lưu và Khôi phục dữ liệu phiếu cân (local-only).
 * Mã hóa dữ liệu thành định dạng JSON để người dùng Premium tự lưu trữ hoặc chia sẻ.
 */
object BackupManager {

    private val gson: Gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
        .create()

    data class CardBackupWrapper(
        val card: Card,
        val entries: List<WeightEntry>,
        val transactions: List<Transaction>
    )

    data class BackupPayload(
        val version: Int,
        val backupDate: Long,
        val data: List<CardBackupWrapper>
    )

    /**
     * Xuất toàn bộ phiếu cân của user hiện tại sang file JSON.
     * Yêu cầu quyền Premium trước khi gọi.
     */
    suspend fun exportData(
        context: Context,
        uri: Uri,
        ownerUid: String,
        cardRepository: CardRepository
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val cards = cardRepository.getAllCards().first().filter { it.ownerUid == ownerUid }
            val wrappers = cards.map { card ->
                val entries = cardRepository.getWeightEntriesByCardId(card.id).first()
                val transactions = cardRepository.getTransactionsByCardId(card.id).first()
                CardBackupWrapper(card, entries, transactions)
            }
            val payload = BackupPayload(
                version = 1,
                backupDate = System.currentTimeMillis(),
                data = wrappers
            )
            val json = gson.toJson(payload)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream, "UTF-8").use { writer ->
                    writer.write(json)
                }
            } ?: throw IllegalStateException("Không thể mở luồng ghi file: $uri")
        }
    }

    /**
     * Khôi phục toàn bộ phiếu cân từ file JSON vào Room DB.
     * Liên kết lại khóa ngoại (foreign keys) để giữ vững tính toàn vẹn liên kết.
     */
    suspend fun importData(
        context: Context,
        uri: Uri,
        ownerUid: String,
        cardRepository: CardRepository
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val json = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, "UTF-8")).use { reader ->
                    reader.readText()
                }
            } ?: throw IllegalStateException("Không thể mở luồng đọc file: $uri")

            val payload = gson.fromJson(json, BackupPayload::class.java)
            var count = 0
            for (wrapper in payload.data) {
                // Nhân bản Card (id tự sinh = 0) và gán ownerUid hiện tại sau khi đã được sanitize tránh lỗi Gson null fields
                val cardCopy = wrapper.card.sanitize().copy(id = 0, ownerUid = ownerUid)
                val newCardId = cardRepository.insertCard(cardCopy)

                // Nhập danh sách bao lúa với cardId mới
                for (entry in wrapper.entries.orEmpty()) {
                    val entryCopy = entry.copy(id = 0, cardId = newCardId)
                    cardRepository.insertWeightEntry(entryCopy)
                }

                // Nhập danh sách giao dịch với cardId mới
                for (tx in wrapper.transactions.orEmpty()) {
                    val txCopy = tx.sanitize().copy(id = 0, cardId = newCardId)
                    cardRepository.insertTransaction(txCopy)
                }

                // Tính toán lại tổng khối lượng/tiền tệ cho Card mới chèn
                cardRepository.updateCardCalculations(newCardId)
                count++
            }
            count
        }
    }
}

/**
 * Sanitize extension to ensure Gson-deserialized Card instances do not contain null properties for non-nullable Kotlin types.
 */
private fun Card.sanitize(): Card {
    return Card(
        id = this.id,
        ownerUid = this.ownerUid ?: "",
        name = this.name ?: "",
        cccd = this.cccd,
        traderName = this.traderName ?: "",
        date = this.date ?: Date(),
        totalWeight = this.totalWeight,
        bagWeight = this.bagWeight,
        impurityWeight = this.impurityWeight,
        netWeight = this.netWeight,
        depositAmount = this.depositAmount,
        pricePerKg = this.pricePerKg,
        totalAmount = this.totalAmount,
        paidAmount = this.paidAmount,
        remainingAmount = this.remainingAmount,
        bagCount = this.bagCount,
        isLocked = this.isLocked,
        riceVariety = this.riceVariety ?: "",
        moisturePercent = this.moisturePercent,
        seasonLabel = this.seasonLabel ?: "",
        qrToken = this.qrToken,
        lockedByTraderId = this.lockedByTraderId,
        latitude = this.latitude,
        longitude = this.longitude,
        traderPhone = this.traderPhone ?: "",
        fieldAddress = this.fieldAddress ?: "",
        firestoreId = this.firestoreId,
        lastModifiedMs = this.lastModifiedMs,
        isPaid = this.isPaid,
        impurityIsPercent = this.impurityIsPercent,
        bagMethodIsSampling = this.bagMethodIsSampling,
        bagSampleCount = this.bagSampleCount,
        bagSampleTotalWeight = this.bagSampleTotalWeight,
        weightInputMode = this.weightInputMode ?: "SMALL"
    )
}

/**
 * Sanitize extension to ensure Gson-deserialized Transaction instances do not contain null properties for non-nullable Kotlin types.
 */
private fun Transaction.sanitize(): Transaction {
    return Transaction(
        id = this.id,
        cardId = this.cardId,
        amount = this.amount,
        type = this.type ?: TransactionType.PAYMENT,
        description = this.description,
        date = this.date ?: Date(),
        firestoreId = this.firestoreId
    )
}

package com.giathinh.canlua.util

import android.content.Context
import android.net.Uri
import com.giathinh.canlua.data.model.Card
import com.giathinh.canlua.data.model.Transaction
import com.giathinh.canlua.data.model.TransactionType
import com.giathinh.canlua.data.model.WeightEntry
import com.giathinh.canlua.repository.CardRepository
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
            // Tối ưu N+1 Query: Sử dụng quan hệ @Relation của Room để tải 1 lần toàn bộ CardWithDetails
            val details = cardRepository.getCardsWithDetails(ownerUid)
            val wrappers = details.map { d ->
                CardBackupWrapper(d.card, d.entries, d.transactions)
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
     * Đóng gói toàn bộ trong 1 Room Transaction duy nhất (All-or-Nothing).
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
            val sanitizedWrappers = payload.data.orEmpty().map { wrapper ->
                CardBackupWrapper(
                    card = wrapper.card.sanitize(),
                    entries = wrapper.entries.orEmpty(),
                    transactions = wrapper.transactions.orEmpty().map { it.sanitize() }
                )
            }
            // Thực thi toàn bộ batch trong 1 Transaction duy nhất qua Repository
            cardRepository.importBackupBatch(ownerUid, sanitizedWrappers)
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

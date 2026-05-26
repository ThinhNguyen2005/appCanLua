package com.GiaThinh.canlua.repository

import com.GiaThinh.canlua.data.dao.CardDao
import com.GiaThinh.canlua.data.dao.TransactionDao
import com.GiaThinh.canlua.data.dao.WeightEntryDao
import com.GiaThinh.canlua.data.model.Card
import com.GiaThinh.canlua.data.model.SeasonStats
import com.GiaThinh.canlua.data.model.SeasonStatsRaw
import com.GiaThinh.canlua.data.model.TraderHistoryItem
import com.GiaThinh.canlua.data.model.TraderStat
import com.GiaThinh.canlua.data.model.Transaction
import com.GiaThinh.canlua.data.model.TransactionType
import com.GiaThinh.canlua.data.model.VarietyStat
import com.GiaThinh.canlua.data.model.WeightEntry
import com.GiaThinh.canlua.util.RiceCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository cho phiếu cân.
 *
 * **Per-user isolation (v12+)**: mọi truy vấn DAO đều forward Firebase UID
 * của user đang sign-in qua [AuthManager.currentUser]. Khi chưa sign-in,
 * trả Flow rỗng / null thay vì throw — tránh crash trong khoảng thời gian
 * AuthState đang chuyển trạng thái (cold start, sign-out → sign-in).
 *
 * Mọi `insert*` tự gán `ownerUid` cho card mới. UI không cần biết về uid.
 */
@Singleton
class CardRepository @Inject constructor(
    private val cardDao: CardDao,
    private val weightEntryDao: WeightEntryDao,
    private val transactionDao: TransactionDao,
    private val deletedCardDao: com.GiaThinh.canlua.data.dao.DeletedCardDao,
    private val authManager: AuthManager
) {
    /** Empty string khi chưa sign-in → DAO query trả empty (không match row nào). */
    private fun uid(): String = authManager.currentUser?.uid.orEmpty()

    fun getAllCards(): Flow<List<Card>> = cardDao.getAllCards(uid())

    suspend fun getCardById(id: Long): Card? = cardDao.getCardById(id, uid())

    /**
     * Insert card mới. Tự stamp `ownerUid` từ session hiện tại + `lastModifiedMs`
     * từ system clock trước khi ghi DB. UI/ViewModel chỉ chuyển business data.
     */
    suspend fun insertCard(card: Card): Long {
        val now = System.currentTimeMillis()
        val owned = card.copy(
            ownerUid = if (card.ownerUid.isBlank()) uid() else card.ownerUid,
            lastModifiedMs = if (card.lastModifiedMs == 0L) now else card.lastModifiedMs
        )
        return cardDao.insertCard(owned)
    }

    /**
     * Update card. Tự stamp lại `lastModifiedMs` để conflict resolver biết
     * bản local mới hơn cloud (cloud sẽ chỉ overwrite local nếu cloud mới hơn).
     */
    suspend fun updateCard(card: Card) {
        cardDao.updateCard(card.copy(lastModifiedMs = System.currentTimeMillis()))
    }

    /**
     * Xoá card local + ghi tombstone trước khi xoá để chống bug "phiếu phục sinh"
     * sau pull. Tombstone lưu snapshot full card data (JSON) để user khôi phục.
     *
     * Caller (SyncableCardRepository) sẽ chịu trách nhiệm xoá Firestore — nếu
     * fail (offline), tombstone giữ flag `cloudDeleted = false` để background
     * worker retry.
     */
    suspend fun deleteCard(card: Card) {
        // 1. Ghi tombstone trước. Nếu app crash giữa chừng, lần khởi động sau
        //    pull sync sẽ skip restore phiếu này (đã có tombstone với firestoreId).
        val ownerUid = card.ownerUid.ifBlank { uid() }
        if (ownerUid.isNotBlank()) {
            deletedCardDao.insert(
                com.GiaThinh.canlua.data.model.DeletedCard(
                    ownerUid = ownerUid,
                    firestoreId = card.firestoreId,
                    localId = card.id,
                    cardJson = serializeCard(card),
                    name = card.name,
                    traderName = card.traderName,
                    totalWeight = card.totalWeight,
                    totalAmount = card.totalAmount,
                    cardDate = card.date.time,
                    seasonLabel = card.seasonLabel,
                    riceVariety = card.riceVariety,
                    // Nếu card chưa từng sync (firestoreId null), không cần xoá cloud.
                    cloudDeleted = card.firestoreId == null
                )
            )
        }
        // 2. Xoá Room — FK CASCADE tự xoá weight_entries + transactions con.
        cardDao.deleteCard(card)
    }

    /**
     * Khôi phục phiếu từ tombstone — recreate card với lastModifiedMs = now
     * để force push lên cloud (cloud có thể đã xoá do delete sync trước đó).
     * Sau khôi phục thành công, purge tombstone.
     */
    suspend fun restoreFromTombstone(tombstoneId: Long): Long? {
        val tombstone = deletedCardDao.getById(tombstoneId) ?: return null
        val card = deserializeCard(tombstone.cardJson) ?: return null
        // Insert card mới — Room auto generate id mới (id cũ có thể đã collide).
        // Reset firestoreId để push tạo doc mới (doc cũ đã bị delete sync).
        val now = System.currentTimeMillis()
        val newId = cardDao.insertCard(
            card.copy(
                id = 0L,
                firestoreId = null,
                lastModifiedMs = now,
                ownerUid = tombstone.ownerUid
            )
        )
        deletedCardDao.purge(tombstoneId)
        return newId
    }

    suspend fun getDeletedCards(uid: String) = deletedCardDao.observe(uid)

    suspend fun isCardTombstoned(uid: String, firestoreId: String): Boolean =
        deletedCardDao.isTombstoned(uid, firestoreId)

    suspend fun getPendingCloudDeletes(uid: String) =
        deletedCardDao.getPendingCloudDeletes(uid)

    suspend fun markTombstoneCloudDeleted(id: Long) = deletedCardDao.markCloudDeleted(id)

    suspend fun purgeTombstone(id: Long) = deletedCardDao.purge(id)

    /**
     * Serialize card → JSON minimal cho tombstone restore.
     * Dùng key=value đơn giản thay vì Gson để tránh thêm dep + giữ schema control.
     */
    private fun serializeCard(card: Card): String {
        val sb = StringBuilder("{")
        sb.append("\"name\":${jsonString(card.name)},")
        sb.append("\"cccd\":${jsonNullable(card.cccd)},")
        sb.append("\"traderName\":${jsonString(card.traderName)},")
        sb.append("\"date\":${card.date.time},")
        sb.append("\"totalWeight\":${card.totalWeight},")
        sb.append("\"bagWeight\":${card.bagWeight},")
        sb.append("\"impurityWeight\":${card.impurityWeight},")
        sb.append("\"netWeight\":${card.netWeight},")
        sb.append("\"depositAmount\":${card.depositAmount},")
        sb.append("\"pricePerKg\":${card.pricePerKg},")
        sb.append("\"totalAmount\":${card.totalAmount},")
        sb.append("\"paidAmount\":${card.paidAmount},")
        sb.append("\"remainingAmount\":${card.remainingAmount},")
        sb.append("\"bagCount\":${card.bagCount},")
        sb.append("\"isLocked\":${card.isLocked},")
        sb.append("\"isPaid\":${card.isPaid},")
        sb.append("\"riceVariety\":${jsonString(card.riceVariety)},")
        sb.append("\"moisturePercent\":${card.moisturePercent},")
        sb.append("\"seasonLabel\":${jsonString(card.seasonLabel)},")
        sb.append("\"qrToken\":${jsonNullable(card.qrToken)},")
        sb.append("\"lockedByTraderId\":${jsonNullable(card.lockedByTraderId)},")
        sb.append("\"latitude\":${card.latitude ?: "null"},")
        sb.append("\"longitude\":${card.longitude ?: "null"},")
        sb.append("\"traderPhone\":${jsonString(card.traderPhone)},")
        sb.append("\"fieldAddress\":${jsonString(card.fieldAddress)},")
        sb.append("\"impurityIsPercent\":${card.impurityIsPercent},")
        sb.append("\"bagMethodIsSampling\":${card.bagMethodIsSampling},")
        sb.append("\"bagSampleCount\":${card.bagSampleCount},")
        sb.append("\"bagSampleTotalWeight\":${card.bagSampleTotalWeight},")
        sb.append("\"weightInputMode\":${jsonString(card.weightInputMode)}")
        sb.append("}")
        return sb.toString()
    }

    private fun deserializeCard(json: String): Card? = runCatching {
        val map = parseSimpleJson(json)
        Card(
            ownerUid = "",
            name = map["name"]?.toString().orEmpty(),
            cccd = map["cccd"] as? String,
            traderName = map["traderName"]?.toString().orEmpty(),
            date = java.util.Date((map["date"] as? Number)?.toLong() ?: 0L),
            totalWeight = (map["totalWeight"] as? Number)?.toDouble() ?: 0.0,
            bagWeight = (map["bagWeight"] as? Number)?.toDouble() ?: 0.0,
            impurityWeight = (map["impurityWeight"] as? Number)?.toDouble() ?: 0.0,
            netWeight = (map["netWeight"] as? Number)?.toDouble() ?: 0.0,
            depositAmount = (map["depositAmount"] as? Number)?.toDouble() ?: 0.0,
            pricePerKg = (map["pricePerKg"] as? Number)?.toDouble() ?: 0.0,
            totalAmount = (map["totalAmount"] as? Number)?.toDouble() ?: 0.0,
            paidAmount = (map["paidAmount"] as? Number)?.toDouble() ?: 0.0,
            remainingAmount = (map["remainingAmount"] as? Number)?.toDouble() ?: 0.0,
            bagCount = (map["bagCount"] as? Number)?.toInt() ?: 0,
            isLocked = map["isLocked"] as? Boolean ?: false,
            isPaid = map["isPaid"] as? Boolean ?: false,
            riceVariety = map["riceVariety"]?.toString().orEmpty(),
            moisturePercent = (map["moisturePercent"] as? Number)?.toDouble() ?: 0.0,
            seasonLabel = map["seasonLabel"]?.toString().orEmpty(),
            qrToken = map["qrToken"] as? String,
            lockedByTraderId = map["lockedByTraderId"] as? String,
            latitude = (map["latitude"] as? Number)?.toDouble(),
            longitude = (map["longitude"] as? Number)?.toDouble(),
            traderPhone = map["traderPhone"]?.toString().orEmpty(),
            fieldAddress = map["fieldAddress"]?.toString().orEmpty(),
            impurityIsPercent = map["impurityIsPercent"] as? Boolean ?: false,
            bagMethodIsSampling = map["bagMethodIsSampling"] as? Boolean ?: false,
            bagSampleCount = (map["bagSampleCount"] as? Number)?.toInt() ?: 0,
            bagSampleTotalWeight = (map["bagSampleTotalWeight"] as? Number)?.toDouble() ?: 0.0,
            weightInputMode = map["weightInputMode"]?.toString().takeIf { !it.isNullOrBlank() } ?: "SMALL"
        )
    }.getOrNull()

    private fun jsonString(s: String): String =
        "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    private fun jsonNullable(s: String?): String =
        if (s == null) "null" else jsonString(s)

    /** Parser minimal cho format do `serializeCard` sinh — không general-purpose. */
    private fun parseSimpleJson(json: String): Map<String, Any?> {
        val obj = org.json.JSONObject(json)
        return buildMap {
            obj.keys().forEach { k ->
                put(k, if (obj.isNull(k)) null else obj.get(k))
            }
        }
    }

    // Weight Entry operations — không cần filter uid vì FK CASCADE qua cardId,
    // và caller chỉ truy cập sau khi đã có cardId từ getAllCards (đã filter).
    fun getWeightEntriesByCardId(cardId: Long): Flow<List<WeightEntry>> =
        weightEntryDao.getWeightEntriesByCardId(cardId)

    suspend fun insertWeightEntry(weightEntry: WeightEntry): Long =
        weightEntryDao.insertWeightEntry(weightEntry)

    suspend fun updateWeightEntry(weightEntry: WeightEntry) =
        weightEntryDao.updateWeightEntry(weightEntry)

    suspend fun deleteWeightEntry(weightEntry: WeightEntry) =
        weightEntryDao.deleteWeightEntry(weightEntry)

    // Transaction operations
    fun getTransactionsByCardId(cardId: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsByCardId(cardId)

    suspend fun insertTransaction(transaction: Transaction): Long =
        transactionDao.insertTransaction(transaction)

    // Calculation methods — uses RiceCalculator with moisture adjustment
    suspend fun calculateCardTotals(cardId: Long): CardCalculationResult {
        val totalRawWeight = weightEntryDao.getTotalRawWeightByCardId(cardId) ?: 0.0
        val totalNetWeight = weightEntryDao.getTotalNetWeightByCardId(cardId) ?: 0.0
        val bagCount = weightEntryDao.getBagCountByCardId(cardId)
        val totalPaid = transactionDao.getTotalPaidAmountByCardId(cardId) ?: 0.0
        val totalDeposit = transactionDao.getTotalDepositAmountByCardId(cardId) ?: 0.0

        return CardCalculationResult(
            totalRawWeight = totalRawWeight,
            totalNetWeight = totalNetWeight,
            bagCount = bagCount,
            totalPaid = totalPaid,
            totalDeposit = totalDeposit
        )
    }

    /**
     * Recalculate aggregate fields trên Card từ các WeightEntry + Transaction con.
     *
     * **Quan trọng:** trước đây hàm này tự tính netWeight bằng công thức tự chế
     * `totalRaw - totalRaw × moisture/100` (trừ thẳng % moisture, sai chuẩn ngành lúa).
     * Đã unify về `RiceCalculator.calcNetWeight` để khớp với công thức tính ở từng
     * `WeightEntry` và đúng chuẩn ngành (quy đổi về độ ẩm chuẩn 14%).
     */
    suspend fun updateCardCalculations(cardId: Long) {
        val card = cardDao.getCardById(cardId, uid()) ?: return
        val calculation = calculateCardTotals(cardId)
        val totalRaw = calculation.totalRawWeight
        val entries = weightEntryDao.getWeightEntriesByCardIdSync(cardId)

        val validEntries = entries.filter { it.weight > 0.0 }
        val validBagCount = validEntries.size

        if (validBagCount > 0) {
            val totalBag = RiceCalculator.calcTotalBagWeight(
                bagCount = validBagCount,
                bagWeight = card.bagWeight,
                methodIsSampling = card.bagMethodIsSampling,
                sampleCount = card.bagSampleCount,
                sampleTotalWeight = card.bagSampleTotalWeight
            )
            val singleBagWeight = totalBag / validBagCount

            val rawAfterBag = (totalRaw - totalBag).coerceAtLeast(0.0)
            val totalImpurity = RiceCalculator.calcTotalImpurity(
                rawAfterBag = rawAfterBag,
                impurityValue = card.impurityWeight,
                isPercent = card.impurityIsPercent
            )
            val singleImpurityWeight = totalImpurity / validBagCount

            entries.forEach { entry ->
                val (bagW, impW, netW) = if (entry.weight > 0.0) {
                    val entryNetWeight = RiceCalculator.calcNetWeight(
                        rawWeight = entry.weight,
                        bagWeight = singleBagWeight,
                        impurityWeight = singleImpurityWeight,
                        moisturePercent = card.moisturePercent
                    )
                    Triple(singleBagWeight, singleImpurityWeight, entryNetWeight)
                } else {
                    Triple(0.0, 0.0, 0.0)
                }

                if (entry.bagWeight != bagW || entry.impurityWeight != impW || entry.netWeight != netW) {
                    weightEntryDao.updateWeightEntry(
                        entry.copy(
                            bagWeight = bagW,
                            impurityWeight = impW,
                            netWeight = netW
                        )
                    )
                }
            }
        } else {
            entries.forEach { entry ->
                if (entry.bagWeight != 0.0 || entry.impurityWeight != 0.0 || entry.netWeight != 0.0) {
                    weightEntryDao.updateWeightEntry(
                        entry.copy(
                            bagWeight = 0.0,
                            impurityWeight = 0.0,
                            netWeight = 0.0
                        )
                    )
                }
            }
        }

        // Tính KL thực có ý thức về mode bao bì (A/B) + tạp chất (kg/%).
        val finalNetWeight = RiceCalculator.calcNetWeightWithModes(
            totalRaw = totalRaw,
            bagCount = validBagCount,
            bagWeight = card.bagWeight,
            bagMethodIsSampling = card.bagMethodIsSampling,
            bagSampleCount = card.bagSampleCount,
            bagSampleTotalWeight = card.bagSampleTotalWeight,
            impurityValue = card.impurityWeight,
            impurityIsPercent = card.impurityIsPercent,
            moisturePercent = card.moisturePercent
        ).coerceAtLeast(0.0)

        val totalAmount = RiceCalculator.calcTotalAmount(
            netWeight = finalNetWeight,
            pricePerKg = card.pricePerKg
        ).coerceAtLeast(0.0)

        // Còn lại không cho phép âm để tránh hiển thị "-100,000đ" vô nghĩa.
        val remainingAmount = RiceCalculator.calcRemainingAmount(
            totalAmount = totalAmount,
            paidAmount = calculation.totalPaid,
            depositAmount = calculation.totalDeposit
        ).coerceAtLeast(0.0)

        val updatedCard = card.copy(
            totalWeight = totalRaw,
            netWeight = finalNetWeight,
            bagCount = calculation.bagCount,
            paidAmount = calculation.totalPaid,
            depositAmount = calculation.totalDeposit,
            totalAmount = totalAmount,
            remainingAmount = remainingAmount
        )

        cardDao.updateCard(updatedCard)
    }

    // === Phase 1: QR Handshake ===
    // QR query KHÔNG filter ownerUid: trader (uid khác farmer) phải tìm được
    // card qua qrToken để xác thực giao dịch chéo. Token đã đủ entropy + được
    // bảo vệ bởi Firestore rule.

    suspend fun findByQrToken(token: String): Card? = cardDao.findByQrToken(token)

    suspend fun updateQrToken(cardId: Long, token: String) = cardDao.updateQrToken(cardId, token)

    suspend fun lockCard(cardId: Long, traderId: String) = cardDao.lockCard(cardId, traderId)

    // === Phase 1: Filter ===

    fun getCardsByRiceVariety(variety: String): Flow<List<Card>> =
        cardDao.getCardsByRiceVariety(variety, uid())

    fun getDistinctRiceVarieties(): Flow<List<String>> =
        cardDao.getDistinctRiceVarieties(uid())

    fun getSuggestedRiceVarieties(): Flow<List<String>> =
        cardDao.getSuggestedRiceVarieties(uid())

    // === Phase 3: Season Statistics Dashboard ===

    fun getDistinctSeasons(): Flow<List<String>> = cardDao.getDistinctSeasons(uid())

    /** Stats cho 1 vụ → map raw → domain. */
    fun getSeasonStats(season: String): Flow<SeasonStats> =
        cardDao.getSeasonStats(season, uid()).map { it.toDomain(season) }

    /** Stats tổng toàn bộ data — dùng khi chưa có vụ nào hoặc fallback. */
    fun getOverallStats(): Flow<SeasonStats> =
        cardDao.getOverallStats(uid()).map { it.toDomain(season = "Tất cả") }

    fun getVarietyBreakdown(season: String): Flow<List<VarietyStat>> =
        cardDao.getVarietyBreakdown(season, uid())

    fun getTopTraders(season: String): Flow<List<TraderStat>> =
        cardDao.getTopTraders(season, uid())

    fun getAllSeasonsComparison(): Flow<List<SeasonStats>> =
        cardDao.getAllSeasonsComparison(uid()).map { rows ->
            rows.map { row ->
                SeasonStatsRaw(
                    cardCount = row.cardCount,
                    totalNetWeight = row.totalNetWeight,
                    totalRevenue = row.totalRevenue,
                    totalPaid = row.totalPaid,
                    totalRemaining = row.totalRemaining,
                    avgPrice = row.avgPrice,
                    avgMoisture = row.avgMoisture,
                    totalBags = row.totalBags,
                    totalImpurity = row.totalImpurity,
                    wetCardCount = row.wetCardCount,
                    dryCardCount = row.dryCardCount
                ).toDomain(row.season).copy(lastDate = row.lastDate)
            }
        }

    /** Lịch sử thương lái đã mua — reactive Flow từ cards table. */
    fun getTraderHistory(): Flow<List<TraderHistoryItem>> = cardDao.getTraderHistory(uid())

    /**
     * One-shot migration: gán toàn bộ cards orphan (ownerUid='') cho user đang
     * đăng nhập. Gọi từ `CanLuaApplication` sau lần đầu sign-in sau update v12.
     * Idempotent — gọi nhiều lần cũng an toàn (không có row orphan thì noop).
     *
     * @return số rows đã claim. 0 = không cần làm gì.
     */
    suspend fun claimOrphanCardsForCurrentUser(): Int {
        val u = uid()
        if (u.isBlank()) return 0
        if (cardDao.countOrphanCards() == 0) return 0
        return cardDao.claimOrphanCards(u)
    }
    /**
     * Đếm số phiếu user hiện tại đã tạo trong ngày hôm nay (00:00 local timezone
     * → bây giờ). Phục vụ Premium gate "free user tối đa N phiếu/ngày".
     * Trả 0 nếu chưa sign-in (uid blank).
     */
    suspend fun countCardsCreatedToday(): Int {
        val u = uid()
        if (u.isBlank()) return 0
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cardDao.countCardsSince(u, cal.timeInMillis)
    }
}

data class CardCalculationResult(
    val totalRawWeight: Double,
    val totalNetWeight: Double,
    val bagCount: Int,
    val totalPaid: Double,
    val totalDeposit: Double
)

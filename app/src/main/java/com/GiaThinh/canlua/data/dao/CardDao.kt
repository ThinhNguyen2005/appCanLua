package com.GiaThinh.canlua.data.dao

import androidx.room.*
import com.GiaThinh.canlua.data.model.Card
import com.GiaThinh.canlua.data.model.SeasonStatsRaw
import com.GiaThinh.canlua.data.model.TraderHistoryItem
import com.GiaThinh.canlua.data.model.TraderStat
import com.GiaThinh.canlua.data.model.VarietyStat
import kotlinx.coroutines.flow.Flow

/**
 * Tất cả query lọc theo `ownerUid` để cô lập data per-user trên cùng device
 * (xem `Card.ownerUid`). Repository chịu trách nhiệm forward Firebase UID đang
 * sign-in vào tham số `:uid`.
 */
@Dao
interface CardDao {
    @Query("SELECT * FROM cards WHERE ownerUid = :uid ORDER BY date DESC")
    fun getAllCards(uid: String): Flow<List<Card>>

    @Query("SELECT * FROM cards WHERE id = :id AND ownerUid = :uid")
    suspend fun getCardById(id: Long, uid: String): Card?

    @Insert
    suspend fun insertCard(card: Card): Long

    @Update
    suspend fun updateCard(card: Card)

    @Delete
    suspend fun deleteCard(card: Card)

    @Query("DELETE FROM cards WHERE id = :id AND ownerUid = :uid")
    suspend fun deleteCardById(id: Long, uid: String)

    // === Phase 1: QR Handshake queries ===
    // QR token là hash SHA-256 — khoá xác thực giữa farmer và trader.
    // Trader (uid khác farmer) phải scan được card nên các query này KHÔNG
    // filter ownerUid; thay vào đó dựa vào tính duy nhất của qrToken + Firestore
    // rule kiểm soát.

    @Query("SELECT * FROM cards WHERE qrToken = :token LIMIT 1")
    suspend fun findByQrToken(token: String): Card?

    @Query("UPDATE cards SET qrToken = :token WHERE id = :cardId")
    suspend fun updateQrToken(cardId: Long, token: String)

    @Query("UPDATE cards SET isLocked = 1, lockedByTraderId = :traderId WHERE id = :cardId")
    suspend fun lockCard(cardId: Long, traderId: String)

    // === Phase 1: Filter & search ===

    @Query("SELECT * FROM cards WHERE ownerUid = :uid AND riceVariety = :variety ORDER BY date DESC")
    fun getCardsByRiceVariety(variety: String, uid: String): Flow<List<Card>>

    @Query("SELECT DISTINCT riceVariety FROM cards WHERE ownerUid = :uid AND riceVariety != '' ORDER BY riceVariety")
    fun getDistinctRiceVarieties(uid: String): Flow<List<String>>

    @Query("""
        SELECT riceVariety FROM cards
        WHERE ownerUid = :uid AND riceVariety != ''
        GROUP BY riceVariety
        ORDER BY MAX(date) DESC, COUNT(*) DESC
        LIMIT 5
    """)
    fun getSuggestedRiceVarieties(uid: String): Flow<List<String>>

    // === Phase 3: Season Statistics Dashboard ===

    /** Lấy danh sách các vụ đã có dữ liệu, sort theo ngày card mới nhất trong vụ đó. */
    @Query("""
        SELECT seasonLabel FROM cards
        WHERE ownerUid = :uid AND seasonLabel != ''
        GROUP BY seasonLabel
        ORDER BY MAX(date) DESC
    """)
    fun getDistinctSeasons(uid: String): Flow<List<String>>

    /**
     * Aggregate stats cho 1 vụ.
     * Trả null khi không có card nào (Room trả empty row → mapper handle).
     * COUNT/SUM trên empty set trả 0/null tương ứng → SeasonStatsRaw fields nullable.
     */
    @Query(QUERY_SEASON_STATS)
    fun getSeasonStats(season: String, uid: String): Flow<SeasonStatsRaw>

    @Query(QUERY_OVERALL_STATS)
    fun getOverallStats(uid: String): Flow<SeasonStatsRaw>

    /** Phân bổ giống lúa trong 1 vụ — sort by weight desc. */
    @Query("""
        SELECT
            riceVariety as variety,
            SUM(netWeight) as weight,
            COUNT(*) as count
        FROM cards
        WHERE ownerUid = :uid AND seasonLabel = :season AND riceVariety != ''
        GROUP BY riceVariety
        ORDER BY weight DESC
    """)
    fun getVarietyBreakdown(season: String, uid: String): Flow<List<VarietyStat>>

    /** Top 5 thương lái theo doanh thu trong 1 vụ. */
    @Query("""
        SELECT
            traderName,
            SUM(totalAmount) as revenue,
            COUNT(*) as deals
        FROM cards
        WHERE ownerUid = :uid AND seasonLabel = :season AND traderName != ''
        GROUP BY traderName
        ORDER BY revenue DESC
        LIMIT 5
    """)
    fun getTopTraders(season: String, uid: String): Flow<List<TraderStat>>

    @Query(QUERY_SEASON_COMPARISON)
    fun getAllSeasonsComparison(uid: String): Flow<List<SeasonStatsWithLabel>>

    /**
     * Lịch sử thương lái đã mua ruộng — aggregate toàn bộ cards.
     * Group theo (traderName, traderPhone) vì có thể cùng tên nhưng khác SDT.
     * COALESCE để trader không có SDT (cards cũ) vẫn group được.
     */
    @Query("""
        SELECT
            traderName,
            COALESCE(traderPhone, '') as traderPhone,
            MAX(cccd) as traderCccd,
            COUNT(*) as deals,
            SUM(totalAmount) as totalRevenue,
            SUM(netWeight) as totalWeight,
            MAX(date) as lastDealDate
        FROM cards
        WHERE ownerUid = :uid AND traderName != ''
        GROUP BY traderName, traderPhone
        ORDER BY lastDealDate DESC
    """)
    fun getTraderHistory(uid: String): Flow<List<TraderHistoryItem>>

    /**
     * One-shot migration: gán toàn bộ cards "orphan" (ownerUid = '')
     * cho user đầu tiên đăng nhập sau update v12.
     *
     * Phù hợp với app cá nhân 1 user/máy trước đó — user A login lại sau update
     * sẽ thấy lại đúng data của mình. Sau lần claim đầu tiên, không còn orphan
     * trên device này, các user kế tiếp đăng nhập (user B, C…) bắt đầu data trắng.
     */
    @Query("UPDATE cards SET ownerUid = :uid WHERE ownerUid = ''")
    suspend fun claimOrphanCards(uid: String): Int

    /**
     * Đếm cards orphan để CanLuaApplication quyết định có cần claim hay không
     * (tránh ghi DB không cần thiết mỗi lần app start).
     */
    @Query("SELECT COUNT(*) FROM cards WHERE ownerUid = ''")
    suspend fun countOrphanCards(): Int

    // === Phase 4: Cross-device sync (v13) ===

    /** Tìm card local theo Firestore id — dùng dedup khi pull về máy mới. */
    @Query("SELECT * FROM cards WHERE firestoreId = :fsId LIMIT 1")
    suspend fun getByFirestoreId(fsId: String): Card?

    /** Stamp Firestore doc id sau khi push thành công. */
    @Query("UPDATE cards SET firestoreId = :fsId WHERE id = :localId")
    suspend fun updateFirestoreId(localId: Long, fsId: String)

    /** Tất cả cards của user hiện tại — dùng để loop push lên cloud. */
    @Query("SELECT * FROM cards WHERE ownerUid = :uid")
    suspend fun getAllCardsForOwnerSync(uid: String): List<Card>

    /**
     * Đếm số phiếu user tạo từ mốc `sinceMs` (timestamp ms epoch). Dùng cho
     * Premium gate "tối đa N phiếu/ngày cho free user". `sinceMs` thường là
     * 00:00 hôm nay theo timezone local.
     */
    @Query("SELECT COUNT(*) FROM cards WHERE ownerUid = :uid AND date >= :sinceMs")
    suspend fun countCardsSince(uid: String, sinceMs: Long): Int

    /**
     * Fallback dedup khi pull về cloud: tìm card local chưa có `firestoreId`
     * (sync trước v13) khớp với composite key (ownerUid + date + name + totalWeight).
     * Tránh tạo duplicate khi user logout/login lại trên cùng máy.
     *
     * Trả về card đầu tiên match — caller stamp `firestoreId` cho row đó.
     */
    @Query("""
        SELECT * FROM cards
        WHERE ownerUid = :uid
          AND firestoreId IS NULL
          AND date = :date
          AND name = :name
          AND ABS(totalWeight - :totalWeight) < 0.01
        LIMIT 1
    """)
    suspend fun findOrphanFirestoreMatch(
        uid: String,
        date: Long,
        name: String,
        totalWeight: Double
    ): Card?

    /** Cards của user chưa từng sync lên Firestore — dùng cho backfill push. */
    @Query("SELECT * FROM cards WHERE ownerUid = :uid AND firestoreId IS NULL")
    suspend fun getUnsyncedCards(uid: String): List<Card>

    companion object {
        const val QUERY_SEASON_STATS = """
            SELECT 
                COUNT(*) as cardCount,
                SUM(netWeight) as totalNetWeight,
                SUM(totalAmount) as totalRevenue,
                SUM(paidAmount) as totalPaid,
                SUM(remainingAmount) as totalRemaining,
                AVG(NULLIF(pricePerKg, 0)) as avgPrice,
                AVG(NULLIF(moisturePercent, 0)) as avgMoisture,
                SUM(bagCount) as totalBags,
                SUM(CASE WHEN impurityIsPercent = 1 THEN
                    (CASE WHEN (totalWeight - (CASE WHEN bagMethodIsSampling = 1 AND bagSampleCount > 0 THEN (bagSampleTotalWeight * 1.0 / bagSampleCount) * bagCount ELSE bagCount * bagWeight END)) < 0.0 THEN 0.0
                     ELSE (totalWeight - (CASE WHEN bagMethodIsSampling = 1 AND bagSampleCount > 0 THEN (bagSampleTotalWeight * 1.0 / bagSampleCount) * bagCount ELSE bagCount * bagWeight END))
                     END) * (impurityWeight / 100.0)
                ELSE
                    impurityWeight
                END) as totalImpurity,
                SUM(CASE WHEN moisturePercent > 14.0 THEN 1 ELSE 0 END) as wetCardCount,
                SUM(CASE WHEN moisturePercent <= 14.0 AND moisturePercent > 0.0 THEN 1 ELSE 0 END) as dryCardCount
            FROM cards 
            WHERE ownerUid = :uid AND seasonLabel = :season
        """
        
        const val QUERY_OVERALL_STATS = """
            SELECT 
                COUNT(*) as cardCount,
                SUM(netWeight) as totalNetWeight,
                SUM(totalAmount) as totalRevenue,
                SUM(paidAmount) as totalPaid,
                SUM(remainingAmount) as totalRemaining,
                AVG(NULLIF(pricePerKg, 0)) as avgPrice,
                AVG(NULLIF(moisturePercent, 0)) as avgMoisture,
                SUM(bagCount) as totalBags,
                SUM(CASE WHEN impurityIsPercent = 1 THEN
                    (CASE WHEN (totalWeight - (CASE WHEN bagMethodIsSampling = 1 AND bagSampleCount > 0 THEN (bagSampleTotalWeight * 1.0 / bagSampleCount) * bagCount ELSE bagCount * bagWeight END)) < 0.0 THEN 0.0
                     ELSE (totalWeight - (CASE WHEN bagMethodIsSampling = 1 AND bagSampleCount > 0 THEN (bagSampleTotalWeight * 1.0 / bagSampleCount) * bagCount ELSE bagCount * bagWeight END))
                     END) * (impurityWeight / 100.0)
                ELSE
                    impurityWeight
                END) as totalImpurity,
                SUM(CASE WHEN moisturePercent > 14.0 THEN 1 ELSE 0 END) as wetCardCount,
                SUM(CASE WHEN moisturePercent <= 14.0 AND moisturePercent > 0.0 THEN 1 ELSE 0 END) as dryCardCount
            FROM cards 
            WHERE ownerUid = :uid
        """

        const val QUERY_SEASON_COMPARISON = """
            SELECT 
                seasonLabel as season,
                COUNT(*) as cardCount,
                SUM(netWeight) as totalNetWeight,
                SUM(totalAmount) as totalRevenue,
                SUM(paidAmount) as totalPaid,
                SUM(remainingAmount) as totalRemaining,
                AVG(NULLIF(pricePerKg, 0)) as avgPrice,
                AVG(NULLIF(moisturePercent, 0)) as avgMoisture,
                SUM(bagCount) as totalBags,
                SUM(CASE WHEN impurityIsPercent = 1 THEN
                    (CASE WHEN (totalWeight - (CASE WHEN bagMethodIsSampling = 1 AND bagSampleCount > 0 THEN (bagSampleTotalWeight * 1.0 / bagSampleCount) * bagCount ELSE bagCount * bagWeight END)) < 0.0 THEN 0.0
                     ELSE (totalWeight - (CASE WHEN bagMethodIsSampling = 1 AND bagSampleCount > 0 THEN (bagSampleTotalWeight * 1.0 / bagSampleCount) * bagCount ELSE bagCount * bagWeight END))
                     END) * (impurityWeight / 100.0)
                ELSE
                    impurityWeight
                END) as totalImpurity,
                SUM(CASE WHEN moisturePercent > 14.0 THEN 1 ELSE 0 END) as wetCardCount,
                SUM(CASE WHEN moisturePercent <= 14.0 AND moisturePercent > 0.0 THEN 1 ELSE 0 END) as dryCardCount,
                MAX(date) as lastDate 
            FROM cards 
            WHERE ownerUid = :uid AND seasonLabel != '' 
            GROUP BY seasonLabel 
            ORDER BY lastDate DESC 
            LIMIT 6
        """
    }
}

/** Helper data class cho query getAllSeasonsComparison — Room map theo column name. */
data class SeasonStatsWithLabel(
    val season: String,
    val cardCount: Int = 0,
    val totalNetWeight: Double? = null,
    val totalRevenue: Double? = null,
    val totalPaid: Double? = null,
    val totalRemaining: Double? = null,
    val avgPrice: Double? = null,
    val avgMoisture: Double? = null,
    val totalBags: Int = 0,
    val totalImpurity: Double? = null,
    val wetCardCount: Int = 0,
    val dryCardCount: Int = 0,
    val lastDate: Long = 0L
)

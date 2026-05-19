package com.GiaThinh.canlua.data.dao

import androidx.room.*
import com.GiaThinh.canlua.data.model.Card
import com.GiaThinh.canlua.data.model.SeasonStatsRaw
import com.GiaThinh.canlua.data.model.TraderHistoryItem
import com.GiaThinh.canlua.data.model.TraderStat
import com.GiaThinh.canlua.data.model.VarietyStat
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Query("SELECT * FROM cards ORDER BY date DESC")
    fun getAllCards(): Flow<List<Card>>

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun getCardById(id: Long): Card?

    @Insert
    suspend fun insertCard(card: Card): Long

    @Update
    suspend fun updateCard(card: Card)

    @Delete
    suspend fun deleteCard(card: Card)

    @Query("DELETE FROM cards WHERE id = :id")
    suspend fun deleteCardById(id: Long)

    // === Phase 1: QR Handshake queries ===

    @Query("SELECT * FROM cards WHERE qrToken = :token LIMIT 1")
    suspend fun findByQrToken(token: String): Card?

    @Query("UPDATE cards SET qrToken = :token WHERE id = :cardId")
    suspend fun updateQrToken(cardId: Long, token: String)

    @Query("UPDATE cards SET isLocked = 1, lockedByTraderId = :traderId WHERE id = :cardId")
    suspend fun lockCard(cardId: Long, traderId: String)

    // === Phase 1: Filter & search ===

    @Query("SELECT * FROM cards WHERE riceVariety = :variety ORDER BY date DESC")
    fun getCardsByRiceVariety(variety: String): Flow<List<Card>>

    @Query("SELECT DISTINCT riceVariety FROM cards WHERE riceVariety != '' ORDER BY riceVariety")
    fun getDistinctRiceVarieties(): Flow<List<String>>

    // === Phase 3: Season Statistics Dashboard ===

    /** Lấy danh sách các vụ đã có dữ liệu, sort theo ngày card mới nhất trong vụ đó. */
    @Query("""
        SELECT seasonLabel FROM cards 
        WHERE seasonLabel != '' 
        GROUP BY seasonLabel 
        ORDER BY MAX(date) DESC
    """)
    fun getDistinctSeasons(): Flow<List<String>>

    /**
     * Aggregate stats cho 1 vụ.
     * Trả null khi không có card nào (Room trả empty row → mapper handle).
     * COUNT/SUM trên empty set trả 0/null tương ứng → SeasonStatsRaw fields nullable.
     */
    @Query("""
        SELECT 
            COUNT(*) as cardCount,
            SUM(netWeight) as totalNetWeight,
            SUM(totalAmount) as totalRevenue,
            SUM(paidAmount) as totalPaid,
            SUM(remainingAmount) as totalRemaining,
            AVG(NULLIF(pricePerKg, 0)) as avgPrice,
            AVG(NULLIF(moisturePercent, 0)) as avgMoisture,
            SUM(bagCount) as totalBags
        FROM cards 
        WHERE seasonLabel = :season
    """)
    fun getSeasonStats(season: String): Flow<SeasonStatsRaw>

    /** Tổng quan toàn bộ data — dùng khi user chưa chọn vụ nào / chưa chuẩn hóa. */
    @Query("""
        SELECT 
            COUNT(*) as cardCount,
            SUM(netWeight) as totalNetWeight,
            SUM(totalAmount) as totalRevenue,
            SUM(paidAmount) as totalPaid,
            SUM(remainingAmount) as totalRemaining,
            AVG(NULLIF(pricePerKg, 0)) as avgPrice,
            AVG(NULLIF(moisturePercent, 0)) as avgMoisture,
            SUM(bagCount) as totalBags
        FROM cards
    """)
    fun getOverallStats(): Flow<SeasonStatsRaw>

    /** Phân bổ giống lúa trong 1 vụ — sort by weight desc. */
    @Query("""
        SELECT 
            riceVariety as variety,
            SUM(netWeight) as weight,
            COUNT(*) as count
        FROM cards 
        WHERE seasonLabel = :season AND riceVariety != ''
        GROUP BY riceVariety 
        ORDER BY weight DESC
    """)
    fun getVarietyBreakdown(season: String): Flow<List<VarietyStat>>

    /** Top 5 thương lái theo doanh thu trong 1 vụ. */
    @Query("""
        SELECT 
            traderName,
            SUM(totalAmount) as revenue,
            COUNT(*) as deals
        FROM cards 
        WHERE seasonLabel = :season AND traderName != ''
        GROUP BY traderName 
        ORDER BY revenue DESC 
        LIMIT 5
    """)
    fun getTopTraders(season: String): Flow<List<TraderStat>>

    /**
     * Aggregate cho TẤT CẢ vụ — dùng cho bar chart so sánh giữa các vụ.
     * Trả về list các (season, totalNetWeight, totalRevenue) — sort newest first.
     */
    @Query("""
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
            MAX(date) as lastDate
        FROM cards 
        WHERE seasonLabel != ''
        GROUP BY seasonLabel 
        ORDER BY lastDate DESC
        LIMIT 6
    """)
    fun getAllSeasonsComparison(): Flow<List<SeasonStatsWithLabel>>

    /**
     * Lịch sử thương lái đã mua ruộng — aggregate toàn bộ cards.
     * Group theo (traderName, traderPhone) vì có thể cùng tên nhưng khác SDT.
     * COALESCE để trader không có SDT (cards cũ) vẫn group được.
     */
    @Query("""
        SELECT 
            traderName,
            COALESCE(traderPhone, '') as traderPhone,
            COUNT(*) as deals,
            SUM(totalAmount) as totalRevenue,
            SUM(netWeight) as totalWeight,
            MAX(date) as lastDealDate
        FROM cards 
        WHERE traderName != ''
        GROUP BY traderName, traderPhone
        ORDER BY lastDealDate DESC
    """)
    fun getTraderHistory(): Flow<List<TraderHistoryItem>>
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
    val lastDate: Long = 0L
)


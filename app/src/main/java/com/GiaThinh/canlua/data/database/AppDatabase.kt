package com.GiaThinh.canlua.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.GiaThinh.canlua.data.dao.CardDao
import com.GiaThinh.canlua.data.dao.NewsArticleDao
import com.GiaThinh.canlua.data.dao.RicePriceDao
import com.GiaThinh.canlua.data.dao.TransactionDao
import com.GiaThinh.canlua.data.dao.WeatherCacheDao
import com.GiaThinh.canlua.data.dao.WeightEntryDao
import com.GiaThinh.canlua.data.model.Card
import com.GiaThinh.canlua.data.model.NewsArticle
import com.GiaThinh.canlua.data.model.PricePoint
import com.GiaThinh.canlua.data.model.RicePrice
import com.GiaThinh.canlua.data.model.Transaction
import com.GiaThinh.canlua.data.model.WeightEntry
import com.GiaThinh.canlua.data.model.Profile
import com.GiaThinh.canlua.data.model.WeatherCache
import com.GiaThinh.canlua.data.converter.DateConverter

@Database(
    entities = [
        Card::class,
        WeightEntry::class,
        Transaction::class,
        Profile::class,
        RicePrice::class,
        PricePoint::class,
        WeatherCache::class,
        NewsArticle::class
    ],
    version = 14,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun weightEntryDao(): WeightEntryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun profileDao(): com.GiaThinh.canlua.data.dao.ProfileDao
    abstract fun ricePriceDao(): RicePriceDao
    abstract fun weatherCacheDao(): WeatherCacheDao
    abstract fun newsArticleDao(): NewsArticleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "canlua_database"
                ).addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11,
                    MIGRATION_11_12,
                    MIGRATION_12_13,
                    MIGRATION_13_14
                ).build()
                INSTANCE = instance
                instance
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE cards ADD COLUMN traderName TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `profiles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `phone` TEXT NOT NULL, `region` TEXT NOT NULL, `note` TEXT NOT NULL, `role` TEXT NOT NULL)"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE profiles ADD COLUMN cccd TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE profiles ADD COLUMN username TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE profiles ADD COLUMN email TEXT NOT NULL DEFAULT ''")
            }
        }

        /** Phase 1: thêm giống lúa, độ ẩm, vụ mùa, QR token, trader ID */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE cards ADD COLUMN riceVariety TEXT NOT NULL DEFAULT ''"
                )
                database.execSQL(
                    "ALTER TABLE cards ADD COLUMN moisturePercent REAL NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE cards ADD COLUMN seasonLabel TEXT NOT NULL DEFAULT ''"
                )
                database.execSQL(
                    "ALTER TABLE cards ADD COLUMN qrToken TEXT"
                )
                database.execSQL(
                    "ALTER TABLE cards ADD COLUMN lockedByTraderId TEXT"
                )
            }
        }

        /** Phase 2.1: Module Thị Trường — bảng giá lúa & lịch sử giá */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `rice_prices` (
                        `id` TEXT NOT NULL,
                        `variety` TEXT NOT NULL,
                        `priceMin` REAL NOT NULL,
                        `priceMax` REAL NOT NULL,
                        `priceAvg7d` REAL NOT NULL,
                        `region` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `traderId` TEXT,
                        `traderName` TEXT,
                        `trend` TEXT NOT NULL DEFAULT 'STABLE',
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `price_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `variety` TEXT NOT NULL,
                        `date` INTEGER NOT NULL,
                        `priceMin` REAL NOT NULL,
                        `priceMax` REAL NOT NULL,
                        `priceAvg` REAL NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        /** Phase 2.3: Cache thời tiết offline */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `weather_cache` (
                        `id` INTEGER PRIMARY KEY NOT NULL,
                        `location` TEXT NOT NULL,
                        `temperature` INTEGER NOT NULL,
                        `feelsLike` INTEGER NOT NULL,
                        `condition` TEXT NOT NULL,
                        `rainChance` INTEGER NOT NULL,
                        `humidity` INTEGER NOT NULL,
                        `windSpeed` REAL NOT NULL,
                        `iconKey` TEXT NOT NULL,
                        `advisory` TEXT,
                        `cachedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        /** Phase 2.6: GPS location cho mỗi thẻ — phục vụ RiceMapScreen */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE cards ADD COLUMN latitude REAL")
                database.execSQL("ALTER TABLE cards ADD COLUMN longitude REAL")
            }
        }

        /** Phase 2.7: NewsFeed — bài báo nông nghiệp từ RSS */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `news_articles` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `link` TEXT NOT NULL,
                        `source` TEXT NOT NULL,
                        `thumbnail` TEXT,
                        `publishedAt` INTEGER NOT NULL,
                        `topic` TEXT NOT NULL,
                        `cachedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `idx_news_topic_published` ON `news_articles` (`topic`, `publishedAt`)"
                )
            }
        }

        /** Phase 2.8: Trader phone + field address (modern CardInfoCard) */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE cards ADD COLUMN traderPhone TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE cards ADD COLUMN fieldAddress TEXT NOT NULL DEFAULT ''")
            }
        }

        /**
         * Phase 3 — Profile keyed by Firebase UID.
         * Bảng cũ dùng `id INTEGER autoGenerate` + DAO `LIMIT 1` → các tài khoản trên cùng máy
         * ghi đè role của nhau. Drop & recreate là cách duy nhất chữa triệt để vì không có
         * đường map row cũ sang UID đúng (1 máy có thể từng login nhiều UID khác nhau).
         * Trade-off: user vào lại ProfileSetup 1 lần — chấp nhận được vì đây là bug fix.
         */
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS profiles")
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `profiles` (
                        `uid` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `phone` TEXT NOT NULL,
                        `region` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `role` TEXT NOT NULL,
                        `cccd` TEXT NOT NULL,
                        `username` TEXT NOT NULL,
                        `email` TEXT NOT NULL,
                        `roleGrantedBy` TEXT NOT NULL DEFAULT 'self',
                        `roleGrantedAt` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`uid`)
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Phase 4 — Per-user data isolation cho `cards`.
         *
         * Bảng `cards` cũ KHÔNG có cột `ownerUid` → trên cùng máy, user B đăng nhập
         * thấy phiếu của user A (data leak nghiêm trọng).
         *
         * Migration:
         *  1. ALTER TABLE thêm `ownerUid TEXT NOT NULL DEFAULT ''`
         *  2. CREATE INDEX để query `WHERE ownerUid = :uid` chạy nhanh
         *
         * Cards cũ có `ownerUid = ''` (orphan) — sẽ được `CardDao.claimOrphanCards()`
         * gán cho user đầu tiên đăng nhập sau update (xem `CanLuaApplication`).
         */
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE cards ADD COLUMN ownerUid TEXT NOT NULL DEFAULT ''")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_cards_ownerUid` ON `cards` (`ownerUid`)")
            }
        }

        /**
         * Phase 4 — Cross-device sync (Pull from Firestore).
         *
         * Thêm cột `firestoreId` vào 3 bảng để làm khoá dedup khi pull về máy mới:
         * cùng firestoreId → cùng entity. Local Room id (autoincrement) khác nhau
         * giữa các thiết bị nên không dùng dedup được.
         *
         * Index trên firestoreId để query `getByFirestoreId(fsId)` chạy nhanh.
         */
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE cards ADD COLUMN firestoreId TEXT")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_cards_firestoreId` ON `cards` (`firestoreId`)")
                database.execSQL("ALTER TABLE weight_entries ADD COLUMN firestoreId TEXT")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_weight_entries_firestoreId` ON `weight_entries` (`firestoreId`)")
                database.execSQL("ALTER TABLE transactions ADD COLUMN firestoreId TEXT")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_firestoreId` ON `transactions` (`firestoreId`)")
            }
        }

        /**
         * Phase 4 — Conflict resolution: thêm `lastModifiedMs` cho Card.
         *
         * Trước đây pull cards = cloud wins blanket (overwrite local). Khi user
         * sửa card offline rồi pull về thì mất sửa. Bây giờ so sánh
         * `lastModifiedMs` local vs `syncTimestamp` cloud — bản mới hơn thắng.
         *
         * Default = `date.time` (mili giây tạo card) cho cards cũ — đủ tốt cho
         * lần đầu, các update sau sẽ tự stamp.
         */
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE cards ADD COLUMN lastModifiedMs INTEGER NOT NULL DEFAULT 0")
                database.execSQL("UPDATE cards SET lastModifiedMs = date WHERE lastModifiedMs = 0")
            }
        }
    }
}

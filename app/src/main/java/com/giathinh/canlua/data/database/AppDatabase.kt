package com.giathinh.canlua.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.giathinh.canlua.data.dao.CardDao
import com.giathinh.canlua.data.dao.TransactionDao
import com.giathinh.canlua.data.dao.WeightEntryDao
import com.giathinh.canlua.data.model.Card
import com.giathinh.canlua.data.model.Transaction
import com.giathinh.canlua.data.model.WeightEntry
import com.giathinh.canlua.data.model.DeletedCard
import com.giathinh.canlua.data.dao.ProfileDao
import com.giathinh.canlua.data.dao.DeletedCardDao
import com.giathinh.canlua.data.model.Profile
import com.giathinh.canlua.data.converter.DateConverter

@Database(
    entities = [
        Card::class,
        WeightEntry::class,
        Transaction::class,
        Profile::class,
        DeletedCard::class
    ],
    version = 19,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun weightEntryDao(): WeightEntryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun profileDao(): ProfileDao
    abstract fun deletedCardDao(): DeletedCardDao

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
                    MIGRATION_13_14,
                    MIGRATION_14_15,
                    MIGRATION_15_16,
                    MIGRATION_16_17,
                    MIGRATION_17_18,
                    MIGRATION_18_19
                ).setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .build()
                INSTANCE = instance
                instance
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE cards ADD COLUMN traderName TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `profiles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `phone` TEXT NOT NULL, `region` TEXT NOT NULL, `note` TEXT NOT NULL, `role` TEXT NOT NULL)"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE profiles ADD COLUMN cccd TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE profiles ADD COLUMN username TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE profiles ADD COLUMN email TEXT NOT NULL DEFAULT ''")
            }
        }

        /** Phase 1: them giong lua, do am, vu mua va legacy sync fields */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE cards ADD COLUMN riceVariety TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "ALTER TABLE cards ADD COLUMN moisturePercent REAL NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE cards ADD COLUMN seasonLabel TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "ALTER TABLE cards ADD COLUMN qrToken TEXT"
                )
                db.execSQL(
                    "ALTER TABLE cards ADD COLUMN lockedByTraderId TEXT"
                )
            }
        }

        /** Phase 2.1: Module Thị Trường — bảng giá lúa & lịch sử giá */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
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
                db.execSQL(
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
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
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
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cards ADD COLUMN latitude REAL")
                db.execSQL("ALTER TABLE cards ADD COLUMN longitude REAL")
            }
        }

        /** Phase 2.7: NewsFeed — bài báo nông nghiệp từ RSS */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
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
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `idx_news_topic_published` ON `news_articles` (`topic`, `publishedAt`)"
                )
            }
        }

        /** Phase 2.8: Trader phone + field address (modern CardInfoCard) */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cards ADD COLUMN traderPhone TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE cards ADD COLUMN fieldAddress TEXT NOT NULL DEFAULT ''")
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
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS profiles")
                db.execSQL(
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
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cards ADD COLUMN ownerUid TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cards_ownerUid` ON `cards` (`ownerUid`)")
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
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cards ADD COLUMN firestoreId TEXT")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cards_firestoreId` ON `cards` (`firestoreId`)")
                db.execSQL("ALTER TABLE weight_entries ADD COLUMN firestoreId TEXT")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_weight_entries_firestoreId` ON `weight_entries` (`firestoreId`)")
                db.execSQL("ALTER TABLE transactions ADD COLUMN firestoreId TEXT")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_firestoreId` ON `transactions` (`firestoreId`)")
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
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cards ADD COLUMN lastModifiedMs INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE cards SET lastModifiedMs = date WHERE lastModifiedMs = 0")
            }
        }

        /**
         * Phase 5 — Tombstone table cho phiếu đã xoá.
         *
         * Fix bug: phiếu xoá rồi quay về sau pull (cloud doc tồn tại + local
         * không match nữa → INSERT lại). Tombstone lưu firestoreId của phiếu
         * đã xoá để pull dedup skip.
         *
         * Bonus: data tombstone = "Lịch sử phiếu đã xoá" cho user khôi phục.
         */
        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `deleted_cards` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `ownerUid` TEXT NOT NULL,
                        `firestoreId` TEXT,
                        `localId` INTEGER,
                        `cardJson` TEXT NOT NULL,
                        `deletedAt` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `traderName` TEXT NOT NULL,
                        `totalWeight` REAL NOT NULL,
                        `totalAmount` REAL NOT NULL,
                        `cardDate` INTEGER NOT NULL,
                        `seasonLabel` TEXT NOT NULL,
                        `riceVariety` TEXT NOT NULL,
                        `cloudDeleted` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_deleted_cards_ownerUid` ON `deleted_cards` (`ownerUid`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_deleted_cards_firestoreId` ON `deleted_cards` (`firestoreId`)")
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cards ADD COLUMN isPaid INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * Phase 6 — Per-card weigh modes.
         *
         * 3 toggle UI cho từng phiếu cân (đặt trên TopBar trang Cân lúa):
         *  1. `impurityIsPercent`: tạp chất nhập kg (false, default) hay % (true)
         *  2. `bagMethodIsSampling`: bao bì = 1-bao-đơn-vị × số bao (false, default)
         *     hay = (mẫu n bao ra X kg)/n × tổng bao (true). Khi true dùng
         *     `bagSampleCount` + `bagSampleTotalWeight`.
         *  3. `weightInputMode`: GridCell auto-confirm sau 3 chữ số ("SMALL", default)
         *     hay 4 chữ số ("LARGE") — cho mẻ cân lớn ≥100kg/lần.
         *
         * Mọi default giữ behavior cũ → phiếu cũ không bị ảnh hưởng tính toán.
         */
        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cards ADD COLUMN impurityIsPercent INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE cards ADD COLUMN bagMethodIsSampling INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE cards ADD COLUMN bagSampleCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE cards ADD COLUMN bagSampleTotalWeight REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE cards ADD COLUMN weightInputMode TEXT NOT NULL DEFAULT 'SMALL'")
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE rice_prices ADD COLUMN riceType TEXT NOT NULL DEFAULT 'lúa Khô'")
            }
        }

        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `rice_prices`")
                db.execSQL("DROP TABLE IF EXISTS `price_history`")
                db.execSQL("DROP TABLE IF EXISTS `news_articles`")
                db.execSQL("DROP TABLE IF EXISTS `weather_cache`")
            }
        }
    }
}

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
    version = 9,
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
                    MIGRATION_8_9
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
    }
}

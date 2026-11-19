package com.giathinh.canlua.data.database

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.giathinh.canlua.data.model.CardSyncState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * T03 — Migration 19→20 chạy trên SQLite thật (không mock).
 *
 * Tạo bảng v19 tối thiểu đủ cho migration, chèn dữ liệu có timestamp trùng nhau,
 * chạy [AppDatabase.MIGRATION_19_20] rồi kiểm tra: bảo toàn dữ liệu nghiệp vụ,
 * backfill syncId duy nhất, ordinal đúng thứ tự (timestamp, id), tạo sync state.
 */
@RunWith(AndroidJUnit4::class)
class Migration19To20Test {

    private fun openV19(): SupportSQLiteDatabase {
        val config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        )
            .name(null) // in-memory
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(19) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """CREATE TABLE cards (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            ownerUid TEXT NOT NULL,
                            name TEXT NOT NULL,
                            date INTEGER NOT NULL,
                            totalWeight REAL NOT NULL DEFAULT 0,
                            firestoreId TEXT
                        )"""
                    )
                    db.execSQL(
                        """CREATE TABLE weight_entries (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            cardId INTEGER NOT NULL,
                            weight REAL NOT NULL,
                            timestamp INTEGER NOT NULL,
                            firestoreId TEXT
                        )"""
                    )
                    db.execSQL(
                        """CREATE TABLE transactions (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            cardId INTEGER NOT NULL,
                            amount REAL NOT NULL,
                            firestoreId TEXT
                        )"""
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        return FrameworkSQLiteOpenHelperFactory().create(config).writableDatabase
    }

    @Test
    fun migration_preservesData_backfillsIdentity_andOrdersEntries() {
        val db = openV19()
        try {
            db.execSQL("INSERT INTO cards (id, ownerUid, name, date, totalWeight, firestoreId) VALUES (1, 'uidA', 'Phiếu 1', 1000, 150.5, 'cloud-1')")
            db.execSQL("INSERT INTO cards (id, ownerUid, name, date, totalWeight, firestoreId) VALUES (2, 'uidA', 'Phiếu 2', 2000, 0, NULL)")
            // timestamp trùng → ordinal phải phân định bằng id
            db.execSQL("INSERT INTO weight_entries (id, cardId, weight, timestamp, firestoreId) VALUES (10, 1, 50.0, 500, 'w-cloud')")
            db.execSQL("INSERT INTO weight_entries (id, cardId, weight, timestamp, firestoreId) VALUES (11, 1, 60.0, 500, NULL)")
            db.execSQL("INSERT INTO weight_entries (id, cardId, weight, timestamp, firestoreId) VALUES (12, 1, 40.0, 400, NULL)")
            db.execSQL("INSERT INTO transactions (id, cardId, amount, firestoreId) VALUES (20, 1, 1000000, NULL)")

            AppDatabase.MIGRATION_19_20.migrate(db)

            // Dữ liệu nghiệp vụ giữ nguyên
            db.query("SELECT totalWeight, name FROM cards WHERE id = 1").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(150.5, c.getDouble(0), 0.0001)
                assertEquals("Phiếu 1", c.getString(1))
            }
            // revision mặc định = 1
            db.query("SELECT revision FROM cards WHERE id = 1").use { c ->
                assertTrue(c.moveToFirst()); assertEquals(1L, c.getLong(0))
            }
            // syncId backfill từ firestoreId cũ
            db.query("SELECT syncId FROM cards WHERE id = 1").use { c ->
                assertTrue(c.moveToFirst()); assertEquals("cloud-1", c.getString(0))
            }
            // syncId khác rỗng + duy nhất cho card không có firestoreId
            db.query("SELECT syncId FROM cards WHERE id = 2").use { c ->
                assertTrue(c.moveToFirst()); assertFalse(c.getString(0).isBlank())
            }
            db.query("SELECT COUNT(DISTINCT syncId) FROM cards").use { c ->
                assertTrue(c.moveToFirst()); assertEquals(2, c.getInt(0))
            }
            // ordinal: id12(ts400)=0, id10(ts500)=1, id11(ts500,id lớn hơn)=2
            db.query("SELECT ordinal FROM weight_entries WHERE id = 12").use { c ->
                assertTrue(c.moveToFirst()); assertEquals(0, c.getInt(0))
            }
            db.query("SELECT ordinal FROM weight_entries WHERE id = 10").use { c ->
                assertTrue(c.moveToFirst()); assertEquals(1, c.getInt(0))
            }
            db.query("SELECT ordinal FROM weight_entries WHERE id = 11").use { c ->
                assertTrue(c.moveToFirst()); assertEquals(2, c.getInt(0))
            }
            // sync state được tạo cho mỗi card, trạng thái LEGACY
            db.query("SELECT COUNT(*), protocolState FROM card_sync_state").use { c ->
                assertTrue(c.moveToFirst()); assertEquals(2, c.getInt(0))
                assertEquals(CardSyncState.STATE_LEGACY, c.getString(1))
            }
            // weight entry syncId backfill từ firestoreId
            db.query("SELECT syncId FROM weight_entries WHERE id = 10").use { c ->
                assertTrue(c.moveToFirst()); assertEquals("w-cloud", c.getString(0))
            }
        } finally {
            db.close()
        }
    }
}
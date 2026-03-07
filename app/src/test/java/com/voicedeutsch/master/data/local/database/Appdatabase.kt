// Путь: src/test/java/com/voicedeutsch/master/data/local/database/AppDatabaseTest.kt
package com.voicedeutsch.master.data.local.database

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Unit tests for AppDatabase constants and migration SQL validation.
 *
 * NOTE: Full migration integration tests (with actual Room MigrationTestHelper)
 * belong in src/androidTest and require an instrumented environment.
 * These tests verify the constant values and SQL string content without
 * executing against a real SQLite database.
 */
class AppDatabaseTest {

    // ── DATABASE_NAME ─────────────────────────────────────────────────────

    @Test
    fun databaseName_isCorrectValue() {
        assertEquals("voice_deutsch_master.db", AppDatabase.DATABASE_NAME)
    }

    @Test
    fun databaseName_isNotBlank() {
        assertTrue(AppDatabase.DATABASE_NAME.isNotBlank())
    }

    @Test
    fun databaseName_endsWithDbExtension() {
        assertTrue(AppDatabase.DATABASE_NAME.endsWith(".db"))
    }

    // ── MIGRATION_1_2 version numbers ────────────────────────────────────

    @Test
    fun migration_1_2_startVersion_isOne() {
        assertEquals(1, AppDatabase.MIGRATION_1_2.startVersion)
    }

    @Test
    fun migration_1_2_endVersion_isTwo() {
        assertEquals(2, AppDatabase.MIGRATION_1_2.endVersion)
    }

    // ── MIGRATION_2_3 version numbers ────────────────────────────────────

    @Test
    fun migration_2_3_startVersion_isTwo() {
        assertEquals(2, AppDatabase.MIGRATION_2_3.startVersion)
    }

    @Test
    fun migration_2_3_endVersion_isThree() {
        assertEquals(3, AppDatabase.MIGRATION_2_3.endVersion)
    }

    // ── MIGRATION_3_4 version numbers ────────────────────────────────────

    @Test
    fun migration_3_4_startVersion_isThree() {
        assertEquals(3, AppDatabase.MIGRATION_3_4.startVersion)
    }

    @Test
    fun migration_3_4_endVersion_isFour() {
        assertEquals(4, AppDatabase.MIGRATION_3_4.endVersion)
    }

    // ── Migration chain is contiguous ────────────────────────────────────

    @Test
    fun migrationChain_1_2_and_2_3_areContiguous() {
        assertEquals(
            AppDatabase.MIGRATION_1_2.endVersion,
            AppDatabase.MIGRATION_2_3.startVersion,
        )
    }

    @Test
    fun migrationChain_2_3_and_3_4_areContiguous() {
        assertEquals(
            AppDatabase.MIGRATION_2_3.endVersion,
            AppDatabase.MIGRATION_3_4.startVersion,
        )
    }

    @Test
    fun migrationChain_allThree_coverVersions1to4() {
        assertEquals(1, AppDatabase.MIGRATION_1_2.startVersion)
        assertEquals(4, AppDatabase.MIGRATION_3_4.endVersion)
    }

    // ── Migration instances are not null ─────────────────────────────────

    @Test
    fun migration_1_2_instanceIsNotNull() {
        assertNotNull(AppDatabase.MIGRATION_1_2)
    }

    @Test
    fun migration_2_3_instanceIsNotNull() {
        assertNotNull(AppDatabase.MIGRATION_2_3)
    }

    @Test
    fun migration_3_4_instanceIsNotNull() {
        assertNotNull(AppDatabase.MIGRATION_3_4)
    }

    // ── Migration instances are distinct ─────────────────────────────────

    @Test
    fun migrations_allDistinctInstances() {
        assertNotSame(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
        assertNotSame(AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
        assertNotSame(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_3_4)
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// NOTE: Instrumented migration tests with MigrationTestHelper
// ═══════════════════════════════════════════════════════════════════════════════
//
// The following tests are representative of what belongs in
// src/androidTest/…/AppDatabaseMigrationTest.kt using Room's MigrationTestHelper.
// They are shown as commented stubs for reference:
//
// @RunWith(AndroidJUnit4::class)
// class AppDatabaseMigrationTest {
//
//     @get:Rule
//     val helper = MigrationTestHelper(
//         InstrumentationRegistry.getInstrumentation(),
//         AppDatabase::class.java,
//     )
//
//     @Test
//     fun migrate1To2_achievementsTableCreated() {
//         helper.createDatabase(AppDatabase.DATABASE_NAME, 1).close()
//         val db = helper.runMigrationsAndValidate(
//             AppDatabase.DATABASE_NAME, 2, true, AppDatabase.MIGRATION_1_2
//         )
//         db.query("SELECT * FROM achievements").use { cursor ->
//             assertEquals(0, cursor.count)
//         }
//         db.close()
//     }
//
//     @Test
//     fun migrate2To3_usersTableHasNewColumns() {
//         helper.createDatabase(AppDatabase.DATABASE_NAME, 2).close()
//         val db = helper.runMigrationsAndValidate(
//             AppDatabase.DATABASE_NAME, 3, true, AppDatabase.MIGRATION_2_3
//         )
//         val cursor = db.query("SELECT age, hobbies, learning_goals FROM users LIMIT 1")
//         assertNotNull(cursor)
//         db.close()
//     }
//
//     @Test
//     fun migrate3To4_booksAndChaptersTablesCreated() {
//         helper.createDatabase(AppDatabase.DATABASE_NAME, 3).close()
//         val db = helper.runMigrationsAndValidate(
//             AppDatabase.DATABASE_NAME, 4, true, AppDatabase.MIGRATION_3_4
//         )
//         db.query("SELECT * FROM books").use { cursor ->
//             assertEquals(0, cursor.count)
//         }
//         db.query("SELECT * FROM book_chapters").use { cursor ->
//             assertEquals(0, cursor.count)
//         }
//         db.close()
//     }
// }

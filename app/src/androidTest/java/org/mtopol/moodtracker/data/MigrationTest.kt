package org.mtopol.moodtracker.data

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mtopol.moodtracker.domain.QUESTION_COUNT

/**
 * Instrumented (device/emulator only) migration-correctness harness — the
 * companion to the JVM `MigrationGuardTest` tripwire. The tripwire proves a
 * migration is *registered and the schema committed*; this proves a registered
 * migration actually transforms an old database into the new schema, against a
 * real SQLite engine.
 *
 * Run with `./gradlew connectedDebugAndroidTest` (needs a connected device).
 *
 * ### Adding a migration test (do this for every `DB_VERSION` bump)
 *
 * When you add `Migration(1, 2)` to `MoodDatabase.MIGRATIONS`, add a test that
 * seeds real v1 rows, runs the migration, and asserts the data survived:
 *
 * ```
 * @Test fun migrate1To2_keepsExistingDays() {
 *     helper.createDatabase(TEST_DB, 1).apply {
 *         execSQL("INSERT INTO mood_entry (epochDay, q1, … q12) VALUES (0, 0, …)")
 *         close()
 *     }
 *     helper.runMigrationsAndValidate(TEST_DB, 2, true, *MoodDatabase.MIGRATIONS).use { db ->
 *         db.query("SELECT q1 FROM mood_entry WHERE epochDay = 0").use { c ->
 *             assertTrue(c.moveToFirst())
 *             // assert the migrated/back-filled values are what you expect
 *         }
 *     }
 * }
 * ```
 *
 * [allMigrationsProduceTheCurrentSchema] already covers the whole 1→…→VERSION
 * chain end to end; per-step tests add the *data preservation* assertions the
 * chain test cannot make.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MoodDatabase::class.java,
    )

    /**
     * Creates the database at the v1 baseline and migrates it all the way to
     * the current [MoodDatabase.VERSION] using the real registered migrations,
     * validating the result matches the exported schema for that version (and
     * that no table was unexpectedly dropped). At v1 this still earns its keep:
     * it proves the schema-asset wiring and the harness work, so the first real
     * migration lands on a verified foundation.
     */
    @Test
    fun allMigrationsProduceTheCurrentSchema() {
        helper.createDatabase(TEST_DB, 1).close()
        helper.runMigrationsAndValidate(
            TEST_DB,
            MoodDatabase.VERSION,
            true,
            *MoodDatabase.MIGRATIONS,
        ).use { db ->
            assertEquals(MoodDatabase.VERSION, db.version)
        }
    }

    /**
     * Opens the *real* compiled `MoodDatabase` on-device and round-trips a row.
     * This is the on-device counterpart to the JVM tests: it catches a schema
     * whose `identityHash` no longer matches the compiled entities (Room would
     * throw on open) — i.e. an entity changed without a version bump.
     */
    @Test
    fun compiledSchemaOpensAndRoundTripsARow() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, MoodDatabase::class.java).build()
        try {
            runBlocking {
                val answers = List(QUESTION_COUNT) { it % 4 }
                db.moodDao().upsert(moodEntryOf(epochDay = 0L, a = answers))
                val row = db.moodDao().getByDay(0L)
                assertNotNull(row)
                assertEquals(answers, row!!.answers())
            }
        } finally {
            db.close()
        }
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}

package org.mtopol.moodtracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

private const val DB_VERSION = 1

/**
 * The app has real users with stored history, so schema changes are
 * **non-destructive from here on**. To change the schema:
 *
 *  1. bump [DB_VERSION];
 *  2. add a [Migration] from the previous version to [MIGRATIONS];
 *  3. build, then commit the regenerated
 *     `app/schemas/org.mtopol.moodtracker.data.MoodDatabase/<version>.json`
 *     (exported because `exportSchema = true`) — it is the baseline the next
 *     migration diffs against.
 *
 * There is intentionally **no** `fallbackToDestructiveMigration`: a missing or
 * wrong migration must fail loudly when the database opens (Room throws) rather
 * than silently drop the user's data. `MigrationGuardTest` enforces steps 1–3
 * in plain CI; migration *correctness* additionally needs an instrumented
 * `MigrationTestHelper` test before release.
 */
@Database(entities = [MoodEntry::class], version = DB_VERSION, exportSchema = true)
abstract class MoodDatabase : RoomDatabase() {

    abstract fun moodDao(): MoodDao

    companion object {
        /** Single source of truth for the schema version (also read by tests). */
        const val VERSION = DB_VERSION

        /**
         * Ordered migrations covering every step 1→2→…→[VERSION]. Empty while
         * the schema is still at v1; an entry, once shipped, is never removed
         * or altered.
         */
        val MIGRATIONS: Array<Migration> = arrayOf()

        @Volatile
        private var instance: MoodDatabase? = null

        fun get(context: Context): MoodDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MoodDatabase::class.java,
                    "mood.db",
                )
                    .addMigrations(*MIGRATIONS)
                    // TRUNCATE (not the default WAL) keeps everything in the
                    // single mood.db file with no -wal sidecar, so Android Auto
                    // Backup can never capture a torn mid-write database. The
                    // write-throughput cost is irrelevant at one tiny row/tap.
                    .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                    .build()
                    .also { instance = it }
            }
    }
}

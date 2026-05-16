package org.mtopol.moodtracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private const val DB_VERSION = 2

/**
 * The app has real users with stored history, so schema changes are
 * **non-destructive from here on**. To change the schema:
 *
 *  1. bump [DB_VERSION];
 *  2. add a [Migration] from the previous version to [MIGRATIONS];
 *  3. build, then commit the regenerated
 *     `app/schemas/org.mtopol.moodtracker.data.CloudyDatabase/<version>.json`
 *     (exported because `exportSchema = true`) — it is the baseline the next
 *     migration diffs against.
 *
 * There is intentionally **no** `fallbackToDestructiveMigration`: a missing or
 * wrong migration must fail loudly when the database opens (Room throws) rather
 * than silently drop the user's data. `MigrationGuardTest` enforces steps 1–3
 * in plain CI; migration *correctness* additionally needs an instrumented
 * `MigrationTestHelper` test before release.
 */
@Database(entities = [CloudyEntry::class], version = DB_VERSION, exportSchema = true)
abstract class CloudyDatabase : RoomDatabase() {

    abstract fun cloudyDao(): CloudyDao

    companion object {
        /** Single source of truth for the schema version (also read by tests). */
        const val VERSION = DB_VERSION

        /**
         * v1→v2: add the optional free-form [CloudyEntry.note] column. A bare
         * `ADD COLUMN ... TEXT` is non-destructive — every existing row keeps
         * its 12 answers and gets `note = NULL` (a day with no note), which is
         * exactly what the nullable Kotlin field expects.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE mood_entry ADD COLUMN note TEXT")
            }
        }

        /**
         * Ordered migrations covering every step 1→2→…→[VERSION]. An entry,
         * once shipped, is never removed or altered.
         */
        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)

        @Volatile
        private var instance: CloudyDatabase? = null

        fun get(context: Context): CloudyDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CloudyDatabase::class.java,
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

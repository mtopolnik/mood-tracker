package org.mtopol.moodtracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * v1 schema. Pre-release we accept destructive migration: any schema change
 * bumps [version] and drops existing data (there is no real user data yet).
 * Post-1.0 this must be replaced with proper [androidx.room.migration.Migration]s.
 */
@Database(entities = [MoodEntry::class], version = 1, exportSchema = true)
abstract class MoodDatabase : RoomDatabase() {

    abstract fun moodDao(): MoodDao

    companion object {
        @Volatile
        private var instance: MoodDatabase? = null

        fun get(context: Context): MoodDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MoodDatabase::class.java,
                    "mood.db",
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
    }
}

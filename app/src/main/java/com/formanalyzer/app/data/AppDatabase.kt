package com.formanalyzer.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [TrainingSessionEntity::class, RepEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun trainingSessionDao(): TrainingSessionDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        /** One database instance for the whole app. */
        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "form_analyzer.db"
                ).build().also { instance = it }
            }
    }
}
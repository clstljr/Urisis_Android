package com.example.urisis_android.auth

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.urisis_android.urinalysis.TestRecordDao
import com.example.urisis_android.urinalysis.TestRecordEntity

@Database(
    entities = [User::class, TestRecordEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun testRecordDao(): TestRecordDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "urisis.db"
                )
                    // No formal migration: existing installs only contain
                    // user accounts (which the user can re-create) and have
                    // no test history yet. Drop the old DB rather than
                    // ship a 1→4 migration that doesn't add value.
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
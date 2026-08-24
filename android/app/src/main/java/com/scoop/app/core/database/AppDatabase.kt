package com.scoop.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.scoop.app.core.database.objects.DownloadedItem

@Database(entities = [DownloadedItem::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadHistoryDao(): DownloadHistoryDao

    companion object {
        const val DATABASE_NAME = "scoop.db"
    }
}

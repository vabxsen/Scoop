package com.scoop.app.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.scoop.app.core.database.objects.DownloadedItem
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadHistoryDao {
    @Query("SELECT * FROM downloaded_items ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DownloadedItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(item: DownloadedItem)

    @Delete suspend fun delete(item: DownloadedItem)
}

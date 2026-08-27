package com.scoop.app.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.scoop.app.core.database.objects.DownloadedItem
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadHistoryDao {
    @Query("SELECT * FROM downloaded_items ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DownloadedItem>>

    @Query("SELECT * FROM downloaded_items ORDER BY createdAt DESC")
    suspend fun getAll(): List<DownloadedItem>

    @Query("SELECT * FROM downloaded_items WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<DownloadedItem?>

    @Query("SELECT * FROM downloaded_items WHERE createdAt < :cutoffMillis")
    suspend fun getOlderThan(cutoffMillis: Long): List<DownloadedItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(item: DownloadedItem)

    @Query("DELETE FROM downloaded_items WHERE id = :id") suspend fun deleteById(id: String)

    @Query("DELETE FROM downloaded_items") suspend fun deleteAll()
}

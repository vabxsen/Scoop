package com.scoop.app.core.database.objects

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloaded_items")
data class DownloadedItem(
    @PrimaryKey val id: String,
    val sourceUrl: String,
    val title: String,
    val filePath: String?,
    val thumbnailUrl: String?,
    val kind: String,
    val createdAt: Long,
    val playlistTitle: String? = null,
)

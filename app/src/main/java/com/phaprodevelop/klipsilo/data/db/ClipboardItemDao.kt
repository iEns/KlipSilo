package com.phaprodevelop.klipsilo.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipboardItemDao {
    @Insert
    suspend fun insert(item: ClipboardItem): Long

    @Insert
    suspend fun insertAll(items: List<ClipboardItem>)

    @Update
    suspend fun update(item: ClipboardItem)

    @Query("DELETE FROM clipboard_items")
    suspend fun deleteAll()

    @Query(
        "SELECT id, contentType, filePath, previewText, timestamp, isPinned, isLocked, latitude, longitude " +
            "FROM clipboard_items ORDER BY isPinned DESC, timestamp DESC",
    )
    fun getAll(): Flow<List<ClipboardItemListRow>>

    @Query(
        "SELECT id, contentType, filePath, previewText, timestamp, isPinned, isLocked, latitude, longitude " +
            "FROM clipboard_items ORDER BY isPinned DESC, timestamp DESC",
    )
    suspend fun getAllListOnce(): List<ClipboardItemListRow>

    @Query("SELECT * FROM clipboard_items")
    suspend fun getAllOnce(): List<ClipboardItem>

    @Query("SELECT * FROM clipboard_items WHERE id = :id")
    suspend fun getById(id: Long): ClipboardItem?

    @Query("DELETE FROM clipboard_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE clipboard_items SET isPinned = :isPinned WHERE id = :id")
    suspend fun updatePinned(
        id: Long,
        isPinned: Boolean,
    )

    @Query("UPDATE clipboard_items SET isLocked = :isLocked WHERE id = :id")
    suspend fun updateLocked(
        id: Long,
        isLocked: Boolean,
    )

    @Query("UPDATE clipboard_items SET previewText = :previewText WHERE id = :id")
    suspend fun updatePreviewText(
        id: Long,
        previewText: String,
    )

    @Query("UPDATE clipboard_items SET filePath = :filePath, previewText = :previewText WHERE id = :id")
    suspend fun updateFilePathAndPreviewText(
        id: Long,
        filePath: String,
        previewText: String,
    )
}

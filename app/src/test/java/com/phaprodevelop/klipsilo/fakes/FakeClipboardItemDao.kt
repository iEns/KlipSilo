package com.phaprodevelop.klipsilo.fakes

import com.phaprodevelop.klipsilo.data.db.ClipboardItem
import com.phaprodevelop.klipsilo.data.db.ClipboardItemDao
import com.phaprodevelop.klipsilo.data.db.ClipboardItemListRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeClipboardItemDao : ClipboardItemDao {
    private val itemsById = mutableMapOf<Long, ClipboardItem>()
    private val allFlow = MutableStateFlow<List<ClipboardItemListRow>>(emptyList())

    private fun emitAll() {
        allFlow.value =
            itemsById.values
                .sortedWith(
                    compareByDescending<ClipboardItem> { it.isPinned }
                        .thenByDescending { it.timestamp },
                ).map { it.toListRow() }
    }

    override suspend fun insert(item: ClipboardItem): Long {
        itemsById[item.id] = item
        emitAll()
        return item.id
    }

    override suspend fun insertAll(items: List<ClipboardItem>) {
        items.forEach { itemsById[it.id] = it }
        emitAll()
    }

    override suspend fun update(item: ClipboardItem) {
        if (itemsById.containsKey(item.id)) {
            itemsById[item.id] = item
            emitAll()
        }
    }

    override suspend fun deleteAll() {
        itemsById.clear()
        emitAll()
    }

    override fun getAll(): Flow<List<ClipboardItemListRow>> = allFlow.asStateFlow()

    override suspend fun getAllOnce(): List<ClipboardItem> = itemsById.values.toList()

    override suspend fun getById(id: Long): ClipboardItem? = itemsById[id]

    override suspend fun deleteById(id: Long) {
        itemsById.remove(id)
        emitAll()
    }

    override suspend fun updatePinned(
        id: Long,
        isPinned: Boolean,
    ) {
        val item = itemsById[id] ?: return
        itemsById[id] = item.copy(isPinned = isPinned)
        emitAll()
    }

    override suspend fun updateLocked(
        id: Long,
        isLocked: Boolean,
    ) {
        val item = itemsById[id] ?: return
        itemsById[id] = item.copy(isLocked = isLocked)
        emitAll()
    }

    override suspend fun updatePreviewText(
        id: Long,
        previewText: String,
    ) {
        val item = itemsById[id] ?: return
        itemsById[id] = item.copy(previewText = previewText)
        emitAll()
    }

    override suspend fun updateFilePathAndPreviewText(
        id: Long,
        filePath: String,
        previewText: String,
    ) {
        val item = itemsById[id] ?: return
        itemsById[id] = item.copy(filePath = filePath, previewText = previewText)
        emitAll()
    }

    private fun ClipboardItem.toListRow() =
        ClipboardItemListRow(
            id = id,
            contentType = contentType,
            filePath = filePath,
            previewText = previewText,
            timestamp = timestamp,
            isPinned = isPinned,
            isLocked = isLocked,
            latitude = latitude,
            longitude = longitude,
        )
}

package com.example.newsnotifier

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// Data class for reading list items
@Entity(tableName = "reading_list")
data class ReadingListItem(
    @PrimaryKey val id: Int,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val url: String? = null
)

// DAO for reading list operations
@Dao
interface ReadingListDao {
    @Query("SELECT * FROM reading_list ORDER BY timestamp DESC")
    fun getAllReadingListItems(): Flow<List<ReadingListItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadingListItem(item: ReadingListItem)

    @Delete
    suspend fun deleteReadingListItem(item: ReadingListItem)

    @Query("DELETE FROM reading_list WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT * FROM reading_list WHERE id = :id")
    suspend fun getById(id: Int): ReadingListItem?
}

// Manager class for reading list operations
class ReadingListManager(private val dao: ReadingListDao) {

    val readingListFlow: Flow<List<ReadingListItem>> = dao.getAllReadingListItems()

    suspend fun addToReadingList(notificationItem: NotificationItem) {
        val readingListItem = ReadingListItem(
            id = notificationItem.id,
            title = notificationItem.title,
            content = notificationItem.content,
            url = notificationItem.url
        )
        dao.insertReadingListItem(readingListItem)
    }

    suspend fun removeFromReadingList(id: Int) {
        dao.deleteById(id)
    }

    suspend fun removeFromReadingList(item: ReadingListItem) {
        dao.deleteReadingListItem(item)
    }

    suspend fun getReadingListItem(id: Int): ReadingListItem? {
        return dao.getById(id)
    }
}
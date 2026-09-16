package com.agydroid.data.local.dao

import androidx.room.*
import com.agydroid.data.local.entities.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE projectId = :projectId ORDER BY timestamp ASC")
    fun getMessagesForProject(projectId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("UPDATE messages SET content = :content, isStreaming = :isStreaming WHERE id = :id")
    suspend fun updateMessageContent(id: String, content: String, isStreaming: Boolean)

    @Query("DELETE FROM messages WHERE projectId = :projectId")
    suspend fun deleteMessagesForProject(projectId: String)

    @Query("SELECT COUNT(*) FROM messages WHERE projectId = :projectId")
    suspend fun getMessageCount(projectId: String): Int
}

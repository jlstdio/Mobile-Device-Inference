// In a new file: app/src/main/java/com/google/mediapipe/examples/llminference/ChatDao.kt
package com.google.mediapipe.examples.llminference

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllChatMessages(): Flow<List<ChatMessageEntity>>

    @Insert
    suspend fun insertChatMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAllMessages()
}
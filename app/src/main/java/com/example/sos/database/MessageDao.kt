package com.example.sos.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE (senderId = :myId AND targetId = :targetId) OR (senderId = :targetId AND targetId = :myId) ORDER BY timestamp ASC")
    fun getChatThread(myId: String, targetId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun getAllMessagesFlow(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE isSynced = 0")
    suspend fun getUnsyncedMessages(): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessage(msg: MessageEntity)

    @Query("UPDATE messages SET isSynced = 1 WHERE messageId = :id")
    suspend fun markAsSynced(id: String)

    @Query("SELECT messageId FROM messages")
    suspend fun getAllKnownMessageIds(): List<String>

    // Gets the timestamp of the newest message you currently have
    @Query("SELECT MAX(timestamp) FROM messages")
    suspend fun getLatestMessageTimestamp(): Long?
}
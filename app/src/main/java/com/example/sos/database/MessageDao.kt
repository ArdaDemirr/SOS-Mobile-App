package com.example.sos.database

import androidx.room.*

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE (senderUuid = :myId AND receiverUuid = :targetId) OR (senderUuid = :targetId AND receiverUuid = :myId) ORDER BY timestamp ASC")
    fun getChatThread(myId: String, targetId: String): kotlinx.coroutines.flow.Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE isSynced = 0")
    suspend fun getUnsyncedMessages(): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessage(msg: MessageEntity)

    @Query("UPDATE messages SET isSynced = 1 WHERE packetId = :id")
    suspend fun markAsSynced(id: String)

    @Query("SELECT packetId FROM messages")
    suspend fun getAllKnownPacketIds(): List<String>
}
package com.example.sos.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val packetId: String, // Unique ID to prevent double-processing
    val senderUuid: String,
    val receiverUuid: String,
    val content: String,
    val timestamp: Long,
    val type: String = "TEXT",      // "TEXT" or "SOS"
    var isSynced: Boolean = false,   // Has it reached the Spring Boot server?
    var isRead: Boolean = false
)
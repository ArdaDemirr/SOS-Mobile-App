package com.example.sos.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val messageId: String,
    val senderId: String,
    val targetId: String,
    val messageType: Int = 0,
    val ttl: Int = 3,
    val isPublic: Boolean = false,
    val digitalSignature: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long,
    val content: String,
    val dogtagPayload: String? = null,
    var relayedBy: String? = null,
    var isSynced: Boolean = false
)
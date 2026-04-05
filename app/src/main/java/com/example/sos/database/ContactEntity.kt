package com.example.sos.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val contactUuid: String, // Their permanent "Phone Number"
    var displayName: String             // The nickname you give them
)
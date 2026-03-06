package com.example.sos.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dogtag_table")
data class DogtagEntity(
    @PrimaryKey
    val id: Int = 1,

    val userUuid: String,
    val fullName: String,
    val bloodType: String,
    val allergies: String,
    val medications: String,
    val pastOperations: String,
    val emergencyUuids: String
)
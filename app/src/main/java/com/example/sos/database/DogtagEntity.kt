package com.example.sos.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dogtag_table")
data class DogtagEntity(
    @PrimaryKey
    val id: Int = 1,

    val userUuid: String,
    val publicKey: String,

    val name: String,
    val surname: String,
    val gender: String, // <-- NEW
    val age: Int,
    val weight: Double,
    val height: Double,

    val bloodType: String,
    val allergies: String,
    val medications: String,
    val pastOperations: String,

    val emergencyContacts: List<String>
)
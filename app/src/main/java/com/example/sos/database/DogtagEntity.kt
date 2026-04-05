package com.example.sos.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dogtag_table")
data class DogtagEntity(
    @PrimaryKey
    val userUuid: String, // Now the primary key for the profile
    
    val publicKey: String,

    val name: String,
    val surname: String,
    val gender: String,
    val age: Int,
    val weight: Double,
    val height: Double,

    val bloodType: String,
    val allergies: String,
    val medications: String,
    val pastOperations: String,

    val emergencyContacts: List<String>
)
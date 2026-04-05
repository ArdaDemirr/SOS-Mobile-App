package com.example.sos.database

import androidx.room.*

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts")
    suspend fun getAllContacts(): List<ContactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveContact(contact: ContactEntity)

    @Delete
    suspend fun deleteContact(contact: ContactEntity)
}
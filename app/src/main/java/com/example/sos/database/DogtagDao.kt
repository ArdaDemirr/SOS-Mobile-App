package com.example.sos.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DogtagDao {
    // If a dogtag already exists, REPLACE it with the newly typed data
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDogtag(dogtag: DogtagEntity): Long

    // Fetch the single profile for this phone
    @Query("SELECT * FROM dogtag_table WHERE id = 1")
    suspend fun getDogtag(): DogtagEntity?
}
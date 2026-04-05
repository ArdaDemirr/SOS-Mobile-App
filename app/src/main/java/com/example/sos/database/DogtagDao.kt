package com.example.sos.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DogtagDao {
    // If a dogtag already exists, REPLACE it with the newly typed data
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDogtag(dogtag: DogtagEntity)

    // Fetch the single profile for this phone. Since we are using UUID as PK,
    // we just fetch the first record as this app is designed for one user profile.
    @Query("SELECT * FROM dogtag_table LIMIT 1")
    suspend fun getDogtag(): DogtagEntity?
}
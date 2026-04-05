package com.example.sos

import com.example.sos.database.DogtagEntity
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface SosApiService {
    // This matches your @PostMapping("/api/users/sync") in UserController
    @POST("api/users/sync")
    suspend fun syncDogtag(@Body dogtag: DogtagEntity): Response<DogtagEntity>
}

object RetrofitInstance {
    // Tactical Connection: Using Hostname instead of volatile IP
    // Replace this with your actual IPv4 address from the command prompt
    private const val BASE_URL = "http://192.168.1.28:8080/"

    val api: SosApiService by lazy {
        retrofit2.Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(SosApiService::class.java)
    }
}
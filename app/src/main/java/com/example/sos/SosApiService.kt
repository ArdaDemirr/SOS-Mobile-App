package com.example.sos

import com.example.sos.database.DogtagEntity
import com.example.sos.database.MessageEntity
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.DELETE
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Header

interface SosApiService {
    // This matches your @PostMapping("/api/users/sync") in UserController
    @POST("api/users/sync")
    suspend fun syncDogtag(@Body dogtag: DogtagEntity): Response<DogtagEntity>

    // 2. NEW: Push Public Waypoint
    @POST("api/waypoints")
    suspend fun createWaypoint(@Body waypoint: WaypointRequestPayload): Response<WaypointResponsePayload>

    // 3. NEW: Download Public Mesh
    @GET("api/waypoints")
    suspend fun getAllWaypoints(): Response<List<WaypointResponsePayload>>

    @DELETE("api/waypoints/{id}")
    suspend fun deleteWaypoint(
        @Path("id") id: Long,
        @Header("X-User-Id") requesterId: String
    ): Response<Unit>

    @PUT("api/waypoints/{id}")
    suspend fun updateWaypoint(
        @Path("id") id: Long,
        @Header("X-User-Id") requesterId: String,
        @Body waypoint: WaypointRequestPayload
    ): Response<WaypointResponsePayload>

    // --- NEW MESSAGING ROUTES ---
    @POST("api/messages")
    suspend fun uploadMessage(@Body msg: MessageEntity): Response<MessageEntity>

    @POST("api/messages/relay")
    suspend fun relayPacket(@Body msg: MessageEntity, @Header("X-Relay-Id") relayNodeId: String): Response<MessageEntity>
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
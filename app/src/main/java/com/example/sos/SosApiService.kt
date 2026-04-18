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
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Query
import java.io.IOException

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

    // 3. Inbox Sync (Fetch all messages targeted for YOU)
    //@GET("api/messages/target/{targetId}")
    //suspend fun fetchInbox(@Path("targetId") targetId: String): Response<List<MessageEntity>>

    @GET("api/messages/target/{targetId}")
    suspend fun fetchInbox(
        @Path("targetId") targetId: String,
        @Query("since") sinceTimestamp: Long
    ): Response<List<MessageEntity>>

    // 4. Thread Sync (Fetch the specific conversation between you and a friend)
    @GET("api/messages/conversation/{userA}/{userB}")
    suspend fun getConversation(
        @Path("userA") userA: String,
        @Path("userB") userB: String
    ): Response<List<MessageEntity>>
}

object RetrofitInstance {

    // Store the UUID here so the interceptor can use it dynamically
    var currentUserUuid: String? = null

    // The Bouncer Interceptor
    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val path = originalRequest.url.encodedPath
        val requestBuilder = originalRequest.newBuilder()

        // TIER 2 ROUTING: User endpoints get the App Secret Key
        if (path.contains("api/users")) {
            requestBuilder.header("X-Client-Secret", Secrets.CLIENT_SECRET)
        }
        // TIER 3 ROUTING: Data endpoints get the User UUID
        else {
            val uuidToSend = currentUserUuid ?: "UNKNOWN_DEVICE"
            requestBuilder.header("X-User-UUID", uuidToSend)
        }

        chain.proceed(requestBuilder.build())
    }

    private val statusInterceptor = Interceptor { chain ->
        try {
            val response = chain.proceed(chain.request())

            if (response.isSuccessful || response.code in 400..499) {
                NetworkStateManager.updateServerState(true)
            } else if (response.code == 502 || response.code == 503) {
                NetworkStateManager.updateServerState(false)
            }

            return@Interceptor response
        } catch (e: IOException) {
            NetworkStateManager.updateServerState(false)
            throw e
        }
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(statusInterceptor)
        .build()

    val api: SosApiService by lazy {
        Retrofit.Builder()
            .baseUrl(Secrets.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SosApiService::class.java)
    }
}
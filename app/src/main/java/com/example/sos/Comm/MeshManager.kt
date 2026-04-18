package com.example.sos.Comm

import android.content.Context
import android.util.Log
import com.example.sos.RetrofitInstance
import com.example.sos.database.AppDatabase
import com.example.sos.database.MessageEntity
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.gson.Gson
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

class MeshManager private constructor(private val context: Context) {
    private val TAG = "TacticalMesh"
    private val SERVICE_ID = "com.example.sos.MESH_SERVICE"
    private val STRATEGY = Strategy.P2P_CLUSTER
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val database = AppDatabase.getDatabase(context)
    private val messageDao = database.messageDao()
    private val dogtagDao = database.dogtagDao()

    // Map of EndpointID -> Remote UUID
    private val connectedEndpoints = ConcurrentHashMap<String, String>()

    companion object {
        @Volatile private var INSTANCE: MeshManager? = null
        fun getInstance(context: Context): MeshManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MeshManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun start() {
        scope.launch {
            val dogtag = dogtagDao.getDogtag()
            val myNodeId = dogtag?.userUuid ?: "UNKNOWN_NODE"

            Nearby.getConnectionsClient(context).stopAllEndpoints()
            startAdvertising(myNodeId)
            startDiscovery()
            startServerSyncLoop(myNodeId)
        }
    }

    private fun startAdvertising(myNodeId: String) {
        val options = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        Nearby.getConnectionsClient(context)
            .startAdvertising(myNodeId, SERVICE_ID, connectionLifecycleCallback, options)
    }

    private fun startDiscovery() {
        val options = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
        Nearby.getConnectionsClient(context)
            .startDiscovery(SERVICE_ID, endpointDiscoveryCallback, options)
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Nearby.getConnectionsClient(context)
                .requestConnection("TacticalNode", endpointId, connectionLifecycleCallback)
        }
        override fun onEndpointLost(endpointId: String) {}
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            // Store their Node ID (endpointName) immediately
            connectedEndpoints[endpointId] = info.endpointName
            Nearby.getConnectionsClient(context).acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                syncMessagesWithEndpoint(endpointId)
            }
        }

        override fun onDisconnected(endpointId: String) {
            connectedEndpoints.remove(endpointId)
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val json = String(payload.asBytes()!!)
                handleIncomingPacket(json, endpointId)
            }
        }
        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    private fun handleIncomingPacket(json: String, sourceEndpointId: String) {
        scope.launch {
            try {
                val msg = gson.fromJson(json, MessageEntity::class.java)
                val knownIds = messageDao.getAllKnownMessageIds()

                if (!knownIds.contains(msg.messageId)) {
                    messageDao.insertMessage(msg)
                    // MULE LOGIC: If I have internet, I push it. If not, I keep it for next mesh contact.
                    tryPushToServer(msg)
                    // RELAY: Flood to everyone else I'm currently connected to
                    relayToOtherNodes(msg, sourceEndpointId)
                }
            } catch (e: Exception) { Log.e(TAG, "Packet Error", e) }
        }
    }

    private fun syncMessagesWithEndpoint(endpointId: String) {
        scope.launch {
            // Mule rule: Send all unsynced messages to the new contact
            val unsynced = messageDao.getUnsyncedMessages()
            unsynced.forEach { sendToEndpoint(endpointId, it) }
        }
    }

    private fun startServerSyncLoop(myNodeId: String) {
        // 1. Arm the network interceptor immediately in case MainActivity didn't run
        RetrofitInstance.currentUserUuid = myNodeId

        scope.launch {
            while (isActive) {
                val unsyncedPackets = messageDao.getUnsyncedMessages()

                // 2. Use a standard for-loop so we can pause between packets
                for (msg in unsyncedPackets) {
                    tryPushToServer(msg)

                    // 3. THE COOLDOWN: Wait half a second before sending the next one
                    delay(500L)
                }

                // 4. The main heartbeat sleep
                delay(15000L)
            }
        }
    }

    private suspend fun tryPushToServer(msg: MessageEntity) {
        val myNodeId = dogtagDao.getDogtag()?.userUuid ?: "UNKNOWN"
        try {
            val response = if (msg.senderId == myNodeId) {
                RetrofitInstance.api.uploadMessage(msg)
            } else {
                // If it's someone else's message, use the relay endpoint
                RetrofitInstance.api.relayPacket(msg, myNodeId)
            }
            if (response.isSuccessful) messageDao.markAsSynced(msg.messageId)
        } catch (e: Exception) { /* No Connection */ }
    }

    private fun relayToOtherNodes(msg: MessageEntity, excludeEndpoint: String) {
        connectedEndpoints.keys.forEach { if (it != excludeEndpoint) sendToEndpoint(it, msg) }
    }

    private fun sendToEndpoint(endpointId: String, msg: MessageEntity) {
        val json = gson.toJson(msg)
        Nearby.getConnectionsClient(context).sendPayload(endpointId, Payload.fromBytes(json.toByteArray()))
    }

    fun sendMessage(msg: MessageEntity) {
        scope.launch {
            messageDao.insertMessage(msg)
            tryPushToServer(msg) // Try cloud first
            connectedEndpoints.keys.forEach { sendToEndpoint(it, msg) } // Try mesh simultaneously
        }
    }
}
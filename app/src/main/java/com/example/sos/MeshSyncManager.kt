package com.example.sos.network

import com.example.sos.database.MessageDao
import com.example.sos.database.MessageEntity
import com.google.gson.Gson
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

object MeshSyncManager {
    private val gson = Gson()

    suspend fun performTacticalSync(socket: BluetoothSocket, dao: MessageDao) = withContext(Dispatchers.IO) {
        try {
            val writer = socket.outputStream
            val reader = BufferedReader(InputStreamReader(socket.inputStream))

            // 1. HANDSHAKE: Send our known Packet IDs
            val myIds = dao.getAllKnownPacketIds()
            val hello = mapOf("type" to "HELLO", "ids" to myIds)
            writer.write((gson.toJson(hello) + "\n").toByteArray())

            // 2. LISTEN: What does the other phone have?
            val line = reader.readLine() ?: return@withContext
            val remoteData = gson.fromJson(line, Map::class.java)

            if (remoteData["type"] == "HELLO") {
                val remoteKnownIds = remoteData["ids"] as List<*>

                // 3. FLOOD: Send them every message WE have that THEY don't
                // We send EVERY message (Mule logic), not just those meant for them.
                val allMessages = dao.getUnsyncedMessages()
                allMessages.forEach { msg ->
                    if (!remoteKnownIds.contains(msg.packetId)) {
                        val packet = mapOf("type" to "DATA", "msg" to msg)
                        writer.write((gson.toJson(packet) + "\n").toByteArray())
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }
}
package com.example.sos

import android.bluetooth.BluetoothSocket
import com.example.sos.database.MessageDao
import com.example.sos.database.MessageEntity
import com.google.gson.Gson
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

            val hello = mapOf("type" to "HELLO", "ids" to dao.getAllKnownMessageIds())
            writer.write((gson.toJson(hello) + "\n").toByteArray())

            val line = reader.readLine() ?: return@withContext
            val remoteData = gson.fromJson(line, Map::class.java)

            if (remoteData["type"] == "HELLO") {
                val remoteKnownIds = remoteData["ids"] as List<*>
                dao.getUnsyncedMessages().forEach { msg ->
                    if (!remoteKnownIds.contains(msg.messageId)) {
                        writer.write((gson.toJson(mapOf("type" to "DATA", "msg" to msg)) + "\n").toByteArray())
                    }
                }
            }

            while (socket.isConnected) {
                val incoming = reader.readLine() ?: break
                val remotePacket = gson.fromJson(incoming, Map::class.java)
                if (remotePacket["type"] == "DATA") {
                    dao.insertMessage(gson.fromJson(gson.toJson(remotePacket["msg"]), MessageEntity::class.java))
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }
}
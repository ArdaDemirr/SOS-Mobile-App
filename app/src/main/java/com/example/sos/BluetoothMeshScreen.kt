package com.example.sos

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

private val KANKI_UUID: UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")
private const val KANKI_SERVICE_NAME = "KankiSOS"

data class BtDevice(val name: String, val address: String, val device: BluetoothDevice)
data class ChatMessage(val text: String, val isMine: Boolean, val sender: String = "")

@SuppressLint("MissingPermission")
@Composable
fun BluetoothMeshScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val bluetoothManager = remember { context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }
    val bluetoothAdapter = remember { bluetoothManager.adapter }

    // --- STATE ---
    var hasPermissions by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }
    var discoveredDevices by remember { mutableStateOf(listOf<BtDevice>()) }
    var connectedDevice by remember { mutableStateOf<BtDevice?>(null) }
    var socket by remember { mutableStateOf<BluetoothSocket?>(null) }
    var serverSocket by remember { mutableStateOf<BluetoothServerSocket?>(null) }
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var inputText by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("BEKLEMEDE") }
    val listState = rememberLazyListState()

    val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms -> hasPermissions = perms.values.all { it } }

    // Check permissions on launch
    LaunchedEffect(Unit) {
        hasPermissions = requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (!hasPermissions) {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    // BT Discovery broadcast receiver
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            @SuppressLint("MissingPermission")
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                        device?.let {
                            val name = try { it.name ?: "Bilinmeyen" } catch (e: Exception) { "Bilinmeyen" }
                            val btDev = BtDevice(name, it.address, it)
                            if (discoveredDevices.none { d -> d.address == btDev.address }) {
                                discoveredDevices = discoveredDevices + btDev
                            }
                        }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        isScanning = false
                        statusText = if (discoveredDevices.isEmpty()) "CİHAZ BULUNAMADI" else "${discoveredDevices.size} CİHAZ BULUNDU"
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        context.registerReceiver(receiver, filter)
        onDispose { context.unregisterReceiver(receiver) }
    }

    // Server: listen for incoming connections
    LaunchedEffect(hasPermissions) {
        if (!hasPermissions || bluetoothAdapter == null) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            try {
                val srv = bluetoothAdapter.listenUsingRfcommWithServiceRecord(KANKI_SERVICE_NAME, KANKI_UUID)
                serverSocket = srv
                val conn = srv.accept() // blocks until connection
                socket = conn
                val remote = try { conn.remoteDevice?.name ?: "Bilinmeyen" } catch (e: Exception) { "Bilinmeyen" }
                withContext(Dispatchers.Main) {
                    connectedDevice = BtDevice(remote, conn.remoteDevice?.address ?: "", conn.remoteDevice)
                    statusText = "BAĞLANDI: $remote"
                }
                srv.close()
                // Start reading
                readLoop(conn, remote) { msg ->
                    messages = messages + ChatMessage(msg, false, remote)
                }
            } catch (e: IOException) {
                // Server closed or error
            }
        }
    }

    // Scroll to bottom when new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    fun startScan() {
        if (!hasPermissions || bluetoothAdapter == null) return
        discoveredDevices = emptyList()
        isScanning = true
        statusText = "TARAMA YAPILIYOR..."
        bluetoothAdapter.startDiscovery()
    }

    fun connectTo(dev: BtDevice) {
        scope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { statusText = "BAĞLANILIYOR: ${dev.name}..." }
            try {
                bluetoothAdapter?.cancelDiscovery()
                val conn = dev.device.createRfcommSocketToServiceRecord(KANKI_UUID)
                conn.connect()
                socket = conn
                withContext(Dispatchers.Main) {
                    connectedDevice = dev
                    statusText = "BAĞLANDI: ${dev.name}"
                }
                readLoop(conn, dev.name) { msg ->
                    messages = messages + ChatMessage(msg, false, dev.name)
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) { statusText = "BAĞLANTI BAŞARISIZ: ${e.message}" }
            }
        }
    }

    fun sendMessage() {
        val msg = inputText.trim()
        if (msg.isEmpty() || socket == null) return
        scope.launch(Dispatchers.IO) {
            try {
                socket!!.outputStream.write((msg + "\n").toByteArray())
                withContext(Dispatchers.Main) {
                    messages = messages + ChatMessage(msg, true)
                    inputText = ""
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) { statusText = "GÖNDERME HATASI" }
            }
        }
    }

    // --- UI ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PipBlack)
            .systemBarsPadding()
    ) {
        // HEADER
        MeshHeader(onBack = onBack, statusText = statusText, isScanning = isScanning)

        if (!hasPermissions) {
            // No permission state
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("BLUETOOTH İZNİ GEREKLİ", color = PipRed, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .border(1.dp, PipAmber)
                            .clickable { permissionLauncher.launch(requiredPermissions) }
                            .padding(12.dp)
                    ) {
                        Text("İZİN VER", color = PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
            return@Column
        }

        if (connectedDevice != null) {
            // CHAT VIEW
            Column(modifier = Modifier.fillMaxSize()) {
                // Connected banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PipAmber.copy(alpha = 0.15f))
                        .padding(8.dp)
                ) {
                    Text(
                        "● ${connectedDevice?.name?.uppercase()} İLE BAĞLANTI KURULDU",
                        color = PipGreen,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                // Messages
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(messages) { msg ->
                        ChatBubble(msg)
                    }
                }
                // Input row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, PipAmber, RectangleShape)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        textStyle = TextStyle(color = PipAmber, fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                        cursorBrush = SolidColor(PipAmber),
                        decorationBox = { inner ->
                            if (inputText.isEmpty()) {
                                Text("mesaj yaz...", color = PipAmber.copy(alpha = 0.4f), fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                            }
                            inner()
                        }
                    )
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(PipAmber)
                            .clickable { sendMessage() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = PipBlack, modifier = Modifier.size(20.dp))
                    }
                }
            }
        } else {
            // SCAN/PAIR VIEW
            Column(modifier = Modifier.fillMaxSize()) {
                // Scan button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .border(2.dp, if (isScanning) PipAmber else PipAmber.copy(alpha = 0.6f), RectangleShape)
                        .background(if (isScanning) PipAmber.copy(alpha = 0.1f) else PipBlack)
                        .clickable { if (!isScanning) startScan() }
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        if (isScanning) {
                            val infiniteTransition = rememberInfiniteTransition(label = "scan")
                            val alpha by infiniteTransition.animateFloat(
                                initialValue = 0.2f, targetValue = 1f,
                                animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "scanAlpha"
                            )
                            Box(modifier = Modifier.size(10.dp).alpha(alpha).background(PipAmber))
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            if (isScanning) "TARAMA YAPILIYOR..." else "CİHAZ TARA",
                            color = PipAmber,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                // Paired devices (previously connected)
                if (bluetoothAdapter != null) {
                    val pairedDevices = try {
                        bluetoothAdapter.bondedDevices?.map { BtDevice(it.name ?: "Bilinmeyen", it.address, it) } ?: emptyList()
                    } catch (e: Exception) { emptyList() }

                    if (pairedDevices.isNotEmpty()) {
                        Text(
                            "  EŞLENMİŞ CİHAZLAR",
                            color = PipAmber.copy(alpha = 0.6f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        pairedDevices.forEach { dev ->
                            DeviceRow(dev, "EŞLENMİŞ") { connectTo(dev) }
                        }
                        if (discoveredDevices.isNotEmpty()) {
                            Divider(color = PipAmber.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }

                if (discoveredDevices.isNotEmpty()) {
                    Text(
                        "  YENİ CİHAZLAR",
                        color = PipAmber.copy(alpha = 0.6f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(discoveredDevices) { dev ->
                        DeviceRow(dev, "YENİ") { connectTo(dev) }
                    }
                }

                // Info box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .border(1.dp, PipAmber.copy(alpha = 0.3f), RectangleShape)
                        .padding(10.dp)
                ) {
                    Text(
                        "İki cihazda da Kanki SOS açık olmalı.\nBağlantı için aynı ağda olmak gerekmez — sadece Bluetooth menzilinde (≈10m) olun.",
                        color = PipAmber.copy(alpha = 0.5f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MeshHeader(onBack: () -> Unit, statusText: String, isScanning: Boolean) {
    val statusColor by animateColorAsState(
        if (isScanning) PipAmber else if (statusText.startsWith("BAĞLANDI")) PipGreen else PipAmber.copy(alpha = 0.7f),
        label = "statusColor"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .padding(top = 40.dp, bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(PipAmber, RoundedCornerShape(6.dp))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = PipBlack)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("BT MESH", color = PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Text(statusText, color = statusColor, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            }
        }
        Divider(color = PipAmber, thickness = 2.dp, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
fun DeviceRow(dev: BtDevice, tag: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .border(1.dp, PipAmber.copy(alpha = 0.5f), RectangleShape)
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(dev.name, color = PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(dev.address, color = PipAmber.copy(alpha = 0.5f), fontFamily = FontFamily.Monospace, fontSize = 10.sp)
        }
        Box(
            modifier = Modifier.background(PipAmber.copy(alpha = 0.15f), RoundedCornerShape(2.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(tag, color = PipAmber, fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessage) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (msg.isMine) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .border(1.dp, if (msg.isMine) PipAmber else PipAmber.copy(alpha = 0.4f), RectangleShape)
                .background(if (msg.isMine) PipAmber.copy(alpha = 0.15f) else PipBlack)
                .padding(8.dp)
        ) {
            Column {
                if (!msg.isMine && msg.sender.isNotEmpty()) {
                    Text(msg.sender, color = PipAmber.copy(alpha = 0.6f), fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                }
                Text(
                    msg.text,
                    color = if (msg.isMine) PipAmber else PipAmber.copy(alpha = 0.85f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )
            }
        }
    }
}

private suspend fun readLoop(socket: BluetoothSocket, senderName: String, onMsg: suspend (String) -> Unit) {
    withContext(Dispatchers.IO) {
        val buffer = ByteArray(1024)
        try {
            while (true) {
                val bytes = socket.inputStream.read(buffer)
                val msg = String(buffer, 0, bytes).trim()
                if (msg.isNotEmpty()) {
                    withContext(Dispatchers.Main) { onMsg(msg) }
                }
            }
        } catch (e: IOException) {
            // Connection lost
        }
    }
}

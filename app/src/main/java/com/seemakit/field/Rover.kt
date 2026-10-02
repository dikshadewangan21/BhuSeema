package com.seemakit.field
import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID

class Rover(private val ctx: Context) {
    val fix = MutableStateFlow<Fix?>(null)
    val status = MutableStateFlow("Not connected")
    private var sock: BluetoothSocket? = null

    @SuppressLint("MissingPermission")
    fun paired(): List<BluetoothDevice> = try {
        ctx.getSystemService(BluetoothManager::class.java).adapter?.bondedDevices?.toList() ?: emptyList()
    } catch (e: SecurityException) { emptyList() }

    @SuppressLint("MissingPermission")
    suspend fun connect(d: BluetoothDevice) = withContext(Dispatchers.IO) {
        try {
            close(); status.value = "Connecting..."
            val s = d.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
            s.connect(); sock = s; status.value = "Connected: ${d.name}"
            s.inputStream.bufferedReader().forEachLine { l -> Nmea.parse(l.trim())?.let { fix.value = it } }
            status.value = "Disconnected"; fix.value = null
        } catch (e: Exception) { status.value = "Error: ${e.message ?: "connection failed"}"; fix.value = null }
    }
    fun close() { try { sock?.close() } catch (_: Exception) {} }
}

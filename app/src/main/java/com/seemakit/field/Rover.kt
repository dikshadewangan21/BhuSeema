package com.seemakit.field

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID

/**
 * Handles communication with the physical RTK Rover over Bluetooth Classic SPP.
 * Also includes a built-in "Virtual Rover Simulator" for live pitching, demos,
 * and testing without needing physical hardware.
 */
class Rover(private val ctx: Context) {
    val fix = MutableStateFlow<Fix?>(null)
    val status = MutableStateFlow("Not connected")
    val isSimulating = MutableStateFlow(false)

    private var sock: BluetoothSocket? = null
    private var simJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Pre-defined demo coordinates for a ~2,450 sq m farm plot in India
    val demoCorners = listOf(
        Pair(21.251480, 81.629500), // Peg 1 (SW)
        Pair(21.251930, 81.629520), // Peg 2 (NW)
        Pair(21.251900, 81.630010), // Peg 3 (NE)
        Pair(21.251450, 81.629980)  // Peg 4 (SE)
    )
    private var currentSimIndex = 0

    @SuppressLint("MissingPermission")
    fun paired(): List<BluetoothDevice> = try {
        ctx.getSystemService(BluetoothManager::class.java).adapter?.bondedDevices?.toList() ?: emptyList()
    } catch (e: SecurityException) { emptyList() }

    @SuppressLint("MissingPermission")
    suspend fun connect(d: BluetoothDevice) = withContext(Dispatchers.IO) {
        try {
            stopSimulation()
            close()
            status.value = "Connecting..."
            val s = d.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
            s.connect()
            sock = s
            status.value = "Connected: ${d.name}"
            s.inputStream.bufferedReader().forEachLine { l ->
                Nmea.parse(l.trim())?.let { fix.value = it }
            }
            status.value = "Disconnected"
            fix.value = null
        } catch (e: Exception) {
            status.value = "Error: ${e.message ?: "connection failed"}"
            fix.value = null
        }
    }

    /**
     * Activates the Virtual Rover Simulator.
     * Emulates centimeter-level RTK Fixed GNSS streaming (Quality 4, HDOP 0.7, 24 sats).
     */
    fun startSimulation() {
        close()
        simJob?.cancel()
        isSimulating.value = true
        status.value = "Virtual RTK Rover (Demo Active)"

        // Start at corner 1
        currentSimIndex = 0
        val (startLat, startLon) = demoCorners[0]
        fix.value = Fix(startLat, startLon, quality = 4, sats = 24, hdop = 0.7)

        // Subtle realistic jitter (±1-2cm random variance)
        simJob = scope.launch {
            while (isActive) {
                delay(1000)
                fix.value?.let { current ->
                    val jitterLat = (Math.random() - 0.5) * 0.0000004
                    val jitterLon = (Math.random() - 0.5) * 0.0000004
                    fix.value = current.copy(
                        lat = current.lat + jitterLat,
                        lon = current.lon + jitterLon,
                        hdop = 0.6 + Math.random() * 0.3
                    )
                }
            }
        }
    }

    /**
     * Steps the virtual rover to the next target corner peg for quick demo workflows.
     */
    fun stepNextDemoCorner(): Int {
        if (!isSimulating.value) startSimulation()
        currentSimIndex = (currentSimIndex + 1) % demoCorners.size
        val (lat, lon) = demoCorners[currentSimIndex]
        fix.value = Fix(lat, lon, quality = 4, sats = 24, hdop = 0.7)
        return currentSimIndex + 1
    }

    /**
     * Moves virtual rover to an exact location (e.g. for stakeout testing).
     */
    fun setSimulatedLocation(lat: Double, lon: Double) {
        fix.value = Fix(lat, lon, quality = 4, sats = 24, hdop = 0.7)
    }

    fun stopSimulation() {
        simJob?.cancel()
        simJob = null
        isSimulating.value = false
        if (sock == null || !sock!!.isConnected) {
            status.value = "Not connected"
            fix.value = null
        }
    }

    fun close() {
        try { sock?.close() } catch (e: Exception) {}
        sock = null
    }
}

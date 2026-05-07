package com.example.urisis_android.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

@SuppressLint("MissingPermission") // permissions verified via hasPermissions()
class BleManager(private val context: Context) {

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager.adapter
    private val scanner get() = adapter?.bluetoothLeScanner

    private val _state = MutableStateFlow(
        BleState(isBluetoothEnabled = adapter?.isEnabled == true)
    )
    val state: StateFlow<BleState> = _state.asStateFlow()

    private var gatt: BluetoothGatt? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null
    private var scanJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Drop any device not seen in this window while scanning
    private val staleThresholdMs = 10_000L

    // Complete JSON documents — emitted only after full reassembly from
    // the Arduino's 20-byte chunks.
    private val _incoming = MutableSharedFlow<String>(
        replay = 0, extraBufferCapacity = 32
    )
    val incoming: SharedFlow<String> = _incoming

    // Reassembles partial chunks into full JSON. Populated lazily once
    // the GATT scope exists.
    private val assembler = JsonChunkAssembler(
        onDocument = { doc ->
            scope.launch { _incoming.emit(doc) }
        },
        onProtocolError = { msg ->
            Log.w(TAG, "[assembler] $msg")
        }
    )

    // ─ Scan callback — fires every time a BLE advertisement is received ────
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val entry = BleDevice(
                address = device.address,
                name = result.scanRecord?.deviceName
                    ?: runCatching { device.name }.getOrNull(),
                rssi = result.rssi
            )
            _state.update { current ->
                val list = current.discoveredDevices.toMutableList()
                val idx = list.indexOfFirst { it.address == entry.address }
                if (idx >= 0) list[idx] = entry else list += entry
                current.copy(
                    discoveredDevices = list.sortedByDescending { it.rssi }
                )
            }
        }

        override fun onScanFailed(errorCode: Int) {
            _state.update {
                it.copy(
                    connectionState = BleConnectionState.ERROR,
                    errorMessage = "Scan failed (code $errorCode)"
                )
            }
        }
    }

    // ─ Permission gate ─────────────────────────────────────────────────────
    fun hasPermissions(): Boolean {
        val required = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        return required.all {
            ContextCompat.checkSelfPermission(context, it) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    // ─ Start scan ──────────────────────────────────────────────────────────
    fun startScan() {
        val scanner = scanner ?: run {
            _state.update { it.copy(errorMessage = "Bluetooth unavailable on this device") }
            return
        }
        if (!hasPermissions()) {
            _state.update { it.copy(errorMessage = "Missing Bluetooth permissions") }
            return
        }
        if (adapter?.isEnabled != true) {
            _state.update { it.copy(errorMessage = "Please turn on Bluetooth") }
            return
        }

        _state.update {
            it.copy(
                connectionState = BleConnectionState.SCANNING,
                discoveredDevices = emptyList(),
                errorMessage = null,
                isBluetoothEnabled = true
            )
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .build()

        scanner.startScan(null, settings, scanCallback)

        // Prune stale devices every second so the list feels "live"
        scanJob?.cancel()
        scanJob = scope.launch {
            while (true) {
                delay(1_000)
                val cutoff = System.currentTimeMillis() - staleThresholdMs
                _state.update { current ->
                    current.copy(
                        discoveredDevices = current.discoveredDevices
                            .filter { it.lastSeen >= cutoff }
                    )
                }
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        runCatching { scanner?.stopScan(scanCallback) }
        _state.update {
            if (it.connectionState == BleConnectionState.SCANNING)
                it.copy(connectionState = BleConnectionState.IDLE) else it
        }
    }

    // ─ Connect / disconnect ────────────────────────────────────────────────
    fun connect(device: BleDevice) {
        val remote: BluetoothDevice = adapter?.getRemoteDevice(device.address) ?: return
        stopScan()
        assembler.reset()
        _state.update {
            it.copy(
                connectionState = BleConnectionState.CONNECTING,
                connectedDevice = device,
                errorMessage = null
            )
        }
        gatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            remote.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            remote.connectGatt(context, false, gattCallback)
        }
    }

    fun disconnect() {
        _state.update { it.copy(connectionState = BleConnectionState.DISCONNECTING) }
        gatt?.disconnect()
    }

    /**
     * Write a JSON command to the Arduino's RX characteristic, chunked
     * into 20-byte writes to fit the default GATT MTU.
     *
     * Arduino's onDataReceived currently just logs incoming JSON — this
     * is a forward-compatible path for future commands.
     *
     * @return true if all chunks were queued, false otherwise.
     */
    fun sendJson(json: String): Boolean {
        val rx = rxCharacteristic ?: return false
        val g = gatt ?: return false

        val bytes = json.toByteArray(Charsets.UTF_8)
        val chunkSize = 20

        var offset = 0
        while (offset < bytes.size) {
            val len = minOf(chunkSize, bytes.size - offset)
            val chunk = bytes.copyOfRange(offset, offset + len)

            val ok = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                g.writeCharacteristic(
                    rx, chunk, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                ) == BluetoothGatt.GATT_SUCCESS
            } else {
                @Suppress("DEPRECATION")
                rx.value = chunk
                @Suppress("DEPRECATION")
                g.writeCharacteristic(rx)
            }

            if (!ok) {
                Log.w(TAG, "writeCharacteristic returned false at offset $offset")
                return false
            }
            offset += len
            // Spacing between chunks — same approach as the Arduino's TX loop
            try { Thread.sleep(15) } catch (_: InterruptedException) {}
        }
        return true
    }

    // ─ GATT callback — connection + service discovery + notifications ─────
    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "GATT connected — discovering services")
                    _state.update { it.copy(connectionState = BleConnectionState.CONNECTED) }
                    g.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i(TAG, "GATT disconnected (status=$status)")
                    g.close()
                    gatt = null
                    rxCharacteristic = null
                    assembler.reset()
                    _state.update {
                        it.copy(
                            connectionState = BleConnectionState.IDLE,
                            connectedDevice = null
                        )
                    }
                }
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "Service discovery failed: $status")
                _state.update {
                    it.copy(
                        connectionState = BleConnectionState.ERROR,
                        errorMessage = "Service discovery failed (status $status)"
                    )
                }
                return
            }

            val service = g.getService(BleConstants.SERVICE_DEVICE_INFO)
            if (service == null) {
                _state.update {
                    it.copy(
                        connectionState = BleConnectionState.ERROR,
                        errorMessage = "Device Information service (180A) not found"
                    )
                }
                return
            }

            val tx = service.getCharacteristic(BleConstants.CHAR_DATA_TX)
            rxCharacteristic = service.getCharacteristic(BleConstants.CHAR_DATA_RX)
            if (tx == null) {
                _state.update {
                    it.copy(
                        connectionState = BleConnectionState.ERROR,
                        errorMessage = "TX characteristic (2A37) not found"
                    )
                }
                return
            }

            // Enable notifications on the TX characteristic so the
            // Arduino's chunked JSON arrives via onCharacteristicChanged.
            if (!g.setCharacteristicNotification(tx, true)) {
                _state.update {
                    it.copy(
                        connectionState = BleConnectionState.ERROR,
                        errorMessage = "Failed to enable TX notifications"
                    )
                }
                return
            }

            val descriptor = tx.getDescriptor(BleConstants.CCC_DESCRIPTOR)
            if (descriptor == null) {
                _state.update {
                    it.copy(
                        connectionState = BleConnectionState.ERROR,
                        errorMessage = "TX has no CCCD descriptor"
                    )
                }
                return
            }

            // Write the CCCD value to actually subscribe to notifications.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                g.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            } else {
                @Suppress("DEPRECATION")
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                g.writeDescriptor(descriptor)
            }
        }

        override fun onDescriptorWrite(
            g: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (descriptor.uuid == BleConstants.CCC_DESCRIPTOR) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.i(TAG, "TX notifications enabled — ready for data")
                } else {
                    Log.w(TAG, "CCCD write failed: $status")
                }
            }
        }

        // Pre-Android 13 notification callback
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            handleNotification(characteristic.uuid, characteristic.value ?: return)
        }

        // Android 13+ notification callback
        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handleNotification(characteristic.uuid, value)
        }

        private fun handleNotification(uuid: UUID, value: ByteArray) {
            if (uuid == BleConstants.CHAR_DATA_TX) {
                assembler.feed(value)
            }
        }
    }

    fun release() {
        stopScan()
        runCatching { gatt?.close() }
        gatt = null
        rxCharacteristic = null
        assembler.reset()
        scope.cancel()
    }

    companion object {
        private const val TAG = "BleManager"
    }
}
package com.example.urisis_android.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    private var scanJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Drop any device not seen in this window while scanning
    private val staleThresholdMs = 10_000L

    // ─ Scan callback — fires every time a BLE advertisement is received ────
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val entry = BleDevice(
                address = device.address,
                name = result.scanRecord?.deviceName ?: runCatching { device.name }.getOrNull(),
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
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
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
        _state.update {
            it.copy(
                connectionState = BleConnectionState.CONNECTING,
                connectedDevice = device,
                errorMessage = null
            )
        }
        gatt = remote.connectGatt(context, false, gattCallback)
    }

    fun disconnect() {
        _state.update { it.copy(connectionState = BleConnectionState.DISCONNECTING) }
        gatt?.disconnect()
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _state.update { it.copy(connectionState = BleConnectionState.CONNECTED) }
                    g.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    g.close()
                    gatt = null
                    _state.update {
                        it.copy(
                            connectionState = BleConnectionState.IDLE,
                            connectedDevice = null
                        )
                    }
                }
            }
        }
    }

    fun release() {
        stopScan()
        runCatching { gatt?.close() }
        gatt = null
        scope.cancel()
    }
}
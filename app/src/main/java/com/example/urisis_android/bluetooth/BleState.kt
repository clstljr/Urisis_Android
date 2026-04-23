package com.example.urisis_android.bluetooth

data class BleState(
    val connectionState: BleConnectionState = BleConnectionState.IDLE,
    val discoveredDevices: List<BleDevice> = emptyList(),
    val connectedDevice: BleDevice? = null,
    val errorMessage: String? = null,
    val isBluetoothEnabled: Boolean = false
)
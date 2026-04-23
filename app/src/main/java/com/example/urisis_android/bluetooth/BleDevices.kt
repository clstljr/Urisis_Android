package com.example.urisis_android.bluetooth

data class BleDevice(
    val address: String,                              // MAC — stable key for list
    val name: String?,                                // may be null for unnamed beacons
    val rssi: Int,                                    // signal strength, dBm
    val lastSeen: Long = System.currentTimeMillis()
)
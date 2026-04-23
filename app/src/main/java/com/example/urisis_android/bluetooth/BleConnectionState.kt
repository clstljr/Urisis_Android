package com.example.urisis_android.bluetooth

enum class BleConnectionState {
    IDLE,
    SCANNING,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    ERROR
}
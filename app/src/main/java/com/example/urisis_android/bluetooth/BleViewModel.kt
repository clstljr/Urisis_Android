package com.example.urisis_android.bluetooth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.StateFlow

class BleViewModel(application: Application) : AndroidViewModel(application) {

    private val manager = BleManager(application.applicationContext)

    val bleState: StateFlow<BleState> = manager.state

    fun startScan()                = manager.startScan()
    fun stopScan()                 = manager.stopScan()
    fun connect(device: BleDevice) = manager.connect(device)
    fun disconnect()               = manager.disconnect()
    fun hasPermissions()           = manager.hasPermissions()

    override fun onCleared() {
        super.onCleared()
        manager.release()
    }
}
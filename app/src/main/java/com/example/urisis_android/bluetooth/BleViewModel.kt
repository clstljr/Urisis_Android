package com.example.urisis_android.bluetooth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

class BleViewModel(application: Application) : AndroidViewModel(application) {

    private val manager = BleManager(application.applicationContext)

    val bleState: StateFlow<BleState> = manager.state

    /** Stream of complete JSON documents reassembled from the Arduino. */
    val incoming: SharedFlow<String> = manager.incoming

    fun startScan()                = manager.startScan()
    fun stopScan()                 = manager.stopScan()
    fun connect(device: BleDevice) = manager.connect(device)
    fun disconnect()               = manager.disconnect()
    fun hasPermissions()           = manager.hasPermissions()

    /** Phone → Arduino write to the RX characteristic. */
    fun sendJson(json: String): Boolean = manager.sendJson(json)

    override fun onCleared() {
        super.onCleared()
        manager.release()
    }
}
package com.example.urisis_android.bluetooth

import java.util.UUID

/**
 * BLE UUIDs the Arduino exposes via ArduinoBLE.
 *
 * The Arduino sketch (ArduinoUrinalysis/Bluetooth.h) uses 16-bit short
 * UUIDs (e.g. "180A"). Android filters/services on full 128-bit UUIDs,
 * which expand using the Bluetooth Base UUID:
 *      0000XXXX-0000-1000-8000-00805F9B34FB
 */
object BleConstants {

    /** Default device name advertised by the Arduino (BLE_DEFAULT_NAME). */
    const val DEFAULT_DEVICE_NAME = "Arduino-Urino"

    /** Device Information service. */
    val SERVICE_DEVICE_INFO: UUID =
        UUID.fromString("0000180A-0000-1000-8000-00805F9B34FB")

    /** Manufacturer Name (Read). */
    val CHAR_MANUFACTURER: UUID =
        UUID.fromString("00002A29-0000-1000-8000-00805F9B34FB")

    /** Model Number (Read). */
    val CHAR_MODEL: UUID =
        UUID.fromString("00002A24-0000-1000-8000-00805F9B34FB")

    /**
     * Data TX (Read | Notify) — Arduino → Phone.
     * The Arduino chunks each JSON document into 20-byte notifications
     * (see Bluetooth.cpp::sendJsonData), so the phone reassembles via
     * [JsonChunkAssembler].
     */
    val CHAR_DATA_TX: UUID =
        UUID.fromString("00002A37-0000-1000-8000-00805F9B34FB")

    /** Data RX (Write) — Phone → Arduino. */
    val CHAR_DATA_RX: UUID =
        UUID.fromString("00002A38-0000-1000-8000-00805F9B34FB")

    /** Standard Client Characteristic Configuration Descriptor. */
    val CCC_DESCRIPTOR: UUID =
        UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")
}
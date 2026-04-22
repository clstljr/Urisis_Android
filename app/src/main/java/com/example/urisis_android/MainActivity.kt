package com.example.urisis_android

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.urisis_android.bluetooth.BleViewModel
import com.example.urisis_android.screens.ConnectDeviceScreen
import com.example.urisis_android.screens.LoginScreen
import com.example.urisis_android.screens.MainDashboardScreen
import com.example.urisis_android.screens.MedicalDisclaimerScreen
import com.example.urisis_android.screens.RegisterScreen
import com.example.urisis_android.screens.SplashScreen

class MainActivity : ComponentActivity() {

    // Requests BLE permissions on launch — result is handled reactively
    // by BleManager.hasRequiredPermissions() before every scan/connect call
    private val blePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* no-op: BleManager checks permissions before each operation */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestBlePermissions()

        setContent {

            // Single BleViewModel — shared by dashboard and connect screen
            // so connection state is visible on the dashboard immediately
            val bleViewModel: BleViewModel = viewModel()

            var screen by remember { mutableStateOf("splash") }
            var loggedInUser by remember { mutableStateOf("KeMeNoToRiKo") }

            when (screen) {

                // ───────────────────── Splash
                "splash" -> SplashScreen(
                    onFinished = { screen = "disclaimer" }
                )

                // ───────────────────── Disclaimer
                "disclaimer" -> MedicalDisclaimerScreen(
                    onAgree = { screen = "login" }
                )

                // ───────────────────── Login
                "login" -> LoginScreen(
                    onLoginClick    = { screen = "dashboard" },
                    onRegisterClick = { screen = "register" }
                )

                // ───────────────────── Register
                "register" -> RegisterScreen(
                    onBackClick          = { screen = "login" },
                    onCreateAccountClick = { screen = "login" },
                    onLoginClick         = { screen = "login" }
                )

                // ───────────────────── Dashboard
                "dashboard" -> MainDashboardScreen(
                    userName          = loggedInUser,
                    bleViewModel      = bleViewModel,
                    onConnectClick    = { screen = "connect" },
                    onStartUrinalysisClick = { /* TODO */ },
                    onHistoryClick    = { /* TODO */ },
                    onProfileClick    = { /* TODO */ },
                    onLogoutClick     = {
                        bleViewModel.disconnect()
                        screen = "login"
                    }
                )

                // ───────────────────── Connect Device
                "connect" -> ConnectDeviceScreen(
                    bleViewModel = bleViewModel,
                    onBackClick = { screen = "dashboard" },
                    onDeviceConnected = { screen = "dashboard" }
                )

            }
        }
    }

    // ── Request correct permissions per Android version ────────────────────

    private fun requestBlePermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ (API 31+) — new granular BLE permissions
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            // Android 11 and below — classic BT + location for scan discovery
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
        blePermissionLauncher.launch(permissions)
    }
}
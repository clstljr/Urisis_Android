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
import com.example.urisis_android.screens.AnalyzingSampleScreen
import com.example.urisis_android.screens.ConnectDeviceScreen
import com.example.urisis_android.screens.LoginScreen
import com.example.urisis_android.screens.MainDashboardScreen
import com.example.urisis_android.screens.MedicalDisclaimerScreen
import com.example.urisis_android.screens.PreTestProtocolScreen
import com.example.urisis_android.screens.RegisterScreen
import com.example.urisis_android.screens.SplashScreen
import com.example.urisis_android.screens.TestResultsScreen
import com.example.urisis_android.screens.WaitingForDeviceScreen
import com.example.urisis_android.urinalysis.UrinalysisViewModel
import com.example.urisis_android.urinalysis.UrinalysisViewModelFactory

class MainActivity : ComponentActivity() {

    // Requests BLE permissions on launch — result is handled reactively
    // by BleManager.hasPermissions() before every scan/connect call
    private val blePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* no-op: BleManager checks permissions before each operation */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestBlePermissions()

        setContent {

            // Shared across dashboard, connect, and urinalysis flow
            val bleViewModel: BleViewModel = viewModel()
            val urinalysisViewModel: UrinalysisViewModel = viewModel(
                factory = UrinalysisViewModelFactory(bleViewModel)
            )

            var screen by remember { mutableStateOf("splash") }
            val loggedInUser by remember { mutableStateOf("KeMeNoToRiKo") }

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
                    userName               = loggedInUser,
                    bleViewModel           = bleViewModel,
                    onConnectClick         = { screen = "connect" },
                    onStartUrinalysisClick = {
                        urinalysisViewModel.reset()
                        screen = "protocol"
                    },
                    onHistoryClick = { /* TODO */ },
                    onProfileClick = { /* TODO */ },
                    onLogoutClick  = {
                        bleViewModel.disconnect()
                        screen = "login"
                    }
                )

                // ───────────────────── Connect Device
                "connect" -> ConnectDeviceScreen(
                    bleViewModel      = bleViewModel,
                    onBackClick       = { screen = "dashboard" },
                    onDeviceConnected = { screen = "dashboard" }
                )

                // ───────────────────── Urinalysis flow ────────────────────
                "protocol" -> PreTestProtocolScreen(
                    onBackClick = { screen = "dashboard" },
                    onContinueClick = {
                        // set demo = false once Arduino firmware is wired
                        urinalysisViewModel.beginSession(demo = true)
                        screen = "waiting"
                    }
                )

                "waiting" -> WaitingForDeviceScreen(
                    viewModel = urinalysisViewModel,
                    onAdvance = { screen = "analyzing" },
                    onBack    = {
                        urinalysisViewModel.reset()
                        screen = "dashboard"
                    }
                )

                "analyzing" -> AnalyzingSampleScreen(
                    viewModel  = urinalysisViewModel,
                    onComplete = { screen = "results" }
                )

                "results" -> TestResultsScreen(
                    viewModel = urinalysisViewModel,
                    onBackClick = {
                        urinalysisViewModel.reset()
                        screen = "dashboard"
                    },
                    onHomeClick = {
                        urinalysisViewModel.reset()
                        screen = "dashboard"
                    },
                    onViewHistoryClick = { /* TODO: screen = "history" */ }
                )
            }
        }
    }

    // ── Request correct permissions per Android version ────────────────────

    private fun requestBlePermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ (API 31+) — granular BLE permissions
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
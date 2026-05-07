package com.example.urisis_android

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.urisis_android.auth.AuthViewModel
import com.example.urisis_android.bluetooth.BleViewModel
import com.example.urisis_android.screens.AnalyzingSampleScreen
import com.example.urisis_android.screens.ConnectDeviceScreen
import com.example.urisis_android.screens.LoginScreen
import com.example.urisis_android.screens.MainDashboardScreen
import com.example.urisis_android.screens.MedicalDisclaimerScreen
import com.example.urisis_android.screens.PreTestProtocolScreen
import com.example.urisis_android.screens.RegisterScreen
import com.example.urisis_android.screens.SplashScreen
import com.example.urisis_android.screens.TestHistoryScreen
import com.example.urisis_android.screens.TestResultsScreen
import com.example.urisis_android.screens.WaitingForDeviceScreen

import com.example.urisis_android.screens.AdvancedResultsScreen
import com.example.urisis_android.screens.AdvancedSensorDataScreen

import com.example.urisis_android.urinalysis.HistoryViewModel
import com.example.urisis_android.urinalysis.HistoryViewModelFactory
import com.example.urisis_android.urinalysis.UrinalysisViewModel
import com.example.urisis_android.urinalysis.UrinalysisViewModelFactory

class MainActivity : ComponentActivity() {

    private val blePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestBlePermissions()

        setContent {
            val bleViewModel: BleViewModel = viewModel()
            val authViewModel: AuthViewModel = viewModel()
            val urinalysisViewModel: UrinalysisViewModel = viewModel(
                factory = UrinalysisViewModelFactory(application, bleViewModel)
            )
            val historyViewModel: HistoryViewModel = viewModel(
                factory = HistoryViewModelFactory(application)
            )

            val authState by authViewModel.ui.collectAsState()
            val currentUser = authState.currentUser

            // Keep both VMs aware of the active user so persistence and
            // history reads are always scoped to the right account.
            LaunchedEffect(currentUser?.email) {
                urinalysisViewModel.setActiveUser(currentUser?.email)
                historyViewModel.setUser(currentUser?.email)
            }

            var screen by remember { mutableStateOf("splash") }
            // Carries the demo-mode choice from the dashboard through the
            // protocol screen into beginSession(). Reset on every fresh
            // navigation so a stale value can't survive a session.
            var pendingDemoMode by remember { mutableStateOf(false) }

            when (screen) {

                "splash" -> SplashScreen(
                    onFinished = {
                        screen = if (currentUser != null) "dashboard" else "disclaimer"
                    }
                )

                "disclaimer" -> MedicalDisclaimerScreen(
                    onAgree = { screen = "login" }
                )

                "login" -> LoginScreen(
                    authViewModel = authViewModel,
                    onLoginSuccess = { screen = "dashboard" },
                    onRegisterClick = { screen = "register" }
                )

                "register" -> RegisterScreen(
                    authViewModel = authViewModel,
                    onBackClick = { screen = "login" },
                    onRegisterSuccess = { screen = "dashboard" },
                    onLoginClick = { screen = "login" }
                )

                "dashboard" -> {
                    if (currentUser == null) {
                        screen = "login"
                    } else {
                        MainDashboardScreen(
                            userName = currentUser.fullName,
                            bleViewModel = bleViewModel,
                            onConnectClick = { screen = "connect" },
                            onStartUrinalysisClick = { demo ->
                                urinalysisViewModel.reset()
                                // Stash demo intent so the protocol screen can
                                // route correctly when the user taps Continue.
                                pendingDemoMode = demo
                                screen = "protocol"
                            },
                            onHistoryClick = { screen = "history" },
                            onProfileClick = { /* TODO */ },
                            onLogoutClick = {
                                bleViewModel.disconnect()
                                authViewModel.logout()
                                screen = "login"
                            }
                        )
                    }
                }

                "connect" -> ConnectDeviceScreen(
                    bleViewModel = bleViewModel,
                    onBackClick = { screen = "dashboard" },
                    onDeviceConnected = { screen = "dashboard" }
                )

                "protocol" -> PreTestProtocolScreen(
                    onBackClick = { screen = "dashboard" },
                    onContinueClick = {
                        // demo flag comes from the dashboard toggle. When
                        // true the ViewModel synthesizes data without
                        // entering BLE listening mode; when false it
                        // subscribes to incoming JSON from the Arduino.
                        urinalysisViewModel.beginSession(demo = pendingDemoMode)
                        screen = "waiting"
                    }
                )

                "waiting" -> WaitingForDeviceScreen(
                    viewModel = urinalysisViewModel,
                    onAdvance = { screen = "analyzing" },
                    onBack = {
                        urinalysisViewModel.reset()
                        screen = "dashboard"
                    }
                )

                "analyzing" -> AnalyzingSampleScreen(
                    viewModel = urinalysisViewModel,
                    onComplete = { screen = "results" }
                )

                // ✅ UPDATED RESULTS SCREEN
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
                    onViewHistoryClick = { screen = "history" },

                    // 👉 MAKE SURE THIS EXISTS IN YOUR SCREEN
                    onAdvancedClick = { screen = "advanced_gate" }
                )

                // ✅ NEW SCREEN 1
                "advanced_gate" -> AdvancedResultsScreen(
                    onUnderstand = { screen = "advanced_data" },
                    onGoBack = { screen = "results" }
                )

                // ✅ NEW SCREEN 2
                "advanced_data" -> AdvancedSensorDataScreen(
                    viewModel = urinalysisViewModel,
                    onBackClick = { screen = "advanced_gate" }
                )

                // History — modern view with trend charts + all-tests list
                "history" -> TestHistoryScreen(
                    viewModel = historyViewModel,
                    onBack = { screen = "dashboard" }
                )
            }
        }
    }

    private fun requestBlePermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
        blePermissionLauncher.launch(permissions)
    }
}
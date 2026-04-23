package com.example.urisis_android

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.urisis_android.screens.TestResultsScreen
import com.example.urisis_android.screens.WaitingForDeviceScreen
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
                factory = UrinalysisViewModelFactory(bleViewModel)
            )

            val authState by authViewModel.ui.collectAsState()
            val currentUser = authState.currentUser

            var screen by remember { mutableStateOf("splash") }

            when (screen) {

                "splash" -> SplashScreen(
                    onFinished = {
                        // If a session was restored while splash was up, skip login
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
                    // Defensive: if session was cleared somewhere, bounce to loginPage
                    if (currentUser == null) {
                        screen = "login"
                    } else {
                        MainDashboardScreen(
                            userName = currentUser.fullName,
                            bleViewModel = bleViewModel,
                            onConnectClick = { screen = "connect" },
                            onStartUrinalysisClick = {
                                urinalysisViewModel.reset()
                                screen = "protocol"
                            },
                            onHistoryClick = { /* TODO */ },
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
                        urinalysisViewModel.beginSession(demo = true)
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
                    onViewHistoryClick = { /* TODO */ }
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
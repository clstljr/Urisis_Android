package com.example.urisis_android

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel

import com.example.urisis_android.auth.AuthViewModel
import com.example.urisis_android.bluetooth.BleViewModel

import com.example.urisis_android.screens.AccountPickerScreen
import com.example.urisis_android.screens.AccountSwitcherSheet
import com.example.urisis_android.screens.AdvancedResultsScreen
import com.example.urisis_android.screens.AdvancedSensorDataScreen
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

import com.example.urisis_android.urinalysis.HistoryViewModel
import com.example.urisis_android.urinalysis.HistoryViewModelFactory
import com.example.urisis_android.urinalysis.UrinalysisViewModel
import com.example.urisis_android.urinalysis.UrinalysisViewModelFactory

class MainActivity : ComponentActivity() {

    private val blePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestBlePermissions()

        setContent {
            com.example.urisis_android.ui.theme.UrisisTheme {
                AppContent()
            }
        }
    }

    @Composable
    private fun AppContent() {

        val bleViewModel: BleViewModel = viewModel()

        val authViewModel: AuthViewModel = viewModel()

        val urinalysisViewModel: UrinalysisViewModel = viewModel(
            factory = UrinalysisViewModelFactory(
                application,
                bleViewModel
            )
        )

        val historyViewModel: HistoryViewModel = viewModel(
            factory = HistoryViewModelFactory(application)
        )

        val authState by authViewModel.ui.collectAsState()

        val currentUser = authState.currentUser

        val storedAccounts by authViewModel
            .storedAccounts
            .collectAsState()

        // Sync active user
        LaunchedEffect(currentUser?.email) {

            urinalysisViewModel.setActiveUser(
                currentUser?.email
            )

            historyViewModel.setUser(
                currentUser?.email
            )
        }

        var screen by remember {
            mutableStateOf("splash")
        }

        var resultExit by remember {
            mutableStateOf("dashboard")
        }

        var showAccountSwitcher by remember {
            mutableStateOf(false)
        }

        when (screen) {

            "splash" -> {

                SplashScreen(
                    isAuthInitialized = authState.isInitialized,

                    onFinished = {

                        screen = when {

                            currentUser != null -> {
                                "dashboard"
                            }

                            storedAccounts.isNotEmpty() -> {
                                "picker"
                            }

                            else -> {
                                "disclaimer"
                            }
                        }
                    }
                )
            }

            "picker" -> {

                AccountPickerScreen(
                    authViewModel = authViewModel,

                    onAccountChosen = {
                        screen = "dashboard"
                    },

                    onAddAccount = {
                        screen = "login"
                    },

                    onBack =
                        if (currentUser != null) {
                            { screen = "dashboard" }
                        } else {
                            null
                        }
                )
            }

            "disclaimer" -> {

                MedicalDisclaimerScreen(
                    onAgree = {
                        screen = "login"
                    }
                )
            }

            "login" -> {

                LoginScreen(
                    authViewModel = authViewModel,

                    onLoginSuccess = {
                        screen = "dashboard"
                    },

                    onRegisterClick = {
                        screen = "register"
                    }
                )
            }

            "register" -> {

                RegisterScreen(
                    authViewModel = authViewModel,

                    onBackClick = {
                        screen = "login"
                    },

                    onRegisterSuccess = {
                        screen = "dashboard"
                    },

                    onLoginClick = {
                        screen = "login"
                    }
                )
            }

            "dashboard" -> {

                if (currentUser == null) {

                    screen =
                        if (storedAccounts.isNotEmpty()) {
                            "picker"
                        } else {
                            "login"
                        }

                } else {

                    MainDashboardScreen(
                        userName = currentUser.fullName,

                        userEmail = currentUser.email,

                        bleViewModel = bleViewModel,

                        onConnectClick = {
                            screen = "connect"
                        },

                        onStartUrinalysisClick = {

                            urinalysisViewModel.reset()

                            screen = "protocol"
                        },

                        onHistoryClick = {
                            screen = "history"
                        },

                        onAccountClick = {
                            showAccountSwitcher = true
                        },

                        onLogoutClick = {

                            bleViewModel.disconnect()

                            authViewModel.logout()

                            screen =
                                if (storedAccounts.size >= 2) {
                                    "picker"
                                } else {
                                    "login"
                                }
                        }
                    )

                    if (showAccountSwitcher) {

                        AccountSwitcherSheet(
                            authViewModel = authViewModel,

                            onDismiss = {
                                showAccountSwitcher = false
                            },

                            onSwitched = {
                                showAccountSwitcher = false
                            },

                            onAddAccount = {

                                showAccountSwitcher = false

                                screen = "login"
                            },

                            onSignOut = {

                                showAccountSwitcher = false

                                bleViewModel.disconnect()

                                authViewModel.logout()

                                screen =
                                    if (storedAccounts.size >= 2) {
                                        "picker"
                                    } else {
                                        "login"
                                    }
                            }
                        )
                    }
                }
            }

            "connect" -> {

                ConnectDeviceScreen(
                    bleViewModel = bleViewModel,

                    onBackClick = {
                        screen = "dashboard"
                    },

                    onDeviceConnected = {
                        screen = "dashboard"
                    }
                )
            }

            "protocol" -> {

                PreTestProtocolScreen(
                    onBackClick = {
                        screen = "dashboard"
                    },

                    onContinueClick = {

                        urinalysisViewModel.beginSession()

                        screen = "waiting"
                    }
                )
            }

            "waiting" -> {

                WaitingForDeviceScreen(
                    viewModel = urinalysisViewModel,

                    onAdvance = {
                        screen = "analyzing"
                    },

                    onBack = {

                        urinalysisViewModel.reset()

                        screen = "dashboard"
                    }
                )
            }

            "analyzing" -> {

                AnalyzingSampleScreen(
                    viewModel = urinalysisViewModel,

                    onComplete = {

                        resultExit = "dashboard"

                        screen = "results"
                    }
                )
            }

            "results" -> {

                TestResultsScreen(
                    viewModel = urinalysisViewModel,

                    onBackClick = {

                        urinalysisViewModel.reset()

                        screen = resultExit
                    },

                    onHomeClick = {

                        urinalysisViewModel.reset()

                        screen = "dashboard"
                    },

                    onViewHistoryClick = {
                        screen = "history"
                    },

                    onAdvancedClick = {
                        screen = "advanced_gate"
                    }
                )
            }

            "advanced_gate" -> {

                AdvancedResultsScreen(
                    onUnderstand = {
                        screen = "advanced_data"
                    },

                    onGoBack = {
                        screen = "results"
                    }
                )
            }

            "advanced_data" -> {

                AdvancedSensorDataScreen(
                    viewModel = urinalysisViewModel,

                    onBackClick = {
                        screen = "advanced_gate"
                    }
                )
            }

            "history" -> {

                TestHistoryScreen(
                    viewModel = historyViewModel,

                    onBack = {
                        screen = "dashboard"
                    },

                    onItemClick = { record ->

                        urinalysisViewModel.loadFromHistory(record)

                        resultExit = "history"

                        screen = "results"
                    }
                )
            }
        }
    }

    private fun requestBlePermissions() {

        val permissions =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

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
package com.example.urisis_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.urisis_android.screens.LoginScreen
import com.example.urisis_android.screens.MedicalDisclaimerScreen
import com.example.urisis_android.screens.RegisterScreen
import com.example.urisis_android.screens.SplashScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            var screen by remember { mutableStateOf("splash") }

            when (screen) {

                // ───────────────────────── Splash Screen
                "splash" -> {
                    SplashScreen(
                        onFinished = {
                            screen = "disclaimer"
                        }
                    )
                }

                // ───────────────────────── Disclaimer Screen
                "disclaimer" -> {
                    MedicalDisclaimerScreen(
                        onAgree = {
                            screen = "login"
                        }
                    )
                }

                // ───────────────────────── Login Screen (NEW)
                "login" -> {
                    LoginScreen(
                        onLoginClick = {
                            // TODO: go to home/dashboard screen later
                            // screen = "home"
                        },
                        onRegisterClick = {
                            screen = "register"
                        }
                    )
                }

                // ───────────────────────── Register Screen (NEW)
                "register" -> {
                    RegisterScreen(
                        onBackClick = {
                            screen = "login"
                        },
                        onCreateAccountClick = {
                            // after register, go back to log in
                            screen = "login"
                        },
                        onLoginClick = {
                            screen = "login"
                        }
                    )
                }
            }
        }
    }
}
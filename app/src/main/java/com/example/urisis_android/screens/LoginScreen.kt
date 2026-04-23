package com.example.urisis_android.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.urisis_android.auth.AuthResult
import com.example.urisis_android.auth.AuthViewModel

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val state by authViewModel.ui.collectAsState()
    val status = state.status

    // React to auth result
    LaunchedEffect(status) {
        if (status is AuthResult.Success) {
            authViewModel.consumeResult()
            onLoginSuccess()
        }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF1565C0), Color(0xFF00ACC1))
                        )
                    )
                    .padding(vertical = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Smart Urinalysis",
                        color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Hydration Monitoring System",
                        color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Your data stays on this device. No account information is sent online.",
                    color = Color(0xFF9E9E9E),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        },
        containerColor = Color.White
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(32.dp))

            Text("Login to Continue",
                fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121))

            Spacer(Modifier.height(28.dp))

            FieldLabel("Email")
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter your email", color = Color(0xFF9E9E9E)) },
                leadingIcon = { Text("✉", fontSize = 18.sp,
                    color = Color(0xFF9E9E9E),
                    modifier = Modifier.padding(start = 4.dp)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(10.dp),
                colors = filledFieldColors(),
                singleLine = true,
                enabled = status !is AuthResult.Loading
            )

            Spacer(Modifier.height(18.dp))

            FieldLabel("Password")
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter your password", color = Color(0xFF9E9E9E)) },
                leadingIcon = { Text("🔒", fontSize = 16.sp,
                    modifier = Modifier.padding(start = 4.dp)) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Text(if (passwordVisible) "🙈" else "👁", fontSize = 16.sp)
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(10.dp),
                colors = filledFieldColors(),
                singleLine = true,
                enabled = status !is AuthResult.Loading
            )

            if (status is AuthResult.Error) {
                Spacer(Modifier.height(10.dp))
                Text(status.message, color = Color(0xFFC62828), fontSize = 13.sp)
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { authViewModel.login(email, password) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                enabled = status !is AuthResult.Loading
            ) {
                if (status is AuthResult.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp, color = Color.White
                    )
                } else {
                    Text("Login", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onRegisterClick,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, Color(0xFF1565C0)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1565C0)),
                enabled = status !is AuthResult.Loading
            ) {
                Text("Register", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF212121))
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun filledFieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor = Color(0xFFF0F4F8),
    focusedContainerColor = Color(0xFFF0F4F8),
    unfocusedBorderColor = Color.Transparent,
    focusedBorderColor = Color(0xFF1565C0)
)
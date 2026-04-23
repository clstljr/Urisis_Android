package com.example.urisis_android.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.urisis_android.auth.AuthResult
import com.example.urisis_android.auth.AuthViewModel

@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel,
    onBackClick: () -> Unit,
    onRegisterSuccess: () -> Unit,
    onLoginClick: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val state by authViewModel.ui.collectAsState()
    val status = state.status
    val isBusy = status is AuthResult.Loading

    LaunchedEffect(status) {
        if (status is AuthResult.Success) {
            authViewModel.consumeResult()
            onRegisterSuccess()
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
                    .padding(top = 12.dp, bottom = 24.dp, start = 4.dp, end = 16.dp)
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.align(Alignment.TopStart)
                ) { Text("←", fontSize = 22.sp, color = Color.White) }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, top = 48.dp)
                ) {
                    Text("Create Account", color = Color.White,
                        fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text("Join the Smart Urinalysis system",
                        color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                }
            }
        },
        containerColor = Color.White
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(28.dp))

            RegisterField(
                label = "Full Name",
                value = fullName,
                onValue = { fullName = it },
                placeholder = "Enter your full name",
                icon = "👤",
                enabled = !isBusy
            )
            Spacer(Modifier.height(18.dp))

            RegisterField(
                label = "Email",
                value = email,
                onValue = { email = it },
                placeholder = "Enter your email",
                icon = "✉",
                keyboardType = KeyboardType.Email,
                enabled = !isBusy
            )
            Spacer(Modifier.height(18.dp))

            PasswordField(
                label = "Password",
                value = password,
                onValue = { password = it },
                placeholder = "Create a password",
                visible = passwordVisible,
                onToggleVisibility = { passwordVisible = !passwordVisible },
                enabled = !isBusy
            )
            Spacer(Modifier.height(18.dp))

            PasswordField(
                label = "Confirm Password",
                value = confirmPassword,
                onValue = { confirmPassword = it },
                placeholder = "Confirm your password",
                visible = confirmPasswordVisible,
                onToggleVisibility = { confirmPasswordVisible = !confirmPasswordVisible },
                enabled = !isBusy
            )

            if (status is AuthResult.Error) {
                Spacer(Modifier.height(12.dp))
                Text(status.message, color = Color(0xFFC62828), fontSize = 13.sp)
            }

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = {
                    authViewModel.register(fullName, email, password, confirmPassword)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                enabled = !isBusy
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp, color = Color.White
                    )
                } else {
                    Text("Create Account", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(16.dp))

            TextButton(
                onClick = onLoginClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isBusy
            ) {
                Text(
                    text = buildAnnotatedString {
                        append("Already have an account? ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Login") }
                    },
                    color = Color(0xFF1565C0),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────
@Composable
private fun RegisterField(
    label: String,
    value: String,
    onValue: (String) -> Unit,
    placeholder: String,
    icon: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean
) {
    Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF212121))
    Spacer(Modifier.height(6.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = Color(0xFF9E9E9E)) },
        leadingIcon = { Text(icon, fontSize = 16.sp, modifier = Modifier.padding(start = 4.dp)) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color(0xFFE0E0E0),
            focusedBorderColor = Color(0xFF1565C0)
        ),
        singleLine = true,
        enabled = enabled
    )
}

@Composable
private fun PasswordField(
    label: String,
    value: String,
    onValue: (String) -> Unit,
    placeholder: String,
    visible: Boolean,
    onToggleVisibility: () -> Unit,
    enabled: Boolean
) {
    Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF212121))
    Spacer(Modifier.height(6.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = Color(0xFF9E9E9E)) },
        leadingIcon = { Text("🔒", fontSize = 16.sp, modifier = Modifier.padding(start = 4.dp)) },
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Text(if (visible) "🙈" else "👁", fontSize = 16.sp)
            }
        },
        visualTransformation = if (visible) VisualTransformation.None
        else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color(0xFFE0E0E0),
            focusedBorderColor = Color(0xFF1565C0)
        ),
        singleLine = true,
        enabled = enabled
    )
}
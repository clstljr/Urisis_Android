package com.example.urisis_android.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.urisis_android.ui.illustrations.WaterDropLogo
import com.example.urisis_android.ui.theme.brandBrush

@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel,
    onBackClick: () -> Unit = {},
    onRegisterSuccess: () -> Unit,
    onLoginClick: () -> Unit,
) {
    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val state by authViewModel.ui.collectAsState()
    val status = state.status
    val dark = isSystemInDarkTheme()

    LaunchedEffect(status) {
        if (status is AuthResult.Success) {
            authViewModel.consumeResult()
            onRegisterSuccess()
        }
    }

    val isLoading = status is AuthResult.Loading
    val errorMessage = (status as? AuthResult.Error)?.message
    val passwordsMatch = password == confirmPassword || confirmPassword.isEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(brandBrush(dark))
                    .padding(horizontal = 24.dp)
                    .padding(top = 52.dp, bottom = 50.dp),
            ) {
                // Back chevron — top-left
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f)),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        WaterDropLogo(logoSize = 56.dp, onColored = true)
                    }

                    Spacer(Modifier.height(14.dp))
                    Text("Create Account",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Start tracking your hydration today",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .offset(y = (-24).dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    elevation = CardDefaults.cardElevation(8.dp),
                ) {
                    Column(modifier = Modifier.padding(22.dp)) {
                        Text("Sign up",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text("Fill in your details to get started",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(Modifier.height(20.dp))

                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Full name") },
                            placeholder = { Text("Jane Doe") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                            ),
                        )

                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = username,
                            onValueChange = { input ->
                                // Force lowercase + strip whitespace as the
                                // user types so the on-screen value mirrors
                                // what's stored and what they'll type at
                                // login.
                                username = input
                                    .lowercase()
                                    .filter { !it.isWhitespace() }
                            },
                            label = { Text("Username") },
                            placeholder = { Text("jane_doe") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.AlternateEmail,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            supportingText = {
                                Text(
                                    "3-20 characters · letters, numbers, . or _",
                                    fontSize = 11.sp,
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                            ),
                        )

                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            placeholder = { Text("you@example.com") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email
                            ),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Email,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                            ),
                        )

                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password
                            ),
                            visualTransformation = if (passwordVisible)
                                VisualTransformation.None
                            else PasswordVisualTransformation(),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible)
                                            Icons.Filled.VisibilityOff
                                        else Icons.Filled.Visibility,
                                        contentDescription = if (passwordVisible)
                                            "Hide password" else "Show password",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                            ),
                        )

                        if (password.isNotEmpty()) {
                            PasswordStrengthRow(password = password)
                            PasswordRequirementsRow(password = password)
                        }

                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Confirm password") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password
                            ),
                            visualTransformation = if (passwordVisible)
                                VisualTransformation.None
                            else PasswordVisualTransformation(),
                            isError = !passwordsMatch && confirmPassword.isNotEmpty(),
                            supportingText = {
                                if (!passwordsMatch && confirmPassword.isNotEmpty()) {
                                    Text("Passwords don't match",
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 11.sp)
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                            ),
                        )

                        if (errorMessage != null) {
                            Spacer(Modifier.height(10.dp))
                            ErrorBanner(errorMessage)
                        }

                        Spacer(Modifier.height(20.dp))

                        Button(
                            onClick = {
                                authViewModel.register(
                                    fullName, username, email, password, confirmPassword
                                )
                            },
                            enabled = !isLoading
                                    && fullName.isNotBlank()
                                    && username.isNotBlank()
                                    && email.isNotBlank()
                                    && isPasswordStrong(password)
                                    && passwordsMatch,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor   = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(22.dp),
                                )
                            } else {
                                Text("Create account",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text("Already have an account?",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Sign in",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { onLoginClick() },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PasswordStrengthRow(password: String) {
    val strength = remember(password) { passwordStrength(password) }
    val (label, color, bars) = when (strength) {
        0 -> Triple("Too weak", MaterialTheme.colorScheme.error, 0)
        1 -> Triple("Weak",     MaterialTheme.colorScheme.error, 1)
        2 -> Triple("Fair",     Color(0xFFD97706), 2)
        3 -> Triple("Almost",   Color(0xFFCA8A04), 3)
        else -> Triple("Strong", Color(0xFF047857), 4)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(4) { i ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (i < bars) color
                        else MaterialTheme.colorScheme.outlineVariant
                    )
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(label,
            fontSize = 11.sp,
            color = color,
            fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Returns 0..4 reflecting how many requirements the password satisfies:
 *   1. 8+ characters
 *   2. At least one uppercase letter
 *   3. At least one digit
 *   4. At least one special character
 *
 * A score of 4 means all registration requirements are met. Anything
 * below that gates the Create-Account button.
 */
private fun passwordStrength(p: String): Int {
    var score = 0
    if (p.length >= 8)                          score++
    if (p.any { it.isUpperCase() })             score++
    if (p.any { it.isDigit() })                 score++
    if (p.any { !it.isLetterOrDigit() })        score++
    return score
}

private fun isPasswordStrong(p: String): Boolean = passwordStrength(p) == 4

@Composable
private fun PasswordRequirementsRow(password: String) {
    val checks = remember(password) {
        listOf(
            "8+ characters"      to (password.length >= 8),
            "Uppercase letter"   to password.any { it.isUpperCase() },
            "Number"             to password.any { it.isDigit() },
            "Special character"  to password.any { !it.isLetterOrDigit() },
        )
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        checks.forEach { (label, met) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 2.dp),
            ) {
                Icon(
                    imageVector = if (met) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = if (met) Color(0xFF047857) else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = if (met) Color(0xFF047857) else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (met) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}
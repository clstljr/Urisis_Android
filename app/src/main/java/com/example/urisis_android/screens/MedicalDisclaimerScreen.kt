package com.example.urisis_android.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MedicalDisclaimerScreen(onAgree: () -> Unit) {

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1565C0))
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Medical Disclaimer",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        bottomBar = {
            Button(
                onClick = onAgree,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1565C0)
                )
            ) {
                Text(
                    text = "Agree & Continue",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(24.dp))

            // Warning icon
            Text(text = "⚠️", fontSize = 48.sp)

            Spacer(modifier = Modifier.height(24.dp))

            // Blue card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE3F2FD)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = buildAnnotatedString {
                            append("This device is for ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("screening purposes only")
                            }
                            append(" and does not replace professional medical advice, diagnosis, or treatment.")
                        },
                        fontSize = 14.sp,
                        color = Color(0xFF1A237E)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Always consult with a qualified healthcare provider regarding any medical concerns or conditions.",
                        fontSize = 12.sp,
                        color = Color(0xFF5C6BC0),
                        textAlign = TextAlign.Start
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Yellow card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFFDE7)
                )
            ) {
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFFE65100))) {
                            append("Important: ")
                        }
                        append("This app is designed for preliminary screening and hydration monitoring. Abnormal results should be confirmed by laboratory testing.")
                    },
                    fontSize = 13.sp,
                    color = Color(0xFF5D4037)
                )
            }
        }
    }
}
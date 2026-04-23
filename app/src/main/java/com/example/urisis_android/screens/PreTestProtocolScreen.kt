package com.example.urisis_android.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class ProtocolStep(
    val number: Int,
    val title: String,
    val description: String,
    val icon: String,
    val iconBg: Color,
    val accent: Color
)

private val protocolSteps = listOf(
    ProtocolStep(1, "Clean Collection Cup",
        "Use a clean, dry collection cup for the sample.",
        "📋", Color(0xFFFDECEC), Color(0xFFC62828)),
    ProtocolStep(2, "Collect Midstream Sample",
        "Collect midstream urine (not the first or last portion).",
        "💧", Color(0xFFE8F5E9), Color(0xFF2E7D32)),
    ProtocolStep(3, "Place Sample in Device",
        "Carefully place the sample inside the device chamber.",
        "📱", Color(0xFFE3F2FD), Color(0xFF1565C0)),
    ProtocolStep(4, "Close Lid to Block Light",
        "Ensure the lid is fully closed to prevent external light interference.",
        "🔒", Color(0xFFFFF3E0), Color(0xFFEF6C00)),
    ProtocolStep(5, "Press Start on Device",
        "Press the physical start button on the Arduino device to begin analysis.",
        "▶", Color(0xFFF3E5F5), Color(0xFF7B1FA2))
)

@Composable
fun PreTestProtocolScreen(
    onBackClick: () -> Unit = {},
    onContinueClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F4F7))
            .verticalScroll(rememberScrollState())
    ) {
        // ── Header ──────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF1565C0), Color(0xFF29B6F6))
                    )
                )
                .padding(horizontal = 20.dp)
                .padding(top = 52.dp, bottom = 28.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { onBackClick() },
                    contentAlignment = Alignment.CenterStart
                ) { Text("←", fontSize = 22.sp, color = Color.White) }

                Spacer(Modifier.height(14.dp))

                Text(
                    "Pre-Test Protocol",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Follow these steps carefully",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp
                )
            }
        }

        // ── Steps ───────────────────────────────────────────────────────
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            protocolSteps.forEach { step -> ProtocolCard(step) }

            // Important warning card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("⚠", fontSize = 18.sp, color = Color(0xFFEF6C00))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Row {
                            Text("Important: ",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFFEF6C00))
                            Text(
                                "Make sure the device is on a stable, flat surface and the lid is completely closed before starting the test.",
                                fontSize = 13.sp,
                                color = Color(0xFF6D4C00),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Continue button
            Button(
                onClick = onContinueClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1565C0)
                )
            ) {
                Text("I understand — continue",
                    fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ProtocolCard(step: ProtocolStep) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(step.iconBg),
                contentAlignment = Alignment.Center
            ) { Text(step.icon, fontSize = 22.sp) }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        step.number.toString(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = step.accent
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        step.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    step.description,
                    fontSize = 13.sp,
                    color = Color(0xFF757575),
                    lineHeight = 18.sp
                )
            }
        }
    }
}
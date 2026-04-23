package com.example.urisis_android.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.urisis_android.urinalysis.HydrationStatus
import com.example.urisis_android.urinalysis.UrinalysisResult
import com.example.urisis_android.urinalysis.UrinalysisViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun TestResultsScreen(
    viewModel: UrinalysisViewModel,
    onBackClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onViewHistoryClick: () -> Unit = {}
) {
    val ui by viewModel.ui.collectAsState()
    val result = ui.result ?: return

    val palette = when (result.status) {
        HydrationStatus.WELL_HYDRATED    -> Palette(Color(0xFF2E7D32), Color(0xFFE8F5E9), "✓")
        HydrationStatus.NORMAL           -> Palette(Color(0xFF1565C0), Color(0xFFE3F2FD), "ℹ")
        HydrationStatus.MILDLY_DEHYDRATED -> Palette(Color(0xFFEF6C00), Color(0xFFFFF3E0), "⚠")
        HydrationStatus.DEHYDRATED       -> Palette(Color(0xFFC62828), Color(0xFFFDECEC), "⚠")
    }

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clickable { onBackClick() },
                        contentAlignment = Alignment.CenterStart
                    ) { Text("←", fontSize = 22.sp, color = Color.White) }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clickable { onHomeClick() },
                        contentAlignment = Alignment.CenterEnd
                    ) { Text("⌂", fontSize = 22.sp, color = Color.White) }
                }
                Spacer(Modifier.height(10.dp))
                Text("Test Results", color = Color.White,
                    fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = formatTimestamp(result),
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp
                )
            }
        }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // ── Status card ─────────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.accent.copy(alpha = 0.35f),
                        RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(palette.bg),
                        contentAlignment = Alignment.Center
                    ) { Text(palette.icon, fontSize = 30.sp, color = palette.accent) }

                    Spacer(Modifier.height(12.dp))
                    Text(
                        result.status.label,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.accent
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        result.status.description,
                        fontSize = 13.sp,
                        color = Color(0xFF757575)
                    )
                    Spacer(Modifier.height(14.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(palette.bg)
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Algorithm Confidence",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.accent)
                            Text(
                                "%.1f%%".format(result.confidence * 100f),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.accent
                            )
                        }
                    }
                }
            }

            // ── Sensor data card ────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Sensor Data",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF212121))
                    Spacer(Modifier.height(14.dp))

                    SensorRow(
                        label = "pH Level",
                        value = "%.1f".format(result.reading.pH!!),
                        inRange = result.pHInRange,
                        reference = "%.1f – %.1f".format(
                            UrinalysisResult.PH_MIN, UrinalysisResult.PH_MAX)
                    )
                    Divider()
                    SensorRow(
                        label = "Specific Gravity",
                        value = "%.3f".format(result.reading.specificGravity!!),
                        inRange = result.sgInRange,
                        reference = "%.3f – %.3f".format(
                            UrinalysisResult.SG_MIN, UrinalysisResult.SG_MAX)
                    )
                    Divider()
                    ColorRow(result = result)
                }
            }

            // ── Recommendation ──────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("💡", fontSize = 18.sp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Recommendation",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF0D47A1))
                        Spacer(Modifier.height(2.dp))
                        Text(
                            result.status.recommendation,
                            fontSize = 13.sp,
                            color = Color(0xFF1565C0),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // ── Actions ─────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onHomeClick,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0D47A1)
                    )
                ) { Text("Back to Dashboard",
                    fontSize = 14.sp, fontWeight = FontWeight.Bold) }

                OutlinedButton(
                    onClick = onViewHistoryClick,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF0D47A1)
                    )
                ) { Text("View History",
                    fontSize = 14.sp, fontWeight = FontWeight.Bold) }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Row helpers ───────────────────────────────────────────────────────────
@Composable
private fun SensorRow(
    label: String,
    value: String,
    inRange: Boolean,
    reference: String
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 13.sp, color = Color(0xFF212121),
                fontWeight = FontWeight.Medium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121))
                Spacer(Modifier.width(6.dp))
                Text(
                    if (inRange) "✓" else "✕",
                    fontSize = 14.sp,
                    color = if (inRange) Color(0xFF2E7D32) else Color(0xFFC62828),
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("Reference", fontSize = 10.sp, color = Color(0xFF9E9E9E))
            Text(reference, fontSize = 12.sp, color = Color(0xFF424242))
        }
    }
}

@Composable
private fun ColorRow(result: UrinalysisResult) {
    val hsv = result.reading.color!!
    val composeColor = Color.hsv(hsv.hue, hsv.saturation / 100f, hsv.value / 100f)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Urine Color", fontSize = 13.sp, color = Color(0xFF212121),
                fontWeight = FontWeight.Medium)
            Text(
                "HSV: %.0f°, %.0f%%, %.0f%%".format(hsv.hue, hsv.saturation, hsv.value),
                fontSize = 11.sp,
                color = Color(0xFF9E9E9E)
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("Reference", fontSize = 10.sp, color = Color(0xFF9E9E9E))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(result.colorLabel, fontSize = 12.sp, color = Color(0xFF424242))
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(composeColor)
                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(6.dp))
                )
            }
        }
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFFEEEEEE))
    )
}

private data class Palette(
    val accent: Color,
    val bg: Color,
    val icon: String
)

private fun formatTimestamp(r: UrinalysisResult): String {
    val fmt = SimpleDateFormat("MMMM d, yyyy 'at' hh:mm a", Locale.getDefault())
    return fmt.format(r.timestamp)
}
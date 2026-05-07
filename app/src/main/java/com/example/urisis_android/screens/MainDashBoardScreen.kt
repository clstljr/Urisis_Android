package com.example.urisis_android.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.urisis_android.bluetooth.BleConnectionState
import com.example.urisis_android.bluetooth.BleViewModel
import com.example.urisis_android.util.Greeting

@Composable
fun MainDashboardScreen(
    userName: String,
    bleViewModel: BleViewModel,
    onConnectClick: () -> Unit = {},
    onStartUrinalysisClick: (demo: Boolean) -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val bleState by bleViewModel.bleState.collectAsState()
    val isConnected = bleState.connectionState == BleConnectionState.CONNECTED
    // Demo mode is only meaningful when no device is connected. The toggle
    // is hidden in the connected branch below, so this flag effectively
    // governs the "no device" path. Force false once connected so a stale
    // toggle from a previous session can't accidentally route real data
    // through the demo synthesizer.
    var demoMode by remember { mutableStateOf(false) }
    val effectiveDemo = demoMode && !isConnected

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Blue gradient header background — extends behind status bar ───
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF1565C0), Color(0xFF29B6F6))
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

            // ── Header row — uses real status bar inset ───────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(WindowInsets.statusBars.asPaddingValues())
                    .padding(horizontal = 20.dp)
                    .padding(top = 24.dp, bottom = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = Greeting.forNow(),
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = userName,
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable { onProfileClick() },
                        contentAlignment = Alignment.Center
                    ) { Text("👤", fontSize = 18.sp) }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable { onLogoutClick() },
                        contentAlignment = Alignment.Center
                    ) { Text("↪", fontSize = 18.sp, color = Color.White) }
                }
            }

            // ── Card body ─────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF2F4F7))
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // ── BLE banner ────────────────────────────────────────────
                if (!isConnected) {

                    // Disconnected card — tap to open ConnectDeviceScreen
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onConnectClick() },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEEEEEE)),
                                contentAlignment = Alignment.Center
                            ) { Text("📵", fontSize = 20.sp) }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "No device connected",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF212121)
                                )
                                Text(
                                    text = "Tap to pair your Arduino sensor",
                                    fontSize = 12.sp,
                                    color = Color(0xFF9E9E9E)
                                )
                            }
                            Text("›", fontSize = 22.sp, color = Color(0xFF9E9E9E))
                        }
                    }

                    // Demo mode toggle — when ON, "Start Urinalysis" runs
                    // the synthetic generator instead of waiting for BLE data.
                    // Useful for screen demos and algorithm verification.
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (demoMode) Color(0xFFFFF3E0)
                            else Color(0xFFFFF8F0)
                        ),
                        elevation = CardDefaults.cardElevation(0.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📊", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Demo mode",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF212121)
                                )
                                Text(
                                    text = if (demoMode)
                                        "ON — tests will use synthetic data"
                                    else
                                        "OFF — connect a device to run a test",
                                    fontSize = 12.sp,
                                    color = if (demoMode) Color(0xFFE65100)
                                    else Color(0xFF9E9E9E),
                                    fontWeight = if (demoMode) FontWeight.SemiBold
                                    else FontWeight.Normal
                                )
                            }
                            Switch(
                                checked = demoMode,
                                onCheckedChange = { demoMode = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFFFF9800)
                                )
                            )
                        }
                    }

                } else {

                    // Connected — green banner with device name
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32)),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) { Text("📡", fontSize = 20.sp) }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(SpanStyle(color = Color(0xFF80FF80))) {
                                            append("● ")
                                        }
                                        withStyle(
                                            SpanStyle(
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        ) { append("Connected") }
                                    },
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = bleState.connectedDevice?.name
                                        ?: bleState.connectedDevice?.address ?: "",
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }

                            OutlinedButton(
                                onClick = { bleViewModel.disconnect() },
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("Disconnect", fontSize = 13.sp)
                            }
                        }
                    }

                    // Live sensor row — shows RSSI once connected
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF3FB)),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📡", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Live sensor data",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF212121)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = bleState.connectedDevice
                                    ?.let { "•  ${it.rssi} dBm" } ?: "•  Streaming",
                                fontSize = 12.sp,
                                color = Color(0xFF9E9E9E),
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = true,
                                onCheckedChange = {},
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF2E7D32)
                                )
                            )
                        }
                    }
                }

                // ── Metric cards ──────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(Modifier.weight(1f), "💧", Color(0xFFE3F2FD), "88%",     "Hydration")
                    MetricCard(Modifier.weight(1f), "📈", Color(0xFFE8F5E9), "5.57",    "pH Level")
                    MetricCard(Modifier.weight(1f), "⚙️", Color(0xFFFFF3E0), "311 ppm", "TDS")
                }

                HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 1.dp)

                // ── Start Urinalysis ──────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = if (effectiveDemo) {
                                    // Orange tint matches the demo card so the
                                    // user has a clear visual cue they're about
                                    // to run a simulated test, not a real one.
                                    listOf(Color(0xFFE65100), Color(0xFFFF9800))
                                } else {
                                    listOf(Color(0xFF1565C0), Color(0xFF42A5F5))
                                }
                            )
                        )
                        .clickable { onStartUrinalysisClick(effectiveDemo) }
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (effectiveDemo) "🧪" else "🧪",
                                fontSize = 26.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (effectiveDemo) "Start Demo Test"
                                else "Start Urinalysis",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (effectiveDemo)
                                    "Generate synthetic data — no device needed"
                                else "Begin a new hydration test",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 13.sp
                            )
                        }
                        Text("→", fontSize = 22.sp, color = Color.White)
                    }
                }

                // ── History ───────────────────────────────────────────────
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onHistoryClick() },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE3F2FD)),
                            contentAlignment = Alignment.Center
                        ) { Text("🕐", fontSize = 22.sp) }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "History",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF212121)
                            )
                            Text(
                                "Past results",
                                fontSize = 12.sp,
                                color = Color(0xFF9E9E9E)
                            )
                        }
                        Text("›", fontSize = 22.sp, color = Color(0xFF9E9E9E))
                    }
                }

                // ── Pro Tip ───────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("💡", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "PRO TIP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9800),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "For best accuracy, collect a midstream urine sample in a clean, dry container before testing.",
                            fontSize = 13.sp,
                            color = Color(0xFF424242),
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}

// ── Reusable metric card ──────────────────────────────────────────────────────
@Composable
private fun MetricCard(
    modifier: Modifier,
    emoji: String,
    emojiBackground: Color,
    value: String,
    label: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(emojiBackground),
                contentAlignment = Alignment.Center
            ) { Text(emoji, fontSize = 18.sp) }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF212121)
            )
            Text(
                label,
                fontSize = 11.sp,
                color = Color(0xFF9E9E9E)
            )
        }
    }
}
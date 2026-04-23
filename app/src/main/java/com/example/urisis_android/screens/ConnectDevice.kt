package com.example.urisis_android.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.urisis_android.bluetooth.BleConnectionState
import com.example.urisis_android.bluetooth.BleDevice
import com.example.urisis_android.bluetooth.BleViewModel

@Composable
fun ConnectDeviceScreen(
    bleViewModel: BleViewModel,
    onBackClick: () -> Unit = {},
    onDeviceConnected: () -> Unit = {}
) {
    val bleState by bleViewModel.bleState.collectAsState()
    val isScanning = bleState.connectionState == BleConnectionState.SCANNING
    val isConnecting = bleState.connectionState == BleConnectionState.CONNECTING

    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) bleViewModel.startScan()
    }

    // Pop back to dashboard once connection lands
    LaunchedEffect(bleState.connectionState) {
        if (bleState.connectionState == BleConnectionState.CONNECTED) onDeviceConnected()
    }

    // Always stop scanning when leaving this screen
    DisposableEffect(Unit) {
        onDispose { bleViewModel.stopScan() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F4F7))
    ) {
        // ── Header ───────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF1565C0), Color(0xFF29B6F6))
                    )
                )
                .padding(horizontal = 20.dp)
                .padding(top = 52.dp, bottom = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable { onBackClick() },
                    contentAlignment = Alignment.Center
                ) { Text("‹", fontSize = 22.sp, color = Color.White) }

                Spacer(Modifier.width(14.dp))

                Column {
                    Text(
                        "Connect device",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (isScanning) "Scanning in real time..."
                        else "Tap Start to discover sensors",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                }
            }
        }

        // ── Scan status card ─────────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                ScanningIndicator(active = isScanning)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isScanning) "Searching nearby..."
                        else "${bleState.discoveredDevices.size} device(s) found",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF212121)
                    )
                    Text(
                        text = bleState.errorMessage
                            ?: if (isScanning) "Updating live — closer devices appear first"
                            else "Tap Start to rescan",
                        fontSize = 12.sp,
                        color = if (bleState.errorMessage != null) Color(0xFFC62828)
                        else Color(0xFF9E9E9E)
                    )
                }
                Button(
                    onClick = {
                        if (isScanning) {
                            bleViewModel.stopScan()
                        } else {
                            val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                arrayOf(
                                    Manifest.permission.BLUETOOTH_SCAN,
                                    Manifest.permission.BLUETOOTH_CONNECT
                                )
                            } else {
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                            if (bleViewModel.hasPermissions()) bleViewModel.startScan()
                            else permLauncher.launch(perms)
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isScanning) Color(0xFFC62828)
                        else Color(0xFF1565C0)
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        if (isScanning) "Stop" else "Start",
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
            }
        }

        // ── Device list or empty state ───────────────────────────────────
        if (bleState.discoveredDevices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔍", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (isScanning) "Looking for sensors..."
                        else "No devices yet",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF424242)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Make sure your Arduino sensor is powered on and within range.",
                        fontSize = 13.sp,
                        color = Color(0xFF9E9E9E),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = bleState.discoveredDevices,
                    key = { it.address }
                ) { device ->
                    DeviceRow(
                        device = device,
                        isConnecting = isConnecting &&
                                bleState.connectedDevice?.address == device.address,
                        onClick = { bleViewModel.connect(device) }
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

// ── Pulsing radar icon ────────────────────────────────────────────────────
@Composable
private fun ScanningIndicator(active: Boolean) {
    val transition = rememberInfiniteTransition(label = "scan")
    val scale by transition.animateFloat(
        initialValue = 1f, targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val alpha by transition.animateFloat(
        initialValue = 0.35f, targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
        if (active) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(Color(0xFF1565C0).copy(alpha = alpha))
            )
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (active) Color(0xFF1565C0) else Color(0xFFBDBDBD)),
            contentAlignment = Alignment.Center
        ) {
            Text(if (active) "📡" else "🔎", fontSize = 18.sp)
        }
    }
}

// ── Single device row with RSSI bars ──────────────────────────────────────
@Composable
private fun DeviceRow(
    device: BleDevice,
    isConnecting: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isConnecting) { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE3F2FD)),
                contentAlignment = Alignment.Center
            ) { Text("🩺", fontSize = 20.sp) }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name?.takeIf { it.isNotBlank() } ?: "Unknown device",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF212121)
                )
                Text(
                    text = device.address,
                    fontSize = 11.sp,
                    color = Color(0xFF9E9E9E)
                )
            }

            Spacer(Modifier.width(8.dp))

            if (isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFF1565C0)
                )
            } else {
                SignalBars(rssi = device.rssi)
            }
        }
    }
}

@Composable
private fun SignalBars(rssi: Int) {
    // RSSI: roughly -30 (very close) .. -100 (far). Map to 0..4 bars.
    val strength = when {
        rssi >= -55 -> 4
        rssi >= -65 -> 3
        rssi >= -75 -> 2
        rssi >= -85 -> 1
        else        -> 0
    }
    val color = when (strength) {
        4, 3 -> Color(0xFF2E7D32)
        2    -> Color(0xFFF9A825)
        else -> Color(0xFFC62828)
    }
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        (1..4).forEach { i ->
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height((4 + i * 3).dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (i <= strength) color else Color(0xFFE0E0E0))
            )
        }
        Spacer(Modifier.width(4.dp))
        Text("${rssi}dBm", fontSize = 10.sp, color = Color(0xFF9E9E9E))
    }
}
package com.example.urisis_android.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import com.example.urisis_android.urinalysis.AnalysisStep
import com.example.urisis_android.urinalysis.UrinalysisStage
import com.example.urisis_android.urinalysis.UrinalysisViewModel

@Composable
fun WaitingForDeviceScreen(
    viewModel: UrinalysisViewModel,
    onAdvance: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val ui by viewModel.ui.collectAsState()

    LaunchedEffect(ui.stage) {
        if (ui.stage == UrinalysisStage.ANALYZING) onAdvance()
    }

    BlueGradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PulsingLoader()
            Spacer(Modifier.height(36.dp))
            Text(
                "Waiting for Device\nInitialization...",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Press the physical button on the device to begin",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            AnimatedDots()
            Spacer(Modifier.height(28.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .padding(16.dp)
            ) {
                Text(
                    "The device will automatically start the analysis once the button is pressed and the sensors are initialized.",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun AnalyzingSampleScreen(
    viewModel: UrinalysisViewModel,
    onComplete: () -> Unit = {}
) {
    val ui by viewModel.ui.collectAsState()

    LaunchedEffect(ui.stage) {
        if (ui.stage == UrinalysisStage.COMPLETE) onComplete()
    }

    BlueGradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top half — pulsing loader + title
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                PulsingLoader()
                Spacer(Modifier.height(32.dp))
                Text(
                    "Analyzing Sample...",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Bottom half — checklist + progress
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.12f))
                    .padding(horizontal = 24.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AnalysisStep.values().forEach { step ->
                    val done = step in ui.completedSteps
                    val active = ui.currentStep == step
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StepIndicator(done = done, active = active)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = if (active) "${step.label}..." else step.label,
                            color = when {
                                done -> Color.White
                                active -> Color.White
                                else -> Color.White.copy(alpha = 0.55f)
                            },
                            fontSize = 14.sp,
                            fontWeight = if (done || active) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.25f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(ui.progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White)
                    )
                }

                Text(
                    "Please wait while the device performs a comprehensive analysis of your urine sample using advanced sensor technology.",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ── Shared visuals ─────────────────────────────────────────────────────────
@Composable
private fun BlueGradientBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF1565C0), Color(0xFF29B6F6)),
                    start = Offset(0f, 0f),
                    end = Offset(1000f, 1000f)
                )
            ),
        content = content
    )
}

@Composable
private fun PulsingLoader() {
    val rot by rememberInfiniteTransition(label = "rot").animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing)
        ),
        label = "rotSpin"
    )
    Box(
        modifier = Modifier
            .size(150.dp)
            .clip(CircleShape)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(130.dp)
                .rotate(rot)
        ) {
            drawArc(
                color = Color(0xFF1565C0),
                startAngle = -90f,
                sweepAngle = 90f,
                useCenter = false,
                style = Stroke(width = 8f, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun AnimatedDots() {
    val phase by rememberInfiniteTransition(label = "dots").animateFloat(
        initialValue = 0f, targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing)
        ),
        label = "dotsPhase"
    )
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(3) { i ->
            val alpha = if (phase.toInt() == i) 0.9f else 0.35f
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = alpha))
            )
        }
    }
}

@Composable
private fun StepIndicator(done: Boolean, active: Boolean) {
    val rotation by rememberInfiniteTransition(label = "step").animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing)
        ),
        label = "stepSpin"
    )
    Box(modifier = Modifier.size(22.dp), contentAlignment = Alignment.Center) {
        when {
            done -> {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2E7D32)),
                    contentAlignment = Alignment.Center
                ) { Text("✓", color = Color.White, fontSize = 13.sp,
                    fontWeight = FontWeight.Bold) }
            }
            active -> {
                Canvas(modifier = Modifier.size(22.dp).rotate(rotation)) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.3f),
                        style = Stroke(width = 3f)
                    )
                    drawArc(
                        color = Color.White,
                        startAngle = -90f, sweepAngle = 90f,
                        useCenter = false,
                        style = Stroke(width = 3f, cap = StrokeCap.Round)
                    )
                }
            }
            else -> {
                Canvas(modifier = Modifier.size(22.dp)) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.4f),
                        style = Stroke(width = 2f)
                    )
                }
            }
        }
    }
}
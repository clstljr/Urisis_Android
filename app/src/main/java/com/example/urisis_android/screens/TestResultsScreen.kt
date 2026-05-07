package com.example.urisis_android.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.urisis_android.urinalysis.HydrationStatus
import com.example.urisis_android.urinalysis.UrinalysisResult
import com.example.urisis_android.urinalysis.UrinalysisViewModel
import java.text.SimpleDateFormat
import java.util.Locale

// ════════════════════════════════════════════════════════════════════════
//  TestResultsScreen — main result screen (mockups 1 & 2)
// ════════════════════════════════════════════════════════════════════════

@Composable
fun TestResultsScreen(
    viewModel: UrinalysisViewModel,
    onBackClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onViewHistoryClick: () -> Unit = {},
    onAdvancedClick: () -> Unit = {}
) {
    val ui by viewModel.ui.collectAsState()
    val result = ui.result ?: return

    val theme = statusTheme(result.status)
    val hydrationPct = hydrationPercent(result.status)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F4F7))
            .verticalScroll(rememberScrollState())
    ) {
        Header(
            timestamp = formatTimestamp(result),
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            StatusCard(
                theme = theme,
                status = result.status,
                hydrationPct = hydrationPct,
                confidence = result.confidence
            )
            HydrationLevelCard(percent = hydrationPct, accent = theme.accent)
            ParameterStatusCard(result = result)
            UrineColorCard(result = result)
            RecommendationCard(message = result.status.recommendation)
            if (result.activeFlags.isNotEmpty()) {
                AbnormalFlagsCard(flags = result.activeFlags)
            }
            AdvancedResultsCard(onClick = onAdvancedClick)

            Spacer(Modifier.height(2.dp))
            PrimaryButton(
                text = "Back to Dashboard",
                leadingEmoji = "⌂",
                onClick = onHomeClick
            )
            SecondaryButton(
                text = "View History",
                leadingEmoji = "🕒",
                onClick = onViewHistoryClick
            )
            Spacer(Modifier.height(10.dp))
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────
@Composable
private fun Header(timestamp: String, onBackClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF1E88E5), Color(0xFF29B6F6))
                )
            )
            .padding(horizontal = 20.dp)
            .padding(top = 52.dp, bottom = 30.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassPill(text = "‹  Dashboard", onClick = onBackClick)
                GlassPill(text = "DEMO", onClick = null, compact = true)
            }
            Spacer(Modifier.height(22.dp))
            Text(
                "Test Results",
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📅", fontSize = 14.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    timestamp,
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun GlassPill(
    text: String,
    onClick: (() -> Unit)?,
    compact: Boolean = false
) {
    val base = Modifier
        .clip(RoundedCornerShape(50))
        .background(Color.White.copy(alpha = 0.22f))
    val tappable = if (onClick != null) base.clickable { onClick() } else base
    Box(
        modifier = tappable.padding(
            horizontal = if (compact) 14.dp else 16.dp,
            vertical = if (compact) 8.dp else 10.dp
        )
    ) {
        Text(
            text,
            color = Color.White,
            fontSize = if (compact) 12.sp else 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ── Status card (filled gradient) ─────────────────────────────────────────
@Composable
private fun StatusCard(
    theme: StatusTheme,
    status: HydrationStatus,
    hydrationPct: Int,
    confidence: Float
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(theme.gradientStart, theme.gradientEnd)
                )
            )
            .padding(horizontal = 18.dp, vertical = 22.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    theme.icon,
                    fontSize = 28.sp,
                    color = theme.accent,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(18.dp))
            Column {
                Text(
                    status.label.uppercase(),
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Hydration: $hydrationPct%",
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.25f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        "▣ %.1f%% confidence".format(confidence * 100f),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ── Hydration level card ──────────────────────────────────────────────────
@Composable
private fun HydrationLevelCard(percent: Int, accent: Color) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("💧", fontSize = 20.sp)
            Spacer(Modifier.width(10.dp))
            Text(
                "Hydration Level",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827)
            )
        }
        Spacer(Modifier.height(16.dp))
        // Custom bar — sidesteps Material3 LinearProgressIndicator API drift
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFFE5E7EB))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percent / 100f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(accent)
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("0% · Dehydrated", fontSize = 12.sp, color = Color(0xFF9CA3AF))
            Text(
                "$percent%",
                fontSize = 15.sp,
                color = accent,
                fontWeight = FontWeight.Bold
            )
            Text("Well Hydrated · 100%", fontSize = 12.sp, color = Color(0xFF9CA3AF))
        }
    }
}

// ── Parameter Status card ─────────────────────────────────────────────────
@Composable
private fun ParameterStatusCard(result: UrinalysisResult) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("〰", fontSize = 18.sp, color = Color(0xFF1E88E5))
            Spacer(Modifier.width(10.dp))
            Text(
                "Parameter Status",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827)
            )
        }
        Spacer(Modifier.height(8.dp))
        ParameterRow(
            iconBg = Color(0xFFE3F2FD),
            iconFg = Color(0xFF1E88E5),
            iconChar = "⚡",
            title = "pH",
            subtitle = "Acid-base balance of urine",
            inRange = result.pHInRange
        )
        RowDivider()
        ParameterRow(
            iconBg = Color(0xFFEDE7F6),
            iconFg = Color(0xFF7E57C2),
            iconChar = "⚖",
            title = "Specific Gravity",
            subtitle = "Urine concentration level",
            inRange = result.sgInRange
        )
        RowDivider()
        // TDS is a real measured channel from the device's probe.
        ParameterRow(
            iconBg = Color(0xFFFFF3E0),
            iconFg = Color(0xFFEF6C00),
            iconChar = "⚛",
            title = "TDS",
            subtitle = "Total dissolved solids",
            inRange = result.tdsInRange
        )
    }
}

@Composable
private fun ParameterRow(
    iconBg: Color,
    iconFg: Color,
    iconChar: String,
    title: String,
    subtitle: String,
    inRange: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                iconChar,
                fontSize = 18.sp,
                color = iconFg,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827)
            )
            Text(subtitle, fontSize = 12.sp, color = Color(0xFF6B7280))
        }
        StatusPill(inRange = inRange)
    }
}

@Composable
private fun StatusPill(inRange: Boolean) {
    val bg = if (inRange) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)
    val fg = if (inRange) Color(0xFF059669) else Color(0xFFDC2626)
    val label = if (inRange) "Normal" else "Abnormal"
    val icon = if (inRange) "✓" else "✕"
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(fg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    icon,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(label, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ── Urine Color card ──────────────────────────────────────────────────────
@Composable
private fun UrineColorCard(result: UrinalysisResult) {
    val hsv = result.reading.color!!
    val swatch = Color.hsv(hsv.hue, hsv.saturation / 100f, hsv.value / 100f)
    val colorOk = result.status == HydrationStatus.WELL_HYDRATED ||
            result.status == HydrationStatus.NORMAL
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🎨", fontSize = 18.sp)
            Spacer(Modifier.width(10.dp))
            Text(
                "Urine Color",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827)
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(78.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(swatch)
                    .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(14.dp))
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    result.colorLabel,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
                Text(
                    colorSubtitle(result.colorLabel),
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )
            }
            StatusPill(inRange = colorOk)
        }
    }
}

// ── Abnormal flags card (Flag A/B/C from the Fuzzy KNN) ──────────────────
@Composable
private fun AbnormalFlagsCard(flags: List<com.example.urisis_android.urinalysis.UrineClass>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, Color(0xFFFECACA))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDC2626)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("⚠", color = Color.White, fontSize = 18.sp,
                        fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "Abnormal Findings",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFB91C1C)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "The Fuzzy KNN classifier flagged the following indicators. " +
                        "These are screening flags only — please consult a physician " +
                        "for clinical interpretation.",
                fontSize = 13.sp,
                color = Color(0xFF7F1D1D),
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(12.dp))
            flags.forEach { flag ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDC2626))
                            .padding(top = 4.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "${flag.displayName} — ${flag.hydrationStatus}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF991B1B)
                        )
                        Text(
                            flag.clinicalNote,
                            fontSize = 12.sp,
                            color = Color(0xFF7F1D1D),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

// ── Recommendation card ───────────────────────────────────────────────────
@Composable
private fun RecommendationCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFCD34D).copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Text("💡", fontSize = 18.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "Recommendation",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF92400E)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    message,
                    fontSize = 14.sp,
                    color = Color(0xFF78350F),
                    lineHeight = 19.sp
                )
            }
        }
    }
}

// ── Advanced Results entry card ───────────────────────────────────────────
@Composable
private fun AdvancedResultsCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF5FF)),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, Color(0xFFDDD6FE))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEDE9FE)),
                contentAlignment = Alignment.Center
            ) {
                Text("〰", fontSize = 18.sp, color = Color(0xFF7C3AED))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Advanced Results",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF6D28D9)
                )
                Text(
                    "Raw sensor values · For qualified professionals only",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280),
                    lineHeight = 16.sp
                )
            }
            Text(
                "›",
                fontSize = 24.sp,
                color = Color(0xFF7C3AED),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ── Buttons ───────────────────────────────────────────────────────────────
@Composable
private fun PrimaryButton(text: String, leadingEmoji: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
    ) {
        Text(leadingEmoji, fontSize = 16.sp, color = Color.White)
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun SecondaryButton(text: String, leadingEmoji: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color(0xFF1E88E5),
            containerColor = Color.White
        )
    ) {
        Text(leadingEmoji, fontSize = 16.sp, color = Color(0xFF1E88E5))
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E88E5)
        )
    }
}

// ════════════════════════════════════════════════════════════════════════
//  AdvancedResultsScreen — Professional-Use gate (mockups 3 & 4)
// ════════════════════════════════════════════════════════════════════════

@Composable
fun AdvancedResultsScreen(
    onUnderstand: () -> Unit = {},
    onGoBack: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F4F7))
            .verticalScroll(rememberScrollState())
    ) {
        // Red warning header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFFB91C1C), Color(0xFFDC2626))
                    )
                )
                .padding(top = 56.dp, bottom = 36.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(78.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "⚠",
                        fontSize = 40.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "PROFESSIONAL\nUSE ONLY",
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 34.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Pink "Do Not Self-Diagnose"
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                elevation = CardDefaults.cardElevation(0.dp),
                border = BorderStroke(1.dp, Color(0xFFFECACA))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("✋", fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Do Not Self-Diagnose",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB91C1C)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "The raw sensor values shown in Advanced Results are " +
                                "highly technical data. They are intended exclusively " +
                                "for qualified healthcare professionals — such as " +
                                "physicians, nurses, or licensed laboratory " +
                                "technicians — who are trained to interpret them " +
                                "within the full clinical context of a patient.",
                        fontSize = 14.sp,
                        color = Color(0xFF1F2937),
                        lineHeight = 20.sp
                    )
                }
            }

            WarningBullet(
                "Interpreting these values without clinical training can " +
                        "lead to incorrect conclusions, unnecessary anxiety, or " +
                        "dangerous inaction."
            )
            WarningBullet(
                "Abnormal readings do not automatically indicate disease. " +
                        "A trained professional must consider your full medical " +
                        "history, symptoms, and other test results before drawing " +
                        "any conclusion."
            )
            WarningBullet(
                "This device is a screening tool only. It does not replace " +
                        "laboratory urinalysis, clinical examination, or " +
                        "professional medical judgment."
            )

            // Yellow note
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                elevation = CardDefaults.cardElevation(0.dp),
                border = BorderStroke(1.dp, Color(0xFFFDE68A))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("👤", fontSize = 18.sp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "If you are not a qualified healthcare professional:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Please tap \"Go Back\" below. Share the overall " +
                                    "results (Normal / Mild Dehydration / Dehydrated) " +
                                    "with your doctor instead of attempting to " +
                                    "interpret the raw values yourself.",
                            fontSize = 13.sp,
                            color = Color(0xFF78350F),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            Button(
                onClick = onUnderstand,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
            ) {
                Text(
                    "✓",
                    fontSize = 16.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "I Am a Qualified Professional — I Understand",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onGoBack() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Go Back — I Don't Need This",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF6B7280)
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun WarningBullet(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(Color(0xFFDC2626)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "✕",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text,
            fontSize = 14.sp,
            color = Color(0xFF1F2937),
            lineHeight = 20.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

// ════════════════════════════════════════════════════════════════════════
//  AdvancedSensorDataScreen — raw values for qualified pros (post-gate)
// ════════════════════════════════════════════════════════════════════════

@Composable
fun AdvancedSensorDataScreen(
    viewModel: UrinalysisViewModel,
    onBackClick: () -> Unit = {}
) {
    val ui by viewModel.ui.collectAsState()
    val result = ui.result ?: return

    val hsv = result.reading.color!!
    val swatch = Color.hsv(hsv.hue, hsv.saturation / 100f, hsv.value / 100f)
    val (rR, rG, rB) = swatchRgb(swatch)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F4F7))
            .verticalScroll(rememberScrollState())
    ) {
        // Purple gradient header — matches the entry card on TestResultsScreen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF7C3AED), Color(0xFF6366F1))
                    )
                )
                .padding(horizontal = 20.dp)
                .padding(top = 52.dp, bottom = 30.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassPill(text = "‹  Results", onClick = onBackClick)
                    GlassPill(text = "PROFESSIONAL", onClick = null, compact = true)
                }
                Spacer(Modifier.height(22.dp))
                Text(
                    "Advanced Results",
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📅", fontSize = 13.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        formatTimestamp(result),
                        color = Color.White.copy(alpha = 0.95f),
                        fontSize = 13.sp
                    )
                }
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            DisclaimerStrip()

            AlgorithmOutputCard(
                status = result.status,
                confidence = result.confidence
            )

            RawValueCard(
                emoji = "⚡",
                accent = Color(0xFF1E88E5),
                bg = Color(0xFFE3F2FD),
                title = "pH",
                subtitle = "Acid-base balance",
                displayValue = "%.2f".format(result.reading.pH!!),
                unit = "",
                rawValue = result.reading.pH!!,
                inRange = result.pHInRange,
                scaleMin = 0f,
                scaleMax = 14f,
                referenceMin = UrinalysisResult.PH_MIN,
                referenceMax = UrinalysisResult.PH_MAX,
                referenceLabel = "%.1f – %.1f".format(
                    UrinalysisResult.PH_MIN, UrinalysisResult.PH_MAX
                )
            )

            RawValueCard(
                emoji = "⚖",
                accent = Color(0xFF7E57C2),
                bg = Color(0xFFEDE7F6),
                title = "Specific Gravity",
                subtitle = "Urine concentration",
                displayValue = "%.3f".format(result.reading.specificGravity!!),
                unit = "",
                rawValue = result.reading.specificGravity!!,
                inRange = result.sgInRange,
                scaleMin = 1.000f,
                scaleMax = 1.040f,
                referenceMin = UrinalysisResult.SG_MIN,
                referenceMax = UrinalysisResult.SG_MAX,
                referenceLabel = "%.3f – %.3f".format(
                    UrinalysisResult.SG_MIN, UrinalysisResult.SG_MAX
                )
            )

            TdsCard(
                tdsPpm = result.reading.tdsPpm ?: 0f,
                inRange = result.tdsInRange
            )

            ColorAnalysisCard(
                swatch = swatch,
                label = result.colorLabel,
                hue = hsv.hue,
                saturation = hsv.saturation,
                value = hsv.value,
                r = rR, g = rG, b = rB
            )

            MetadataCard(result = result)

            Spacer(Modifier.height(2.dp))
            SecondaryButton(
                text = "Back to Results",
                leadingEmoji = "‹",
                onClick = onBackClick
            )
            Spacer(Modifier.height(10.dp))
        }
    }
}

// ── Disclaimer strip ──────────────────────────────────────────────────────
@Composable
private fun DisclaimerStrip() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFEF2F2))
            .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDC2626)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "!",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                "Raw values — clinical interpretation required.",
                fontSize = 12.sp,
                color = Color(0xFF991B1B),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ── Algorithm output ──────────────────────────────────────────────────────
@Composable
private fun AlgorithmOutputCard(status: HydrationStatus, confidence: Float) {
    val theme = statusTheme(status)
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("⚙", fontSize = 18.sp, color = Color(0xFF6B7280))
            Spacer(Modifier.width(10.dp))
            Text(
                "Algorithm Output",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827)
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text("Classification", fontSize = 12.sp, color = Color(0xFF6B7280))
                Text(
                    status.label.uppercase(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.accent
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Confidence", fontSize = 12.sp, color = Color(0xFF6B7280))
                Text(
                    "%.1f%%".format(confidence * 100f),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFFE5E7EB))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(confidence.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(theme.accent)
            )
        }
    }
}

// ── Raw value card with reference-range bar ───────────────────────────────
@Composable
private fun RawValueCard(
    emoji: String,
    accent: Color,
    bg: Color,
    title: String,
    subtitle: String,
    displayValue: String,
    unit: String,
    rawValue: Float,
    inRange: Boolean,
    scaleMin: Float,
    scaleMax: Float,
    referenceMin: Float,
    referenceMax: Float,
    referenceLabel: String
) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    emoji,
                    fontSize = 18.sp,
                    color = accent,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
                Text(subtitle, fontSize = 12.sp, color = Color(0xFF6B7280))
            }
            StatusPill(inRange = inRange)
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                displayValue,
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827),
                fontFamily = FontFamily.Monospace
            )
            if (unit.isNotEmpty()) {
                Spacer(Modifier.width(6.dp))
                Text(
                    unit,
                    fontSize = 16.sp,
                    color = Color(0xFF6B7280),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        RangeBar(
            value = rawValue,
            scaleMin = scaleMin,
            scaleMax = scaleMax,
            referenceMin = referenceMin,
            referenceMax = referenceMax,
            markerColor = if (inRange) Color(0xFF059669) else Color(0xFFDC2626)
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Reference: $referenceLabel",
                fontSize = 12.sp,
                color = Color(0xFF6B7280)
            )
            Text(
                if (inRange) "Within range" else "Out of range",
                fontSize = 12.sp,
                color = if (inRange) Color(0xFF059669) else Color(0xFFDC2626),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun RangeBar(
    value: Float,
    scaleMin: Float,
    scaleMax: Float,
    referenceMin: Float,
    referenceMax: Float,
    markerColor: Color
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
    ) {
        val width = maxWidth
        val totalRange = (scaleMax - scaleMin).coerceAtLeast(0.0001f)
        val refStart = ((referenceMin - scaleMin) / totalRange).coerceIn(0f, 1f)
        val refEnd = ((referenceMax - scaleMin) / totalRange).coerceIn(0f, 1f)
        val markerPos = ((value - scaleMin) / totalRange).coerceIn(0f, 1f)

        // Background track
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFFE5E7EB))
        )
        // Reference range highlight
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = width * refStart)
                .width(width * (refEnd - refStart))
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFFA7F3D0))
        )
        // Value marker
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (width * markerPos - 2.dp).coerceAtLeast(0.dp))
                .width(4.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(markerColor)
        )
    }
}

// ── TDS (measured) ────────────────────────────────────────────────────────
@Composable
private fun TdsCard(tdsPpm: Float, inRange: Boolean) {
    val tdsValue = tdsPpm.toInt().coerceAtLeast(0)
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFFFF3E0)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "⚛",
                    fontSize = 18.sp,
                    color = Color(0xFFEF6C00),
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Total Dissolved Solids",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
                Text(
                    "Measured directly by TDS probe",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )
            }
            StatusPill(inRange = inRange)
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "%,d".format(tdsValue),
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827),
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "ppm",
                fontSize = 16.sp,
                color = Color(0xFF6B7280),
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        RangeBar(
            value = tdsPpm,
            scaleMin = 0f,
            scaleMax = 1500f,
            referenceMin = UrinalysisResult.TDS_MIN,
            referenceMax = UrinalysisResult.TDS_MAX,
            markerColor = if (inRange) Color(0xFF059669) else Color(0xFFDC2626)
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Reference: ${UrinalysisResult.TDS_MIN.toInt()} – ${UrinalysisResult.TDS_MAX.toInt()} ppm",
                fontSize = 12.sp,
                color = Color(0xFF6B7280)
            )
            Text(
                if (inRange) "Within range" else "Out of range",
                fontSize = 12.sp,
                color = if (inRange) Color(0xFF059669) else Color(0xFFDC2626),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ── Color analysis ────────────────────────────────────────────────────────
@Composable
private fun ColorAnalysisCard(
    swatch: Color,
    label: String,
    hue: Float,
    saturation: Float,
    value: Float,
    r: Int, g: Int, b: Int
) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🎨", fontSize = 18.sp)
            Spacer(Modifier.width(10.dp))
            Text(
                "Color Analysis",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827)
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(swatch)
                    .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Classified as", fontSize = 12.sp, color = Color(0xFF6B7280))
                Text(
                    label,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
                Text(
                    "Sensor: TCS34725",
                    fontSize = 11.sp,
                    color = Color(0xFF9CA3AF)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "HSV",
            fontSize = 11.sp,
            color = Color(0xFF6B7280),
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChannelChip(label = "H", value = "%.0f°".format(hue), modifier = Modifier.weight(1f))
            ChannelChip(label = "S", value = "%.0f%%".format(saturation), modifier = Modifier.weight(1f))
            ChannelChip(label = "V", value = "%.0f%%".format(value), modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "RGB",
            fontSize = 11.sp,
            color = Color(0xFF6B7280),
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChannelChip(label = "R", value = r.toString(), modifier = Modifier.weight(1f))
            ChannelChip(label = "G", value = g.toString(), modifier = Modifier.weight(1f))
            ChannelChip(label = "B", value = b.toString(), modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Hex",
                fontSize = 11.sp,
                color = Color(0xFF6B7280),
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFF3F4F6))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    "#%02X%02X%02X".format(r, g, b),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF111827),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ChannelChip(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF3F4F6))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6B7280)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827),
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// ── Sample metadata ───────────────────────────────────────────────────────
@Composable
private fun MetadataCard(result: UrinalysisResult) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("ⓘ", fontSize = 18.sp, color = Color(0xFF6B7280))
            Spacer(Modifier.width(10.dp))
            Text(
                "Sample Metadata",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827)
            )
        }
        Spacer(Modifier.height(8.dp))
        MetadataRow(
            "Sample ID",
            "URN-${result.timestamp.time.toString().takeLast(8)}"
        )
        MetadataRow("Captured", formatTimestamp(result))
        MetadataRow("Device", "Arduino + TCS34725")
        MetadataRow("Algorithm", "Enhanced Fuzzy KNN")
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = Color(0xFF6B7280))
        Text(
            value,
            fontSize = 13.sp,
            color = Color(0xFF111827),
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
    }
}

private fun swatchRgb(c: Color): Triple<Int, Int, Int> = Triple(
    (c.red * 255f).toInt().coerceIn(0, 255),
    (c.green * 255f).toInt().coerceIn(0, 255),
    (c.blue * 255f).toInt().coerceIn(0, 255)
)

// ── Shared helpers ────────────────────────────────────────────────────────
@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), content = content)
    }
}

@Composable
private fun RowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 52.dp)
            .height(1.dp)
            .background(Color(0xFFF1F5F9))
    )
}

private data class StatusTheme(
    val accent: Color,
    val gradientStart: Color,
    val gradientEnd: Color,
    val icon: String
)

private fun statusTheme(s: HydrationStatus): StatusTheme = when (s) {
    HydrationStatus.OVERHYDRATED -> StatusTheme(
        accent = Color(0xFF0891B2),
        gradientStart = Color(0xFF06B6D4),
        gradientEnd = Color(0xFF0891B2),
        icon = "≋"
    )
    HydrationStatus.WELL_HYDRATED -> StatusTheme(
        accent = Color(0xFF059669),
        gradientStart = Color(0xFF10B981),
        gradientEnd = Color(0xFF059669),
        icon = "✓"
    )
    HydrationStatus.NORMAL -> StatusTheme(
        accent = Color(0xFF16A34A),
        gradientStart = Color(0xFF22C55E),
        gradientEnd = Color(0xFF16A34A),
        icon = "✓"
    )
    HydrationStatus.MILDLY_DEHYDRATED -> StatusTheme(
        accent = Color(0xFFD97706),
        gradientStart = Color(0xFFFB923C),
        gradientEnd = Color(0xFFEA580C),
        icon = "⚠"
    )
    HydrationStatus.DEHYDRATED -> StatusTheme(
        accent = Color(0xFFDC2626),
        gradientStart = Color(0xFFEF4444),
        gradientEnd = Color(0xFFB91C1C),
        icon = "⚠"
    )
}

private fun hydrationPercent(s: HydrationStatus): Int = when (s) {
    HydrationStatus.OVERHYDRATED -> 95
    HydrationStatus.WELL_HYDRATED -> 80
    HydrationStatus.NORMAL -> 55
    HydrationStatus.MILDLY_DEHYDRATED -> 35
    HydrationStatus.DEHYDRATED -> 15
}

private fun colorSubtitle(label: String): String = when (label.lowercase()) {
    "transparent", "pale yellow", "straw" -> "Indicates adequate hydration"
    "yellow" -> "Within healthy range"
    "dark yellow", "amber" -> "Possible mild dehydration"
    "honey", "brown", "dark amber" -> "Possible dehydration"
    else -> "—"
}

private fun formatTimestamp(r: UrinalysisResult): String {
    val fmt = SimpleDateFormat("d MMMM yyyy 'at' HH:mm", Locale.getDefault())
    return fmt.format(r.timestamp)
}
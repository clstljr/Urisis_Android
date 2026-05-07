package com.example.urisis_android.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.urisis_android.urinalysis.DailyAggregate
import com.example.urisis_android.urinalysis.HistoryViewModel
import com.example.urisis_android.urinalysis.HydrationStatus
import com.example.urisis_android.urinalysis.TestRecord
import com.example.urisis_android.urinalysis.UrinalysisResult
import com.example.urisis_android.urinalysis.UrineClass
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * History screen.
 *
 * Layout:
 *   - Blue gradient header
 *   - Three trend cards (Hydration / pH / Specific Gravity)
 *   - "All Tests" list, newest first, with abnormal badges
 *
 * Charts handle three states:
 *   - 0 tests : "No history yet" placeholder
 *   - 1 test  : single dot, no line
 *   - 2+      : smooth Catmull-Rom curve through points
 */
@Composable
fun TestHistoryScreen(
    viewModel: HistoryViewModel,
    onBack: () -> Unit = {},
) {
    val tests by viewModel.tests.collectAsState()
    val days by viewModel.last7Days.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F4F7)),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            HistoryHeader(testCount = tests.size, onBack = onBack)
        }
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                TrendCard(
                    title = "Hydration Trend – Last 7 Days",
                    days = days,
                    valueSelector = { it.avgHydrationPercent },
                    yMin = 0f,
                    yMax = 100f,
                    yTicks = listOf(0f, 25f, 50f, 75f, 100f),
                    yFormat = { it.toInt().toString() },
                    accent = Color(0xFF1E88E5),
                    referenceLabel = null,
                )
                TrendCard(
                    title = "pH Level Trend – Last 7 Days",
                    days = days,
                    valueSelector = { it.avgPH },
                    yMin = 4f,
                    yMax = 9f,
                    yTicks = listOf(4f, 6f, 9f),
                    yFormat = { "%.0f".format(it) },
                    accent = Color(0xFFEF6C00),
                    referenceLabel =
                        "Reference Range: ${"%.1f".format(UrinalysisResult.PH_MIN)}" +
                                " – ${"%.1f".format(UrinalysisResult.PH_MAX)}",
                )
                TrendCard(
                    title = "Specific Gravity Trend – Last 7 Days",
                    days = days,
                    valueSelector = { it.avgSpecificGravity },
                    yMin = 1.000f,
                    yMax = 1.040f,
                    yTicks = listOf(1.009f, 1.018f, 1.035f),
                    yFormat = { "%.3f".format(it) },
                    accent = Color(0xFF06B6D4),
                    referenceLabel =
                        "Reference Range: ${"%.3f".format(UrinalysisResult.SG_MIN)}" +
                                " – ${"%.3f".format(UrinalysisResult.SG_MAX)}",
                )

                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("📅", fontSize = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "All Tests",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827),
                        modifier = Modifier.weight(1f),
                    )
                    if (tests.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearHistory() }) {
                            Text(
                                "Clear",
                                fontSize = 13.sp,
                                color = Color(0xFF6B7280),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }

        if (tests.isEmpty()) {
            item { EmptyHistoryCard() }
        } else {
            items(tests, key = { it.id }) { record ->
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
                    HistoryListItem(record = record)
                }
            }
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────
@Composable
private fun HistoryHeader(testCount: Int, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF1E88E5), Color(0xFF29B6F6))
                )
            )
            .padding(horizontal = 20.dp)
            .padding(top = 52.dp, bottom = 26.dp),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onBack() }
                    .padding(8.dp),
            ) {
                Text("←", color = Color.White, fontSize = 22.sp,
                    fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "Test History",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (testCount == 0) "No tests recorded yet"
                else "Track your hydration trends over time",
                color = Color.White.copy(alpha = 0.92f),
                fontSize = 14.sp
            )
        }
    }
}

// ── Trend card ────────────────────────────────────────────────────────────
@Composable
private fun TrendCard(
    title: String,
    days: List<DailyAggregate>,
    valueSelector: (DailyAggregate) -> Float,
    yMin: Float,
    yMax: Float,
    yTicks: List<Float>,
    yFormat: (Float) -> String,
    accent: Color,
    referenceLabel: String?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) { Text("📈", fontSize = 14.sp) }
                Spacer(Modifier.width(10.dp))
                Text(
                    title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
            }
            Spacer(Modifier.height(14.dp))

            if (days.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Need at least one test to display a trend.",
                        fontSize = 13.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }
            } else {
                LineChart(
                    days = days,
                    valueSelector = valueSelector,
                    yMin = yMin, yMax = yMax,
                    yTicks = yTicks, yFormat = yFormat,
                    accent = accent,
                )
            }

            if (referenceLabel != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    referenceLabel,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 11.sp,
                    color = Color(0xFF9CA3AF),
                    fontWeight = FontWeight.SemiBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

// ── Line chart (custom Canvas) ────────────────────────────────────────────
@Composable
private fun LineChart(
    days: List<DailyAggregate>,
    valueSelector: (DailyAggregate) -> Float,
    yMin: Float,
    yMax: Float,
    yTicks: List<Float>,
    yFormat: (Float) -> String,
    accent: Color,
) {
    val density = LocalDensity.current
    val labelTextSizePx = with(density) { 10.sp.toPx() }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawTrendChart(
                days = days,
                valueSelector = valueSelector,
                yMin = yMin, yMax = yMax,
                yTicks = yTicks, yFormat = yFormat,
                accent = accent,
                labelTextSizePx = labelTextSizePx,
            )
        }
    }
}

private fun DrawScope.drawTrendChart(
    days: List<DailyAggregate>,
    valueSelector: (DailyAggregate) -> Float,
    yMin: Float,
    yMax: Float,
    yTicks: List<Float>,
    yFormat: (Float) -> String,
    accent: Color,
    labelTextSizePx: Float,
) {
    // Reserve gutters for axis labels
    val leftGutter = labelTextSizePx * 4.5f
    val rightGutter = labelTextSizePx * 1.0f
    val topGutter = labelTextSizePx * 1.4f
    val bottomGutter = labelTextSizePx * 2.4f

    val plotLeft = leftGutter
    val plotRight = size.width - rightGutter
    val plotTop = topGutter
    val plotBottom = size.height - bottomGutter
    val plotWidth = plotRight - plotLeft
    val plotHeight = plotBottom - plotTop

    val gridColor = Color(0xFFE5E7EB)
    val labelColor = Color(0xFF9CA3AF)
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

    // ── Horizontal grid lines + Y labels ─────────────────────────────
    val yRange = (yMax - yMin).coerceAtLeast(1e-6f)
    fun yPx(v: Float) =
        plotTop + (1f - ((v - yMin) / yRange).coerceIn(0f, 1f)) * plotHeight

    yTicks.forEach { tick ->
        val y = yPx(tick)
        drawLine(
            color = gridColor,
            start = Offset(plotLeft, y),
            end = Offset(plotRight, y),
            strokeWidth = 1f,
            pathEffect = dashEffect,
        )
        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                color = labelColor.toArgb()
                textSize = labelTextSizePx
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.RIGHT
            }
            drawText(
                yFormat(tick),
                plotLeft - labelTextSizePx * 0.4f,
                y + labelTextSizePx * 0.35f,
                paint,
            )
        }
    }

    // ── Map data points to plot coordinates ──────────────────────────
    val n = days.size
    val xStep = if (n > 1) plotWidth / (n - 1) else 0f
    val points = days.mapIndexed { i, d ->
        val x = plotLeft + i * xStep + if (n == 1) plotWidth / 2f else 0f
        val y = yPx(valueSelector(d).coerceIn(yMin, yMax))
        Offset(x, y)
    }

    // ── Smooth Catmull-Rom curve (only if 2+ points) ─────────────────
    if (points.size >= 2) {
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 0 until points.size - 1) {
                val p0 = points.getOrElse(i - 1) { points[i] }
                val p1 = points[i]
                val p2 = points[i + 1]
                val p3 = points.getOrElse(i + 2) { points[i + 1] }
                // Catmull-Rom → cubic bezier conversion (tension 0.5)
                val c1x = p1.x + (p2.x - p0.x) / 6f
                val c1y = p1.y + (p2.y - p0.y) / 6f
                val c2x = p2.x - (p3.x - p1.x) / 6f
                val c2y = p2.y - (p3.y - p1.y) / 6f
                cubicTo(c1x, c1y, c2x, c2y, p2.x, p2.y)
            }
        }
        drawPath(
            path = path,
            color = accent,
            style = Stroke(width = 3.5f, cap = StrokeCap.Round),
        )
    }

    // ── Data point circles ───────────────────────────────────────────
    points.forEach { p ->
        drawCircle(color = Color.White, radius = 6f, center = p)
        drawCircle(color = accent, radius = 4.5f, center = p)
    }

    // ── X-axis day labels ────────────────────────────────────────────
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            color = labelColor.toArgb()
            textSize = labelTextSizePx
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
        days.forEachIndexed { i, d ->
            val x = points[i].x
            drawText(
                d.dayLabel,
                x,
                plotBottom + labelTextSizePx * 1.6f,
                paint,
            )
        }
    }
}

private fun Color.toArgb(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(),
    (red   * 255).toInt(),
    (green * 255).toInt(),
    (blue  * 255).toInt(),
)

// ── History list item ─────────────────────────────────────────────────────
@Composable
private fun HistoryListItem(record: TestRecord) {
    val severity = record.severity()
    val theme = severity.theme()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = theme.background),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, theme.border),
    ) {
        Box {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusIconBadge(theme = theme)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        formatDateTime(record.timestamp),
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        labelFor(record),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.titleColor,
                        lineHeight = 20.sp,
                    )
                    if (severity != Severity.NORMAL) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ParameterPill(
                                label = "pH",
                                value = "%.1f".format(record.pH),
                                inRange = record.pHInRange,
                                tooHigh = record.pH > UrinalysisResult.PH_MAX,
                            )
                            ParameterPill(
                                label = "SG",
                                value = "%.3f".format(record.specificGravity),
                                inRange = record.sgInRange,
                                tooHigh = record.specificGravity > UrinalysisResult.SG_MAX,
                            )
                        }
                    } else {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "pH: ${"%.1f".format(record.pH)}  |  " +
                                    "SG: ${"%.3f".format(record.specificGravity)}",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    "›",
                    fontSize = 22.sp,
                    color = Color(0xFFCBD5E1),
                    fontWeight = FontWeight.Bold
                )
            }

            // ABNORMAL badge top-right
            if (severity != Severity.NORMAL && severity != Severity.MILD) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 0.dp)
                        .clip(
                            RoundedCornerShape(
                                topStart = 0.dp, topEnd = 14.dp,
                                bottomStart = 8.dp, bottomEnd = 0.dp
                            )
                        )
                        .background(theme.badgeBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        "ABNORMAL",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusIconBadge(theme: ItemTheme) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(theme.iconBg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            theme.icon,
            color = theme.iconFg,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ParameterPill(
    label: String,
    value: String,
    inRange: Boolean,
    tooHigh: Boolean,
) {
    val arrow = when {
        inRange -> "✓"
        tooHigh -> "↑"
        else    -> "↓"
    }
    val bg: Color
    val fg: Color
    if (inRange) {
        bg = Color(0xFFD1FAE5); fg = Color(0xFF059669)
    } else {
        bg = Color(0xFFFEE2E2); fg = Color(0xFFB91C1C)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            "$label: $value$arrow",
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

// ── Empty state ───────────────────────────────────────────────────────────
@Composable
private fun EmptyHistoryCard() {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxWidth()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(0.dp),
            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0F2FE)),
                    contentAlignment = Alignment.Center
                ) { Text("📭", fontSize = 26.sp) }
                Spacer(Modifier.height(12.dp))
                Text(
                    "No tests recorded yet",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Run a urinalysis to start tracking your trends.",
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280),
                )
            }
        }
    }
}

// ── Severity classification + theming ─────────────────────────────────────

private enum class Severity { NORMAL, MILD, MEDIUM, HIGH }

private data class ItemTheme(
    val background: Color,
    val border: Color,
    val titleColor: Color,
    val iconBg: Color,
    val iconFg: Color,
    val icon: String,
    val badgeBg: Color,
)

private fun TestRecord.severity(): Severity {
    // Flags from the Fuzzy KNN are the strongest abnormal signal
    if (activeFlags.isNotEmpty()) return Severity.HIGH
    val outs = listOfNotNull(
        if (!pHInRange) "pH" else null,
        if (!sgInRange) "SG" else null,
    )
    if (outs.size >= 2) return Severity.HIGH
    if (outs.size == 1) return Severity.MEDIUM
    if (status == HydrationStatus.MILDLY_DEHYDRATED ||
        status == HydrationStatus.OVERHYDRATED) return Severity.MILD
    if (status == HydrationStatus.DEHYDRATED) return Severity.HIGH
    return Severity.NORMAL
}

private fun Severity.theme(): ItemTheme = when (this) {
    Severity.NORMAL -> ItemTheme(
        background = Color.White,
        border     = Color(0xFFE5E7EB),
        titleColor = Color(0xFF059669),
        iconBg     = Color(0xFFD1FAE5),
        iconFg     = Color(0xFF059669),
        icon       = "✓",
        badgeBg    = Color(0xFF059669),
    )
    Severity.MILD -> ItemTheme(
        background = Color.White,
        border     = Color(0xFFE5E7EB),
        titleColor = Color(0xFFD97706),
        iconBg     = Color(0xFFFEF3C7),
        iconFg     = Color(0xFFD97706),
        icon       = "⚠",
        badgeBg    = Color(0xFFF59E0B),
    )
    Severity.MEDIUM -> ItemTheme(
        background = Color(0xFFFFFBEB),
        border     = Color(0xFFFCD34D),
        titleColor = Color(0xFFD97706),
        iconBg     = Color(0xFFFEF3C7),
        iconFg     = Color(0xFFD97706),
        icon       = "⚠",
        badgeBg    = Color(0xFFF59E0B),
    )
    Severity.HIGH -> ItemTheme(
        background = Color(0xFFFEF2F2),
        border     = Color(0xFFFECACA),
        titleColor = Color(0xFFDC2626),
        iconBg     = Color(0xFFFEE2E2),
        iconFg     = Color(0xFFDC2626),
        icon       = "⚠",
        badgeBg    = Color(0xFFDC2626),
    )
}

private fun labelFor(record: TestRecord): String {
    if (record.activeFlags.isNotEmpty()) {
        // Prefer the descriptive flag names from the table
        val names = record.activeFlags.map { it.hydrationStatus }.distinct()
        return "Abnormal – ${names.joinToString(" + ")}"
    }
    val outs = buildList {
        if (!record.pHInRange) add("pH")
        if (!record.sgInRange) add("SG")
    }
    return when (outs.size) {
        0 -> record.status.label.titleCase()
        1 -> "Abnormal – ${outs.first()} Out of Range"
        else -> "Abnormal – pH & SG Out of Range"
    }
}

private fun String.titleCase(): String =
    split(' ').joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { it.uppercase() }
    }

private val DATE_FMT = SimpleDateFormat("MMM d, yyyy • hh:mm a", Locale.getDefault())
private fun formatDateTime(d: Date): String = DATE_FMT.format(d)
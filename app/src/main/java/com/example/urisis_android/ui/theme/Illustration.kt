package com.example.urisis_android.ui.illustrations

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Compose-drawn illustrations.
 */

// ─────────────────────────────────────────────────────────────
// 1. Water Drop Logo
// ─────────────────────────────────────────────────────────────

@Composable
fun WaterDropLogo(
    modifier: Modifier = Modifier,
    logoSize: Dp = 96.dp,
    onColored: Boolean = false,
) {

    val primary = MaterialTheme.colorScheme.primary
    val accent = MaterialTheme.colorScheme.tertiary

    val fillColor =
        if (onColored) Color.White else primary

    val highlightColor =
        if (onColored) {
            Color.White.copy(alpha = 0.35f)
        } else {
            accent.copy(alpha = 0.55f)
        }

    Box(
        modifier = modifier.size(logoSize),
        contentAlignment = Alignment.Center,
    ) {

        Canvas(
            modifier = Modifier.size(logoSize)
        ) {
            drawWaterDrop(fillColor, highlightColor)
        }
    }
}

private fun DrawScope.drawWaterDrop(
    fill: Color,
    highlight: Color,
) {

    val w = this.size.width
    val h = this.size.height

    val path = Path().apply {

        moveTo(w * 0.5f, h * 0.10f)

        cubicTo(
            w * 0.85f,
            h * 0.40f,

            w * 0.95f,
            h * 0.65f,

            w * 0.5f,
            h * 0.92f,
        )

        cubicTo(
            w * 0.05f,
            h * 0.65f,

            w * 0.15f,
            h * 0.40f,

            w * 0.5f,
            h * 0.10f,
        )

        close()
    }

    drawPath(
        path = path,
        color = fill,
    )

    drawOval(
        color = highlight,
        topLeft = Offset(
            w * 0.30f,
            h * 0.32f,
        ),
        size = Size(
            w * 0.22f,
            h * 0.30f,
        ),
    )
}

// ─────────────────────────────────────────────────────────────
// 2. Hydration Hero
// ─────────────────────────────────────────────────────────────

@Composable
fun HydrationHero(
    modifier: Modifier = Modifier,
    heroSize: Dp = 140.dp,
    onColored: Boolean = true,
) {

    val whiteSoft =
        Color.White.copy(alpha = 0.18f)

    val whiteSofter =
        Color.White.copy(alpha = 0.10f)

    val primary =
        MaterialTheme.colorScheme.primary

    val tertiary =
        MaterialTheme.colorScheme.tertiary

    val baseFill =
        if (onColored) Color.White else primary

    val mid =
        if (onColored) {
            Color.White.copy(alpha = 0.7f)
        } else {
            tertiary
        }

    val small =
        if (onColored) {
            Color.White.copy(alpha = 0.5f)
        } else {
            primary.copy(alpha = 0.7f)
        }

    Box(
        modifier = modifier.size(heroSize),
        contentAlignment = Alignment.Center,
    ) {

        Canvas(
            modifier = Modifier.size(heroSize)
        ) {

            val canvasW = this.size.width
            val canvasH = this.size.height

            val center = Offset(
                canvasW * 0.5f,
                canvasH * 0.5f,
            )

            val radius = canvasW * 0.46f

            drawCircle(
                color = whiteSofter,
                radius = radius * 1.08f,
                center = center,
            )

            drawCircle(
                color = whiteSoft,
                radius = radius * 0.92f,
                center = center,
            )

            val mainSize = canvasW * 0.55f

            val mainOffset = Offset(
                center.x - mainSize / 2f,
                center.y - mainSize / 2f - canvasH * 0.05f,
            )

            translate(mainOffset.x, mainOffset.y) {

                drawWaterDropLocal(
                    size = mainSize,
                    fill = baseFill,
                    highlight =
                        if (onColored) {
                            Color.White.copy(alpha = 0.30f)
                        } else {
                            tertiary.copy(alpha = 0.55f)
                        },
                )
            }

            val smallSize = canvasW * 0.22f

            val smallOffset = Offset(
                center.x + canvasW * 0.10f,
                center.y + canvasH * 0.15f,
            )

            translate(smallOffset.x, smallOffset.y) {

                drawWaterDropLocal(
                    size = smallSize,
                    fill = mid,
                    highlight = Color.Transparent,
                )
            }

            val tinySize = canvasW * 0.14f

            val tinyOffset = Offset(
                center.x - canvasW * 0.30f,
                center.y - canvasH * 0.20f,
            )

            translate(tinyOffset.x, tinyOffset.y) {

                drawWaterDropLocal(
                    size = tinySize,
                    fill = small,
                    highlight = Color.Transparent,
                )
            }
        }
    }
}

private inline fun DrawScope.translate(
    dx: Float,
    dy: Float,
    block: DrawScope.() -> Unit,
) {

    drawContext.transform.translate(dx, dy)

    block()

    drawContext.transform.translate(-dx, -dy)
}

private fun DrawScope.drawWaterDropLocal(
    size: Float,
    fill: Color,
    highlight: Color,
) {

    val path = Path().apply {

        moveTo(size * 0.5f, size * 0.10f)

        cubicTo(
            size * 0.85f,
            size * 0.40f,

            size * 0.95f,
            size * 0.65f,

            size * 0.5f,
            size * 0.92f,
        )

        cubicTo(
            size * 0.05f,
            size * 0.65f,

            size * 0.15f,
            size * 0.40f,

            size * 0.5f,
            size * 0.10f,
        )

        close()
    }

    drawPath(
        path = path,
        color = fill,
    )

    if (highlight != Color.Transparent) {

        drawOval(
            color = highlight,
            topLeft = Offset(
                size * 0.30f,
                size * 0.32f,
            ),
            size = Size(
                size * 0.22f,
                size * 0.30f,
            ),
        )
    }
}

// ─────────────────────────────────────────────────────────────
// 3. Avatar Helpers
// ─────────────────────────────────────────────────────────────

fun avatarColors(
    seed: String,
): Pair<Color, Color> {

    val palettes = listOf(

        Color(0xFF1E88E5) to Color.White,
        Color(0xFF00ACC1) to Color.White,
        Color(0xFF7C3AED) to Color.White,
        Color(0xFFD97706) to Color.White,
        Color(0xFF059669) to Color.White,
        Color(0xFFDB2777) to Color.White,
        Color(0xFF0891B2) to Color.White,
        Color(0xFFDC2626) to Color.White,
    )

    val index =
        (seed.hashCode().toUInt() %
                palettes.size.toUInt()).toInt()

    return palettes[index]
}

fun initialsFor(
    name: String,
    email: String,
): String {

    val source =
        name.trim().ifBlank {
            email.substringBefore("@").trim()
        }

    if (source.isBlank()) return "?"

    val parts = source
        .split(" ", "_", ".", "-")
        .filter { it.isNotBlank() }

    return when {

        parts.size >= 2 -> {
            "${parts[0].first()}${parts[1].first()}"
                .uppercase()
        }

        else -> {
            parts[0]
                .take(2)
                .uppercase()
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 4. Empty History Illustration
// ─────────────────────────────────────────────────────────────

@Composable
fun EmptyHistoryIllustration(
    modifier: Modifier = Modifier,
    illustrationSize: Dp = 120.dp,
) {

    val primary =
        MaterialTheme.colorScheme.primary

    val muted =
        MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = modifier.size(illustrationSize),
        contentAlignment = Alignment.Center,
    ) {

        Canvas(
            modifier = Modifier.size(illustrationSize)
        ) {

            val w = this.size.width
            val h = this.size.height

            drawCircle(
                color = muted,
                radius = w * 0.48f,
                center = Offset(
                    w * 0.5f,
                    h * 0.5f,
                ),
                style = Stroke(
                    width = 4f,
                    pathEffect =
                        PathEffect.dashPathEffect(
                            floatArrayOf(8f, 8f)
                        ),
                ),
            )

            val points = listOf(
                Offset(w * 0.30f, h * 0.60f),
                Offset(w * 0.42f, h * 0.45f),
                Offset(w * 0.55f, h * 0.55f),
                Offset(w * 0.70f, h * 0.40f),
            )

            for (i in 0 until points.size - 1) {

                drawLine(
                    color = primary,
                    start = points[i],
                    end = points[i + 1],
                    strokeWidth = 4f,
                )
            }

            points.forEach { point ->

                drawCircle(
                    color = primary,
                    radius = 5f,
                    center = point,
                )
            }
        }
    }
}
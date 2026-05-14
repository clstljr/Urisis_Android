package com.example.urisis_android.ui.theme

import androidx.compose.ui.graphics.Color

// ── Brand seeds — derived from the existing blue gradient ──────────────
val BrandPrimary       = Color(0xFF1565C0)   // deep blue
val BrandPrimaryAccent = Color(0xFF29B6F6)   // cyan-blue (gradient end)
val BrandSecondary     = Color(0xFF00ACC1)   // teal

// Dark-mode variants — lighter, less saturated so they read on dark
val BrandPrimaryDark       = Color(0xFF60A5FA)   // soft sky blue
val BrandPrimaryAccentDark = Color(0xFF7DD3FC)
val BrandSecondaryDark     = Color(0xFF22D3EE)

// ── Hydration status colours (status-tier palette) ─────────────────────
val HydrationGreen  = Color(0xFF2E7D32)
val HydrationBlue   = Color(0xFF1565C0)
val HydrationAmber  = Color(0xFFEF6C00)
val HydrationRed    = Color(0xFFC62828)

// ── Surface tints used by dashboard cards ──────────────────────────────
val TintHydration = Color(0xFFE3F2FD)
val TintPh        = Color(0xFFE8F5E9)
val TintTds       = Color(0xFFFFF3E0)
val TintWarning   = Color(0xFFFFF8E1)

// ── Legacy (kept for any holdouts) ─────────────────────────────────────
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
val DarkBlue = Color(0xFF1E3A8A)
val Blue = Color(0xFF1565C0)
val Gray = Color(0xFF757575)
val Green = Color(0xFF2E7D32)
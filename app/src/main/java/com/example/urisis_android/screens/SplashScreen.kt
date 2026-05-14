package com.example.urisis_android.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.urisis_android.ui.illustrations.WaterDropLogo
import com.example.urisis_android.ui.theme.brandBrush
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Splash screen.
 *
 * Two-stage exit condition:
 *   1. Animation must have run for at least [MIN_DISPLAY_MS]
 *   2. Auth must have finished its initial DataStore restore
 *
 * Both must be true before [onFinished] fires. This prevents the splash
 * from completing before auto-login has decided whether the user is
 * already signed in.
 *
 * @param isAuthInitialized true once AuthViewModel.init has resolved the
 *                          stored active account (if any). When false,
 *                          the splash holds even past its animation.
 */
@Composable
fun SplashScreen(
    isAuthInitialized: Boolean,
    onFinished: () -> Unit,
) {
    val dark = isSystemInDarkTheme()

    val logoScale       = remember { Animatable(0f) }
    val wordmarkOffsetY = remember { Animatable(40f) }
    val wordmarkAlpha   = remember { Animatable(0f) }
    val taglineAlpha    = remember { Animatable(0f) }
    val progressAlpha   = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch {
                delay(150)
                logoScale.animateTo(1f, tween(700, easing = EaseOutBack))
            }
            launch {
                delay(550)
                wordmarkAlpha.animateTo(1f, tween(500))
            }
            launch {
                delay(550)
                wordmarkOffsetY.animateTo(0f, tween(600, easing = EaseOutBack))
            }
            launch {
                delay(900)
                taglineAlpha.animateTo(1f, tween(500))
            }
            launch {
                delay(1300)
                progressAlpha.animateTo(1f, tween(400))
            }
        }
    }

    // Wait for both animation minimum and auth init before exiting
    LaunchedEffect(isAuthInitialized) {
        delay(MIN_DISPLAY_MS)
        if (isAuthInitialized) {
            onFinished()
        } else {
            // Animation has played but auth still loading — wait then re-check.
            // The composition will re-run this LaunchedEffect when
            // isAuthInitialized flips to true.
        }
    }
    LaunchedEffect(isAuthInitialized) {
        if (isAuthInitialized) {
            // If auth finishes after MIN_DISPLAY_MS has already elapsed,
            // exit immediately. Otherwise the other LaunchedEffect handles it.
            delay(MIN_DISPLAY_MS)
            onFinished()
        }
    }

    val infinite = rememberInfiniteTransition(label = "splash-pulse")
    val pulse by infinite.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "splash-pulse-alpha",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brandBrush(dark)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
        ) {
            // Logo bubble + halo
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .scale(1f + (pulse - 0.5f) * 0.15f)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.10f))
                )
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .graphicsLayer {
                            scaleX = logoScale.value
                            scaleY = logoScale.value
                        }
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    WaterDropLogo(logoSize = 78.dp, onColored = true)
                }
            }

            Spacer(Modifier.height(36.dp))

            Column(
                modifier = Modifier.graphicsLayer {
                    translationY = wordmarkOffsetY.value
                    alpha = wordmarkAlpha.value
                },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Urisis",
                    color = Color.White,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp)
            }

            Spacer(Modifier.height(14.dp))

            Text(
                "Smart Hydration Screening",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { alpha = taglineAlpha.value },
            )

            Spacer(Modifier.height(72.dp))

            Box(modifier = Modifier.graphicsLayer { alpha = progressAlpha.value }) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(36.dp),
                )
            }
        }

        Text(
            "by Silver Swan",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .graphicsLayer { alpha = taglineAlpha.value },
        )
    }
}

private const val MIN_DISPLAY_MS = 1800L
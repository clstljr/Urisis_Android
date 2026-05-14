package com.example.urisis_android.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.urisis_android.auth.AuthViewModel
import com.example.urisis_android.ui.illustrations.avatarColors
import com.example.urisis_android.ui.illustrations.initialsFor

/**
 * Bottom sheet for switching, adding, or signing out accounts from the
 * dashboard. Matches the in-app account-switcher patterns used by
 * Gmail / Google apps.
 *
 * Layout:
 *   - Active account at top with checkmark
 *   - Divider
 *   - Other stored accounts (tap to switch)
 *   - Divider
 *   - "Add another account" — routes to login
 *   - "Sign out" — clears active pointer (account stays on device)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSwitcherSheet(
    authViewModel: AuthViewModel,
    onDismiss: () -> Unit,
    onSwitched: () -> Unit,
    onAddAccount: () -> Unit,
    onSignOut: () -> Unit,
) {
    val accounts by authViewModel.storedAccounts.collectAsState()
    val authState by authViewModel.ui.collectAsState()
    val active = authState.currentUser

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)) {

            Text(
                "Accounts",
                modifier = Modifier.padding(start = 24.dp, top = 6.dp, bottom = 14.dp),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Active account
            active?.let {
                AccountRow(
                    name = it.fullName,
                    email = it.email,
                    isActive = true,
                    onClick = { /* already active, no-op */ },
                )
                Divider()
            }

            // Other accounts
            accounts
                .filter { it.email != active?.email }
                .forEach { acc ->
                    AccountRow(
                        name = acc.name,
                        email = acc.email,
                        isActive = false,
                        onClick = {
                            authViewModel.switchAccount(acc.email)
                            onSwitched()
                        },
                    )
                }

            if (accounts.any { it.email != active?.email }) Divider()

            // Add account
            ActionRow(
                icon = Icons.Filled.Add,
                label = "Add another account",
                onClick = onAddAccount,
            )

            // Sign out (active only — keeps the account on device for one-tap return)
            if (active != null) {
                ActionRow(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    label = "Sign out",
                    onClick = onSignOut,
                    destructive = true,
                )
            }
        }
    }
}

@Composable
private fun AccountRow(
    name: String,
    email: String,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val (avBg, avFg) = avatarColors(email)
    val initials = initialsFor(name, email)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isActive) { onClick() }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(avBg),
            contentAlignment = Alignment.Center,
        ) {
            Text(initials, color = avFg, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                name.ifBlank { email.substringBefore("@") },
                fontSize = 15.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(email,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (isActive) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Active account",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    destructive: Boolean = false,
) {
    val color =
        if (destructive) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(18.dp))
        Text(label,
            fontSize = 14.sp,
            color = color,
            fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}
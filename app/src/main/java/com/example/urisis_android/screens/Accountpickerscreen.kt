package com.example.urisis_android.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.urisis_android.auth.AccountStore
import com.example.urisis_android.auth.AuthViewModel
import com.example.urisis_android.ui.illustrations.WaterDropLogo
import com.example.urisis_android.ui.illustrations.avatarColors
import com.example.urisis_android.ui.illustrations.initialsFor
import com.example.urisis_android.ui.theme.brandBrush

/**
 * Account picker shown when the device has 2+ stored accounts.
 *
 * Best-practice patterns adopted (Gmail / Outlook / Slack 2026):
 *   - Hero header with brand identity, not a generic "Choose account" bar
 *   - Each account = a tappable card with coloured avatar + name + email
 *   - Per-account overflow menu with "Remove from this device"
 *   - Persistent "Add another account" tile at the bottom of the list
 *   - One tap = switch + navigate; no extra confirmations
 *
 * Use cases:
 *   - App launch when more than one account exists
 *   - Triggered explicitly from the dashboard's account-switcher sheet
 */
@Composable
fun AccountPickerScreen(
    authViewModel: AuthViewModel,
    onAccountChosen: () -> Unit,
    onAddAccount: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    val accounts by authViewModel.storedAccounts.collectAsState()
    val dark = isSystemInDarkTheme()

    var pendingRemoval by remember { mutableStateOf<AccountStore.StoredAccount?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 28.dp),
        ) {
            item {
                // Hero header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(brandBrush(dark))
                        .padding(horizontal = 24.dp)
                        .padding(top = 56.dp, bottom = 40.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            WaterDropLogo(logoSize = 40.dp, onColored = true)
                        }
                        Spacer(Modifier.height(14.dp))
                        Text("Choose account",
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Tap an account to continue",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 14.sp,
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            items(accounts, key = { it.email }) { account ->
                AccountTile(
                    account = account,
                    onTap = {
                        authViewModel.switchAccount(account.email)
                        onAccountChosen()
                    },
                    onRemoveRequested = { pendingRemoval = account },
                )
                Spacer(Modifier.height(10.dp))
            }

            item {
                Spacer(Modifier.height(6.dp))
                AddAccountTile(onClick = onAddAccount)
            }

            if (onBack != null) {
                item {
                    Spacer(Modifier.height(20.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        TextButton(onClick = onBack) {
                            Text("Cancel",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    pendingRemoval?.let { acc ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text("Remove account?") },
            text = {
                Text(
                    "${acc.name.ifBlank { acc.email }} will be removed from " +
                            "this device. Test history stays in the cloud, but you'll " +
                            "need to sign in again to access it here."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    authViewModel.removeAccount(acc.email)
                    pendingRemoval = null
                }) {
                    Text("Remove",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoval = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun AccountTile(
    account: AccountStore.StoredAccount,
    onTap: () -> Unit,
    onRemoveRequested: () -> Unit,
) {
    val (avBg, avFg) = remember(account.email) { avatarColors(account.email) }
    val initials = remember(account.name, account.email) {
        initialsFor(account.name, account.email)
    }
    var menuOpen by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onTap() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(avBg),
                contentAlignment = Alignment.Center,
            ) {
                Text(initials,
                    color = avFg,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    account.name.ifBlank { account.email.substringBefore("@") },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    account.email,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Account options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Remove from this device",
                            color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menuOpen = false
                            onRemoveRequested()
                        },
                    )
                }
            }
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun AddAccountTile(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Add another account",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "Sign in or create a new account",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
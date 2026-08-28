package com.example.moneyminder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.moneyminder.theme.CardBackground
import com.example.moneyminder.theme.CardBackgroundElevated
import com.example.moneyminder.theme.CardBorder
import com.example.moneyminder.theme.CardBorderSubtle
import com.example.moneyminder.theme.ExpenseCardBg
import com.example.moneyminder.theme.ExpenseRed
import com.example.moneyminder.theme.TextDisabled
import com.example.moneyminder.theme.TextMuted
import com.example.moneyminder.theme.TextPrimary
import com.example.moneyminder.theme.TextSecondary
import com.example.moneyminder.theme.TransferBlueGrey
import com.example.moneyminder.ui.viewmodel.MainViewModel

import com.example.moneyminder.theme.BankAccent
import com.example.moneyminder.theme.IncomeGreen
import com.example.moneyminder.theme.WalletAccent

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    backupConnectedEmail: String = "",
    backupLastBackupAt: Long = 0L,
    backupIsAutoEnabled: Boolean = false,
    onExportExcel: () -> Unit,
    onExportPdf: () -> Unit,
    onOpenBackupSync: () -> Unit,
    onDismiss: () -> Unit
) {
    var showDeleteAllConfirm by remember { mutableStateOf(false) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(24.dp),
            color = CardBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Settings & Privacy",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CardBackgroundElevated)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Preferences Section
                    item {
                        SettingsSectionHeader("PREFERENCES")
                        Spacer(modifier = Modifier.height(8.dp))
                        SettingsItemCard(
                            icon = Icons.Default.AttachMoney,
                            title = "Primary Currency",
                            subtitle = "₹ INR (Indian Rupee)",
                            badge = "Default"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SettingsItemCard(
                            icon = Icons.Default.CalendarMonth,
                            title = "First Day of Month",
                            subtitle = "1st of every month",
                            badge = "Standard"
                        )
                    }

                    // Export Reports Section
                    item {
                        SettingsSectionHeader("DATA EXPORT")
                        Spacer(modifier = Modifier.height(8.dp))
                        SettingsActionCard(
                            icon = Icons.Default.TableChart,
                            title = "Export to Excel (.xlsx)",
                            subtitle = "Full transaction statement with balance summaries",
                            onClick = onExportExcel
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SettingsActionCard(
                            icon = Icons.Default.PictureAsPdf,
                            title = "Export to PDF Document",
                            subtitle = "Formatted financial report with tables & categories",
                            onClick = onExportPdf
                        )
                    }

                    // Privacy & Security Section
                    item {
                        SettingsSectionHeader("PRIVACY & DATA STORAGE")
                        Spacer(modifier = Modifier.height(8.dp))
                        SettingsActionCard(
                            icon = Icons.Default.Shield,
                            title = "Privacy Policy",
                            subtitle = "100% offline, no tracking, zero cloud data transfer",
                            onClick = { showPrivacyPolicy = true }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SettingsActionCard(
                            icon = Icons.Default.DeleteForever,
                            title = "Delete All Local Data",
                            subtitle = "Wipe all transactions, accounts, and categories",
                            textColor = ExpenseRed,
                            iconColor = ExpenseRed,
                            onClick = { showDeleteAllConfirm = true }
                        )
                    }

                    // Backup & Sync Section
                    item {
                        SettingsSectionHeader("BACKUP & SYNC")
                        Spacer(modifier = Modifier.height(8.dp))
                        // Status summary card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(CardBackgroundElevated)
                                .border(1.dp, if (backupConnectedEmail.isNotBlank()) Color(0xFF1E4A2A) else CardBorderSubtle, RoundedCornerShape(16.dp))
                                .clickable { onOpenBackupSync() }
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size(36.dp).clip(CircleShape)
                                            .background(if (backupConnectedEmail.isNotBlank()) Color(0xFF0D2211) else Color(0xFF1C1C26)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Backup,
                                            contentDescription = null,
                                            tint = if (backupConnectedEmail.isNotBlank()) IncomeGreen else TextMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Backup & Sync",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 14.sp
                                            )
                                        )
                                        Text(
                                            text = if (backupConnectedEmail.isNotBlank())
                                                backupConnectedEmail
                                            else
                                                "No account connected",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = if (backupConnectedEmail.isNotBlank()) IncomeGreen else TextMuted,
                                                fontSize = 11.5.sp
                                            )
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                        .background(if (backupConnectedEmail.isNotBlank()) Color(0xFF1A3A1A) else Color(0xFF1E1E28))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (backupIsAutoEnabled) "AUTO ON" else if (backupConnectedEmail.isNotBlank()) "Manual" else "Off",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (backupIsAutoEnabled) IncomeGreen else TextMuted,
                                            fontSize = 10.sp, fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Future Upgrades Section (Placeholders)
                    item {
                        SettingsSectionHeader("FUTURE EXTENSIONS")
                        Spacer(modifier = Modifier.height(8.dp))
                        SettingsItemCard(
                            icon = Icons.Default.Fingerprint,
                            title = "Biometric App Lock",
                            subtitle = "Fingerprint / Face unlock security",
                            badge = "Coming Soon"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SettingsItemCard(
                            icon = Icons.Default.ReceiptLong,
                            title = "Monthly Budgets & Limits",
                            subtitle = "Set category limits with alerts",
                            badge = "Coming Soon"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SettingsItemCard(
                            icon = Icons.Default.Repeat,
                            title = "Recurring Transactions",
                            subtitle = "Auto-repeat subscriptions and salaries",
                            badge = "Coming Soon"
                        )
                    }

                }
            }
        }
    }

    // Privacy Policy Dialog
    if (showPrivacyPolicy) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicy = false },
            title = { Text("Money Minder Privacy Promise", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Text(
                    text = "• Offline-First: Money Minder does not upload your data to any remote server or cloud service.\n\n" +
                           "• Local Storage: All financial logs, categories, and account balances reside exclusively inside your device's private SQLite database.\n\n" +
                           "• No Login Required: No email, phone number, password, or third-party registration is required.\n\n" +
                           "• Your Control: You can export your data to Excel or PDF or wipe everything at any moment.",
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showPrivacyPolicy = false },
                    colors = ButtonDefaults.buttonColors(containerColor = TextPrimary, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Understood", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = CardBackground,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Factory Reset Confirmation Dialog
    if (showDeleteAllConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteAllConfirm = false },
            title = { Text("Delete All Data?", fontWeight = FontWeight.Bold, color = ExpenseRed) },
            text = {
                Text(
                    text = "This will permanently remove all your transactions, custom categories, and reset Bank, Wallet, and Cash balances to ₹0. This action cannot be undone.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAllConfirm = false
                        viewModel.resetAllData()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete Everything", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteAllConfirm = false },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) {
                    Text("Cancel")
                }
            },
            containerColor = CardBackground,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            color = TextMuted,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    )
}

@Composable
private fun SettingsItemCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badge: String? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackgroundElevated)
            .border(1.dp, CardBorderSubtle, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF262632)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            fontSize = 14.sp
                        )
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 11.5.sp
                        )
                    )
                }
            }

            if (badge != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E1E28))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    textColor: Color = TextPrimary,
    iconColor: Color = TextPrimary,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackgroundElevated)
            .border(1.dp, CardBorderSubtle, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF262632)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = textColor,
                        fontSize = 14.sp
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 11.5.sp
                    )
                )
            }
        }
    }
}

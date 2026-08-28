package com.example.moneyminder.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.moneyminder.data.model.BackupFrequency
import com.example.moneyminder.data.model.BackupOperationStatus
import com.example.moneyminder.data.model.BackupRestoreMode
import com.example.moneyminder.theme.BankAccent
import com.example.moneyminder.theme.CardBackground
import com.example.moneyminder.theme.CardBackgroundElevated
import com.example.moneyminder.theme.CardBorder
import com.example.moneyminder.theme.CardBorderSubtle
import com.example.moneyminder.theme.ExpenseRed
import com.example.moneyminder.theme.IncomeGreen
import com.example.moneyminder.theme.TextDisabled
import com.example.moneyminder.theme.TextMuted
import com.example.moneyminder.theme.TextPrimary
import com.example.moneyminder.theme.TextSecondary
import com.example.moneyminder.theme.TransferBlueGrey
import com.example.moneyminder.ui.viewmodel.BackupViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupSyncScreen(
    viewModel: BackupViewModel,
    onViewHistory: () -> Unit,
    onRestoreBackup: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val status by viewModel.backupStatus.collectAsState()
    var showDirectEmailDialog by remember { mutableStateOf(false) }
    var showDisconnectConfirm by remember { mutableStateOf(false) }
    var showDeleteAllConfirm by remember { mutableStateOf(false) }
    var showFrequencyPicker by remember { mutableStateOf(false) }
    var showRestoreModeDialog by remember { mutableStateOf<Uri?>(null) }

    // File picker launcher for restoring from local / downloaded backup file
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            showRestoreModeDialog = uri
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp),
            color = CardBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Header ───────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp, 20.dp, 20.dp, 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Backup & Sync",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold, color = TextPrimary
                            )
                        )
                        Text(
                            "Private email backup & restoration",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(CardBackgroundElevated)
                    ) {
                        Icon(Icons.Default.Close, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                // ── Progress overlay ─────────────────────────────────────────
                AnimatedVisibility(
                    visible = status.operationStatus == BackupOperationStatus.IN_PROGRESS,
                    enter = fadeIn(), exit = fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0D1117))
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = BankAccent
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                status.operationMessage,
                                style = MaterialTheme.typography.bodySmall.copy(color = BankAccent)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { status.operationProgress },
                            modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                            color = BankAccent,
                            trackColor = CardBorder,
                            strokeCap = StrokeCap.Round
                        )
                    }
                }

                // ── Success/Fail banner ───────────────────────────────────────
                AnimatedVisibility(
                    visible = status.operationStatus == BackupOperationStatus.SUCCESS ||
                              status.operationStatus == BackupOperationStatus.FAILED,
                    enter = fadeIn(), exit = fadeOut()
                ) {
                    val isSuccess = status.operationStatus == BackupOperationStatus.SUCCESS
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isSuccess) Color(0xFF0D2211) else Color(0xFF200D0D))
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                            .clickable { viewModel.clearOperationStatus() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                if (isSuccess) Icons.Default.Check else Icons.Default.CloudOff,
                                null,
                                tint = if (isSuccess) IncomeGreen else ExpenseRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                status.operationMessage,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isSuccess) IncomeGreen else ExpenseRed
                                )
                            )
                        }
                        Text("✕", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
                    }
                }

                // ── Scrollable content ────────────────────────────────────────
                LazyColumn(
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Privacy notice
                    item {
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0C1826))
                                .border(1.dp, Color(0xFF1A3048), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                "🔒  Backups are saved directly in your personal email inbox. Money Minder uses zero external cloud servers.",
                                style = MaterialTheme.typography.bodySmall.copy(color = BankAccent, fontSize = 11.sp),
                                lineHeight = 16.sp
                            )
                        }
                    }

                    // ── 1-TAP INSTANT BACKUP (Zero-Setup / Zero Errors) ───────
                    item {
                        BackupSectionHeader("INSTANT EMAIL BACKUP")
                        Spacer(Modifier.height(6.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF0E2316))
                                .border(1.dp, Color(0xFF1C502E), RoundedCornerShape(14.dp))
                                .clickable {
                                    viewModel.shareBackupViaEmail(context)
                                }
                                .padding(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BackupIconCircle(Icons.Default.Send, IncomeGreen)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "Backup & Send to Gmail",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold, color = IncomeGreen, fontSize = 14.sp
                                        )
                                    )
                                    Text(
                                        "1-Tap: Attaches backup directly in your Gmail app to email to yourself. 100% working, no setup needed.",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFF81C784), fontSize = 11.sp
                                        ),
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }

                    // ── BACKUP ACCOUNT section ────────────────────────────────
                    item {
                        BackupSectionHeader("AUTOMATED BACKGROUND BACKUP")
                        Spacer(Modifier.height(6.dp))

                        if (status.isConnected) {
                            // Connected state card
                            BackupInfoCard(
                                icon = Icons.Default.Email,
                                iconTint = IncomeGreen,
                                title = status.connectedEmail,
                                subtitle = "Connected for automatic scheduled backups"
                            )
                        } else {
                            BackupActionCard(
                                icon = Icons.Default.Email,
                                iconTint = BankAccent,
                                title = "Connect Email for Background Sync",
                                subtitle = "Connect your email address to enable automatic daily backups in background",
                                onClick = { showDirectEmailDialog = true }
                            )
                        }
                    }

                    // ── AUTOMATIC BACKUP section ──────────────────────────────
                    item {
                        BackupSectionHeader("AUTOMATIC BACKUP SCHEDULE")
                        Spacer(Modifier.height(6.dp))

                        // Auto backup toggle
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(CardBackgroundElevated)
                                .border(1.dp, CardBorderSubtle, RoundedCornerShape(14.dp))
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    BackupIconCircle(Icons.Default.CloudSync, if (status.isAutoEnabled) IncomeGreen else TextMuted)
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text("Automatic Backup",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 14.sp
                                            )
                                        )
                                        Text(
                                            if (status.isAutoEnabled) "Enabled — ${status.frequency.label}" else "Disabled",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = if (status.isAutoEnabled) IncomeGreen else TextMuted, fontSize = 11.sp
                                            )
                                        )
                                    }
                                }
                                Switch(
                                    checked = status.isAutoEnabled,
                                    onCheckedChange = {
                                        if (!status.isConnected) {
                                            showDirectEmailDialog = true
                                            return@Switch
                                        }
                                        viewModel.setAutoBackupEnabled(it)
                                    },
                                    enabled = status.isConnected,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = IncomeGreen,
                                        uncheckedThumbColor = TextMuted,
                                        uncheckedTrackColor = CardBorder
                                    )
                                )
                            }
                        }

                        // Frequency picker
                        if (status.isAutoEnabled) {
                            Spacer(Modifier.height(8.dp))
                            BackupActionCard(
                                icon = Icons.Default.Schedule,
                                iconTint = TransferBlueGrey,
                                title = "Backup Frequency",
                                subtitle = status.frequency.label,
                                onClick = { showFrequencyPicker = true }
                            )
                        }
                    }

                    // ── BACKUP STATUS section ─────────────────────────────────
                    item {
                        BackupSectionHeader("BACKUP STATUS")
                        Spacer(Modifier.height(6.dp))

                        val hasLastBackup = status.lastBackupAt > 0L
                        val lastBackupText = if (hasLastBackup) {
                            SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                                .format(Date(status.lastBackupAt))
                        } else "Never backed up"

                        BackupInfoCard(
                            icon = Icons.Default.Backup,
                            iconTint = if (hasLastBackup) TransferBlueGrey else TextDisabled,
                            title = "Last Backup",
                            subtitle = buildString {
                                append(lastBackupText)
                                if (hasLastBackup && status.lastBackupSizeBytes > 0) {
                                    append("  ·  ${status.lastBackupSizeBytes / 1024} KB")
                                }
                                if (status.lastBackupId.isNotBlank()) {
                                    append("\nID: ${status.lastBackupId.take(8)}…")
                                }
                            }
                        )
                    }

                    // ── PRIMARY ACTIONS ───────────────────────────────────────
                    item {
                        BackupSectionHeader("RESTORE & HISTORY")
                        Spacer(Modifier.height(6.dp))

                        // Restore from File / Email Download
                        BackupActionCard(
                            icon = Icons.Default.FolderOpen,
                            iconTint = BankAccent,
                            title = "Restore from Backup File",
                            subtitle = "Pick .mmbackup file from your Downloads or Gmail attachment",
                            onClick = {
                                filePickerLauncher.launch("*/*")
                            }
                        )

                        Spacer(Modifier.height(8.dp))
                        BackupActionCard(
                            icon = Icons.Default.History,
                            iconTint = TransferBlueGrey,
                            title = "View Email Backup History",
                            subtitle = "Search and restore backups directly from connected inbox",
                            enabled = status.isConnected,
                            onClick = {
                                viewModel.loadBackupHistory()
                                onViewHistory()
                            }
                        )

                        Spacer(Modifier.height(8.dp))
                        BackupActionCard(
                            icon = Icons.Default.Restore,
                            iconTint = IncomeGreen,
                            title = "Restore Assistant",
                            subtitle = "Restore data from a previous email backup",
                            enabled = status.isConnected,
                            onClick = {
                                viewModel.loadBackupHistory()
                                onRestoreBackup()
                            }
                        )
                    }

                    // ── ACCOUNT MANAGEMENT ────────────────────────────────────
                    item {
                        if (status.isConnected) {
                            BackupSectionHeader("ACCOUNT")
                            Spacer(Modifier.height(6.dp))
                            BackupActionCard(
                                icon = Icons.Default.LinkOff,
                                iconTint = ExpenseRed,
                                title = "Disconnect Account",
                                subtitle = "Stop background sync. Local data is not affected.",
                                textColor = ExpenseRed,
                                onClick = { showDisconnectConfirm = true }
                            )
                            Spacer(Modifier.height(8.dp))
                            BackupActionCard(
                                icon = Icons.Default.Delete,
                                iconTint = ExpenseRed,
                                title = "Delete Cloud Backups",
                                subtitle = "Permanently remove all Money Minder backup emails from inbox",
                                textColor = ExpenseRed,
                                onClick = { showDeleteAllConfirm = true }
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    // ── Dialog: Connect Email & Password ──────────────────────────────────────
    if (showDirectEmailDialog) {
        var emailInput by remember { mutableStateOf(viewModel.prefs.connectedEmail.ifBlank { "" }) }
        var passwordInput by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showDirectEmailDialog = false },
            title = {
                Text("Connect Email", fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Set your email for private backups. Leave password blank for 1-Tap Gmail mode (no password needed!):",
                        color = TextSecondary, fontSize = 12.sp
                    )

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Email Address") },
                        placeholder = { Text("akilask1997@gmail.com") },
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = BankAccent, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BankAccent,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("App Password (Optional)") },
                        placeholder = { Text("Optional — for background auto-sync") },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = IncomeGreen, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    null, tint = TextMuted, modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IncomeGreen,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        "💡 Tip: If you leave password blank, you can use the green 1-Tap 'Backup & Send to Gmail' button anytime with zero passwords!",
                        color = BankAccent, fontSize = 11.sp, lineHeight = 15.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.connectDirectEmail(
                            email = emailInput,
                            password = passwordInput,
                            onSuccess = { showDirectEmailDialog = false }
                        )
                    },
                    enabled = emailInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen)
                ) {
                    Text(if (passwordInput.isBlank()) "Save Email" else "Connect", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDirectEmailDialog = false }) { Text("Cancel") }
            },
            containerColor = CardBackground,
            shape = RoundedCornerShape(20.dp)
        )
    }


    // ── Dialog: Select Restore Mode for File Picker ────────────────────────────
    showRestoreModeDialog?.let { uri ->
        AlertDialog(
            onDismissRequest = { showRestoreModeDialog = null },
            title = { Text("Restore Data", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Text(
                    "How would you like to restore this backup file?\n\n• MERGE: Adds new transactions and keeps your current data intact.\n\n• REPLACE: Replaces current data with the backup contents.",
                    color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreModeDialog = null
                        viewModel.restoreFromFileUri(context, uri, BackupRestoreMode.MERGE)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen)
                ) {
                    Text("Merge (Keep Both)", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showRestoreModeDialog = null
                        viewModel.restoreFromFileUri(context, uri, BackupRestoreMode.REPLACE)
                    }
                ) {
                    Text("Replace All")
                }
            },
            containerColor = CardBackground,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // ── Frequency picker dialog ───────────────────────────────────────────────
    if (showFrequencyPicker) {
        AlertDialog(
            onDismissRequest = { showFrequencyPicker = false },
            title = { Text("Backup Frequency", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BackupFrequency.values().forEach { freq ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (status.frequency == freq) Color(0xFF1A2E1A) else CardBackgroundElevated)
                                .border(1.dp, if (status.frequency == freq) IncomeGreen else CardBorderSubtle, RoundedCornerShape(10.dp))
                                .clickable {
                                    viewModel.setBackupFrequency(freq)
                                    showFrequencyPicker = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(freq.label, color = if (status.frequency == freq) IncomeGreen else TextPrimary,
                                fontWeight = if (status.frequency == freq) FontWeight.Bold else FontWeight.Normal)
                            if (status.frequency == freq) {
                                Icon(Icons.Default.Check, null, tint = IncomeGreen, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                OutlinedButton(onClick = { showFrequencyPicker = false }) { Text("Cancel") }
            },
            containerColor = CardBackground,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // ── Disconnect confirmation ───────────────────────────────────────────────
    if (showDisconnectConfirm) {
        AlertDialog(
            onDismissRequest = { showDisconnectConfirm = false },
            title = { Text("Disconnect Account?", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("Automatic backup will stop. Your local Money Minder data will not be affected. Your existing backups in your email will remain intact.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showDisconnectConfirm = false
                        viewModel.disconnectAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) { Text("Disconnect", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDisconnectConfirm = false }) { Text("Cancel") }
            },
            containerColor = CardBackground,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // ── Delete all backups confirmation ───────────────────────────────────────
    if (showDeleteAllConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteAllConfirm = false },
            title = { Text("Delete All Cloud Backups?", fontWeight = FontWeight.Bold, color = ExpenseRed) },
            text = { Text("This will permanently delete all Money Minder backup emails from your email inbox. Your local app data will not be affected. This action cannot be undone.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAllConfirm = false
                        val history = viewModel.backupHistory.value
                        history.forEach { meta -> viewModel.deleteBackup(meta) }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) { Text("Delete All", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteAllConfirm = false }) { Text("Cancel") }
            },
            containerColor = CardBackground,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

// ── Shared sub-components ─────────────────────────────────────────────────────

@Composable
fun BackupSectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelSmall.copy(
            color = TextMuted, fontWeight = FontWeight.Bold, letterSpacing = 1.sp
        )
    )
}

@Composable
fun BackupIconCircle(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF1C1C26)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun BackupInfoCard(icon: ImageVector, iconTint: Color, title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBackgroundElevated)
            .border(1.dp, CardBorderSubtle, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            BackupIconCircle(icon, iconTint)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 14.sp
                    )
                )
                Text(subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun BackupActionCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    textColor: Color = TextPrimary,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBackgroundElevated)
            .border(1.dp, CardBorderSubtle, RoundedCornerShape(14.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackupIconCircle(icon, if (enabled) iconTint else TextDisabled)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = if (enabled) textColor else TextDisabled,
                        fontSize = 14.sp
                    )
                )
                Text(subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (enabled) TextSecondary else TextDisabled,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

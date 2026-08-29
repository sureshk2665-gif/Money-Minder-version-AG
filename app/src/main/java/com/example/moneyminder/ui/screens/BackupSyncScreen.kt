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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.moneyminder.data.backup.BackupFileManager
import com.example.moneyminder.data.model.BackupOperationStatus
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
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val status by viewModel.backupStatus.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) viewModel.restoreFromFileUri(context, uri)
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

                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp, 20.dp, 20.dp, 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Backup",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold, color = TextPrimary
                            )
                        )
                        Text(
                            "Keep your data safe",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CardBackgroundElevated)
                    ) {
                        Icon(Icons.Default.Close, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                // Progress overlay
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = BankAccent,
                            trackColor = CardBorder,
                            strokeCap = StrokeCap.Round
                        )
                    }
                }

                // Success/Fail banner
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
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
                        Text("x", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
                    }
                }

                // Scrollable content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Auto backup schedule info
                    item {
                        Spacer(Modifier.height(4.dp))
                        BackupSectionHeader("AUTO BACKUP")
                        Spacer(Modifier.height(6.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF0D2211))
                                .border(1.dp, Color(0xFF1C502E), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1A3D22)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Schedule,
                                        null,
                                        tint = IncomeGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "3x Daily Auto Backup",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary,
                                            fontSize = 14.sp
                                        )
                                    )
                                    Text(
                                        "10:00 AM  |  2:00 PM  |  10:00 PM",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = IncomeGreen, fontSize = 12.sp
                                        )
                                    )
                                    Text(
                                        "Each backup replaces the previous one",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextMuted, fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Storage location info
                    item {
                        Spacer(Modifier.height(4.dp))
                        BackupSectionHeader("STORAGE LOCATION")
                        Spacer(Modifier.height(6.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(CardBackgroundElevated)
                                .border(1.dp, CardBorderSubtle, RoundedCornerShape(14.dp))
                                .padding(14.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.FolderOpen,
                                        null,
                                        tint = BankAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        BackupFileManager.getDisplayPath(),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                                Text(
                                    "Auto backups: .../Automatic/  (replaced each time)\nManual backups: .../Manual/  (kept forever)\nPDF & Excel exports saved in root folder",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextMuted, fontSize = 10.5.sp, lineHeight = 15.sp
                                    )
                                )
                            }
                        }
                    }

                    // Last backup status
                    item {
                        val hasLastBackup = status.lastBackupAt > 0L
                        val lastBackupText = if (hasLastBackup) {
                            SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                                .format(Date(status.lastBackupAt))
                        } else "No backup yet"

                        BackupInfoCard(
                            icon = Icons.Default.Backup,
                            iconTint = if (hasLastBackup) TransferBlueGrey else TextDisabled,
                            title = "Last Backup",
                            subtitle = buildString {
                                append(lastBackupText)
                                if (hasLastBackup && status.lastBackupSizeBytes > 0) {
                                    append("  |  ${status.lastBackupSizeBytes / 1024} KB")
                                }
                            }
                        )
                    }

                    // Manual backup
                    item {
                        BackupSectionHeader("MANUAL BACKUP")
                        Spacer(Modifier.height(6.dp))

                        BackupActionCard(
                            icon = Icons.Default.Save,
                            iconTint = IncomeGreen,
                            title = "Backup Now",
                            subtitle = "Save to Manual/ folder (MMBKMN...)",
                            onClick = { viewModel.backupNow() }
                        )

                        Spacer(Modifier.height(8.dp))

                        BackupActionCard(
                            icon = Icons.Default.Send,
                            iconTint = BankAccent,
                            title = "Share Backup",
                            subtitle = "Send backup file via email or any app",
                            onClick = { viewModel.shareBackup(context) }
                        )
                    }

                    // Restore
                    item {
                        BackupSectionHeader("RESTORE")
                        Spacer(Modifier.height(6.dp))

                        if (viewModel.hasLocalBackup()) {
                            BackupActionCard(
                                icon = Icons.Default.Restore,
                                iconTint = TransferBlueGrey,
                                title = "Restore from Last Backup",
                                subtitle = "Replace all data with the saved backup",
                                onClick = { viewModel.restoreFromBackup() }
                            )
                            Spacer(Modifier.height(8.dp))
                        }

                        BackupActionCard(
                            icon = Icons.Default.FolderOpen,
                            iconTint = BankAccent,
                            title = "Restore from File",
                            subtitle = "Pick .mmbackup file from Downloads",
                            onClick = { filePickerLauncher.launch("*/*") }
                        )

                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

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
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(0xFF1C1C26)),
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
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 14.sp
                    )
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary, fontSize = 11.sp
                    ),
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
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBackgroundElevated)
            .border(1.dp, CardBorderSubtle, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackupIconCircle(icon, iconTint)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = textColor,
                        fontSize = 14.sp
                    )
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

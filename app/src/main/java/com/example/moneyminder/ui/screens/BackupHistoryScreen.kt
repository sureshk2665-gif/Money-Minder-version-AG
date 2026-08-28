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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.moneyminder.data.model.BackupMetadata
import com.example.moneyminder.data.model.BackupOperationStatus
import com.example.moneyminder.data.model.BackupType
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
import com.example.moneyminder.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupHistoryScreen(
    viewModel: BackupViewModel,
    onRestoreSelected: (BackupMetadata) -> Unit,
    onDismiss: () -> Unit
) {
    val status by viewModel.backupStatus.collectAsState()
    val history by viewModel.backupHistory.collectAsState()
    var deletePending by remember { mutableStateOf<BackupMetadata?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp),
            color = CardBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp, 20.dp, 20.dp, 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Backup History",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                        Text("${history.size} backup${if (history.size != 1) "s" else ""} found",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(CardBackgroundElevated)
                    ) {
                        Icon(Icons.Default.Close, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (status.operationStatus == BackupOperationStatus.IN_PROGRESS) {
                    Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = BankAccent)
                            Spacer(Modifier.width(10.dp))
                            Text("Loading backups…", color = TextSecondary)
                        }
                    }
                }

                if (history.isEmpty() && status.operationStatus != BackupOperationStatus.IN_PROGRESS) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Backup, null, tint = TextDisabled, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("No backups found", color = TextMuted, fontWeight = FontWeight.SemiBold)
                            Text("Create your first backup from\nBackup & Sync settings",
                                color = TextDisabled, fontSize = 12.sp, lineHeight = 18.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(history) { meta ->
                            BackupHistoryCard(
                                meta = meta,
                                onRestore = { onRestoreSelected(meta) },
                                onDelete = { deletePending = meta }
                            )
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }

    // Delete confirmation
    deletePending?.let { meta ->
        val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            .format(Date(meta.createdAt))
        AlertDialog(
            onDismissRequest = { deletePending = null },
            title = { Text("Delete Backup?", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("Delete the backup from $dateStr?\n\n${meta.sizeBytes / 1024} KB  ·  ${meta.transactionCount} transactions\n\nThis cannot be undone.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBackup(meta)
                        deletePending = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) { Text("Delete", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                OutlinedButton(onClick = { deletePending = null }) { Text("Cancel") }
            },
            containerColor = CardBackground,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun BackupHistoryCard(
    meta: BackupMetadata,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        .format(Date(meta.createdAt))
    val isAuto = meta.type == BackupType.AUTOMATIC

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBackgroundElevated)
            .border(1.dp, CardBorderSubtle, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF1C1C26)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isAuto) Icons.Default.AutoAwesome else Icons.Default.Person,
                            null, tint = if (isAuto) BankAccent else IncomeGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(dateStr,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp
                            )
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isAuto) Color(0xFF0C1826) else Color(0xFF0D2211))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    if (isAuto) "Auto" else "Manual",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isAuto) BankAccent else IncomeGreen,
                                        fontSize = 9.sp, fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Text("${meta.transactionCount} transactions  ·  ${meta.sizeBytes / 1024} KB",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Balance snapshot row
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "Bank" to meta.bankSnapshot,
                    "Wallet" to meta.walletSnapshot,
                    "Cash" to meta.cashSnapshot
                ).forEach { (label, value) ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF151520))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("$label ${CurrencyFormatter.format(value)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary, fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onRestore,
                    modifier = Modifier.weight(1f).height(34.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BankAccent, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Restore, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Restore", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.height(34.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF441C20))
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Delete", fontSize = 12.sp)
                }
            }
        }
    }
}

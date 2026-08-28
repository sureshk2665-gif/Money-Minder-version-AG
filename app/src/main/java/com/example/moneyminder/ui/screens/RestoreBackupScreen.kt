package com.example.moneyminder.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.moneyminder.data.model.BackupMetadata
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
import com.example.moneyminder.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class RestoreStep { SELECT_BACKUP, SELECT_MODE, MERGE_PREVIEW, CONFIRM_REPLACE, RESTORING }

@Composable
fun RestoreBackupScreen(
    viewModel: BackupViewModel,
    onComplete: () -> Unit,
    onDismiss: () -> Unit
) {
    val history by viewModel.backupHistory.collectAsState()
    val status by viewModel.backupStatus.collectAsState()
    val selected by viewModel.selectedBackupForRestore.collectAsState()
    val mergePreview by viewModel.mergePreview.collectAsState()

    var step by remember { mutableStateOf(RestoreStep.SELECT_BACKUP) }
    var selectedMode by remember { mutableStateOf(BackupRestoreMode.MERGE) }

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
                        Text("Restore Backup",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                        Text(
                            when (step) {
                                RestoreStep.SELECT_BACKUP -> "Step 1: Select a backup to restore"
                                RestoreStep.SELECT_MODE   -> "Step 2: Choose restore mode"
                                RestoreStep.MERGE_PREVIEW -> "Step 3: Review merge summary"
                                RestoreStep.CONFIRM_REPLACE -> "Step 3: Confirm data replacement"
                                RestoreStep.RESTORING     -> "Restoring…"
                            },
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

                Spacer(Modifier.height(8.dp))

                // Progress bar if in progress
                if (status.operationStatus == BackupOperationStatus.IN_PROGRESS) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = BankAccent)
                            Spacer(Modifier.width(8.dp))
                            Text(status.operationMessage, color = BankAccent,
                                style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { status.operationProgress },
                            modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                            color = BankAccent, trackColor = CardBorder, strokeCap = StrokeCap.Round
                        )
                    }
                }

                // ── Step content ──────────────────────────────────────────────
                AnimatedContent(
                    targetState = step,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                    modifier = Modifier.weight(1f)
                ) { currentStep ->
                    when (currentStep) {

                        // ── Step 1: Select backup ─────────────────────────────
                        RestoreStep.SELECT_BACKUP -> {
                            if (history.isEmpty()) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Backup, null, tint = TextDisabled, modifier = Modifier.size(48.dp))
                                        Spacer(Modifier.height(12.dp))
                                        Text("No backups available", color = TextMuted)
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(history) { meta ->
                                        val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                                            .format(Date(meta.createdAt))
                                        val isSelected = selected?.gmailMessageId == meta.gmailMessageId
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(if (isSelected) Color(0xFF0C1826) else CardBackgroundElevated)
                                                .border(
                                                    1.dp,
                                                    if (isSelected) BankAccent else CardBorderSubtle,
                                                    RoundedCornerShape(14.dp)
                                                )
                                                .clickable {
                                                    viewModel.selectBackupForRestore(meta)
                                                    step = RestoreStep.SELECT_MODE
                                                }
                                                .padding(14.dp)
                                        ) {
                                            Column {
                                                Text(dateStr,
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp
                                                    ))
                                                Spacer(Modifier.height(4.dp))
                                                Text("${meta.transactionCount} transactions  ·  ${meta.sizeBytes / 1024} KB  ·  ${meta.type.name.lowercase()}",
                                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp))
                                                Spacer(Modifier.height(6.dp))
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    listOf("Bank" to meta.bankSnapshot, "Wallet" to meta.walletSnapshot, "Cash" to meta.cashSnapshot)
                                                        .forEach { (lbl, v) ->
                                                            Box(
                                                                Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF151520)).padding(6.dp, 3.dp)
                                                            ) {
                                                                Text("$lbl ${CurrencyFormatter.format(v)}",
                                                                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp))
                                                            }
                                                        }
                                                }
                                            }
                                        }
                                    }
                                    item { Spacer(Modifier.height(16.dp)) }
                                }
                            }
                        }

                        // ── Step 2: Select mode ───────────────────────────────
                        RestoreStep.SELECT_MODE -> {
                            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                selected?.let { meta ->
                                    val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                                        .format(Date(meta.createdAt))
                                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF0C1826))
                                        .border(1.dp, Color(0xFF1A3048), RoundedCornerShape(12.dp)).padding(12.dp)) {
                                        Text("📅  $dateStr  ·  ${meta.transactionCount} transactions  ·  ${meta.sizeBytes/1024} KB",
                                            style = MaterialTheme.typography.bodySmall.copy(color = BankAccent))
                                    }
                                }

                                // Merge option
                                Box(
                                    modifier = Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (selectedMode == BackupRestoreMode.MERGE) Color(0xFF0D2211) else CardBackgroundElevated)
                                        .border(1.dp, if (selectedMode == BackupRestoreMode.MERGE) IncomeGreen else CardBorderSubtle, RoundedCornerShape(14.dp))
                                        .clickable { selectedMode = BackupRestoreMode.MERGE }
                                        .padding(16.dp)
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CallMerge, null, tint = IncomeGreen, modifier = Modifier.size(20.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("Merge with Current Data",
                                                fontWeight = FontWeight.Bold, color = IncomeGreen, fontSize = 14.sp)
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text("Add new transactions from the backup. Existing transactions with the same IDs are kept. No duplicates created.",
                                            color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
                                        Spacer(Modifier.height(4.dp))
                                        Text("✓ Recommended — safe, no data loss", color = Color(0xFF4DB66A), fontSize = 11.sp)
                                    }
                                }

                                // Replace option
                                Box(
                                    modifier = Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (selectedMode == BackupRestoreMode.REPLACE) Color(0xFF200D0D) else CardBackgroundElevated)
                                        .border(1.dp, if (selectedMode == BackupRestoreMode.REPLACE) ExpenseRed else CardBorderSubtle, RoundedCornerShape(14.dp))
                                        .clickable { selectedMode = BackupRestoreMode.REPLACE }
                                        .padding(16.dp)
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Warning, null, tint = ExpenseRed, modifier = Modifier.size(20.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("Replace Current Data",
                                                fontWeight = FontWeight.Bold, color = ExpenseRed, fontSize = 14.sp)
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text("Wipe all existing transactions and restore entirely from the selected backup. All current data is permanently erased.",
                                            color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
                                        Spacer(Modifier.height(4.dp))
                                        Text("⚠ Destructive — current data will be lost", color = ExpenseRed, fontSize = 11.sp)
                                    }
                                }

                                Spacer(Modifier.weight(1f))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { step = RestoreStep.SELECT_BACKUP }, modifier = Modifier.weight(1f)) {
                                        Text("Back")
                                    }
                                    Button(
                                        onClick = {
                                            selected?.let { meta ->
                                                if (selectedMode == BackupRestoreMode.MERGE) {
                                                    viewModel.previewMerge(meta)
                                                    step = RestoreStep.MERGE_PREVIEW
                                                } else {
                                                    step = RestoreStep.CONFIRM_REPLACE
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = BankAccent)
                                    ) { Text("Continue", fontWeight = FontWeight.Bold) }
                                }
                            }
                        }

                        // ── Step 3a: Merge preview ────────────────────────────
                        RestoreStep.MERGE_PREVIEW -> {
                            Column(Modifier.fillMaxSize().padding(16.dp)) {
                                if (mergePreview == null) {
                                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = BankAccent)
                                    }
                                } else {
                                    val preview = mergePreview!!
                                    Text("Merge Summary", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                                    Spacer(Modifier.height(12.dp))

                                    listOf(
                                        Triple("New transactions to add", preview.newTransactionsToAdd.toString(), IncomeGreen),
                                        Triple("Existing transactions kept", preview.existingTransactionsKept.toString(), TextSecondary),
                                        Triple("Possible duplicates (skipped)", preview.possibleDuplicates.toString(),
                                            if (preview.possibleDuplicates > 0) ExpenseRed else TextSecondary),
                                        Triple("Transfers detected", preview.transfersDetected.toString(), TransferBlueGrey),
                                        Triple("Categories to add", preview.categoriesToAdd.toString(), BankAccent)
                                    ).forEach { (label, value, color) ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(label, color = TextSecondary, fontSize = 13.sp)
                                            Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        Box(Modifier.fillMaxWidth().height(1.dp).background(CardBorderSubtle))
                                    }

                                    Spacer(Modifier.height(12.dp))
                                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFF0C1826))
                                        .border(1.dp, Color(0xFF1A3048), RoundedCornerShape(10.dp)).padding(12.dp)) {
                                        Text("After merge — Bank: ${CurrencyFormatter.format(preview.bankBalanceAfter)}  " +
                                             "Wallet: ${CurrencyFormatter.format(preview.walletBalanceAfter)}  " +
                                             "Cash: ${CurrencyFormatter.format(preview.cashBalanceAfter)}",
                                            color = BankAccent, fontSize = 11.sp, lineHeight = 16.sp)
                                    }
                                }

                                Spacer(Modifier.weight(1f))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { step = RestoreStep.SELECT_MODE }, modifier = Modifier.weight(1f)) {
                                        Text("Back")
                                    }
                                    Button(
                                        onClick = {
                                            selected?.let { meta ->
                                                step = RestoreStep.RESTORING
                                                viewModel.performRestore(meta, BackupRestoreMode.MERGE)
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        enabled = mergePreview != null,
                                        colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen)
                                    ) { Text("Restore & Merge", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                                }
                            }
                        }

                        // ── Step 3b: Replace confirmation ─────────────────────
                        RestoreStep.CONFIRM_REPLACE -> {
                            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF200D0D))
                                    .border(1.dp, Color(0xFF441C20), RoundedCornerShape(12.dp))
                                    .padding(14.dp)) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Warning, null, tint = ExpenseRed, modifier = Modifier.size(20.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("This will permanently erase all your current data",
                                                fontWeight = FontWeight.Bold, color = ExpenseRed, fontSize = 14.sp)
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Text("• All current transactions will be deleted\n" +
                                             "• All current categories will be deleted\n" +
                                             "• Your Bank, Wallet, and Cash balances will be reset\n" +
                                             "• Data from the selected backup will be restored\n" +
                                             "• Balances will be recalculated from restored transaction history",
                                            color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp)
                                    }
                                }

                                selected?.let { meta ->
                                    val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(meta.createdAt))
                                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(CardBackgroundElevated)
                                        .border(1.dp, CardBorderSubtle, RoundedCornerShape(12.dp)).padding(14.dp)) {
                                        Column {
                                            Text("Backup to restore:", color = TextMuted, fontSize = 12.sp)
                                            Text(dateStr, color = TextPrimary, fontWeight = FontWeight.Bold)
                                            Text("${meta.transactionCount} transactions  ·  ${meta.sizeBytes/1024} KB",
                                                color = TextSecondary, fontSize = 12.sp)
                                        }
                                    }
                                }

                                Spacer(Modifier.weight(1f))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { step = RestoreStep.SELECT_MODE }, modifier = Modifier.weight(1f)) {
                                        Text("Cancel")
                                    }
                                    Button(
                                        onClick = {
                                            selected?.let { meta ->
                                                step = RestoreStep.RESTORING
                                                viewModel.performRestore(meta, BackupRestoreMode.REPLACE)
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                                    ) { Text("Erase & Restore", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                                }
                            }
                        }

                        // ── Step 4: Restoring ─────────────────────────────────
                        RestoreStep.RESTORING -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    if (status.operationStatus == BackupOperationStatus.IN_PROGRESS) {
                                        CircularProgressIndicator(color = BankAccent, modifier = Modifier.size(48.dp))
                                        Spacer(Modifier.height(16.dp))
                                        Text(status.operationMessage, color = TextSecondary)
                                    } else if (status.operationStatus == BackupOperationStatus.SUCCESS) {
                                        Icon(Icons.Default.Backup, null, tint = IncomeGreen, modifier = Modifier.size(56.dp))
                                        Spacer(Modifier.height(16.dp))
                                        Text("Restore Complete!", color = IncomeGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        Spacer(Modifier.height(8.dp))
                                        Text("Your data has been restored.\nBalances and history are now up to date.",
                                            color = TextSecondary, fontSize = 13.sp, lineHeight = 20.sp)
                                        Spacer(Modifier.height(24.dp))
                                        Button(
                                            onClick = {
                                                viewModel.acknowledgeRestore()
                                                onComplete()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen)
                                        ) { Text("Go to Home", fontWeight = FontWeight.Bold) }
                                    } else if (status.operationStatus == BackupOperationStatus.FAILED) {
                                        Icon(Icons.Default.Warning, null, tint = ExpenseRed, modifier = Modifier.size(56.dp))
                                        Spacer(Modifier.height(16.dp))
                                        Text("Restore Failed", color = ExpenseRed, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        Spacer(Modifier.height(8.dp))
                                        Text(status.operationMessage, color = TextSecondary, fontSize = 13.sp)
                                        Text("Your existing data was not modified.", color = TextMuted, fontSize = 12.sp)
                                        Spacer(Modifier.height(20.dp))
                                        Button(
                                            onClick = { step = RestoreStep.SELECT_BACKUP },
                                            colors = ButtonDefaults.buttonColors(containerColor = BankAccent)
                                        ) { Text("Try Again") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

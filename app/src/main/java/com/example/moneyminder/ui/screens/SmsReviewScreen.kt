package com.example.moneyminder.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.moneyminder.data.model.InboxSmsMessage
import com.example.moneyminder.data.model.SmsCandidate
import com.example.moneyminder.data.model.TransactionType
import com.example.moneyminder.theme.BackgroundDark
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
import com.example.moneyminder.ui.viewmodel.MainViewModel
import com.example.moneyminder.util.CurrencyFormatter
import com.example.moneyminder.util.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun SmsReviewScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Inbox Sync", "Paste SMS")
    var showBlockedSendersDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CardBackgroundElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Sms,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SMS Transactions",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = "Sync from inbox or paste manually",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }
            val blockedCount by viewModel.blockedSmsSenders.collectAsState()
            if (blockedCount.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(ExpenseRed.copy(alpha = 0.15f))
                        .border(1.dp, ExpenseRed.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .clickable { showBlockedSendersDialog = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            tint = ExpenseRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${blockedCount.size}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }

        // Tab bar
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = BackgroundDark,
            contentColor = TextPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = TextPrimary,
                    height = 2.dp
                )
            },
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) TextPrimary else TextMuted,
                            fontSize = 13.sp
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> InboxSyncContent(
                viewModel = viewModel,
                onBlockSender = { senderName ->
                    viewModel.blockSmsSender(senderName)
                }
            )
            1 -> PasteSmsContent(viewModel = viewModel)
        }
    }

    if (showBlockedSendersDialog) {
        BlockedSendersDialog(
            viewModel = viewModel,
            onDismiss = { showBlockedSendersDialog = false }
        )
    }
}

@Composable
private fun InboxSyncContent(
    viewModel: MainViewModel,
    onBlockSender: (String) -> Unit
) {
    val context = LocalContext.current
    val inboxSms by viewModel.inboxSmsList.collectAsState()
    val isLoading by viewModel.smsLoading.collectAsState()
    val permissionGranted by viewModel.smsPermissionGranted.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setSmsPermissionGranted(granted)
    }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            viewModel.setSmsPermissionGranted(true)
        }
    }

    if (!permissionGranted) {
        // Permission request
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(CardBackgroundElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    tint = BankAccent,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Sync SMS Inbox",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold, color = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Money Minder will sync all your SMS messages and highlight bank transaction alerts. Tap any transaction to add it.\n\nNo data leaves your device.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary, lineHeight = 18.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { permissionLauncher.launch(Manifest.permission.READ_SMS) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BankAccent,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(50.dp)
            ) {
                Icon(Icons.Default.Sms, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Allow SMS Access", fontWeight = FontWeight.Bold)
            }
        }
    } else if (isLoading) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = BankAccent,
                modifier = Modifier.size(40.dp),
                strokeWidth = 3.dp
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Syncing all SMS messages...",
                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
            )
        }
    } else if (inboxSms.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "No SMS found",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold, color = TextMuted
                )
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "No messages found in the last 90 days",
                style = MaterialTheme.typography.bodySmall.copy(color = TextDisabled),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            OutlinedButton(
                onClick = { viewModel.syncInboxSms() },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Refresh")
            }
        }
    } else {
        val financialCount = inboxSms.count { it.isFinancial }

        // Top bar with counts and refresh
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "${inboxSms.size} messages synced",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary, fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    "$financialCount transaction SMS detected",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = BankAccent, fontSize = 11.sp
                    )
                )
            }
            OutlinedButton(
                onClick = { viewModel.syncInboxSms() },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Refresh", fontSize = 12.sp)
            }
        }

        // SMS list — grouped by date like native inbox
        val grouped = inboxSms.groupBy { msg ->
            getDateLabel(msg.dateMillis)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            grouped.forEach { (dateLabel, messages) ->
                // Date header
                item(key = "header_$dateLabel") {
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 0.5.sp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BackgroundDark)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                items(messages, key = { it.id }) { msg ->
                    SmsInboxItem(
                        message = msg,
                        onClick = {
                            if (msg.isFinancial && !msg.isDuplicate) {
                                viewModel.useInboxSmsToAdd(msg)
                            }
                        },
                        onBlockSender = { onBlockSender(msg.sender) }
                    )
                    HorizontalDivider(
                        color = CardBorderSubtle,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 72.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SmsInboxItem(
    message: InboxSmsMessage,
    onClick: () -> Unit,
    onBlockSender: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val timeText = timeFormat.format(Date(message.dateMillis))
    val isTransaction = message.isFinancial
    val isDuplicate = message.isDuplicate
    var showDropdown by remember { mutableStateOf(false) }

    Box {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isTransaction && !isDuplicate) CardBackground
                else BackgroundDark
            )
            .combinedClickable(
                enabled = true,
                onClick = {
                    if (isTransaction && !isDuplicate) onClick()
                },
                onLongClick = { showDropdown = true }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Sender avatar
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isDuplicate -> IncomeGreen.copy(alpha = 0.15f)
                        isTransaction -> BankAccent.copy(alpha = 0.15f)
                        else -> CardBackgroundElevated
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isDuplicate) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = IncomeGreen,
                    modifier = Modifier.size(22.dp)
                )
            } else if (isTransaction) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = BankAccent,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message.sender.ifBlank { "Unknown" },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isTransaction) FontWeight.Bold else FontWeight.SemiBold,
                        color = when {
                            isDuplicate -> TextMuted
                            isTransaction -> TextPrimary
                            else -> TextSecondary
                        }
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = timeText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isTransaction && !isDuplicate) TextSecondary else TextDisabled,
                        fontSize = 11.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            // For financial SMS: show amount + body preview
            if (isTransaction && message.parsedCandidate != null) {
                val candidate = message.parsedCandidate
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val amountColor = when {
                        isDuplicate -> TextMuted
                        candidate.type == TransactionType.INCOME -> IncomeGreen
                        else -> ExpenseRed
                    }
                    val prefix = if (candidate.type == TransactionType.INCOME) "+" else "-"
                    Text(
                        text = "$prefix${CurrencyFormatter.format(candidate.amount)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = amountColor,
                            fontSize = 12.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "·",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = candidate.suggestedAccount.displayName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isDuplicate) TextDisabled else TextSecondary,
                            fontSize = 11.sp
                        )
                    )
                    if (isDuplicate) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "· Imported",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = IncomeGreen,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
            }

            // Body preview
            Text(
                text = message.body.replace("\n", " ").trim(),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (isDuplicate) TextDisabled else TextSecondary,
                    lineHeight = 16.sp,
                    fontSize = 12.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

        MaterialTheme(
            colorScheme = MaterialTheme.colorScheme.copy(
                surface = CardBackgroundElevated,
                onSurface = TextPrimary
            )
        ) {
            DropdownMenu(
                expanded = showDropdown,
                onDismissRequest = { showDropdown = false },
                offset = DpOffset(48.dp, 0.dp)
            ) {
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                tint = ExpenseRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Remove sender",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = ExpenseRed,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    },
                    onClick = {
                        showDropdown = false
                        onBlockSender()
                    }
                )
            }
        }
    }
}

@Composable
private fun PasteSmsContent(viewModel: MainViewModel) {
    var pastedText by remember { mutableStateOf("") }
    val candidates by viewModel.smsCandidates.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (candidates.isEmpty()) {
            OutlinedTextField(
                value = pastedText,
                onValueChange = { pastedText = it },
                placeholder = {
                    Text(
                        text = "Paste one or more transaction SMS messages here...\n\nExample:\nUnion Bank of India A/c *0531 Debited Rs:349.00 on 25-08-2026 10:00:38 by Mob Bk ref no 214034114011, Fvg: Airtel Avl Bal Rs:24163.65.",
                        color = TextDisabled,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TextPrimary,
                    unfocusedBorderColor = CardBorder,
                    focusedContainerColor = CardBackgroundElevated,
                    unfocusedContainerColor = CardBackgroundElevated,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    if (pastedText.isNotBlank()) {
                        viewModel.parsePastedSms(pastedText)
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TextPrimary,
                    contentColor = Color.Black
                ),
                enabled = pastedText.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Default.ContentPaste, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Review SMS", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        } else {
            Text(
                text = "Tap a transaction below to pre-fill the Add screen:",
                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(candidates) { candidate ->
                    PastedSmsCard(
                        candidate = candidate,
                        onClick = { viewModel.useSmsCandidateToAdd(candidate) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    viewModel.clearSmsCandidates()
                    pastedText = ""
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Text("Paste Another SMS", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun PastedSmsCard(
    candidate: SmsCandidate,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackgroundElevated)
            .border(
                1.dp,
                if (candidate.isDuplicate) IncomeGreen.copy(alpha = 0.4f) else CardBorder,
                RoundedCornerShape(16.dp)
            )
            .clickable(enabled = !candidate.isDuplicate) { onClick() }
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = candidate.suggestedCategory,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (candidate.isDuplicate) TextMuted else TextPrimary
                    ),
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (candidate.type == TransactionType.INCOME)
                        "+ ${CurrencyFormatter.format(candidate.amount)}"
                    else
                        "- ${CurrencyFormatter.format(candidate.amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (candidate.isDuplicate) TextMuted
                        else if (candidate.type == TransactionType.INCOME) IncomeGreen
                        else ExpenseRed
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            val accLabel = if (candidate.type == TransactionType.EXPENSE)
                "Paid from ${candidate.suggestedAccount.displayName}"
            else
                "Received in ${candidate.suggestedAccount.displayName}"
            Text(
                text = "$accLabel  |  ${DateTimeUtils.formatDateTime(candidate.timestamp)}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary, fontSize = 11.5.sp
                )
            )

            if (candidate.isDuplicate) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = IncomeGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = candidate.duplicateReason ?: "Already imported",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = IncomeGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun BlockedSendersDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val blockedSenders by viewModel.blockedSmsSenders.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.6f),
            shape = RoundedCornerShape(24.dp),
            color = CardBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            tint = ExpenseRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Blocked Senders",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "SMS from these senders are hidden from your inbox sync",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (blockedSenders.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No blocked senders",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(blockedSenders.sorted()) { sender ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CardBackgroundElevated)
                                    .border(1.dp, CardBorderSubtle, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = sender,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.unblockSmsSender(sender) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Unblock",
                                        tint = ExpenseRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getDateLabel(millis: Long): String {
    val msgCal = Calendar.getInstance().apply { timeInMillis = millis }
    val todayCal = Calendar.getInstance()
    val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

    return when {
        msgCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                msgCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR) -> "Today"
        msgCal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
                msgCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR) -> "Yesterday"
        else -> SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault()).format(Date(millis))
    }
}

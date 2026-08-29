package com.example.moneyminder.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.moneyminder.data.model.SmsCandidate
import com.example.moneyminder.data.model.TransactionType
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
import com.example.moneyminder.theme.WalletAccent
import com.example.moneyminder.ui.viewmodel.MainViewModel
import com.example.moneyminder.util.CurrencyFormatter
import com.example.moneyminder.util.DateTimeUtils

@Composable
fun SmsReviewScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Inbox Sync", "Paste SMS")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CardBackground)
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
            Column {
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
        }

        // Tab bar
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = CardBackground,
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
            0 -> InboxSyncContent(viewModel = viewModel)
            1 -> PasteSmsContent(viewModel = viewModel)
        }
    }
}

@Composable
private fun InboxSyncContent(viewModel: MainViewModel) {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (!permissionGranted) {
            // Permission request state
            Column(
                modifier = Modifier.fillMaxSize(),
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
                    text = "Money Minder can scan your SMS inbox for bank transaction alerts and list them here. Tap any message to add it as a transaction.\n\nNo data leaves your device.",
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
            // Loading state
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
                    "Scanning inbox for transactions...",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                )
            }
        } else if (inboxSms.isEmpty()) {
            // No transactions found
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "No financial SMS found",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold, color = TextMuted
                    )
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "No bank transaction messages detected in the last 90 days",
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
            // Results
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${inboxSms.size} financial SMS found (last 90 days)",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextMuted, fontWeight = FontWeight.SemiBold
                    )
                )
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

            Spacer(Modifier.height(6.dp))

            Text(
                "Tap any transaction to review and add it:",
                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp)
            )

            Spacer(Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(inboxSms) { candidate ->
                    InboxSmsCard(
                        candidate = candidate,
                        onClick = { viewModel.useSmsCandidateToAdd(candidate) }
                    )
                }
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
                    InboxSmsCard(
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
private fun InboxSmsCard(
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = candidate.suggestedCategory,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (candidate.isDuplicate) TextMuted else TextPrimary
                        ),
                        maxLines = 1
                    )
                }

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

            if (candidate.postBalance != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Post Balance: ${CurrencyFormatter.format(candidate.postBalance)}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextMuted, fontSize = 10.5.sp
                    )
                )
            }

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

            if (candidate.isPendingVerification) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Pending Verification in SMS",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                )
            }
        }
    }
}

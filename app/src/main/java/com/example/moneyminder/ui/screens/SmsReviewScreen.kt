package com.example.moneyminder.ui.screens

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
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.moneyminder.data.model.SmsCandidate
import com.example.moneyminder.data.model.TransactionType
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
    var pastedText by remember { mutableStateOf("") }
    val candidates by viewModel.smsCandidates.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CardBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                    text = if (candidates.isEmpty()) "SMS Review" else "SMS Extraction Results",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = if (candidates.isEmpty()) "Paste financial SMS alerts to extract details" else "${candidates.size} transaction(s) detected",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (candidates.isEmpty()) {
            // Paste Input State — full page with text box and review button
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

            // Review Button
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
                Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Review SMS", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        } else {
            // Results State
            Text(
                text = "Tap a transaction below to pre-fill the Add screen for confirmation before saving:",
                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(candidates) { candidate ->
                    CandidateSmsCard(
                        candidate = candidate,
                        onClick = {
                            viewModel.useSmsCandidateToAdd(candidate)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom action — paste another
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
private fun CandidateSmsCard(
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
                if (candidate.isDuplicate) WalletAccent.copy(alpha = 0.5f) else CardBorder,
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
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
                        color = TextPrimary
                    )
                )

                Text(
                    text = if (candidate.type == TransactionType.INCOME) "+ ${CurrencyFormatter.format(candidate.amount)}" else "- ${CurrencyFormatter.format(candidate.amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (candidate.type == TransactionType.INCOME) IncomeGreen else ExpenseRed
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            val accLabel = if (candidate.type == TransactionType.EXPENSE) "Paid from ${candidate.suggestedAccount.displayName}" else "Received in ${candidate.suggestedAccount.displayName}"
            Text(
                text = "$accLabel  |  ${DateTimeUtils.formatDateTime(candidate.timestamp)}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 11.5.sp
                )
            )

            if (candidate.postBalance != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Post Balance in SMS: ${CurrencyFormatter.format(candidate.postBalance)}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextMuted,
                        fontSize = 10.5.sp
                    )
                )
            }

            if (candidate.isDuplicate) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = WalletAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = candidate.duplicateReason ?: "Possible Duplicate",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = WalletAccent,
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

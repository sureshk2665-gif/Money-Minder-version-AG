package com.example.moneyminder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.moneyminder.data.model.TransactionEntity
import com.example.moneyminder.data.model.TransactionType
import com.example.moneyminder.theme.CardBackground
import com.example.moneyminder.theme.CardBackgroundElevated
import com.example.moneyminder.theme.CardBorder
import com.example.moneyminder.theme.ExpenseCardBg
import com.example.moneyminder.theme.ExpenseRed
import com.example.moneyminder.theme.IncomeGreen
import com.example.moneyminder.theme.TextMuted
import com.example.moneyminder.theme.TextPrimary
import com.example.moneyminder.theme.TextSecondary
import com.example.moneyminder.ui.viewmodel.MainViewModel
import com.example.moneyminder.util.CurrencyFormatter
import com.example.moneyminder.util.DateTimeUtils

@Composable
fun TransactionDetailDialog(
    transaction: TransactionEntity,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = CardBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header: Type + Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when (transaction.type) {
                                    TransactionType.INCOME -> IncomeGreen.copy(alpha = 0.2f)
                                    TransactionType.EXPENSE -> ExpenseRed.copy(alpha = 0.2f)
                                    TransactionType.TRANSFER -> CardBackgroundElevated
                                }
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = transaction.type.displayName.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = when (transaction.type) {
                                    TransactionType.INCOME -> IncomeGreen
                                    TransactionType.EXPENSE -> ExpenseRed
                                    TransactionType.TRANSFER -> TextPrimary
                                }
                            )
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(CardBackgroundElevated)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Large Amount
                Text(
                    text = when (transaction.type) {
                        TransactionType.INCOME -> "+ ${CurrencyFormatter.format(transaction.amount)}"
                        TransactionType.EXPENSE -> "− ${CurrencyFormatter.format(transaction.amount)}"
                        TransactionType.TRANSFER -> CurrencyFormatter.format(transaction.amount)
                    },
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = when (transaction.type) {
                            TransactionType.INCOME -> IncomeGreen
                            TransactionType.EXPENSE -> ExpenseRed
                            TransactionType.TRANSFER -> TextPrimary
                        }
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Category or Direction
                Text(
                    text = if (transaction.type == TransactionType.TRANSFER) {
                        "${transaction.fromAccount?.displayName} → ${transaction.toAccount?.displayName}"
                    } else {
                        transaction.category
                    },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = CardBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                // Details Rows
                DetailItemRow(label = "Date & Time", value = DateTimeUtils.formatDateTime(transaction.timestamp))

                when (transaction.type) {
                    TransactionType.EXPENSE -> {
                        DetailItemRow(label = "Paid From", value = transaction.fromAccount?.displayName ?: "Bank")
                        DetailItemRow(label = "Bank/Account Balance After", value = CurrencyFormatter.format(transaction.balanceAfterPrimary))
                    }
                    TransactionType.INCOME -> {
                        DetailItemRow(label = "Received In", value = transaction.toAccount?.displayName ?: "Bank")
                        DetailItemRow(label = "Bank/Account Balance After", value = CurrencyFormatter.format(transaction.balanceAfterPrimary))
                    }
                    TransactionType.TRANSFER -> {
                        DetailItemRow(label = "Transfer From", value = transaction.fromAccount?.displayName ?: "Bank")
                        DetailItemRow(label = "${transaction.fromAccount?.shortName} Balance After", value = CurrencyFormatter.format(transaction.balanceAfterPrimary))
                        DetailItemRow(label = "Transfer To", value = transaction.toAccount?.displayName ?: "Wallet")
                        DetailItemRow(label = "${transaction.toAccount?.shortName} Balance After", value = CurrencyFormatter.format(transaction.balanceAfterSecondary))
                    }
                }

                if (!transaction.referenceNumber.isNullOrBlank()) {
                    DetailItemRow(label = "Reference No", value = transaction.referenceNumber)
                }

                if (transaction.note.isNotBlank()) {
                    DetailItemRow(label = "Note", value = transaction.note)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions: Duplicate, Delete
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Duplicate Button
                    OutlinedButton(
                        onClick = { viewModel.duplicateTransaction(transaction) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Duplicate")
                    }

                    // Delete Button
                    Button(
                        onClick = { showDeleteConfirm = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ExpenseCardBg,
                            contentColor = ExpenseRed
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, ExpenseRed.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Confirmation Dialog before deletion
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Transaction", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = if (transaction.type == TransactionType.TRANSFER) {
                        "Are you sure? Deleting this transfer will restore the transferred amount back to ${transaction.fromAccount?.displayName} and remove it from ${transaction.toAccount?.displayName}."
                    } else {
                        "Are you sure you want to delete this transaction? All affected balances will be recalculated."
                    },
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteTransaction(transaction.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteConfirm = false },
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
private fun DetailItemRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        )
    }
}

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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.moneyminder.data.model.AccountBalances
import com.example.moneyminder.data.model.AccountType
import com.example.moneyminder.data.model.TransactionType
import com.example.moneyminder.theme.BankAccent
import com.example.moneyminder.theme.CardBackground
import com.example.moneyminder.theme.CardBackgroundElevated
import com.example.moneyminder.theme.CardBorder
import com.example.moneyminder.theme.CashAccent
import com.example.moneyminder.theme.ExpenseRed
import com.example.moneyminder.theme.IncomeGreen
import com.example.moneyminder.theme.TextMuted
import com.example.moneyminder.theme.TextPrimary
import com.example.moneyminder.theme.TextSecondary
import com.example.moneyminder.theme.WalletAccent
import com.example.moneyminder.ui.components.TransactionCard
import com.example.moneyminder.ui.viewmodel.MainViewModel
import com.example.moneyminder.util.CurrencyFormatter

@Composable
fun AccountDetailDialog(
    account: AccountType,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val balances by viewModel.balances.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val typeFilter by viewModel.accountDetailTypeFilter.collectAsState()

    val accountBalance = balances.getBalance(account)
    val accountIcon = when (account) {
        AccountType.BANK -> Icons.Default.AccountBalance
        AccountType.WALLET -> Icons.Default.AccountBalanceWallet
        AccountType.CASH -> Icons.Default.Payments
        AccountType.OVERALL -> Icons.Default.AccountBalance
    }
    val accentColor = when (account) {
        AccountType.BANK -> BankAccent
        AccountType.WALLET -> WalletAccent
        AccountType.CASH -> CashAccent
        AccountType.OVERALL -> TextPrimary
    }

    // Filter transactions affecting this specific account
    val accountTransactions = allTransactions.filter { tx ->
        (tx.fromAccount == account || tx.toAccount == account) &&
        (typeFilter == null || tx.type == typeFilter)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = CardBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
            ) {
                // Header with Account Icon, Name, and Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.15f))
                                .border(1.dp, accentColor.copy(alpha = 0.35f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = accountIcon,
                                contentDescription = account.displayName,
                                tint = accentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "${account.displayName} Account",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "Current Balance: ${CurrencyFormatter.format(accountBalance)}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }

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

                // Income & Expense Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChipItem(
                        title = "All",
                        isSelected = typeFilter == null,
                        onClick = { viewModel.setAccountDetailTypeFilter(null) }
                    )
                    FilterChipItem(
                        title = "Income",
                        isSelected = typeFilter == TransactionType.INCOME,
                        activeColor = IncomeGreen,
                        onClick = { viewModel.setAccountDetailTypeFilter(TransactionType.INCOME) }
                    )
                    FilterChipItem(
                        title = "Expense",
                        isSelected = typeFilter == TransactionType.EXPENSE,
                        activeColor = ExpenseRed,
                        onClick = { viewModel.setAccountDetailTypeFilter(TransactionType.EXPENSE) }
                    )
                    FilterChipItem(
                        title = "Transfer",
                        isSelected = typeFilter == TransactionType.TRANSFER,
                        onClick = { viewModel.setAccountDetailTypeFilter(TransactionType.TRANSFER) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Account Timeline List
                if (accountTransactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No matching transactions for ${account.displayName}",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(accountTransactions, key = { it.id }) { tx ->
                            TransactionCard(
                                transaction = tx,
                                onClick = {
                                    onDismiss()
                                    viewModel.openTransactionDetail(tx)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipItem(
    title: String,
    isSelected: Boolean,
    activeColor: Color = TextPrimary,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) activeColor.copy(alpha = 0.2f) else CardBackgroundElevated)
            .border(
                width = 1.dp,
                color = if (isSelected) activeColor else CardBorder,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) activeColor else TextSecondary,
                fontSize = 12.sp
            )
        )
    }
}

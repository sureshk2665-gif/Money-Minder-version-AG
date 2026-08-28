package com.example.moneyminder.ui.components

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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moneyminder.data.model.TransactionEntity
import com.example.moneyminder.data.model.TransactionType
import com.example.moneyminder.theme.CardBackground
import com.example.moneyminder.theme.CardBorder
import com.example.moneyminder.theme.ExpenseBorder
import com.example.moneyminder.theme.ExpenseCardBg
import com.example.moneyminder.theme.ExpenseRed
import com.example.moneyminder.theme.IncomeBorder
import com.example.moneyminder.theme.IncomeCardBg
import com.example.moneyminder.theme.IncomeGreen
import com.example.moneyminder.theme.TextMuted
import com.example.moneyminder.theme.TextPrimary
import com.example.moneyminder.theme.TextSecondary
import com.example.moneyminder.theme.TransferBlueGrey
import com.example.moneyminder.theme.TransferBorder
import com.example.moneyminder.theme.TransferCardBg
import com.example.moneyminder.util.CurrencyFormatter
import com.example.moneyminder.util.DateTimeUtils

@Composable
fun TransactionCard(
    transaction: TransactionEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (cardBg, borderColor, iconBg, iconColor, iconVector) = when (transaction.type) {
        TransactionType.INCOME -> Tuple5(
            listOf(IncomeCardBg, Color(0xFF0F1811)),
            IncomeBorder,
            IncomeGreen.copy(alpha = 0.18f),
            IncomeGreen,
            Icons.Default.ArrowUpward
        )
        TransactionType.EXPENSE -> Tuple5(
            listOf(ExpenseCardBg, Color(0xFF190F11)),
            ExpenseBorder,
            ExpenseRed.copy(alpha = 0.18f),
            ExpenseRed,
            Icons.Default.ArrowDownward
        )
        TransactionType.TRANSFER -> Tuple5(
            listOf(TransferCardBg, Color(0xFF13171C)),
            TransferBorder,
            TransferBlueGrey.copy(alpha = 0.18f),
            TransferBlueGrey,
            Icons.Default.SwapHoriz
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(cardBg))
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Icon + Category + Context description
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Direction Icon
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(iconBg)
                        .border(width = 1.dp, color = iconColor.copy(alpha = 0.35f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = transaction.type.displayName,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    // Category / Name
                    Text(
                        text = transaction.category,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = when (transaction.type) {
                                TransactionType.INCOME -> IncomeGreen
                                TransactionType.EXPENSE -> TextPrimary
                                TransactionType.TRANSFER -> TextPrimary
                            }
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Context line: e.g. "Received in Bank · 24 Aug 2026, 03:58 PM"
                    val contextText = when (transaction.type) {
                        TransactionType.EXPENSE -> "Paid from ${transaction.fromAccount?.displayName ?: "Account"} · ${DateTimeUtils.formatDateTime(transaction.timestamp)}"
                        TransactionType.INCOME -> "Received in ${transaction.toAccount?.displayName ?: "Account"} · ${DateTimeUtils.formatDateTime(transaction.timestamp)}"
                        TransactionType.TRANSFER -> "${transaction.fromAccount?.shortName ?: ""} → ${transaction.toAccount?.shortName ?: ""} · ${DateTimeUtils.formatDateTime(transaction.timestamp)}"
                    }

                    Text(
                        text = contextText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Right: Amount + Historical Account Balance
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                // Amount
                val amountText = when (transaction.type) {
                    TransactionType.INCOME -> "+ ${CurrencyFormatter.format(transaction.amount)}"
                    TransactionType.EXPENSE -> "− ${CurrencyFormatter.format(transaction.amount)}"
                    TransactionType.TRANSFER -> CurrencyFormatter.format(transaction.amount)
                }

                val amountColor = when (transaction.type) {
                    TransactionType.INCOME -> IncomeGreen
                    TransactionType.EXPENSE -> ExpenseRed
                    TransactionType.TRANSFER -> TextPrimary
                }

                Text(
                    text = amountText,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = amountColor,
                        fontSize = 15.sp
                    )
                )

                Spacer(modifier = Modifier.height(3.dp))

                // Historical balance of affected account immediately after this transaction
                val runningBalanceText = when (transaction.type) {
                    TransactionType.EXPENSE -> "${transaction.fromAccount?.displayName ?: "Account"} Balance: ${CurrencyFormatter.format(transaction.balanceAfterPrimary)}"
                    TransactionType.INCOME -> "${transaction.toAccount?.displayName ?: "Account"} Balance: ${CurrencyFormatter.format(transaction.balanceAfterPrimary)}"
                    TransactionType.TRANSFER -> "${transaction.fromAccount?.shortName}: ${CurrencyFormatter.format(transaction.balanceAfterPrimary)} | ${transaction.toAccount?.shortName}: ${CurrencyFormatter.format(transaction.balanceAfterSecondary)}"
                }

                Text(
                    text = runningBalanceText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextMuted,
                        fontSize = 10.5.sp
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

private data class Tuple5<A, B, C, D, E>(
    val a: A,
    val b: B,
    val c: C,
    val d: D,
    val e: E
)

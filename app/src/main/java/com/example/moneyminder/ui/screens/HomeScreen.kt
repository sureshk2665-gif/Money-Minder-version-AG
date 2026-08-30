package com.example.moneyminder.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moneyminder.data.model.AccountType
import com.example.moneyminder.data.model.TransactionEntity
import com.example.moneyminder.data.model.TransactionType
import com.example.moneyminder.theme.BackgroundDark
import com.example.moneyminder.theme.CardBackground
import com.example.moneyminder.theme.CardBackgroundElevated
import com.example.moneyminder.theme.CardBorder
import com.example.moneyminder.theme.CardBorderSubtle
import com.example.moneyminder.theme.ExpenseCardBg
import com.example.moneyminder.theme.ExpenseRed
import com.example.moneyminder.theme.IncomeCardBg
import com.example.moneyminder.theme.IncomeGreen
import com.example.moneyminder.theme.TextDisabled
import com.example.moneyminder.theme.TextMuted
import com.example.moneyminder.theme.TextPrimary
import com.example.moneyminder.theme.TextSecondary
import com.example.moneyminder.theme.TransferBlueGrey
import com.example.moneyminder.ui.components.AccountCardsRow
import com.example.moneyminder.ui.components.MoneyMinderHeader
import com.example.moneyminder.ui.components.TransactionCard
import com.example.moneyminder.ui.viewmodel.MainViewModel
import com.example.moneyminder.util.CurrencyFormatter

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToAdd: (type: TransactionType) -> Unit,
    onOpenExport: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val selectedYear by viewModel.selectedYear.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val balances by viewModel.balances.collectAsState()
    val monthlySummary by viewModel.monthlySummary.collectAsState()
    val transactions by viewModel.currentMonthTransactions.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // App Header with Money Minder Logo, Month Picker & Settings Button
        MoneyMinderHeader(
            selectedYear = selectedYear,
            selectedMonth = selectedMonth,
            onMonthClick = { viewModel.setShowMonthPicker(true) },
            onSettingsClick = { viewModel.setShowSettings(true) }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Primary Overall Balance Card
            item {
                OverallBalanceCard(
                    overallBalance = balances.overallBalance,
                    monthlyIncome = monthlySummary.totalIncome,
                    monthlyExpense = monthlySummary.totalExpense,
                    dateRangeText = monthlySummary.dateRangeText
                )
            }

            // 2. Compact Account Cards (Bank, Wallet, Cash - Icons only, no text labels)
            item {
                AccountCardsRow(
                    balances = balances,
                    onAccountClick = { account ->
                        viewModel.openAccountDetail(account)
                    }
                )
            }

            // 3. Monthly Transfer Movement Summary
            item {
                MonthlyTransferSummaryCard(
                    transfersMoved = monthlySummary.totalTransfersMoved
                )
            }

            // 4. Quick Actions
            item {
                QuickActionsSection(
                    onAddExpense = { onNavigateToAdd(TransactionType.EXPENSE) },
                    onAddIncome = { onNavigateToAdd(TransactionType.INCOME) },
                    onTransfer = { onNavigateToAdd(TransactionType.TRANSFER) }
                )
            }

            // 5. Overall Activity Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Overall Activity",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "${transactions.size} records",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextMuted
                        )
                    )
                }
            }

            // 6. Transaction Activity Feed
            if (transactions.isEmpty()) {
                item {
                    EmptyActivityView(
                        onAddExpense = { onNavigateToAdd(TransactionType.EXPENSE) },
                        onAddIncome = { onNavigateToAdd(TransactionType.INCOME) }
                    )
                }
            } else {
                items(transactions, key = { it.id }) { tx ->
                    TransactionCard(
                        transaction = tx,
                        onClick = { viewModel.openTransactionDetail(tx) },
                        modifier = Modifier.animateItem(
                            fadeInSpec = spring(stiffness = Spring.StiffnessLow),
                            fadeOutSpec = spring(stiffness = Spring.StiffnessLow)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun OverallBalanceCard(
    overallBalance: Double,
    monthlyIncome: Double,
    monthlyExpense: Double,
    dateRangeText: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF22222C),
                        Color(0xFF16161C)
                    )
                )
            )
            .border(width = 1.dp, color = CardBorder, shape = RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Overall Balance",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                )
                Text(
                    text = dateRangeText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Large Overall Balance Value
            Text(
                text = CurrencyFormatter.format(overallBalance),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = (-0.5).sp
                )
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0x22FFFFFF), thickness = 1.dp)
            Spacer(modifier = Modifier.height(14.dp))

            // Income & Expense Sub-Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Income Column
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(IncomeGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Income",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = IncomeGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "+ ${CurrencyFormatter.format(monthlyIncome)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen,
                            fontSize = 15.sp
                        )
                    )
                }

                // Expense Column
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(ExpenseRed)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Expense",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = ExpenseRed,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "− ${CurrencyFormatter.format(monthlyExpense)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed,
                            fontSize = 15.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthlyTransferSummaryCard(
    transfersMoved: Double
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(width = 1.dp, color = CardBorderSubtle, shape = RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = null,
                    tint = TransferBlueGrey,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Transfers this month",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            Text(
                text = "${CurrencyFormatter.format(transfersMoved)} moved",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
        }
    }
}

@Composable
private fun QuickActionsSection(
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    onTransfer: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            QuickActionButton(
                title = "Expense",
                icon = Icons.Default.ArrowDownward,
                color = ExpenseRed,
                onClick = onAddExpense,
                modifier = Modifier.weight(1f)
            )
            QuickActionButton(
                title = "Income",
                icon = Icons.Default.ArrowUpward,
                color = IncomeGreen,
                onClick = onAddIncome,
                modifier = Modifier.weight(1f)
            )
            QuickActionButton(
                title = "Transfer",
                icon = Icons.Default.SwapHoriz,
                color = TransferBlueGrey,
                onClick = onTransfer,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "quickActionPress"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(width = 1.dp, color = CardBorder, shape = RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(horizontal = 10.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    fontSize = 12.5.sp
                )
            )
        }
    }
}

@Composable
private fun EmptyActivityView(
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBackground)
            .border(width = 1.dp, color = CardBorder, shape = RoundedCornerShape(20.dp))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "No transactions in this month",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Add an expense, income, or transfer to see historical running balances.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextMuted,
                    fontSize = 12.sp
                ),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onAddIncome,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IncomeCardBg,
                        contentColor = IncomeGreen
                    ),
                    modifier = Modifier.border(1.dp, IncomeGreen.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                ) {
                    Text("+ Add Income", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onAddExpense,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ExpenseCardBg,
                        contentColor = ExpenseRed
                    ),
                    modifier = Modifier.border(1.dp, ExpenseRed.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                ) {
                    Text("− Add Expense", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

package com.example.moneyminder.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moneyminder.data.model.TransactionType
import com.example.moneyminder.theme.BackdropGradient
import com.example.moneyminder.theme.ExpenseRed
import com.example.moneyminder.theme.IncomeGreen
import com.example.moneyminder.theme.TextMuted
import com.example.moneyminder.theme.TextPrimary
import com.example.moneyminder.theme.TextSecondary
import com.example.moneyminder.theme.TransferBlueGrey
import com.example.moneyminder.theme.AccentSoft
import com.example.moneyminder.ui.components.AccountCardsRow
import com.example.moneyminder.ui.components.AppCard
import com.example.moneyminder.ui.components.GradientPillButton
import com.example.moneyminder.ui.components.HeroCard
import com.example.moneyminder.ui.components.MoneyMinderHeader
import com.example.moneyminder.ui.components.PillChip
import com.example.moneyminder.ui.components.SectionHeader
import com.example.moneyminder.ui.components.StatBlock
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
            .background(BackdropGradient)
    ) {
        MoneyMinderHeader(
            selectedYear = selectedYear,
            selectedMonth = selectedMonth,
            onMonthClick = { viewModel.setShowMonthPicker(true) },
            onSettingsClick = { viewModel.setShowSettings(true) }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                BalanceHero(
                    overallBalance = balances.overallBalance,
                    monthlyIncome = monthlySummary.totalIncome,
                    monthlyExpense = monthlySummary.totalExpense,
                    dateRangeText = monthlySummary.dateRangeText,
                    transfersMoved = monthlySummary.totalTransfersMoved
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader(title = "Accounts")
                    AccountCardsRow(
                        balances = balances,
                        onAccountClick = { account -> viewModel.openAccountDetail(account) }
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader(title = "Quick Add")
                    QuickAddChips(
                        onAddExpense = { onNavigateToAdd(TransactionType.EXPENSE) },
                        onAddIncome = { onNavigateToAdd(TransactionType.INCOME) },
                        onTransfer = { onNavigateToAdd(TransactionType.TRANSFER) }
                    )
                }
            }

            item {
                SectionHeader(
                    title = "Activity",
                    actionLabel = if (transactions.isEmpty()) null else "${transactions.size} records"
                )
            }

            if (transactions.isEmpty()) {
                item {
                    EmptyActivityView(
                        onAddExpense = { onNavigateToAdd(TransactionType.EXPENSE) }
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
private fun BalanceHero(
    overallBalance: Double,
    monthlyIncome: Double,
    monthlyExpense: Double,
    dateRangeText: String,
    transfersMoved: Double
) {
    HeroCard {
        Column(modifier = Modifier.padding(22.dp)) {
            Text(
                text = "Overall Balance",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = AccentSoft,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = CurrencyFormatter.format(overallBalance),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = (-1).sp
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = dateRangeText,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextSecondary,
                    fontSize = 11.5.sp
                )
            )

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = Color(0x1FFFFFFF), thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                StatBlock(
                    label = "Income",
                    value = "+ ${CurrencyFormatter.format(monthlyIncome)}",
                    accent = IncomeGreen,
                    modifier = Modifier.weight(1f)
                )
                StatBlock(
                    label = "Expense",
                    value = "− ${CurrencyFormatter.format(monthlyExpense)}",
                    accent = ExpenseRed,
                    modifier = Modifier.weight(1f),
                    alignEnd = true
                )
            }

            if (transfersMoved > 0.0) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transfers this month",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextMuted,
                            fontSize = 11.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = CurrencyFormatter.format(transfersMoved),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TransferBlueGrey,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickAddChips(
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    onTransfer: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillChip(
                label = "Expense",
                icon = Icons.Default.ArrowDownward,
                accent = ExpenseRed,
                onClick = onAddExpense,
                modifier = Modifier.weight(1f)
            )
            PillChip(
                label = "Income",
                icon = Icons.Default.ArrowUpward,
                accent = IncomeGreen,
                onClick = onAddIncome,
                modifier = Modifier.weight(1f)
            )
        }
        PillChip(
            label = "Transfer between accounts",
            icon = Icons.Default.SwapHoriz,
            accent = TransferBlueGrey,
            onClick = onTransfer,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun EmptyActivityView(
    onAddExpense: () -> Unit
) {
    AppCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Nothing recorded yet",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Add your first transaction to start tracking this month.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 12.5.sp
                ),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            GradientPillButton(
                text = "Add Transaction",
                leadingIcon = Icons.Default.Add,
                onClick = onAddExpense
            )
        }
    }
}

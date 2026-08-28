package com.example.moneyminder.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.moneyminder.data.model.AccountType
import com.example.moneyminder.data.model.CategorySpending
import com.example.moneyminder.data.model.TransactionEntity
import com.example.moneyminder.data.model.TransactionType
import com.example.moneyminder.theme.BackgroundDark
import com.example.moneyminder.theme.BankAccent
import com.example.moneyminder.theme.CardBackground
import com.example.moneyminder.theme.CardBackgroundElevated
import com.example.moneyminder.theme.CardBorder
import com.example.moneyminder.theme.CardBorderSubtle
import com.example.moneyminder.theme.CashAccent
import com.example.moneyminder.theme.ExpenseRed
import com.example.moneyminder.theme.IncomeGreen
import com.example.moneyminder.theme.TextDisabled
import com.example.moneyminder.theme.TextMuted
import com.example.moneyminder.theme.TextPrimary
import com.example.moneyminder.theme.TextSecondary
import com.example.moneyminder.theme.TransferBlueGrey
import com.example.moneyminder.theme.WalletAccent
import com.example.moneyminder.ui.components.DonutSpendingChart
import com.example.moneyminder.ui.components.MoneyMinderHeader
import com.example.moneyminder.ui.components.TransactionCard
import com.example.moneyminder.ui.viewmodel.MainViewModel
import com.example.moneyminder.util.CurrencyFormatter
import java.util.Calendar

@Composable
fun InsightsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedYear by viewModel.selectedYear.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedAccount by viewModel.selectedAccountFilter.collectAsState()
    val balances by viewModel.balances.collectAsState()
    val monthlySummary by viewModel.monthlySummary.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()

    val categorySpendings = remember(selectedYear, selectedMonth, selectedAccount, allTransactions) {
        viewModel.dao.getCategorySpendings(selectedYear, selectedMonth, selectedAccount)
    }

    val accountMovements = remember(selectedYear, selectedMonth, allTransactions) {
        viewModel.dao.getAccountMovements(selectedYear, selectedMonth)
    }

    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }

    val daysInMonth = remember(selectedYear, selectedMonth) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, selectedYear)
        cal.set(Calendar.MONTH, selectedMonth - 1)
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    val dailyAverageSpending = if (daysInMonth > 0) monthlySummary.totalExpense / daysInMonth else 0.0
    val topSpendingCategory = categorySpendings.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        MoneyMinderHeader(
            title = "Insights",
            selectedYear = selectedYear,
            selectedMonth = selectedMonth,
            onMonthClick = { viewModel.setShowMonthPicker(true) },
            onSettingsClick = { viewModel.setShowSettings(true) }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Account Filter Tabs (Overall, Bank, Wallet, Cash)
            item {
                AccountFilterBar(
                    selectedAccount = selectedAccount,
                    onSelect = { viewModel.setAccountFilter(it) }
                )
            }

            // 1. Monthly Financial Balance & Net Summary Card
            item {
                MonthlyBalanceOverviewCard(
                    monthlyIncome = monthlySummary.totalIncome,
                    monthlyExpense = monthlySummary.totalExpense,
                    netBalance = monthlySummary.netBalance,
                    balances = balances,
                    selectedAccount = selectedAccount
                )
            }

            // 2. Spending Donut Chart Card (Transfers strictly excluded)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardBackground)
                        .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Expense Distribution",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "Self-transfers excluded",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextMuted,
                                    fontSize = 10.5.sp
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        DonutSpendingChart(
                            categorySpendings = categorySpendings,
                            totalSpending = monthlySummary.totalExpense,
                            onCategoryClick = { catName ->
                                selectedCategoryFilter = catName
                            }
                        )
                    }
                }
            }

            // 3. Top Spending & Daily Average Metrics
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Top Category Card
                    MetricCard(
                        title = "Top Category",
                        value = topSpendingCategory?.categoryName ?: "None",
                        subValue = topSpendingCategory?.let { CurrencyFormatter.format(it.totalAmount) } ?: "₹0",
                        icon = Icons.Default.Category,
                        accentColor = ExpenseRed,
                        modifier = Modifier.weight(1f)
                    )

                    // Daily Average Card
                    MetricCard(
                        title = "Daily Average",
                        value = CurrencyFormatter.format(dailyAverageSpending),
                        subValue = "across $daysInMonth days",
                        icon = Icons.Default.DateRange,
                        accentColor = TransferBlueGrey,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 4. Income vs Expense Comparison Bar
            item {
                IncomeVsExpenseBarCard(
                    income = monthlySummary.totalIncome,
                    expense = monthlySummary.totalExpense
                )
            }

            // 5. Account Transfers Movement Card (Bank <-> Wallet <-> Cash)
            item {
                AccountMovementsCard(
                    movements = accountMovements,
                    totalMoved = monthlySummary.totalTransfersMoved
                )
            }
        }
    }

    // Modal when tapping a category slice to view matching transactions
    if (selectedCategoryFilter != null) {
        val catTxs = allTransactions.filter { tx ->
            tx.category.equals(selectedCategoryFilter, ignoreCase = true)
        }

        Dialog(onDismissRequest = { selectedCategoryFilter = null }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .fillMaxHeight(0.8f),
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
                        Text(
                            text = "${selectedCategoryFilter} Expenses",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        IconButton(onClick = { selectedCategoryFilter = null }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(catTxs, key = { it.id }) { tx ->
                            TransactionCard(
                                transaction = tx,
                                onClick = {
                                    selectedCategoryFilter = null
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
private fun AccountFilterBar(
    selectedAccount: AccountType,
    onSelect: (AccountType) -> Unit
) {
    val accounts = listOf(
        AccountType.OVERALL,
        AccountType.BANK,
        AccountType.WALLET,
        AccountType.CASH
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(accounts) { acc ->
            val isSelected = selectedAccount == acc
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) TextPrimary else CardBackground)
                    .border(1.dp, if (isSelected) TextPrimary else CardBorder, RoundedCornerShape(16.dp))
                    .clickable { onSelect(acc) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = acc.displayName,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.Black else TextSecondary
                    )
                )
            }
        }
    }
}

@Composable
private fun MonthlyBalanceOverviewCard(
    monthlyIncome: Double,
    monthlyExpense: Double,
    netBalance: Double,
    balances: com.example.moneyminder.data.model.AccountBalances,
    selectedAccount: AccountType
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF202028), Color(0xFF14141A))))
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Column {
            Text(
                text = if (selectedAccount == AccountType.OVERALL) "Net Monthly Cash Flow" else "${selectedAccount.displayName} Net Flow",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (netBalance >= 0) "+ ${CurrencyFormatter.format(netBalance)}" else "− ${CurrencyFormatter.format(Math.abs(netBalance))}",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (netBalance >= 0) IncomeGreen else ExpenseRed,
                    fontSize = 26.sp
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Sub-metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Income", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
                    Text("+ ${CurrencyFormatter.format(monthlyIncome)}", style = MaterialTheme.typography.titleMedium.copy(color = IncomeGreen, fontWeight = FontWeight.Bold))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Expense", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
                    Text("− ${CurrencyFormatter.format(monthlyExpense)}", style = MaterialTheme.typography.titleMedium.copy(color = ExpenseRed, fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subValue: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(CardBackground)
            .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                ),
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subValue,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 11.sp
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun IncomeVsExpenseBarCard(
    income: Double,
    expense: Double
) {
    val total = income + expense
    val incomePercent = if (total > 0) (income / total).toFloat() else 0.5f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardBackground)
            .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
            .padding(18.dp)
    ) {
        Column {
            Text(
                text = "Income vs Expense Ratio",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Two-tone progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(ExpenseRed)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(incomePercent)
                        .fillMaxHeight()
                        .background(IncomeGreen)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Income: ${(incomePercent * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(color = IncomeGreen, fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "Expense: ${((1f - incomePercent) * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(color = ExpenseRed, fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

@Composable
private fun AccountMovementsCard(
    movements: List<com.example.moneyminder.data.model.AccountMovement>,
    totalMoved: Double
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardBackground)
            .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
            .padding(18.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Account Transfers",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = "${CurrencyFormatter.format(totalMoved)} total",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (movements.isEmpty()) {
                Text(
                    text = "No transfers recorded this month.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    movements.forEach { m ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(CardBackgroundElevated)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${m.fromAccount.displayName} → ${m.toAccount.displayName}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = CurrencyFormatter.format(m.totalTransferred),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

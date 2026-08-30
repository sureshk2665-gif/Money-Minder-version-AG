package com.example.moneyminder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.moneyminder.data.model.DaySummary
import com.example.moneyminder.data.model.TransactionEntity
import com.example.moneyminder.data.model.TransactionType
import com.example.moneyminder.theme.BackgroundDark
import com.example.moneyminder.theme.CardBackground
import com.example.moneyminder.theme.CardBackgroundElevated
import com.example.moneyminder.theme.CardBorder
import com.example.moneyminder.theme.CardBorderSubtle
import com.example.moneyminder.theme.ExpenseRed
import com.example.moneyminder.theme.IncomeGreen
import com.example.moneyminder.theme.TextMuted
import com.example.moneyminder.theme.TextPrimary
import com.example.moneyminder.theme.TextSecondary
import com.example.moneyminder.theme.TransferBlueGrey
import com.example.moneyminder.ui.components.DonutSpendingChart
import com.example.moneyminder.ui.components.MoneyMinderHeader
import com.example.moneyminder.ui.components.TransactionCard
import com.example.moneyminder.ui.viewmodel.MainViewModel
import com.example.moneyminder.util.CurrencyFormatter
import com.example.moneyminder.util.DateTimeUtils
import java.util.Calendar

private val insightTabs = listOf("Overview", "Calendar", "Daily", "Monthly", "Total", "Note")
private val weekDays = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")

@Composable
fun InsightsScreen(
    viewModel: MainViewModel,
    onNavigateToAdd: (timestamp: Long) -> Unit = {},
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

    val daySummaries = remember(selectedYear, selectedMonth, selectedAccount, allTransactions) {
        viewModel.dao.getDaySummaries(selectedYear, selectedMonth, selectedAccount)
    }

    var activeTab by remember { mutableStateOf("Overview") }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var selectedDayModal by remember { mutableStateOf<DaySummary?>(null) }

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

        // Month Navigation Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { viewModel.prevMonth() },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CardBackground)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous Month",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBackground)
                        .clickable { viewModel.setShowMonthPicker(true) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = DateTimeUtils.formatMonthYear(selectedYear, selectedMonth),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                IconButton(
                    onClick = { viewModel.nextMonth() },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CardBackground)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next Month",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Account filter chip
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                AccountType.entries.filter { it != AccountType.OVERALL || selectedAccount == AccountType.OVERALL }.take(4).forEach { acc ->
                    val isSelected = selectedAccount == acc
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) CardBackgroundElevated else Color.Transparent)
                            .border(1.dp, if (isSelected) TextPrimary else CardBorderSubtle, RoundedCornerShape(10.dp))
                            .clickable { viewModel.setAccountFilter(acc) }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (acc == AccountType.OVERALL) "All" else acc.displayName.take(1),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) TextPrimary else TextMuted,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }

        // Sub-Tab Bar
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(insightTabs) { tab ->
                val isSelected = activeTab == tab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) CardBackgroundElevated else CardBackground)
                        .border(1.dp, if (isSelected) TextPrimary else CardBorder, RoundedCornerShape(14.dp))
                        .clickable { activeTab = tab }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) TextPrimary else TextSecondary,
                            fontSize = 11.5.sp
                        )
                    )
                }
            }
        }

        // Summary Metrics Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryPill(
                label = "Income",
                value = CurrencyFormatter.format(monthlySummary.totalIncome),
                color = IncomeGreen,
                modifier = Modifier.weight(1f)
            )
            SummaryPill(
                label = "Expense",
                value = CurrencyFormatter.format(monthlySummary.totalExpense),
                color = ExpenseRed,
                modifier = Modifier.weight(1f)
            )
            SummaryPill(
                label = "Net",
                value = CurrencyFormatter.format(monthlySummary.netBalance),
                color = if (monthlySummary.netBalance >= 0) IncomeGreen else ExpenseRed,
                modifier = Modifier.weight(1f)
            )
            SummaryPill(
                label = "Transfer",
                value = CurrencyFormatter.format(monthlySummary.totalTransfersMoved),
                color = TransferBlueGrey,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        when (activeTab) {
            "Overview" -> {
                OverviewContent(
                    categorySpendings = categorySpendings,
                    totalExpense = monthlySummary.totalExpense,
                    topSpendingCategory = topSpendingCategory,
                    dailyAverageSpending = dailyAverageSpending,
                    daysInMonth = daysInMonth,
                    monthlyIncome = monthlySummary.totalIncome,
                    monthlyExpense = monthlySummary.totalExpense,
                    netBalance = monthlySummary.netBalance,
                    accountMovements = accountMovements,
                    totalMoved = monthlySummary.totalTransfersMoved,
                    onCategoryClick = { selectedCategoryFilter = it }
                )
            }
            "Calendar" -> {
                CalendarGridView(
                    year = selectedYear,
                    month = selectedMonth,
                    daySummaries = daySummaries,
                    onDayClick = { selectedDayModal = it }
                )
            }
            "Daily" -> {
                DailyBreakdownView(
                    daySummaries = daySummaries.filter { it.transactions.isNotEmpty() },
                    onTransactionClick = { viewModel.openTransactionDetail(it) }
                )
            }
            "Monthly" -> {
                MonthlyBreakdownView(
                    transactions = allTransactions.filter { tx ->
                        val start = DateTimeUtils.getStartOfMonth(selectedYear, selectedMonth)
                        val end = DateTimeUtils.getEndOfMonth(selectedYear, selectedMonth)
                        tx.timestamp in start..end
                    },
                    onTransactionClick = { viewModel.openTransactionDetail(it) }
                )
            }
            "Total" -> {
                TotalStatsView(
                    monthlySummary = monthlySummary,
                    txCount = daySummaries.sumOf { it.transactions.size }
                )
            }
            "Note" -> {
                NotesJournalView(
                    transactions = allTransactions.filter { it.note.isNotBlank() },
                    onTransactionClick = { viewModel.openTransactionDetail(it) }
                )
            }
        }
    }

    // Category drill-down dialog
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
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
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

    // Day detail dialog
    if (selectedDayModal != null) {
        val day = selectedDayModal!!
        Dialog(onDismissRequest = { selectedDayModal = null }) {
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
                        Column {
                            Text(
                                text = DateTimeUtils.formatDate(day.dateMillis),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                            )
                            Text("${day.transactions.size} records", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                        }
                        IconButton(onClick = { selectedDayModal = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    if (day.transactions.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("No records on this day", style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted))
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        selectedDayModal = null
                                        onNavigateToAdd(day.dateMillis)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = TextPrimary, contentColor = Color.Black)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add Entry on This Date", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(day.transactions, key = { it.id }) { tx ->
                                TransactionCard(
                                    transaction = tx,
                                    onClick = {
                                        selectedDayModal = null
                                        viewModel.openTransactionDetail(tx)
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                selectedDayModal = null
                                onNavigateToAdd(day.dateMillis)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CardBackgroundElevated, contentColor = TextPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("+ Add Another on This Date")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CardBackground)
            .border(1.dp, CardBorderSubtle, RoundedCornerShape(14.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp))
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = color, fontSize = 11.sp),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun OverviewContent(
    categorySpendings: List<CategorySpending>,
    totalExpense: Double,
    topSpendingCategory: CategorySpending?,
    dailyAverageSpending: Double,
    daysInMonth: Int,
    monthlyIncome: Double,
    monthlyExpense: Double,
    netBalance: Double,
    accountMovements: List<com.example.moneyminder.data.model.AccountMovement>,
    totalMoved: Double,
    onCategoryClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Donut Chart (Primary)
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
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                        )
                        Text(
                            text = "Self-transfers excluded",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.5.sp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    DonutSpendingChart(
                        categorySpendings = categorySpendings,
                        totalSpending = totalExpense,
                        onCategoryClick = onCategoryClick
                    )
                }
            }
        }

        // Top Spending & Daily Average
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    title = "Top Category",
                    value = topSpendingCategory?.categoryName ?: "None",
                    subValue = topSpendingCategory?.let { CurrencyFormatter.format(it.totalAmount) } ?: "₹0",
                    icon = Icons.Default.Category,
                    accentColor = ExpenseRed,
                    modifier = Modifier.weight(1f)
                )
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

        // Income vs Expense
        item { IncomeVsExpenseBarCard(income = monthlyIncome, expense = monthlyExpense) }

        // Account Movements
        item { AccountMovementsCard(movements = accountMovements, totalMoved = totalMoved) }
    }
}

// ─── Calendar Grid ───

@Composable
private fun CalendarGridView(
    year: Int,
    month: Int,
    daySummaries: List<DaySummary>,
    onDayClick: (DaySummary) -> Unit
) {
    val cal = Calendar.getInstance()
    cal.set(Calendar.YEAR, year)
    cal.set(Calendar.MONTH, month - 1)
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            weekDays.forEach { w ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(w, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextMuted, fontSize = 11.sp))
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            items(firstDayOfWeek) {
                Box(modifier = Modifier.aspectRatio(0.85f))
            }
            items(daySummaries) { daySummary ->
                DayGridCell(daySummary = daySummary, onClick = { onDayClick(daySummary) })
            }
        }
    }
}

@Composable
private fun DayGridCell(daySummary: DaySummary, onClick: () -> Unit) {
    val hasTx = daySummary.transactions.isNotEmpty()
    Box(
        modifier = Modifier
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(10.dp))
            .background(if (hasTx) CardBackgroundElevated else CardBackground)
            .border(1.dp, if (hasTx) CardBorder else CardBorderSubtle, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = daySummary.dayOfMonth.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (hasTx) FontWeight.Bold else FontWeight.Normal,
                        color = if (hasTx) TextPrimary else TextMuted,
                        fontSize = 11.sp
                    )
                )
                if (daySummary.hasTransfers) {
                    Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(TransferBlueGrey))
                }
            }
            if (hasTx) {
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    if (daySummary.incomeTotal > 0) {
                        Text(
                            text = "+${CurrencyFormatter.formatPlain(daySummary.incomeTotal)}",
                            style = MaterialTheme.typography.labelSmall.copy(color = IncomeGreen, fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold),
                            maxLines = 1
                        )
                    }
                    if (daySummary.expenseTotal > 0) {
                        Text(
                            text = "−${CurrencyFormatter.formatPlain(daySummary.expenseTotal)}",
                            style = MaterialTheme.typography.labelSmall.copy(color = ExpenseRed, fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

// ─── Calendar sub-views ───

@Composable
private fun DailyBreakdownView(daySummaries: List<DaySummary>, onTransactionClick: (TransactionEntity) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(daySummaries) { day ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(DateTimeUtils.formatDate(day.dateMillis), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                day.transactions.forEach { tx ->
                    TransactionCard(transaction = tx, onClick = { onTransactionClick(tx) })
                }
            }
        }
    }
}

@Composable
private fun MonthlyBreakdownView(transactions: List<TransactionEntity>, onTransactionClick: (TransactionEntity) -> Unit) {
    val grouped = transactions.groupBy { it.category }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(grouped.entries.toList()) { entry ->
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardBackground)
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp)).padding(14.dp)
            ) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(entry.key, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                        Text("${entry.value.size} tx", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    entry.value.forEach { tx ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onTransactionClick(tx) }.padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(DateTimeUtils.formatDate(tx.timestamp), style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
                            Text(
                                CurrencyFormatter.format(tx.amount),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (tx.type == TransactionType.INCOME) IncomeGreen else ExpenseRed
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TotalStatsView(monthlySummary: com.example.moneyminder.data.model.MonthlySummary, txCount: Int) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CardBackground)
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp)).padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Total Statistics", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Transactions", color = TextSecondary); Text(txCount.toString(), fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Income", color = TextSecondary); Text("+ ${CurrencyFormatter.format(monthlySummary.totalIncome)}", fontWeight = FontWeight.Bold, color = IncomeGreen)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Expense", color = TextSecondary); Text("− ${CurrencyFormatter.format(monthlySummary.totalExpense)}", fontWeight = FontWeight.Bold, color = ExpenseRed)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Net Savings", color = TextSecondary); Text(CurrencyFormatter.format(monthlySummary.netBalance), fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            }
        }
    }
}

@Composable
private fun NotesJournalView(transactions: List<TransactionEntity>, onTransactionClick: (TransactionEntity) -> Unit) {
    if (transactions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No notes found in transactions.", color = TextMuted)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(transactions, key = { it.id }) { tx ->
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardBackground)
                        .border(1.dp, CardBorder, RoundedCornerShape(16.dp)).clickable { onTransactionClick(tx) }.padding(14.dp)
                ) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(tx.category, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(DateTimeUtils.formatDate(tx.timestamp), style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(tx.note, style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary))
                    }
                }
            }
        }
    }
}

// ─── Reusable cards ───

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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(title, style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary), maxLines = 1)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subValue, style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp), maxLines = 1)
        }
    }
}

@Composable
private fun IncomeVsExpenseBarCard(income: Double, expense: Double) {
    val total = income + expense
    val incomePercent = if (total > 0) (income / total).toFloat() else 0.5f
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CardBackground)
            .border(1.dp, CardBorder, RoundedCornerShape(18.dp)).padding(18.dp)
    ) {
        Column {
            Text("Income vs Expense Ratio", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
            Spacer(modifier = Modifier.height(14.dp))
            Box(modifier = Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)).background(ExpenseRed)) {
                Box(modifier = Modifier.fillMaxWidth(incomePercent).fillMaxHeight().background(IncomeGreen))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Income: ${(incomePercent * 100).toInt()}%", style = MaterialTheme.typography.labelSmall.copy(color = IncomeGreen, fontWeight = FontWeight.SemiBold))
                Text("Expense: ${((1f - incomePercent) * 100).toInt()}%", style = MaterialTheme.typography.labelSmall.copy(color = ExpenseRed, fontWeight = FontWeight.SemiBold))
            }
        }
    }
}

@Composable
private fun AccountMovementsCard(movements: List<com.example.moneyminder.data.model.AccountMovement>, totalMoved: Double) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CardBackground)
            .border(1.dp, CardBorder, RoundedCornerShape(18.dp)).padding(18.dp)
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Account Transfers", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                Text("${CurrencyFormatter.format(totalMoved)} total", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (movements.isEmpty()) {
                Text("No transfers recorded this month.", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    movements.forEach { m ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(CardBackgroundElevated)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${m.fromAccount.displayName} → ${m.toAccount.displayName}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary))
                            Text(CurrencyFormatter.format(m.totalTransferred), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextSecondary))
                        }
                    }
                }
            }
        }
    }
}

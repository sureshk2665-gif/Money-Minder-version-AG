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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.moneyminder.data.model.AccountType
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
import com.example.moneyminder.theme.TextDisabled
import com.example.moneyminder.theme.TextMuted
import com.example.moneyminder.theme.TextPrimary
import com.example.moneyminder.theme.TextSecondary
import com.example.moneyminder.theme.TransferBlueGrey
import com.example.moneyminder.ui.components.MoneyMinderHeader
import com.example.moneyminder.ui.components.TransactionCard
import com.example.moneyminder.ui.viewmodel.MainViewModel
import com.example.moneyminder.util.CurrencyFormatter
import com.example.moneyminder.util.DateTimeUtils
import java.util.Calendar

private val weekDays = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
private val calendarTabs = listOf("Daily", "Calendar", "Monthly", "Total", "Note")

@Composable
fun CalendarScreen(
    viewModel: MainViewModel,
    onNavigateToAdd: (timestamp: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedYear by viewModel.selectedYear.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedAccount by viewModel.selectedAccountFilter.collectAsState()
    val monthlySummary by viewModel.monthlySummary.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()

    var activeViewTab by remember { mutableStateOf("Calendar") }
    var searchQuery by remember { mutableStateOf("") }
    var showSearchRow by remember { mutableStateOf(false) }

    val daySummaries = remember(selectedYear, selectedMonth, selectedAccount, allTransactions) {
        viewModel.dao.getDaySummaries(selectedYear, selectedMonth, selectedAccount)
    }

    var selectedDayModal by remember { mutableStateOf<DaySummary?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // App Header
        MoneyMinderHeader(
            title = "Calendar",
            onSettingsClick = { viewModel.setShowSettings(true) }
        )

        // Month Navigation & Search Controls
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

            IconButton(
                onClick = { showSearchRow = !showSearchRow },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CardBackground)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = if (showSearchRow) TextPrimary else TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Search Bar if toggled
        if (showSearchRow) {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search transactions, notes, categories...", color = TextDisabled) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TextPrimary,
                        unfocusedBorderColor = CardBorder,
                        focusedContainerColor = CardBackground,
                        unfocusedContainerColor = CardBackground,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Top 4 Summary Metrics (Income, Expense, Net, Transfer)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CalendarSummaryItem(
                label = "Income",
                value = CurrencyFormatter.format(monthlySummary.totalIncome),
                color = IncomeGreen,
                modifier = Modifier.weight(1f)
            )
            CalendarSummaryItem(
                label = "Expense",
                value = CurrencyFormatter.format(monthlySummary.totalExpense),
                color = ExpenseRed,
                modifier = Modifier.weight(1f)
            )
            CalendarSummaryItem(
                label = "Net",
                value = CurrencyFormatter.format(monthlySummary.netBalance),
                color = if (monthlySummary.netBalance >= 0) IncomeGreen else ExpenseRed,
                modifier = Modifier.weight(1f)
            )
            CalendarSummaryItem(
                label = "Transfer",
                value = CurrencyFormatter.format(monthlySummary.totalTransfersMoved),
                color = TransferBlueGrey,
                modifier = Modifier.weight(1f)
            )
        }

        // 5 Views Sub-Tab Bar (Daily, Calendar, Monthly, Total, Note)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(calendarTabs) { tab ->
                val isSelected = activeViewTab == tab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) TextPrimary else CardBackground)
                        .border(1.dp, if (isSelected) TextPrimary else CardBorder, RoundedCornerShape(14.dp))
                        .clickable { activeViewTab = tab }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.Black else TextSecondary,
                            fontSize = 11.5.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Tab Content
        when (activeViewTab) {
            "Calendar" -> {
                CalendarGridView(
                    year = selectedYear,
                    month = selectedMonth,
                    daySummaries = daySummaries,
                    onDayClick = { daySummary ->
                        selectedDayModal = daySummary
                    }
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
                    transactions = allTransactions.filter { tx ->
                        tx.note.isNotBlank() && (searchQuery.isBlank() || tx.note.contains(searchQuery, ignoreCase = true))
                    },
                    onTransactionClick = { viewModel.openTransactionDetail(it) }
                )
            }
        }
    }

    // Modal when a day cell is tapped
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
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "${day.transactions.size} records",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                        IconButton(onClick = { selectedDayModal = null }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (day.transactions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No records on this day",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        val targetTime = day.dateMillis
                                        selectedDayModal = null
                                        onNavigateToAdd(targetTime)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = TextPrimary,
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add Entry on This Date", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
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
                                val targetTime = day.dateMillis
                                selectedDayModal = null
                                onNavigateToAdd(targetTime)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CardBackgroundElevated,
                                contentColor = TextPrimary
                            ),
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

    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0 for Sunday
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Week Days Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            weekDays.forEach { w ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = w,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Grid Cells
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // Empty offset cells for start of month
            items(firstDayOfWeek) {
                Box(modifier = Modifier.aspectRatio(0.85f))
            }

            // Day Cells
            items(daySummaries) { daySummary ->
                DayGridCell(
                    daySummary = daySummary,
                    onClick = { onDayClick(daySummary) }
                )
            }
        }
    }
}

@Composable
private fun DayGridCell(
    daySummary: DaySummary,
    onClick: () -> Unit
) {
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
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Day Number + Transfer Indicator Dot
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = daySummary.dayOfMonth.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (hasTx) FontWeight.Bold else FontWeight.Normal,
                        color = if (hasTx) TextPrimary else TextMuted,
                        fontSize = 11.sp
                    )
                )

                if (daySummary.hasTransfers) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(TransferBlueGrey)
                    )
                }
            }

            // Compact Income & Expense indicators
            if (hasTx) {
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    if (daySummary.incomeTotal > 0) {
                        Text(
                            text = "+${CurrencyFormatter.formatPlain(daySummary.incomeTotal)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = IncomeGreen,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            maxLines = 1
                        )
                    }
                    if (daySummary.expenseTotal > 0) {
                        Text(
                            text = "−${CurrencyFormatter.formatPlain(daySummary.expenseTotal)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ExpenseRed,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarSummaryItem(
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
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = color,
                    fontSize = 11.sp
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DailyBreakdownView(
    daySummaries: List<DaySummary>,
    onTransactionClick: (TransactionEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(daySummaries) { day ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = DateTimeUtils.formatDate(day.dateMillis),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                day.transactions.forEach { tx ->
                    TransactionCard(
                        transaction = tx,
                        onClick = { onTransactionClick(tx) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthlyBreakdownView(
    transactions: List<TransactionEntity>,
    onTransactionClick: (TransactionEntity) -> Unit
) {
    val grouped = transactions.groupBy { it.category }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(grouped.entries.toList()) { entry ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBackground)
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = entry.key,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "${entry.value.size} tx",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    entry.value.forEach { tx ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTransactionClick(tx) }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = DateTimeUtils.formatDate(tx.timestamp),
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                            )
                            Text(
                                text = CurrencyFormatter.format(tx.amount),
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
private fun TotalStatsView(
    monthlySummary: com.example.moneyminder.data.model.MonthlySummary,
    txCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardBackground)
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Total Statistics", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Transactions", color = TextSecondary)
                    Text(txCount.toString(), fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Income", color = TextSecondary)
                    Text("+ ${CurrencyFormatter.format(monthlySummary.totalIncome)}", fontWeight = FontWeight.Bold, color = IncomeGreen)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Expense", color = TextSecondary)
                    Text("− ${CurrencyFormatter.format(monthlySummary.totalExpense)}", fontWeight = FontWeight.Bold, color = ExpenseRed)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Net Savings", color = TextSecondary)
                    Text(CurrencyFormatter.format(monthlySummary.netBalance), fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            }
        }
    }
}

@Composable
private fun NotesJournalView(
    transactions: List<TransactionEntity>,
    onTransactionClick: (TransactionEntity) -> Unit
) {
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBackground)
                        .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                        .clickable { onTransactionClick(tx) }
                        .padding(14.dp)
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

package com.example.moneyminder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moneyminder.data.model.BudgetItem
import com.example.moneyminder.data.model.LentReturnItem
import com.example.moneyminder.theme.BackgroundDark
import com.example.moneyminder.theme.CardBackground
import com.example.moneyminder.theme.CardBackgroundElevated
import com.example.moneyminder.theme.CardBorder
import com.example.moneyminder.theme.CardBorderSubtle
import com.example.moneyminder.theme.ExpenseRed
import com.example.moneyminder.theme.IncomeGreen
import com.example.moneyminder.theme.IncomeGreenDark
import com.example.moneyminder.theme.TextPrimary
import com.example.moneyminder.theme.TextSecondary
import com.example.moneyminder.theme.TransferBlueGrey
import com.example.moneyminder.ui.components.MoneyMinderHeader
import com.example.moneyminder.ui.viewmodel.MainViewModel
import com.example.moneyminder.util.CurrencyFormatter
import com.example.moneyminder.util.DateTimeUtils

@Composable
fun BudgetScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedYear by viewModel.selectedYear.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val salary by viewModel.salary.collectAsState()
    val budgetItems by viewModel.budgetItems.collectAsState()
    val lentReturnItems by viewModel.lentReturnItems.collectAsState()

    LaunchedEffect(selectedYear, selectedMonth) {
        viewModel.loadBudgetData()
    }

    val totalPlanned = budgetItems.sumOf { it.amount }
    val balance = salary - totalPlanned

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        MoneyMinderHeader(
            title = "Budget",
            onSettingsClick = { viewModel.setShowSettings(true) }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Month Label
            item {
                Text(
                    text = DateTimeUtils.formatMonthYear(selectedYear, selectedMonth),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                )
            }

            // ─── Salary Budget Section ───
            item {
                SalaryBudgetSection(
                    salary = salary,
                    balance = balance,
                    budgetItems = budgetItems,
                    onSalaryChange = { viewModel.setSalary(it) },
                    onAddItem = { purpose, amount -> viewModel.addBudgetItem(purpose, amount) },
                    onToggleDone = { id, done -> viewModel.toggleBudgetDone(id, done) },
                    onRemove = { id -> viewModel.removeBudgetItem(id) }
                )
            }

            // ─── Incomes & Returns Section ───
            item {
                IncomesReturnsSection(
                    items = lentReturnItems,
                    onAddItem = { name, amount, type -> viewModel.addLentReturnItem(name, amount, type) },
                    onToggleDone = { id, done -> viewModel.toggleLentReturnDone(id, done) },
                    onRemove = { id -> viewModel.removeLentReturnItem(id) }
                )
            }
        }
    }
}

@Composable
private fun SalaryBudgetSection(
    salary: Double,
    balance: Double,
    budgetItems: List<BudgetItem>,
    onSalaryChange: (Double) -> Unit,
    onAddItem: (String, Double) -> Unit,
    onToggleDone: (Long, Boolean) -> Unit,
    onRemove: (Long) -> Unit
) {
    var salaryText by remember(salary) {
        mutableStateOf(if (salary > 0) salary.toLong().toString() else "")
    }
    var newPurpose by remember { mutableStateOf("") }
    var newAmount by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBackground)
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column {
            // Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Salary Budget",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (balance >= 0) IncomeGreenDark.copy(alpha = 0.3f) else ExpenseRed.copy(alpha = 0.3f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (balance >= 0) "ON TRACK" else "OVER",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (balance >= 0) IncomeGreen else ExpenseRed,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Total Salary Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Salary",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.width(100.dp)
                )
                OutlinedTextField(
                    value = salaryText,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() }
                        salaryText = filtered
                        filtered.toDoubleOrNull()?.let { onSalaryChange(it) }
                    },
                    singleLine = true,
                    prefix = {
                        Text("₹ ", style = MaterialTheme.typography.bodyLarge.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        ))
                    },
                    placeholder = { Text("Enter salary", color = TextSecondary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IncomeGreen,
                        unfocusedBorderColor = CardBorderSubtle,
                        focusedContainerColor = CardBackgroundElevated,
                        unfocusedContainerColor = CardBackgroundElevated,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(52.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Balance Display Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Balance",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.width(100.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBackgroundElevated)
                        .border(1.dp, CardBorderSubtle, RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "₹ ${CurrencyFormatter.formatPlain(balance)}",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Add new budget item row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newPurpose,
                    onValueChange = { newPurpose = it },
                    singleLine = true,
                    placeholder = { Text("Purpose", color = TextSecondary, fontSize = 13.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TextPrimary,
                        unfocusedBorderColor = CardBorderSubtle,
                        focusedContainerColor = CardBackgroundElevated,
                        unfocusedContainerColor = CardBackgroundElevated,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                )
                OutlinedTextField(
                    value = newAmount,
                    onValueChange = { newAmount = it.filter { c -> c.isDigit() || c == '.' } },
                    singleLine = true,
                    prefix = { Text("₹", color = TextSecondary, fontSize = 13.sp) },
                    placeholder = { Text("Amount", color = TextSecondary, fontSize = 13.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TextPrimary,
                        unfocusedBorderColor = CardBorderSubtle,
                        focusedContainerColor = CardBackgroundElevated,
                        unfocusedContainerColor = CardBackgroundElevated,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(0.7f).height(48.dp)
                )

                // Plus button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(IncomeGreen)
                        .clickable {
                            val amt = newAmount.toDoubleOrNull()
                            if (newPurpose.isNotBlank() && amt != null && amt > 0) {
                                onAddItem(newPurpose.trim(), amt)
                                newPurpose = ""
                                newAmount = ""
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            if (budgetItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                // Budget items list
                budgetItems.forEach { item ->
                    BudgetItemRow(
                        item = item,
                        onToggleDone = { onToggleDone(item.id, !item.isDone) },
                        onRemove = { onRemove(item.id) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Summary
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CardBackgroundElevated)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${budgetItems.size} planned items",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                    Text(
                        text = "Total: ₹${CurrencyFormatter.formatPlain(budgetItems.sumOf { it.amount })}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetItemRow(
    item: BudgetItem,
    onToggleDone: () -> Unit,
    onRemove: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (item.isDone) IncomeGreenDark.copy(alpha = 0.15f)
                    else CardBackgroundElevated
                )
                .border(
                    1.dp,
                    if (item.isDone) IncomeGreen.copy(alpha = 0.3f) else CardBorderSubtle,
                    RoundedCornerShape(12.dp)
                )
                .pointerInput(Unit) {
                    detectTapGestures(onLongPress = { showMenu = true })
                }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                if (item.isDone) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Done",
                        tint = IncomeGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = item.purpose,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = if (item.isDone) IncomeGreen else TextPrimary,
                        textDecoration = if (item.isDone) TextDecoration.LineThrough else TextDecoration.None
                    )
                )
            }
            Text(
                text = "₹${CurrencyFormatter.formatPlain(item.amount)}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (item.isDone) IncomeGreen.copy(alpha = 0.7f) else ExpenseRed
                )
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            offset = DpOffset(x = 100.dp, y = 0.dp),
            modifier = Modifier.background(CardBackgroundElevated)
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        if (item.isDone) "Undo Done" else "Done",
                        color = IncomeGreen,
                        fontWeight = FontWeight.Bold
                    )
                },
                onClick = {
                    showMenu = false
                    onToggleDone()
                }
            )
            DropdownMenuItem(
                text = { Text("Remove", color = ExpenseRed, fontWeight = FontWeight.Bold) },
                onClick = {
                    showMenu = false
                    onRemove()
                }
            )
        }
    }
}

@Composable
private fun IncomesReturnsSection(
    items: List<LentReturnItem>,
    onAddItem: (String, Double, String) -> Unit,
    onToggleDone: (Long, Boolean) -> Unit,
    onRemove: (Long) -> Unit
) {
    var newName by remember { mutableStateOf("") }
    var newAmount by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("LENT") }

    val lentItems = items.filter { it.type == "LENT" }
    val returnItems = items.filter { it.type == "RETURN" }
    val totalLent = lentItems.sumOf { it.amount }
    val totalReturned = returnItems.sumOf { it.amount }
    val totalRecovered = lentItems.filter { it.isReturned }.sumOf { it.amount }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBackground)
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column {
            // Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Incomes & Returns",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                if (items.isNotEmpty()) {
                    Text(
                        text = "${items.size} entries",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Track money lent to friends or returns received",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Type toggle: LENT / RETURN
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("LENT" to "Lent", "RETURN" to "Return").forEach { (type, label) ->
                    val isSelected = selectedType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) CardBackgroundElevated else Color.Transparent)
                            .border(
                                1.dp,
                                if (isSelected) {
                                    if (type == "LENT") TransferBlueGrey else IncomeGreen
                                } else CardBorderSubtle,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedType = type }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) {
                                    if (type == "LENT") TransferBlueGrey else IncomeGreen
                                } else TextSecondary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Add new entry row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    placeholder = { Text("Friend name", color = TextSecondary, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (selectedType == "LENT") TransferBlueGrey else IncomeGreen,
                        unfocusedBorderColor = CardBorderSubtle,
                        focusedContainerColor = CardBackgroundElevated,
                        unfocusedContainerColor = CardBackgroundElevated,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                )
                OutlinedTextField(
                    value = newAmount,
                    onValueChange = { newAmount = it.filter { c -> c.isDigit() || c == '.' } },
                    singleLine = true,
                    prefix = { Text("₹", color = TextSecondary, fontSize = 13.sp) },
                    placeholder = { Text("Amount", color = TextSecondary, fontSize = 13.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (selectedType == "LENT") TransferBlueGrey else IncomeGreen,
                        unfocusedBorderColor = CardBorderSubtle,
                        focusedContainerColor = CardBackgroundElevated,
                        unfocusedContainerColor = CardBackgroundElevated,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(0.7f).height(48.dp)
                )

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (selectedType == "LENT") TransferBlueGrey else IncomeGreen)
                        .clickable {
                            val amt = newAmount.toDoubleOrNull()
                            if (newName.isNotBlank() && amt != null && amt > 0) {
                                onAddItem(newName.trim(), amt, selectedType)
                                newName = ""
                                newAmount = ""
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }

            // Lent items
            if (lentItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Money Lent",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TransferBlueGrey
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                lentItems.forEach { item ->
                    LentReturnItemRow(
                        item = item,
                        onToggleDone = { onToggleDone(item.id, !item.isReturned) },
                        onRemove = { onRemove(item.id) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            // Return items
            if (returnItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Returns Received",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = IncomeGreen
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                returnItems.forEach { item ->
                    LentReturnItemRow(
                        item = item,
                        onToggleDone = { onToggleDone(item.id, !item.isReturned) },
                        onRemove = { onRemove(item.id) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            // Summary
            if (items.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CardBackgroundElevated)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Total Lent", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                        Text(
                            "₹${CurrencyFormatter.formatPlain(totalLent)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TransferBlueGrey)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Recovered", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                        Text(
                            "₹${CurrencyFormatter.formatPlain(totalRecovered)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = IncomeGreen)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Returns", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                        Text(
                            "₹${CurrencyFormatter.formatPlain(totalReturned)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = IncomeGreen)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LentReturnItemRow(
    item: LentReturnItem,
    onToggleDone: () -> Unit,
    onRemove: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val isLent = item.type == "LENT"
    val accentColor = if (isLent) TransferBlueGrey else IncomeGreen

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (item.isReturned) IncomeGreenDark.copy(alpha = 0.15f)
                    else CardBackgroundElevated
                )
                .border(
                    1.dp,
                    if (item.isReturned) IncomeGreen.copy(alpha = 0.3f) else CardBorderSubtle,
                    RoundedCornerShape(12.dp)
                )
                .pointerInput(Unit) {
                    detectTapGestures(onLongPress = { showMenu = true })
                }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                if (item.isReturned) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Done", tint = IncomeGreen, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    Icon(Icons.Default.Person, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = item.personName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = if (item.isReturned) IncomeGreen else TextPrimary,
                        textDecoration = if (item.isReturned) TextDecoration.LineThrough else TextDecoration.None
                    )
                )
            }
            Text(
                text = "₹${CurrencyFormatter.formatPlain(item.amount)}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (item.isReturned) IncomeGreen.copy(alpha = 0.7f) else accentColor
                )
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            offset = DpOffset(x = 100.dp, y = 0.dp),
            modifier = Modifier.background(CardBackgroundElevated)
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        if (item.isReturned) "Undo" else if (isLent) "Returned" else "Done",
                        color = IncomeGreen,
                        fontWeight = FontWeight.Bold
                    )
                },
                onClick = {
                    showMenu = false
                    onToggleDone()
                }
            )
            DropdownMenuItem(
                text = { Text("Remove", color = ExpenseRed, fontWeight = FontWeight.Bold) },
                onClick = {
                    showMenu = false
                    onRemove()
                }
            )
        }
    }
}

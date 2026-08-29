package com.example.moneyminder.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moneyminder.data.model.AccountBalances
import com.example.moneyminder.data.model.AccountType
import com.example.moneyminder.data.model.CategoryEntity
import com.example.moneyminder.data.model.TransactionType
import com.example.moneyminder.theme.BackgroundDark
import com.example.moneyminder.theme.CardBackground
import com.example.moneyminder.theme.CardBackgroundElevated
import com.example.moneyminder.theme.CardBorder
import com.example.moneyminder.theme.CardBorderSubtle
import com.example.moneyminder.theme.ExpensePillBg
import com.example.moneyminder.theme.ExpenseRed
import com.example.moneyminder.theme.IncomeGreen
import com.example.moneyminder.theme.IncomePillBg
import com.example.moneyminder.theme.TextDisabled
import com.example.moneyminder.theme.TextMuted
import com.example.moneyminder.theme.TextPrimary
import com.example.moneyminder.theme.TextSecondary
import com.example.moneyminder.theme.TransferBlueGrey
import com.example.moneyminder.theme.TransferPillBg
import com.example.moneyminder.ui.components.AppDatePickerDialog
import com.example.moneyminder.ui.components.AppTimePickerDialog
import com.example.moneyminder.ui.components.MoneyMinderHeader
import com.example.moneyminder.ui.viewmodel.MainViewModel
import com.example.moneyminder.util.CurrencyFormatter
import com.example.moneyminder.util.DateTimeUtils
import kotlinx.coroutines.delay
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionScreen(
    viewModel: MainViewModel,
    initialType: TransactionType = TransactionType.EXPENSE,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val balances by viewModel.balances.collectAsState()
    val prefilledTx by viewModel.prefilledTransaction.collectAsState()

    var selectedType by remember { mutableStateOf(initialType) }
    var amountText by remember { mutableStateOf("") }
    var categoryText by remember { mutableStateOf("") }
    var fromAccount by remember { mutableStateOf(AccountType.BANK) }
    var toAccount by remember { mutableStateOf(AccountType.BANK) }
    var timestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var noteText by remember { mutableStateOf("") }
    var referenceNumber by remember { mutableStateOf<String?>(null) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showSuccessAnim by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var savedCategories by remember { mutableStateOf<List<CategoryEntity>>(emptyList()) }

    // Apply pending transaction type from quick actions
    val pendingType by viewModel.pendingTransactionType.collectAsState()
    LaunchedEffect(pendingType) {
        pendingType?.let { type ->
            selectedType = type
            viewModel.clearPendingTransactionType()
        }
    }

    // Load pre-filled transaction if available
    LaunchedEffect(prefilledTx) {
        prefilledTx?.let { tx ->
            selectedType = tx.type
            amountText = if (tx.amount > 0) CurrencyFormatter.formatPlain(tx.amount) else ""
            categoryText = tx.category
            if (tx.fromAccount != null) fromAccount = tx.fromAccount
            if (tx.toAccount != null) toAccount = tx.toAccount
            if (tx.type == TransactionType.INCOME && toAccount == AccountType.WALLET) {
                toAccount = AccountType.BANK
            }
            timestamp = tx.timestamp
            noteText = tx.note
            referenceNumber = tx.referenceNumber
        }
    }

    fun getAllowedTransferDestinations(from: AccountType): List<AccountType> = when (from) {
        AccountType.BANK -> listOf(AccountType.WALLET, AccountType.CASH)
        AccountType.WALLET -> listOf(AccountType.BANK)
        AccountType.CASH -> listOf(AccountType.BANK)
        AccountType.OVERALL -> emptyList()
    }

    // Auto-correct transfer destination when source changes
    LaunchedEffect(fromAccount, selectedType) {
        if (selectedType == TransactionType.TRANSFER) {
            val allowed = getAllowedTransferDestinations(fromAccount)
            if (toAccount !in allowed) {
                toAccount = allowed.firstOrNull() ?: AccountType.BANK
            }
        }
    }

    // Refresh saved categories when type changes
    LaunchedEffect(selectedType) {
        if (selectedType != TransactionType.TRANSFER) {
            savedCategories = viewModel.getSavedCategories(selectedType)
        }
    }

    val scrollState = rememberScrollState()

    fun resetForm() {
        amountText = ""
        categoryText = ""
        noteText = ""
        referenceNumber = null
        timestamp = System.currentTimeMillis()
        errorMessage = null
    }

    fun submitTransaction(addAnother: Boolean) {
        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            errorMessage = "Please enter a valid amount"
            return
        }

        if (selectedType == TransactionType.TRANSFER) {
            if (fromAccount == toAccount) {
                errorMessage = "Source and destination accounts cannot be the same"
                return
            }
        } else {
            if (categoryText.isBlank()) {
                errorMessage = "Please enter or select a category"
                return
            }
        }

        if (selectedType != TransactionType.INCOME) {
            val available = balances.getBalance(fromAccount)
            if (amount > available) {
                errorMessage = "Insufficient balance in ${fromAccount.displayName}. Available: ${CurrencyFormatter.format(available)}"
                return
            }
        }

        errorMessage = null

        val saveFrom = when (selectedType) {
            TransactionType.EXPENSE, TransactionType.TRANSFER -> fromAccount
            TransactionType.INCOME -> null
        }
        val saveTo = when (selectedType) {
            TransactionType.INCOME, TransactionType.TRANSFER -> toAccount
            TransactionType.EXPENSE -> null
        }

        viewModel.saveTransaction(
            type = selectedType,
            amount = amount,
            category = categoryText,
            fromAccount = saveFrom,
            toAccount = saveTo,
            timestamp = timestamp,
            note = noteText,
            referenceNumber = referenceNumber,
            onSuccess = {
                showSuccessAnim = true
                if (addAnother) {
                    resetForm()
                } else {
                    onCancel()
                }
            }
        )
    }

    LaunchedEffect(showSuccessAnim) {
        if (showSuccessAnim) {
            delay(1200)
            showSuccessAnim = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp)
        ) {
            // Header with Logo
            MoneyMinderHeader(
                title = "Add Transaction"
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Large 3-Option Segmented Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBackgroundElevated)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Expense Option (Dark Red 70% opacity)
                    SegmentedTabItem(
                        title = "Expense",
                        isSelected = selectedType == TransactionType.EXPENSE,
                        activeBg = ExpensePillBg,
                        activeBorder = ExpenseRed,
                        onClick = {
                            selectedType = TransactionType.EXPENSE
                            errorMessage = null
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // Income Option (Dark Green 70% opacity)
                    SegmentedTabItem(
                        title = "Income",
                        isSelected = selectedType == TransactionType.INCOME,
                        activeBg = IncomePillBg,
                        activeBorder = IncomeGreen,
                        onClick = {
                            selectedType = TransactionType.INCOME
                            errorMessage = null
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // Transfer Option (Dark Charcoal / Muted Slate 70% opacity)
                    SegmentedTabItem(
                        title = "Transfer",
                        isSelected = selectedType == TransactionType.TRANSFER,
                        activeBg = TransferPillBg,
                        activeBorder = TransferBlueGrey,
                        onClick = {
                            selectedType = TransactionType.TRANSFER
                            errorMessage = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // 2. Amount Input Card with Permanent ₹ Symbol OUTSIDE Text Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardBackground)
                        .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                        .padding(18.dp)
                ) {
                    Column {
                        Text(
                            text = "Amount",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = TextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Permanent ₹ Symbol placed outside text box
                            Text(
                                text = "₹",
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 34.sp,
                                    color = when (selectedType) {
                                        TransactionType.INCOME -> IncomeGreen
                                        TransactionType.EXPENSE -> ExpenseRed
                                        TransactionType.TRANSFER -> TextPrimary
                                    }
                                ),
                                modifier = Modifier.padding(end = 12.dp)
                            )

                            OutlinedTextField(
                                value = amountText,
                                onValueChange = {
                                    if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                        amountText = it
                                    }
                                },
                                placeholder = {
                                    Text(
                                        text = "0",
                                        style = MaterialTheme.typography.displayMedium.copy(
                                            color = TextDisabled,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                },
                                textStyle = MaterialTheme.typography.displayMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 3. Category Field (Only for Expense & Income)
                if (selectedType != TransactionType.TRANSFER) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(CardBackground)
                            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                            .padding(18.dp)
                    ) {
                        Column {
                            Text(
                                text = "Category",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = TextSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = categoryText,
                                onValueChange = { categoryText = it },
                                placeholder = { Text("e.g. Salary, LIC, Groceries, Dinner", color = TextDisabled) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TextPrimary,
                                    unfocusedBorderColor = CardBorder,
                                    focusedContainerColor = CardBackgroundElevated,
                                    unfocusedContainerColor = CardBackgroundElevated,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Saved Categories Chips (starts empty, remembers saved)
                            if (savedCategories.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Recent / Saved Categories",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    savedCategories.take(8).forEach { cat ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(CardBackgroundElevated)
                                                .border(1.dp, CardBorderSubtle, RoundedCornerShape(16.dp))
                                                .clickable { categoryText = cat.name }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = cat.name,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = TextSecondary,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Account Selectors
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardBackground)
                        .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        when (selectedType) {
                            TransactionType.EXPENSE -> {
                                Text(
                                    text = "Paid From",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = TextSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                AccountSelectorRow(
                                    selected = fromAccount,
                                    onSelect = { fromAccount = it }
                                )
                            }
                            TransactionType.INCOME -> {
                                Text(
                                    text = "Received In",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = TextSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                AccountSelectorRow(
                                    selected = toAccount,
                                    onSelect = { toAccount = it },
                                    accounts = listOf(AccountType.BANK, AccountType.CASH)
                                )
                            }
                            TransactionType.TRANSFER -> {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    // Transfer From + Balance Preview
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Transfer From",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                color = TextSecondary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                        Text(
                                            text = "Available: ${CurrencyFormatter.format(balances.getBalance(fromAccount))}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = TextMuted,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                    AccountSelectorRow(
                                        selected = fromAccount,
                                        onSelect = { fromAccount = it }
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Transfer To
                                    Text(
                                        text = "Transfer To",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = TextSecondary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                    AccountSelectorRow(
                                        selected = toAccount,
                                        onSelect = { toAccount = it },
                                        accounts = getAllowedTransferDestinations(fromAccount)
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. Date & Time Selection
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardBackground)
                        .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                        .padding(18.dp)
                ) {
                    Column {
                        Text(
                            text = "Date & Time",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = TextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Date Button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CardBackgroundElevated)
                                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                                    .clickable { showDatePicker = true }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = DateTimeUtils.formatDate(timestamp),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }

                            // Time Button (Opens Clock Dial)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CardBackgroundElevated)
                                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                                    .clickable { showTimePicker = true }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = DateTimeUtils.formatTime(timestamp),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // 6. Optional Note & Reference Number
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardBackground)
                        .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                        .padding(18.dp)
                ) {
                    Column {
                        Text(
                            text = "Optional Note",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = TextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            placeholder = { Text("Add payment details, purpose, or reference", color = TextDisabled) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TextPrimary,
                                unfocusedBorderColor = CardBorder,
                                focusedContainerColor = CardBackgroundElevated,
                                unfocusedContainerColor = CardBackgroundElevated,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Error Message if any
                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = ExpenseRed,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                // 7. Action Buttons
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    // Save Transaction Button
                    Button(
                        onClick = { submitTransaction(addAnother = false) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TextPrimary,
                            contentColor = Color.Black
                        )
                    ) {
                        Text(
                            text = "Save Transaction",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    // Save and Add Another Button
                    OutlinedButton(
                        onClick = { submitTransaction(addAnother = true) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                    ) {
                        Text("Save and Add Another", fontWeight = FontWeight.SemiBold)
                    }

                    // Cancel Button
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Transparent)
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }

        // Date Picker Modal
        if (showDatePicker) {
            AppDatePickerDialog(
                initialTimestamp = timestamp,
                onDateSelected = { selectedDateMillis ->
                    val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
                    val currentCal = Calendar.getInstance().apply { timeInMillis = timestamp }
                    cal.set(Calendar.HOUR_OF_DAY, currentCal.get(Calendar.HOUR_OF_DAY))
                    cal.set(Calendar.MINUTE, currentCal.get(Calendar.MINUTE))
                    timestamp = cal.timeInMillis
                },
                onDismiss = { showDatePicker = false }
            )
        }

        // Time Picker Modal (Clock Dial)
        if (showTimePicker) {
            AppTimePickerDialog(
                initialTimestamp = timestamp,
                onTimeSelected = { hour, minute ->
                    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
                    cal.set(Calendar.HOUR_OF_DAY, hour)
                    cal.set(Calendar.MINUTE, minute)
                    timestamp = cal.timeInMillis
                },
                onDismiss = { showTimePicker = false }
            )
        }

        // Success Checkmark Toast Overlay
        AnimatedVisibility(
            visible = showSuccessAnim,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xE61E1E26))
                    .border(1.dp, IncomeGreen.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(IncomeGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Transaction Saved!",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SegmentedTabItem(
    title: String,
    isSelected: Boolean,
    activeBg: Color,
    activeBorder: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) activeBg else Color.Transparent)
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = if (isSelected) activeBorder else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) TextPrimary else TextSecondary,
                fontSize = 13.sp
            )
        )
    }
}

@Composable
private fun AccountSelectorRow(
    selected: AccountType,
    onSelect: (AccountType) -> Unit,
    accounts: List<AccountType> = AccountType.primaryAccounts,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        accounts.forEach { acc ->
            val isSelected = selected == acc
            val icon = when (acc) {
                AccountType.BANK -> Icons.Default.AccountBalance
                AccountType.WALLET -> Icons.Default.AccountBalanceWallet
                AccountType.CASH -> Icons.Default.Payments
                AccountType.OVERALL -> Icons.Default.AccountBalance
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) CardBackgroundElevated else CardBackground)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) TextPrimary else CardBorderSubtle,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelect(acc) },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = acc.displayName,
                        tint = if (isSelected) TextPrimary else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = acc.displayName,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) TextPrimary else TextSecondary
                        )
                    )
                }
            }
        }
    }
}

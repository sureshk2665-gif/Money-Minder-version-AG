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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Remove
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
import com.example.moneyminder.data.model.LendReturnKind
import com.example.moneyminder.data.model.TransactionType
import com.example.moneyminder.theme.BackdropGradient
import com.example.moneyminder.theme.CardBackground
import com.example.moneyminder.theme.CardBackgroundElevated
import com.example.moneyminder.theme.CardBorder
import com.example.moneyminder.theme.CardBorderSubtle
import com.example.moneyminder.theme.ExpensePillBg
import com.example.moneyminder.theme.ExpenseRed
import com.example.moneyminder.theme.IncomeGreen
import com.example.moneyminder.theme.IncomePillBg
import com.example.moneyminder.theme.TextPrimary
import com.example.moneyminder.theme.TextSecondary
import com.example.moneyminder.theme.TransferBlueGrey
import com.example.moneyminder.theme.TransferPillBg
import com.example.moneyminder.theme.WalletAccent
import com.example.moneyminder.ui.components.AppDatePickerDialog
import com.example.moneyminder.ui.components.GradientPillButton
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

    var selectedTab by remember { mutableStateOf(EntryTab.fromType(initialType)) }
    var lendKind by remember { mutableStateOf(LendReturnKind.BORROWED) }
    var personName by remember { mutableStateOf("") }
    var lendAccount by remember { mutableStateOf(AccountType.BANK) }
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
    var categoryRefreshKey by remember { mutableStateOf(0) }
    var counterpartySuggestions by remember { mutableStateOf<List<String>>(emptyList()) }

    val isLendMode = selectedTab == EntryTab.LEND_RETURN
    val selectedType = when (selectedTab) {
        EntryTab.EXPENSE -> TransactionType.EXPENSE
        EntryTab.INCOME -> TransactionType.INCOME
        EntryTab.TRANSFER -> TransactionType.TRANSFER
        EntryTab.LEND_RETURN -> lendKind.transactionType
    }

    // Apply pending transaction type from quick actions
    val pendingType by viewModel.pendingTransactionType.collectAsState()
    LaunchedEffect(pendingType) {
        pendingType?.let { type ->
            selectedTab = EntryTab.fromType(type)
            viewModel.clearPendingTransactionType()
        }
    }

    // Load pre-filled transaction if available
    LaunchedEffect(prefilledTx) {
        prefilledTx?.let { tx ->
            selectedTab = EntryTab.fromType(tx.type)
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

    // Refresh saved categories when type changes or the list is edited
    LaunchedEffect(selectedType, categoryRefreshKey) {
        if (selectedType != TransactionType.TRANSFER) {
            savedCategories = viewModel.getSavedCategories(selectedType)
        }
    }

    LaunchedEffect(isLendMode, lendKind) {
        counterpartySuggestions = if (isLendMode) {
            viewModel.getCounterpartySuggestions(lendKind)
        } else {
            emptyList()
        }
    }

    val scrollState = rememberScrollState()

    fun resetForm() {
        amountText = ""
        categoryText = ""
        noteText = ""
        personName = ""
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

        if (isLendMode) {
            if (personName.isBlank()) {
                errorMessage = "Please enter the person's name"
                return
            }
            if (!lendKind.isMoneyIn) {
                val available = balances.getBalance(lendAccount)
                if (amount > available) {
                    errorMessage = "Insufficient balance in ${lendAccount.displayName}. Available: ${CurrencyFormatter.format(available)}"
                    return
                }
            }
            errorMessage = null
            viewModel.saveLendReturn(
                kind = lendKind,
                personName = personName,
                amount = amount,
                account = lendAccount,
                timestamp = timestamp,
                note = noteText,
                onSuccess = {
                    showSuccessAnim = true
                    if (addAnother) resetForm() else onCancel()
                }
            )
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
            .background(BackdropGradient)
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
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        isSelected = selectedTab == EntryTab.EXPENSE,
                        activeBg = ExpensePillBg,
                        activeBorder = ExpenseRed,
                        onClick = {
                            selectedTab = EntryTab.EXPENSE
                            errorMessage = null
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // Income Option (Dark Green 70% opacity)
                    SegmentedTabItem(
                        title = "Income",
                        isSelected = selectedTab == EntryTab.INCOME,
                        activeBg = IncomePillBg,
                        activeBorder = IncomeGreen,
                        onClick = {
                            selectedTab = EntryTab.INCOME
                            errorMessage = null
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // Transfer Option (Dark Charcoal / Muted Slate 70% opacity)
                    SegmentedTabItem(
                        title = "Transfer",
                        isSelected = selectedTab == EntryTab.TRANSFER,
                        activeBg = TransferPillBg,
                        activeBorder = TransferBlueGrey,
                        onClick = {
                            selectedTab = EntryTab.TRANSFER
                            errorMessage = null
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // Lend & Return Option
                    SegmentedTabItem(
                        title = "Lend",
                        isSelected = selectedTab == EntryTab.LEND_RETURN,
                        activeBg = WalletAccent.copy(alpha = 0.22f),
                        activeBorder = WalletAccent,
                        onClick = {
                            selectedTab = EntryTab.LEND_RETURN
                            errorMessage = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Lend & Return: direction + person
                if (isLendMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CardBackground)
                            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Column {
                            Text(
                                text = "What happened?",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = TextSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LendReturnKind.entries.forEach { kind ->
                                    val active = lendKind == kind
                                    val tint = if (kind.isMoneyIn) IncomeGreen else ExpenseRed
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (active) tint.copy(alpha = 0.18f) else CardBackgroundElevated)
                                            .border(
                                                1.dp,
                                                if (active) tint else CardBorderSubtle,
                                                RoundedCornerShape(14.dp)
                                            )
                                            .clickable {
                                                lendKind = kind
                                                errorMessage = null
                                            }
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = kind.displayName,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                color = if (active) tint else TextSecondary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = if (lendKind.isMoneyIn) {
                                    "Money comes in — recorded as Income"
                                } else {
                                    "Money goes out — recorded as Expense"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (lendKind.isMoneyIn) IncomeGreen else ExpenseRed,
                                    fontSize = 12.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Person",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = TextSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = personName,
                                onValueChange = { personName = it },
                                placeholder = { Text("Who? e.g. Ravi", color = TextSecondary) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = WalletAccent,
                                    unfocusedBorderColor = CardBorder,
                                    focusedContainerColor = CardBackgroundElevated,
                                    unfocusedContainerColor = CardBackgroundElevated,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Repaying or being repaid: offer the people who
                            // still have an open balance in that direction.
                            if (counterpartySuggestions.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (lendKind == LendReturnKind.RETURN_PAID) {
                                        "You still owe"
                                    } else {
                                        "Still owes you"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextSecondary,
                                        fontSize = 11.5.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    counterpartySuggestions.forEach { name ->
                                        val selected = personName.equals(name, ignoreCase = true)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(
                                                    if (selected) {
                                                        WalletAccent.copy(alpha = 0.18f)
                                                    } else {
                                                        CardBackgroundElevated
                                                    }
                                                )
                                                .border(
                                                    1.dp,
                                                    if (selected) WalletAccent else CardBorderSubtle,
                                                    RoundedCornerShape(16.dp)
                                                )
                                                .clickable { personName = name }
                                                .padding(horizontal = 12.dp, vertical = 7.dp)
                                        ) {
                                            Text(
                                                text = name,
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    color = if (selected) WalletAccent else TextPrimary,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 12.5.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Amount — label and field share one row to save height
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBackground)
                        .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "₹",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp,
                                color = when (selectedType) {
                                    TransactionType.INCOME -> IncomeGreen
                                    TransactionType.EXPENSE -> ExpenseRed
                                    TransactionType.TRANSFER -> TextPrimary
                                }
                            ),
                            modifier = Modifier.padding(end = 10.dp)
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
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 26.sp
                                    )
                                )
                            },
                            textStyle = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 26.sp
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

                // 3. Category Field (Only for Expense & Income)
                if (selectedType != TransactionType.TRANSFER && !isLendMode) {
                    CategoryPickerCard(
                        categoryText = categoryText,
                        savedCategories = savedCategories,
                        onCategorySelected = { categoryText = it },
                        onAddCategory = { name ->
                            viewModel.addCategory(name, selectedType) { added ->
                                if (added) {
                                    categoryText = name.trim()
                                    categoryRefreshKey++
                                }
                            }
                        },
                        onDeleteCategory = { cat ->
                            viewModel.deleteCategory(cat.id) {
                                if (categoryText.equals(cat.name, ignoreCase = true)) categoryText = ""
                                categoryRefreshKey++
                            }
                        }
                    )
                }

                // 4. Account Selectors
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBackground)
                        .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isLendMode) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (lendKind.isMoneyIn) "Received In" else "Paid From",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = TextSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                if (!lendKind.isMoneyIn) {
                                    Text(
                                        text = "Available: ${CurrencyFormatter.format(balances.getBalance(lendAccount))}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                            AccountSelectorRow(
                                selected = lendAccount,
                                onSelect = { lendAccount = it },
                                accounts = listOf(AccountType.BANK, AccountType.CASH)
                            )
                        } else when (selectedType) {
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
                                                color = TextSecondary,
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

                // 5. Date, time and note share one card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBackground)
                        .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Date Button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CardBackgroundElevated)
                                    .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                                    .clickable { showDatePicker = true }
                                    .padding(horizontal = 10.dp, vertical = 9.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = DateTimeUtils.formatDate(timestamp),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            }

                            // Time Button (Opens Clock Dial)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CardBackgroundElevated)
                                    .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                                    .clickable { showTimePicker = true }
                                    .padding(horizontal = 10.dp, vertical = 9.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = DateTimeUtils.formatTime(timestamp),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            placeholder = { Text("Note (optional)", color = TextSecondary, fontSize = 12.sp) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TextPrimary,
                                unfocusedBorderColor = CardBorder,
                                focusedContainerColor = CardBackgroundElevated,
                                unfocusedContainerColor = CardBackgroundElevated,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
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

                // 7. Action Buttons — secondary actions share a row to save height
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    GradientPillButton(
                        text = "Save Transaction",
                        onClick = { submitTransaction(addAnother = false) }
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { submitTransaction(addAnother = true) },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                        ) {
                            Text("Save & Add Another", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier
                                .weight(0.5f)
                                .height(42.dp),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Transparent)
                        ) {
                            Text("Cancel", fontSize = 13.sp)
                        }
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

enum class EntryTab {
    EXPENSE,
    INCOME,
    TRANSFER,
    LEND_RETURN;

    companion object {
        fun fromType(type: TransactionType): EntryTab = when (type) {
            TransactionType.EXPENSE -> EXPENSE
            TransactionType.INCOME -> INCOME
            TransactionType.TRANSFER -> TRANSFER
        }
    }
}

@Composable
private fun CategoryPickerCard(
    categoryText: String,
    savedCategories: List<CategoryEntity>,
    onCategorySelected: (String) -> Unit,
    onAddCategory: (String) -> Unit,
    onDeleteCategory: (CategoryEntity) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showAddField by remember { mutableStateOf(false) }
    var newCategoryText by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CardBackgroundElevated)
                            .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                            .clickable { expanded = true }
                            .padding(horizontal = 12.dp, vertical = 11.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = categoryText.ifBlank { "Select category" },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (categoryText.isBlank()) TextSecondary else TextPrimary,
                                    fontSize = 13.sp
                                )
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Open categories",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth(0.7f)
                    ) {
                        if (savedCategories.isEmpty()) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "No categories yet — tap + to add",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                    )
                                },
                                onClick = { expanded = false }
                            )
                        } else {
                            savedCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = cat.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
                                        )
                                    },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Remove,
                                            contentDescription = "Delete ${cat.name}",
                                            tint = ExpenseRed,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .clickable {
                                                    expanded = false
                                                    onDeleteCategory(cat)
                                                }
                                        )
                                    },
                                    onClick = {
                                        onCategorySelected(cat.name)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Green plus: add a new category
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(IncomeGreen.copy(alpha = 0.18f))
                        .border(1.dp, IncomeGreen, RoundedCornerShape(10.dp))
                        .clickable {
                            showAddField = !showAddField
                            if (!showAddField) newCategoryText = ""
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (showAddField) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = if (showAddField) "Cancel add category" else "Add category",
                        tint = IncomeGreen,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = showAddField,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = newCategoryText,
                            onValueChange = { newCategoryText = it },
                            placeholder = { Text("New category name", color = TextSecondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = IncomeGreen,
                                unfocusedBorderColor = CardBorderSubtle,
                                focusedContainerColor = CardBackgroundElevated,
                                unfocusedContainerColor = CardBackgroundElevated,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (newCategoryText.isNotBlank()) {
                                    onAddCategory(newCategoryText)
                                    newCategoryText = ""
                                    showAddField = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = IncomeGreen,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
            }
        }
    }
}

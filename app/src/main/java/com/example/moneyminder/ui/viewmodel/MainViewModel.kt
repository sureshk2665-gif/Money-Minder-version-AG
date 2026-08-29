package com.example.moneyminder.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneyminder.data.db.TransactionDao
import com.example.moneyminder.data.io.ExcelExporter
import com.example.moneyminder.data.io.ExcelImporter
import com.example.moneyminder.data.io.PdfExporter
import com.example.moneyminder.data.io.PdfImporter
import com.example.moneyminder.data.model.AccountBalances
import com.example.moneyminder.data.model.AccountMovement
import com.example.moneyminder.data.model.AccountType
import com.example.moneyminder.data.model.CategoryEntity
import com.example.moneyminder.data.model.CategorySpending
import com.example.moneyminder.data.model.DaySummary
import com.example.moneyminder.data.model.ImportItem
import com.example.moneyminder.data.model.MonthlySummary
import com.example.moneyminder.data.model.SmsCandidate
import com.example.moneyminder.data.model.TransactionEntity
import com.example.moneyminder.data.model.TransactionType
import com.example.moneyminder.data.parser.SmsParser
import com.example.moneyminder.util.DateTimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val dao = TransactionDao(application)

    // Current navigation tab: 0=Home, 1=Insights, 2=Add, 3=Calendar, 4=SMS
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Selected Month & Year context
    private val _selectedYear = MutableStateFlow(DateTimeUtils.getCurrentYear())
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    private val _selectedMonth = MutableStateFlow(DateTimeUtils.getCurrentMonth())
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    // Filter accounts on Insights and Calendar
    private val _selectedAccountFilter = MutableStateFlow(AccountType.OVERALL)
    val selectedAccountFilter: StateFlow<AccountType> = _selectedAccountFilter.asStateFlow()

    // Selected Account for Home Account Popup Timeline
    private val _selectedDetailAccount = MutableStateFlow<AccountType?>(null)
    val selectedDetailAccount: StateFlow<AccountType?> = _selectedDetailAccount.asStateFlow()

    private val _accountDetailTypeFilter = MutableStateFlow<TransactionType?>(null)
    val accountDetailTypeFilter: StateFlow<TransactionType?> = _accountDetailTypeFilter.asStateFlow()

    // Selected Transaction for Detail / Edit / Delete Modal
    private val _selectedTransactionDetail = MutableStateFlow<TransactionEntity?>(null)
    val selectedTransactionDetail: StateFlow<TransactionEntity?> = _selectedTransactionDetail.asStateFlow()

    // Show Dialogs State
    private val _showMonthPicker = MutableStateFlow(false)
    val showMonthPicker: StateFlow<Boolean> = _showMonthPicker.asStateFlow()

    private val _showSettings = MutableStateFlow(false)
    val showSettings: StateFlow<Boolean> = _showSettings.asStateFlow()

    private val _showSmsReview = MutableStateFlow(false)
    val showSmsReview: StateFlow<Boolean> = _showSmsReview.asStateFlow()

    // SMS Extraction Candidates
    private val _smsCandidates = MutableStateFlow<List<SmsCandidate>>(emptyList())
    val smsCandidates: StateFlow<List<SmsCandidate>> = _smsCandidates.asStateFlow()

    // Pre-filled Transaction data for Add Screen (e.g. from SMS or duplicate)
    private val _prefilledTransaction = MutableStateFlow<TransactionEntity?>(null)
    val prefilledTransaction: StateFlow<TransactionEntity?> = _prefilledTransaction.asStateFlow()

    // Pending transaction type from quick actions (Expense/Income/Transfer)
    private val _pendingTransactionType = MutableStateFlow<TransactionType?>(null)
    val pendingTransactionType: StateFlow<TransactionType?> = _pendingTransactionType.asStateFlow()

    // Import Items Preview
    private val _importItems = MutableStateFlow<List<ImportItem>>(emptyList())
    val importItems: StateFlow<List<ImportItem>> = _importItems.asStateFlow()

    private val _showImportReview = MutableStateFlow(false)
    val showImportReview: StateFlow<Boolean> = _showImportReview.asStateFlow()

    // Status Message / Toast feedback
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // Combined Reactive Data
    val balances: StateFlow<AccountBalances> = dao.dataVersion.combine(_selectedTab) { _, _ ->
        dao.getCurrentBalances()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AccountBalances())

    val monthlySummary: StateFlow<MonthlySummary> = combine(
        dao.dataVersion,
        _selectedYear,
        _selectedMonth
    ) { _, year, month ->
        dao.getMonthlySummary(year, month)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        MonthlySummary(DateTimeUtils.getCurrentYear(), DateTimeUtils.getCurrentMonth())
    )

    val allTransactions: StateFlow<List<TransactionEntity>> = dao.dataVersion.combine(_selectedTab) { _, _ ->
        dao.getAllCalculatedTransactions().reversed() // Newest first for feeds
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentMonthTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        _selectedYear,
        _selectedMonth
    ) { list, year, month ->
        val start = DateTimeUtils.getStartOfMonth(year, month)
        val end = DateTimeUtils.getEndOfMonth(year, month)
        list.filter { it.timestamp in start..end }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun setPendingTransactionType(type: TransactionType) {
        _pendingTransactionType.value = type
    }

    fun clearPendingTransactionType() {
        _pendingTransactionType.value = null
    }

    fun setSelectedYearMonth(year: Int, month: Int) {
        _selectedYear.value = year
        _selectedMonth.value = month
        _showMonthPicker.value = false
    }

    fun nextMonth() {
        var m = _selectedMonth.value + 1
        var y = _selectedYear.value
        if (m > 12) {
            m = 1
            y += 1
        }
        _selectedMonth.value = m
        _selectedYear.value = y
    }

    fun prevMonth() {
        var m = _selectedMonth.value - 1
        var y = _selectedYear.value
        if (m < 1) {
            m = 12
            y -= 1
        }
        _selectedMonth.value = m
        _selectedYear.value = y
    }

    fun setAccountFilter(account: AccountType) {
        _selectedAccountFilter.value = account
    }

    fun openAccountDetail(account: AccountType) {
        _selectedDetailAccount.value = account
        _accountDetailTypeFilter.value = null
    }

    fun closeAccountDetail() {
        _selectedDetailAccount.value = null
    }

    fun setAccountDetailTypeFilter(type: TransactionType?) {
        _accountDetailTypeFilter.value = type
    }

    fun openTransactionDetail(tx: TransactionEntity) {
        _selectedTransactionDetail.value = tx
    }

    fun closeTransactionDetail() {
        _selectedTransactionDetail.value = null
    }

    fun setShowMonthPicker(show: Boolean) {
        _showMonthPicker.value = show
    }

    fun setShowSettings(show: Boolean) {
        _showSettings.value = show
    }

    fun setShowSmsReview(show: Boolean) {
        _showSmsReview.value = show
    }

    fun setShowImportReview(show: Boolean) {
        _showImportReview.value = show
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    /**
     * Forces all reactive state flows to recompute by bumping the DAO data version.
     * Called after a successful backup restore to reload all balances, transactions,
     * insights, and calendar data from the freshly-written database.
     */
    fun refreshAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.notifyDataChangedPublic()
        }
    }

    // CRUD Transactions
    fun saveTransaction(
        type: TransactionType,
        amount: Double,
        category: String,
        fromAccount: AccountType?,
        toAccount: AccountType?,
        timestamp: Long,
        note: String = "",
        referenceNumber: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val entity = TransactionEntity(
                type = type,
                amount = amount,
                category = category.trim().ifBlank { if (type == TransactionType.TRANSFER) "Transfer" else type.displayName },
                fromAccount = fromAccount,
                toAccount = toAccount,
                timestamp = timestamp,
                note = note.trim(),
                referenceNumber = referenceNumber?.trim()
            )
            dao.insertTransaction(entity)
            withContext(Dispatchers.Main) {
                _statusMessage.value = "Transaction saved successfully!"
                _prefilledTransaction.value = null
                onSuccess()
            }
        }
    }

    fun updateTransaction(
        id: Long,
        type: TransactionType,
        amount: Double,
        category: String,
        fromAccount: AccountType?,
        toAccount: AccountType?,
        timestamp: Long,
        note: String = "",
        referenceNumber: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val entity = TransactionEntity(
                id = id,
                type = type,
                amount = amount,
                category = category.trim().ifBlank { if (type == TransactionType.TRANSFER) "Transfer" else type.displayName },
                fromAccount = fromAccount,
                toAccount = toAccount,
                timestamp = timestamp,
                note = note.trim(),
                referenceNumber = referenceNumber?.trim()
            )
            dao.updateTransaction(entity)
            withContext(Dispatchers.Main) {
                _statusMessage.value = "Transaction updated!"
                closeTransactionDetail()
                onSuccess()
            }
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteTransaction(id)
            withContext(Dispatchers.Main) {
                _statusMessage.value = "Transaction deleted."
                closeTransactionDetail()
            }
        }
    }

    fun duplicateTransaction(tx: TransactionEntity) {
        _prefilledTransaction.value = tx.copy(id = 0, timestamp = System.currentTimeMillis())
        _selectedTab.value = 2 // Switch to Add screen
        closeTransactionDetail()
    }

    fun getSavedCategories(type: TransactionType): List<CategoryEntity> {
        return dao.getCategories(type)
    }

    // SMS Parsing Flow
    fun parsePastedSms(text: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val parsedList = SmsParser.parseMultiple(text)
            val validatedList = parsedList.map { candidate ->
                val dup = dao.findDuplicate(
                    referenceNumber = candidate.referenceNumber,
                    timestamp = candidate.timestamp,
                    amount = candidate.amount,
                    type = candidate.type,
                    account = candidate.suggestedAccount
                )
                if (dup != null) {
                    candidate.copy(
                        isDuplicate = true,
                        duplicateReason = if (!candidate.referenceNumber.isNullOrBlank()) "Reference number already exists" else "Similar transaction found on same date"
                    )
                } else {
                    candidate
                }
            }
            withContext(Dispatchers.Main) {
                _smsCandidates.value = validatedList
            }
        }
    }

    fun useSmsCandidateToAdd(candidate: SmsCandidate) {
        _prefilledTransaction.value = TransactionEntity(
            type = candidate.type,
            amount = candidate.amount,
            category = candidate.suggestedCategory,
            fromAccount = if (candidate.type == TransactionType.EXPENSE) candidate.suggestedAccount else null,
            toAccount = if (candidate.type == TransactionType.INCOME) candidate.suggestedAccount else null,
            timestamp = candidate.timestamp,
            referenceNumber = candidate.referenceNumber,
            isPending = candidate.isPendingVerification
        )
        _showSmsReview.value = false
        _selectedTab.value = 2 // Add tab
    }

    fun clearSmsCandidates() {
        _smsCandidates.value = emptyList()
    }

    // Import from File (Excel / PDF)
    fun processFileImport(uri: Uri, isPdf: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val rawItems = if (isPdf) {
                PdfImporter.importFromPdf(context, uri)
            } else {
                ExcelImporter.importFromFile(context, uri)
            }

            // Check duplicates
            val reviewedItems = rawItems.map { item ->
                val dup = dao.findDuplicate(
                    referenceNumber = item.referenceNumber,
                    timestamp = item.timestamp,
                    amount = item.amount,
                    type = item.type,
                    account = item.fromAccount ?: item.toAccount
                )
                if (dup != null) {
                    item.copy(isDuplicate = true)
                } else {
                    item
                }
            }

            withContext(Dispatchers.Main) {
                _importItems.value = reviewedItems
                _showImportReview.value = true
            }
        }
    }

    fun toggleImportItemSelection(id: String) {
        _importItems.value = _importItems.value.map {
            if (it.id == id) it.copy(isSelected = !it.isSelected) else it
        }
    }

    fun confirmBatchImport() {
        val selected = _importItems.value.filter { it.isSelected }
        viewModelScope.launch(Dispatchers.IO) {
            val entities = selected.map { item ->
                TransactionEntity(
                    type = item.type,
                    amount = item.amount,
                    category = item.category,
                    fromAccount = item.fromAccount,
                    toAccount = item.toAccount,
                    timestamp = item.timestamp,
                    note = item.note,
                    referenceNumber = item.referenceNumber
                )
            }
            dao.insertBatch(entities)
            withContext(Dispatchers.Main) {
                _showImportReview.value = false
                _importItems.value = emptyList()
                _statusMessage.value = "Successfully imported ${selected.size} transactions!"
            }
        }
    }

    // Export Reports
    fun exportReport(isPdf: Boolean): File {
        val context = getApplication<Application>()
        val allTx = dao.getAllCalculatedTransactions()
        val bal = dao.getCurrentBalances()
        return if (isPdf) {
            PdfExporter.exportToPdf(context, allTx, bal)
        } else {
            ExcelExporter.exportToExcel(context, allTx, bal)
        }
    }

    // Clear / Reset All Data
    fun resetAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteAllData()
            withContext(Dispatchers.Main) {
                _statusMessage.value = "All local data deleted."
                _showSettings.value = false
            }
        }
    }
}

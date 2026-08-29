package com.example.moneyminder

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.moneyminder.data.model.AccountType
import com.example.moneyminder.data.model.TransactionType
import com.example.moneyminder.theme.BackgroundDark
import com.example.moneyminder.theme.CardBackground
import com.example.moneyminder.theme.CardBorder
import com.example.moneyminder.theme.MoneyMinderTheme
import com.example.moneyminder.theme.TextPrimary
import com.example.moneyminder.theme.TextSecondary
import com.example.moneyminder.ui.components.GlassBottomNavigation
import com.example.moneyminder.ui.components.MonthYearPickerDialog
import com.example.moneyminder.ui.screens.AccountDetailDialog
import com.example.moneyminder.ui.screens.AddTransactionScreen
import com.example.moneyminder.ui.screens.CalendarScreen
import com.example.moneyminder.ui.screens.HomeScreen
import com.example.moneyminder.ui.screens.ImportReviewScreen
import com.example.moneyminder.ui.screens.InsightsScreen
import com.example.moneyminder.ui.screens.SettingsScreen
import com.example.moneyminder.ui.screens.SmsReviewScreen
import com.example.moneyminder.ui.screens.SplashScreen
import com.example.moneyminder.ui.screens.TransactionDetailDialog
import com.example.moneyminder.ui.screens.WelcomeScreen
import com.example.moneyminder.ui.viewmodel.MainViewModel
import com.example.moneyminder.ui.screens.BackupSyncScreen
import com.example.moneyminder.ui.viewmodel.BackupViewModel
import java.io.File

enum class AppNavState {
    SPLASH,
    WELCOME,
    MAIN
}

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val backupViewModel: BackupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = getSharedPreferences("money_minder_prefs", Context.MODE_PRIVATE)
        val isFirstLaunch = prefs.getBoolean("is_first_launch", true)

        setContent {
            MoneyMinderTheme {
                var currentScreen by remember { mutableStateOf(AppNavState.SPLASH) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundDark
                ) {
                    when (currentScreen) {
                        AppNavState.SPLASH -> {
                            SplashScreen(
                                onTimeout = {
                                    currentScreen = if (isFirstLaunch) AppNavState.WELCOME else AppNavState.MAIN
                                }
                            )
                        }
                        AppNavState.WELCOME -> {
                            WelcomeScreen(
                                onGetStarted = {
                                    prefs.edit().putBoolean("is_first_launch", false).apply()
                                    currentScreen = AppNavState.MAIN
                                }
                            )
                        }
                        AppNavState.MAIN -> {
                            MainAppContent(viewModel = viewModel, backupViewModel = backupViewModel)
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
fun MainAppContent(viewModel: MainViewModel, backupViewModel: BackupViewModel) {
    val context = LocalContext.current
    val selectedTab by viewModel.selectedTab.collectAsState()
    val showMonthPicker by viewModel.showMonthPicker.collectAsState()
    val showSettings by viewModel.showSettings.collectAsState()
    val showImportReview by viewModel.showImportReview.collectAsState()
    val selectedDetailAccount by viewModel.selectedDetailAccount.collectAsState()
    val selectedTxDetail by viewModel.selectedTransactionDetail.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    // Backup state
    val backupStatus by backupViewModel.backupStatus.collectAsState()
    val backupToastMessage by backupViewModel.toastMessage.collectAsState()
    val restoreComplete by backupViewModel.restoreComplete.collectAsState()
    var showBackupSync by remember { mutableStateOf(false) }

    var showExportDialog by remember { mutableStateOf(false) }

    // File picker launcher for Excel/PDF import
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val isPdf = uri.toString().endsWith(".pdf", ignoreCase = true)
            viewModel.processFileImport(uri, isPdf)
        }
    }

    // Display Status Toast feedback
    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearStatusMessage()
        }
    }

    // Backup toast messages
    LaunchedEffect(backupToastMessage) {
        backupToastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            backupViewModel.clearToast()
        }
    }

    // Restore complete → reload all data and navigate home
    LaunchedEffect(restoreComplete) {
        if (restoreComplete) {
            viewModel.refreshAllData()
            showBackupSync = false
            viewModel.setSelectedTab(0)
            backupViewModel.acknowledgeRestore()
        }
    }

    fun shareExportedFile(file: File, mimeType: String) {
        try {
            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Report"))
        } catch (e: Exception) {
            Toast.makeText(context, "Export saved to: ${file.name}", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BackgroundDark,
        bottomBar = {
            GlassBottomNavigation(
                selectedTab = selectedTab,
                onTabSelected = { tabIndex ->
                    viewModel.setSelectedTab(tabIndex)
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tabContent"
            ) { tab ->
                when (tab) {
                    0 -> HomeScreen(
                        viewModel = viewModel,
                        onNavigateToAdd = { type ->
                            viewModel.setPendingTransactionType(type)
                            viewModel.setSelectedTab(2)
                        },
                        onOpenExport = { showExportDialog = true }
                    )
                    1 -> InsightsScreen(
                        viewModel = viewModel
                    )
                    2 -> AddTransactionScreen(
                        viewModel = viewModel,
                        onCancel = { viewModel.setSelectedTab(0) }
                    )
                    3 -> CalendarScreen(
                        viewModel = viewModel,
                        onNavigateToAdd = { timestamp ->
                            viewModel.setSelectedTab(2)
                        }
                    )
                    4 -> SmsReviewTabContent(
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    // 1. Month / Year Picker Modal
    if (showMonthPicker) {
        val selectedYear by viewModel.selectedYear.collectAsState()
        val selectedMonth by viewModel.selectedMonth.collectAsState()
        MonthYearPickerDialog(
            currentYear = selectedYear,
            currentMonth = selectedMonth,
            onConfirm = { y, m ->
                viewModel.setSelectedYearMonth(y, m)
            },
            onDismiss = { viewModel.setShowMonthPicker(false) }
        )
    }

    // 2. Per-Account Detail Timeline Popup
    selectedDetailAccount?.let { account ->
        AccountDetailDialog(
            account = account,
            viewModel = viewModel,
            onDismiss = { viewModel.closeAccountDetail() }
        )
    }

    // 3. Transaction Detail / Edit / Delete Dialog
    selectedTxDetail?.let { tx ->
        TransactionDetailDialog(
            transaction = tx,
            viewModel = viewModel,
            onDismiss = { viewModel.closeTransactionDetail() }
        )
    }

    // 4. Excel / PDF Import Review Modal
    if (showImportReview) {
        ImportReviewScreen(
            viewModel = viewModel,
            onDismiss = { viewModel.setShowImportReview(false) }
        )
    }

    // 6. Settings & Privacy Dialog
    if (showSettings) {
        SettingsScreen(
            viewModel = viewModel,
            onExportExcel = {
                val file = viewModel.exportReport(isPdf = false)
                shareExportedFile(file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            },
            onExportPdf = {
                val file = viewModel.exportReport(isPdf = true)
                shareExportedFile(file, "application/pdf")
            },
            onOpenBackupSync = { showBackupSync = true },
            onDismiss = { viewModel.setShowSettings(false) }
        )
    }

    // 6b. Backup & Sync Screen
    if (showBackupSync) {
        BackupSyncScreen(
            viewModel = backupViewModel,
            onDismiss = { showBackupSync = false }
        )
    }

    // 7. Export / Import Choice Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export / Import Data", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Choose an action:", color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            showExportDialog = false
                            val file = viewModel.exportReport(isPdf = false)
                            shareExportedFile(file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TextPrimary, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export to Excel (.xlsx)", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            showExportDialog = false
                            val file = viewModel.exportReport(isPdf = true)
                            shareExportedFile(file, "application/pdf")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2E3A), contentColor = TextPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export to PDF", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            showExportDialog = false
                            filePickerLauncher.launch("*/*")
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Import Existing Data (.xlsx / .pdf)")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                OutlinedButton(
                    onClick = { showExportDialog = false },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) {
                    Text("Close")
                }
            },
            containerColor = CardBackground,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun SmsReviewTabContent(viewModel: MainViewModel) {
    SmsReviewScreen(viewModel = viewModel)
}

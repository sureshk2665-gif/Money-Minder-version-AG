package com.example.moneyminder.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moneyminder.data.security.MpinPreferences
import com.example.moneyminder.theme.BackgroundDark
import com.example.moneyminder.theme.CardBackground
import com.example.moneyminder.theme.CardBackgroundElevated
import com.example.moneyminder.theme.CardBorder
import com.example.moneyminder.theme.CardBorderSubtle
import com.example.moneyminder.theme.ExpenseRed
import com.example.moneyminder.theme.IncomeGreen
import com.example.moneyminder.theme.TextPrimary
import com.example.moneyminder.theme.TextSecondary

enum class MpinMode {
    VERIFY,
    SETUP_NEW,
    SETUP_CONFIRM,
    SETUP_SECURITY,
    RESET_OLD,
    RESET_NEW,
    RESET_CONFIRM,
    FORGOT_SECURITY,
    FORGOT_NEW,
    FORGOT_CONFIRM
}

@Composable
fun MpinScreen(
    mpinPrefs: MpinPreferences,
    mode: MpinMode,
    onSuccess: () -> Unit,
    onForgot: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null
) {
    var enteredPin by remember { mutableStateOf("") }
    var firstPin by remember { mutableStateOf("") }
    var currentMode by remember { mutableStateOf(mode) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lockoutRemaining by remember { mutableStateOf(0L) }

    // Security question state
    var selectedQ1 by remember { mutableStateOf("") }
    var selectedQ2 by remember { mutableStateOf("") }
    var answer1 by remember { mutableStateOf("") }
    var answer2 by remember { mutableStateOf("") }

    LaunchedEffect(lockoutRemaining) {
        if (lockoutRemaining > 0) {
            kotlinx.coroutines.delay(1000)
            val remaining = mpinPrefs.lockoutUntil - System.currentTimeMillis()
            lockoutRemaining = if (remaining > 0) remaining else 0
        }
    }

    LaunchedEffect(Unit) {
        val remaining = mpinPrefs.lockoutUntil - System.currentTimeMillis()
        if (remaining > 0) lockoutRemaining = remaining
    }

    val isLockedOut = lockoutRemaining > 0

    val isSecurityScreen = currentMode == MpinMode.SETUP_SECURITY || currentMode == MpinMode.FORGOT_SECURITY

    val title = when (currentMode) {
        MpinMode.VERIFY -> "Enter MPIN"
        MpinMode.SETUP_NEW -> "Set New MPIN"
        MpinMode.SETUP_CONFIRM -> "Confirm MPIN"
        MpinMode.SETUP_SECURITY -> "Security Questions"
        MpinMode.RESET_OLD -> "Enter Old MPIN"
        MpinMode.RESET_NEW -> "Enter New MPIN"
        MpinMode.RESET_CONFIRM -> "Confirm New MPIN"
        MpinMode.FORGOT_SECURITY -> "Verify Identity"
        MpinMode.FORGOT_NEW -> "Set New MPIN"
        MpinMode.FORGOT_CONFIRM -> "Confirm New MPIN"
    }

    val subtitle = when (currentMode) {
        MpinMode.VERIFY -> "Enter your 4-digit PIN to unlock"
        MpinMode.SETUP_NEW -> "Choose a 4-digit PIN"
        MpinMode.SETUP_CONFIRM -> "Re-enter PIN to confirm"
        MpinMode.SETUP_SECURITY -> "Set up recovery questions"
        MpinMode.RESET_OLD -> "Verify your current PIN"
        MpinMode.RESET_NEW -> "Choose a new 4-digit PIN"
        MpinMode.RESET_CONFIRM -> "Re-enter new PIN to confirm"
        MpinMode.FORGOT_SECURITY -> "Answer your security questions"
        MpinMode.FORGOT_NEW -> "Choose a new 4-digit PIN"
        MpinMode.FORGOT_CONFIRM -> "Re-enter new PIN to confirm"
    }

    fun handlePinComplete(pin: String) {
        when (currentMode) {
            MpinMode.VERIFY -> {
                if (mpinPrefs.verifyMpin(pin)) {
                    mpinPrefs.failedAttempts = 0
                    mpinPrefs.lockoutUntil = 0L
                    onSuccess()
                } else {
                    val attempts = mpinPrefs.failedAttempts + 1
                    mpinPrefs.failedAttempts = attempts
                    if (attempts >= 3) {
                        mpinPrefs.lockoutUntil = System.currentTimeMillis() + 30_000
                        lockoutRemaining = 30_000
                        errorMessage = "Too many attempts. Wait 30 seconds."
                        mpinPrefs.failedAttempts = 0
                    } else {
                        errorMessage = "Wrong PIN. ${3 - attempts} attempts left."
                    }
                    enteredPin = ""
                }
            }
            MpinMode.SETUP_NEW -> {
                firstPin = pin
                enteredPin = ""
                currentMode = MpinMode.SETUP_CONFIRM
                errorMessage = null
            }
            MpinMode.SETUP_CONFIRM -> {
                if (pin == firstPin) {
                    enteredPin = ""
                    currentMode = MpinMode.SETUP_SECURITY
                    errorMessage = null
                } else {
                    errorMessage = "PINs don't match. Try again."
                    enteredPin = ""
                    firstPin = ""
                    currentMode = MpinMode.SETUP_NEW
                }
            }
            MpinMode.RESET_OLD -> {
                if (mpinPrefs.verifyMpin(pin)) {
                    enteredPin = ""
                    currentMode = MpinMode.RESET_NEW
                    errorMessage = null
                } else {
                    errorMessage = "Wrong PIN."
                    enteredPin = ""
                }
            }
            MpinMode.RESET_NEW -> {
                firstPin = pin
                enteredPin = ""
                currentMode = MpinMode.RESET_CONFIRM
                errorMessage = null
            }
            MpinMode.RESET_CONFIRM -> {
                if (pin == firstPin) {
                    mpinPrefs.setMpin(pin)
                    onSuccess()
                } else {
                    errorMessage = "PINs don't match. Try again."
                    enteredPin = ""
                    firstPin = ""
                    currentMode = MpinMode.RESET_NEW
                }
            }
            MpinMode.FORGOT_NEW -> {
                firstPin = pin
                enteredPin = ""
                currentMode = MpinMode.FORGOT_CONFIRM
                errorMessage = null
            }
            MpinMode.FORGOT_CONFIRM -> {
                if (pin == firstPin) {
                    mpinPrefs.setMpin(pin)
                    mpinPrefs.failedAttempts = 0
                    mpinPrefs.lockoutUntil = 0L
                    onSuccess()
                } else {
                    errorMessage = "PINs don't match. Try again."
                    enteredPin = ""
                    firstPin = ""
                    currentMode = MpinMode.FORGOT_NEW
                }
            }
            else -> {}
        }
    }

    if (isSecurityScreen) {
        SecurityQuestionsScreen(
            title = title,
            subtitle = subtitle,
            selectedQ1 = selectedQ1,
            selectedQ2 = selectedQ2,
            answer1 = answer1,
            answer2 = answer2,
            onQ1Change = { selectedQ1 = it },
            onQ2Change = { selectedQ2 = it },
            onA1Change = { answer1 = it },
            onA2Change = { answer2 = it },
            errorMessage = errorMessage,
            isForgotMode = currentMode == MpinMode.FORGOT_SECURITY,
            mpinPrefs = mpinPrefs,
            onSubmit = {
                if (currentMode == MpinMode.SETUP_SECURITY) {
                    if (selectedQ1.isEmpty() || selectedQ2.isEmpty()) {
                        errorMessage = "Please select both questions."
                    } else if (selectedQ1 == selectedQ2) {
                        errorMessage = "Choose two different questions."
                    } else if (answer1.isBlank() || answer2.isBlank()) {
                        errorMessage = "Please answer both questions."
                    } else {
                        mpinPrefs.setMpin(firstPin)
                        mpinPrefs.setSecurityQuestions(selectedQ1, answer1, selectedQ2, answer2)
                        onSuccess()
                    }
                } else {
                    if (answer1.isBlank() || answer2.isBlank()) {
                        errorMessage = "Please answer both questions."
                    } else if (mpinPrefs.verifySecurityAnswers(answer1, answer2)) {
                        errorMessage = null
                        currentMode = MpinMode.FORGOT_NEW
                    } else {
                        errorMessage = "Wrong answers. Try again."
                    }
                }
            },
            onCancel = onCancel
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(CardBackgroundElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(4) { index ->
                val isFilled = index < enteredPin.length
                val dotScale by animateFloatAsState(
                    targetValue = if (isFilled) 1.2f else 1f,
                    animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessLow),
                    label = "dotScale"
                )
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .scale(dotScale)
                        .clip(CircleShape)
                        .background(if (isFilled) TextPrimary else Color.Transparent)
                        .border(2.dp, if (isFilled) TextPrimary else CardBorder, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        AnimatedVisibility(
            visible = errorMessage != null || isLockedOut,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = if (isLockedOut) "Locked. Wait ${lockoutRemaining / 1000}s" else errorMessage ?: "",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = ExpenseRed,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        val numberRows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "DEL")
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            numberRows.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    row.forEach { key ->
                        if (key.isEmpty()) {
                            Spacer(modifier = Modifier.size(72.dp))
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(if (key == "DEL") Color.Transparent else CardBackground)
                                    .border(
                                        1.dp,
                                        if (key == "DEL") Color.Transparent else CardBorder,
                                        CircleShape
                                    )
                                    .clickable(enabled = !isLockedOut) {
                                        if (key == "DEL") {
                                            if (enteredPin.isNotEmpty()) {
                                                enteredPin = enteredPin.dropLast(1)
                                                errorMessage = null
                                            }
                                        } else if (enteredPin.length < 4) {
                                            enteredPin += key
                                            errorMessage = null
                                            if (enteredPin.length == 4) {
                                                handlePinComplete(enteredPin)
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (key == "DEL") {
                                    Icon(
                                        imageVector = Icons.Default.Backspace,
                                        contentDescription = "Delete",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Text(
                                        text = key,
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (currentMode == MpinMode.VERIFY && onForgot != null) {
                Text(
                    text = "Forgot MPIN?",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = ExpenseRed,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.clickable { onForgot() }
                )
            }
            if (onCancel != null && currentMode != MpinMode.VERIFY) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.clickable { onCancel() }
                )
            }
        }
    }
}

@Composable
private fun SecurityQuestionsScreen(
    title: String,
    subtitle: String,
    selectedQ1: String,
    selectedQ2: String,
    answer1: String,
    answer2: String,
    onQ1Change: (String) -> Unit,
    onQ2Change: (String) -> Unit,
    onA1Change: (String) -> Unit,
    onA2Change: (String) -> Unit,
    errorMessage: String?,
    isForgotMode: Boolean,
    mpinPrefs: MpinPreferences,
    onSubmit: () -> Unit,
    onCancel: (() -> Unit)?
) {
    val questions = MpinPreferences.SECURITY_QUESTIONS

    LaunchedEffect(isForgotMode) {
        if (isForgotMode) {
            onQ1Change(mpinPrefs.getSecurityQuestion1())
            onQ2Change(mpinPrefs.getSecurityQuestion2())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(CardBackgroundElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Question 1
        SecurityQuestionField(
            label = "Question 1",
            selectedQuestion = selectedQ1,
            answer = answer1,
            onQuestionChange = onQ1Change,
            onAnswerChange = onA1Change,
            questions = if (isForgotMode) emptyList() else questions,
            excludeQuestion = selectedQ2,
            readOnlyQuestion = isForgotMode
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Question 2
        SecurityQuestionField(
            label = "Question 2",
            selectedQuestion = selectedQ2,
            answer = answer2,
            onQuestionChange = onQ2Change,
            onAnswerChange = onA2Change,
            questions = if (isForgotMode) emptyList() else questions,
            excludeQuestion = selectedQ1,
            readOnlyQuestion = isForgotMode
        )

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(
            visible = errorMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = errorMessage ?: "",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = ExpenseRed,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TextPrimary,
                contentColor = Color.Black
            )
        ) {
            Text(
                text = if (isForgotMode) "Verify & Reset PIN" else "Save & Continue",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            )
        }

        if (onCancel != null) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Cancel",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.clickable { onCancel() }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SecurityQuestionField(
    label: String,
    selectedQuestion: String,
    answer: String,
    onQuestionChange: (String) -> Unit,
    onAnswerChange: (String) -> Unit,
    questions: List<String>,
    excludeQuestion: String,
    readOnlyQuestion: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    val availableQuestions = questions.filter { it != excludeQuestion }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        if (readOnlyQuestion) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBackgroundElevated)
                    .border(1.dp, CardBorderSubtle, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = selectedQuestion,
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
                )
            }
        } else {
            Box {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBackgroundElevated)
                        .border(1.dp, CardBorderSubtle, RoundedCornerShape(12.dp))
                        .clickable { expanded = true }
                        .padding(14.dp)
                ) {
                    Text(
                        text = selectedQuestion.ifEmpty { "Select a question..." },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (selectedQuestion.isEmpty()) TextSecondary else TextPrimary
                        )
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    availableQuestions.forEach { q ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = q,
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                                    fontSize = 13.sp
                                )
                            },
                            onClick = {
                                onQuestionChange(q)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = answer,
            onValueChange = onAnswerChange,
            singleLine = true,
            placeholder = { Text("Your answer", color = TextSecondary, fontSize = 13.sp) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = IncomeGreen,
                unfocusedBorderColor = CardBorderSubtle,
                cursorColor = IncomeGreen,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium,
            shape = RoundedCornerShape(12.dp)
        )
    }
}

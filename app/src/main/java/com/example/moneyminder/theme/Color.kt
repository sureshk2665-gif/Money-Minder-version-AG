package com.example.moneyminder.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Backgrounds & Surface (deep violet-black, tinted rather than neutral)
val BackgroundDark = Color(0xFF0E0818)
val SurfaceDark = Color(0xFF141020)
val CardBackground = Color(0xFF1A1428)
val CardBackgroundElevated = Color(0xFF241B36)
val CardBorder = Color(0xFF3A2C56)
val CardBorderSubtle = Color(0xFF2B2142)

// Text Colors
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFC3B6DC)
val TextMuted = Color(0xFF8B7CA8)
val TextDisabled = Color(0xFF574B70)

// Violet brand accents
val VioletPrimary = Color(0xFFA86AFF)
val VioletDeep = Color(0xFF7C3AED)
val VioletSoft = Color(0xFFC9A6FF)
val PinkAccent = Color(0xFFFF6FD8)

// Signature gradient, lifted from the pink-to-violet call to action
val AccentGradient = Brush.horizontalGradient(listOf(PinkAccent, VioletPrimary))

// Page backdrop: violet bloom fading into the near-black base
val BackdropGradient = Brush.verticalGradient(
    listOf(Color(0xFF1C1033), Color(0xFF0E0818), Color(0xFF0B0614))
)

// Income Accents (Green, warmed slightly to sit on violet)
val IncomeGreen = Color(0xFF4ADE80)
val IncomeGreenLight = Color(0xFF86EFAC)
val IncomeGreenDark = Color(0xFF16A34A)
val IncomeCardBg = Color(0xFF122A1E)
val IncomeBorder = Color(0xFF1E4A33)
val IncomePillBg = Color(0xB31E4A33) // 70% opacity

// Expense Accents (Red)
val ExpenseRed = Color(0xFFFB7185)
val ExpenseRedLight = Color(0xFFFDA4AF)
val ExpenseRedDark = Color(0xFFE11D48)
val ExpenseCardBg = Color(0xFF2A1322)
val ExpenseBorder = Color(0xFF4F1D38)
val ExpensePillBg = Color(0xB34F1D38) // 70% opacity

// Transfer Accents (violet-leaning slate so transfers read as brand-neutral)
val TransferBlueGrey = Color(0xFF9D8EC7)
val TransferBlueGreyLight = Color(0xFFC4B8E4)
val TransferCardBg = Color(0xFF1B1730)
val TransferBorder = Color(0xFF342A52)
val TransferPillBg = Color(0xB3342A52)

// Glass Bar & Highlights
val GlassNavBackground = Color(0xE61A1428) // 90% blur glass
val GlassSelectionHighlight = Color(0xFF2E2347)
val GlassBorder = Color(0x33C9A6FF)
val GlassNavPillTop = Color(0xFF2E2347)
val GlassNavPillBottom = Color(0xFF1F1833)
val GlassBorderStrong = Color(0x80C9A6FF)

// Accounts Accent
val BankAccent = Color(0xFF818CF8)
val WalletAccent = Color(0xFFFBBF24)
val CashAccent = Color(0xFF2DD4BF)
val OverallAccent = Color(0xFFA86AFF)

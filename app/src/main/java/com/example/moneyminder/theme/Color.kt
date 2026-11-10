package com.example.moneyminder.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// The backdrop sits LIGHTER than the content blocks on purpose: a cool, glassy
// blue-black that the solid black cards read against. Inverting the usual
// card-lighter-than-background relationship is what gives the layered feel.
val BackgroundDark = Color(0xFF0A1120)
val SurfaceDark = Color(0xFF0C1526)

// Content blocks: near-pure black, so each card reads as a solid slab.
val CardBackground = Color(0xFF03060C)
val CardBackgroundElevated = Color(0xFF0B1220)
val CardBorder = Color(0xFF1E2B3F)
val CardBorderSubtle = Color(0xFF16212F)

// Text Colors
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFA8BBD4)
val TextMuted = Color(0xFF6F85A3)
val TextDisabled = Color(0xFF42536B)

// Blue accents
val AccentPrimary = Color(0xFF3B82F6)
val AccentDeep = Color(0xFF1D4ED8)
val AccentSoft = Color(0xFF93C5FD)
val AccentHighlight = Color(0xFF22D3EE)

// Signature gradient: cyan into blue
val AccentGradient = Brush.horizontalGradient(listOf(AccentHighlight, AccentPrimary))

// Glassy backdrop: a cool bloom at the top fading into black
val BackdropGradient = Brush.verticalGradient(
    listOf(Color(0xFF13203A), Color(0xFF0A1120), Color(0xFF05080F))
)

// Income Accents (Emerald, legible against blue-black)
val IncomeGreen = Color(0xFF34D399)
val IncomeGreenLight = Color(0xFF6EE7B7)
val IncomeGreenDark = Color(0xFF059669)
val IncomeCardBg = Color(0xFF04201A)
val IncomeBorder = Color(0xFF10493A)
val IncomePillBg = Color(0xB310493A) // 70% opacity

// Expense Accents (Red)
val ExpenseRed = Color(0xFFF87171)
val ExpenseRedLight = Color(0xFFFCA5A5)
val ExpenseRedDark = Color(0xFFDC2626)
val ExpenseCardBg = Color(0xFF240E12)
val ExpenseBorder = Color(0xFF4C1A20)
val ExpensePillBg = Color(0xB34C1A20) // 70% opacity

// Transfer Accents (steel blue, brand-neutral against the accents)
val TransferBlueGrey = Color(0xFF7D9BC1)
val TransferBlueGreyLight = Color(0xFFB2C7DF)
val TransferCardBg = Color(0xFF0B1725)
val TransferBorder = Color(0xFF203347)
val TransferPillBg = Color(0xB3203347)

// Glass Bar & Highlights
val GlassNavBackground = Color(0xE6070D18) // 90% blur glass
val GlassSelectionHighlight = Color(0xFF16263C)
val GlassBorder = Color(0x338EC5FF)
val GlassNavPillTop = Color(0xFF16263C)
val GlassNavPillBottom = Color(0xFF0B1424)
val GlassBorderStrong = Color(0x808EC5FF)

// Accounts Accent
val BankAccent = Color(0xFF3B82F6)
val WalletAccent = Color(0xFFF59E0B)
val CashAccent = Color(0xFF10B981)
val OverallAccent = Color(0xFF22D3EE)

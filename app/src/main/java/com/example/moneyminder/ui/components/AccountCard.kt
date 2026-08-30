package com.example.moneyminder.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moneyminder.data.model.AccountBalances
import com.example.moneyminder.data.model.AccountType
import com.example.moneyminder.theme.BankAccent
import com.example.moneyminder.theme.CardBackground
import com.example.moneyminder.theme.CardBorder
import com.example.moneyminder.theme.CashAccent
import com.example.moneyminder.theme.TextMuted
import com.example.moneyminder.theme.TextPrimary
import com.example.moneyminder.theme.TextSecondary
import com.example.moneyminder.theme.WalletAccent
import com.example.moneyminder.util.CurrencyFormatter

@Composable
fun AccountCardsRow(
    balances: AccountBalances,
    onAccountClick: (AccountType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Bank Card
        AccountCardItem(
            account = AccountType.BANK,
            balance = balances.bankBalance,
            icon = Icons.Default.AccountBalance,
            accentColor = BankAccent,
            onClick = { onAccountClick(AccountType.BANK) },
            modifier = Modifier.weight(1f)
        )

        // Wallet Card
        AccountCardItem(
            account = AccountType.WALLET,
            balance = balances.walletBalance,
            icon = Icons.Default.AccountBalanceWallet,
            accentColor = WalletAccent,
            onClick = { onAccountClick(AccountType.WALLET) },
            modifier = Modifier.weight(1f)
        )

        // Cash Card
        AccountCardItem(
            account = AccountType.CASH,
            balance = balances.cashBalance,
            icon = Icons.Default.Payments,
            accentColor = CashAccent,
            onClick = { onAccountClick(AccountType.CASH) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AccountCardItem(
    account: AccountType,
    balance: Double,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "accountPress"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(width = 1.dp, color = CardBorder, shape = RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(vertical = 12.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Simple Account Icon Badge (No text label)
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f))
                    .border(width = 1.dp, color = accentColor.copy(alpha = 0.3f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = account.displayName,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Current Balance
            Text(
                text = CurrencyFormatter.format(balance),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = TextPrimary
                ),
                maxLines = 1
            )
        }
    }
}

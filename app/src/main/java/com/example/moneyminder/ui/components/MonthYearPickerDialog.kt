package com.example.moneyminder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.moneyminder.theme.CardBackground
import com.example.moneyminder.theme.CardBackgroundElevated
import com.example.moneyminder.theme.CardBorder
import com.example.moneyminder.theme.TextMuted
import com.example.moneyminder.theme.TextPrimary
import com.example.moneyminder.theme.TextSecondary

private val monthNames = listOf(
    "JAN", "FEB", "MAR", "APR", "MAY", "JUN",
    "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"
)

@Composable
fun MonthYearPickerDialog(
    currentYear: Int,
    currentMonth: Int,
    onConfirm: (year: Int, month: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedYear by remember { mutableIntStateOf(currentYear) }
    var selectedMonth by remember { mutableIntStateOf(currentMonth) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = CardBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Select Month & Year",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Year Stepper Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBackgroundElevated)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedYear-- }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Year",
                            tint = TextSecondary
                        )
                    }

                    Text(
                        text = selectedYear.toString(),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )

                    IconButton(onClick = { selectedYear++ }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Year",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 12 Months Grid (3 columns x 4 rows)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.height(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(monthNames) { index, name ->
                        val monthNum = index + 1
                        val isSelected = monthNum == selectedMonth

                        Box(
                            modifier = Modifier
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) CardBackgroundElevated else CardBackgroundElevated)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) TextPrimary else CardBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedMonth = monthNum },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = { onConfirm(selectedYear, selectedMonth) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TextPrimary,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Apply", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

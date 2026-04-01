package com.ethiobalance.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethiobalance.app.data.TransactionEntity
import com.ethiobalance.app.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionItem(
    transaction: TransactionEntity,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val isIncome = transaction.type == "INCOME"
    val bgColor = if (isIncome) Emerald50 else Rose50
    val amountColor = if (isIncome) Emerald600 else Rose600
    val iconBg = if (isIncome) Emerald100 else Rose100
    val sign = if (isIncome) "+" else "-"

    val formatted = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(transaction.amount)

    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.US)
    val fullDateFormat = SimpleDateFormat("EEEE, MMM dd yyyy HH:mm:ss", Locale.US)

    val icon = when (transaction.category.uppercase()) {
        "PURCHASE" -> Icons.Default.ShoppingCart
        "CREDIT" -> Icons.Default.AccountBalanceWallet
        "GIFT" -> Icons.Default.CardGiftcard
        "TRANSFER" -> Icons.Default.SwapHoriz
        else -> if (isIncome) Icons.Default.TrendingUp else Icons.Default.TrendingDown
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(20.dp),
        color = bgColor
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = amountColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Description + metadata
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.category.replaceFirstChar { it.uppercase() },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row {
                        Text(
                            text = transaction.source,
                            fontSize = 12.sp,
                            color = Slate400
                        )
                        Text(
                            text = " · ${dateFormat.format(Date(transaction.timestamp))}",
                            fontSize = 12.sp,
                            color = Slate400
                        )
                    }
                }

                // Amount
                Text(
                    text = "${sign}ETB $formatted",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )

                Spacer(modifier = Modifier.width(4.dp))

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand",
                    tint = Slate400,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Expanded details
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = if (isIncome) Emerald100 else Rose100)
                    Spacer(modifier = Modifier.height(8.dp))
                    DetailRow("Date", fullDateFormat.format(Date(transaction.timestamp)))
                    DetailRow("Category", transaction.category)
                    DetailRow("Source", transaction.source)
                    DetailRow("Transaction ID", transaction.id)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Slate400,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            fontSize = 11.sp,
            color = Slate600,
            modifier = Modifier.weight(0.6f)
        )
    }
}

package com.ethiobalance.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethiobalance.app.data.TransactionEntity
import com.ethiobalance.app.ui.Translations
import com.ethiobalance.app.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// Translation helper for type display - converts to lowercase for key lookup
private fun translateType(lang: String, type: String): String {
    val key = type.lowercase().trim()
    val normalizedLang = lang.lowercase().trim().take(2) // Handle "en-US" -> "en"
    val translated = Translations.t(normalizedLang, key)
    return translated.takeIf { it != key } ?: type.uppercase()
}

// Translation helper for category display
private fun translateCategory(lang: String, category: String?): String {
    if (category == null) return ""
    val key = category.lowercase().trim()
    val normalizedLang = lang.lowercase().trim().take(2)
    val translated = Translations.t(normalizedLang, key)
    return translated.takeIf { it != key } ?: category.replaceFirstChar { it.uppercase() }
}

@Composable
fun TransactionItem(
    transaction: TransactionEntity,
    modifier: Modifier = Modifier,
    language: String = "en",
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    val isIncome = transaction.type.uppercase() == "INCOME"

    // Colors mapping to React's emerald-50/40 etc
    val bgColor = if (isIncome) Emerald50.copy(alpha=0.6f) else Rose50.copy(alpha=0.6f)
    val borderColor = if (isIncome) Emerald100.copy(alpha=0.6f) else Rose100.copy(alpha=0.6f)
    
    val selectedBgColor = if (isIncome) Emerald50 else Rose50
    val selectedBorderColor = if (isIncome) Emerald200 else Rose200

    val currentBgColor = if (isSelected) selectedBgColor else bgColor
    val currentBorderColor = if (isSelected) selectedBorderColor else borderColor
    val amountColor = if (isIncome) Emerald600 else Rose600
    val iconBgColor = if (isIncome) Emerald100.copy(alpha=0.6f) else Rose100.copy(alpha=0.6f)
    
    val sign = if (isIncome) "+" else "-"

    val formatted = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(transaction.amount)

    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.US)
    val formattedDate = try { dateFormat.format(Date(transaction.timestamp)) } catch(e:Exception) { "N/A" }

    val iconData = when (transaction.category.uppercase()) {
        "UTILITY", "BILLS" -> Pair(Icons.Default.Bolt, Blue500)
        "GROCERY", "SHOPPING", "MARKET" -> Pair(Icons.Default.ShoppingBag, Purple500)
        "DINING", "FOOD", "RESTAURANT" -> Pair(Icons.Default.Restaurant, Orange500)
        "TELECOM", "RECHARGE", "PHONE" -> Pair(Icons.Default.Smartphone, Green500)
        else -> if (isIncome) Pair(Icons.Default.TrendingUp, Emerald500) else Pair(Icons.Default.TrendingDown, Rose500)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(currentBgColor)
            .border(1.dp, currentBorderColor, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Left Side (Icon + Text)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(iconBgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconData.first,
                            contentDescription = null,
                            tint = iconData.second,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                    
                        Text(
                            text = transaction.partyName?.let { "To: $it" } ?: formattedDate,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate400,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Right Side (Amount + Category + Chevron)
                Row(verticalAlignment = Alignment.Top) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$sign $formatted",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = amountColor
                        )
                        Text(
                            text = translateType(language, transaction.type),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate400,
                            letterSpacing = 2.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

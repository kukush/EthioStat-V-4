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
import androidx.compose.ui.tooling.preview.Preview
import com.ethiobalance.app.data.TransactionEntity
import com.ethiobalance.app.ui.Translations
import com.ethiobalance.app.ui.theme.*
import java.text.NumberFormat
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

// Split partyName into (displayName, identifier) so IDs/phones move to row 2
private fun splitPartyName(partyName: String): Pair<String, String?> {
    // "743236 - Andualem Meketa Mekonen" → name="Andualem Meketa Mekonen", id="743236"
    val merchantMatch = Regex("^(\\d+)\\s*-\\s*(.+)$").find(partyName)
    if (merchantMatch != null) {
        return Pair(merchantMatch.groupValues[2].trim(), merchantMatch.groupValues[1].trim())
    }
    // "SELAMAWIT ALEMU(2519****0146)" or "Shemsu Ertiban (2519****7573)" → name, phone
    val namePhoneMatch = Regex("^(.+?)\\s*\\(([^)]+)\\)$").find(partyName)
    if (namePhoneMatch != null) {
        val part1 = namePhoneMatch.groupValues[1].trim()
        val part2 = namePhoneMatch.groupValues[2].trim()
        // If part1 is digits-only (phone), swap: "0911XX (Name)" → display=Name, id=phone
        return if (part1.all { it.isDigit() || it == '*' }) {
            Pair(part2, part1)
        } else {
            Pair(part1, part2)
        }
    }
    return Pair(partyName, null)
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

    // Use MaterialTheme colors for theme-aware backgrounds (supports dark/forest/midnight themes)
    val colorScheme = MaterialTheme.colorScheme
    
    // Background colors using surfaceVariant with income/expense tinting
    val bgColor = if (isIncome) 
        colorScheme.surfaceVariant.copy(alpha=0.7f) 
    else 
        colorScheme.surfaceVariant.copy(alpha=0.7f)
    
    val borderColor = if (isIncome)
        Emerald500.copy(alpha=0.3f)
    else
        Rose500.copy(alpha=0.3f)
    
    val selectedBgColor = if (isIncome) 
        Emerald500.copy(alpha=0.15f) 
    else 
        Rose500.copy(alpha=0.15f)
    
    val selectedBorderColor = if (isIncome) Emerald500.copy(alpha=0.5f) else Rose500.copy(alpha=0.5f)

    val currentBgColor = if (isSelected) selectedBgColor else bgColor
    val currentBorderColor = if (isSelected) selectedBorderColor else borderColor
    val amountColor = if (isIncome) Emerald500 else Rose500
    val iconBgColor = colorScheme.surfaceVariant.copy(alpha=0.8f)
    
    val sign = if (isIncome) "+" else "-"

    val formatted = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(transaction.amount)

    val formattedDate = try { Translations.formatDate(language, transaction.timestamp) } catch(e:Exception) { "N/A" }

    val iconData = when (transaction.category.uppercase()) {
        "UTILITY", "BILLS" -> Pair(Icons.Default.Bolt, Blue500)
        "GROCERY", "SHOPPING", "MARKET" -> Pair(Icons.Default.ShoppingBag, Purple500)
        "DINING", "FOOD", "RESTAURANT" -> Pair(Icons.Default.Restaurant, Orange500)
        "TELECOM", "RECHARGE", "PHONE", "AIRTIME" -> Pair(Icons.Default.Smartphone, Green500)
        "INTERNET" -> Pair(Icons.Default.Wifi, Blue500)
        "VOICE" -> Pair(Icons.Default.Phone, Green500)
        "SMS" -> Pair(Icons.Default.Message, Purple500)
        "PURCHASE" -> Pair(Icons.Default.ShoppingCart, Orange500)
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                        Text(
                            text = transaction.source,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Medium,
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        // Split party name into display name + optional identifier (phone/ID)
                        val (displayName, identifier) = if (transaction.partyName != null)
                            splitPartyName(transaction.partyName)
                        else Pair(null, null)

                        // Show party name with From:/To: prefix, or date if no party
                        val displayText = when {
                            displayName != null && transaction.category.uppercase() in listOf("INTERNET", "VOICE", "SMS", "TELECOM") ->
                                displayName
                            displayName != null && isIncome -> "${Translations.t(language, "from")}: $displayName"
                            displayName != null -> "${Translations.t(language, "to")}: $displayName"
                            else -> formattedDate
                        }
                        Text(
                            text = displayText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        // Show identifier + date on second row, or just date
                        if (transaction.partyName != null) {
                            val secondRow = if (identifier != null) "$identifier \u00B7 $formattedDate" else formattedDate
                            Text(
                                text = secondRow,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Normal,
                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.padding(top = 1.dp)
                            )
                        }
                        // Show category badge for purchases
                        if (transaction.category.uppercase() in listOf("INTERNET", "VOICE", "SMS", "TELECOM", "PURCHASE", "AIRTIME")) {
                            Text(
                                text = translateCategory(language, transaction.category),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
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
                            color = colorScheme.onSurfaceVariant,
                            letterSpacing = 2.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TransactionItemPreview() {
    TransactionItem(
        transaction = TransactionEntity(
            id = "preview-1",
            type = "INCOME",
            amount = 1500.00,
            category = "PURCHASE",
            source = "TELEBIRR",
            timestamp = System.currentTimeMillis(),
            reference = null,
            partyName = "Abebe Kebede",
            transactionSubType = null
        ),
        language = "en"
    )
}

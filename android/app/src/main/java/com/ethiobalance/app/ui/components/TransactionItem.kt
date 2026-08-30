package com.ethiobalance.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethiobalance.app.data.TransactionEntity
import com.ethiobalance.app.ui.Translations
import com.ethiobalance.app.ui.theme.*
import java.text.NumberFormat
import java.util.*

private fun translateType(lang: String, type: String): String {
    val key = type.lowercase().trim()
    val normalizedLang = lang.lowercase().trim().take(2)
    val translated = Translations.t(normalizedLang, key)
    return translated.takeIf { it != key } ?: type.uppercase()
}

private fun translateCategory(lang: String, category: String?): String {
    if (category == null) return ""
    val key = category.lowercase().trim()
    val normalizedLang = lang.lowercase().trim().take(2)
    val translated = Translations.t(normalizedLang, key)
    return translated.takeIf { it != key } ?: category.replaceFirstChar { it.uppercase() }
}

private fun splitPartyName(partyName: String): Pair<String, String?> {
    val parts = partyName.split(" - ")
    return if (parts.size >= 2) {
        Pair(parts[0], parts.drop(1).joinToString(" - "))
    } else {
        val phoneRegex = Regex("""(\+2519|\+2517|09|07)\d{8}""")
        val match = phoneRegex.find(partyName)
        if (match != null) {
            val phone = match.value
            val name = partyName.replace(phone, "").trim().trim('-').trim()
            if (name.isNotEmpty()) Pair(name, phone) else Pair(partyName, null)
        } else {
            Pair(partyName, null)
        }
    }
}

@Composable
fun TransactionItem(
    transaction: TransactionEntity,
    modifier: Modifier = Modifier,
    language: String = "en",
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    var isExpanded by remember { mutableStateOf(false) }
    val isIncome = transaction.type.uppercase() == "INCOME"
    
    // Explicit Dark Theme Colors
    val bgColor = Slate900.copy(alpha = 0.7f)
    
    val borderColor = if (isIncome)
        Emerald500.copy(alpha=0.2f)
    else
        Rose500.copy(alpha=0.2f)
        
    val selectedBgColor = if (isIncome) 
        Emerald500.copy(alpha=0.15f) 
    else 
        Rose500.copy(alpha=0.15f)
        
    val selectedBorderColor = if (isIncome) Emerald500.copy(alpha=0.4f) else Rose500.copy(alpha=0.4f)

    val currentBgColor = if (isSelected) selectedBgColor else bgColor
    val currentBorderColor = if (isSelected) selectedBorderColor else borderColor
    val amountColor = if (isIncome) Emerald400 else Rose400
    val iconBgColor = Slate800
    
    val sign = if (isIncome) "+" else "-"
    val formatted = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(transaction.amount)
    
    val formattedDate = try { Translations.formatDate(language, transaction.timestamp) } catch(e:Exception) { "N/A" }
    
    val iconData = when (transaction.category.uppercase()) {
        "UTILITY", "BILLS" -> Pair(Icons.Default.Bolt, Blue400)
        "GROCERY", "SHOPPING", "MARKET" -> Pair(Icons.Default.ShoppingBag, Purple400)
        "DINING", "FOOD", "RESTAURANT" -> Pair(Icons.Default.Restaurant, Orange400)
        "TELECOM", "RECHARGE", "PHONE", "AIRTIME" -> Pair(Icons.Default.Smartphone, Emerald400)
        "INTERNET" -> Pair(Icons.Default.Wifi, Blue400)
        "VOICE" -> Pair(Icons.Default.Phone, Emerald400)
        "SMS" -> Pair(Icons.AutoMirrored.Filled.Message, Purple400)
        "PURCHASE" -> Pair(Icons.Default.ShoppingCart, Orange400)
        else -> if (isIncome) Pair(Icons.AutoMirrored.Filled.TrendingUp, Emerald400) else Pair(Icons.AutoMirrored.Filled.TrendingDown, Rose400)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(currentBgColor)
            .border(1.dp, currentBorderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = { 
                isExpanded = !isExpanded
                onClick()
            })
            .padding(14.dp)
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
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(iconBgColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = iconData.first,
                                contentDescription = null,
                                tint = iconData.second,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text(
                            text = transaction.source,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate400,
                            maxLines = 1,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        val (displayName, identifier) = if (transaction.partyName != null)
                            splitPartyName(transaction.partyName)
                        else Pair(null, null)
                        
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
                            color = Color.White,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        if (transaction.partyName != null) {
                            val secondRow = if (identifier != null) "$identifier \u00B7 $formattedDate" else formattedDate
                            Text(
                                text = secondRow,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Normal,
                                color = Slate400,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        
                        if (transaction.category.uppercase() in listOf("INTERNET", "VOICE", "SMS", "TELECOM", "PURCHASE", "AIRTIME")) {
                            Text(
                                text = translateCategory(language, transaction.category),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = Slate300,
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
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = amountColor
                        )
                        Text(
                            text = translateType(language, transaction.type),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate500,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Slate500,
                            modifier = Modifier.size(16.dp).padding(top = 2.dp)
                        )
                    }
                }
            }
            
            // Expanded Content
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Slate800, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))
                
                // Raw SMS / Details
                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Slate950.copy(alpha = 0.5f)).padding(10.dp)) {
                    Text(
                        text = "DETAILS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate500,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = "No details available", // Commented out transaction.rawSmsBody
                        fontSize = 11.sp,
                        color = Slate300,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    if (transaction.reference != null) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(text = "Ref: ${transaction.reference}", fontSize = 10.sp, color = Slate400, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            // Could add a copy button here
                        }
                    }
                }
            }
        }
    }
}

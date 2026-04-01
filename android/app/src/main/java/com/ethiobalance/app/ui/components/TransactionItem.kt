package com.ethiobalance.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.draw.rotate
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

@Composable
fun TransactionItem(
    transaction: TransactionEntity,
    modifier: Modifier = Modifier,
    language: String = "en"
) {
    var isExpanded by remember { mutableStateOf(false) }
    val isIncome = transaction.type.uppercase() == "INCOME"

    // Colors mapping to React's emerald-50/40 etc
    val bgColor = if (isIncome) Emerald50.copy(alpha=0.6f) else Rose50.copy(alpha=0.6f)
    val borderColor = if (isIncome) Emerald100.copy(alpha=0.6f) else Rose100.copy(alpha=0.6f)
    
    val expandedBgColor = if (isIncome) Emerald50 else Rose50
    val expandedBorderColor = if (isIncome) Emerald200 else Rose200

    val currentBgColor = if (isExpanded) expandedBgColor else bgColor
    val currentBorderColor = if (isExpanded) expandedBorderColor else borderColor
    val amountColor = if (isIncome) Emerald600 else Rose600
    val iconBgColor = if (isIncome) Emerald100.copy(alpha=0.6f) else Rose100.copy(alpha=0.6f)
    
    val sign = if (isIncome) "+" else "-"

    val formatted = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(transaction.amount)

    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.US)
    val fullDateFormat = SimpleDateFormat("EEEE, MMM dd yyyy HH:mm:ss", Locale.US)
    val formattedDate = try { dateFormat.format(Date(transaction.timestamp)) } catch(e:Exception) { "N/A" }
    val fullFormattedDate = try { fullDateFormat.format(Date(transaction.timestamp)) } catch(e:Exception) { "N/A" }

    val iconData = when (transaction.category.uppercase()) {
        "UTILITY", "BILLS" -> Pair(Icons.Default.Bolt, Blue500)
        "GROCERY", "SHOPPING", "MARKET" -> Pair(Icons.Default.ShoppingBag, Purple500)
        "DINING", "FOOD", "RESTAURANT" -> Pair(Icons.Default.Restaurant, Orange500)
        "TELECOM", "RECHARGE", "PHONE" -> Pair(Icons.Default.Smartphone, Green500)
        else -> if (isIncome) Pair(Icons.Default.TrendingUp, Emerald500) else Pair(Icons.Default.TrendingDown, Rose500)
    }

    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(300),
        label = "chevron_rotate"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(currentBgColor)
            .border(1.dp, currentBorderColor, RoundedCornerShape(24.dp))
            .clickable { isExpanded = !isExpanded }
            .then(if(isExpanded) Modifier.shadow(8.dp, RoundedCornerShape(24.dp), ambientColor = if(isIncome) Emerald200 else Rose200) else Modifier)
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
                            text = transaction.category.replaceFirstChar { it.uppercase() }.takeIf { it.isNotBlank() } ?: "Transaction", // Fallback for description if category is all we have
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${transaction.source} • $formattedDate",
                            fontSize = 11.sp,
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
                            text = "$sign ETB $formatted",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = amountColor
                        )
                        Text(
                            text = transaction.category.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate400,
                            letterSpacing = 2.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                        tint = Slate300,
                        modifier = Modifier.size(16.dp).rotate(chevronRotation).padding(top = 4.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(300)),
                exit = shrinkVertically(animationSpec = tween(300))
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(color = Slate50)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Date Detail
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, null, tint = Slate500, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("FULL DATE", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Slate400, letterSpacing = 2.sp)
                                Text(fullFormattedDate, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate700)
                            }
                        }
                        
                        // Category Detail
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalOffer, null, tint = Slate500, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("CATEGORY", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Slate400, letterSpacing = 2.sp)
                                Text(transaction.category, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate700)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Transaction ID
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Slate50).padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, null, tint = Slate500, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("TRANSACTION ID", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Slate400, letterSpacing = 2.sp)
                            Text(transaction.id, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate700)
                        }
                    }
                }
            }
        }
    }
}

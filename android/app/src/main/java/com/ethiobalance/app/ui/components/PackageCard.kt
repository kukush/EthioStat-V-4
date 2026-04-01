package com.ethiobalance.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethiobalance.app.data.BalancePackageEntity
import com.ethiobalance.app.ui.Translations
import com.ethiobalance.app.ui.theme.*

@Composable
fun PackageCard(
    pkg: BalancePackageEntity,
    language: String,
    modifier: Modifier = Modifier
) {
    val typeColors = mapOf(
        "DATA" to Pair(Blue600, Blue50),
        "VOICE" to Pair(Green600, Green50),
        "SMS" to Pair(Purple600, Color(0xFFF3E8FF)),
        "BONUS" to Pair(Amber500, Color(0xFFFFFBEB)),
        "AIRTIME" to Pair(Blue600, Blue50),
        "DATA_AIRTIME" to Pair(Blue600, Blue50),
        "INTERNET" to Pair(Blue600, Blue50)
    )

    val (accentColor, _) = typeColors[pkg.type.uppercase()] ?: Pair(Blue600, Blue50)
    val bgColor = accentColor.copy(alpha = 0.08f)

    val usagePercent = if (pkg.totalAmount > 0) {
        (pkg.remainingAmount / pkg.totalAmount).coerceIn(0.0, 1.0).toFloat()
    } else 0f

    val now = System.currentTimeMillis()
    val daysLeft = if (pkg.expiryDate > now) {
        ((pkg.expiryDate - now) / (24L * 60 * 60 * 1000)).toInt()
    } else 0

    val totalDays = if (pkg.lastUpdated > 0 && pkg.expiryDate > pkg.lastUpdated) {
        ((pkg.expiryDate - pkg.lastUpdated) / (24L * 60 * 60 * 1000)).toInt().coerceAtLeast(1)
    } else 30

    val expiryPercent = (daysLeft.toFloat() / totalDays).coerceIn(0f, 1f)

    val typeLabel = when (pkg.type.uppercase()) {
        "DATA", "DATA_AIRTIME", "INTERNET" -> Translations.t(language, "data")
        "VOICE" -> Translations.t(language, "voice")
        "SMS" -> Translations.t(language, "sms")
        "BONUS" -> Translations.t(language, "bonus")
        "AIRTIME" -> Translations.t(language, "availableAirtime")
        else -> pkg.type
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: Info
            Column(modifier = Modifier.weight(1f)) {
                // Type badge
                Text(
                    text = typeLabel.uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Package name
                val displayName = when {
                    pkg.id.contains("-sim") -> {
                        val type = pkg.id.split("-").firstOrNull() ?: ""
                        when (type.lowercase()) {
                            "voice" -> "Voice Package"
                            "sms" -> "SMS Package"
                            "internet", "data" -> "Data Package"
                            "bonus" -> "Bonus Balance"
                            else -> type.replaceFirstChar { it.uppercase() }
                        }
                    }
                    pkg.id.contains("_") -> pkg.id.split("_").joinToString(" ") { 
                        it.replaceFirstChar { c -> if (c.isLowerCase()) c.uppercase() else c.toString() }
                    }
                    else -> pkg.id.replaceFirstChar { it.uppercase() }
                }
                Text(
                    text = displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Value + unit
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = String.format("%.1f", pkg.remainingAmount),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Slate900
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = pkg.unit,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate400,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Expiry progress bar
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(accentColor.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(expiryPercent)
                                .clip(RoundedCornerShape(2.dp))
                                .background(accentColor)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$daysLeft ${Translations.t(language, "daysLeft")}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate400
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Right side: Circular progress ring
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(72.dp)
            ) {
                Canvas(modifier = Modifier.size(72.dp)) {
                    val strokeWidth = 6.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                    val arcSize = Size(radius * 2, radius * 2)

                    // Background ring
                    drawArc(
                        color = accentColor.copy(alpha = 0.15f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    // Progress ring
                    drawArc(
                        color = accentColor,
                        startAngle = -90f,
                        sweepAngle = 360f * usagePercent,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                Text(
                    text = "${(usagePercent * 100).toInt()}%",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = accentColor
                )
            }
        }
    }
}

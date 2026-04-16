package com.ethiobalance.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethiobalance.app.ui.theme.*
import com.ethiobalance.app.ui.Translations
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PackageCard(
    type: String, // "internet", "voice", "sms", "bonus"
    value: Double,
    total: Double,
    unit: String,
    label: String,
    expiryMs: Long,
    daysLeft: Int,
    totalDays: Int,
    language: String,
    modifier: Modifier = Modifier
) {
    // Determine Theme based on type
    val theme = when (type.lowercase()) {
        "internet", "data" -> PackageTheme(Blue600, Color.White, Blue400.copy(alpha=0.3f), Color.White, Blue100, Blue400.copy(alpha=0.3f), Color.White)
        "voice" -> PackageTheme(Green600, Color.White, Green400.copy(alpha=0.3f), Color.White, Green100, Green400.copy(alpha=0.3f), Color.White)
        "sms" -> PackageTheme(Purple600, Color.White, Purple400.copy(alpha=0.3f), Color.White, Purple100, Purple400.copy(alpha=0.3f), Color.White)
        "bonus" -> PackageTheme(Amber500, Color.White, Amber300.copy(alpha=0.3f), Color.White, Amber50, Amber300.copy(alpha=0.3f), Color.White)
        else -> PackageTheme(Blue600, Color.White, Blue400.copy(alpha=0.3f), Color.White, Blue100, Blue400.copy(alpha=0.3f), Color.White)
    }

    val percentage = if (total > 0) Math.min(100.0, (value / total) * 100) else 0.0
    val expiryPercentage = if (totalDays > 0) Math.min(100.0, (daysLeft.toDouble() / totalDays) * 100) else 0.0

    // Urgency colors for circular bar (usage remaining)
    val circularColor = when {
        percentage < 5  -> Rose500    // 🔴 Critical: <5% remaining
        percentage < 20 -> Amber500   // 🟠 Warning: <20% remaining (>80% used)
        else -> theme.progressFg      // 🟢 Healthy
    }

    // Urgency colors for linear bar (expiry)
    val expiryBarColor = when {
        daysLeft < 3 -> Rose500       // 🔴 Critical: <3 days left
        daysLeft < 7 -> Amber500      // 🟠 Warning: 3-6 days left
        else -> theme.barFg           // 🟢 Healthy
    }

    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
    val expiryDateStr = if (expiryMs > 0) dateFormat.format(Date(expiryMs)) else "N/A"
    
    val translatedType = Translations.t(language, type.lowercase())

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .background(theme.bg)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = translatedType.uppercase(),
                    color = theme.accent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = label, 
                    color = theme.text,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString(),
                        color = theme.text,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = unit,
                        color = theme.text.copy(alpha=0.8f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Validity Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(Translations.t(language, "validity").uppercase().takeIf { it.isNotEmpty() } ?: "VALIDITY", fontSize = 9.sp, fontWeight = FontWeight.Black, color = theme.text.copy(alpha=0.6f))
                    Text("$daysLeft / $totalDays ${Translations.t(language, "daysLeft")}", fontSize = 10.sp, fontWeight = FontWeight.Black, color = theme.text)
                }
                Spacer(Modifier.height(4.dp))
                // Linear Progress
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(theme.barBg)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((expiryPercentage / 100f).toFloat())
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(expiryBarColor)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${Translations.t(language, "expires").uppercase().takeIf { it.isNotEmpty() } ?: "EXPIRES"}: $expiryDateStr",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.text.copy(alpha=0.6f)
                )
            }

            Spacer(Modifier.width(16.dp))

            // Right Circular Progress
            Box(
                modifier = Modifier.size(96.dp),
                contentAlignment = Alignment.Center
            ) {
                // Circle Drawing
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .drawBehind {
                            drawArc(
                                color = theme.progressBg,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = circularColor,
                                startAngle = -90f,
                                sweepAngle = (percentage * 3.6).toFloat(),
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${percentage.toInt()}%",
                        color = theme.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${total.toInt()} ${unit}",
                        color = theme.text.copy(alpha=0.8f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

data class PackageTheme(
    val bg: Color,
    val text: Color,
    val progressBg: Color,
    val progressFg: Color,
    val accent: Color,
    val barBg: Color,
    val barFg: Color
)

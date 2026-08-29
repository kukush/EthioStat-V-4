package com.ethiobalance.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethiobalance.app.ui.Translations
import com.ethiobalance.app.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun TelecomAssetCard(
    language: String,
    dataVol: Double,
    voiceVol: Double,
    smsVol: Double,
    airtimeBalance: Double = 0.0,
    onOpenUssd: ((action: String) -> Unit)? = null,
    isCompact: Boolean = false
) {
    val fmt = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    
    val dataGb = dataVol / 1024.0 // Assuming dataVol is MB initially, or it's already GB? In Android's previous code it showed raw dataVol. Let's assume it's GB if that's what was shown, wait: `dataVol` is computed in HomeScreen.kt. Let's just use it directly.

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Slate900,
        border = BorderStroke(1.dp, Slate800),
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(if (isCompact) 14.dp else 20.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Blue600.copy(alpha = 0.1f))
                            .border(1.dp, Blue500.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CellTower, null, tint = Blue400, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            Translations.t(language, "telecomAssets").takeIf { it.isNotEmpty() }?.uppercase() ?: "TELECOM ASSETS",
                            fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate400, letterSpacing = 2.sp
                        )
                        Text(
                            "Ethio Telecom / *804#",
                            fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Slate500
                        )
                    }
                }
                
                if (onOpenUssd != null && !isCompact) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            color = Emerald600.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.3f)),
                            onClick = { onOpenUssd("recharge") }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
                                Icon(Icons.Default.AddCircleOutline, null, tint = Emerald300, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(Translations.t(language, "recharge").takeIf { it.isNotEmpty() } ?: "Recharge", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Emerald300)
                            }
                        }
                        
                        Surface(
                            color = Cyan600.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Cyan500.copy(alpha = 0.3f)),
                            onClick = { onOpenUssd("transfer") }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
                                Icon(Icons.Default.SwapHoriz, null, tint = Cyan300, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(Translations.t(language, "transfer").takeIf { it.isNotEmpty() } ?: "Transfer", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Cyan300)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Grid of 4 items: Data, Voice, SMS, Airtime
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 12.dp)) {
                // Column 1: Data and SMS
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 12.dp)) {
                    // Data Card
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Blue900.copy(alpha=0.4f), Slate900)), RoundedCornerShape(16.dp))
                            .border(1.dp, Blue500.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            .padding(if (isCompact) 10.dp else 14.dp)
                    ) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Language, null, tint = Blue400, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(Translations.t(language, "data").takeIf { it.isNotEmpty() }?.uppercase() ?: "DATA", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Blue400)
                                }
                                Box(modifier = Modifier.background(Blue500.copy(alpha=0.2f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                    Text("GB", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Blue300)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                String.format(Locale.US, "%.2f", dataVol),
                                fontSize = if (isCompact) 18.sp else 24.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = (-0.5).sp
                            )
                            Text("Available Internet", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Slate400, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                    
                    // SMS Card
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Amber900.copy(alpha=0.4f), Slate900)), RoundedCornerShape(16.dp))
                            .border(1.dp, Amber500.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            .padding(if (isCompact) 10.dp else 14.dp)
                    ) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ChatBubbleOutline, null, tint = Amber400, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(Translations.t(language, "sms").takeIf { it.isNotEmpty() }?.uppercase() ?: "SMS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Amber400)
                                }
                                Box(modifier = Modifier.background(Amber500.copy(alpha=0.2f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                    Text("SMS", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Amber300)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                smsVol.toInt().toString(),
                                fontSize = if (isCompact) 18.sp else 24.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = (-0.5).sp
                            )
                            Text("SMS Left", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Slate400, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
                
                // Column 2: Voice and Airtime
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 12.dp)) {
                    // Voice Card
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Purple900.copy(alpha=0.4f), Slate900)), RoundedCornerShape(16.dp))
                            .border(1.dp, Purple500.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            .padding(if (isCompact) 10.dp else 14.dp)
                    ) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Phone, null, tint = Purple400, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(Translations.t(language, "voice").takeIf { it.isNotEmpty() }?.uppercase() ?: "VOICE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Purple400)
                                }
                                Box(modifier = Modifier.background(Purple500.copy(alpha=0.2f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                    Text("MIN", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Purple300)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                voiceVol.toInt().toString(),
                                fontSize = if (isCompact) 18.sp else 24.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = (-0.5).sp
                            )
                            Text("Voice Minutes", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Slate400, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                    
                    // Airtime Card
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Emerald900.copy(alpha=0.4f), Slate900)), RoundedCornerShape(16.dp))
                            .border(1.dp, Emerald500.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            .padding(if (isCompact) 10.dp else 14.dp)
                    ) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, null, tint = Emerald400, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("AIRTIME", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Emerald400)
                                }
                                Box(modifier = Modifier.background(Emerald500.copy(alpha=0.2f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                    Text("ETB", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Emerald300)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                fmt.format(airtimeBalance),
                                fontSize = if (isCompact) 18.sp else 24.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = (-0.5).sp
                            )
                            Text("Prepaid Credit", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Slate400, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
            }
        }
    }
}

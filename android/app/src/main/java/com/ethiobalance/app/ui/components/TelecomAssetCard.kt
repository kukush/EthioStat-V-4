package com.ethiobalance.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
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
    modifier: Modifier = Modifier
) {
    val fmt = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(40.dp))
            .background(Slate900)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(listOf(Blue600.copy(alpha = 0.3f), Color.Transparent)),
                    radius = size.width * 0.4f,
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.1f)
                )
                drawCircle(
                    brush = Brush.radialGradient(listOf(Purple600.copy(alpha = 0.3f), Color.Transparent)),
                    radius = size.width * 0.4f,
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.9f)
                )
            }
    ) {
        Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Bolt, null, tint = Blue400, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        Translations.t(language, "telecomAssets").takeIf { it.isNotEmpty() }?.uppercase()
                            ?: "TELECOM ASSETS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.6f),
                        letterSpacing = 2.sp
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Language, null, tint = Blue400, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    val ethioTelecom = Translations.t(language, "ethio_telecom").takeIf { it.isNotBlank() } ?: "ETHIO TELECOM"
                    Text(
                        ethioTelecom.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Blue400,
                        letterSpacing = 2.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Package Summaries
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PackageItem(
                    Translations.t(language, "data_label").uppercase(),
                    "%.1f".format(Locale.US, dataVol),
                    " ${Translations.t(language, "gb_unit")}",
                    Blue500
                )
                PackageItem(
                    Translations.t(language, "audio_label").uppercase(),
                    fmt.format(voiceVol),
                    " ${Translations.t(language, "min_unit")}",
                    Emerald500
                )
                PackageItem(
                    Translations.t(language, "sms_label").uppercase(),
                    fmt.format(smsVol),
                    " ${Translations.t(language, "sms_unit")}",
                    Purple500
                )
            }
        }
    }
}

@Composable
private fun PackageItem(label: String, value: String, unit: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(24.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                label,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.6f),
                letterSpacing = 2.sp
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                Text(unit, fontSize = 8.sp, color = Color.White.copy(alpha = 0.6f), modifier = Modifier.padding(bottom = 1.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TelecomAssetCardPreview() {
    TelecomAssetCard(language = "en", dataVol = 3.5, voiceVol = 120.0, smsVol = 50.0)
}

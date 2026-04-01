package com.ethiobalance.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethiobalance.app.ui.theme.*

@Composable
fun Chip(
    label: String,
    variant: String = "default",
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, borderColor) = when (variant.lowercase()) {
        "internet", "data" -> Triple(Blue100, Blue700, Blue200)
        "voice" -> Triple(Green100, Green700, Green200)
        "sms" -> Triple(Purple100, Purple700, Purple200)
        "bonus" -> Triple(Amber100, Amber700, Amber200)
        else -> Triple(Slate100, Slate700, Slate200)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(percent = 50),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Text(
            text = label.uppercase(),
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
        )
    }
}

package com.ethiobalance.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethiobalance.app.ui.Translations
import com.ethiobalance.app.ui.theme.*

data class NavTab(
    val route: String,
    val labelKey: String,
    val icon: ImageVector,
    val activeColor: Color,
    val activeBg: Color
)

val navTabs = listOf(
    NavTab("home", "home", Icons.Default.Home, Slate900, Slate100),
    NavTab("telecom", "telecom", Icons.Default.CardGiftcard, Blue600, Blue50),
    NavTab("transactions", "transactions", Icons.Default.SwapHoriz, Green600, Green50),
    NavTab("settings", "settings", Icons.Default.Settings, Slate900, Slate100),
)

@Composable
fun BottomNavBar(
    currentRoute: String,
    language: String,
    onTabSelected: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.95f),
        tonalElevation = 0.dp,
        shadowElevation = 8.dp
    ) {
        Column {
            HorizontalDivider(color = Slate100, thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                navTabs.forEach { tab ->
                    val isActive = currentRoute == tab.route
                    val scale by animateFloatAsState(if (isActive) 1.1f else 1f, label = "scale")

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .scale(scale)
                            .clickable { onTabSelected(tab.route) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Active dot indicator
                        if (isActive) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(tab.activeColor)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Icon with background pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isActive) tab.activeBg else Color.Transparent)
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.labelKey,
                                tint = if (isActive) tab.activeColor else Slate400,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = Translations.t(language, tab.labelKey),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) tab.activeColor else Slate400,
                            textAlign = TextAlign.Center,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}

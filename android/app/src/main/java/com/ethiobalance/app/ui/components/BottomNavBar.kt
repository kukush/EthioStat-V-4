package com.ethiobalance.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.tooling.preview.Preview
import com.ethiobalance.app.ui.Translations
import com.ethiobalance.app.ui.theme.*

data class NavTab(
    val route: String,
    val labelKey: String,
    val icon: ImageVector,
    val activeColor: Color,
    val activeBg: Color
)

// Colors will be determined dynamically based on theme
val navTabs = listOf(
    NavTab("home", "home", Icons.Default.Home, Color.Unspecified, Color.Unspecified),
    NavTab("telecom", "telecom", Icons.Default.CardGiftcard, Color.Unspecified, Color.Unspecified),
    NavTab("transactions", "transactions", Icons.Default.SwapHoriz, Color.Unspecified, Color.Unspecified),
    NavTab("settings", "settings", Icons.Default.Settings, Color.Unspecified, Color.Unspecified),
)

@Composable
fun BottomNavBar(
    currentRoute: String,
    language: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    hasPermissionWarning: Boolean = false
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 0.dp,
        shadowElevation = 8.dp
    ) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
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
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Icon with background pill + optional warning badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.labelKey,
                                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            if (tab.route == "settings" && hasPermissionWarning) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 4.dp, y = (-4).dp)
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.error)
                                        .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = Translations.t(language, tab.labelKey),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BottomNavBarPreview() {
    BottomNavBar(currentRoute = "home", language = "en", onTabSelected = {})
}

package com.ethiobalance.app.ui.screens

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethiobalance.app.AppConstants
import com.ethiobalance.app.ui.Translations
import com.ethiobalance.app.ui.theme.Emerald600
import com.ethiobalance.app.ui.theme.Slate800
import com.ethiobalance.app.ui.theme.Slate900
import com.ethiobalance.app.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

internal object OnboardingConfig {
    const val TOTAL_PAGES = 4

    fun shouldShowOnboarding(hasSeenOnboarding: Boolean): Boolean = !hasSeenOnboarding
}

@Composable
fun OnboardingScreen(
    settingsViewModel: SettingsViewModel,
    onComplete: () -> Unit
) {
    val language by settingsViewModel.language.collectAsState()
    val pagerState = rememberPagerState(pageCount = { OnboardingConfig.TOTAL_PAGES })
    val scope = rememberCoroutineScope()

    val steps = listOf(
        Triple(
            "100% Offline & Private Financial Manager",
            "EthioStat is a 100% offline-first financial and telecommunication resource manager designed for privacy and speed.",
            Icons.Default.Security
        ),
        Triple(
            "Smart SMS Parsing & Reconciliation",
            "Automatically extracts transaction amounts, fees, references, and ledger balances from Telebirr, CBE, Awash, Dashen, and 20+ Ethiopian banks.",
            Icons.Default.Memory
        ),
        Triple(
            "Ethio Telecom Resource Tracking",
            "Keep track of your Internet Data (GB), Voice Minutes, and SMS packs. Check balances with *804#, recharge scratch cards with *805*, or transfer airtime via *806*.",
            Icons.Default.Smartphone
        ),
        Triple(
            "Multi-Lingual Experience",
            "Fully localized in English, Amharic (አማርኛ), and Afaan Oromoo with Ethiopian calendar dates and native banking terms.",
            Icons.Default.Layers
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(32.dp),
            color = Slate900,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top row with branding and language picker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brush.linearGradient(listOf(Emerald600, Color(0xFF3B82F6)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = com.ethiobalance.app.R.drawable.app_icon),
                            contentDescription = AppConstants.APP_NAME,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("EN" to "en", "አማ" to "am", "OR" to "om").forEach { (label, code) ->
                            LanguageChip(
                                label = label,
                                selected = language == code,
                                onClick = { settingsViewModel.setLanguage(code) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Pager content
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                ) { page ->
                    val (title, desc, icon) = steps[page]
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Slate800)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (page == 0) {
                                Image(
                                    painter = painterResource(id = com.ethiobalance.app.R.drawable.app_icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = when (page) {
                                        1 -> Color(0xFFC084FC) // purple-400
                                        2 -> Color(0xFF60A5FA) // blue-400
                                        else -> Color(0xFFFBBF24) // amber-400
                                    },
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = desc,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFFCBD5E1), // slate-300
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Step Indicator dots
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(OnboardingConfig.TOTAL_PAGES) { index ->
                        val selected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .height(8.dp)
                                .width(if (selected) 24.dp else 8.dp)
                                .clip(CircleShape)
                                .background(if (selected) Emerald600 else Slate800)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Bottom buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val currentPage = pagerState.currentPage
                    if (currentPage > 0) {
                        TextButton(
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(currentPage - 1) }
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFCBD5E1))
                        ) {
                            Text(
                                text = Translations.t(language, "back"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }

                    Button(
                        onClick = {
                            if (currentPage < OnboardingConfig.TOTAL_PAGES - 1) {
                                scope.launch { pagerState.animateScrollToPage(currentPage + 1) }
                            } else {
                                Log.d("OnboardingDebug", "Onboarding completed")
                                onComplete()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Emerald600,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text(
                            text = if (currentPage == OnboardingConfig.TOTAL_PAGES - 1) Translations.t(language, "onboardingGetStarted") else Translations.t(language, "next"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (currentPage < OnboardingConfig.TOTAL_PAGES - 1) {
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Emerald600 else Slate800,
        modifier = Modifier.height(32.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.White else Color(0xFF94A3B8)
            )
        }
    }
}

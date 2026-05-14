package com.ethiobalance.app.ui.screens

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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethiobalance.app.ui.Translations
import com.ethiobalance.app.ui.theme.Blue400
import com.ethiobalance.app.ui.theme.Blue600
import com.ethiobalance.app.ui.theme.Emerald600
import com.ethiobalance.app.ui.theme.Rose500
import com.ethiobalance.app.ui.theme.Slate400
import com.ethiobalance.app.ui.theme.Slate600
import com.ethiobalance.app.ui.theme.Slate700
import com.ethiobalance.app.ui.theme.Slate800
import com.ethiobalance.app.ui.theme.Slate900
import com.ethiobalance.app.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    settingsViewModel: SettingsViewModel,
    onComplete: () -> Unit
) {
    val language by settingsViewModel.language.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 5 })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Pager content
        Box(modifier = Modifier.weight(1f)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> OnboardingWelcomeSlide(
                        language = language,
                        settingsViewModel = settingsViewModel
                    )
                    1 -> OnboardingSlide(
                        language = language,
                        titleKey = "onboardingTelecomTitle",
                        descKey = "onboardingTelecomDesc",
                        punchLineContent = { PunchLine(language, "onboardingPunchTelecom") },
                        illustration = { TelecomIllustration() },
                        settingsViewModel = settingsViewModel,
                        showLanguagePicker = false
                    )
                    2 -> OnboardingSlide(
                        language = language,
                        titleKey = "onboardingTransactionTitle",
                        descKey = "onboardingTransactionDesc",
                        punchLineContent = { PunchLine(language, "onboardingPunchTrans") },
                        illustration = { TransactionsIllustration() },
                        settingsViewModel = settingsViewModel,
                        showLanguagePicker = false
                    )
                    3 -> OnboardingSlide(
                        language = language,
                        titleKey = "onboardingSettingsTitle",
                        descKey = "onboardingSettingsDesc",
                        punchLineContent = { PunchLine(language, "onboardingPunchSetting") },
                        illustration = { SettingsIllustration() },
                        settingsViewModel = settingsViewModel,
                        showLanguagePicker = false
                    )
                    4 -> OnboardingSetupSlide(
                        language = language,
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Dot indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(5) { index ->
                    val selected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (selected) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) Emerald600 else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val currentPage = pagerState.currentPage

                // Skip button (slides 0-3 only)
                if (currentPage < 4) {
                    TextButton(
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(4) }
                        }
                    ) {
                        Text(
                            text = Translations.t(language, "onboardingSkip"),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(64.dp))
                }

                // Next / Get Started button
                val isLastPage = currentPage == 4
                Button(
                    onClick = {
                        if (isLastPage) {
                            onComplete()
                        } else {
                            scope.launch { pagerState.animateScrollToPage(currentPage + 1) }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Emerald600,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(
                        text = if (isLastPage) Translations.t(language, "onboardingGetStarted") else Translations.t(language, "onboardingNext"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (!isLastPage) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun OnboardingSlide(
    language: String,
    titleKey: String,
    descKey: String,
    punchLineContent: @Composable () -> Unit,
    illustration: @Composable () -> Unit,
    settingsViewModel: SettingsViewModel,
    showLanguagePicker: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Logo + optional language picker row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // App icon with gradient background
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(listOf(Emerald600, Blue600))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = com.ethiobalance.app.R.drawable.app_icon),
                    contentDescription = "EthioStat",
                    modifier = Modifier.size(24.dp)
                )
            }
            if (showLanguagePicker) {
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
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Phone frame with illustration only (compact height)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .clip(RoundedCornerShape(32.dp))
                .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            illustration()
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Punch line - cream card below phone frame
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFFDF6E3)) // Cream background
                .border(1.dp, Color(0xFFE8DCC8), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            punchLineContent()
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Title
        Text(
            text = Translations.t(language, titleKey),
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Description
        Text(
            text = Translations.t(language, descKey),
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun PunchLine(language: String, key: String) {
    val text = Translations.t(language, key)
    val lines = text.split("\n")
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        lines.forEach { line ->
            when {
                line.isEmpty() -> Spacer(Modifier.height(2.dp))
                line.first().let { it.isSurrogate() || it.code > 0x2000 } && !line.startsWith("•") -> {
                    // Emoji header line
                    val spaceIdx = line.indexOf(' ')
                    val emoji = if (spaceIdx > 0) line.substring(0, spaceIdx) else ""
                    val label = if (spaceIdx > 0) line.substring(spaceIdx + 1) else line
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(emoji, fontSize = 12.sp)
                        Spacer(Modifier.width(5.dp))
                        Text(
                            label,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Emerald600,
                            letterSpacing = 1.sp
                        )
                    }
                }
                line.startsWith("•") -> {
                    Text(
                        line,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black,
                        lineHeight = 15.sp
                    )
                }
                else -> {
                    Text(
                        line,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Emerald600,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingSetupSlide(
    language: String,
    settingsViewModel: SettingsViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Logo row with app icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(listOf(Emerald600, Blue600))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = com.ethiobalance.app.R.drawable.app_icon),
                    contentDescription = "EthioStat",
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = Translations.t(language, "onboardingSetupTitle"),
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = Translations.t(language, "onboardingSetupDesc"),
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Punch line
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Slate900,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Slate900.copy(alpha = 0.97f))
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                PunchLine(language, "onboardingPunchSetup")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SMS Permission Warning
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF7C2D12).copy(alpha = 0.2f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        Translations.t(language, "onboardingPermissionWarningTitle"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFF59E0B)
                    )
                    Text(
                        Translations.t(language, "onboardingPermissionWarningBody"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF000000),
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

    }
}

@Composable
private fun LanguageChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) Emerald600 else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.height(40.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ========== Welcome Slide ==========

@Composable
private fun OnboardingWelcomeSlide(
    language: String,
    settingsViewModel: SettingsViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Slate900, MaterialTheme.colorScheme.background)
                )
            )
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))

        // Language chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("EN" to "en", "አማ" to "am", "OR" to "om").forEach { (label, code) ->
                LanguageChip(
                    label = label,
                    selected = language == code,
                    onClick = { settingsViewModel.setLanguage(code) }
                )
                Spacer(Modifier.width(6.dp))
            }
        }

        Spacer(Modifier.height(28.dp))

        // Logo with app icon
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(listOf(Emerald600, Blue600))
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = com.ethiobalance.app.R.drawable.app_icon),
                contentDescription = "EthioStat",
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        // App name
        Text(
            Translations.t(language, "onboardingWelcomeTitle"),
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Text(
            Translations.t(language, "onboardingWelcomeSubtitle"),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Emerald600,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(20.dp))

        // Description - elegant, white, readable
        Text(
            Translations.t(language, "onboardingWelcomeDesc"),
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = Color.White.copy(alpha = 0.9f),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(Modifier.height(24.dp))

        // Feature highlights using punch lines
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Slate800)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Telecom features
            WelcomePunchLine(language, "onboardingPunchTelecom", Emerald600)
            // Transaction features
            WelcomePunchLine(language, "onboardingPunchTrans", Blue400)
            // Settings features
            WelcomePunchLine(language, "onboardingPunchSetting", Color(0xFFA78BFA))
            // Privacy/Setup features (just first section)
            WelcomePunchLineSection(language, "onboardingPunchSetup", 0, Color(0xFF34D399))
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun WelcomeFeatureRow(emoji: String, title: String, desc: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Emerald600.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) { Text(emoji, fontSize = 18.sp) }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontSize = 10.sp, fontWeight = FontWeight.Black, color = Emerald600, letterSpacing = 1.sp)
            Text(desc, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Slate400, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun WelcomeBadge(emoji: String, label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Slate700)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(emoji, fontSize = 14.sp)
        Spacer(Modifier.width(5.dp))
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Slate400, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun WelcomePunchLine(language: String, key: String, accentColor: Color) {
    val text = Translations.t(language, key)
    val lines = text.split("\n")
    // Get the emoji header (first line that starts with emoji)
    val headerLine = lines.firstOrNull { it.isNotEmpty() && (it.first().isSurrogate() || it.first().code > 0x2000) } ?: return
    val spaceIdx = headerLine.indexOf(' ')
    val emoji = if (spaceIdx > 0) headerLine.substring(0, spaceIdx) else ""
    val label = if (spaceIdx > 0) headerLine.substring(spaceIdx + 1) else headerLine
    
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) { Text(emoji, fontSize = 18.sp) }
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = accentColor,
            letterSpacing = 0.5.sp,
            lineHeight = 14.sp
        )
    }
}

@Composable
private fun WelcomePunchLineSection(language: String, key: String, sectionIndex: Int, accentColor: Color) {
    val text = Translations.t(language, key)
    val lines = text.split("\n")
    // Find sections (lines starting with emoji)
    val sections = mutableListOf<List<String>>()
    var currentSection = mutableListOf<String>()
    lines.forEach { line ->
        if (line.isNotEmpty() && (line.first().isSurrogate() || line.first().code > 0x2000)) {
            if (currentSection.isNotEmpty()) sections.add(currentSection.toList())
            currentSection = mutableListOf(line)
        } else if (line.isNotEmpty()) {
            currentSection.add(line)
        }
    }
    if (currentSection.isNotEmpty()) sections.add(currentSection.toList())
    
    // Get requested section
    val section = sections.getOrNull(sectionIndex) ?: return
    val headerLine = section.firstOrNull() ?: return
    val spaceIdx = headerLine.indexOf(' ')
    val emoji = if (spaceIdx > 0) headerLine.substring(0, spaceIdx) else ""
    val label = if (spaceIdx > 0) headerLine.substring(spaceIdx + 1) else headerLine
    
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) { Text(emoji, fontSize = 18.sp) }
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = accentColor,
            letterSpacing = 0.5.sp,
            lineHeight = 14.sp
        )
    }
}

// ========== Compose-Drawn Illustrations ==========

@Composable
private fun TelecomIllustration() {
    val bgColor = Slate900
    val surfaceColor = Slate800

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // ── Telecom Asset Card (compact) ──────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(surfaceColor)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(listOf(Blue600.copy(alpha = 0.35f), Color.Transparent)),
                        radius = size.width * 0.45f,
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.88f, size.height * 0.1f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(listOf(Emerald600.copy(alpha = 0.25f), Color.Transparent)),
                        radius = size.width * 0.4f,
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.9f)
                    )
                }
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bolt, null, tint = Blue400, modifier = Modifier.size(10.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("TELECOM ASSETS", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Slate400, letterSpacing = 1.5.sp)
                    }
                    Text("ETHIO TELECOM", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Blue400, letterSpacing = 1.sp)
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TelecomAssetItem("DATA", "8.3", " GB", Blue400)
                    TelecomAssetItem("VOICE", "244", " MIN", Color(0xFF34D399))
                    TelecomAssetItem("SMS", "136", " SMS", Color(0xFFA78BFA))
                }
            }
        }

        // ── Action buttons (compact) ──────────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TelecomActionBtn("SYNC", Icons.Default.Refresh, Blue600, modifier = Modifier.weight(1f))
            TelecomActionBtn("RECHARGE", Icons.Default.Add, Emerald600, modifier = Modifier.weight(1f))
        }

        // ── Section label ─────────────────────────────────────────────────────
        Text("ACTIVE PACKAGES", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Slate400, letterSpacing = 2.sp)

        // ── Package cards (compact) ───────────────────────────────────────────
        TelecomPackageMock(
            typeLabel = "INTERNET", typeColor = Blue400,
            packageName = "Monthly", value = "8,358", unit = "MB",
            pct = 0.68f, pctColor = Blue400,
            daysLeft = 18, totalDays = 30,
            barColor = Blue400, bgColor = Blue600.copy(alpha = 0.15f), textColor = Color.White
        )
        TelecomPackageMock(
            typeLabel = "VOICE", typeColor = Color(0xFF34D399),
            packageName = "Regular", value = "244", unit = "MIN",
            pct = 0.92f, pctColor = Color(0xFF34D399),
            daysLeft = 22, totalDays = 30,
            barColor = Color(0xFF34D399), bgColor = Color(0xFF064E3B).copy(alpha = 0.5f), textColor = Color.White
        )
    }
}

@Composable
private fun TelecomAssetItem(label: String, value: String, unit: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(3.dp).height(20.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Column {
            Text(label, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Slate400, letterSpacing = 1.5.sp)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
                Text(unit, fontSize = 8.sp, color = Slate400, modifier = Modifier.padding(bottom = 1.dp))
            }
        }
    }
}

@Composable
private fun TelecomActionBtn(label: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Black, color = color, letterSpacing = 0.5.sp)
        }
    }
}

@Composable
private fun TelecomPackageMock(
    typeLabel: String, typeColor: Color,
    packageName: String, value: String, unit: String,
    pct: Float, pctColor: Color,
    daysLeft: Int, totalDays: Int,
    barColor: Color, bgColor: Color, textColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(typeLabel, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = typeColor, letterSpacing = 2.sp)
                Text(packageName, fontSize = 14.sp, fontWeight = FontWeight.Black, color = textColor)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(value, fontSize = 22.sp, fontWeight = FontWeight.Black, color = textColor)
                    Spacer(Modifier.width(3.dp))
                    Text(unit, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 2.dp))
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text("VALIDITY", fontSize = 7.sp, fontWeight = FontWeight.Black, color = textColor.copy(alpha = 0.5f))
                    Text("$daysLeft / $totalDays days left", fontSize = 8.sp, fontWeight = FontWeight.Black, color = textColor)
                }
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(barColor.copy(alpha = 0.25f))
                ) {
                    Box(modifier = Modifier.fillMaxWidth(pct).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(barColor))
                }
            }
            Spacer(Modifier.width(14.dp))
            // Circular progress
            Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.matchParentSize().drawBehind {
                    drawArc(pctColor.copy(alpha = 0.2f), 0f, 360f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 7.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
                    drawArc(pctColor, -90f, pct * 360f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 7.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
                })
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${(pct * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Black, color = textColor)
                    Text("left", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
private fun TransactionsIllustration() {
    val bgColor = Slate900
    val surfaceColor = Slate800

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ── Summary card ──────────────────────────────────────────────────────
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = surfaceColor,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(24.dp).clip(CircleShape).background(Blue600.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Payments, null, tint = Blue400, modifier = Modifier.size(13.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("FINANCIAL SUMMARY", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Slate400, letterSpacing = 1.5.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, null, tint = Slate400, modifier = Modifier.size(10.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("THIS MONTH", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Slate400, letterSpacing = 1.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("NET BALANCE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Slate400, letterSpacing = 1.5.sp)
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("ETB ", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Slate400, modifier = Modifier.padding(bottom = 2.dp))
                            Text("74,550.00", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Emerald600, letterSpacing = (-0.5).sp)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingUp, null, tint = Emerald600, modifier = Modifier.size(10.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("+87,500.00", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Emerald600)
                        }
                        Spacer(Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingDown, null, tint = Rose500, modifier = Modifier.size(10.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("-12,950.00", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Rose500)
                        }
                    }
                }
            }
        }

        // ── Filter chips ──────────────────────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            TxFilterChipMock("ALL TIME", selected = false, surfaceColor = surfaceColor)
            TxFilterChipMock("THIS MONTH", selected = true, surfaceColor = surfaceColor)
            TxFilterChipMock("THIS WEEK", selected = false, surfaceColor = surfaceColor)
            TxFilterChipMock("▼", selected = false, surfaceColor = surfaceColor)
        }

        // ── Transaction rows ──────────────────────────────────────────────────
        TransactionRowMock("INCOME",  "+87,500.00", "CBE",      "Today",     surfaceColor)
        TransactionRowMock("EXPENSE", "-2,574.00",  "Awash",    "Today",     surfaceColor)
        TransactionRowMock("INCOME",  "+5,000.00",  "BOA",      "Yesterday", surfaceColor)
        TransactionRowMock("EXPENSE", "-450.00",    "Telebirr", "ሚያዝ 20",    surfaceColor)
    }
}

@Composable
private fun TxFilterChipMock(label: String, selected: Boolean, surfaceColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Blue600 else surfaceColor)
            .border(1.dp, if (selected) Blue600 else Slate600, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = if (selected) Color.White else Slate400,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun TransactionRowMock(type: String, amount: String, source: String, date: String, surfaceColor: Color) {
    val isIncome = type == "INCOME"
    val amtColor = if (isIncome) Emerald600 else Rose500
    val iconBg = if (isIncome) Emerald600.copy(alpha = 0.15f) else Rose500.copy(alpha = 0.15f)
    val typeLabel = if (isIncome) "INCOME" else "EXPENSE"

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = surfaceColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = amtColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(source, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("$typeLabel • $date", fontSize = 9.sp, fontWeight = FontWeight.Medium, color = Slate400)
            }
            Text(
                text = "Br $amount",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = amtColor
            )
        }
    }
}

@Composable
private fun SettingsIllustration() {
    val bgColor = Slate900
    val surfaceColor = Slate800

    val sources = listOf(
        Triple("CB", "Commercial Bank of Ethiopia", "CBEBirr"),
        Triple("TB", "Telebirr",                   "TeleBirr"),
        Triple("AW", "Awash Bank",                 "Awash"),
        Triple("DB", "Dashen Bank",                "DashenBank")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ── Section header ────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CreditCard, null, tint = Slate400, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(5.dp))
            Text("TRANSACTION SOURCES", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Slate400, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Add, null, tint = Blue400, modifier = Modifier.size(11.dp))
                Spacer(Modifier.width(3.dp))
                Text("ADD NEW", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Blue400, letterSpacing = 1.5.sp)
            }
        }

        // ── Source rows card ──────────────────────────────────────────────────
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = surfaceColor,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                sources.forEachIndexed { idx, (abbr, name, sender) ->
                    if (idx > 0) HorizontalDivider(color = Slate700, thickness = 0.5.dp)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(Slate700).border(1.dp, Slate600, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(abbr, fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White, maxLines = 1)
                            Text("ACTIVE SYNC · $sender", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Slate400, letterSpacing = 0.5.sp)
                        }
                        Icon(Icons.Default.DeleteOutline, null, tint = Slate600, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

     

        // ── Privacy card ──────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Emerald600.copy(alpha = 0.1f))
                .border(1.dp, Emerald600.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(Emerald600.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Shield, null, tint = Emerald600, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Offline-First Privacy", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Emerald600)
                Text("All SMS data stays on your device.", fontSize = 8.sp, color = Emerald600.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun BankLogoMock(initial: String, color: Color) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = color
        )
    }
}

@Composable
private fun LanguageChipMock(label: String, selected: Boolean) {
    Box(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Emerald600 else MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = Emerald600
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

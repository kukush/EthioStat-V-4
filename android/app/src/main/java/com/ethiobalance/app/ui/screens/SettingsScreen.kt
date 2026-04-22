package com.ethiobalance.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import androidx.compose.ui.platform.LocalContext
import com.ethiobalance.app.data.TransactionSourceEntity
import com.ethiobalance.app.services.UssdAccessibilityService
import com.ethiobalance.app.ui.Translations
import com.ethiobalance.app.ui.theme.*
import com.ethiobalance.app.constants.Avatars
import com.ethiobalance.app.constants.Languages
import com.ethiobalance.app.constants.PhoneConstants
import com.ethiobalance.app.AppConstants


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    language: String,
    theme: String,
    userName: String,
    userPhone: String,
    userAvatar: String,
    transactionSources: List<TransactionSourceEntity>,
    onLanguageChange: (String) -> Unit,
    onThemeChange: (String) -> Unit,
    onProfileUpdate: (String, String, String) -> Unit,
    onAddSource: (TransactionSourceEntity) -> Unit,
    onRemoveSource: (String) -> Unit,
    onClearData: () -> Unit
) {
    var showEditProfile by remember { mutableStateOf(false) }
    var showAddSource by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .statusBarsPadding()
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            Translations.t(language, "settings").takeIf { it.isNotEmpty() } ?: "Settings",
            fontSize = 30.sp, fontWeight = FontWeight.Black, color = Slate900, letterSpacing = (-1).sp
        )

        // Profile Area
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(96.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Box(
                    modifier = Modifier.size(96.dp).clip(CircleShape).background(Slate200)
                        .border(4.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userAvatar.ifEmpty { Avatars.DEFAULT },
                        fontSize = 48.sp
                    )
                }
                IconButton(
                    onClick = { showEditProfile = true },
                    modifier = Modifier.size(28.dp).offset(x = (-4).dp, y = (-4).dp)
                        .background(Blue600, CircleShape)
                        .border(2.dp, Color.White, CircleShape)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(userName.ifEmpty { "User" }, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Slate900)
            Spacer(modifier = Modifier.height(4.dp))
            Text(userPhone.ifEmpty { "No Phone Number" }, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate400)
        }

        // Appearance
        SectionHeader("Appearance", Icons.Default.Palette)
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val themes = listOf(
                Triple("light", "Light", Icons.Default.LightMode) to Color.White,
                Triple("dark", "Dark", Icons.Default.DarkMode) to Slate900,
                Triple("midnight", "Midnight", Icons.Default.Cloud) to Blue950,
                Triple("forest", "Forest", Icons.Default.Bolt) to Emerald950
            )
            themes.chunked(2).forEach { chunk ->
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    chunk.forEach { (meta, _bgColor) ->
                        val (id, label, icon) = meta
                        val isActive = theme == id
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onThemeChange(id) },
                            shape = RoundedCornerShape(24.dp),
                            color = if (isActive) Slate900 else Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isActive) Slate900 else Slate100),
                            shadowElevation = if (isActive) 8.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(12.dp))
                                        .background(if (isActive) Color.White.copy(alpha=0.2f) else Slate100),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(icon, null, tint = if (isActive) Color.White else Slate600, modifier = Modifier.size(16.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActive) Color.White else Slate600,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isActive) {
                                    Icon(Icons.Default.Check, null, tint = Blue400, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // Language Section
        SectionHeader(Translations.t(language, "language").takeIf{it.isNotEmpty()}?:"LANGUAGE", Icons.Default.Language)
        Spacer(Modifier.height(16.dp))
        Surface(
            shape = RoundedCornerShape(40.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate100),
            shadowElevation = 1.dp
        ) {
            Column {
                Languages.SUPPORTED.forEachIndexed { idx, lang ->
                    val code = lang.code
                    val label = lang.displayName
                    val isActive = language == code
                    if (idx > 0) HorizontalDivider(color = Slate50)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isActive) Blue50.copy(alpha=0.5f) else Color.Transparent)
                            .clickable { onLanguageChange(code) }
                            .padding(horizontal = 24.dp, vertical = 20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                color = if (isActive) Blue600 else Slate600,
                                modifier = Modifier.weight(1f))
                            if (isActive) {
                                Icon(Icons.Default.Check, null, tint = Blue600, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // Transaction Sources
        SectionHeader(Translations.t(language, "transactionSources").takeIf{it.isNotEmpty()}?:"TRANSACTION SOURCES", Icons.Default.CreditCard, "Add New", { showAddSource = true })
        Spacer(Modifier.height(16.dp))
        Surface(
            shape = RoundedCornerShape(40.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate100),
            shadowElevation = 1.dp
        ) {
            Column {
                if (transactionSources.isEmpty()) {
                    Text("No sources configured", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate400,
                        modifier = Modifier.padding(32.dp).fillMaxWidth(), textAlign = TextAlign.Center)
                } else {
                    transactionSources.forEachIndexed { idx, source ->
                        if (idx > 0) HorizontalDivider(color = Slate50)
                        SourceRow(source = source, onRemove = { onRemoveSource(source.abbreviation) })
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // Accessibility Options — dynamic badge
        SectionHeader("Accessibility Options", Icons.Default.Accessibility)
        Spacer(Modifier.height(16.dp))
        UssdAccessibilityCard(context = context)

        Spacer(Modifier.height(32.dp))

        // Privacy Section
        SectionHeader("Privacy", Icons.Default.Shield)
        Spacer(Modifier.height(16.dp))
        Surface(
            shape = RoundedCornerShape(40.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate100),
            shadowElevation = 1.dp
        ) {
            Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(Emerald50),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Shield, null, tint = Emerald600, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Offline-First Privacy", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Slate900)
                    Text("All your SMS data is processed locally on your device. No data ever leaves your phone.", fontSize = 12.sp, color = Slate400, modifier = Modifier.padding(top=4.dp), lineHeight = 18.sp)
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // Developer Tools
        SectionHeader("Developer Tools", Icons.Default.Code)
        Spacer(Modifier.height(16.dp))
        Surface(
            shape = RoundedCornerShape(40.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate100),
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Button(
                    onClick = onClearData,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Rose50, contentColor = Rose600)
                ) {
                    Icon(Icons.Default.DeleteOutline, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Clear All App Data", fontWeight = FontWeight.Black, fontSize = 14.sp)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Warning: This will permanently purge your local persistence storage.",
                    fontSize = 10.sp,
                    color = Slate400,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(100.dp))
    }

    // Edit Profile Modal
    if (showEditProfile) {
        EditProfileSheet(
            language = language,
            currentName = userName,
            currentPhone = userPhone,
            currentAvatar = userAvatar,
            onDismiss = { showEditProfile = false },
            onSave = { name, phone, avatar ->
                onProfileUpdate(name, phone, avatar)
                showEditProfile = false
            }
        )
    }

    // Add Source Modal
    if (showAddSource) {
        val configuredAbbreviations = transactionSources.map { it.abbreviation }
        AddSourceSheet(
            _language = language,
            configuredSources = configuredAbbreviations,
            onDismiss = { showAddSource = false },
            onAdd = { source ->
                onAddSource(source)
                showAddSource = false
            }
        )
    }
}

@Composable
private fun SectionHeader(text: String, icon: ImageVector, actionTitle: String? = null, onAction: (() -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(icon, null, tint = Slate400, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold,
            color = Slate400, letterSpacing = 2.sp, modifier = Modifier.weight(1f)
        )
        if (actionTitle != null && onAction != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onAction() }
            ) {
                Icon(Icons.Default.Add, null, tint = Blue600, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(actionTitle.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Blue600, letterSpacing = 2.sp)
            }
        }
    }
}

@Composable
private fun SourceRow(source: TransactionSourceEntity, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(16.dp)).background(Color.White).border(1.dp, Slate100, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            val letter = source.abbreviation.take(2).uppercase()
            Text(letter, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Slate900)
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(source.name, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Slate900, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("ACTIVE SYNC · ${source.senderId}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate400, letterSpacing = 1.sp, modifier = Modifier.padding(top=2.dp))
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.DeleteOutline, null, tint = Slate300, modifier = Modifier.size(18.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileSheet(
    language: String,
    currentName: String,
    currentPhone: String,
    currentAvatar: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var phone by remember { mutableStateOf(currentPhone) }
    var avatar by remember { mutableStateOf(currentAvatar) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 48.dp, topEnd = 48.dp),
        containerColor = Color.White,
        dragHandle = null
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Edit Profile", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Slate900)
                IconButton(onClick = onDismiss, modifier = Modifier.background(Slate50, CircleShape)) {
                    Icon(Icons.Default.Close, null, tint = Slate900, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(32.dp))

            Text("FULL NAME", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Slate400, letterSpacing = 2.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(color = Slate900),
                shape = RoundedCornerShape(24.dp), singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Slate50,
                    focusedContainerColor = Slate50,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Blue500,
                    unfocusedTextColor = Slate900,
                    focusedTextColor = Slate900
                )
            )

            Spacer(Modifier.height(24.dp))
            Text("PHONE NUMBER", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Slate400, letterSpacing = 2.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = phone, 
                onValueChange = {
                    if (it.length <= PhoneConstants.MAX_FULL_LENGTH) phone = it
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(color = Slate900),
                leadingIcon = {
                    Row(
                        modifier = Modifier.padding(start = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(PhoneConstants.FLAG_EMOJI, fontSize = 20.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(PhoneConstants.COUNTRY_CODE, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate600)
                        Spacer(Modifier.width(8.dp))
                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(Slate200))
                    }
                },
                placeholder = { Text("911223344", color = Slate400) },
                shape = RoundedCornerShape(24.dp), singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Slate50,
                    focusedContainerColor = Slate50,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Blue500,
                    unfocusedTextColor = Slate900,
                    focusedTextColor = Slate900
                )
            )

            Spacer(Modifier.height(24.dp))
            Text("CHOOSE AVATAR", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Slate400, letterSpacing = 2.sp)
            Spacer(Modifier.height(16.dp))

            // Avatar grid
            val rows = Avatars.OPTIONS.chunked(6)
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { emoji ->
                        val isSelected = avatar == emoji
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) Blue50 else Slate50)
                                .border(
                                    if (isSelected) 2.dp else 0.dp,
                                    if (isSelected) Blue600 else Color.Transparent,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable { avatar = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 24.sp)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            Spacer(Modifier.height(32.dp))
            Button(
                onClick = {
                    val validatedPhone = if (phone.startsWith(PhoneConstants.LOCAL_PREFIX)) phone.substring(1) else phone
                    if (name.isNotBlank() && validatedPhone.length >= PhoneConstants.MAX_LOCAL_LENGTH) {
                        onSave(name, "${PhoneConstants.COUNTRY_CODE}$validatedPhone", avatar)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                enabled = name.isNotBlank() && (phone.startsWith(PhoneConstants.LOCAL_PREFIX) && phone.length == 10 || !phone.startsWith(PhoneConstants.LOCAL_PREFIX) && phone.length == PhoneConstants.MAX_LOCAL_LENGTH),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Slate900)
            ) {
                Text("SAVE CHANGES", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            }
            Spacer(Modifier.height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp))
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSourceSheet(
    _language: String,
    configuredSources: List<String>,
    onDismiss: () -> Unit,
    onAdd: (TransactionSourceEntity) -> Unit
) {
    var newSource by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    // Filter out already-configured sources so users can't add duplicates
    val availableBanks = AppConstants.KNOWN_BANKS.filter {
        it.abbreviation !in configuredSources
    }

    val filtered = if (searchQuery.isBlank()) availableBanks
    else availableBanks.filter {
        it.abbreviation.contains(searchQuery, ignoreCase = true) ||
        it.fullName.contains(searchQuery, ignoreCase = true)
    }

    // Helper to get all sender variants for a bank (mirrors SettingsRepository.getAllSenderIdsForBank)
    fun getAllSenderIdsForBank(abbreviation: String): String {
        val upper = abbreviation.uppercase()
        val variants = mutableSetOf<String>()

        // Add all entries from SMS_SENDER_WHITELIST that resolve to this abbreviation
        AppConstants.SMS_SENDER_WHITELIST.forEach { senderId ->
            if (AppConstants.resolveSource(senderId) == upper) {
                variants.add(senderId)
            }
        }

        // Add the abbreviation itself
        variants.add(upper)

        // Also check TELECOM_SENDERS for Telebirr
        if (upper == "TELEBIRR") {
            AppConstants.TELECOM_SENDERS.forEach { senderId ->
                if (AppConstants.resolveSource(senderId) == upper) {
                    variants.add(senderId)
                }
            }
        }

        return variants.joinToString(",")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 48.dp, topEnd = 48.dp),
        containerColor = Color.White,
        dragHandle = null
    ) {
        Column(modifier = Modifier.padding(24.dp).heightIn(max = 700.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Add Bank Source", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Slate900)
                    Text("Select a bank or enter a custom ID", fontSize = 14.sp, color = Slate400, modifier = Modifier.padding(top=4.dp))
                }
                IconButton(onClick = onDismiss, modifier = Modifier.background(Slate50, CircleShape)) {
                    Icon(Icons.Default.Close, null, tint = Slate900, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(32.dp))

            Text("CUSTOM SENDER ID", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Slate400, letterSpacing = 2.sp)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newSource, onValueChange = { newSource = it },
                    placeholder = { Text("e.g. CBE, Telebirr") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Slate50, focusedContainerColor = Slate50,
                        unfocusedBorderColor = Color.Transparent, focusedBorderColor = Blue500
                    )
                )
                Spacer(Modifier.width(16.dp))
                Button(
                    onClick = { 
                        onAdd(TransactionSourceEntity(newSource, newSource, "", newSource, true)) 
                        onDismiss()
                    },
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Slate900),
                    enabled = newSource.isNotBlank()
                ) {
                    Text("ADD", fontWeight = FontWeight.Black)
                }
            }
            
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = searchQuery, onValueChange = { searchQuery = it },
                placeholder = { Text("Search Ethiopian Banks...") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Slate400) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp), singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Slate50, focusedContainerColor = Slate50,
                    unfocusedBorderColor = Color.Transparent, focusedBorderColor = Blue500
                )
            )
            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier.weight(1f, fill=false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filtered.forEach { bank ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Populate all sender variants for this bank
                                val allSenderIds = getAllSenderIdsForBank(bank.abbreviation)
                                onAdd(TransactionSourceEntity(
                                    abbreviation = bank.abbreviation,
                                    name = bank.fullName,
                                    ussd = "",
                                    senderId = allSenderIds, // e.g., "889,847,CBE,CBEBirr,CBEBIRR"
                                    isEnabled = true
                                ))
                                onDismiss()
                            },
                        shape = RoundedCornerShape(24.dp),
                        color = Slate50
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AccountBalance, null, tint = Slate400, modifier = Modifier.size(24.dp))
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(bank.fullName, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Slate900)
                                Text("${bank.abbreviation} • ${bank.senderId}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate400, letterSpacing = 2.sp, modifier = Modifier.padding(top=4.dp))
                            }
                            Icon(Icons.Default.Add, null, tint = Slate400)
                        }
                    }
                }
            }
            Spacer(Modifier.height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp))
        }
    }
}

// ── Accessibility Service Status Card ────────────────────────────────────────

/**
 * Shows whether [UssdAccessibilityService] is enabled.
 * - GREEN card  → service active, USSD popups will be captured automatically.
 * - AMBER card  → service not enabled; provides a one-tap deep-link to Settings.
 *
 * Re-evaluates on every recomposition (e.g. when user returns from Settings).
 */
@Composable
private fun UssdAccessibilityCard(context: Context) {
    val isEnabled = remember {
        derivedStateOf { isAccessibilityServiceEnabled(context) }
    }

    val cardColor   = if (isEnabled.value) Emerald50  else Amber50
    val borderColor = if (isEnabled.value) Emerald200 else Amber200
    val iconBg      = if (isEnabled.value) Emerald100 else Amber100
    val iconTint    = if (isEnabled.value) Emerald600 else Amber500
    val icon        = if (isEnabled.value) Icons.Default.CheckCircle else Icons.Default.WarningAmber

    Surface(
        shape = RoundedCornerShape(40.dp),
        color = cardColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = iconTint, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Read USSD Popups",
                        fontSize = 14.sp, fontWeight = FontWeight.Black, color = Slate900
                    )
                    Text(
                        if (isEnabled.value)
                            "Active — *804# balance captured automatically"
                        else
                            "Required to capture balance from *804#",
                        fontSize = 12.sp,
                        color = if (isEnabled.value) Emerald600 else Amber500,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            if (!isEnabled.value) {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { context.startActivity(UssdAccessibilityService.buildSettingsIntent()) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Slate900)
                ) {
                    Icon(Icons.Default.Settings, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Open Accessibility Settings", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Steps: Settings → Accessibility → Installed Services → EthioStat → Turn On",
                    fontSize = 10.sp,
                    color = Amber500,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/** Returns true if our UssdAccessibilityService is enabled by the user. */
private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val expectedId = "${context.packageName}/${UssdAccessibilityService::class.java.name}"
    return try {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabled)
        colonSplitter.any { it.equals(expectedId, ignoreCase = true) }
    } catch (e: Exception) {
        false
    }
}


package com.ethiobalance.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
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
import androidx.compose.ui.platform.LocalContext
import com.ethiobalance.app.data.TransactionSourceEntity
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
    smsPermissionGranted: Boolean = true,
    onThemeChange: (String) -> Unit,
    onProfileUpdate: (String, String, String) -> Unit,
    onAddSource: (TransactionSourceEntity) -> Unit,
    onRemoveSource: (String) -> Unit,
    onRequestPermissions: () -> Unit = {}
) {
    var showEditProfile by remember { mutableStateOf(false) }
    var showAddSource by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Profile Area
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
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
                        .background(MaterialTheme.colorScheme.outline, CircleShape)
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
                            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                            shadowElevation = if (isActive) 8.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(12.dp))
                                        .background(if (isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(icon, null, tint = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isActive) {
                                    Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Permission Warning Card
        if (!smsPermissionGranted) {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                Translations.t(language, "permissionRequired").takeIf { it.isNotEmpty() } ?: "Permission Required",
                                fontSize = 16.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                Translations.t(language, "permissionMainMessage").takeIf { it.isNotEmpty() }
                                    ?: "To track balance, data, and expenses. Recharge easily.",
                                fontSize = 12.sp, color = Slate600, modifier = Modifier.padding(top = 4.dp), lineHeight = 18.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    // Explanation bullets
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PermissionBullet(
                            icon = Icons.Default.CreditCard,
                            text = Translations.t(language, "permissionBankInfo").takeIf { it.isNotEmpty() }
                                ?: "Only chosen banks or wallets from Settings are used to track your transactions"
                        )
                        PermissionBullet(
                            icon = Icons.Default.CardGiftcard,
                            text = Translations.t(language, "permissionTelecomInfo").takeIf { it.isNotEmpty() }
                                ?: "For telecom packages: reads messages from 994"
                        )
                        PermissionBullet(
                            icon = Icons.Default.Phone,
                            text = Translations.t(language, "permissionUssdInfo").takeIf { it.isNotEmpty() }
                                ?: "For balance checks (*804#, *805#) via USSD dial"
                        )
                        PermissionBullet(
                            icon = Icons.Default.Info,
                            text = Translations.t(language, "permissionNoSend").takeIf { it.isNotEmpty() }
                                ?: "EthioStat never sends SMS. Android shows \"send\" in the dialog, but only read access is used."
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = onRequestPermissions,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate900)
                    ) {
                        Icon(Icons.Default.Lock, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            (Translations.t(language, "grantPermission").takeIf { it.isNotEmpty() } ?: "Grant Permission").uppercase(),
                            fontWeight = FontWeight.Black, letterSpacing = 1.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // Transaction Sources
        SectionHeader(
            Translations.t(language, "transactionSources").takeIf{it.isNotEmpty()}?:"TRANSACTION SOURCES",
            Icons.Default.CreditCard,
            if (smsPermissionGranted) "Add New" else null,
            if (smsPermissionGranted) ({ showAddSource = true }) else null
        )
        Spacer(Modifier.height(16.dp))
        Surface(
            shape = RoundedCornerShape(40.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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

        Spacer(Modifier.height(20.dp))

        // Privacy Section
        SectionHeader("Privacy", Icons.Default.Shield)
        Spacer(Modifier.height(16.dp))
        Surface(
            shape = RoundedCornerShape(40.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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

        Spacer(Modifier.height(80.dp))
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

    // Add Source Modal (only when permission granted)
    if (showAddSource && smsPermissionGranted) {
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
    var phone by remember { mutableStateOf(currentPhone.removePrefix(PhoneConstants.COUNTRY_CODE)) }
    var avatar by remember { mutableStateOf(currentAvatar) }
    var phoneError by remember { mutableStateOf("") }

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
                    if (it.length <= PhoneConstants.MAX_LOCAL_LENGTH) {
                        phone = it
                        // Validate phone number
                        phoneError = when {
                            it.startsWith("0") -> Translations.t(language, "phoneErrorPrefix")
                            it.isNotEmpty() && !it.first().let { c -> c == '7' || c == '9' } -> Translations.t(language, "phoneValidationError")
                            it.length == PhoneConstants.MAX_LOCAL_LENGTH && !PhoneConstants.isValidEthiopianPhone(it) -> Translations.t(language, "phoneValidationError")
                            else -> ""
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(color = Slate900),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
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
                ),
                isError = phoneError.isNotEmpty()
            )

            if (phoneError.isNotEmpty()) {
                Text(
                    phoneError,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

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
                    if (name.isNotBlank() && PhoneConstants.isValidEthiopianPhone(phone)) {
                        onSave(name, "${PhoneConstants.COUNTRY_CODE}$phone", avatar.ifEmpty { Avatars.DEFAULT })
                    }
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                enabled = name.isNotBlank() && PhoneConstants.isValidEthiopianPhone(phone) && phoneError.isEmpty(),
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
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(modifier = Modifier.padding(24.dp).heightIn(max = 700.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Add Bank Source", fontSize = 24.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                    Text("Select a bank from the list below", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top=4.dp))
                }
                IconButton(onClick = onDismiss, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)) {
                    Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = searchQuery, onValueChange = { searchQuery = it },
                placeholder = { Text("Search Ethiopian Banks...") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp), singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedBorderColor = Color.Transparent, focusedBorderColor = MaterialTheme.colorScheme.primary
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
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AccountBalance, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(bank.fullName, fontSize = 14.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                                Text("${bank.abbreviation} • ${bank.senderId}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 2.sp, modifier = Modifier.padding(top=4.dp))
                            }
                            Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Spacer(Modifier.height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp))
        }
    }
}

@Composable
private fun PermissionBullet(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.outline),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp, modifier = Modifier.weight(1f))
    }
}

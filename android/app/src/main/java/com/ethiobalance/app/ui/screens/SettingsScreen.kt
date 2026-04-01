package com.ethiobalance.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethiobalance.app.data.SimCardEntity
import com.ethiobalance.app.data.TransactionSourceEntity
import com.ethiobalance.app.ui.Translations
import com.ethiobalance.app.ui.theme.*

private val avatarList = listOf(
    "👤", "🧑", "👩", "👨", "🧔", "👵", "🧑‍💼", "👩‍💻", "👨‍🎓",
    "🦁", "🐯", "🦊", "🐻", "🐼", "🐨", "🐸", "🦉", "🐝"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    language: String,
    theme: String,
    userName: String,
    userPhone: String,
    userAvatar: String,
    simCards: List<SimCardEntity>,
    transactionSources: List<TransactionSourceEntity>,
    onLanguageChange: (String) -> Unit,
    onThemeChange: (String) -> Unit,
    onProfileUpdate: (String, String, String) -> Unit,
    onDetectSims: () -> Unit,
    onDeleteSim: (String) -> Unit,
    onSetPrimarySim: (String) -> Unit,
    onAddSource: (TransactionSourceEntity) -> Unit,
    onRemoveSource: (String) -> Unit,
    onClearData: () -> Unit
) {
    var showEditProfile by remember { mutableStateOf(false) }
    var showAddSource by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .statusBarsPadding()
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            Translations.t(language, "settings"),
            fontSize = 30.sp, fontWeight = FontWeight.Black, color = Slate900
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Profile Section
        Surface(
            shape = RoundedCornerShape(40.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate100)
        ) {
            Row(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Blue50),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (userAvatar.isNotEmpty()) userAvatar else "👤",
                        fontSize = 32.sp
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(userName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Slate900)
                    if (userPhone.isNotEmpty()) {
                        Text(userPhone, fontSize = 13.sp, color = Slate400)
                    }
                }
                IconButton(
                    onClick = { showEditProfile = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Blue600)
                ) {
                    Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // SIM Cards Section
        SectionHeader(Translations.t(language, "simCards"))
        Spacer(Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate100)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (simCards.isEmpty()) {
                    Text("No SIM cards detected", fontSize = 13.sp, color = Slate400,
                        modifier = Modifier.padding(8.dp))
                } else {
                    simCards.forEachIndexed { idx, sim ->
                        if (idx > 0) HorizontalDivider(color = Slate50, modifier = Modifier.padding(vertical = 8.dp))
                        SimCardRow(
                            sim = sim,
                            onSetPrimary = { onSetPrimarySim(sim.id) },
                            onDelete = { onDeleteSim(sim.id) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onDetectSims,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate100)
                ) {
                    Icon(Icons.Default.SimCard, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Detect SIM Cards", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Theme Section
        SectionHeader(Translations.t(language, "theme"))
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "light" to Pair(Color.White, Slate900),
                "dark" to Pair(Slate900, Color.White),
                "midnight" to Pair(Blue950, Blue400),
                "forest" to Pair(Emerald950, Emerald500)
            ).forEach { (id, colors) ->
                val isActive = theme == id
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onThemeChange(id) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isActive) Slate900 else colors.first,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, if (isActive) Slate900 else Slate100
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isActive) {
                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        Text(
                            Translations.t(language, id),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) Color.White else colors.second,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Language Section
        SectionHeader(Translations.t(language, "language"))
        Spacer(Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate100)
        ) {
            Column(modifier = Modifier.padding(4.dp)) {
                listOf(
                    "en" to "English",
                    "am" to "አማርኛ",
                    "om" to "Afaan Oromoo"
                ).forEach { (code, label) ->
                    val isActive = language == code
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLanguageChange(code) },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isActive) Blue50 else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                color = if (isActive) Blue600 else Slate900,
                                modifier = Modifier.weight(1f))
                            if (isActive) {
                                Icon(Icons.Default.Check, null, tint = Blue600, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Transaction Sources
        SectionHeader(Translations.t(language, "transactionSources"))
        Spacer(Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate100)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (transactionSources.isEmpty()) {
                    Text("No sources configured", fontSize = 13.sp, color = Slate400,
                        modifier = Modifier.padding(8.dp))
                } else {
                    transactionSources.forEachIndexed { idx, source ->
                        if (idx > 0) HorizontalDivider(color = Slate50, modifier = Modifier.padding(vertical = 8.dp))
                        SourceRow(source = source, onRemove = { onRemoveSource(source.abbreviation) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showAddSource = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate100)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(Translations.t(language, "addSource"), fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Privacy Section
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate100)
        ) {
            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Emerald50),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Shield, null, tint = Emerald600, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Offline-First Privacy", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate900)
                    Text("All data stays on your device. No external servers.", fontSize = 12.sp, color = Slate400)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Developer Tools
        SectionHeader("Developer Tools")
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onClearData,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Rose600.copy(alpha = 0.3f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Rose600)
        ) {
            Icon(Icons.Default.DeleteForever, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Clear All Data", fontWeight = FontWeight.SemiBold)
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
        AddSourceSheet(
            language = language,
            onDismiss = { showAddSource = false },
            onAdd = { source ->
                onAddSource(source)
                showAddSource = false
            }
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold,
        color = Slate400, letterSpacing = 2.sp
    )
}

@Composable
private fun SimCardRow(sim: SimCardEntity, onSetPrimary: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                .background(if (sim.isPrimary) Blue50 else Slate50),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.SimCard, null, tint = if (sim.isPrimary) Blue600 else Slate400,
                modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(sim.carrierName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Slate900)
                if (sim.isPrimary) {
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Blue50
                    ) {
                        Text("Primary", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Blue600,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
            Text(sim.phoneNumber, fontSize = 12.sp, color = Slate400)
        }
        if (!sim.isPrimary) {
            IconButton(onClick = onSetPrimary, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Star, null, tint = Slate300, modifier = Modifier.size(16.dp))
            }
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, null, tint = Slate300, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun SourceRow(source: TransactionSourceEntity, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Blue50),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.AccountBalance, null, tint = Blue600, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(source.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Slate900)
            Text("Active Sync · ${source.senderId}", fontSize = 11.sp, color = Slate400)
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, null, tint = Slate300, modifier = Modifier.size(16.dp))
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
        containerColor = Color.White
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Edit Profile", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Slate900)
            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp), singleLine = true
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = phone, onValueChange = { phone = it },
                label = { Text("Phone") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp), singleLine = true
            )
            Spacer(Modifier.height(16.dp))

            Text("Choose Avatar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate400)
            Spacer(Modifier.height(8.dp))

            // Avatar grid
            val rows = avatarList.chunked(6)
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { emoji ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (avatar == emoji) Blue50 else Slate50)
                                .border(
                                    if (avatar == emoji) 2.dp else 0.dp,
                                    if (avatar == emoji) Blue600 else Color.Transparent,
                                    CircleShape
                                )
                                .clickable { avatar = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 22.sp)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onSave(name, phone, avatar) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Slate900)
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

private val bankList = listOf(
    Triple("CBE", "Commercial Bank of Ethiopia", "847"),
    Triple("TELEBIRR", "Telebirr", "127"),
    Triple("AWASH", "Awash Bank", "901"),
    Triple("DASHEN", "Dashen Bank", "721"),
    Triple("BOA", "Bank of Abyssinia", "815"),
    Triple("COOPBANK", "Cooperative Bank of Oromia", "896"),
    Triple("HIBRET", "Hibret Bank", "844"),
    Triple("WEGAGEN", "Wegagen Bank", "889"),
    Triple("ABAY", "Abay Bank", "812"),
    Triple("NIB", "Nib International Bank", "865"),
    Triple("BUNNA", "Bunna Bank", "252"),
    Triple("ZEMEN", "Zemen Bank", "710"),
    Triple("BERHAN", "Berhan Bank", "811"),
    Triple("ENAT", "Enat Bank", "846"),
    Triple("TSEHAY", "Tsehay Bank", "921"),
    Triple("SIINQEE", "Siinqee Bank", "767"),
    Triple("AMHARA", "Amhara Bank", "946"),
    Triple("LION", "Lion International Bank", "801"),
    Triple("OROMIA", "Oromia Bank", ""),
    Triple("GLOBAL", "Global Bank Ethiopia", "842"),
    Triple("GADAA", "Gadaa Bank", "898"),
    Triple("HIJRA", "Hijra Bank", "881"),
    Triple("ZAD", "Zad Bank", "899"),
    Triple("AHADU", "Ahadu Bank", "895"),
    Triple("SHABELLE", "Shabelle Bank", "808"),
    Triple("ACSI", "Amhara Credit and Saving", "810"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSourceSheet(
    language: String,
    onDismiss: () -> Unit,
    onAdd: (TransactionSourceEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filtered = if (searchQuery.isBlank()) bankList
    else bankList.filter {
        it.first.contains(searchQuery, ignoreCase = true) ||
        it.second.contains(searchQuery, ignoreCase = true)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 48.dp, topEnd = 48.dp),
        containerColor = Color.White
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(Translations.t(language, "addSource"), fontSize = 22.sp,
                fontWeight = FontWeight.Black, color = Slate900)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = searchQuery, onValueChange = { searchQuery = it },
                placeholder = { Text("Search banks...") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Slate400) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp), singleLine = true
            )
            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                filtered.forEach { (abbr, name, senderId) ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onAdd(TransactionSourceEntity(
                                    abbreviation = abbr,
                                    name = name,
                                    ussd = "",
                                    senderId = senderId.ifEmpty { abbr },
                                    isEnabled = true
                                ))
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Blue50),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AccountBalance, null, tint = Blue600, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Slate900)
                                Text("Sender: ${senderId.ifEmpty { abbr }}", fontSize = 11.sp, color = Slate400)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

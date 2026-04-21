package com.ethiobalance.app

/**
 * AppConstants — single source of truth for all string constants, sender numbers,
 * broadcast actions, and notification identifiers used across the Android native layer.
 *
 * NEVER hardcode these values elsewhere. Reference them via AppConstants.
 *
 * Sender IDs are aligned with banks.ts in the web layer. Each bank's SMS sender
 * is either its alpha-name (e.g., "CBEBirr") or the numeric short code derived
 * from its USSD code (e.g., CBE *847# → sender "847"). Both forms are included
 * to catch all variants across carriers.
 */
object AppConstants {

    // -------------------------------------------------------------------------
    // SMS Sender Whitelist — Default Transaction Sources
    //
    // Covers: EthioTelecom, Telebirr, CBE, Awash, BoA, Dashen, Coopbank,
    //         Hibret, Wegagen, Abay, NIB, Bunna, Zemen, Berhan, Enat,
    //         Tsehay, Siinqee, Amhara Bank, Lion, Oromia, Global, Gadaa,
    //         Hijra, Zad, Ahadu, Shabelle, ACSI
    //
    // EthioTelecom Code Reference:
    //   830   → CRBT (Caller Ring Back Tone) subscription / management
    //   806   → Airtime / Credit Transfer between subscribers
    //   994   → Customer Service Hotline
    //   8994  → SMS-based Inquiry and Support
    //   *999# → Main Menu (Voice/Data/SMS packages) — USSD dial code only,
    //           NOT an SMS sender; handled by AccessibilityService
    //
    // NOTE: "USSD" is intentionally excluded — USSD responses arrive via
    // AccessibilityService text harvest, not as an SMS sender number.
    // -------------------------------------------------------------------------
    // Core whitelist: numeric short-codes + known multi-word / compound alpha senders.
    // SOURCE_DISPLAY_NAMES values (e.g. "Awash", "Dashen", "Coopbank") are merged in below
    // so that whatever short name a bank uses in the SMS inbox is automatically covered.
    private val SMS_SENDER_WHITELIST_BASE: Set<String> = setOf(

        // ── EthioTelecom ──────────────────────────────────────────────────────
        "994",           // Customer Service Hotline
        "251994",        // Full international-format sender
        "804",           // *804# balance / data query responses
        "810",           // *810# (shared with ACSI)
        "830",           // CRBT — Caller Ring Back Tone subscription / management
        "806",           // Airtime / Credit Transfer between subscribers
        "805",           // Airtime Recharge

        // NOTE: *999# (Voice/Data/SMS package menu) is a USSD *dial* code,
        // not an SMS sender — it is handled by UssdAccessibilityService.

        // ── Telebirr (EthioTelecom Mobile Money) — *127# ─────────────────────
        "127",
        "TELEBIRR", "Telebirr",

        // ── Commercial Bank of Ethiopia (CBE) — *889#, *847# ─────────────────
        "889", "847",
        "CBE", "CBEBirr", "CBEBIRR",

        // ── Awash Bank — *901# ────────────────────────────────────────────────
        "901",
        "Awash", "AwashBank", "Awash Bank",

        // ── Bank of Abyssinia (BoA) — *815#, *999# ───────────────────────────
        "815", "999",
        "BOA", "Abyssinia", "AbyssiniaBank",

        // ── Dashen Bank — *996#, *675# ────────────────────────────────────────
        "996", "675",
        "Dashen", "DashenBank", "Dashen Bank",

        // ── Cooperative Bank of Oromia (Coopbank) — *841#, *896# ─────────────
        "841", "896",
        "Coopbank", "Cooperative Bank of Oromia",

        // ── Oromia Bank — *840# ───────────────────────────────────────────────
        "840",
        "Oromia", "OromiaBank", "Oromia Bank",

        // ── Hibret Bank — *811# ───────────────────────────────────────────────
        "811",
        "Hibret", "HibretBank", "Hibret Bank",

        // ── Wegagen Bank — *866# ──────────────────────────────────────────────
        "866",
        "Wegagen", "WegagenBank", "Wegagen Bank",

        // ── Global Bank Ethiopia — *8027#, *9335# ────────────────────────────
        "8027", "9335",
        "GBE", "Global", "GlobalBank", "Global Bank Ethiopia",

        // ── Amhara Bank — *690# ───────────────────────────────────────────────
        "690",
        "Amhara", "AmharaBank", "Amhara Bank",

        // ── Bunna Bank — *820# ────────────────────────────────────────────────
        "820",
        "Bunna", "BunnaBank", "Bunna Bank",

        // ── Zemen Bank — *844# ────────────────────────────────────────────────
        "844",
        "Zemen", "ZemenBank", "Zemen Bank",

        // ── NIB International Bank — *865# ───────────────────────────────────
        "865",
        "NIB", "NIBBank", "NibBank", "Nib Bank",

        // ── Abay Bank — *812# ─────────────────────────────────────────────────
        "812",
        "Abay", "AbayBank", "Abay Bank",

        // ── Berhan Bank — *881# ───────────────────────────────────────────────
        "881",
        "Berhan", "BerhanBank", "Berhan Bank",

        // ── Enat Bank — *845# ─────────────────────────────────────────────────
        "845",
        "Enat", "EnatBank", "Enat Bank",

        // ── Siinqee Bank — *871# ──────────────────────────────────────────────
        "871",
        "Siinqee", "SiinqeeBank", "Siinqee Bank",

        // ── Tsedey Bank — *616# ───────────────────────────────────────────────
        "616",
        "Tsedey", "TsedeyBank", "Tsedey Bank",

        // ── Ahadu Bank — *611# ────────────────────────────────────────────────
        "611",
        "Ahadu", "AhaduBank", "Ahadu Bank",

        // ── Gadaa Bank — *877# ────────────────────────────────────────────────
        "877",
        "Gadaa", "GadaaBank", "Gadaa Bank",

        // ── Hijra Bank — *827# ────────────────────────────────────────────────
        "827",
        "Hijra", "HijraBank", "Hijra Bank",

        // ── ZamZam Bank — *600# ───────────────────────────────────────────────
        "600",
        "ZamZam", "ZamZamBank", "ZamZam Bank",

        // ── Shabelle Bank ─────────────────────────────────────────────────────
        "Shabelle", "ShabelleBank", "Shabelle Bank",

        // ── Tsehay Bank — *921# ───────────────────────────────────────────────
        "921",
        "Tsehay", "TsehayBank", "Tsehay Bank",

        // ── Zad Bank — *899# ──────────────────────────────────────────────────
        "899",
        "Zad", "ZadBank", "Zad Bank",

        // ── Lion International Bank — *801# ───────────────────────────────────
        "801",
        "Lion", "LionBank", "Lion International Bank",

        // ── Amhara Credit and Saving (ACSI) — *690# ──────────────────────────
        "ACSI", "ACSIBank"
    )

    // Full whitelist = base entries ∪ SOURCE_DISPLAY_NAMES values.
    // SOURCE_DISPLAY_NAMES holds the exact short names banks use as SMS sender addresses
    // (e.g. "Awash", "Dashen", "Coopbank"), so any new entry there is automatically scanned.
    val SMS_SENDER_WHITELIST: Set<String> by lazy {
        SMS_SENDER_WHITELIST_BASE + SOURCE_DISPLAY_NAMES.values
    }

    // Source labels used in TransactionEntity.source — always UPPERCASE for consistency
    const val SOURCE_TELEBIRR = "TELEBIRR"
    const val SOURCE_AIRTIME  = "AIRTIME" // Used for Telecom Assets, excluded from Financials

    /**
     * Resolves a human-readable source label from a raw SMS sender number.
     * Telebirr senders: 127, 830, 806 or any sender whose number contains "TELEBIRR".
     */
    // Telebirr is identified by sender "127" or an alpha-sender containing "TELEBIRR".
    // 830 (CRBT) and 806 (Airtime Transfer) are EthioTelecom service codes, not Telebirr.
    val TELEBIRR_SENDERS: Set<String> = setOf("127")

    /**
     * Resolves a raw SMS sender (numeric short-code or alpha-name, any case) to a canonical
     * UPPERCASE bank/service abbreviation stored in TransactionEntity.source.
     *
     * Always returns UPPERCASE so that "AWASH", "Awash", "awash" all produce "AWASH".
     * All callers must compare using .uppercase() or .equals(ignoreCase=true).
     */
    fun resolveSource(sender: String): String {
        val upper = sender.trim().uppercase()
        
        // Telebirr
        if (upper.contains("TELEBIRR") || TELEBIRR_SENDERS.contains(upper) || TELEBIRR_SENDERS.contains(sender.trim())) return SOURCE_TELEBIRR
        
        // Commercial Bank of Ethiopia (CBE) — 889, 847
        if (upper in setOf("889", "847") || upper.contains("CBE") || upper.contains("CBEBIRR")) return "CBE"
        
        // Awash Bank — 901
        if (upper == "901" || upper.contains("AWASH")) return "AWASH"
        
        // Bank of Abyssinia (BoA) — 815, 999
        if (upper in setOf("815", "999") || upper.contains("BOA") || upper.contains("ABYSSINIA")) return "BOA"
        
        // Dashen Bank — 996, 675
        if (upper in setOf("996", "675") || upper.contains("DASHEN")) return "DASHEN"
        
        // Cooperative Bank of Oromia (Coopbank) — 841, 896
        if (upper in setOf("841", "896") || upper.contains("COOP")) return "COOPBANK"
        
        // Oromia Bank — 840
        if (upper == "840" || upper.contains("OROMIA")) return "OROMIA"
        
        // Hibret Bank — 811
        if (upper == "811" || upper.contains("HIBRET")) return "HIBRET"
        
        // Wegagen Bank — 866
        if (upper == "866" || upper.contains("WEGAGEN")) return "WEGAGEN"
        
        // Global Bank Ethiopia — 8027, 9335
        if (upper in setOf("8027", "9335") || upper.contains("GLOBAL") || upper.contains("GBE")) return "GLOBAL"
        
        // Amhara Bank — 690
        if (upper == "690" || upper.contains("AMHARA")) return "AMHARA"
        
        // Bunna Bank — 820
        if (upper == "820" || upper.contains("BUNNA")) return "BUNNA"
        
        // Zemen Bank — 844
        if (upper == "844" || upper.contains("ZEMEN")) return "ZEMEN"
        
        // NIB International Bank — 865
        if (upper == "865" || upper.contains("NIB")) return "NIB"
        
        // Abay Bank — 812
        if (upper == "812" || upper.contains("ABAY")) return "ABAY"
        
        // Berhan Bank — 881
        if (upper == "881" || upper.contains("BERHAN")) return "BERHAN"
        
        // Enat Bank — 845
        if (upper == "845" || upper.contains("ENAT")) return "ENAT"
        
        // Siinqee Bank — 871
        if (upper == "871" || upper.contains("SIINQEE")) return "SIINQEE"
        
        // Tsedey Bank — 616
        if (upper == "616" || upper.contains("TSEDEY")) return "TSEDEY"
        
        // Ahadu Bank — 611
        if (upper == "611" || upper.contains("AHADU")) return "AHADU"
        
        // Gadaa Bank — 877
        if (upper == "877" || upper.contains("GADAA")) return "GADAA"
        
        // Hijra Bank — 827
        if (upper == "827" || upper.contains("HIJRA")) return "HIJRA"
        
        // ZamZam Bank — 600
        if (upper == "600" || upper.contains("ZAMZAM")) return "ZAMZAM"
        
        // Shabelle Bank (alpha disambiguation — numeric 866 hits WEGAGEN first)
        if (upper.contains("SHABELLE")) return "SHABELLE"
        
        // Tsehay Bank — 921
        if (upper == "921" || upper.contains("TSEHAY")) return "TSEHAY"
        
        // Zad Bank — 899
        if (upper == "899" || upper.contains("ZAD")) return "ZAD"
        
        // Lion International Bank — 801
        if (upper == "801" || upper.contains("LION")) return "LION"
        
        // Amhara Credit and Saving (ACSI)
        if (upper.contains("ACSI")) return "ACSI"
        
        // EthioTelecom (Airtime / Telecom Assets) — must be after banks to avoid false matches
        val ethioTelecomSenders = setOf("804", "805", "806", "810", "830", "994", "251994", "8994")
        if (ethioTelecomSenders.contains(upper) || upper.contains("ETHIOTELECOM") || upper.contains("ETHIO TELECOM")) return SOURCE_AIRTIME

        // Fallback: return the uppercased sender so comparisons remain case-insensitive
        return upper
    }

    /**
     * Converts a canonical source key (e.g. "AWASH", "CBE", "TELEBIRR") to a
     * human-readable display label (e.g. "Awash", "CBE", "Telebirr").
     *
     * Rules:
     *  - Well-known abbreviations that should stay uppercase: CBE, BOA, NIB, GBE, ACSI
     *  - Multi-word brands with fixed casing: Telebirr, Coopbank
     *  - Everything else: first letter uppercase, rest lowercase (e.g. "AWASH" → "Awash")
     */
    private val SOURCE_DISPLAY_NAMES: Map<String, String> = mapOf(
        "CBE"      to "CBE",
        "TELEBIRR" to "Telebirr",
        "AWASH"    to "Awash",
        "DASHEN"   to "Dashen",
        "BOA"      to "BOA",
        "COOPBANK" to "Coopbank",
        "HIBRET"   to "Hibret",
        "WEGAGEN"  to "Wegagen",
        "ABAY"     to "Abay",
        "NIB"      to "NIB",
        "BUNNA"    to "Bunna",
        "ZEMEN"    to "Zemen",
        "BERHAN"   to "Berhan",
        "ENAT"     to "Enat",
        "TSEHAY"   to "Tsehay",
        "SIINQEE"  to "Siinqee",
        "AMHARA"   to "Amhara",
        "LION"     to "Lion",
        "OROMIA"   to "Oromia",
        "GLOBAL"   to "Global",
        "GADAA"    to "Gadaa",
        "HIJRA"    to "Hijra",
        "ZAD"      to "Zad",
        "AHADU"    to "Ahadu",
        "SHABELLE" to "Shabelle",
        "TSEDEY"   to "Tsedey",
        "ZAMZAM"   to "ZamZam",
        "ACSI"     to "ACSI",
        "AIRTIME"  to "Airtime"
    )

    fun displaySource(source: String): String {
        val key = source.trim().uppercase()
        return SOURCE_DISPLAY_NAMES[key]
            ?: key.lowercase().replaceFirstChar { it.uppercase() }
    }

    // -------------------------------------------------------------------------
    // USSD Codes — sourced from gradle.properties via BuildConfig
    // -------------------------------------------------------------------------
    val USSD_BALANCE_CHECK: String get() = BuildConfig.USSD_BALANCE_CHECK
    val USSD_RECHARGE_SELF: String get() = BuildConfig.USSD_RECHARGE_SELF
    val USSD_RECHARGE_OTHER: String get() = BuildConfig.USSD_RECHARGE_OTHER
    val USSD_TRANSFER_AIRTIME: String get() = BuildConfig.USSD_TRANSFER_AIRTIME
    val USSD_GIFT_PACKAGE: String get() = BuildConfig.USSD_GIFT_PACKAGE
    
    // -------------------------------------------------------------------------
    // Broadcast Actions
    // -------------------------------------------------------------------------
    const val ACTION_USSD_RESPONSE = "com.ethiobalance.app.ACTION_USSD_RESPONSE"
    const val ACTION_TRIGGER_REFRESH = "com.ethiobalance.app.ACTION_TRIGGER_REFRESH"

    // -------------------------------------------------------------------------
    // Notification Channel
    // -------------------------------------------------------------------------
    const val NOTIFICATION_CHANNEL_ID   = "SmsMonitorChannel"
    const val NOTIFICATION_CHANNEL_NAME = "SMS Monitor Service"
    const val NOTIFICATION_ID_SMS       = 1

    // -------------------------------------------------------------------------
    // SIM Slot Defaults
    // The default SIM slot used until the user configures a primary SIM.
    // Slot 1 = first physical SIM in a dual-SIM device.
    // -------------------------------------------------------------------------
    const val DEFAULT_SIM_SLOT = 1

    // -------------------------------------------------------------------------
    // USSD Harvesting
    // Package name of the Android phone/dialer app whose windows carry USSD popups.
    // -------------------------------------------------------------------------
    val PHONE_APP_PACKAGE: String get() = BuildConfig.PHONE_APP_PACKAGE

    // -------------------------------------------------------------------------
    // USSD Response DB label
    // -------------------------------------------------------------------------
    const val USSD_REQUEST_LABEL = "Last USSD Request"

    // -------------------------------------------------------------------------
    // SharedPreferences
    // -------------------------------------------------------------------------
    const val PREFS_NAME              = "ethio_balance_prefs"
    const val PREF_KEY_SMS_WHITELIST  = "sms_whitelist"

    // -------------------------------------------------------------------------
    // SmsLogEntity parsed type labels
    // -------------------------------------------------------------------------
    const val SMS_LOG_TYPE_PROCESSING = "PROCESSING"

    // -------------------------------------------------------------------------
    // Known Banks — single source of truth for bank metadata
    // Format: Triple(abbreviation, fullName, senderId)
    // -------------------------------------------------------------------------
    data class BankInfo(val abbreviation: String, val fullName: String, val senderId: String)

    val KNOWN_BANKS: List<BankInfo> = listOf(
        BankInfo("CBE",      "Commercial Bank of Ethiopia",    "847"),
        BankInfo("TELEBIRR", "Telebirr",                       "127"),
        BankInfo("AWASH",    "Awash Bank",                     "901"),
        BankInfo("DASHEN",   "Dashen Bank",                    "996"),
        BankInfo("BOA",      "Bank of Abyssinia",              "815"),
        BankInfo("COOPBANK", "Cooperative Bank of Oromia",     "841"),
        BankInfo("HIBRET",   "Hibret Bank",                    "811"),
        BankInfo("WEGAGEN",  "Wegagen Bank",                   "866"),
        BankInfo("ABAY",     "Abay Bank",                      "812"),
        BankInfo("NIB",      "Nib International Bank",         "865"),
        BankInfo("BUNNA",    "Bunna Bank",                     "820"),
        BankInfo("ZEMEN",    "Zemen Bank",                     "844"),
        BankInfo("BERHAN",   "Berhan Bank",                    "881"),
        BankInfo("ENAT",     "Enat Bank",                      "845"),
        BankInfo("TSEHAY",   "Tsehay Bank",                    "921"),
        BankInfo("SIINQEE",  "Siinqee Bank",                   "871"),
        BankInfo("AMHARA",   "Amhara Bank",                    "690"),
        BankInfo("LION",     "Lion International Bank",        "801"),
        BankInfo("OROMIA",   "Oromia Bank",                    "840"),
        BankInfo("GLOBAL",   "Global Bank Ethiopia",           "8027"),
        BankInfo("GADAA",    "Gadaa Bank",                     "877"),
        BankInfo("HIJRA",    "Hijra Bank",                     "827"),
        BankInfo("ZAD",      "Zad Bank",                       "899"),
        BankInfo("AHADU",    "Ahadu Bank",                     "611"),
        BankInfo("SHABELLE", "Shabelle Bank",                  "ShabelleBank"),
        BankInfo("TSEDEY",   "Tsedey Bank",                    "616"),
        BankInfo("ZAMZAM",   "ZamZam Bank",                    "600"),
        BankInfo("ACSI",     "Amhara Credit and Saving",       "ACSI")
    )

    // -------------------------------------------------------------------------
    // Telecom Senders — centralized to avoid duplication
    // -------------------------------------------------------------------------
    val TELECOM_SENDERS = setOf("994", "251994", "+251994", "0994")

    // -------------------------------------------------------------------------
    // Time Constants
    // -------------------------------------------------------------------------
    const val MILLISECONDS_PER_DAY = 24L * 60L * 60L * 1000L
}

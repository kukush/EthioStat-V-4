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
    val SMS_SENDER_WHITELIST: Set<String> = setOf(

        // ── EthioTelecom ──────────────────────────────────────────────────────
        "994",           // Customer Service Hotline
        "251994",        // Full international-format sender
        "804",           // *804# balance / data query responses
        "810",           // *810# (shared with ACSI)
        "830",           // CRBT — Caller Ring Back Tone subscription / management
        "806",           // Airtime / Credit Transfer between subscribers
        "8994",          // SMS-based Inquiry and Support
        // NOTE: *999# (Voice/Data/SMS package menu) is a USSD *dial* code,
        // not an SMS sender — it is handled by UssdAccessibilityService.

        // ── Telebirr (EthioTelecom Mobile Money) — *127# ─────────────────────
        "127",
        "TELEBIRR",

        // ── Commercial Bank of Ethiopia (CBE) — *847# ────────────────────────
        "847",
        "CBEBirr",
        "CBEBIRR",

        // ── Awash Bank — *901# ────────────────────────────────────────────────
        "901",
        "AwashBank",

        // ── Bank of Abyssinia (BoA) — *815# ──────────────────────────────────
        "815",
        "AbyssiniaBank",

        // ── Dashen Bank — *721# ───────────────────────────────────────────────
        "721",
        "DashenBank",

        // ── Cooperative Bank of Oromia (Coopbank) — *896# ────────────────────
        "896",
        "Coopbank",

        // ── Hibret Bank — *844# ───────────────────────────────────────────────
        "844",
        "HibretBank",

        // ── Wegagen Bank — *889# ──────────────────────────────────────────────
        "889",
        "WegagenBank",

        // ── Abay Bank — *812# ─────────────────────────────────────────────────
        "812",
        "AbayBank",

        // ── Nib International Bank (NIB) — *865# ─────────────────────────────
        "865",
        "NIBBank",

        // ── Bunna Bank — *252# ────────────────────────────────────────────────
        "252",
        "BunnaBank",

        // ── Zemen Bank — *710# ────────────────────────────────────────────────
        "710",
        "ZemenBank",

        // ── Berhan Bank — *811# ───────────────────────────────────────────────
        "811",
        "BerhanBank",

        // ── Enat Bank — *846# ─────────────────────────────────────────────────
        "846",
        "EnatBank",

        // ── Tsehay Bank — *921# ───────────────────────────────────────────────
        "921",
        "TsehayBank",

        // ── Siinqee Bank — *767# ──────────────────────────────────────────────
        "767",
        "SiinqeeBank",

        // ── Amhara Bank — *946# ───────────────────────────────────────────────
        "946",
        "AmharaBank",

        // ── Lion International Bank — *801# ───────────────────────────────────
        "801",
        "LionBank",

        // ── Oromia Bank — *804# (alpha-sender used to distinguish from EthioTelecom 804)
        "OromiaBank",

        // ── Global Bank Ethiopia — *842# ──────────────────────────────────────
        "842",
        "GlobalBank",

        // ── Gadaa Bank — *898# ────────────────────────────────────────────────
        "898",
        "GadaaBank",

        // ── Hijra Bank — *881# ────────────────────────────────────────────────
        "881",
        "HijraBank",

        // ── Zad Bank — *899# ──────────────────────────────────────────────────
        "899",
        "ZadBank",

        // ── Ahadu Bank — *895# ────────────────────────────────────────────────
        "895",
        "AhaduBank",

        // ── Shabelle Bank — *808# ─────────────────────────────────────────────
        "808",
        "ShabelleBank",

        // ── Amhara Credit and Saving (ACSI) — *810# ──────────────────────────
        "ACSI",
        "ACSIBank"
    )

    // Source labels used in TransactionEntity.source
    const val SOURCE_TELEBIRR = "TELEBIRR"
    const val SOURCE_AIRTIME  = "AIRTIME"

    /**
     * Resolves a human-readable source label from a raw SMS sender number.
     * Telebirr senders: 127, 830, 806 or any sender whose number contains "TELEBIRR".
     */
    // Telebirr is identified by sender "127" or an alpha-sender containing "TELEBIRR".
    // 830 (CRBT) and 806 (Airtime Transfer) are EthioTelecom service codes, not Telebirr.
    val TELEBIRR_SENDERS: Set<String> = setOf("127")

    fun resolveSource(sender: String): String {
        val upper = sender.uppercase()
        
        // Telebirr
        if (upper.contains("TELEBIRR") || TELEBIRR_SENDERS.contains(sender)) return SOURCE_TELEBIRR
        
        // Commercial Bank of Ethiopia (CBE)
        if (upper.contains("CBE") || sender == "847") return "CBE"
        
        // Awash Bank
        if (upper.contains("AWASH") || sender == "901") return "AWASH"
        
        // Dashen Bank
        if (upper.contains("DASHEN") || sender == "721") return "DASHEN"
        
        // Coopbank
        if (upper.contains("COOP") || sender == "896") return "COOPBANK"
        
        // EthioTelecom (Airtime) natively restricted to its real numbers
        val ethioTelecomSenders = setOf("804", "805", "806", "810", "830", "994", "251994", "8994")
        if (ethioTelecomSenders.contains(sender)) return SOURCE_AIRTIME

        // Fallback to literal sender identity
        return sender
    }

    // -------------------------------------------------------------------------
    // Broadcast Actions
    // -------------------------------------------------------------------------
    const val ACTION_USSD_RESPONSE = "com.ethiobalance.app.ACTION_USSD_RESPONSE"

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
    const val PHONE_APP_PACKAGE = "com.android.phone"

    // -------------------------------------------------------------------------
    // USSD Response DB label
    // -------------------------------------------------------------------------
    const val USSD_REQUEST_LABEL = "Last USSD Request"

    // -------------------------------------------------------------------------
    // SmsLogEntity parsed type labels
    // -------------------------------------------------------------------------
    const val SMS_LOG_TYPE_PROCESSING = "PROCESSING"
}

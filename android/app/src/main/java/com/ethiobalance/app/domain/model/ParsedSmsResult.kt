package com.ethiobalance.app.domain.model

import com.ethiobalance.app.data.BalancePackageEntity

data class ParsedSmsResult(
    val scenario: SmsScenario,
    val confidence: Float,
    val packages: MutableList<BalancePackageEntity> = mutableListOf(),
    val deductedAmount: Double? = null,
    val addedAmount: Double? = null,
    val isRecharge: Boolean = false,
    val airtimeBalance: Double? = null,
    val transactionCategory: String? = null,
    val reference: String? = null,
    val partyName: String? = null
)

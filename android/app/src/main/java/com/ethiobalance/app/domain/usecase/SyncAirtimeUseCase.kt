package com.ethiobalance.app.domain.usecase

import com.ethiobalance.app.AppConstants
import com.ethiobalance.app.repository.SmsRepository
import javax.inject.Inject

class SyncAirtimeUseCase @Inject constructor(
    private val smsRepo: SmsRepository
) {
    operator fun invoke() {
        // Enforce business rule: trigger balance check via USSD *804#
        smsRepo.dialUssd(AppConstants.USSD_BALANCE_CHECK)
    }

    fun recharge(voucher: String) {
        val code = "${AppConstants.USSD_RECHARGE_SELF}$voucher#"
        smsRepo.dialUssd(code)
    }

    fun transfer(recipient: String, amount: String) {
        val code = "${AppConstants.USSD_TRANSFER_AIRTIME}$recipient*$amount#"
        smsRepo.dialUssd(code)
    }
}

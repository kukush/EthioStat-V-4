package com.ethiobalance.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Seeding of default transaction sources is intentionally NOT done here.
 * It must run AFTER the SMS permission request in [MainActivity] so that
 * [SettingsRepository.seedDefaultSourcesIfEmpty] can inspect the SMS inbox
 * and only seed sources that have actual messages in the last 90 days.
 */
@HiltAndroidApp
class EthioBalanceApp : Application()

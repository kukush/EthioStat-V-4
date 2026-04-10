package com.ethiobalance.app.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import com.ethiobalance.app.data.SimCardEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import com.ethiobalance.app.data.SimCardDao
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

class SimRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val simCardDao: SimCardDao
) {

    fun getAllSimCards(): Flow<List<SimCardEntity>> = simCardDao.getAllSimCards()

    suspend fun detectSimCards(): List<SimCardEntity> = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return@withContext emptyList()
        }

        val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        val activeSubscriptions = subscriptionManager.activeSubscriptionInfoList ?: return@withContext emptyList()

        @Suppress("DEPRECATION")
        val sims = activeSubscriptions.map { info ->
            SimCardEntity(
                id = info.subscriptionId.toString(),
                slotIndex = info.simSlotIndex,
                carrierName = info.carrierName?.toString() ?: "Unknown",
                phoneNumber = info.number ?: "Unknown",
                isPrimary = info.simSlotIndex == 0,
                lastUpdated = System.currentTimeMillis()
            )
        }

        sims.forEach { sim -> simCardDao.insertOrUpdate(sim) }
        sims
    }

    suspend fun insertOrUpdate(sim: SimCardEntity) = withContext(Dispatchers.IO) {
        simCardDao.insertOrUpdate(sim)
    }

    suspend fun delete(simId: String) = withContext(Dispatchers.IO) {
        simCardDao.deleteById(simId)
    }

    suspend fun setPrimary(simId: String) = withContext(Dispatchers.IO) {
        simCardDao.clearPrimary()
        simCardDao.setPrimary(simId)
    }
}

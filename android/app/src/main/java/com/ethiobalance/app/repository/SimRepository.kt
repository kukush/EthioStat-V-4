package com.ethiobalance.app.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import com.ethiobalance.app.data.AppDatabase
import com.ethiobalance.app.data.SimCardEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SimRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)

    fun getAllSimCards(): Flow<List<SimCardEntity>> = db.simCardDao().getAllSimCards()

    suspend fun detectSimCards(): List<SimCardEntity> = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return@withContext emptyList()
        }

        val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        val activeSubscriptions = subscriptionManager.activeSubscriptionInfoList ?: return@withContext emptyList()

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

        sims.forEach { sim -> db.simCardDao().insertOrUpdate(sim) }
        sims
    }

    suspend fun insertOrUpdate(sim: SimCardEntity) = withContext(Dispatchers.IO) {
        db.simCardDao().insertOrUpdate(sim)
    }

    suspend fun delete(simId: String) = withContext(Dispatchers.IO) {
        db.simCardDao().deleteById(simId)
    }

    suspend fun setPrimary(simId: String) = withContext(Dispatchers.IO) {
        db.simCardDao().clearPrimary()
        db.simCardDao().setPrimary(simId)
    }
}

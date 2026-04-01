package com.ethiobalance.app.plugins

import android.Manifest
import android.content.Context
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import com.ethiobalance.app.data.AppDatabase
import com.ethiobalance.app.data.SimCardEntity
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import com.getcapacitor.annotation.Permission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@CapacitorPlugin(
    name = "SimDetection",
    permissions = [
        Permission(
            strings = [Manifest.permission.READ_PHONE_STATE],
            alias = "phone"
        )
    ]
)
class SimDetectionPlugin : Plugin() {

    @PluginMethod
    fun getSimCards(call: PluginCall) {
        if (getPermissionState("phone") != com.getcapacitor.PermissionState.GRANTED) {
            requestPermissionForAlias("phone", call, "checkSimCallback")
            return
        }
        detectSims(call)
    }

    @PluginMethod
    fun checkSimCallback(call: PluginCall) {
        if (getPermissionState("phone") == com.getcapacitor.PermissionState.GRANTED) {
            detectSims(call)
        } else {
            call.reject("Permission READ_PHONE_STATE is required to detect SIM cards")
        }
    }

    private fun detectSims(call: PluginCall) {
        val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        val activeSubscriptions = subscriptionManager.activeSubscriptionInfoList ?: emptyList()
        
        val defaultVoiceId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            SubscriptionManager.getDefaultVoiceSubscriptionId()
        } else {
            -1
        }

        val simList = JSArray()
        val db = AppDatabase.getDatabase(context)

        CoroutineScope(Dispatchers.IO).launch {
            activeSubscriptions.forEachIndexed { index, info ->
                val slotIndex = info.simSlotIndex
                val carrierName = info.carrierName.toString()
                val phoneNumber = info.number ?: "Unknown"
                val subId = info.subscriptionId
                
                val isPrimary = subId == defaultVoiceId || (defaultVoiceId == -1 && index == 0)

                val entity = SimCardEntity(
                    id = "sim_$subId",
                    slotIndex = slotIndex,
                    carrierName = carrierName,
                    phoneNumber = phoneNumber,
                    isPrimary = isPrimary
                )
                db.simCardDao().insert(entity)

                val jsObj = JSObject()
                jsObj.put("id", entity.id)
                jsObj.put("slotIndex", slotIndex)
                jsObj.put("carrierName", carrierName)
                jsObj.put("phoneNumber", phoneNumber)
                jsObj.put("isPrimary", isPrimary)
                simList.put(jsObj)
            }

            val ret = JSObject()
            ret.put("sims", simList)
            call.resolve(ret)
        }
    }

    @PluginMethod
    fun setPrimarySim(call: PluginCall) {
        val simId = call.getString("id") ?: return call.reject("SIM ID is required")
        val db = AppDatabase.getDatabase(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Reset all to non-primary
                db.simCardDao().clearPrimaryStatus()
                // Set target as primary
                db.simCardDao().setPrimaryStatus(simId, true)
                call.resolve()
            } catch (e: Exception) {
                call.reject("Failed to set primary SIM", e)
            }
        }
    }
}

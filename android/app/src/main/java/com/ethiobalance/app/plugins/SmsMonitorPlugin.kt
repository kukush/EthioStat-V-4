package com.ethiobalance.app.plugins

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.ethiobalance.app.AppConstants
import com.ethiobalance.app.data.AppDatabase
import com.ethiobalance.app.data.TransactionSourceEntity
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import com.getcapacitor.annotation.Permission
import com.getcapacitor.PermissionState
import android.Manifest
import com.ethiobalance.app.services.ReconciliationEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@CapacitorPlugin(
    name = "SmsMonitor",
    permissions = [
        Permission(
            strings = [Manifest.permission.READ_SMS],
            alias = "sms"
        )
    ]
)
class SmsMonitorPlugin : Plugin() {

    private var ussdReceiver: BroadcastReceiver? = null

    override fun load() {
        super.load()
        ussdReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (AppConstants.ACTION_USSD_RESPONSE == intent.action) {
                    val text = intent.getStringExtra("ussd_text")
                    val ret = JSObject()
                    ret.put("text", text)
                    notifyListeners("ussdReceived", ret)
                }
            }
        }
        context.registerReceiver(
            ussdReceiver,
            IntentFilter(AppConstants.ACTION_USSD_RESPONSE),
            Context.RECEIVER_EXPORTED // Requires API 33+ explicit scope if needed, keeping default for now
        )
    }

    @PluginMethod
    fun startMonitoring(call: PluginCall) {
        call.resolve()
    }

    @PluginMethod
    fun scanHistory(call: PluginCall) {
        if (getPermissionState("sms") != PermissionState.GRANTED) {
            requestPermissionForAlias("sms", call, "checkSmsCallback")
            return
        }
        performSmsScan(call)
    }

    @PluginMethod
    fun checkSmsCallback(call: PluginCall) {
        if (getPermissionState("sms") == PermissionState.GRANTED) {
            performSmsScan(call)
        } else {
            call.reject("Permission READ_SMS is required to scan history")
        }
    }

    private fun performSmsScan(call: PluginCall) {
        val senderId = call.getString("senderId")
        if (senderId == null) {
            call.reject("senderId is required")
            return
        }
        val days = call.getInt("days") ?: 7
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val cutoffTime = System.currentTimeMillis() - (days * 24L * 60L * 60L * 1000L)
                
                val uri = android.provider.Telephony.Sms.Inbox.CONTENT_URI
                val projection = arrayOf("address", "body", "date")
                val selection = "address LIKE ? AND date >= ?"
                val selectionArgs = arrayOf("%$senderId%", cutoffTime.toString())
                
                val cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, "date ASC")
                var matchCount = 0

                cursor?.use {
                    val addressIndex = it.getColumnIndex("address")
                    val bodyIndex = it.getColumnIndex("body")
                    val dateIndex = it.getColumnIndex("date")
                    
                    while (it.moveToNext()) {
                        val sender = it.getString(addressIndex)
                        val body = it.getString(bodyIndex)
                        val timestamp = it.getLong(dateIndex)
                        
                        // Pass each existing message through the ReconciliationEngine
                        // Since we made it idempotent, duplicates won't be created.
                        ReconciliationEngine.processSms(sender, body, timestamp, db)
                        matchCount++
                    }
                }
                
                val ret = JSObject()
                ret.put("scanned", matchCount)
                call.resolve(ret)

            } catch (e: Exception) {
                call.reject("Error scanning SMS history", e)
            }
        }
    }

    @PluginMethod
    fun updateTransactionSources(call: PluginCall) {
        val sourcesArray = call.getArray("sources") ?: JSArray()
        val db = AppDatabase.getDatabase(context)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entities = mutableListOf<TransactionSourceEntity>()
                for (i in 0 until sourcesArray.length()) {
                    val obj = sourcesArray.getJSONObject(i)
                    entities.add(TransactionSourceEntity(
                        abbreviation = obj.getString("abbreviation") ?: "",
                        name = obj.getString("name") ?: "",
                        ussd = obj.getString("ussd") ?: "",
                        senderId = obj.getString("senderId") ?: obj.getString("abbreviation") ?: "",
                        isEnabled = obj.getBoolean("isEnabled") ?: true,
                        lastUpdated = System.currentTimeMillis()
                    ))
                }
                
                db.transactionSourceDao().insertAll(entities)
                
                // Update SharedPreferences Cache for fast access in SmsReceiver
                val prefs = context.getSharedPreferences("ethio_balance_prefs", Context.MODE_PRIVATE)
                val senderIds = entities.filter { it.isEnabled }.map { it.senderId }.toSet()
                prefs.edit().putStringSet("sms_whitelist", senderIds).apply()
                
                call.resolve()
            } catch (e: Exception) {
                call.reject("Failed to update transaction sources", e)
            }
        }
    }

    @PluginMethod
    fun getBalances(call: PluginCall) {
        val db = AppDatabase.getDatabase(context)
        CoroutineScope(Dispatchers.IO).launch {
            val packages = db.balancePackageDao().getAllPackages().first()
            val jsArray = JSArray()
            packages.forEach { pkg ->
                val obj = JSObject()
                obj.put("id", pkg.id)
                obj.put("type", pkg.type)
                obj.put("total", pkg.totalAmount)
                obj.put("value", pkg.remainingAmount)
                obj.put("unit", pkg.unit)
                obj.put("expiryDate", pkg.expiryDate.toString()) // Keep original Long as string for frontend parsing
                
                // Calculate days left
                val now = System.currentTimeMillis()
                val diffMills = pkg.expiryDate - now
                val daysLeft = if (diffMills > 0) (diffMills / (24 * 60 * 60 * 1000)).toInt() else 0
                obj.put("daysLeft", daysLeft)
                obj.put("totalDays", 30) // Fallback total days, could be improved if tracked
                
                jsArray.put(obj)
            }
            
            val ret = JSObject()
            ret.put("packages", jsArray)

            // Calculate Net Balance dynamically
            val income = db.transactionDao().getTotalByType("INCOME") ?: 0.0
            val expense = db.transactionDao().getTotalByType("EXPENSE") ?: 0.0
            ret.put("netBalance", income - expense)

            call.resolve(ret)
        }
    }

    @PluginMethod
    fun getTransactions(call: PluginCall) {
        val db = AppDatabase.getDatabase(context)
        CoroutineScope(Dispatchers.IO).launch {
            val transactions = db.transactionDao().getAllTransactions().first()
            val jsArray = JSArray()
            transactions.forEach { trans ->
                val obj = JSObject()
                obj.put("id", trans.id)
                obj.put("type", trans.type)
                obj.put("amount", trans.amount)
                obj.put("category", trans.category)
                obj.put("source", trans.source)
                obj.put("timestamp", trans.timestamp)
                jsArray.put(obj)
            }
            val ret = JSObject()
            ret.put("transactions", jsArray)
            call.resolve(ret)
        }
    }

    @PluginMethod
    fun dialUssd(call: PluginCall) {
        val code = call.getString("code")
        if (code == null) {
            call.reject("USSD code is required")
            return
        }
        
        // Encode # as %23 for the URI to be parsed correctly by Android
        val encodedCode = code.replace("#", "%23")
        val uri = android.net.Uri.parse("tel:$encodedCode")
        
        // Use ACTION_DIAL to avoid needing CALL_PHONE permission while still opening dialer directly
        val intent = Intent(Intent.ACTION_DIAL, uri)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        
        context.startActivity(intent)
        call.resolve()
    }

    override fun handleOnDestroy() {
        super.handleOnDestroy()
        ussdReceiver?.let {
            context.unregisterReceiver(it)
        }
    }
}

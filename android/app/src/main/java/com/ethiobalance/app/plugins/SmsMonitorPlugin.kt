package com.ethiobalance.app.plugins

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.ethiobalance.app.data.AppDatabase
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@CapacitorPlugin(name = "SmsMonitor")
class SmsMonitorPlugin : Plugin() {

    private var ussdReceiver: BroadcastReceiver? = null

    override fun load() {
        super.load()
        ussdReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if ("com.ethiobalance.app.ACTION_USSD_RESPONSE" == intent.action) {
                    val text = intent.getStringExtra("ussd_text")
                    val ret = JSObject()
                    ret.put("text", text)
                    notifyListeners("ussdReceived", ret)
                }
            }
        }
        context.registerReceiver(
            ussdReceiver,
            IntentFilter("com.ethiobalance.app.ACTION_USSD_RESPONSE"),
            Context.RECEIVER_EXPORTED // Requires API 33+ explicit scope if needed, keeping default for now
        )
    }

    @PluginMethod
    fun startMonitoring(call: PluginCall) {
        call.resolve()
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
                obj.put("totalAmount", pkg.totalAmount)
                obj.put("remainingAmount", pkg.remainingAmount)
                obj.put("unit", pkg.unit)
                obj.put("expiryDate", pkg.expiryDate)
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

    override fun handleOnDestroy() {
        super.handleOnDestroy()
        ussdReceiver?.let {
            context.unregisterReceiver(it)
        }
    }
}

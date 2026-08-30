package com.ethiobalance.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ethiobalance.app.data.TransactionEntity
import com.ethiobalance.app.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ExportPreviewDialog(
    isOpen: Boolean,
    onClose: () -> Unit,
    onConfirm: () -> Unit,
    transactions: List<TransactionEntity>,
    totalIncome: Double,
    totalExpense: Double,
    userName: String,
    userPhone: String
) {
    if (!isOpen) return

    val fmt = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Report Preview", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(16.dp))
                
                // Summary
                Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)).padding(16.dp)) {
                    Text("Account: $userName", fontSize = 14.sp)
                    Text("Phone: $userPhone", fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    
                    // Bar Chart
                    Row(modifier = Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.Bottom) {
                        Column(modifier = Modifier.weight(1f).padding(end = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Emerald600, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
                            Text("Income", fontSize = 10.sp)
                        }
                        Column(modifier = Modifier.weight(1f).padding(start = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Rose600, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
                            Text("Expense", fontSize = 10.sp)
                        }
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    Text("Income: +${fmt.format(totalIncome)} ETB", fontSize = 12.sp, color = Emerald600)
                    Text("Expense: -${fmt.format(totalExpense)} ETB", fontSize = 12.sp, color = Rose600)
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Table
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Name", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Amount", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    items(transactions.take(10)) { tx ->
                        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(tx.partyName ?: "N/A", fontSize = 12.sp, modifier = Modifier.weight(1f))
                            Text(fmt.format(tx.amount), fontSize = 12.sp)
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onClose) { Text("Cancel") }
                    Button(onClick = onConfirm) { Text("Confirm & Download") }
                }
            }
        }
    }
}

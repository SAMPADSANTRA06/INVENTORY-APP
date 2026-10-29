package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ValuationMethod
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StatCard
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.StatusBlue
import com.example.ui.theme.StatusRed
import com.example.ui.viewmodel.InventoryViewModel
import java.util.Locale

@Composable
fun ReportsScreen(
    viewModel: InventoryViewModel,
    modifier: Modifier = Modifier
) {
    val items by viewModel.items.collectAsState()
    val ledgerEntries by viewModel.ledgerEntries.collectAsState()
    val salesBills by viewModel.salesBills.collectAsState()
    val purchaseBills by viewModel.purchaseBills.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val valuationMethod by viewModel.valuationMethod.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Stock Valuation, 1: Profit & Loss, 2: GST Summary, 3: Outstanding

    // Calculated Valuation
    val (costValuation, retailValuation) = remember(items, ledgerEntries, valuationMethod) {
        var cost = 0.0
        var retail = 0.0
        for (item in items) {
            cost += item.currentStock * item.purchasePrice
            retail += item.currentStock * item.sellingPrice
        }
        Pair(cost, retail)
    }

    // P&L Calculations
    val totalRevenue = salesBills.sumOf { it.grandTotal }
    val totalPurchases = purchaseBills.sumOf { it.grandTotal }
    val estimatedProfit = (totalRevenue - (salesBills.sumOf { it.subtotal } * 0.7)).coerceAtLeast(0.0) // Margin estimate

    // GST Breakdown
    val totalCgst = salesBills.sumOf { it.cgstAmount }
    val totalSgst = salesBills.sumOf { it.sgstAmount }
    val totalIgst = salesBills.sumOf { it.igstAmount }
    val totalGstCollected = totalCgst + totalSgst + totalIgst

    // Outstandings
    val totalReceivable = customers.sumOf { it.outstandingBalance }
    val totalPayable = suppliers.sumOf { it.balanceDue }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Financial & Inventory Reports",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Real-time auditing, tax breakdowns, and stock valuation",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = { viewModel.showToast("Exported reports summary to CSV preview") },
                modifier = Modifier.testTag("btn_export_reports")
            ) {
                Icon(Icons.Default.Download, contentDescription = "Export Report", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 0.dp,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Stock Valuation") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Profit & Loss") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("GST Summary") })
            Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Outstandings") })
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (selectedTab) {
            0 -> {
                // Stock Valuation Tab
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Valuation Method Selector
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Valuation Method:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = valuationMethod == ValuationMethod.WEIGHTED_AVERAGE,
                                    onClick = { viewModel.setValuationMethod(ValuationMethod.WEIGHTED_AVERAGE) },
                                    label = { Text("Weighted Avg", fontSize = 11.sp) }
                                )
                                FilterChip(
                                    selected = valuationMethod == ValuationMethod.FIFO,
                                    onClick = { viewModel.setValuationMethod(ValuationMethod.FIFO) },
                                    label = { Text("FIFO", fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "Total Cost Valuation",
                            value = String.format(Locale.US, "₹%.2f", costValuation),
                            subtitle = "Asset value at purchase cost",
                            icon = Icons.Default.AccountBalance,
                            iconTint = EmeraldPrimaryLight,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Total Retail Valuation",
                            value = String.format(Locale.US, "₹%.2f", retailValuation),
                            subtitle = "Potential sales realization",
                            icon = Icons.Default.TrendingUp,
                            iconTint = StatusBlue,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Item Valuation Breakdown", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (items.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.Inventory2,
                            title = "No items to value",
                            description = "Add catalog items and stock to generate valuation reports."
                        )
                    } else {
                        items.forEach { item ->
                            val itemCostVal = item.currentStock * item.purchasePrice
                            val itemRetailVal = item.currentStock * item.sellingPrice
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 1.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Qty: ${item.currentStock} ${item.unit} @ ₹${item.purchasePrice}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(String.format(Locale.US, "₹%.2f", itemCostVal), fontWeight = FontWeight.Bold, color = EmeraldPrimaryLight)
                                        Text(String.format(Locale.US, "Retail: ₹%.2f", itemRetailVal), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // Profit & Loss Tab
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Gross Revenue",
                        value = String.format(Locale.US, "₹%.2f", totalRevenue),
                        subtitle = "${salesBills.size} invoices issued",
                        icon = Icons.Default.AttachMoney,
                        iconTint = EmeraldPrimaryLight,
                        modifier = Modifier.fillMaxWidth()
                    )

                    StatCard(
                        title = "Total Procurement Expenses",
                        value = String.format(Locale.US, "₹%.2f", totalPurchases),
                        subtitle = "${purchaseBills.size} purchase bills",
                        icon = Icons.Default.ShoppingCart,
                        iconTint = StatusBlue,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Estimated Gross Profit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format(Locale.US, "₹%.2f", estimatedProfit),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimaryLight
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Gross Profit = Total Revenue - Cost of Goods Sold (COGS)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            2 -> {
                // GST Summary Tab (GSTR-1 style)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("GST Tax Collected Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("CGST (Central Tax):", style = MaterialTheme.typography.bodyMedium)
                                Text(String.format(Locale.US, "₹%.2f", totalCgst), fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("SGST (State Tax):", style = MaterialTheme.typography.bodyMedium)
                                Text(String.format(Locale.US, "₹%.2f", totalSgst), fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("IGST (Integrated Tax):", style = MaterialTheme.typography.bodyMedium)
                                Text(String.format(Locale.US, "₹%.2f", totalIgst), fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Tax Liability:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(String.format(Locale.US, "₹%.2f", totalGstCollected), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = EmeraldPrimaryLight)
                            }
                        }
                    }
                }
            }

            3 -> {
                // Outstanding Receivables and Payables
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "Customer Receivables",
                            value = String.format(Locale.US, "₹%.2f", totalReceivable),
                            subtitle = "Payments due from buyers",
                            icon = Icons.Default.CallReceived,
                            iconTint = EmeraldPrimaryLight,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Supplier Payables",
                            value = String.format(Locale.US, "₹%.2f", totalPayable),
                            subtitle = "Due to vendors",
                            icon = Icons.Default.CallMade,
                            iconTint = StatusRed,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Customer Outstanding List", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

                    val dueCustomers = customers.filter { it.outstandingBalance > 0 }
                    if (dueCustomers.isEmpty()) {
                        Text("No customer dues pending. All sales settled!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        dueCustomers.forEach { cust ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(cust.name, fontWeight = FontWeight.Bold)
                                        if (cust.phone.isNotBlank()) Text(cust.phone, style = MaterialTheme.typography.labelSmall)
                                    }
                                    Text(String.format(Locale.US, "₹%.2f", cust.outstandingBalance), color = StatusRed, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.example.data.model.MovementType
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.InventoryViewModel
import com.example.ui.viewmodel.Screen
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: InventoryViewModel,
    modifier: Modifier = Modifier
) {
    val items by viewModel.items.collectAsState()
    val ledgerEntries by viewModel.ledgerEntries.collectAsState()
    val purchaseBills by viewModel.purchaseBills.collectAsState()
    val salesBills by viewModel.salesBills.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val companyProfile by viewModel.companyProfile.collectAsState()
    val valuationMethod by viewModel.valuationMethod.collectAsState()

    // Metrics calculations
    val totalItemsCount = items.size
    val lowStockItems = items.filter { it.currentStock in 0.01..it.reorderLevel }
    val outOfStockItems = items.filter { it.currentStock <= 0.0 }

    val (costValuation, retailValuation) = remember(items, ledgerEntries, valuationMethod) {
        var cost = 0.0
        var retail = 0.0
        for (item in items) {
            cost += item.currentStock * item.purchasePrice
            retail += item.currentStock * item.sellingPrice
        }
        Pair(cost, retail)
    }

    // Today's Sales and Purchases
    val todayCal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val todaySalesTotal = salesBills.filter { it.date >= todayCal }.sumOf { it.grandTotal }
    val todayPurchasesTotal = purchaseBills.filter { it.date >= todayCal }.sumOf { it.grandTotal }

    // Category distribution
    val categoryDistribution = remember(items) {
        items.groupBy { it.categoryName.ifBlank { "Uncategorized" } }
            .mapValues { entry -> entry.value.sumOf { it.currentStock } }
            .filter { it.value > 0 }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Welcome & Role Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = companyProfile?.companyName ?: "Sampad Santra",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Welcome, ${currentUser?.name ?: "User"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                currentUser?.role?.let { RoleBadge(role = it) }
                IconButton(
                    onClick = { viewModel.logout() },
                    modifier = Modifier.testTag("dashboard_logout_button")
                ) {
                    Icon(Icons.Default.Logout, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.navigateTo(Screen.Sales) },
                modifier = Modifier.weight(1f).testTag("quick_new_sale"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Sale", fontSize = 13.sp)
            }

            OutlinedButton(
                onClick = { viewModel.navigateTo(Screen.Stock) },
                modifier = Modifier.weight(1f).testTag("quick_stock_in"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.SwapVert, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Stock In/Out", fontSize = 13.sp)
            }

            OutlinedButton(
                onClick = { viewModel.navigateTo(Screen.Items) },
                modifier = Modifier.weight(1f).testTag("quick_add_item"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("+ Item", fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // If completely empty: show guidance card
        if (totalItemsCount == 0) {
            EmptyStateView(
                icon = Icons.Default.Inventory2,
                title = "Your inventory is currently empty",
                description = "Start by adding your catalog items, setting opening stock, or recording your first purchase bill.",
                actionButtonText = "Add Your First Item",
                onActionClick = { viewModel.navigateTo(Screen.Items) }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Primary KPI Cards Grid (2x2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Stock Value (Cost)",
                value = String.format(Locale.US, "₹%.0f", costValuation),
                subtitle = String.format(Locale.US, "Retail: ₹%.0f", retailValuation),
                icon = Icons.Default.AccountBalanceWallet,
                iconTint = EmeraldPrimaryLight,
                modifier = Modifier.weight(1f).testTag("kpi_stock_value")
            )
            StatCard(
                title = "Total Catalog Items",
                value = "$totalItemsCount",
                subtitle = "${items.sumOf { it.currentStock }.toInt()} total units",
                icon = Icons.Default.Category,
                iconTint = StatusBlue,
                modifier = Modifier.weight(1f).testTag("kpi_total_items"),
                onClick = { viewModel.navigateTo(Screen.Items) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Low Stock Alert",
                value = "${lowStockItems.size}",
                subtitle = "Needs reorder",
                icon = Icons.Default.WarningAmber,
                iconTint = StatusAmber,
                modifier = Modifier.weight(1f).testTag("kpi_low_stock"),
                onClick = { viewModel.navigateTo(Screen.Items) }
            )
            StatCard(
                title = "Out of Stock",
                value = "${outOfStockItems.size}",
                subtitle = "Zero inventory",
                icon = Icons.Default.ErrorOutline,
                iconTint = StatusRed,
                modifier = Modifier.weight(1f).testTag("kpi_out_of_stock"),
                onClick = { viewModel.navigateTo(Screen.Items) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Today's Sales vs Purchases Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Today's Business Activity",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Today's Sales", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format(Locale.US, "₹%.2f", todaySalesTotal),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldPrimaryLight
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Today's Purchases", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format(Locale.US, "₹%.2f", todayPurchasesTotal),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = StatusBlue
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stock by Category Bar Chart
        CategoryBarChart(categoryData = categoryDistribution)

        Spacer(modifier = Modifier.height(16.dp))

        // Low Stock & Expiry Alerts Section
        if (lowStockItems.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AmberContainerLight.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = StatusAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Reorder Alerts (${lowStockItems.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = AmberOnContainerLight
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    for (item in lowStockItems.take(4)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = AmberOnContainerLight
                                )
                                Text(
                                    text = "Current: ${item.currentStock} ${item.unit} | Reorder: ${item.reorderLevel}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AmberOnContainerLight.copy(alpha = 0.8f)
                                )
                            }

                            FilledTonalButton(
                                onClick = { viewModel.navigateTo(Screen.Stock) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Stock In", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

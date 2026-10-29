package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ItemEntity
import com.example.data.model.MovementType
import com.example.data.model.StockLedgerEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusBlue
import com.example.ui.theme.StatusRed
import com.example.ui.viewmodel.InventoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockMovementsScreen(
    viewModel: InventoryViewModel,
    modifier: Modifier = Modifier
) {
    val items by viewModel.items.collectAsState()
    val godowns by viewModel.godowns.collectAsState()
    val ledgerEntries by viewModel.ledgerEntries.collectAsState()

    var showMovementDialog by remember { mutableStateOf(false) }
    var selectedMovementType by remember { mutableStateOf(MovementType.ADJUSTMENT_ADD) }
    var ledgerSearchQuery by remember { mutableStateOf("") }

    val filteredLedger = remember(ledgerEntries, ledgerSearchQuery) {
        if (ledgerSearchQuery.isBlank()) ledgerEntries
        else ledgerEntries.filter {
            it.itemName.contains(ledgerSearchQuery, ignoreCase = true) ||
                    it.referenceDoc.contains(ledgerSearchQuery, ignoreCase = true) ||
                    it.movementType.displayName.contains(ledgerSearchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Action Row
        Text(
            text = "Stock Movements & Ledger",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = "Every inventory change is tracked with double-entry integrity. Negative stock is strictly prevented.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Action Buttons Row (Stock In, Stock Out, Adjust, Transfer)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    selectedMovementType = MovementType.ADJUSTMENT_ADD
                    showMovementDialog = true
                },
                modifier = Modifier.weight(1f).testTag("btn_stock_in"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimaryLight)
            ) {
                Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Stock In", fontSize = 12.sp)
            }

            Button(
                onClick = {
                    selectedMovementType = MovementType.ADJUSTMENT_SUB
                    showMovementDialog = true
                },
                modifier = Modifier.weight(1f).testTag("btn_stock_out"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StatusAmber)
            ) {
                Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Stock Out", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = {
                    selectedMovementType = MovementType.TRANSFER_IN
                    showMovementDialog = true
                },
                modifier = Modifier.weight(1f).testTag("btn_transfer"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.SyncAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Transfer", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Ledger Search
        OutlinedTextField(
            value = ledgerSearchQuery,
            onValueChange = { ledgerSearchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search ledger by Item Name or Reference Doc...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Ledger List
        if (filteredLedger.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.ReceiptLong,
                title = "No ledger entries yet",
                description = "Stock movements will appear here automatically when purchases, sales, transfers, or adjustments occur."
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredLedger, key = { it.id }) { entry ->
                    LedgerCard(entry = entry)
                }
            }
        }
    }

    // Movement Dialog (Stock In / Out / Adjust / Transfer)
    if (showMovementDialog) {
        RecordMovementDialog(
            items = items,
            godowns = godowns.ifEmpty { listOf(com.example.data.model.GodownEntity(1, "Main Godown")) },
            initialMovementType = selectedMovementType,
            onDismiss = { showMovementDialog = false },
            onSubmit = { itemId, godownId, godownName, type, qty, cost, ref, notes ->
                viewModel.recordStockMovement(
                    itemId = itemId,
                    godownId = godownId,
                    godownName = godownName,
                    movementType = type,
                    quantity = qty,
                    unitCost = cost,
                    referenceDoc = ref,
                    notes = notes
                ) {
                    showMovementDialog = false
                }
            }
        )
    }
}

@Composable
fun LedgerCard(entry: StockLedgerEntity) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val (typeBg, typeColor) = when {
        entry.movementType.isPositive -> Pair(EmeraldPrimaryLight.copy(alpha = 0.12f), EmeraldPrimaryLight)
        else -> Pair(StatusRed.copy(alpha = 0.12f), StatusRed)
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = RoundedCornerShape(6.dp), color = typeBg) {
                        Text(
                            text = entry.movementType.displayName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = typeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = entry.itemName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "${if (entry.movementType.isPositive) "+" else "-"}${entry.quantity}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = typeColor
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${entry.godownName} • ${dateFormat.format(Date(entry.timestamp))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Closing: ${entry.balanceAfter}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (entry.referenceDoc.isNotBlank() || entry.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = listOf(
                        entry.referenceDoc.takeIf { it.isNotBlank() }?.let { "Ref: $it" },
                        entry.notes.takeIf { it.isNotBlank() }?.let { "Note: $it" }
                    ).filterNotNull().joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordMovementDialog(
    items: List<ItemEntity>,
    godowns: List<com.example.data.model.GodownEntity>,
    initialMovementType: MovementType,
    onDismiss: () -> Unit,
    onSubmit: (Long, Long, String, MovementType, Double, Double, String, String) -> Unit
) {
    var selectedItem by remember { mutableStateOf(items.firstOrNull()) }
    var movementType by remember { mutableStateOf(initialMovementType) }
    var selectedGodown by remember { mutableStateOf(godowns.firstOrNull()) }
    var quantityText by remember { mutableStateOf("") }
    var referenceDoc by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Record Stock Movement",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                if (items.isEmpty()) {
                    Text("No items available. Please create an item first.", color = MaterialTheme.colorScheme.error)
                    Button(onClick = onDismiss) { Text("Close") }
                    return@Column
                }

                // Item Selector Dropdown
                var itemDropdownExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = itemDropdownExpanded,
                    onExpandedChange = { itemDropdownExpanded = !itemDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedItem?.name ?: "Select Item",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Item *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = itemDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = itemDropdownExpanded,
                        onDismissRequest = { itemDropdownExpanded = false }
                    ) {
                        items.forEach { item ->
                            DropdownMenuItem(
                                text = { Text("${item.name} (Stock: ${item.currentStock} ${item.unit})") },
                                onClick = {
                                    selectedItem = item
                                    itemDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Available Stock Banner
                selectedItem?.let { itm ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Current Stock:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "${itm.currentStock} ${itm.unit}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = if (itm.currentStock <= 0) StatusRed else EmeraldPrimaryLight
                            )
                        }
                    }
                }

                // Movement Type Radio Row
                Text("Movement Type:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        Pair(MovementType.ADJUSTMENT_ADD, "Stock In (+)"),
                        Pair(MovementType.ADJUSTMENT_SUB, "Stock Out (-)"),
                        Pair(MovementType.TRANSFER_IN, "Transfer")
                    ).forEach { (type, label) ->
                        FilterChip(
                            selected = movementType == type,
                            onClick = { movementType = type },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("Quantity *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("movement_input_qty"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = referenceDoc,
                    onValueChange = { referenceDoc = it },
                    label = { Text("Reference Doc / Challan #") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Reason / Notes (e.g. Damage, Expired, Restock)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (errorMsg != null) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val itm = selectedItem
                            if (itm == null) {
                                errorMsg = "Select an item"
                                return@Button
                            }
                            val qty = quantityText.toDoubleOrNull()
                            if (qty == null || qty <= 0) {
                                errorMsg = "Enter a valid quantity > 0"
                                return@Button
                            }
                            if (!movementType.isPositive && itm.currentStock < qty) {
                                errorMsg = "Cannot perform: Negative stock prevented! Available: ${itm.currentStock}, Requested: $qty"
                                return@Button
                            }

                            val godown = selectedGodown ?: godowns.first()
                            onSubmit(
                                itm.id,
                                godown.id,
                                godown.name,
                                movementType,
                                qty,
                                itm.purchasePrice,
                                referenceDoc.trim(),
                                notes.trim()
                            )
                        },
                        modifier = Modifier.testTag("btn_submit_movement")
                    ) {
                        Text("Save Movement")
                    }
                }
            }
        }
    }
}

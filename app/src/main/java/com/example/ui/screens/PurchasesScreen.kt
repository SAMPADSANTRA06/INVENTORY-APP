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
import com.example.data.model.*
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusBlue
import com.example.ui.theme.StatusRed
import com.example.ui.viewmodel.InventoryViewModel
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.ui.draw.clip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchasesScreen(
    viewModel: InventoryViewModel,
    modifier: Modifier = Modifier
) {
    val suppliers by viewModel.suppliers.collectAsState()
    val purchaseBills by viewModel.purchaseBills.collectAsState()
    val items by viewModel.items.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Purchase Bills, 1: Suppliers
    var showAddSupplierDialog by remember { mutableStateOf(false) }
    var supplierToEdit by remember { mutableStateOf<SupplierEntity?>(null) }
    var supplierToDelete by remember { mutableStateOf<SupplierEntity?>(null) }
    var showNewBillDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (activeTab == 0) {
                        if (suppliers.isEmpty()) {
                            viewModel.showToast("Please add at least one supplier first!")
                            showAddSupplierDialog = true
                        } else if (items.isEmpty()) {
                            viewModel.showToast("Please add items to your catalog first!")
                        } else {
                            showNewBillDialog = true
                        }
                    } else {
                        supplierToEdit = null
                        showAddSupplierDialog = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_purchase_action")
            ) {
                Icon(
                    imageVector = if (activeTab == 0) Icons.Default.PostAdd else Icons.Default.PersonAdd,
                    contentDescription = "Add"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Purchases & Suppliers",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(10.dp))

            TabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Purchase Bills (${purchaseBills.size})") }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Suppliers (${suppliers.size})") }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (activeTab == 0) {
                // Purchase Bills List
                if (purchaseBills.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.Receipt,
                        title = "No purchase bills yet",
                        description = "Record purchase invoices from your vendors. Stock and supplier balances will update automatically.",
                        actionButtonText = "+ New Purchase Bill",
                        onActionClick = {
                            if (suppliers.isEmpty()) {
                                showAddSupplierDialog = true
                            } else {
                                showNewBillDialog = true
                            }
                        }
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(purchaseBills, key = { it.id }) { bill ->
                            PurchaseBillCard(bill = bill)
                        }
                    }
                }
            } else {
                // Suppliers List
                if (suppliers.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.PeopleOutline,
                        title = "No suppliers added yet",
                        description = "Add vendors and suppliers with GSTIN, phone, address, and outstanding balance.",
                        actionButtonText = "+ Add Supplier",
                        onActionClick = {
                            supplierToEdit = null
                            showAddSupplierDialog = true
                        }
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(suppliers, key = { it.id }) { sup ->
                            SupplierCard(
                                supplier = sup,
                                onEdit = {
                                    supplierToEdit = sup
                                    showAddSupplierDialog = true
                                },
                                onDelete = { supplierToDelete = sup }
                            )
                        }
                    }
                }
            }
        }
    }

    // Add/Edit Supplier Dialog
    if (showAddSupplierDialog) {
        AddEditSupplierDialog(
            supplier = supplierToEdit,
            onDismiss = { showAddSupplierDialog = false },
            onSave = { savedSup ->
                viewModel.saveSupplier(savedSup) {
                    showAddSupplierDialog = false
                }
            }
        )
    }

    // New Purchase Bill Dialog
    if (showNewBillDialog) {
        NewPurchaseBillDialog(
            suppliers = suppliers,
            items = items,
            onDismiss = { showNewBillDialog = false },
            onSave = { bill, billItems ->
                viewModel.createPurchaseBill(bill, billItems) {
                    showNewBillDialog = false
                }
            }
        )
    }

    // Confirm Delete Supplier
    if (supplierToDelete != null) {
        ConfirmDeleteDialog(
            title = "Delete Supplier?",
            message = "Are you sure you want to delete supplier '${supplierToDelete!!.name}'?",
            onConfirm = {
                viewModel.deleteSupplier(supplierToDelete!!.id)
                supplierToDelete = null
            },
            onDismiss = { supplierToDelete = null }
        )
    }
}

@Composable
fun PurchaseBillCard(bill: PurchaseBillEntity) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val (statusBg, statusColor) = when (bill.paymentStatus) {
        PaymentStatus.PAID -> Pair(EmeraldPrimaryLight.copy(alpha = 0.15f), EmeraldPrimaryLight)
        PaymentStatus.PARTIAL -> Pair(StatusAmber.copy(alpha = 0.15f), StatusAmber)
        PaymentStatus.DUE -> Pair(StatusRed.copy(alpha = 0.15f), StatusRed)
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Bill #${bill.billNumber}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${bill.supplierName} • ${dateFormat.format(Date(bill.date))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(shape = RoundedCornerShape(8.dp), color = statusBg) {
                    Text(
                        text = bill.paymentStatus.displayName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text("Taxable", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(String.format(Locale.US, "₹%.2f", bill.subtotal), fontWeight = FontWeight.Medium)
                    }
                    Column {
                        Text("GST Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(String.format(Locale.US, "₹%.2f", bill.gstTotal), fontWeight = FontWeight.Medium)
                    }
                    if (bill.dueAmount > 0) {
                        Column {
                            Text("Due", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(String.format(Locale.US, "₹%.2f", bill.dueAmount), fontWeight = FontWeight.Bold, color = StatusRed)
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Grand Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = String.format(Locale.US, "₹%.2f", bill.grandTotal),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = EmeraldPrimaryLight
                    )
                }
            }
        }
    }
}

@Composable
fun SupplierCard(
    supplier: SupplierEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = supplier.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    if (supplier.phone.isNotBlank()) {
                        Text("Phone: ${supplier.phone}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (supplier.gstin.isNotBlank()) {
                        Text("GSTIN: ${supplier.gstin}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (supplier.balanceDue > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Outstanding Due: ₹${supplier.balanceDue}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = StatusRed
                )
            }
        }
    }
}

@Composable
fun AddEditSupplierDialog(
    supplier: SupplierEntity?,
    onDismiss: () -> Unit,
    onSave: (SupplierEntity) -> Unit
) {
    var name by remember { mutableStateOf(supplier?.name ?: "") }
    var gstin by remember { mutableStateOf(supplier?.gstin ?: "") }
    var phone by remember { mutableStateOf(supplier?.phone ?: "") }
    var email by remember { mutableStateOf(supplier?.email ?: "") }
    var address by remember { mutableStateOf(supplier?.address ?: "") }
    var state by remember { mutableStateOf(supplier?.state ?: "West Bengal") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (supplier == null) "Add Supplier" else "Edit Supplier",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Supplier / Company Name *") },
                    modifier = Modifier.fillMaxWidth().testTag("supplier_input_name"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = gstin,
                    onValueChange = { gstin = it.uppercase() },
                    label = { Text("GSTIN") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = state,
                        onValueChange = { state = it },
                        label = { Text("State") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(
                                    (supplier ?: SupplierEntity(name = name)).copy(
                                        name = name.trim(),
                                        gstin = gstin.trim(),
                                        phone = phone.trim(),
                                        email = email.trim(),
                                        address = address.trim(),
                                        state = state.trim()
                                    )
                                )
                            }
                        },
                        modifier = Modifier.testTag("btn_save_supplier")
                    ) {
                        Text("Save Supplier")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPurchaseBillDialog(
    suppliers: List<SupplierEntity>,
    items: List<ItemEntity>,
    onDismiss: () -> Unit,
    onSave: (PurchaseBillEntity, List<PurchaseBillItemEntity>) -> Unit
) {
    var billNumber by remember { mutableStateOf("PB-${System.currentTimeMillis() % 100000}") }
    var selectedSupplier by remember { mutableStateOf(suppliers.firstOrNull()) }
    var selectedItem by remember { mutableStateOf(items.firstOrNull()) }
    var quantityText by remember { mutableStateOf("10") }
    var unitPriceText by remember { mutableStateOf(selectedItem?.purchasePrice?.toString() ?: "100") }
    var paidAmountText by remember { mutableStateOf("") }

    var billItems by remember { mutableStateOf(listOf<PurchaseBillItemEntity>()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "New Purchase Bill",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = billNumber,
                            onValueChange = { billNumber = it },
                            label = { Text("Bill / Invoice #") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        // Supplier selector
                        var supDropdown by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = supDropdown,
                            onExpandedChange = { supDropdown = !supDropdown },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = selectedSupplier?.name ?: "Select Supplier",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Supplier *") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supDropdown) },
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = supDropdown,
                                onDismissRequest = { supDropdown = false }
                            ) {
                                suppliers.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text(s.name) },
                                        onClick = {
                                            selectedSupplier = s
                                            supDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Add Line Item Section
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Add Line Item", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)

                            var itmDropdown by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = itmDropdown,
                                onExpandedChange = { itmDropdown = !itmDropdown }
                            ) {
                                OutlinedTextField(
                                    value = selectedItem?.name ?: "Select Item",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Catalog Item") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = itmDropdown) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = itmDropdown,
                                    onDismissRequest = { itmDropdown = false }
                                ) {
                                    items.forEach { itm ->
                                        DropdownMenuItem(
                                            text = { Text("${itm.name} (Cost: ₹${itm.purchasePrice})") },
                                            onClick = {
                                                selectedItem = itm
                                                unitPriceText = itm.purchasePrice.toString()
                                                itmDropdown = false
                                            }
                                        )
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = quantityText,
                                    onValueChange = { quantityText = it },
                                    label = { Text("Qty") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = unitPriceText,
                                    onValueChange = { unitPriceText = it },
                                    label = { Text("Unit Cost (₹)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                Button(
                                    onClick = {
                                        val itm = selectedItem ?: return@Button
                                        val q = quantityText.toDoubleOrNull() ?: 1.0
                                        val p = unitPriceText.toDoubleOrNull() ?: 0.0
                                        val gstAmt = (q * p * itm.gstRate / 100.0)
                                        val total = (q * p) + gstAmt

                                        billItems = billItems + PurchaseBillItemEntity(
                                            billId = 0L,
                                            itemId = itm.id,
                                            itemName = itm.name,
                                            quantity = q,
                                            unitPrice = p,
                                            gstRate = itm.gstRate,
                                            gstAmount = gstAmt,
                                            totalAmount = total
                                        )
                                    },
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                ) {
                                    Text("+ Add")
                                }
                            }
                        }
                    }

                    // Added items list
                    if (billItems.isNotEmpty()) {
                        Text("Items in Bill (${billItems.size}):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        billItems.forEachIndexed { idx, bi ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${bi.itemName} x ${bi.quantity} @ ₹${bi.unitPrice}", fontSize = 13.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(String.format(Locale.US, "₹%.2f", bi.totalAmount), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    IconButton(onClick = {
                                        billItems = billItems.toMutableList().also { it.removeAt(idx) }
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    val subtotal = billItems.sumOf { it.quantity * it.unitPrice }
                    val gstTotal = billItems.sumOf { it.gstAmount }
                    val grandTotal = subtotal + gstTotal

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Subtotal:", style = MaterialTheme.typography.bodySmall)
                                Text(String.format(Locale.US, "₹%.2f", subtotal), fontWeight = FontWeight.Medium)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("GST Amount:", style = MaterialTheme.typography.bodySmall)
                                Text(String.format(Locale.US, "₹%.2f", gstTotal), fontWeight = FontWeight.Medium)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Grand Total:", fontWeight = FontWeight.Bold)
                                Text(String.format(Locale.US, "₹%.2f", grandTotal), fontWeight = FontWeight.Bold, color = EmeraldPrimaryLight)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = paidAmountText,
                        onValueChange = { paidAmountText = it },
                        label = { Text("Amount Paid (₹) - Leave blank for full payment") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val sup = selectedSupplier
                            if (sup == null) {
                                errorMsg = "Select a supplier"
                                return@Button
                            }
                            if (billItems.isEmpty()) {
                                errorMsg = "Add at least one item to bill"
                                return@Button
                            }

                            val subtotal = billItems.sumOf { it.quantity * it.unitPrice }
                            val gstTotal = billItems.sumOf { it.gstAmount }
                            val grandTotal = subtotal + gstTotal
                            val paid = paidAmountText.toDoubleOrNull() ?: grandTotal
                            val due = (grandTotal - paid).coerceAtLeast(0.0)

                            val status = when {
                                due <= 0 -> PaymentStatus.PAID
                                paid > 0 -> PaymentStatus.PARTIAL
                                else -> PaymentStatus.DUE
                            }

                            val bill = PurchaseBillEntity(
                                billNumber = billNumber.trim(),
                                supplierId = sup.id,
                                supplierName = sup.name,
                                subtotal = subtotal,
                                gstTotal = gstTotal,
                                grandTotal = grandTotal,
                                paidAmount = paid,
                                dueAmount = due,
                                paymentStatus = status
                            )

                            onSave(bill, billItems)
                        },
                        modifier = Modifier.testTag("btn_save_purchase_bill")
                    ) {
                        Text("Create Bill & Update Stock")
                    }
                }
            }
        }
    }
}

package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.ui.components.EmptyStateView
import com.example.ui.components.PdfInvoiceGenerator
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusRed
import com.example.ui.viewmodel.InventoryViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesPosScreen(
    viewModel: InventoryViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val items by viewModel.items.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val selectedCustomer by viewModel.selectedCustomer.collectAsState()
    val companyProfile by viewModel.companyProfile.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showCustomerDialog by remember { mutableStateOf(false) }
    var selectedPaymentMode by remember { mutableStateOf(PaymentMode.CASH) }
    var overallDiscount by remember { mutableStateOf("0") }
    var paidAmountInput by remember { mutableStateOf("") }

    var lastCompletedBill by remember { mutableStateOf<SalesBillEntity?>(null) }
    var lastCompletedItems by remember { mutableStateOf<List<SalesBillItemEntity>>(emptyList()) }
    var showInvoiceSuccessDialog by remember { mutableStateOf(false) }

    // Filter available items for catalog
    val availableCatalog = remember(items, searchQuery) {
        items.filter { item ->
            searchQuery.isBlank() ||
                    item.name.contains(searchQuery, ignoreCase = true) ||
                    item.barcode.contains(searchQuery, ignoreCase = true) ||
                    item.sku.contains(searchQuery, ignoreCase = true)
        }
    }

    // Totals calculation
    val companyState = companyProfile?.state ?: "West Bengal"
    val customerState = selectedCustomer?.state ?: companyState
    val isIntraState = companyState.equals(customerState, ignoreCase = true)

    val cartSubtotal = cart.sumOf { it.taxableAmount }
    val discountVal = overallDiscount.toDoubleOrNull() ?: 0.0

    var totalCgst = 0.0
    var totalSgst = 0.0
    var totalIgst = 0.0

    cart.forEach { c ->
        if (isIntraState) {
            val half = c.taxableAmount * (c.item.gstRate / 2.0) / 100.0
            totalCgst += half
            totalSgst += half
        } else {
            totalIgst += c.taxableAmount * c.item.gstRate / 100.0
        }
    }

    val grandTotal = (cartSubtotal - discountVal + totalCgst + totalSgst + totalIgst).coerceAtLeast(0.0)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // POS Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "POS Fast Billing",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Customer: ${selectedCustomer?.name ?: "Cash / Walk-in Customer"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Button(
                onClick = { showCustomerDialog = true },
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (selectedCustomer == null) "Select Customer" else "Change", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar for Barcode & Items
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Scan Barcode or Search Items...") },
            leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("pos_search_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Two Section View: Catalog & Cart
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left Pane: Catalog
            Column(modifier = Modifier.weight(1.1f)) {
                Text(
                    text = "Catalog Items (${availableCatalog.size})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (availableCatalog.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.Inventory,
                        title = "No products found",
                        description = "Add items in Items Master to sell.",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(availableCatalog, key = { it.id }) { item ->
                            val inCart = cart.any { it.item.id == item.id }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 2.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = item.currentStock > 0) {
                                        viewModel.addToCart(item, 1.0)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "₹${item.sellingPrice} | Stock: ${item.currentStock}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (item.currentStock <= 0) StatusRed else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (item.currentStock > 0) {
                                        FilledTonalIconButton(
                                            onClick = { viewModel.addToCart(item, 1.0) },
                                            modifier = Modifier.size(32.dp).testTag("add_to_cart_${item.id}")
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                                        }
                                    } else {
                                        Text("Out", fontSize = 11.sp, color = StatusRed, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Right Pane: Active Cart
            Card(
                modifier = Modifier.weight(1.3f).fillMaxHeight(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Cart (${cart.sumOf { it.quantity }.toInt()})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        if (cart.isNotEmpty()) {
                            TextButton(
                                onClick = { viewModel.clearCart() },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Clear", color = StatusRed, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (cart.isEmpty()) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cart is empty.\nTap items on the left to add.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(cart, key = { it.item.id }) { c ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(c.item.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                            Text("₹${c.unitPrice} x ${c.quantity} + GST ${c.item.gstRate.toInt()}%", fontSize = 11.sp)
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { viewModel.updateCartItemQty(c.item.id, c.quantity - 1) },
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Icon(Icons.Default.Remove, contentDescription = "Minus", modifier = Modifier.size(14.dp))
                                            }
                                            Text("${c.quantity.toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            IconButton(
                                                onClick = { viewModel.updateCartItemQty(c.item.id, c.quantity + 1) },
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = "Plus", modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Billing Summary
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Subtotal", style = MaterialTheme.typography.bodySmall)
                                Text(String.format(Locale.US, "₹%.2f", cartSubtotal), style = MaterialTheme.typography.bodySmall)
                            }
                            if (isIntraState) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("CGST + SGST", style = MaterialTheme.typography.bodySmall)
                                    Text(String.format(Locale.US, "₹%.2f", totalCgst + totalSgst), style = MaterialTheme.typography.bodySmall)
                                }
                            } else {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("IGST", style = MaterialTheme.typography.bodySmall)
                                    Text(String.format(Locale.US, "₹%.2f", totalIgst), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Grand Total", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(String.format(Locale.US, "₹%.2f", grandTotal), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = EmeraldPrimaryLight)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Payment Mode Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PaymentMode.values().forEach { mode ->
                            FilterChip(
                                selected = selectedPaymentMode == mode,
                                onClick = { selectedPaymentMode = mode },
                                label = { Text(mode.displayName, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Complete Sale Button
                    Button(
                        onClick = {
                            val paid = if (selectedPaymentMode == PaymentMode.CREDIT) {
                                paidAmountInput.toDoubleOrNull() ?: 0.0
                            } else {
                                grandTotal
                            }
                            viewModel.completeSale(
                                paymentMode = selectedPaymentMode,
                                discountOverall = discountVal,
                                paidAmount = paid,
                                notes = "POS Quick Checkout"
                            ) { bill, billItems ->
                                lastCompletedBill = bill
                                lastCompletedItems = billItems
                                showInvoiceSuccessDialog = true
                            }
                        },
                        enabled = cart.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().height(46.dp).testTag("btn_complete_sale"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimaryLight)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Checkout • ₹${String.format(Locale.US, "%.0f", grandTotal)}", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Customer Selector Dialog
    if (showCustomerDialog) {
        CustomerSelectorDialog(
            customers = customers,
            selected = selectedCustomer,
            onSelect = { cust ->
                viewModel.setSelectedCustomer(cust)
                showCustomerDialog = false
            },
            onAddCustomer = { newCust ->
                viewModel.saveCustomer(newCust) {
                    viewModel.setSelectedCustomer(newCust)
                    showCustomerDialog = false
                }
            },
            onDismiss = { showCustomerDialog = false }
        )
    }

    // Invoice Success & Print/Share Dialog
    if (showInvoiceSuccessDialog && lastCompletedBill != null) {
        AlertDialog(
            onDismissRequest = { showInvoiceSuccessDialog = false },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldPrimaryLight) },
            title = { Text("Invoice Generated!") },
            text = {
                Column {
                    Text("Invoice #${lastCompletedBill!!.invoiceNumber}")
                    Text("Total: ₹${lastCompletedBill!!.grandTotal} (${lastCompletedBill!!.paymentMode.displayName})")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tax invoice PDF is ready to print, share via WhatsApp, or save.")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        PdfInvoiceGenerator.generateAndShareInvoice(
                            context = context,
                            bill = lastCompletedBill!!,
                            items = lastCompletedItems,
                            profile = companyProfile
                        )
                        showInvoiceSuccessDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimaryLight)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Print / Share PDF")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInvoiceSuccessDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
fun CustomerSelectorDialog(
    customers: List<CustomerEntity>,
    selected: CustomerEntity?,
    onSelect: (CustomerEntity?) -> Unit,
    onAddCustomer: (CustomerEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var showCreateForm by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var newGstin by remember { mutableStateOf("") }
    var newState by remember { mutableStateOf("West Bengal") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select Customer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                if (!showCreateForm) {
                    // Walk-in Option
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (selected == null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(null) }
                    ) {
                        Text("Walk-in / Cash Customer", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Medium)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                        items(customers) { cust ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected?.id == cust.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onSelect(cust) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(cust.name, fontWeight = FontWeight.Bold)
                                        if (cust.phone.isNotBlank()) Text(cust.phone, style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (cust.outstandingBalance > 0) {
                                        Text("Due: ₹${cust.outstandingBalance}", color = StatusRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showCreateForm = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ New Customer")
                    }
                } else {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Customer Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPhone,
                        onValueChange = { newPhone = it },
                        label = { Text("Phone") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newGstin,
                        onValueChange = { newGstin = it.uppercase() },
                        label = { Text("GSTIN (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newState,
                        onValueChange = { newState = it },
                        label = { Text("State") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showCreateForm = false }) { Text("Back") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            if (newName.isNotBlank()) {
                                onAddCustomer(
                                    CustomerEntity(
                                        name = newName.trim(),
                                        phone = newPhone.trim(),
                                        gstin = newGstin.trim(),
                                        state = newState.trim()
                                    )
                                )
                            }
                        }) {
                            Text("Save & Select")
                        }
                    }
                }
            }
        }
    }
}

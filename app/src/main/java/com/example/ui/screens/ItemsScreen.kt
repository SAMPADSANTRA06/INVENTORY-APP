package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ItemEntity
import com.example.ui.components.*
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusRed
import com.example.ui.viewmodel.InventoryViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemsScreen(
    viewModel: InventoryViewModel,
    modifier: Modifier = Modifier
) {
    val items by viewModel.items.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val brands by viewModel.brands.collectAsState()
    val units by viewModel.units.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var stockFilter by remember { mutableStateOf("ALL") } // ALL, LOW, OUT

    var showAddEditDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<ItemEntity?>(null) }
    var barcodeItemToShow by remember { mutableStateOf<ItemEntity?>(null) }
    var itemToDelete by remember { mutableStateOf<ItemEntity?>(null) }
    var showCsvImportDialog by remember { mutableStateOf(false) }

    // Filter items
    val filteredItems = remember(items, searchQuery, selectedCategoryFilter, stockFilter) {
        items.filter { item ->
            val matchQuery = searchQuery.isBlank() ||
                    item.name.contains(searchQuery, ignoreCase = true) ||
                    item.sku.contains(searchQuery, ignoreCase = true) ||
                    item.barcode.contains(searchQuery, ignoreCase = true) ||
                    item.brand.contains(searchQuery, ignoreCase = true)

            val matchCategory = selectedCategoryFilter == null || item.categoryName.equals(selectedCategoryFilter, ignoreCase = true)

            val matchStock = when (stockFilter) {
                "LOW" -> item.currentStock in 0.01..item.reorderLevel
                "OUT" -> item.currentStock <= 0.0
                else -> true
            }

            matchQuery && matchCategory && matchStock
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    itemToEdit = null
                    showAddEditDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_item")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Item")
            }
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Header with CSV Export & Import actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Item Master Catalog",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${filteredItems.size} items listed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { showCsvImportDialog = true },
                        modifier = Modifier.testTag("btn_csv_import")
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = "Import CSV", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(
                        onClick = {
                            val csvHeader = "Name,SKU,Barcode,Category,Brand,Unit,HSN,GST,PurchasePrice,SellingPrice,MRP,Stock,ReorderLevel\n"
                            val csvData = items.joinToString("\n") {
                                "${it.name},${it.sku},${it.barcode},${it.categoryName},${it.brand},${it.unit},${it.hsnCode},${it.gstRate},${it.purchasePrice},${it.sellingPrice},${it.mrp},${it.currentStock},${it.reorderLevel}"
                            }
                            viewModel.showToast("Exported ${items.size} items to CSV preview")
                        },
                        modifier = Modifier.testTag("btn_csv_export")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Export CSV", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("items_search_input"),
                placeholder = { Text("Search by Item Name, SKU, Barcode, Brand...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Stock Status Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = stockFilter == "ALL",
                        onClick = { stockFilter = "ALL" },
                        label = { Text("All Stock") }
                    )
                }
                item {
                    FilterChip(
                        selected = stockFilter == "LOW",
                        onClick = { stockFilter = "LOW" },
                        label = { Text("Low Stock") },
                        leadingIcon = { Icon(Icons.Default.WarningAmber, null, tint = StatusAmber, modifier = Modifier.size(16.dp)) }
                    )
                }
                item {
                    FilterChip(
                        selected = stockFilter == "OUT",
                        onClick = { stockFilter = "OUT" },
                        label = { Text("Out of Stock") },
                        leadingIcon = { Icon(Icons.Default.ErrorOutline, null, tint = StatusRed, modifier = Modifier.size(16.dp)) }
                    )
                }

                // Category Chips
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategoryFilter == cat.name,
                        onClick = {
                            selectedCategoryFilter = if (selectedCategoryFilter == cat.name) null else cat.name
                        },
                        label = { Text(cat.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Items List
            if (filteredItems.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Inventory2,
                    title = if (items.isEmpty()) "No items created yet" else "No matching items",
                    description = if (items.isEmpty()) "Add products with SKU, Barcode, GST rates, and purchase/selling prices." else "Try adjusting your search query or filter tags.",
                    actionButtonText = if (items.isEmpty()) "Add First Item" else null,
                    onActionClick = {
                        itemToEdit = null
                        showAddEditDialog = true
                    }
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        ItemCard(
                            item = item,
                            onEdit = {
                                itemToEdit = item
                                showAddEditDialog = true
                            },
                            onDelete = { itemToDelete = item },
                            onShowBarcode = { barcodeItemToShow = item }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Item Dialog
    if (showAddEditDialog) {
        AddEditItemDialog(
            item = itemToEdit,
            totalItemsExisting = items.size,
            categories = categories.map { it.name },
            brands = brands.map { it.name },
            units = units.map { it.name }.ifEmpty { listOf("PCS", "KG", "LTR", "BOX", "MTR") },
            onDismiss = { showAddEditDialog = false },
            onSave = { savedItem ->
                viewModel.saveItem(savedItem) {
                    showAddEditDialog = false
                }
            }
        )
    }

    // Barcode / QR Preview Dialog
    if (barcodeItemToShow != null) {
        BarcodePreviewDialog(
            item = barcodeItemToShow!!,
            onDismiss = { barcodeItemToShow = null }
        )
    }

    // Delete Confirmation Dialog
    if (itemToDelete != null) {
        ConfirmDeleteDialog(
            title = "Delete Item?",
            message = "Are you sure you want to delete '${itemToDelete!!.name}' (SKU: ${itemToDelete!!.sku})? This action cannot be undone.",
            onConfirm = {
                viewModel.deleteItem(itemToDelete!!.id)
                itemToDelete = null
            },
            onDismiss = { itemToDelete = null }
        )
    }

    // Simple CSV Import Dialog
    if (showCsvImportDialog) {
        CsvImportDialog(
            onDismiss = { showCsvImportDialog = false },
            onImport = { lines ->
                var importedCount = 0
                for (line in lines) {
                    val cols = line.split(",").map { it.trim() }
                    if (cols.size >= 4 && cols[0].isNotBlank()) {
                        val name = cols[0]
                        val sku = if (cols.size > 1 && cols[1].isNotBlank()) cols[1] else "SS-${System.currentTimeMillis() % 10000}"
                        val barcode = if (cols.size > 2 && cols[2].isNotBlank()) cols[2] else "890" + (System.currentTimeMillis() % 1000000000L)
                        val cat = if (cols.size > 3) cols[3] else "General"
                        val purchase = if (cols.size > 8) cols[8].toDoubleOrNull() ?: 0.0 else 0.0
                        val sell = if (cols.size > 9) cols[9].toDoubleOrNull() ?: 0.0 else 0.0
                        val stock = if (cols.size > 11) cols[11].toDoubleOrNull() ?: 0.0 else 0.0

                        viewModel.saveItem(
                            ItemEntity(
                                name = name,
                                sku = sku,
                                barcode = barcode,
                                categoryId = 0,
                                categoryName = cat,
                                purchasePrice = purchase,
                                sellingPrice = sell,
                                mrp = sell * 1.1,
                                openingStock = stock,
                                currentStock = stock
                            )
                        )
                        importedCount++
                    }
                }
                viewModel.showToast("Imported $importedCount items")
                showCsvImportDialog = false
            }
        )
    }
}

@Composable
fun ItemCard(
    item: ItemEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShowBarcode: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "SKU: ${item.sku} | Barcode: ${item.barcode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (item.categoryName.isNotBlank() || item.brand.isNotBlank()) {
                        Text(
                            text = listOf(item.brand, item.categoryName).filter { it.isNotBlank() }.joinToString(" • "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Stock Badge
                val (stockBg, stockText) = when {
                    item.currentStock <= 0 -> Pair(StatusRed.copy(alpha = 0.15f), StatusRed)
                    item.currentStock <= item.reorderLevel -> Pair(StatusAmber.copy(alpha = 0.15f), StatusAmber)
                    else -> Pair(EmeraldPrimaryLight.copy(alpha = 0.15f), EmeraldPrimaryLight)
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = stockBg,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = "${item.currentStock} ${item.unit}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = stockText,
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
                        Text("Sale Price", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(String.format(Locale.US, "₹%.2f", item.sellingPrice), fontWeight = FontWeight.Bold, color = EmeraldPrimaryLight)
                    }
                    Column {
                        Text("Cost", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(String.format(Locale.US, "₹%.2f", item.purchasePrice), fontWeight = FontWeight.Medium)
                    }
                    Column {
                        Text("GST", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${item.gstRate.toInt()}%", fontWeight = FontWeight.Medium)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onShowBarcode, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.QrCode, contentDescription = "View Barcode", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditItemDialog(
    item: ItemEntity?,
    totalItemsExisting: Int,
    categories: List<String>,
    brands: List<String>,
    units: List<String>,
    onDismiss: () -> Unit,
    onSave: (ItemEntity) -> Unit
) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var sku by remember { mutableStateOf(item?.sku ?: String.format(Locale.US, "SS-ITM-%04d", totalItemsExisting + 1)) }
    var barcode by remember { mutableStateOf(item?.barcode ?: "890${System.currentTimeMillis() % 1000000000L}") }
    var categoryName by remember { mutableStateOf(item?.categoryName ?: (categories.firstOrNull() ?: "General")) }
    var brand by remember { mutableStateOf(item?.brand ?: "") }
    var unit by remember { mutableStateOf(item?.unit ?: "PCS") }
    var hsnCode by remember { mutableStateOf(item?.hsnCode ?: "") }
    var gstRate by remember { mutableStateOf((item?.gstRate ?: 18.0).toString()) }
    var purchasePrice by remember { mutableStateOf(if (item != null) item.purchasePrice.toString() else "0") }
    var sellingPrice by remember { mutableStateOf(if (item != null) item.sellingPrice.toString() else "0") }
    var mrp by remember { mutableStateOf(if (item != null) item.mrp.toString() else "0") }
    var openingStock by remember { mutableStateOf(if (item != null) item.openingStock.toString() else "0") }
    var reorderLevel by remember { mutableStateOf(if (item != null) item.reorderLevel.toString() else "5") }
    var batchNo by remember { mutableStateOf(item?.batchNo ?: "") }
    var expiryDate by remember { mutableStateOf(item?.expiryDate ?: "") }

    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Text(
                    text = if (item == null) "Add New Item" else "Edit Item",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Item Name *") },
                        modifier = Modifier.fillMaxWidth().testTag("item_input_name"),
                        singleLine = true
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = sku,
                            onValueChange = { sku = it },
                            label = { Text("SKU *") },
                            modifier = Modifier.weight(1f).testTag("item_input_sku"),
                            singleLine = true
                        )
                        IconButton(
                            onClick = { sku = String.format(Locale.US, "SS-%05d", (1000..9999).random()) },
                            modifier = Modifier.align(Alignment.CenterVertically)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Auto SKU")
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = barcode,
                            onValueChange = { barcode = it },
                            label = { Text("Barcode / EAN *") },
                            modifier = Modifier.weight(1f).testTag("item_input_barcode"),
                            singleLine = true
                        )
                        IconButton(
                            onClick = { barcode = "890" + (100000000L..999999999L).random() },
                            modifier = Modifier.align(Alignment.CenterVertically)
                        ) {
                            Icon(Icons.Default.QrCode, contentDescription = "Generate Barcode")
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = categoryName,
                            onValueChange = { categoryName = it },
                            label = { Text("Category") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = brand,
                            onValueChange = { brand = it },
                            label = { Text("Brand") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it.uppercase() },
                            label = { Text("Unit (PCS, KG...)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = hsnCode,
                            onValueChange = { hsnCode = it },
                            label = { Text("HSN Code") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = purchasePrice,
                            onValueChange = { purchasePrice = it },
                            label = { Text("Purchase Price (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = sellingPrice,
                            onValueChange = { sellingPrice = it },
                            label = { Text("Selling Price (₹) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f).testTag("item_input_sell_price"),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = mrp,
                            onValueChange = { mrp = it },
                            label = { Text("MRP (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = gstRate,
                            onValueChange = { gstRate = it },
                            label = { Text("GST Rate (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    if (item == null) {
                        OutlinedTextField(
                            value = openingStock,
                            onValueChange = { openingStock = it },
                            label = { Text("Opening Stock (Qty)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = reorderLevel,
                            onValueChange = { reorderLevel = it },
                            label = { Text("Reorder Alert Level") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = batchNo,
                            onValueChange = { batchNo = it },
                            label = { Text("Batch No.") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = expiryDate,
                        onValueChange = { expiryDate = it },
                        label = { Text("Expiry Date (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                errorMsg = "Item name is required"
                                return@Button
                            }
                            val sell = sellingPrice.toDoubleOrNull() ?: 0.0
                            val purchase = purchasePrice.toDoubleOrNull() ?: 0.0
                            val gst = gstRate.toDoubleOrNull() ?: 18.0
                            val mrpVal = mrp.toDoubleOrNull() ?: sell
                            val reorder = reorderLevel.toDoubleOrNull() ?: 5.0
                            val opening = openingStock.toDoubleOrNull() ?: 0.0

                            val updated = (item ?: ItemEntity(
                                name = name.trim(),
                                sku = sku.trim(),
                                barcode = barcode.trim(),
                                categoryId = 0,
                                categoryName = categoryName.trim(),
                                brand = brand.trim(),
                                unit = unit.trim().uppercase(),
                                hsnCode = hsnCode.trim(),
                                gstRate = gst,
                                purchasePrice = purchase,
                                sellingPrice = sell,
                                mrp = mrpVal,
                                openingStock = opening,
                                currentStock = opening,
                                reorderLevel = reorder,
                                batchNo = batchNo.trim(),
                                expiryDate = expiryDate.trim()
                            )).copy(
                                name = name.trim(),
                                sku = sku.trim(),
                                barcode = barcode.trim(),
                                categoryName = categoryName.trim(),
                                brand = brand.trim(),
                                unit = unit.trim().uppercase(),
                                hsnCode = hsnCode.trim(),
                                gstRate = gst,
                                purchasePrice = purchase,
                                sellingPrice = sell,
                                mrp = mrpVal,
                                reorderLevel = reorder,
                                batchNo = batchNo.trim(),
                                expiryDate = expiryDate.trim()
                            )

                            onSave(updated)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_save_item")
                    ) {
                        Text("Save Item")
                    }
                }
            }
        }
    }
}

@Composable
fun BarcodePreviewDialog(
    item: ItemEntity,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "SKU: ${item.sku} | MRP: ₹${item.mrp}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text("1D Barcode (Code128)", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(6.dp))
                Barcode1DView(
                    code = item.barcode,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text("2D QR Matrix", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(6.dp))
                QrCodeView(
                    data = "SKU:${item.sku}|EAN:${item.barcode}|PRICE:${item.sellingPrice}"
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun CsvImportDialog(
    onDismiss: () -> Unit,
    onImport: (List<String>) -> Unit
) {
    var csvText by remember {
        mutableStateOf(
            "Cotton T-Shirt,SS-001,8901234567,Apparel,Polo,PCS,6109,12,350,699,799,50,10\n" +
            "Leather Wallet,SS-002,8901234568,Accessories,Wildhorn,PCS,4202,18,250,550,650,30,5\n" +
            "Ceramic Mug,SS-003,8901234569,Home,ClayCraft,PCS,6911,18,80,180,200,80,15"
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Import Items from CSV", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Format: Name, SKU, Barcode, Category, Brand, Unit, HSN, GST, PurchasePrice, SellingPrice, MRP, Stock, Reorder",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = csvText,
                    onValueChange = { csvText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    maxLines = 8
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        val lines = csvText.lines().filter { it.isNotBlank() }
                        onImport(lines)
                    }) {
                        Text("Import Lines")
                    }
                }
            }
        }
    }
}

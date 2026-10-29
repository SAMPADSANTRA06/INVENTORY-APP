package com.example.data.repository

import com.example.data.dao.InventoryDao
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class InventoryRepository(private val dao: InventoryDao) {

    // --- Flows ---
    val users: Flow<List<UserEntity>> = dao.getAllUsers()
    val userCount: Flow<Int> = dao.getUserCount()
    val categories: Flow<List<CategoryEntity>> = dao.getAllCategories()
    val brands: Flow<List<BrandEntity>> = dao.getAllBrands()
    val units: Flow<List<UnitEntity>> = dao.getAllUnits()
    val godowns: Flow<List<GodownEntity>> = dao.getAllGodowns()
    val items: Flow<List<ItemEntity>> = dao.getAllItems()
    val ledgerEntries: Flow<List<StockLedgerEntity>> = dao.getAllLedgerEntries()
    val suppliers: Flow<List<SupplierEntity>> = dao.getAllSuppliers()
    val customers: Flow<List<CustomerEntity>> = dao.getAllCustomers()
    val purchaseBills: Flow<List<PurchaseBillEntity>> = dao.getAllPurchaseBills()
    val salesBills: Flow<List<SalesBillEntity>> = dao.getAllSalesBills()
    val companyProfile: Flow<CompanyProfileEntity?> = dao.getCompanyProfile()
    val auditLogs: Flow<List<AuditLogEntity>> = dao.getAllAuditLogs()

    suspend fun hasUsers(): Boolean = withContext(Dispatchers.IO) {
        dao.getUserCountSync() > 0
    }

    suspend fun getUserByPin(pin: String): UserEntity? = withContext(Dispatchers.IO) {
        dao.getUserByPin(pin)
    }

    suspend fun insertUser(user: UserEntity): Long = withContext(Dispatchers.IO) {
        dao.insertUser(user)
    }

    suspend fun deleteUser(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteUser(id)
    }

    // --- Masters ---
    suspend fun insertCategory(name: String) = withContext(Dispatchers.IO) {
        dao.insertCategory(CategoryEntity(name = name.trim()))
    }

    suspend fun deleteCategory(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteCategory(id)
    }

    suspend fun insertBrand(name: String) = withContext(Dispatchers.IO) {
        dao.insertBrand(BrandEntity(name = name.trim()))
    }

    suspend fun deleteBrand(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteBrand(id)
    }

    suspend fun insertUnit(name: String) = withContext(Dispatchers.IO) {
        dao.insertUnit(UnitEntity(name = name.trim().uppercase()))
    }

    suspend fun deleteUnit(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteUnit(id)
    }

    suspend fun insertGodown(name: String, location: String = "") = withContext(Dispatchers.IO) {
        dao.insertGodown(GodownEntity(name = name.trim(), location = location.trim()))
    }

    suspend fun deleteGodown(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteGodown(id)
    }

    // --- Items ---
    suspend fun saveItem(item: ItemEntity, performedBy: String = "Admin"): Long = withContext(Dispatchers.IO) {
        if (item.id == 0L) {
            val insertedId = dao.insertItem(item)
            // If opening stock > 0, record initial opening stock in ledger
            if (item.openingStock > 0) {
                dao.updateItemStock(insertedId, item.openingStock)
                dao.insertLedgerEntry(
                    StockLedgerEntity(
                        itemId = insertedId,
                        itemName = item.name,
                        movementType = MovementType.ADJUSTMENT_ADD,
                        quantity = item.openingStock,
                        unitCost = item.purchasePrice,
                        balanceAfter = item.openingStock,
                        referenceDoc = "OPENING-STOCK",
                        notes = "Initial opening stock recorded",
                        performedBy = performedBy
                    )
                )
            }
            dao.insertAuditLog(
                AuditLogEntity(
                    action = "ADD_ITEM",
                    details = "Created item '${item.name}' (SKU: ${item.sku})",
                    userRole = performedBy
                )
            )
            insertedId
        } else {
            dao.updateItem(item)
            dao.insertAuditLog(
                AuditLogEntity(
                    action = "UPDATE_ITEM",
                    details = "Updated item '${item.name}' (ID: ${item.id})",
                    userRole = performedBy
                )
            )
            item.id
        }
    }

    suspend fun deleteItem(id: Long, performedBy: String = "Admin") = withContext(Dispatchers.IO) {
        val item = dao.getItemById(id)
        dao.deleteItem(id)
        if (item != null) {
            dao.insertAuditLog(
                AuditLogEntity(
                    action = "DELETE_ITEM",
                    details = "Deleted item '${item.name}' (SKU: ${item.sku})",
                    userRole = performedBy
                )
            )
        }
    }

    suspend fun getItemByBarcodeOrSku(code: String): ItemEntity? = withContext(Dispatchers.IO) {
        dao.getItemByBarcodeOrSku(code.trim())
    }

    // --- Stock Movements & Negative Stock Prevention ---
    suspend fun recordMovement(
        itemId: Long,
        godownId: Long = 1,
        godownName: String = "Main Godown",
        movementType: MovementType,
        quantity: Double,
        unitCost: Double,
        referenceDoc: String = "",
        notes: String = "",
        performedBy: String = "Admin"
    ): Result<Double> = withContext(Dispatchers.IO) {
        val item = dao.getItemById(itemId)
            ?: return@withContext Result.failure(IllegalArgumentException("Item not found"))

        if (quantity <= 0) {
            return@withContext Result.failure(IllegalArgumentException("Quantity must be greater than zero"))
        }

        val newStock = if (movementType.isPositive) {
            item.currentStock + quantity
        } else {
            if (item.currentStock < quantity) {
                return@withContext Result.failure(
                    IllegalStateException(
                        "Cannot perform movement! Negative stock prevented. Available: ${item.currentStock}, Requested: $quantity"
                    )
                )
            }
            item.currentStock - quantity
        }

        dao.updateItemStock(itemId, newStock)
        dao.insertLedgerEntry(
            StockLedgerEntity(
                itemId = itemId,
                itemName = item.name,
                godownId = godownId,
                godownName = godownName,
                movementType = movementType,
                quantity = quantity,
                unitCost = unitCost,
                balanceAfter = newStock,
                referenceDoc = referenceDoc,
                notes = notes,
                performedBy = performedBy
            )
        )

        dao.insertAuditLog(
            AuditLogEntity(
                action = "STOCK_MOVEMENT",
                details = "${movementType.displayName}: ${item.name} (${if (movementType.isPositive) "+" else "-"}$quantity) -> Balance: $newStock",
                userRole = performedBy
            )
        )

        Result.success(newStock)
    }

    // --- Purchases ---
    suspend fun saveSupplier(supplier: SupplierEntity) = withContext(Dispatchers.IO) {
        if (supplier.id == 0L) {
            dao.insertSupplier(supplier)
        } else {
            dao.updateSupplier(supplier)
        }
    }

    suspend fun deleteSupplier(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteSupplier(id)
    }

    suspend fun createPurchaseBill(
        bill: PurchaseBillEntity,
        items: List<PurchaseBillItemEntity>,
        performedBy: String = "Admin"
    ): Long = withContext(Dispatchers.IO) {
        val billId = dao.insertPurchaseBill(bill)
        val itemsWithBillId = items.map { it.copy(billId = billId) }
        dao.insertPurchaseItems(itemsWithBillId)

        // Add to stock for each item
        for (line in itemsWithBillId) {
            val item = dao.getItemById(line.itemId)
            if (item != null) {
                val newStock = item.currentStock + line.quantity
                // Update purchase price to latest
                dao.updateItem(item.copy(currentStock = newStock, purchasePrice = line.unitPrice))
                dao.insertLedgerEntry(
                    StockLedgerEntity(
                        itemId = item.id,
                        itemName = item.name,
                        movementType = MovementType.PURCHASE,
                        quantity = line.quantity,
                        unitCost = line.unitPrice,
                        balanceAfter = newStock,
                        referenceDoc = bill.billNumber,
                        notes = "Purchase from ${bill.supplierName}",
                        performedBy = performedBy
                    )
                )
            }
        }

        // Adjust supplier outstanding if due
        if (bill.dueAmount > 0) {
            dao.updateSupplierBalance(bill.supplierId, bill.dueAmount)
        }

        dao.insertAuditLog(
            AuditLogEntity(
                action = "PURCHASE_BILL",
                details = "Bill #${bill.billNumber} from ${bill.supplierName} - Total: ₹${bill.grandTotal}",
                userRole = performedBy
            )
        )

        billId
    }

    // --- Customers ---
    suspend fun saveCustomer(customer: CustomerEntity) = withContext(Dispatchers.IO) {
        if (customer.id == 0L) {
            dao.insertCustomer(customer)
        } else {
            dao.updateCustomer(customer)
        }
    }

    suspend fun deleteCustomer(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteCustomer(id)
    }

    suspend fun collectCustomerPayment(customerId: Long, amount: Double, notes: String, performedBy: String) = withContext(Dispatchers.IO) {
        dao.updateCustomerOutstanding(customerId, -amount)
        dao.insertAuditLog(
            AuditLogEntity(
                action = "CUSTOMER_PAYMENT",
                details = "Collected ₹$amount from Customer ID $customerId. Notes: $notes",
                userRole = performedBy
            )
        )
    }

    // --- Sales / POS Billing ---
    suspend fun createSalesBill(
        bill: SalesBillEntity,
        items: List<SalesBillItemEntity>,
        performedBy: String = "Staff"
    ): Result<Long> = withContext(Dispatchers.IO) {
        // Step 1: Pre-validate stock for every item to prevent partial commits
        for (line in items) {
            val item = dao.getItemById(line.itemId)
                ?: return@withContext Result.failure(IllegalArgumentException("Item '${line.itemName}' not found"))
            if (item.currentStock < line.quantity) {
                return@withContext Result.failure(
                    IllegalStateException(
                        "Cannot complete sale: Insufficient stock for '${item.name}'! In stock: ${item.currentStock}, Requested: ${line.quantity}"
                    )
                )
            }
        }

        // Step 2: Insert Sales Bill & items
        val billId = dao.insertSalesBill(bill)
        val itemsWithBillId = items.map { it.copy(billId = billId) }
        dao.insertSalesItems(itemsWithBillId)

        // Step 3: Deduct stock and write to Stock Ledger
        for (line in itemsWithBillId) {
            val item = dao.getItemById(line.itemId)!!
            val newStock = item.currentStock - line.quantity
            dao.updateItemStock(item.id, newStock)
            dao.insertLedgerEntry(
                StockLedgerEntity(
                    itemId = item.id,
                    itemName = item.name,
                    movementType = MovementType.SALE,
                    quantity = line.quantity,
                    unitCost = item.purchasePrice,
                    balanceAfter = newStock,
                    referenceDoc = bill.invoiceNumber,
                    notes = "Sale to ${bill.customerName}",
                    performedBy = performedBy
                )
            )
        }

        // Step 4: If credit sale with due amount, update customer outstanding
        if (bill.dueAmount > 0 && bill.customerId != null) {
            dao.updateCustomerOutstanding(bill.customerId, bill.dueAmount)
        }

        // Step 5: Update next invoice sequence in company profile
        val profile = dao.getCompanyProfileSync()
        if (profile != null) {
            dao.saveCompanyProfile(profile.copy(nextInvoiceSeq = profile.nextInvoiceSeq + 1))
        }

        dao.insertAuditLog(
            AuditLogEntity(
                action = "SALES_INVOICE",
                details = "Invoice #${bill.invoiceNumber} for ${bill.customerName} - Total: ₹${bill.grandTotal} (${bill.paymentMode.displayName})",
                userRole = performedBy
            )
        )

        Result.success(billId)
    }

    suspend fun getSalesItemsForBillSync(billId: Long): List<SalesBillItemEntity> = withContext(Dispatchers.IO) {
        dao.getSalesItemsForBillSync(billId)
    }

    suspend fun getPurchaseItemsForBillSync(billId: Long): List<PurchaseBillItemEntity> = withContext(Dispatchers.IO) {
        dao.getPurchaseItemsForBillSync(billId)
    }

    // --- Company Profile ---
    suspend fun saveCompanyProfile(profile: CompanyProfileEntity) = withContext(Dispatchers.IO) {
        dao.saveCompanyProfile(profile)
    }

    suspend fun getCompanyProfileSync(): CompanyProfileEntity? = withContext(Dispatchers.IO) {
        dao.getCompanyProfileSync()
    }

    // --- Stock Valuation Calculations ---
    fun calculateStockValuation(
        items: List<ItemEntity>,
        ledger: List<StockLedgerEntity>,
        method: ValuationMethod
    ): Pair<Double, Double> {
        var totalCost = 0.0
        var totalRetail = 0.0

        for (item in items) {
            totalRetail += item.currentStock * item.sellingPrice
            if (method == ValuationMethod.WEIGHTED_AVERAGE) {
                totalCost += item.currentStock * item.purchasePrice
            } else {
                // FIFO calculation: trace from oldest purchases
                val purchases = ledger.filter {
                    it.itemId == item.id && (it.movementType == MovementType.PURCHASE || it.movementType == MovementType.ADJUSTMENT_ADD)
                }.sortedBy { it.timestamp }

                var remainingStockToValue = item.currentStock
                var itemCost = 0.0
                for (batch in purchases.reversed()) { // recent batches for remaining stock
                    if (remainingStockToValue <= 0) break
                    val qtyFromBatch = minOf(remainingStockToValue, batch.quantity)
                    itemCost += qtyFromBatch * batch.unitCost
                    remainingStockToValue -= qtyFromBatch
                }
                if (remainingStockToValue > 0) {
                    itemCost += remainingStockToValue * item.purchasePrice
                }
                totalCost += itemCost
            }
        }
        return Pair(totalCost, totalRetail)
    }

    // --- JSON Backup & Restore ---
    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        val profile = dao.getCompanyProfileSync()
        if (profile != null) {
            val profObj = JSONObject().apply {
                put("companyName", profile.companyName)
                put("gstin", profile.gstin)
                put("address", profile.address)
                put("state", profile.state)
                put("phone", profile.phone)
                put("email", profile.email)
                put("invoicePrefix", profile.invoicePrefix)
                put("nextInvoiceSeq", profile.nextInvoiceSeq)
                put("termsConditions", profile.termsConditions)
            }
            root.put("profile", profObj)
        }
        root.put("exportedAt", System.currentTimeMillis())
        root.toString(2)
    }

    suspend fun clearAllData(performedBy: String) = withContext(Dispatchers.IO) {
        dao.clearItems()
        dao.clearStockLedger()
        dao.clearPurchaseBills()
        dao.clearPurchaseItems()
        dao.clearSalesBills()
        dao.clearSalesItems()
        dao.clearSuppliers()
        dao.clearCustomers()
        dao.clearCategories()
        dao.clearBrands()
        dao.clearUnits()
        dao.clearGodowns()

        dao.insertAuditLog(
            AuditLogEntity(
                action = "SYSTEM_WIPE",
                details = "Wiped all inventory data and transaction records",
                userRole = performedBy
            )
        )
    }
}

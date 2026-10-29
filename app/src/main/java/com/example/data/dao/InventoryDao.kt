package com.example.data.dao

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {

    // --- USERS ---
    @Query("SELECT COUNT(*) FROM users")
    fun getUserCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCountSync(): Int

    @Query("SELECT * FROM users ORDER BY createdAt ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE pin = :pin LIMIT 1")
    suspend fun getUserByPin(pin: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUser(id: Long)

    // --- MASTERS: CATEGORIES, BRANDS, UNITS, GODOWNS ---
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategory(id: Long)

    @Query("SELECT * FROM brands ORDER BY name ASC")
    fun getAllBrands(): Flow<List<BrandEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBrand(brand: BrandEntity): Long

    @Query("DELETE FROM brands WHERE id = :id")
    suspend fun deleteBrand(id: Long)

    @Query("SELECT * FROM units ORDER BY name ASC")
    fun getAllUnits(): Flow<List<UnitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnit(unit: UnitEntity): Long

    @Query("DELETE FROM units WHERE id = :id")
    suspend fun deleteUnit(id: Long)

    @Query("SELECT * FROM godowns ORDER BY name ASC")
    fun getAllGodowns(): Flow<List<GodownEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGodown(godown: GodownEntity): Long

    @Query("DELETE FROM godowns WHERE id = :id")
    suspend fun deleteGodown(id: Long)

    // --- ITEMS ---
    @Query("SELECT * FROM items ORDER BY name ASC")
    fun getAllItems(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): ItemEntity?

    @Query("SELECT * FROM items WHERE barcode = :code OR sku = :code LIMIT 1")
    suspend fun getItemByBarcodeOrSku(code: String): ItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ItemEntity): Long

    @Update
    suspend fun updateItem(item: ItemEntity)

    @Query("UPDATE items SET currentStock = :newStock WHERE id = :id")
    suspend fun updateItemStock(id: Long, newStock: Double)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteItem(id: Long)

    @Query("SELECT COUNT(*) FROM items")
    fun getItemsCount(): Flow<Int>

    // --- STOCK LEDGER ---
    @Query("SELECT * FROM stock_ledger ORDER BY timestamp DESC")
    fun getAllLedgerEntries(): Flow<List<StockLedgerEntity>>

    @Query("SELECT * FROM stock_ledger WHERE itemId = :itemId ORDER BY timestamp DESC")
    fun getLedgerForItem(itemId: Long): Flow<List<StockLedgerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgerEntry(entry: StockLedgerEntity): Long

    // --- SUPPLIERS ---
    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    fun getAllSuppliers(): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM suppliers WHERE id = :id LIMIT 1")
    suspend fun getSupplierById(id: Long): SupplierEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: SupplierEntity): Long

    @Update
    suspend fun updateSupplier(supplier: SupplierEntity)

    @Query("UPDATE suppliers SET balanceDue = balanceDue + :delta WHERE id = :id")
    suspend fun updateSupplierBalance(id: Long, delta: Double)

    @Query("DELETE FROM suppliers WHERE id = :id")
    suspend fun deleteSupplier(id: Long)

    // --- CUSTOMERS ---
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: Long): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("UPDATE customers SET outstandingBalance = outstandingBalance + :delta WHERE id = :id")
    suspend fun updateCustomerOutstanding(id: Long, delta: Double)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomer(id: Long)

    // --- PURCHASE BILLS ---
    @Query("SELECT * FROM purchase_bills ORDER BY date DESC")
    fun getAllPurchaseBills(): Flow<List<PurchaseBillEntity>>

    @Query("SELECT * FROM purchase_bills WHERE id = :id LIMIT 1")
    suspend fun getPurchaseBillById(id: Long): PurchaseBillEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseBill(bill: PurchaseBillEntity): Long

    @Update
    suspend fun updatePurchaseBill(bill: PurchaseBillEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseItems(items: List<PurchaseBillItemEntity>)

    @Query("SELECT * FROM purchase_items WHERE billId = :billId")
    fun getPurchaseItemsForBill(billId: Long): Flow<List<PurchaseBillItemEntity>>

    @Query("SELECT * FROM purchase_items WHERE billId = :billId")
    suspend fun getPurchaseItemsForBillSync(billId: Long): List<PurchaseBillItemEntity>

    // --- SALES BILLS ---
    @Query("SELECT * FROM sales_bills ORDER BY date DESC")
    fun getAllSalesBills(): Flow<List<SalesBillEntity>>

    @Query("SELECT * FROM sales_bills WHERE id = :id LIMIT 1")
    suspend fun getSalesBillById(id: Long): SalesBillEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSalesBill(bill: SalesBillEntity): Long

    @Update
    suspend fun updateSalesBill(bill: SalesBillEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSalesItems(items: List<SalesBillItemEntity>)

    @Query("SELECT * FROM sales_items WHERE billId = :billId")
    fun getSalesItemsForBill(billId: Long): Flow<List<SalesBillItemEntity>>

    @Query("SELECT * FROM sales_items WHERE billId = :billId")
    suspend fun getSalesItemsForBillSync(billId: Long): List<SalesBillItemEntity>

    // --- COMPANY PROFILE ---
    @Query("SELECT * FROM company_profile WHERE id = 1 LIMIT 1")
    fun getCompanyProfile(): Flow<CompanyProfileEntity?>

    @Query("SELECT * FROM company_profile WHERE id = 1 LIMIT 1")
    suspend fun getCompanyProfileSync(): CompanyProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCompanyProfile(profile: CompanyProfileEntity)

    // --- AUDIT LOGS ---
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 300")
    fun getAllAuditLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity): Long

    // --- BULK BACKUP / WIPE QUERIES ---
    @Query("DELETE FROM items")
    suspend fun clearItems()

    @Query("DELETE FROM stock_ledger")
    suspend fun clearStockLedger()

    @Query("DELETE FROM categories")
    suspend fun clearCategories()

    @Query("DELETE FROM brands")
    suspend fun clearBrands()

    @Query("DELETE FROM units")
    suspend fun clearUnits()

    @Query("DELETE FROM godowns")
    suspend fun clearGodowns()

    @Query("DELETE FROM suppliers")
    suspend fun clearSuppliers()

    @Query("DELETE FROM customers")
    suspend fun clearCustomers()

    @Query("DELETE FROM purchase_bills")
    suspend fun clearPurchaseBills()

    @Query("DELETE FROM purchase_items")
    suspend fun clearPurchaseItems()

    @Query("DELETE FROM sales_bills")
    suspend fun clearSalesBills()

    @Query("DELETE FROM sales_items")
    suspend fun clearSalesItems()
}

package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.*
import com.example.data.repository.InventoryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class Screen(val title: String) {
    object Dashboard : Screen("Dashboard")
    object Items : Screen("Items & Barcodes")
    object Stock : Screen("Stock Movements")
    object Purchases : Screen("Purchases & Suppliers")
    object Sales : Screen("POS Billing")
    object Reports : Screen("Financial Reports")
    object Settings : Screen("Settings & Masters")
}

data class CartItem(
    val item: ItemEntity,
    var quantity: Double = 1.0,
    var unitPrice: Double = item.sellingPrice,
    var discountPercent: Double = 0.0
) {
    val taxableAmount: Double
        get() {
            val gross = quantity * unitPrice
            return (gross - (gross * discountPercent / 100.0)).coerceAtLeast(0.0)
        }

    val gstAmount: Double
        get() = (taxableAmount * item.gstRate / 100.0)

    val totalAmount: Double
        get() = taxableAmount + gstAmount
}

class InventoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: InventoryRepository

    init {
        val db = AppDatabase.getInstance(application)
        repository = InventoryRepository(db.inventoryDao())
        viewModelScope.launch {
            if (!repository.hasUsers()) {
                val admin = UserEntity(name = "Sampad Santra (Admin)", pin = "1234", role = UserRole.ADMIN)
                val manager = UserEntity(name = "Store Manager", pin = "2345", role = UserRole.MANAGER)
                val staff = UserEntity(name = "Sales Staff", pin = "3456", role = UserRole.STAFF)
                repository.insertUser(admin)
                repository.insertUser(manager)
                repository.insertUser(staff)
                repository.saveCompanyProfile(
                    CompanyProfileEntity(
                        companyName = "Sampad Santra",
                        gstin = "",
                        address = "Store Location",
                        state = "West Bengal",
                        phone = "",
                        invoicePrefix = "SS-INV-",
                        nextInvoiceSeq = 1
                    )
                )
                repository.insertGodown("Main Godown", "Central Warehouse")
                _currentUser.value = repository.getUserByPin("1234")
            } else {
                val defaultAdmin = repository.getUserByPin("1234")
                if (defaultAdmin != null) {
                    _currentUser.value = defaultAdmin
                }
            }
        }
    }

    // --- State Streams ---
    val users = repository.users.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val userCount = repository.userCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), -1)
    val items = repository.items.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val categories = repository.categories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val brands = repository.brands.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val units = repository.units.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val godowns = repository.godowns.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val suppliers = repository.suppliers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val customers = repository.customers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val purchaseBills = repository.purchaseBills.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val salesBills = repository.salesBills.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val ledgerEntries = repository.ledgerEntries.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val companyProfile = repository.companyProfile.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val auditLogs = repository.auditLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Current Session & Navigation ---
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Dashboard)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    // --- POS Cart State ---
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    private val _selectedCustomer = MutableStateFlow<CustomerEntity?>(null)
    val selectedCustomer: StateFlow<CustomerEntity?> = _selectedCustomer.asStateFlow()

    // --- Valuation Mode ---
    private val _valuationMethod = MutableStateFlow(ValuationMethod.WEIGHTED_AVERAGE)
    val valuationMethod: StateFlow<ValuationMethod> = _valuationMethod.asStateFlow()

    fun setValuationMethod(method: ValuationMethod) {
        _valuationMethod.value = method
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun showToast(msg: String) {
        viewModelScope.launch { _toastMessage.emit(msg) }
    }

    // --- Auth & Onboarding ---
    fun loginWithPin(pin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val user = repository.getUserByPin(pin)
            if (user != null) {
                _currentUser.value = user
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun logout() {
        _currentUser.value = null
    }

    fun completeFirstLaunchSetup(
        companyName: String,
        adminName: String,
        adminPin: String,
        gstin: String,
        address: String,
        state: String,
        phone: String
    ) {
        viewModelScope.launch {
            // 1. Create Master Admin
            repository.insertUser(
                UserEntity(
                    name = adminName.ifBlank { "Sampad Santra" },
                    pin = adminPin,
                    role = UserRole.ADMIN
                )
            )

            // 2. Setup Company Profile
            repository.saveCompanyProfile(
                CompanyProfileEntity(
                    companyName = companyName.ifBlank { "Sampad Santra" },
                    gstin = gstin,
                    address = address,
                    state = state.ifBlank { "West Bengal" },
                    phone = phone,
                    invoicePrefix = "SS-INV-",
                    nextInvoiceSeq = 1
                )
            )

            // 3. Setup Initial Godown ("Main Godown")
            repository.insertGodown("Main Godown", address)

            // 4. Auto-login as Admin
            val admin = repository.getUserByPin(adminPin)
            _currentUser.value = admin
            showToast("Welcome to Sampad Santra Inventory!")
        }
    }

    fun createUser(name: String, pin: String, role: UserRole) {
        viewModelScope.launch {
            repository.insertUser(UserEntity(name = name, pin = pin, role = role))
            showToast("User '$name' (${role.displayName}) created")
        }
    }

    fun deleteUser(id: Long) {
        viewModelScope.launch {
            repository.deleteUser(id)
            showToast("User removed")
        }
    }

    // --- Masters ---
    fun addCategory(name: String) = viewModelScope.launch { repository.insertCategory(name) }
    fun deleteCategory(id: Long) = viewModelScope.launch { repository.deleteCategory(id) }

    fun addBrand(name: String) = viewModelScope.launch { repository.insertBrand(name) }
    fun deleteBrand(id: Long) = viewModelScope.launch { repository.deleteBrand(id) }

    fun addUnit(name: String) = viewModelScope.launch { repository.insertUnit(name) }
    fun deleteUnit(id: Long) = viewModelScope.launch { repository.deleteUnit(id) }

    fun addGodown(name: String, loc: String) = viewModelScope.launch { repository.insertGodown(name, loc) }
    fun deleteGodown(id: Long) = viewModelScope.launch { repository.deleteGodown(id) }

    // --- Items ---
    fun saveItem(item: ItemEntity, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            val role = _currentUser.value?.role?.displayName ?: "Admin"
            repository.saveItem(item, performedBy = role)
            showToast("Item '${item.name}' saved successfully")
            onDone?.invoke()
        }
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch {
            val role = _currentUser.value?.role?.displayName ?: "Admin"
            repository.deleteItem(id, performedBy = role)
            showToast("Item deleted")
        }
    }

    // --- Stock Movements ---
    fun recordStockMovement(
        itemId: Long,
        godownId: Long,
        godownName: String,
        movementType: MovementType,
        quantity: Double,
        unitCost: Double,
        referenceDoc: String,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val role = _currentUser.value?.role?.displayName ?: "Admin"
            val result = repository.recordMovement(
                itemId = itemId,
                godownId = godownId,
                godownName = godownName,
                movementType = movementType,
                quantity = quantity,
                unitCost = unitCost,
                referenceDoc = referenceDoc,
                notes = notes,
                performedBy = role
            )

            result.fold(
                onSuccess = { newStock ->
                    showToast("Movement recorded! New stock: $newStock")
                    onSuccess()
                },
                onFailure = { err ->
                    showToast(err.message ?: "Failed to record movement")
                }
            )
        }
    }

    // --- Purchases ---
    fun saveSupplier(supplier: SupplierEntity, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.saveSupplier(supplier)
            showToast("Supplier saved")
            onDone?.invoke()
        }
    }

    fun deleteSupplier(id: Long) {
        viewModelScope.launch {
            repository.deleteSupplier(id)
            showToast("Supplier removed")
        }
    }

    fun createPurchaseBill(
        bill: PurchaseBillEntity,
        items: List<PurchaseBillItemEntity>,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            val role = _currentUser.value?.role?.displayName ?: "Admin"
            repository.createPurchaseBill(bill, items, performedBy = role)
            showToast("Purchase bill #${bill.billNumber} created & stock updated")
            onDone()
        }
    }

    // --- Customers ---
    fun saveCustomer(customer: CustomerEntity, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.saveCustomer(customer)
            showToast("Customer saved")
            onDone?.invoke()
        }
    }

    fun deleteCustomer(id: Long) {
        viewModelScope.launch {
            repository.deleteCustomer(id)
            showToast("Customer removed")
        }
    }

    // --- POS Cart Operations ---
    fun addToCart(item: ItemEntity, qty: Double = 1.0) {
        val current = _cart.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.item.id == item.id }
        if (existingIndex >= 0) {
            val existing = current[existingIndex]
            val newQty = existing.quantity + qty
            if (newQty > item.currentStock) {
                showToast("Only ${item.currentStock} in stock!")
                return
            }
            current[existingIndex] = existing.copy(quantity = newQty)
        } else {
            if (qty > item.currentStock) {
                showToast("Only ${item.currentStock} in stock!")
                return
            }
            current.add(CartItem(item = item, quantity = qty))
        }
        _cart.value = current
    }

    fun updateCartItemQty(itemId: Long, qty: Double) {
        val current = _cart.value.toMutableList()
        val index = current.indexOfFirst { it.item.id == itemId }
        if (index >= 0) {
            if (qty <= 0) {
                current.removeAt(index)
            } else {
                val item = current[index].item
                if (qty > item.currentStock) {
                    showToast("Cannot exceed available stock (${item.currentStock})")
                    return
                }
                current[index] = current[index].copy(quantity = qty)
            }
            _cart.value = current
        }
    }

    fun removeFromCart(itemId: Long) {
        _cart.value = _cart.value.filter { it.item.id != itemId }
    }

    fun clearCart() {
        _cart.value = emptyList()
        _selectedCustomer.value = null
    }

    fun setSelectedCustomer(customer: CustomerEntity?) {
        _selectedCustomer.value = customer
    }

    fun completeSale(
        paymentMode: PaymentMode,
        discountOverall: Double,
        paidAmount: Double,
        notes: String,
        onSuccess: (SalesBillEntity, List<SalesBillItemEntity>) -> Unit
    ) {
        val cartItems = _cart.value
        if (cartItems.isEmpty()) {
            showToast("Cart is empty!")
            return
        }

        viewModelScope.launch {
            val profile = repository.getCompanyProfileSync()
            val seq = profile?.nextInvoiceSeq ?: 1
            val prefix = profile?.invoicePrefix ?: "SS-INV-"
            val invNumber = String.format(java.util.Locale.US, "%s%04d", prefix, seq)

            val companyState = profile?.state ?: "West Bengal"
            val customerState = _selectedCustomer.value?.state ?: companyState
            val isIntraState = companyState.equals(customerState, ignoreCase = true)

            var subtotal = 0.0
            var totalCgst = 0.0
            var totalSgst = 0.0
            var totalIgst = 0.0

            val salesItems = cartItems.map { c ->
                val lineTaxable = c.taxableAmount
                subtotal += lineTaxable

                val cgst: Double
                val sgst: Double
                val igst: Double

                if (isIntraState) {
                    val halfRate = c.item.gstRate / 2.0
                    cgst = lineTaxable * halfRate / 100.0
                    sgst = lineTaxable * halfRate / 100.0
                    igst = 0.0
                    totalCgst += cgst
                    totalSgst += sgst
                } else {
                    cgst = 0.0
                    sgst = 0.0
                    igst = lineTaxable * c.item.gstRate / 100.0
                    totalIgst += igst
                }

                SalesBillItemEntity(
                    billId = 0L,
                    itemId = c.item.id,
                    itemName = c.item.name,
                    hsnCode = c.item.hsnCode,
                    quantity = c.quantity,
                    unitPrice = c.unitPrice,
                    discount = c.discountPercent,
                    gstRate = c.item.gstRate,
                    cgst = cgst,
                    sgst = sgst,
                    igst = igst,
                    totalAmount = lineTaxable + cgst + sgst + igst
                )
            }

            val grandTotal = (subtotal - discountOverall + totalCgst + totalSgst + totalIgst).coerceAtLeast(0.0)
            val actualPaid = if (paymentMode == PaymentMode.CREDIT) paidAmount else grandTotal
            val due = (grandTotal - actualPaid).coerceAtLeast(0.0)

            val bill = SalesBillEntity(
                invoiceNumber = invNumber,
                customerId = _selectedCustomer.value?.id,
                customerName = _selectedCustomer.value?.name ?: "Cash Customer",
                customerPhone = _selectedCustomer.value?.phone ?: "",
                customerGstin = _selectedCustomer.value?.gstin ?: "",
                customerState = customerState,
                subtotal = subtotal,
                discountAmount = discountOverall,
                cgstAmount = totalCgst,
                sgstAmount = totalSgst,
                igstAmount = totalIgst,
                grandTotal = grandTotal,
                paymentMode = paymentMode,
                paidAmount = actualPaid,
                dueAmount = due,
                notes = notes
            )

            val role = _currentUser.value?.role?.displayName ?: "Staff"
            val result = repository.createSalesBill(bill, salesItems, performedBy = role)

            result.fold(
                onSuccess = { billId ->
                    showToast("Sale completed! Invoice #$invNumber")
                    val savedBill = bill.copy(id = billId)
                    clearCart()
                    onSuccess(savedBill, salesItems)
                },
                onFailure = { err ->
                    showToast(err.message ?: "Failed to complete sale")
                }
            )
        }
    }

    // --- Settings & Profile ---
    fun updateCompanyProfile(profile: CompanyProfileEntity) {
        viewModelScope.launch {
            repository.saveCompanyProfile(profile)
            showToast("Company profile updated")
        }
    }

    fun exportBackupJson(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportBackupJson()
            onResult(json)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            val role = _currentUser.value?.role?.displayName ?: "Admin"
            repository.clearAllData(role)
            showToast("All inventory data wiped clean")
        }
    }
}

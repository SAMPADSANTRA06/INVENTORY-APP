package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val pin: String,
    val role: UserRole,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(tableName = "brands")
data class BrandEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(tableName = "units")
data class UnitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(tableName = "godowns")
data class GodownEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val location: String = ""
)

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sku: String,
    val barcode: String,
    val categoryId: Long,
    val categoryName: String = "",
    val brand: String = "",
    val unit: String = "PCS",
    val hsnCode: String = "",
    val gstRate: Double = 18.0,
    val purchasePrice: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val mrp: Double = 0.0,
    val openingStock: Double = 0.0,
    val currentStock: Double = 0.0,
    val reorderLevel: Double = 5.0,
    val batchNo: String = "",
    val expiryDate: String = "",
    val imageUri: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "stock_ledger")
data class StockLedgerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val itemName: String,
    val godownId: Long = 1,
    val godownName: String = "Main Godown",
    val movementType: MovementType,
    val quantity: Double,
    val unitCost: Double,
    val balanceAfter: Double,
    val referenceDoc: String = "",
    val notes: String = "",
    val performedBy: String = "Admin",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val gstin: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val state: String = "West Bengal",
    val balanceDue: Double = 0.0
)

@Entity(tableName = "purchase_bills")
data class PurchaseBillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val billNumber: String,
    val supplierId: Long,
    val supplierName: String,
    val date: Long = System.currentTimeMillis(),
    val subtotal: Double = 0.0,
    val gstTotal: Double = 0.0,
    val grandTotal: Double = 0.0,
    val paidAmount: Double = 0.0,
    val dueAmount: Double = 0.0,
    val paymentStatus: PaymentStatus = PaymentStatus.PAID,
    val notes: String = ""
)

@Entity(tableName = "purchase_items")
data class PurchaseBillItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val billId: Long,
    val itemId: Long,
    val itemName: String,
    val quantity: Double,
    val unitPrice: Double,
    val gstRate: Double,
    val gstAmount: Double,
    val totalAmount: Double
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val state: String = "West Bengal",
    val gstin: String = "",
    val outstandingBalance: Double = 0.0
)

@Entity(tableName = "sales_bills")
data class SalesBillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val customerId: Long? = null,
    val customerName: String = "Cash Customer",
    val customerPhone: String = "",
    val customerGstin: String = "",
    val customerState: String = "West Bengal",
    val date: Long = System.currentTimeMillis(),
    val subtotal: Double = 0.0,
    val discountAmount: Double = 0.0,
    val cgstAmount: Double = 0.0,
    val sgstAmount: Double = 0.0,
    val igstAmount: Double = 0.0,
    val grandTotal: Double = 0.0,
    val paymentMode: PaymentMode = PaymentMode.CASH,
    val paidAmount: Double = 0.0,
    val dueAmount: Double = 0.0,
    val status: BillStatus = BillStatus.COMPLETED,
    val notes: String = ""
)

@Entity(tableName = "sales_items")
data class SalesBillItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val billId: Long,
    val itemId: Long,
    val itemName: String,
    val hsnCode: String = "",
    val quantity: Double,
    val unitPrice: Double,
    val discount: Double = 0.0,
    val gstRate: Double = 0.0,
    val cgst: Double = 0.0,
    val sgst: Double = 0.0,
    val igst: Double = 0.0,
    val totalAmount: Double
)

@Entity(tableName = "company_profile")
data class CompanyProfileEntity(
    @PrimaryKey val id: Long = 1,
    val companyName: String = "Sampad Santra",
    val gstin: String = "",
    val address: String = "",
    val state: String = "West Bengal",
    val phone: String = "",
    val email: String = "",
    val invoicePrefix: String = "SS-INV-",
    val nextInvoiceSeq: Int = 1,
    val termsConditions: String = "Goods once sold cannot be returned without original receipt.",
    val valuationMethod: ValuationMethod = ValuationMethod.WEIGHTED_AVERAGE
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val action: String,
    val details: String,
    val userRole: String,
    val timestamp: Long = System.currentTimeMillis()
)

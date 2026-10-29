package com.example.data.model

enum class UserRole {
    ADMIN,
    MANAGER,
    STAFF;

    val displayName: String
        get() = when (this) {
            ADMIN -> "Admin"
            MANAGER -> "Manager"
            STAFF -> "Staff"
        }
}

enum class MovementType(val displayName: String, val isPositive: Boolean) {
    PURCHASE("Purchase Bill", true),
    SALE("Sales Invoice", false),
    TRANSFER_IN("Transfer Received", true),
    TRANSFER_OUT("Transfer Dispatched", false),
    ADJUSTMENT_ADD("Stock Adjustment (+)", true),
    ADJUSTMENT_SUB("Stock Adjustment (-)", false),
    PURCHASE_RETURN("Purchase Return", false),
    SALES_RETURN("Sales Return", true)
}

enum class PaymentMode(val displayName: String) {
    CASH("Cash"),
    UPI("UPI / QR"),
    CARD("Card / POS"),
    CREDIT("Store Credit / Due")
}

enum class PaymentStatus(val displayName: String) {
    PAID("Paid"),
    PARTIAL("Partial"),
    DUE("Due")
}

enum class ValuationMethod(val displayName: String) {
    WEIGHTED_AVERAGE("Weighted Average Cost"),
    FIFO("First In, First Out (FIFO)")
}

enum class BillStatus(val displayName: String) {
    COMPLETED("Completed"),
    RETURNED("Returned"),
    CANCELLED("Cancelled")
}


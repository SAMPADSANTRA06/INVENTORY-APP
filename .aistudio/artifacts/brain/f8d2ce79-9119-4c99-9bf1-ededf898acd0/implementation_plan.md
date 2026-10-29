# Sampad Santra Inventory Management System (Android)

A modern, offline-first native Android ERP & Inventory Management application for **Sampad Santra**, built with Jetpack Compose, Room SQLite persistence, and Material Design 3. The app starts completely clean (zero dummy data), with an initial Admin onboarding flow, role-based access, automated GST billing, FIFO/Weighted Average stock valuation, barcode scanning, and PDF tax invoices.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural and workflow decisions have been confirmed and will guide the implementation:

- **Confirmed: Native Android Architecture**: Built entirely in Kotlin and Jetpack Compose with local Room SQLite persistence, meeting the offline-first ("no backend, private to device") requirement while leveraging native Android features (Camera Barcode scanning, PDF generation, and system printing).
- **Confirmed: First-Launch Admin Setup**: The database starts completely empty. On initial launch, a streamlined setup wizard prompts the user to establish the company profile ("Sampad Santra") and create the master Admin 4-digit PIN. Subsequent users (Managers, Staff) can be configured by the Admin.
- **Confirmed: Adaptive Design System**: Responsive layout supporting compact phone viewports (M3 Bottom Navigation Bar with safe-area insets) and wide/tablet displays (Navigation Rail with dual-pane POS and inventory lists).

---

## 1. Overview & Core Concept

**Sampad Santra Inventory** is a production-grade enterprise inventory and billing suite tailored for Indian and global retail/wholesale trade. It provides end-to-end stock tracking, preventing negative inventory, maintaining an immutable double-entry stock ledger, and automating multi-rate GST tax invoicing.

### Target Persona & Context
- **Business Owner (Sampad Santra / Admin)**: Needs real-time stock valuation (FIFO/Weighted Average), profit & loss visibility, GST summaries, and full administrative control over godowns and staff accounts.
- **Store Manager**: Handles purchase bills, stock transfers between warehouses, stock adjustments with reason tracking, and supplier payments.
- **Sales Staff**: Operates the rapid POS billing screen on handheld devices, scanning barcodes or picking items, collecting payments (Cash, UPI, Card, Credit), and generating instant printable PDF tax invoices.

### Core Value Proposition
- **Zero-Trust Inventory Integrity**: Prevents negative stock at the database transaction level; every movement (Purchase, Sale, Transfer, Adjustment) writes to a permanent stock ledger.
- **Completely Private & Offline**: All master records, transactions, audit logs, and configurations remain strictly on-device in Room SQLite with full JSON backup/restore.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Onboarding / Zero-State**:
   - First launch detects empty database $\rightarrow$ displays Welcome Wizard $\rightarrow$ sets business name ("Sampad Santra"), default GSTIN/State, and creates the Admin PIN.
   - Clean dashboard reveals zero-data state with motivational onboarding cards: *"Setup Categories"*, *"Add Your First Item"*, *"Record Opening Stock"*.
2. **Item Management & Barcode Generation**:
   - Items list with search bar, category chips, low-stock filter, and sorting.
   - Detailed item entry: Name, auto-generated SKU (`SS-0001`), Barcode with instant QR/Code128 vector generation, HSN, GST %, Purchase/Selling/MRP, Reorder Level, and Batch/Expiry.
3. **POS Quick Billing & PDF Tax Invoices**:
   - Cash register style screen with fast search and barcode scanner launcher.
   - Auto-calculated CGST + SGST (intra-state) or IGST (inter-state) based on customer state vs company state.
   - Instant payment capture (Cash, UPI QR shortcut, Card, Customer Credit ledger).
   - Generates standard GST-compliant PDF invoice (downloadable, printable via Android PrintManager, or shareable via WhatsApp/Email).
4. **Stock Movements & Transfers**:
   - Multi-godown management (Main Store, Godown A, Godown B).
   - Stock In (vendor receipt), Stock Out (store issue), Adjustment (breakage/damage), and Godown Transfer.
5. **Comprehensive Reports & Valuation**:
   - Real-time stock valuation toggle: **FIFO** vs. **Weighted Average Cost (WAC)**.
   - Date-filtered Profit & Loss, GST breakdown (GSTR-1 format), and Customer/Supplier receivables aging.

### Visual Identity & Theme
- **Aesthetic Direction**: *Precision Commercial ERP* — clean, high-contrast, professional, and tactile.
- **Color Palette**:
  - Primary: Deep Emerald (`#0A5C36` / `#34D399` in dark mode) representing commerce and growth.
  - Secondary: Slate Blue (`#1E293B` / `#94A3B8`) for structured tabular data and invoices.
  - Tertiary / Accent: Warm Amber (`#D97706`) for low-stock and expiry warnings.
  - Error: Coral Crimson (`#DC2626`) for negative stock alerts and out-of-stock badges.
  - Background: Crisp dynamic neutral (`#F8FAFC` light / `#0F172A` dark).
- **Typography & Layout**:
  - Clean Material 3 typography with tabular figures (`tnum`) for monetary values and quantities.
  - Strict 48dp touch targets, clear tactile ripples, and floating action shortcuts.

```
┌─────────────────────────────────────────────────────────────────┐
│                    SAMPAD SANTRA INVENTORY                      │
├───────────────────────────────┬─────────────────────────────────┤
│ [Adaptive Nav: Rail / Bottom] │ Quick KPI Summary Cards         │
│  • Dashboard                  │  [Stock: ₹0]  [Items: 0]        │
│  • Items & Barcodes           │  [Sales: ₹0]  [Low Stock: 0]    │
│  • Stock Movements            ├─────────────────────────────────┤
│  • Purchases & Suppliers      │ Visual Analytics & Alerts       │
│  • POS Sales & Billing        │  • Stock by Category Chart      │
│  • Financial Reports          │  • Low-Stock / Expiry Alert     │
│  • Settings & Masters         │  • Quick "+ New Sale / + Item"  │
└───────────────────────────────┴─────────────────────────────────┘
```

---

## 3. Key Product Decisions & Trade-Offs

| Decision | Chosen Implementation | Rationale | Alternatives Considered |
|---|---|---|---|
| **Persistence Engine** | Room Database (SQLite with Flow) | High performance, transactional atomic writes, robust relational schemas, zero network overhead. | DataStore (insufficient for relational queries) / Cloud (rejected by user prompt). |
| **Stock Ledger Model** | Event-sourced Ledger + Current Balance table | Guarantees auditability; historical movements cannot be silently lost or corrupted. | Simple mutable quantity column (prone to discrepancy, cannot track FIFO batches). |
| **Valuation Engine** | Dual: FIFO Batch queue & Weighted Average | Standard accounting compliance; allows retailers to switch valuation models transparently. | Single arbitrary purchase price (inaccurate for fluctuating wholesale rates). |
| **Tax Calculation** | Dynamic GST State Engine | Auto-determines Intra-state (CGST + SGST) vs Inter-state (IGST) matching Indian GST rules. | Flat tax rate (violates commercial compliance). |
| **Barcode/QR Handling** | Native Canvas Barcode/QR Renderer + Camera Intent | Instant generation without external cloud dependencies; works fully offline. | Third-party cloud barcode API (breaks offline requirement). |
| **Invoice Output** | Native Android `PdfDocument` | Zero-dependency, produces vector PDF files compatible with all POS thermal/A4 printers. | HTML WebView printing (heavy memory footprint, inconsistent formatting across devices). |

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌───────────────────────────────────────────────────────────────────────┐
│                           UI LAYER (Compose)                          │
│                                                                       │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐  ┌──────────┐ │
│  │ DashboardView│   │  ItemsView   │   │  POSBilling  │  │ReportsUI │ │
│  └──────┬───────┘   └──────┬───────┘   └──────┬───────┘  └────┬─────┘ │
│         │                  │                  │               │       │
│         └──────────────────┼──────────────────┴───────────────┘       │
│                            ▼                                          │
│                   AppViewModel / StateHolders                         │
│         (Role Session, Cart State, Ledger Actions, UI Filters)        │
└────────────────────────────┬──────────────────────────────────────────┘
                             │ Coroutine Dispatchers (IO)
┌────────────────────────────▼──────────────────────────────────────────┐
│                          DATA LAYER (Room)                            │
│                                                                       │
│  ┌───────────────────────┐             ┌───────────────────────────┐  │
│  │   InventoryRepository │             │      BillingRepository    │  │
│  └──────────┬────────────┘             └─────────────┬─────────────┘  │
│             │                                        │                │
│             ▼                                        ▼                │
│  ┌─────────────────────────────────────────────────────────────────┐  │
│  │                        AppDatabase                              │  │
│  │  • ItemsEntity         • StockLedgerEntity   • GodownsEntity    │  │
│  │  • SuppliersEntity     • PurchaseBillsEntity • PurchaseItems    │  │
│  │  • CustomersEntity     • SalesBillsEntity    • SalesItems       │  │
│  │  • CategoriesEntity    • UnitsEntity         • SettingsEntity   │  │
│  │  • AuditLogEntity      • UsersEntity (Roles)                    │  │
│  └─────────────────────────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────────────────────────┘
```

### Relational Data Models & State
1. **User / Auth Entity**: `id`, `name`, `pinHash`, `role` (`ADMIN`, `MANAGER`, `STAFF`), `createdAt`.
2. **Item Entity**: `id`, `name`, `sku`, `barcode`, `categoryId`, `brand`, `unitId`, `hsnCode`, `gstRate`, `purchasePrice`, `sellingPrice`, `mrp`, `reorderLevel`, `batchNumber`, `expiryDate`, `imageUri`, `currentStock`.
3. **Stock Ledger Entity**: `id`, `itemId`, `godownId`, `movementType` (`PURCHASE`, `SALE`, `TRANSFER_IN`, `TRANSFER_OUT`, `ADJUSTMENT_ADD`, `ADJUSTMENT_SUB`), `quantity`, `unitCost`, `referenceId`, `notes`, `timestamp`, `balanceAfter`.
4. **Sales Bill & Items**: `id`, `invoiceNumber`, `customerId`, `date`, `taxableAmount`, `cgstAmount`, `sgstAmount`, `igstAmount`, `discountAmount`, `totalAmount`, `paymentMode`, `paidAmount`, `dueAmount`, `status`.
5. **Purchase Bill & Items**: `id`, `billNumber`, `supplierId`, `date`, `taxableAmount`, `gstAmount`, `totalAmount`, `paymentStatus`, `paidAmount`, `dueAmount`.
6. **Company Settings**: `id`, `companyName`, `gstin`, `address`, `state`, `phone`, `email`, `invoicePrefix`, `invoiceNextNumber`, `defaultValuationMethod`.

### Execution Plan (Upon Approval)
1. **Phase A - Database & Architecture**:
   - Setup Room Entities, DAOs, TypeConverters, and Repository with reactive Flows.
   - Configure Room Database with thread-safe singleton initialization.
2. **Phase B - Business Logic & Valuation**:
   - Implement strict transactional stock movement controller (preventing negative inventory).
   - Implement FIFO and Weighted Average cost calculators.
   - Implement GST tax calculation engine.
3. **Phase C - Adaptive UI & Navigation**:
   - Build Theme, ColorScheme, and responsive layout shell (BottomBar on phones, Rail on tablets).
   - Implement Role login and first-launch Setup Wizard.
4. **Phase D - Feature Modules**:
   - Dashboard with KPI cards, Category distribution chart, and alert banners.
   - Items master with Barcode/QR generator, search, filter, and CSV import/export.
   - POS Billing screen with instant cart calculations and native PDF Tax Invoice generator.
   - Purchase module, Stock Transfer/Adjustment screens, and Supplier/Customer ledgers.
   - Reports engine (Valuation, P&L, GSTR-1 summary) and Settings backup/restore.
5. **Phase E - Verification**:
   - Run compilation via `compile_applet` and test edge cases.

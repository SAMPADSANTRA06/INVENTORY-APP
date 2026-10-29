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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AuditLogEntity
import com.example.data.model.CompanyProfileEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.RoleBadge
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.StatusRed
import com.example.ui.viewmodel.InventoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: InventoryViewModel,
    modifier: Modifier = Modifier
) {
    val companyProfile by viewModel.companyProfile.collectAsState()
    val users by viewModel.users.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val brands by viewModel.brands.collectAsState()
    val units by viewModel.units.collectAsState()
    val godowns by viewModel.godowns.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()

    var showAddUserDialog by remember { mutableStateOf(false) }
    var showAddMasterDialog by remember { mutableStateOf<String?>(null) } // "CATEGORY", "BRAND", "UNIT", "GODOWN"
    var showBackupDialog by remember { mutableStateOf(false) }
    var backupJsonContent by remember { mutableStateOf("") }
    var showWipeConfirm by remember { mutableStateOf(false) }

    // Company Profile editable state
    var companyName by remember(companyProfile) { mutableStateOf(companyProfile?.companyName ?: "Sampad Santra") }
    var gstin by remember(companyProfile) { mutableStateOf(companyProfile?.gstin ?: "") }
    var address by remember(companyProfile) { mutableStateOf(companyProfile?.address ?: "") }
    var state by remember(companyProfile) { mutableStateOf(companyProfile?.state ?: "West Bengal") }
    var phone by remember(companyProfile) { mutableStateOf(companyProfile?.phone ?: "") }
    var invoicePrefix by remember(companyProfile) { mutableStateOf(companyProfile?.invoicePrefix ?: "SS-INV-") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings & Administration",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )

        // 1. Company Profile Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Company Profile & Invoicing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text("Company Name") },
                    modifier = Modifier.fillMaxWidth().testTag("settings_company_name"),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = gstin,
                        onValueChange = { gstin = it.uppercase() },
                        label = { Text("GSTIN") },
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

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = invoicePrefix,
                        onValueChange = { invoicePrefix = it },
                        label = { Text("Invoice Prefix") },
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

                Button(
                    onClick = {
                        val current = companyProfile ?: CompanyProfileEntity()
                        viewModel.updateCompanyProfile(
                            current.copy(
                                companyName = companyName.trim(),
                                gstin = gstin.trim(),
                                state = state.trim(),
                                phone = phone.trim(),
                                invoicePrefix = invoicePrefix.trim(),
                                address = address.trim()
                            )
                        )
                    },
                    modifier = Modifier.align(Alignment.End).testTag("btn_save_company_profile")
                ) {
                    Text("Save Profile")
                }
            }
        }

        // 2. User & Role Management Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.People, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Users & Access Control", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    FilledTonalButton(
                        onClick = { showAddUserDialog = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp).testTag("btn_add_user")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ User", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                users.forEach { user ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            RoleBadge(role = user.role)
                            Text(user.name, fontWeight = FontWeight.SemiBold)
                        }

                        if (user.role != UserRole.ADMIN || users.count { it.role == UserRole.ADMIN } > 1) {
                            IconButton(onClick = { viewModel.deleteUser(user.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StatusRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        // 3. Masters Management (Categories, Brands, Units, Godowns)
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Masters & Categories", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showAddMasterDialog = "CATEGORY" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+ Category (${categories.size})", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = { showAddMasterDialog = "BRAND" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+ Brand (${brands.size})", fontSize = 11.sp)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showAddMasterDialog = "UNIT" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+ Unit (${units.size})", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = { showAddMasterDialog = "GODOWN" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+ Godown (${godowns.size})", fontSize = 11.sp)
                    }
                }
            }
        }

        // 4. Backup & Factory Wipe
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Data Backup & Recovery", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            viewModel.exportBackupJson { json ->
                                backupJsonContent = json
                                showBackupDialog = true
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("btn_export_backup")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export JSON")
                    }

                    OutlinedButton(
                        onClick = { showWipeConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRed),
                        modifier = Modifier.weight(1f).testTag("btn_wipe_data")
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Wipe All Data")
                    }
                }
            }
        }

        // 5. Chronological Audit Log
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("System Audit Trail (${auditLogs.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (auditLogs.isEmpty()) {
                    Text("No audit events recorded yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
                    auditLogs.take(15).forEach { log ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(log.action, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                Text(dateFormat.format(Date(log.timestamp)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(log.details, style = MaterialTheme.typography.bodySmall)
                            Text("By: ${log.userRole}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Divider(modifier = Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }

    // Add User Dialog
    if (showAddUserDialog) {
        AddUserDialog(
            onDismiss = { showAddUserDialog = false },
            onSave = { name, pin, role ->
                viewModel.createUser(name, pin, role)
                showAddUserDialog = false
            }
        )
    }

    // Add Master Dialog
    if (showAddMasterDialog != null) {
        AddMasterDialog(
            type = showAddMasterDialog!!,
            onDismiss = { showAddMasterDialog = null },
            onSave = { name, location ->
                when (showAddMasterDialog) {
                    "CATEGORY" -> viewModel.addCategory(name)
                    "BRAND" -> viewModel.addBrand(name)
                    "UNIT" -> viewModel.addUnit(name)
                    "GODOWN" -> viewModel.addGodown(name, location)
                }
                showAddMasterDialog = null
            }
        )
    }

    // Backup JSON Dialog
    if (showBackupDialog) {
        Dialog(onDismissRequest = { showBackupDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Database Backup JSON", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = backupJsonContent,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().height(200.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showBackupDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }

    // Wipe Confirmation
    if (showWipeConfirm) {
        ConfirmDeleteDialog(
            title = "Wipe All Inventory Data?",
            message = "This will permanently delete all items, stock ledgers, bills, and customers, resetting the app to a completely clean state.",
            onConfirm = {
                viewModel.clearAllData()
                showWipeConfirm = false
            },
            onDismiss = { showWipeConfirm = false }
        )
    }
}

@Composable
fun AddUserDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, UserRole) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(UserRole.STAFF) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Create User Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("User Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6) pin = it },
                    label = { Text("4 to 6-Digit PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Assign Role:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF).forEach { r ->
                        FilterChip(
                            selected = role == r,
                            onClick = { role = r },
                            label = { Text(r.displayName) }
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank() && pin.length >= 4) {
                                onSave(name.trim(), pin.trim(), role)
                            }
                        }
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}

@Composable
fun AddMasterDialog(
    type: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }

    val label = when (type) {
        "CATEGORY" -> "Category Name"
        "BRAND" -> "Brand Name"
        "UNIT" -> "Unit Symbol (e.g. PCS, KG, LTR)"
        else -> "Godown / Warehouse Name"
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Add $type", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(label) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (type == "GODOWN") {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location / Address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(name.trim(), location.trim())
                            }
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

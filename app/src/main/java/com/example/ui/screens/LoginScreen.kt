package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.ui.components.RoleBadge
import com.example.ui.viewmodel.InventoryViewModel

@Composable
fun LoginScreen(
    viewModel: InventoryViewModel,
    modifier: Modifier = Modifier
) {
    var pin by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    val users by viewModel.users.collectAsState()
    val companyProfile by viewModel.companyProfile.collectAsState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = companyProfile?.companyName ?: "Sampad Santra",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Enter your security PIN to unlock",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // PIN Dots Indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 4) {
                    val filled = i < pin.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (filled) MaterialTheme.colorScheme.primary else Color.Transparent
                            )
                            .border(
                                2.dp,
                                if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                CircleShape
                            )
                    )
                }
            }

            if (errorText != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorText!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Numeric Keypad
            val keyRows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("C", "0", "DEL")
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                for (row in keyRows) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        for (digit in row) {
                            Surface(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        errorText = null
                                        when (digit) {
                                            "C" -> pin = ""
                                            "DEL" -> if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                            else -> {
                                                if (pin.length < 6) {
                                                    val newPin = pin + digit
                                                    pin = newPin
                                                    if (newPin.length >= 4) {
                                                        viewModel.loginWithPin(newPin) { success ->
                                                            if (!success) {
                                                                errorText = "Invalid PIN"
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    .testTag("pin_key_$digit"),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                tonalElevation = 2.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (digit == "DEL") {
                                        Icon(
                                            Icons.Default.Backspace,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    } else {
                                        Text(
                                            text = digit,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Available Roles hint
            if (users.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Roles:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    users.map { it.role }.distinct().forEach { role ->
                        RoleBadge(role = role)
                    }
                }
            }
        }
    }
}

package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.ui.screens.*
import com.example.ui.theme.SampadSantraTheme
import com.example.ui.viewmodel.InventoryViewModel
import com.example.ui.viewmodel.Screen

class MainActivity : ComponentActivity() {

    private val viewModel: InventoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SampadSantraTheme {
                MainAppContainer(viewModel = viewModel)
            }
        }
    }
}

data class NavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String,
    val minRole: UserRole = UserRole.STAFF
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer(viewModel: InventoryViewModel) {
    val userCount by viewModel.userCount.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Collect Toast/Snackbar events
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Handle Hardware / Gesture Back Button
    if (currentUser != null && currentScreen != Screen.Dashboard) {
        BackHandler {
            viewModel.navigateTo(Screen.Dashboard)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when {
            // User Explicitly Logged Out: Show PIN Pad
            currentUser == null -> {
                LoginScreen(viewModel = viewModel)
            }
            // App Unlocked: Show Full ERP
            else -> {
                val navItems = listOf(
                    NavItem(Screen.Dashboard, Icons.Default.Dashboard, "Home"),
                    NavItem(Screen.Items, Icons.Default.Inventory2, "Items"),
                    NavItem(Screen.Stock, Icons.Default.SwapVert, "Stock"),
                    NavItem(Screen.Purchases, Icons.Default.ShoppingCart, "Purchase", UserRole.MANAGER),
                    NavItem(Screen.Sales, Icons.Default.PointOfSale, "POS"),
                    NavItem(Screen.Reports, Icons.Default.Assessment, "Reports", UserRole.MANAGER),
                    NavItem(Screen.Settings, Icons.Default.Settings, "Settings", UserRole.ADMIN)
                )

                val userRole = currentUser?.role ?: UserRole.STAFF
                val visibleNavItems = navItems.filter { item ->
                    when (item.minRole) {
                        UserRole.STAFF -> true
                        UserRole.MANAGER -> userRole == UserRole.ADMIN || userRole == UserRole.MANAGER
                        UserRole.ADMIN -> userRole == UserRole.ADMIN
                    }
                }

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val isTablet = maxWidth >= 600.dp

                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        bottomBar = {
                            if (!isTablet) {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 3.dp,
                                    windowInsets = WindowInsets.navigationBars
                                ) {
                                    visibleNavItems.forEach { item ->
                                        NavigationBarItem(
                                            selected = currentScreen == item.screen,
                                            onClick = { viewModel.navigateTo(item.screen) },
                                            icon = { Icon(item.icon, contentDescription = item.label) },
                                            label = { Text(item.label, fontSize = 11.sp) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                            ),
                                            modifier = Modifier.testTag("nav_item_${item.label.lowercase()}")
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (isTablet) {
                                NavigationRail(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    header = {
                                        Icon(
                                            Icons.Default.Inventory,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(vertical = 12.dp)
                                        )
                                    }
                                ) {
                                    visibleNavItems.forEach { item ->
                                        NavigationRailItem(
                                            selected = currentScreen == item.screen,
                                            onClick = { viewModel.navigateTo(item.screen) },
                                            icon = { Icon(item.icon, contentDescription = item.label) },
                                            label = { Text(item.label) },
                                            modifier = Modifier.testTag("rail_item_${item.label.lowercase()}")
                                        )
                                    }
                                }
                            }

                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                AnimatedContent(
                                    targetState = currentScreen,
                                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                                    label = "screen_transition"
                                ) { target ->
                                    when (target) {
                                        Screen.Dashboard -> DashboardScreen(viewModel = viewModel)
                                        Screen.Items -> ItemsScreen(viewModel = viewModel)
                                        Screen.Stock -> StockMovementsScreen(viewModel = viewModel)
                                        Screen.Purchases -> PurchasesScreen(viewModel = viewModel)
                                        Screen.Sales -> SalesPosScreen(viewModel = viewModel)
                                        Screen.Reports -> ReportsScreen(viewModel = viewModel)
                                        Screen.Settings -> SettingsScreen(viewModel = viewModel)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

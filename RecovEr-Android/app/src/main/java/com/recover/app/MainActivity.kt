package com.recover.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.mlkit.vision.common.InputImage
import com.recover.app.data.SubscriptionPlan
import com.recover.app.data.WearableSource
import com.recover.app.ui.*
import com.recover.app.ui.theme.RecovErTheme
import com.recover.app.ui.theme.NavyDark
import com.recover.app.ui.theme.AccentBlue
import com.recover.app.ui.theme.TextSec
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var paymentSheet: PaymentSheet
    private var pendingPlan: SubscriptionPlan = SubscriptionPlan.FREE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val recoveryManager     = RecovErViewModel.recoveryManager(this)
        val subscriptionManager = RecovErViewModel.subscriptionManager(this)

        paymentSheet = PaymentSheet(this) { result ->
            subscriptionManager.onPaymentResult(result, pendingPlan)
        }

        setContent {
            RecovErTheme {
                RecovErApp(
                    recoveryManager     = recoveryManager,
                    subscriptionManager = subscriptionManager,
                    onCheckout          = { plan ->
                        pendingPlan = plan
                        launchStripePayment(subscriptionManager, plan)
                    }
                )
            }
        }
    }

    private fun launchStripePayment(
        subscriptionManager: com.recover.app.data.SubscriptionManager,
        plan: SubscriptionPlan
    ) {
        lifecycleScope.launch {
            val params = subscriptionManager.fetchStripeParams(plan) ?: return@launch
            paymentSheet.presentWithPaymentIntent(
                paymentIntentClientSecret = params.clientSecret,
                configuration = PaymentSheet.Configuration(
                    merchantDisplayName = "RecovEr",
                    customer = PaymentSheet.CustomerConfiguration(
                        id            = params.customerId,
                        ephemeralKeySecret = params.ephemeralKey
                    ),
                    allowsDelayedPaymentMethods = false
                )
            )
        }
    }
}

// MARK: - Composable App

@Composable
fun RecovErApp(
    recoveryManager: com.recover.app.data.RecoveryManager,
    subscriptionManager: com.recover.app.data.SubscriptionManager,
    onCheckout: (SubscriptionPlan) -> Unit
) {
    val navController = rememberNavController()
    val scope         = rememberCoroutineScope()

    // Observe state
    val score       by recoveryManager.score.collectAsState()
    val snapshot    by recoveryManager.snapshot.collectAsState()
    val isLoading   by recoveryManager.isLoading.collectAsState()
    val faceScan    by recoveryManager.faceScanState.collectAsState()
    val devices     by recoveryManager.devices.collectAsState()
    val history     by recoveryManager.history.collectAsState()

    val plan        by subscriptionManager.currentPlan.collectAsState()
    val isPromo     by subscriptionManager.isPromoActive.collectAsState()
    val message     by subscriptionManager.promoMessage.collectAsState()
    val purchasing  by subscriptionManager.isPurchasing.collectAsState()

    LaunchedEffect(Unit) { recoveryManager.checkAndConnect() }

    val tabs = listOf(
        NavTab("dashboard", "Atsigavimas", Icons.Default.FavoriteBorder),
        NavTab("facescan",  "Veidas",      Icons.Default.Face),
        NavTab("wearables", "Apyrankės",   Icons.Default.Watch),
        NavTab("premium",   "Premium",     Icons.Default.Star)
    )

    Scaffold(
        containerColor = NavyDark,
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF0D1220), tonalElevation = 0.dp) {
                val backStack by navController.currentBackStackEntryAsState()
                val current   = backStack?.destination?.route

                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = current == tab.route,
                        onClick  = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        },
                        icon  = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, fontSize = androidx.compose.ui.unit.TextUnit.Unspecified) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor   = AccentBlue,
                            selectedTextColor   = AccentBlue,
                            unselectedIconColor = TextSec,
                            unselectedTextColor = TextSec,
                            indicatorColor      = AccentBlue.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController    = navController,
            startDestination = "dashboard",
            modifier         = Modifier.fillMaxSize().padding(padding)
        ) {
            composable("dashboard") {
                DashboardScreen(
                    score       = score,
                    snapshot    = snapshot,
                    history     = history,
                    isLoading   = isLoading,
                    hasPremium  = subscriptionManager.hasPremium,
                    hasAIAdvice = subscriptionManager.hasAIAdvice,
                    onRefresh   = { scope.launch { recoveryManager.refresh() } }
                )
            }
            composable("facescan") {
                FaceScanScreen(
                    faceScanState  = faceScan,
                    canUseFaceScan = subscriptionManager.canUseFaceScan,
                    onStartScan    = { image: InputImage? ->
                        scope.launch { recoveryManager.startFaceScan(image) }
                    },
                    onReset        = { recoveryManager.resetFaceScan() }
                )
            }
            composable("wearables") {
                WearableScreen(
                    devices            = devices,
                    canUseAllWearables = subscriptionManager.canUseWearables,
                    onToggle           = { source: WearableSource -> recoveryManager.toggleDevice(source) }
                )
            }
            composable("premium") {
                SubscriptionScreen(
                    currentPlan  = plan,
                    isPromoActive = isPromo,
                    promoMessage = message,
                    isPurchasing = purchasing,
                    onCheckout   = onCheckout,
                    onApplyPromo = subscriptionManager::applyPromoCode,
                    onRestore    = { scope.launch { subscriptionManager.restore() } },
                    onClearMessage = subscriptionManager::clearMessage
                )
            }
        }
    }
}

data class NavTab(val route: String, val label: String, val icon: ImageVector)

// Simple factory
object RecovErViewModel {
    private var rm: com.recover.app.data.RecoveryManager?     = null
    private var sm: com.recover.app.data.SubscriptionManager? = null

    fun recoveryManager(ctx: android.content.Context): com.recover.app.data.RecoveryManager {
        if (rm == null) rm = com.recover.app.data.RecoveryManager(ctx.applicationContext)
        return rm!!
    }

    fun subscriptionManager(ctx: android.content.Context): com.recover.app.data.SubscriptionManager {
        if (sm == null) sm = com.recover.app.data.SubscriptionManager(ctx.applicationContext)
        return sm!!
    }
}

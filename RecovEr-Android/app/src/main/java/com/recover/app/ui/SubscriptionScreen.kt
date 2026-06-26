package com.recover.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.recover.app.data.SubscriptionPlan
import com.recover.app.ui.theme.*

@Composable
fun SubscriptionScreen(
    currentPlan: SubscriptionPlan,
    isPromoActive: Boolean,
    promoMessage: String,
    isPurchasing: Boolean,
    onCheckout: (SubscriptionPlan) -> Unit,
    onApplyPromo: (String) -> Unit,
    onRestore: () -> Unit,
    onClearMessage: () -> Unit
) {
    var selectedPlan  by remember { mutableStateOf(SubscriptionPlan.MONTHLY) }
    var promoInput    by remember { mutableStateOf("") }
    var showPromo     by remember { mutableStateOf(false) }
    val keyboard      = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Premium", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)

        if (isPromoActive) {
            PromoActiveCard()
        } else {
            StatusCard(currentPlan)
            PlanSelector(selectedPlan) { selectedPlan = it }
            StripeButton(selectedPlan, isPurchasing) { onCheckout(selectedPlan) }
            PromoSection(showPromo, promoInput,
                onToggle  = { showPromo = !showPromo },
                onChange  = { promoInput = it },
                onApply   = {
                    keyboard?.hide()
                    onApplyPromo(promoInput)
                    promoInput = ""
                    showPromo  = false
                }
            )
            TextButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) {
                Text("Atkurti pirkimus", color = TextSec)
            }
        }

        if (promoMessage.isNotEmpty()) {
            MessageBanner(promoMessage, onClearMessage)
        }

        PaymentInfoCard()
    }
}

@Composable
private fun PromoActiveCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = Color(0xFF0F3320)),
        shape    = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier            = Modifier.padding(20.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("✅", fontSize = 48.sp)
            Text("Visos funkcijos atrakintos!", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                "Promo kodas NOREMBO aktyvus.\nNaudojatės pilna RecovEr versija nemokamai.",
                fontSize = 13.sp, color = TextSec, textAlign = TextAlign.Center
            )
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "Veido biometrinis skenavimas (ML Kit)",
                    "Visos apyrankės: Garmin, Huawei, Fitbit…",
                    "Neribota istorija (14+ dienų)",
                    "AI atsigavimo rekomendacijos",
                    "Stripe mokėjimų integracija",
                    "PDF eksportas"
                ).forEach { feature ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("✓", color = AccentGreen, fontWeight = FontWeight.Bold)
                        Text(feature, fontSize = 13.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCard(plan: SubscriptionPlan) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = CardBg),
        shape    = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier  = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(if (plan == SubscriptionPlan.FREE) "⭐" else "👑", fontSize = 28.sp)
            Column {
                Text("${plan.displayName} planas", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text(if (plan == SubscriptionPlan.FREE) "Atrakinkite Premium" else "Pilna prieiga",
                    fontSize = 12.sp, color = TextSec)
            }
        }
    }
}

@Composable
private fun PlanSelector(selected: SubscriptionPlan, onSelect: (SubscriptionPlan) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(SubscriptionPlan.MONTHLY, SubscriptionPlan.YEARLY).forEach { plan ->
            val isSelected = selected == plan
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) AccentBlue else Color.White.copy(0.1f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .background(if (isSelected) AccentBlue.copy(0.1f) else CardBg)
                    .clickable { onSelect(plan) }
                    .padding(14.dp)
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text(plan.displayName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            if (plan == SubscriptionPlan.YEARLY) {
                                Surface(
                                    color = AccentGreen,
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text(
                                        "IŠSAUGO 50%",
                                        fontSize   = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = Color.Black,
                                        modifier   = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                        Text(plan.price, fontSize = 12.sp, color = TextSec)
                    }
                    if (isSelected) {
                        Surface(color = AccentBlue, shape = RoundedCornerShape(50)) {
                            Text("✓", color = Color.White, fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StripeButton(plan: SubscriptionPlan, isPurchasing: Boolean, onClick: () -> Unit) {
    Button(
        onClick  = onClick,
        enabled  = !isPurchasing,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape    = RoundedCornerShape(14.dp),
        colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF635BFF))  // Stripe purple
    ) {
        if (isPurchasing) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Text("Jungiamasi prie Stripe…")
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("💳", fontSize = 18.sp)
                Text("Mokėti ${plan.price} per Stripe", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun PromoSection(
    showPromo: Boolean,
    promoInput: String,
    onToggle: () -> Unit,
    onChange: (String) -> Unit,
    onApply: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = CardBg),
        shape    = RoundedCornerShape(16.dp)
    ) {
        Column {
            Row(
                modifier  = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🏷️", fontSize = 18.sp)
                    Text("Promo kodas", fontSize = 14.sp, color = Color.White)
                }
                Text(if (showPromo) "▲" else "▼", fontSize = 12.sp, color = TextSec)
            }

            if (showPromo) {
                HorizontalDivider(color = Color.White.copy(0.07f))
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value         = promoInput,
                        onValueChange = onChange,
                        placeholder   = { Text("Įvesk promo kodą", color = TextSec) },
                        modifier      = Modifier.fillMaxWidth(),
                        singleLine    = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onApply() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = AccentBlue,
                            unfocusedBorderColor = Color.White.copy(0.2f),
                            focusedTextColor     = Color.White,
                            unfocusedTextColor   = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Button(
                        onClick  = onApply,
                        enabled  = promoInput.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                        shape    = RoundedCornerShape(12.dp)
                    ) {
                        Text("Pritaikyti")
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBanner(message: String, onDismiss: () -> Unit) {
    val isSuccess = message.contains("priimtas") || message.contains("aktyvuotas") || message.contains("atkurta")
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onDismiss),
        colors   = CardDefaults.cardColors(
            containerColor = if (isSuccess) Color(0xFF0F3320) else Color(0xFF3B0A0A)
        ),
        shape    = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (isSuccess) "✅" else "❌", fontSize = 16.sp)
            Text(message, fontSize = 13.sp, color = Color.White, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PaymentInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = CardBg),
        shape    = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Mokėjimo metodai", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextSec)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        color = Color(0xFF635BFF).copy(0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "🤖 Android",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF8B83FF),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Text("Stripe Payment Sheet", fontSize = 11.sp, color = TextSec)
                    Text("Visa, Mastercard, Google Pay", fontSize = 10.sp, color = TextSec)
                }

                Box(modifier = Modifier.width(1.dp).height(50.dp).background(Color.White.copy(0.08f)))

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        color = Color.White.copy(0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "🍎 iOS / Watch",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Text("Apple Pay", fontSize = 11.sp, color = TextSec)
                    Text("PKPaymentAuthorizationController", fontSize = 10.sp, color = TextSec)
                }
            }

            Text(
                "🔒 Visi mokėjimai šifruojami. Stripe PCI DSS lygis 1. Atšaukti galima bet kada.",
                fontSize = 10.sp, color = TextSec
            )
        }
    }
}

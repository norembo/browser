package com.recover.app.data

import android.content.Context
import androidx.core.content.edit
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class SubscriptionManager(context: Context) {

    private val prefs      = context.getSharedPreferences("recover_prefs", Context.MODE_PRIVATE)
    private val httpClient = OkHttpClient()
    private val promoCode  = "Norembo"

    private val _currentPlan    = MutableStateFlow(loadSavedPlan())
    private val _isPromoActive  = MutableStateFlow(prefs.getBoolean("promo_active", false))
    private val _promoMessage   = MutableStateFlow("")
    private val _isPurchasing   = MutableStateFlow(false)

    val currentPlan:   StateFlow<SubscriptionPlan> = _currentPlan
    val isPromoActive: StateFlow<Boolean>           = _isPromoActive
    val promoMessage:  StateFlow<String>            = _promoMessage
    val isPurchasing:  StateFlow<Boolean>           = _isPurchasing

    // MARK: - Feature Gates

    val hasPremium       get() = _isPromoActive.value || _currentPlan.value != SubscriptionPlan.FREE
    val canUseFaceScan   get() = hasPremium
    val canUseWearables  get() = hasPremium
    val hasAIAdvice      get() = hasPremium
    val hasUnlimitedHistory get() = hasPremium

    // MARK: - Promo Code

    fun applyPromoCode(code: String) {
        if (code.trim().equals(promoCode, ignoreCase = true)) {
            _isPromoActive.value = true
            _currentPlan.value   = SubscriptionPlan.YEARLY
            prefs.edit {
                putBoolean("promo_active", true)
                putString("subscription_plan", SubscriptionPlan.YEARLY.name)
            }
            _promoMessage.value = "Promo kodas priimtas! Visos funkcijos atrakintos."
        } else {
            _promoMessage.value = "Neteisingas promo kodas."
        }
    }

    fun clearMessage() { _promoMessage.value = "" }

    // MARK: - Stripe Payment Sheet

    // Call this from your Activity/Fragment to get the PaymentSheet params.
    // Flow:
    // 1. fetchStripeParams() → calls backend /api/create-payment-intent
    // 2. Backend creates PaymentIntent with Stripe SDK and returns { clientSecret, ephemeralKey, customerId }
    // 3. Present PaymentSheet with those params
    // 4. On PaymentSheetResult.Completed → confirm server-side and activate plan

    suspend fun fetchStripeParams(plan: SubscriptionPlan): StripeCheckoutParams? =
        withContext(Dispatchers.IO) {
            _isPurchasing.value = true
            try {
                val body = JSONObject().apply {
                    put("priceId",    plan.stripePriceId)
                    put("plan",       plan.name)
                }.toString().toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("${com.recover.app.BuildConfig.BACKEND_URL}/api/create-payment-intent")
                    .post(body)
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val json = JSONObject(response.body?.string() ?: return@withContext null)
                    StripeCheckoutParams(
                        clientSecret   = json.getString("clientSecret"),
                        ephemeralKey   = json.getString("ephemeralKey"),
                        customerId     = json.getString("customerId")
                    )
                }
            } catch (e: Exception) {
                _promoMessage.value = "Ryšio klaida: ${e.message}"
                null
            } finally {
                _isPurchasing.value = false
            }
        }

    fun onPaymentResult(result: PaymentSheetResult, plan: SubscriptionPlan) {
        when (result) {
            is PaymentSheetResult.Completed -> {
                _currentPlan.value = plan
                prefs.edit { putString("subscription_plan", plan.name) }
                _promoMessage.value = "${plan.displayName} planas aktyvuotas!"
            }
            is PaymentSheetResult.Canceled  -> _promoMessage.value = "Mokėjimas atšauktas."
            is PaymentSheetResult.Failed    -> _promoMessage.value = "Mokėjimo klaida: ${result.error.message}"
        }
    }

    suspend fun restore() {
        _isPurchasing.value = true
        delay(800)
        _isPurchasing.value = false
        _promoMessage.value = if (_isPromoActive.value) "Promo versija atkurta." else "Nieko neaptikta."
    }

    // MARK: - Helpers

    private fun loadSavedPlan(): SubscriptionPlan {
        val name = prefs.getString("subscription_plan", SubscriptionPlan.FREE.name)
        return SubscriptionPlan.entries.find { it.name == name } ?: SubscriptionPlan.FREE
    }
}

data class StripeCheckoutParams(
    val clientSecret: String,
    val ephemeralKey: String,
    val customerId:   String
)

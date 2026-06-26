package com.recover.app

import android.app.Application
import com.stripe.android.PaymentConfiguration

class RecovErApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PaymentConfiguration.init(applicationContext, BuildConfig.STRIPE_PUBLISHABLE_KEY)
    }
}

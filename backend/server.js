'use strict'

require('dotenv').config()

const express = require('express')
const cors    = require('cors')
const helmet  = require('helmet')
const stripe  = require('stripe')(process.env.STRIPE_SECRET_KEY)

const app  = express()
const PORT = process.env.PORT || 3000

// Security headers
app.use(helmet())
app.use(cors())

// Raw body needed for Stripe webhook signature verification
app.use('/api/webhook', express.raw({ type: 'application/json' }))
app.use(express.json())

// ---------- Price IDs (set these in your Stripe dashboard) ----------
const PRICES = {
  MONTHLY: process.env.STRIPE_PRICE_MONTHLY || 'price_recover_monthly',
  YEARLY:  process.env.STRIPE_PRICE_YEARLY  || 'price_recover_yearly',
}

// ---------- POST /api/create-payment-intent ----------
// Android calls this before presenting Stripe PaymentSheet.
// Returns: { clientSecret, ephemeralKey, customerId }
app.post('/api/create-payment-intent', async (req, res) => {
  try {
    const { plan, customerId: existingCustomerId } = req.body

    if (!['MONTHLY', 'YEARLY'].includes(plan)) {
      return res.status(400).json({ error: 'Invalid plan' })
    }

    // Reuse customer if provided, otherwise create a new one
    let customerId = existingCustomerId
    if (!customerId) {
      const customer = await stripe.customers.create()
      customerId = customer.id
    }

    const ephemeralKey = await stripe.ephemeralKeys.create(
      { customer: customerId },
      { apiVersion: '2024-06-20' }
    )

    const amount = plan === 'MONTHLY' ? 999 : 5999  // cents

    const paymentIntent = await stripe.paymentIntents.create({
      amount,
      currency:               'eur',
      customer:               customerId,
      automatic_payment_methods: { enabled: true },
      metadata:               { plan, source: 'android' },
    })

    res.json({
      clientSecret: paymentIntent.client_secret,
      ephemeralKey: ephemeralKey.secret,
      customerId,
    })
  } catch (err) {
    console.error('create-payment-intent error:', err.message)
    res.status(500).json({ error: err.message })
  }
})

// ---------- GET /api/subscription-status ----------
// Poll after payment to confirm active plan.
app.get('/api/subscription-status', async (req, res) => {
  const { customerId } = req.query
  if (!customerId) return res.status(400).json({ error: 'customerId required' })

  try {
    const subscriptions = await stripe.subscriptions.list({
      customer: customerId,
      status:   'active',
      limit:    1,
    })

    if (subscriptions.data.length === 0) {
      return res.json({ active: false })
    }

    const sub = subscriptions.data[0]
    res.json({
      active:  true,
      plan:    sub.metadata.plan || 'MONTHLY',
      validTo: sub.current_period_end,
    })
  } catch (err) {
    res.status(500).json({ error: err.message })
  }
})

// ---------- POST /api/webhook ----------
// Stripe calls this when payment events happen.
// Configure in Stripe Dashboard → Webhooks → your endpoint.
app.post('/api/webhook', (req, res) => {
  const sig    = req.headers['stripe-signature']
  const secret = process.env.STRIPE_WEBHOOK_SECRET

  let event
  try {
    event = stripe.webhooks.constructEvent(req.body, sig, secret)
  } catch (err) {
    console.error('Webhook signature verification failed:', err.message)
    return res.status(400).send(`Webhook Error: ${err.message}`)
  }

  switch (event.type) {
    case 'payment_intent.succeeded': {
      const pi = event.data.object
      console.log(`Payment succeeded: ${pi.id} | plan: ${pi.metadata.plan} | customer: ${pi.customer}`)
      // TODO: Update your database to mark this customer as premium
      break
    }
    case 'customer.subscription.deleted':
    case 'customer.subscription.updated': {
      const sub = event.data.object
      console.log(`Subscription ${event.type}: ${sub.id} | status: ${sub.status}`)
      // TODO: Update your database accordingly
      break
    }
    default:
      console.log(`Unhandled event type: ${event.type}`)
  }

  res.json({ received: true })
})

// ---------- Health check ----------
app.get('/health', (_req, res) => res.json({ status: 'ok', app: 'RecovEr' }))

app.listen(PORT, () => console.log(`RecovEr backend running on port ${PORT}`))

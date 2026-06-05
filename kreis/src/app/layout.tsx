import type { Metadata, Viewport } from 'next'
import './globals.css'

export const metadata: Metadata = {
  title: {
    default:  'Kreis — Free AI Conversion Audit for E-commerce',
    template: '%s | Kreis',
  },
  description:
    'Get a free, AI-powered CRO audit for your Shopify store in under 60 seconds. ' +
    'Identify conversion killers and auto-apply fixes without a developer.',
  keywords: ['CRO audit', 'Shopify', 'conversion rate optimization', 'AI', 'e-commerce'],
  openGraph: {
    type:    'website',
    locale:  'en_US',
    siteName: 'Kreis',
  },
}

export const viewport: Viewport = {
  themeColor: '#09090f',
  width:      'device-width',
  initialScale: 1,
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" className="dark">
      <body className="min-h-screen antialiased">
        {children}
      </body>
    </html>
  )
}

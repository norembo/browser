import type { NextConfig } from 'next'

const nextConfig: NextConfig = {
  // Playwright uses native Node.js binaries — exclude from webpack bundling
  serverExternalPackages: ['playwright', 'playwright-core'],

  images: {
    remotePatterns: [
      { protocol: 'https', hostname: '**.myshopify.com' },
      { protocol: 'https', hostname: '**.shopify.com' },
    ],
  },
}

export default nextConfig

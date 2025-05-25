// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  ssr: false,
  devtools: { enabled: true },
  devServer: {
    port: 3001,
    host: 'localhost'
  },
  vite: {
    server: {
      allowedHosts: true,
    },
  },
  modules: [
    '@nuxt/ui',
    '@pinia/nuxt',
    '@nuxtjs/tailwindcss',
  ],
  // Plugins are auto-loaded from the plugins directory
  runtimeConfig: {
    public: {
      serverUrl: process.env.NUXT_PUBLIC_SERVER_URL || 'https://dev.uppdragsradarn.se',
      apiBase: process.env.NUXT_PUBLIC_API_BASE || 'https://dev.uppdragsradarn.se/api'
    }
  },
  app: {
    baseURL: '/admin/',
    head: {
      title: 'Uppdragsradarn Admin',
      htmlAttrs: {
        lang: 'en',
        class: 'nojs' // Will be replaced with js class by plugin if JS is enabled
      },
      meta: [
        { name: 'description', content: 'Admin panel for Uppdragsradarn' },
        { name: 'viewport', content: 'width=device-width, initial-scale=1' },
        { name: 'format-detection', content: 'telephone=no' },
        { name: 'theme-color', content: '#337b4c' },
        { name: 'robots', content: 'noindex, nofollow' }
      ],
      link: [
        { rel: 'icon', type: 'image/x-icon', href: '/favicon.ico' }
      ]
    }
  },
  typescript: {
    strict: true
  },
  tailwindcss: {
    cssPath: '~/assets/css/tailwind.css',
    configPath: '~/tailwind.config.ts'
  },
  css: [
    '~/assets/css/main.css'
  ],
  compatibilityDate: '2025-05-06',
})
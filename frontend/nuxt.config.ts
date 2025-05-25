// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  ssr: false,
  devtools: { enabled: true },
  devServer: {
    port: 3000
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
    '@nuxtjs/i18n'
  ],
  // Plugins are auto-loaded from the plugins directory
  i18n: {
    lazy: false, // Switch to eager loading to avoid path issues
    langDir: 'lang',
    defaultLocale: 'sv',
    locales: [
      { code: 'sv', iso: 'sv-SE', file: 'sv.json', name: 'Svenska' },
      { code: 'en', iso: 'en-US', file: 'en.json', name: 'English' }
    ],
    strategy: 'no_prefix',
    detectBrowserLanguage: false,
  },
  runtimeConfig: {
    public: {
      serverUrl: process.env.NUXT_PUBLIC_SERVER_URL || 'https://dev.uppdragsradarn.se',
      apiBase: process.env.NUXT_PUBLIC_API_BASE || 'https://dev.uppdragsradarn.se/api'
    }
  },
  app: {
    head: {
      title: 'Uppdragsradarn',
      htmlAttrs: {
        lang: 'sv',
        class: 'nojs' // Will be replaced with js class by plugin if JS is enabled
      },
      meta: [
        { name: 'description', content: 'Hitta ditt nästa uppdrag på nolltid' },
        { name: 'viewport', content: 'width=device-width, initial-scale=1' },
        { name: 'format-detection', content: 'telephone=no' },
        { name: 'theme-color', content: '#337b4c' },
        { property: 'og:title', content: 'Uppdragsradarn' },
        { property: 'og:description', content: 'Hitta ditt nästa uppdrag på nolltid' },
        { property: 'og:type', content: 'website' }
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
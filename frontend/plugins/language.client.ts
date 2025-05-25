import {watch} from 'vue'

export default defineNuxtPlugin({
  name: 'language-plugin',
  setup(nuxtApp) {
    // Initialize language setup directly without using composable at plugin level
    const i18n = nuxtApp.$i18n

    // Add function to enforce the language from localStorage
    // This ensures it's applied consistently across the application
    const enforceLanguageSetting = () => {
      if (process.client) {
        const savedLocale = localStorage.getItem('locale')
        if (savedLocale && [
            ...Array.isArray(i18n.locales.value) ? i18n.locales.value : Object.values(i18n.locales.value)
          ].some(loc => {
            return typeof loc === 'string' ? loc === savedLocale : loc.code === savedLocale
          })) {
          // Set from local storage
          i18n.locale.value = savedLocale

          // Update HTML lang attribute
          document.documentElement.setAttribute('lang', savedLocale.split('-')[0])
        }
      }
    }

    // Expose this function to the app
    nuxtApp.provide('enforceLanguageSetting', enforceLanguageSetting)

    // Run on app:mounted
    nuxtApp.hook('app:mounted', () => {
      // This runs in client-side only
      if (process.client) {
        enforceLanguageSetting()

        // Watch for locale changes to update HTML lang attribute
        watch(i18n.locale, (newLocale) => {
          if (newLocale) {
            localStorage.setItem('locale', newLocale)
            document.documentElement.setAttribute('lang', newLocale.split('-')[0])
          }
        })
      }
    })

    // Also run on page changes
    nuxtApp.hook('page:start', () => {
      if (process.client) {
        enforceLanguageSetting()
      }
    })
  }
})
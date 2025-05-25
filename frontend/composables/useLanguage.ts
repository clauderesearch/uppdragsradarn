import {onMounted, ref, watch} from 'vue'
import {useNuxtApp} from '#app'

// Create a singleton state that can be shared across component instances
const currentLocale = ref('')
let initialized = false
let localeRef: any = null
let localesRef: any = null

export default function useLanguage() {
  const nuxtApp = useNuxtApp()
  const i18n = nuxtApp.$i18n
  
  // Initialize references if not already done
  if (!localeRef) {
    localeRef = i18n.locale
    localesRef = i18n.locales
  }
  
  // Get available locales as an array
  const availableLocales = Array.isArray(localesRef.value) 
    ? localesRef.value 
    : Object.values(localesRef.value)

  const toggleLanguage = () => {
    // Find the next locale in the list
    const currentIndex = availableLocales.findIndex(loc => {
      return typeof loc === 'string' 
        ? loc === currentLocale.value 
        : loc.code === currentLocale.value
    })
    
    const nextIndex = (currentIndex + 1) % availableLocales.length
    const nextLocale = availableLocales[nextIndex]
    
    // Update the locale
    const localeCode = typeof nextLocale === 'string' ? nextLocale : nextLocale.code
    currentLocale.value = localeCode
    localeRef.value = localeCode
    
    // Save to localStorage
    localStorage.setItem('locale', localeCode)
    
    // Update HTML lang attribute
    if (process.client) {
      document.documentElement.setAttribute('lang', localeCode.split('-')[0])
    }
  }

  const setLanguage = (localeCode: string) => {
    if (availableLocales.some(loc => {
      return typeof loc === 'string' 
        ? loc === localeCode 
        : loc.code === localeCode
    })) {
      currentLocale.value = localeCode
      localeRef.value = localeCode
      localStorage.setItem('locale', localeCode)
      
      // Update HTML lang attribute
      if (process.client) {
        document.documentElement.setAttribute('lang', localeCode.split('-')[0])
      }
    }
  }

  const initLanguage = () => {
    // Avoid initializing multiple times
    if (initialized) return
    initialized = true
    
    // Get the current locale
    currentLocale.value = localeRef.value
    
    // Check localStorage for saved preference
    const savedLocale = localStorage.getItem('locale')
    if (savedLocale && availableLocales.some(loc => {
      return typeof loc === 'string' 
        ? loc === savedLocale 
        : loc.code === savedLocale
    })) {
      currentLocale.value = savedLocale
      localeRef.value = savedLocale
    }
    
    // Update HTML lang attribute
    if (process.client) {
      document.documentElement.setAttribute('lang', currentLocale.value.split('-')[0])
    }
  }

  // Watch for changes in currentLocale and update locale
  watch(currentLocale, (newLocale) => {
    if (newLocale && newLocale !== localeRef.value) {
      localeRef.value = newLocale
    }
  })

  // Initialize language when component is mounted in browser environment
  onMounted(() => {
    if (process.client) {
      initLanguage()
    }
  })

  // When used in a plugin, initLanguage is called directly and not in onMounted
  if (process.client && !initialized) {
    initLanguage()
  }

  return {
    currentLocale,
    availableLocales,
    toggleLanguage,
    setLanguage
  }
}
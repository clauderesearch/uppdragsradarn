import {onMounted, ref, watch} from 'vue'

// Create a singleton state that can be shared across component instances
const isDark = ref(false)
let initialized = false
let observer: MutationObserver | null = null

export default function useTheme() {
  const toggleDarkMode = () => {
    isDark.value = !isDark.value
    updateTheme()
  }

  const updateTheme = () => {
    if (isDark.value) {
      document.documentElement.classList.add('dark')
      localStorage.setItem('darkMode', 'true')
    } else {
      document.documentElement.classList.remove('dark')
      localStorage.setItem('darkMode', 'false')
    }
  }

  const initTheme = () => {
    // Avoid initializing multiple times
    if (initialized) return
    initialized = true
    
    const savedMode = localStorage.getItem('darkMode')
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
    
    // Use saved mode if it exists, otherwise use system preference
    isDark.value = savedMode === 'true' || (savedMode === null && prefersDark)
    
    // Apply the theme immediately
    updateTheme()
    
    // Add event listener for system preference changes
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
    
    const handleSystemThemeChange = (e: MediaQueryListEvent) => {
      if (localStorage.getItem('darkMode') === null) {
        isDark.value = e.matches
        updateTheme()
      }
    }
    
    // Prefer addEventListener but fall back to older method for compatibility
    if (mediaQuery.addEventListener) {
      mediaQuery.addEventListener('change', handleSystemThemeChange)
    } else {
      // @ts-ignore - older browsers may not support addEventListener
      mediaQuery.addListener(handleSystemThemeChange)
    }
    
    // Also listen for dark mode class changes on HTML element
    observer = new MutationObserver(mutations => {
      mutations.forEach(mutation => {
        if (mutation.attributeName === 'class') {
          const hasDarkClass = document.documentElement.classList.contains('dark')
          if (isDark.value !== hasDarkClass) {
            isDark.value = hasDarkClass
          }
        }
      })
    })
    
    observer.observe(document.documentElement, { attributes: true })
  }

  // Watch for changes in isDark and update the theme accordingly
  watch(isDark, () => {
    updateTheme()
  })

  // Initialize theme when component is mounted in browser environment
  onMounted(() => {
    if (process.client) {
      initTheme()
    }
  })

  // When used in a plugin, initTheme is called directly and not in onMounted
  if (process.client && !initialized) {
    initTheme()
  }

  return {
    isDark,
    toggleDarkMode
  }
}

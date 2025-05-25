// Plugin for accessibility enhancements
export default defineNuxtPlugin({
  name: 'accessibility-plugin',
  enforce: 'pre', // Run before other plugins
  setup() {
    // Add js class to html element when JavaScript is enabled
    // This allows for providing fallbacks when JS is disabled
    if (process.client) {
      document.documentElement.classList.remove('nojs')
      document.documentElement.classList.add('js')

      // Setup keyboard navigation helpers
      setupKeyboardNavigation()

      // Setup global keyboard accessibility
      setupGlobalKeyboardHandlers()
    }
  }
})

// Helper function to enhance keyboard navigation
function setupKeyboardNavigation() {
  // Add class when using keyboard navigation to enhance focus states
  document.body.addEventListener('keydown', (e) => {
    if (e.key === 'Tab') {
      document.body.classList.add('keyboard-user')

      // Add attribute for screen readers
      document.documentElement.setAttribute('data-navigation-method', 'keyboard')
    }
  })

  // Remove the class when mouse is used
  document.body.addEventListener('mousedown', () => {
    document.body.classList.remove('keyboard-user')

    // Update attribute for screen readers
    document.documentElement.setAttribute('data-navigation-method', 'mouse')
  })
}

// Setup global keyboard handlers for improved accessibility
function setupGlobalKeyboardHandlers() {
  document.addEventListener('keydown', (e) => {
    // Home key - Navigate to main content
    if (e.key === 'Home' && (e.ctrlKey || e.metaKey)) {
      e.preventDefault()
      const mainContent = document.getElementById('main-content')
      if (mainContent) {
        mainContent.focus()
        window.scrollTo(0, 0)
      }
    }

    // ESC key for closing modals, dropdowns, etc. is handled by individual components

    // Add ARIA announcements for key combinations
    if (e.altKey && !e.ctrlKey && !e.metaKey) {
      // Create or get announcer element
      let announcer = document.getElementById('aria-keyboard-announcer')
      if (!announcer) {
        announcer = document.createElement('div')
        announcer.id = 'aria-keyboard-announcer'
        announcer.setAttribute('aria-live', 'polite')
        announcer.setAttribute('class', 'sr-only')
        document.body.appendChild(announcer)
      }

      if (e.key === 'k') {
        e.preventDefault()
        announcer.textContent = 'Press / to focus search'

        // Focus the search input if available
        setTimeout(() => {
          const searchInput = document.querySelector('input[type="search"], input[placeholder*="search" i], input[placeholder*="sök" i]')
          if (searchInput) {
            (searchInput as HTMLElement).focus()
          }
        }, 100)
      }
    }
  })
}
/**
 * Global router options with security enhancements
 */
import type {RouterConfig} from '@nuxt/schema'

// https://router.vuejs.org/api/interfaces/routeroptions.html
export default <RouterConfig> {
  // Set routes to scroll to top on navigation for better user experience
  scrollBehavior(to, from, savedPosition) {
    // Return saved position when using browser navigation buttons
    if (savedPosition) {
      return savedPosition
    }
    
    // Custom behavior with security enhancements
    // Hash links are potentially susceptible to DOM clobbering
    // so we sanitize the selector
    if (to.hash) {
      // Sanitize hash to prevent injection
      const sanitizedHash = to.hash.replace(/[^a-zA-Z0-9-_]/g, '')
      if (sanitizedHash !== to.hash) {
        console.warn('Potentially unsafe hash navigation was sanitized')
        return { el: `#${sanitizedHash}`, top: 0 }
      }
      return { el: to.hash, top: 0 }
    }
    
    // Default: Scroll to top
    return { top: 0 }
  }
}
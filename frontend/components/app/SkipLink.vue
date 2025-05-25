<template>
  <a
    href="#main-content"
    class="skip-link sr-only focus:not-sr-only focus:absolute focus:z-50 focus:p-4 focus:text-white focus:bg-primary-600 focus:top-0 focus:left-0 focus:outline-0 focus:no-underline"
    @click.prevent="focusMainContent"
  >
    {{ $t('accessibility.skip_to_content') || 'Skip to main content' }}
  </a>
</template>

<script setup lang="ts">
/**
 * SkipLink component allows keyboard users to bypass navigation 
 * and go directly to the main content
 */
function focusMainContent() {
  // Find the main content area
  const mainContent = document.getElementById('main-content')
  if (mainContent) {
    // Set tabindex to make it focusable if it's not already
    if (!mainContent.hasAttribute('tabindex')) {
      mainContent.setAttribute('tabindex', '-1')
    }
    
    // Focus the main content
    mainContent.focus()
    
    // Set the URL hash
    window.location.hash = 'main-content'
  }
}
</script>

<style scoped>
.skip-link:focus {
  clip: auto;
  height: auto;
  width: auto;
  white-space: normal;
}
</style>
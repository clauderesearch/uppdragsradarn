<template>
  <div class="min-h-screen">
    <NuxtLayout>
      <NuxtPage />
    </NuxtLayout>
  </div>
</template>

<script setup lang="ts">
// Set default language for the application
// We'll adapt this based on user preference in the language plugin
useHead({
  htmlAttrs: {
    lang: 'en'
  }
})

// Add a watcher to ensure language is applied correctly at the root level
// This ensures language is consistent throughout the application
const { locale } = useI18n()
watch(locale, (newLocale) => {
  if (process.client && newLocale) {
    document.documentElement.lang = newLocale
  }
}, { immediate: true })
</script>
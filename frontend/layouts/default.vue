<template>
  <div class="min-h-screen flex flex-col text-textColor-light dark:text-textColor-dark transition-colors bg-bgColor-light dark:bg-bgColor-dark">
    <!-- Skip to content link with improved accessibility -->
    <SkipLink />
    
    <Header
      role="banner" 
      aria-label="Site header"
      :show-search="$route.path === '/' && $route.query.q && $route.query.q.length > 0"
      :show-logo="$route.path === '/' && $route.query.q && $route.query.q.length > 0"
      @search="handleSearch"
    />
    <main id="main-content" class="flex-grow" tabindex="-1">
      <slot />
    </main>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import Header from "../components/app/Header.vue";
import SkipLink from "../components/app/SkipLink.vue";
import {onMounted} from "vue";

const router = useRouter()

// Handle search from header
function handleSearch(query: string) {
  if (query && query.trim().length >= 2) {
    router.push({
      path: '/',
      query: { q: query.trim() }
    })
  }
}
</script>

<style scoped>
/* Remove outline when element is focused programmatically but keep it for keyboard focus */
#main-content:focus:not(:focus-visible) {
  outline: none;
}

/* Style focus state when using keyboard navigation */
#main-content:focus-visible {
  outline: 2px solid var(--color-primary-600);
  outline-offset: 2px;
}
</style>
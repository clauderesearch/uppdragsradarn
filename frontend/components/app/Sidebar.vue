<template>
  <aside class="w-64 bg-white shadow-md h-screen overflow-y-auto hidden md:block">
    <div class="py-6 px-4">
      <h2 class="text-lg font-semibold text-gray-900 mb-6">Navigation</h2>
      
      <nav class="space-y-1">
        <NuxtLink
          v-for="item in navigationItems"
          :key="item.name"
          :to="item.href"
          class="group flex items-center px-3 py-2 text-sm font-medium rounded-md"
          :class="[
            isActiveRoute(item.href) 
              ? 'bg-primary-50 text-primary-700' 
              : 'text-gray-700 hover:bg-gray-50 hover:text-gray-900'
          ]"
        >
          <component 
            :is="item.icon" 
            class="mr-3 flex-shrink-0 h-5 w-5" 
            :class="isActiveRoute(item.href) ? 'text-primary-700' : 'text-gray-400 group-hover:text-gray-500'"
            aria-hidden="true" 
          />
          {{ item.name }}
        </NuxtLink>
      </nav>
      
      <div class="mt-10">
        <h3 class="text-md font-medium text-gray-500 mb-3">Saved Searches</h3>
        <div v-if="savedSearches.length === 0" class="text-sm text-gray-500">
          No saved searches yet
        </div>
        <ul v-else class="space-y-2">
          <li v-for="search in savedSearches" :key="search.id">
            <NuxtLink 
              :to="`/search/${search.id}`"
              class="block text-sm text-gray-700 hover:text-primary-600 truncate"
            >
              {{ search.name }}
            </NuxtLink>
          </li>
        </ul>
        <NuxtLink 
          to="/saved-searches"
          class="mt-3 inline-flex items-center text-sm text-primary-600 hover:text-primary-700"
        >
          View all saved searches
        </NuxtLink>
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import {computed} from 'vue'
import {useRouter} from 'vue-router'
import {useSearchStore} from '~/stores/search'

// Importing icons from Heroicons (we'll be using the components)
// These would normally be imported, but for the simplicity of this example, we'll use placeholders

const router = useRouter()
const searchStore = useSearchStore()

const navigationItems = [
  { name: 'Dashboard', href: '/dashboard', icon: 'HomeIcon' },
  { name: 'Assignments', href: '/assignments', icon: 'BriefcaseIcon' },
  { name: 'Search Assignments', href: '/search', icon: 'MagnifyingGlassIcon' },
  { name: 'My Profile', href: '/profile', icon: 'UserIcon' },
  { name: 'Settings', href: '/settings', icon: 'Cog6ToothIcon' }
]

const savedSearches = computed(() => searchStore.savedSearches)

function isActiveRoute(path: string) {
  return router.currentRoute.value.path === path
}
</script>
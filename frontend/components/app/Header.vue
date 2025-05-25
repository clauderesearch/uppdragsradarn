<template>
  <header class="bg-bgColor-light dark:bg-bgColor-dark transition-colors border-b border-gray-200 dark:border-gray-800">
    <div class="container mx-auto px-4">
      <div class="flex justify-between items-center h-16">
        <!-- Site logo/home link with manual navigation -->
        <a href="#" @click.prevent="navigateHome" class="flex items-center focus:outline-none focus:ring-2 focus:ring-primary-500 rounded-md" aria-label="Go to home page">
          <AppLogo v-if="showLogo" size="tiny" class="mr-2" />
        </a>

        <!-- Search bar - visible only when showSearch is true -->
        <div v-if="showSearch" class="hidden sm:block flex-1 max-w-xl mx-4">
          <form @submit.prevent="$emit('search', searchQuery)" class="relative">
            <input
              v-model="searchQuery"
              type="text"
              :placeholder="$t('search.placeholder')"
              class="w-full px-4 py-1.5 text-sm border border-gray-300 dark:border-gray-700 rounded-full focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-primary-500 bg-white dark:bg-primary-950 text-gray-900 dark:text-gray-100"
            />
            <button
              type="submit"
              class="absolute right-1.5 top-1/2 transform -translate-y-1/2 bg-primary-600 dark:bg-primary-500 text-white px-2 py-0.5 rounded-full hover:bg-primary-700 dark:hover:bg-primary-400"
            >
              <MagnifyingGlassIcon class="h-3.5 w-3.5" />
            </button>
          </form>
        </div>

        <!-- Navigation controls -->
        <nav class="flex items-center space-x-4" aria-label="Main navigation">
          <DarkModeToggle />
          <LanguageToggle />

          <template v-if="isAuthenticated">
            <div class="relative ml-3">
              <button
                ref="userMenuButtonRef"
                @click="toggleUserMenu"
                class="flex text-sm border-2 border-transparent rounded-full focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                :aria-expanded="userMenuOpen"
                aria-haspopup="menu"
                aria-controls="user-menu"
                id="user-menu-button"
                :aria-label="$t('accessibility.open_user_menu')"
              >
                <span class="sr-only">{{ $t('accessibility.open_user_menu') }}</span>
                <div class="h-8 w-8 rounded-full bg-primary-200 dark:bg-primary-800 flex items-center justify-center text-primary-700 dark:text-primary-300">
                  {{ userInitials }}
                </div>
              </button>

              <div
                v-if="userMenuOpen"
                id="user-menu"
                class="origin-top-right absolute right-0 mt-2 w-48 rounded-md shadow-lg py-1 bg-white dark:bg-primary-950 ring-1 ring-black ring-opacity-5 z-10"
                role="menu"
                aria-orientation="vertical"
                aria-labelledby="user-menu-button"
                tabindex="-1"
              >
                <NuxtLink
                  to="/profile"
                  class="block px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-primary-900 focus:outline-none focus:bg-gray-100 dark:focus:bg-primary-900 focus:text-primary-600"
                  @click="userMenuOpen = false"
                  role="menuitem"
                  tabindex="0"
                >
                  {{ $t('common.your_profile') }}
                </NuxtLink>
                <button
                  @click="logout"
                  class="block w-full text-left px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-primary-900 focus:outline-none focus:bg-gray-100 dark:focus:bg-primary-900 focus:text-primary-600"
                  role="menuitem"
                  tabindex="0"
                >
                  {{ $t('common.sign_out') }}
                </button>
              </div>
            </div>
          </template>
          <template v-else>
            <button
              @click="authStore.login()"
              class="text-gray-700 dark:text-gray-300 hover:text-primary-600 dark:hover:text-primary-400 px-3 py-2 rounded-md text-sm font-medium focus:outline-none focus:ring-2 focus:ring-primary-500"
              aria-label="Sign in to your account"
            >
              {{ $t('common.sign_in') }}
            </button>
          </template>
        </nav>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import {computed, onMounted, onUnmounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {useAuthStore} from '~/stores/auth'
import DarkModeToggle from './DarkModeToggle.vue'
import LanguageToggle from './LanguageToggle.vue'
import AppLogo from './Logo.vue'
import {MagnifyingGlassIcon} from '@heroicons/vue/24/outline'

defineProps({
  showSearch: {
    type: Boolean,
    default: false
  },
  showLogo: {
    type: Boolean,
    default: false
  }
})

defineEmits(['search'])

const router = useRouter()
const authStore = useAuthStore()
const searchQuery = ref('')

const userMenuOpen = ref(false)
const userMenuButtonRef = ref<HTMLElement | null>(null)

// Handle keyboard events for accessibility
function handleKeyDown(event: KeyboardEvent) {
  if (event.key === 'Escape' && userMenuOpen.value) {
    userMenuOpen.value = false
    // Return focus to menu button when menu is closed with Escape
    if (userMenuButtonRef.value) {
      userMenuButtonRef.value.focus()
    }
  }
}

// Setup and cleanup event listeners
onMounted(() => {
  document.addEventListener('keydown', handleKeyDown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeyDown)
})

const isAuthenticated = computed(() => authStore.isAuthenticated)
const userInitials = computed(() => {
  if (!authStore.user?.given_name && !authStore.user?.family_name) return 'U'
  return `${authStore.user?.given_name?.charAt(0) || ''}${authStore.user?.family_name?.charAt(0) || ''}`
})

function toggleUserMenu() {
  userMenuOpen.value = !userMenuOpen.value
  
  // If menu is open, focus on the first menu item after the menu appears
  if (userMenuOpen.value) {
    setTimeout(() => {
      const firstMenuItem = document.querySelector('#user-menu a, #user-menu button')
      if (firstMenuItem) {
        (firstMenuItem as HTMLElement).focus()
      }
    }, 10)
  }
}


function logout() {
  authStore.logout()
  userMenuOpen.value = false
  router.push('/')
}

function navigateHome() {
  // Force a hard navigation to the root URL
  window.location.href = '/'
}
</script>
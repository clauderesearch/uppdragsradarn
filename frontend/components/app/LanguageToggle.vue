<template>
  <div class="relative">
    <button
      ref="languageMenuButtonRef"
      @click="toggleLanguageMenu"
      class="p-2 rounded-full hover:bg-gray-100 dark:hover:bg-primary-900 transition-colors text-gray-700 dark:text-gray-300 focus:outline-none focus:ring-2 focus:ring-primary-500"
      :aria-label="$t('accessibility.toggle_language')"
      aria-haspopup="listbox"
      :aria-expanded="languageMenuOpen"
      id="language-menu-button"
    >
      <span class="text-sm font-medium">{{ getCurrentLocaleName() }}</span>
      <span class="sr-only">{{ $t('accessibility.keyboard_nav_hint') }}</span>
    </button>

    <div
      v-if="languageMenuOpen"
      id="language-menu"
      class="origin-top-right absolute right-0 mt-2 w-48 rounded-md shadow-lg py-1 bg-white dark:bg-primary-950 ring-1 ring-black ring-opacity-5 z-10"
      role="listbox"
      aria-labelledby="language-menu-button"
      tabindex="-1"
    >
      <button
        v-for="loc in availableLocales"
        :key="getLocaleCode(loc)"
        @click="selectLanguage(getLocaleCode(loc))"
        @keydown.esc="closeLanguageMenu"
        class="block w-full text-left px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-primary-900 focus:outline-none focus:bg-gray-100 dark:focus:bg-primary-900 focus:text-primary-600"
        role="option"
        :aria-selected="getLocaleCode(loc) === locale.value"
        :tabindex="languageMenuOpen ? 0 : -1"
      >
        {{ getLocaleName(loc) }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, onUnmounted, ref} from 'vue'
import {useI18n} from '#imports'

const languageMenuOpen = ref(false)
const languageMenuButtonRef = ref<HTMLElement | null>(null)

const { locale, locales } = useI18n()

// Get available locales as an array
const availableLocales = computed(() => {
  return Array.isArray(locales.value) 
    ? locales.value 
    : Object.values(locales.value)
})

// Handle keyboard events for accessibility
function handleKeyDown(event: KeyboardEvent) {
  if (event.key === 'Escape' && languageMenuOpen.value) {
    languageMenuOpen.value = false
    // Return focus to menu button when menu is closed with Escape
    if (languageMenuButtonRef.value) {
      languageMenuButtonRef.value.focus()
    }
  }
}

// Setup and cleanup event listeners
onMounted(() => {
  document.addEventListener('keydown', handleKeyDown)
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeyDown)
  document.removeEventListener('click', handleClickOutside)
})

function handleClickOutside(event: MouseEvent) {
  if (
    languageMenuButtonRef.value && 
    languageMenuOpen.value &&
    !languageMenuButtonRef.value.contains(event.target as Node)
  ) {
    const menu = document.getElementById('language-menu')
    if (menu && !menu.contains(event.target as Node)) {
      languageMenuOpen.value = false
    }
  }
}

function toggleLanguageMenu() {
  languageMenuOpen.value = !languageMenuOpen.value

  // If menu is open, focus on the first menu item after the menu appears
  if (languageMenuOpen.value) {
    setTimeout(() => {
      const firstMenuItem = document.querySelector('#language-menu button')
      if (firstMenuItem) {
        (firstMenuItem as HTMLElement).focus()
      }
    }, 10)

    // Announce for screen readers that the menu is open
    const announcer = getOrCreateAnnouncer()
    announcer.textContent = 'Language menu opened. Use up and down arrow keys to navigate, Enter to select a language, Escape to close menu.'
  }
}

function closeLanguageMenu() {
  languageMenuOpen.value = false
  // Return focus to button when closing the menu
  if (languageMenuButtonRef.value) {
    languageMenuButtonRef.value.focus()
  }

  // Announce for screen readers that the menu is closed
  const announcer = getOrCreateAnnouncer()
  announcer.textContent = 'Language menu closed.'
}

// Helper to get or create the aria live announcer element
function getOrCreateAnnouncer() {
  let announcer = document.getElementById('aria-live-announcer')
  if (!announcer) {
    announcer = document.createElement('div')
    announcer.id = 'aria-live-announcer'
    announcer.setAttribute('aria-live', 'polite')
    announcer.setAttribute('class', 'sr-only')
    document.body.appendChild(announcer)
  }
  return announcer
}

function selectLanguage(localeCode: string) {
  locale.value = localeCode
  localStorage.setItem('locale', localeCode)
  languageMenuOpen.value = false
}

function getLocaleCode(locale: any): string {
  return typeof locale === 'string' ? locale : locale.code
}

function getLocaleName(locale: any): string {
  if (typeof locale === 'string') {
    return locale
  }
  return locale.name || locale.code
}

function getCurrentLocaleName(): string {
  const currentLoc = availableLocales.value.find(loc => {
    return typeof loc === 'string' 
      ? loc === locale.value 
      : loc.code === locale.value
  })
  
  return getLocaleName(currentLoc || locale.value)
}
</script>
<template>
  <div class="inline-block">
    <img 
      v-if="isDark" 
      src="~/assets/logo-dark.png"
      alt="UppdragsRadarn" 
      :class="sizeClass" 
    />
    <img 
      v-else 
      src="~/assets/logo-light.png"
      alt="UppdragsRadarn" 
      :class="sizeClass" 
    />
  </div>
</template>

<script setup lang="ts">
import {computed} from 'vue'
import useTheme from '~/composables/useTheme'

const props = defineProps({
  size: {
    type: String,
    default: 'medium' // 'small', 'medium', 'large'
  }
})

const { isDark } = useTheme()

const sizeClass = computed(() => {
  switch (props.size) {
    case 'tiny':
      return 'w-24 mx-auto'
    case 'small':
      return 'w-32 mx-auto'
    case 'large':
      return 'w-64 mx-auto'
    case 'medium':
    default:
      return 'w-48 mx-auto'
  }
})
</script>
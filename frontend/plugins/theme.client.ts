import useTheme from '~/composables/useTheme'

export default defineNuxtPlugin({
  name: 'theme-plugin',
  setup() {
    // Use the theme composable to initialize theme on app start
    // No need to store the return value since the composable manages the state internally
    // and will initialize when imported
    useTheme()
  }
})
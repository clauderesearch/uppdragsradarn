import type {Config} from 'tailwindcss'

export default <Config>{
  content: [
    "./components/**/*.{js,vue,ts}",
    "./layouts/**/*.vue",
    "./pages/**/*.vue",
    "./plugins/**/*.{js,ts}",
    "./app.vue",
    "./error.vue"
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        primary: {
          50: '#e6f4fc',
          100: '#cce9f9',
          200: '#99d3f3',
          300: '#66bded',
          400: '#33a7e6',
          500: '#008dd2', // Main brand color
          600: '#0071a8',
          700: '#00547e',
          800: '#003854',
          900: '#001c2a',
          950: '#0a0e10', // Dark mode background color - darker and more gray/black
        },
        textColor: {
          light: '#337b4c',  // Light mode text
          dark: '#ffffff'    // Dark mode text
        },
        bgColor: {
          light: '#fbfaf9',  // Light mode background
          dark: '#0a0e10'    // Dark mode background - darker and more gray/black
        }
      }
    }
  },
  plugins: [require('@tailwindcss/typography')]
}
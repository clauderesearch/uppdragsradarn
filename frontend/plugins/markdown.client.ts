// Plugin to enable trusted content with v-html by marking it as '__NUXT_TRUSTED__'
// This is important for rendering markdown content with v-html
// See: https://nuxt.com/docs/api/configuration/nuxt-config#content

export default defineNuxtPlugin((nuxtApp) => {
  nuxtApp.vueApp.config.compilerOptions = {
    isCustomElement: (tag) => tag === 'secure-content'
  };
});
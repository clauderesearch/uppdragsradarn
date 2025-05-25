<template>
  <div>
    <h1>Configuration Test</h1>
    <pre>{{ config }}</pre>
    <button @click="testFetch">Test API Call</button>
    <pre v-if="result">{{ result }}</pre>
  </div>
</template>

<script setup>
const config = useRuntimeConfig()
const result = ref(null)

const testFetch = async () => {
  try {
    const response = await fetch(`${config.public.apiBase}/api/session`, {
      credentials: 'include'
    })
    result.value = {
      url: `${config.public.apiBase}/api/session`,
      status: response.status,
      headers: [...response.headers.entries()]
    }
  } catch (error) {
    result.value = {
      error: error.message,
      apiBase: config.public.apiBase
    }
  }
}
</script>
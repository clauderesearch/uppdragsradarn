<template>
  <!-- This component doesn't render anything visible -->
</template>

<script setup lang="ts">
interface SeoProps {
  title?: string;
  description?: string;
  ogImage?: string;
  ogType?: 'website' | 'article';
  twitterCard?: 'summary' | 'summary_large_image';
  jsonLd?: Record<string, any>;
  canonicalUrl?: string;
}

const props = withDefaults(defineProps<SeoProps>(), {
  title: 'Uppdragsradarn',
  description: 'Hitta ditt nästa uppdrag på nolltid',
  ogImage: '/logo-og.png',
  ogType: 'website',
  twitterCard: 'summary',
  jsonLd: () => ({}),
  canonicalUrl: ''
});

// Get the base URL and full URL
const { locale } = useI18n();
const config = useRuntimeConfig();
const route = useRoute();
const baseUrl = process.client ? window.location.origin : 'https://uppdragsradarn.se';
const fullUrl = `${baseUrl}${route.fullPath}`;

// Compute the canonical URL based on props or current URL
const canonicalUrlFull = computed(() => {
  return props.canonicalUrl ? (props.canonicalUrl.startsWith('http') ? props.canonicalUrl : `${baseUrl}${props.canonicalUrl}`) : fullUrl;
});

// Setup head metadata
useHead({
  title: props.title,
  meta: [
    // Basic metadata
    { name: 'description', content: props.description },
    
    // Open Graph tags
    { property: 'og:title', content: props.title },
    { property: 'og:description', content: props.description },
    { property: 'og:type', content: props.ogType },
    { property: 'og:url', content: fullUrl },
    { property: 'og:image', content: props.ogImage.startsWith('http') ? props.ogImage : `${baseUrl}${props.ogImage}` },
    { property: 'og:locale', content: locale.value === 'sv' ? 'sv_SE' : 'en_US' },
    { property: 'og:site_name', content: 'Uppdragsradarn' },
    
    // Twitter card
    { name: 'twitter:card', content: props.twitterCard },
    { name: 'twitter:title', content: props.title },
    { name: 'twitter:description', content: props.description },
    { name: 'twitter:image', content: props.ogImage.startsWith('http') ? props.ogImage : `${baseUrl}${props.ogImage}` },
    
    // Additional metadata
    { name: 'format-detection', content: 'telephone=no' },
    { name: 'theme-color', content: '#337b4c' }
  ],
  link: [
    // Canonical URL
    { rel: 'canonical', href: canonicalUrlFull.value }
  ],
  // Add structured data for search engines if provided
  script: Object.keys(props.jsonLd).length > 0 
    ? [{ type: 'application/ld+json', children: JSON.stringify(props.jsonLd) }]
    : []
});

// Generate and provide JSON-LD structured data for the page
const structuredData = computed(() => {
  if (Object.keys(props.jsonLd).length > 0) {
    return props.jsonLd;
  }
  
  // Default structured data
  return {
    '@context': 'https://schema.org',
    '@type': 'WebSite',
    name: 'Uppdragsradarn',
    url: baseUrl,
    description: props.description
  };
});

// Add the structured data to the page
useHead({
  script: [
    {
      type: 'application/ld+json',
      children: JSON.stringify(structuredData.value)
    }
  ]
});
</script>
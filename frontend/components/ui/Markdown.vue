<template>
  <div 
    v-if="content" 
    class="prose max-w-none dark:prose-invert" 
    v-html="renderedContent"
    :aria-label="ariaLabel"
    :tabindex="tabindex"
  ></div>
  <div 
    v-else 
    class="text-gray-600 dark:text-gray-400 italic"
    :aria-label="ariaLabel"
    :tabindex="tabindex"
  >
    {{ emptyMessage }}
  </div>
</template>

<script setup lang="ts">
import {computed} from 'vue';
import {useMarkdown} from '../../composables/useMarkdown';

const props = defineProps({
  content: {
    type: String,
    default: ''
  },
  emptyMessage: {
    type: String,
    default: 'No content available'
  },
  ariaLabel: {
    type: String,
    default: ''
  },
  tabindex: {
    type: [String, Number],
    default: '0'
  }
});

const { renderMarkdown } = useMarkdown();

const renderedContent = computed(() => {
  return props.content ? renderMarkdown(props.content) : '';
});
</script>

<style scoped>
/* Additional styling for markdown content */
.prose h1 {
  font-size: 1.5rem;
  font-weight: bold;
  margin-top: 1.5rem;
  margin-bottom: 1rem;
}

.prose h2 {
  font-size: 1.25rem;
  font-weight: 600;
  margin-top: 1.25rem;
  margin-bottom: 0.75rem;
}

.prose h3 {
  font-size: 1.125rem;
  font-weight: 500;
  margin-top: 1rem;
  margin-bottom: 0.5rem;
}

.prose ul {
  list-style-type: disc;
  padding-left: 1.25rem;
  margin-top: 0.75rem;
  margin-bottom: 0.75rem;
}

.prose ol {
  list-style-type: decimal;
  padding-left: 1.25rem;
  margin-top: 0.75rem;
  margin-bottom: 0.75rem;
}

.prose p {
  margin-top: 0.75rem;
  margin-bottom: 0.75rem;
}

.prose a {
  color: #008dd2;
  text-decoration: underline;
}

.prose a:hover {
  color: #0071a8;
}

.dark .prose a {
  color: #66bded;
}

.dark .prose a:hover {
  color: #99d3f3;
}

.prose code {
  background-color: #f3f4f6;
  padding: 0.125rem 0.25rem;
  border-radius: 0.25rem;
}

.dark .prose code {
  background-color: #1f2937;
}

.prose blockquote {
  border-left-width: 4px;
  border-left-color: #d1d5db;
  padding-left: 1rem;
  font-style: italic;
}

.dark .prose blockquote {
  border-left-color: #4b5563;
}
</style>
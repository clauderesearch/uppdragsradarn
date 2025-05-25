<template>
  <div 
    v-if="content" 
    class="formatted-text text-gray-800 dark:text-gray-200" 
    :aria-label="ariaLabel"
    :tabindex="tabindex"
    v-html="formattedContent"
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

// Format the content to handle headers and structure
const formattedContent = computed(() => {
  if (!props.content) return '';
  
  // Escape HTML to prevent XSS
  const escapeHtml = (text: string) => {
    const map: Record<string, string> = {
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#039;'
    };
    return text.replace(/[&<>"']/g, m => map[m]);
  };
  
  // The backend formats headers in uppercase (e.g., "START:", "KOMPETENS:", etc.)
  // Let's enhance them with better styling
  const lines = props.content.split('\n');
  const formattedLines = lines.map(line => {
    const escapedLine = escapeHtml(line);
    
    // Check if line starts with an uppercase word followed by colon
    const headerMatch = escapedLine.match(/^([A-ZÅÄÖ][A-ZÅÄÖ\s]*):(.*)$/);
    if (headerMatch) {
      const [, header, content] = headerMatch;
      return `<span class="block font-semibold text-gray-900 dark:text-gray-100">${header}:</span>${content}`;
    }
    
    // Preserve empty lines for spacing
    if (escapedLine.trim() === '') {
      return '<br>';
    }
    
    return escapedLine;
  });
  
  return formattedLines.join('\n');
});
</script>

<style scoped>
/* Ensure proper spacing for the plain text content */
.formatted-text {
  white-space: pre-wrap;
  word-wrap: break-word;
  line-height: 1.6;
  font-size: 1rem;
}

/* Add some spacing after headers */
.formatted-text :deep(span.block) {
  margin-top: 1rem;
  margin-bottom: 0.25rem;
}

/* First header shouldn't have top margin */
.formatted-text :deep(span.block:first-child) {
  margin-top: 0;
}
</style>
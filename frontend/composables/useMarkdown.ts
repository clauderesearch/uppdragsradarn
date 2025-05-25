import MarkdownIt from 'markdown-it';

// Initialize markdown-it instance with desired options
const md = new MarkdownIt({
  html: true,        // Enable HTML tags in source (carefully sanitized by default)
  breaks: true,      // Convert '\n' in paragraphs into <br>
  linkify: true,     // Autoconvert URL-like text to links
  typographer: true  // Enable smartquotes and other typographic replacements
});

export function useMarkdown() {
  /**
   * Render markdown content to HTML
   * @param content Markdown content to render
   * @returns HTML string
   */
  const renderMarkdown = (content: string): string => {
    if (!content) return '';
    return md.render(content);
  };

  return {
    renderMarkdown
  };
}
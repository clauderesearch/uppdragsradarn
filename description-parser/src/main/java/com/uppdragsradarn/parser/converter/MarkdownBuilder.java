package com.uppdragsradarn.parser.converter;

import com.uppdragsradarn.parser.ParserOptions;

/**
 * Helper class for building well-formatted Markdown content.
 * Handles proper spacing, indentation, and structure.
 */
public class MarkdownBuilder {
    private final StringBuilder content;
    private final ParserOptions options;
    private boolean lastWasNewline;
    private boolean inParagraph;
    private boolean inList;
    private boolean inBlockquote;
    private int currentIndentLevel;
    
    public MarkdownBuilder(ParserOptions options) {
        this.content = new StringBuilder();
        this.options = options != null ? options : ParserOptions.builder().build();
        this.lastWasNewline = false; // Start with false so we don't trim initial content
        this.inParagraph = false;
        this.inList = false;
        this.inBlockquote = false;
        this.currentIndentLevel = 0;
    }
    
    public MarkdownBuilder append(String text) {
        if (text == null || text.isEmpty()) {
            return this;
        }
        
        // Clean whitespace if needed but preserve inline spacing  
        if (options.isCleanWhitespace()) {
            // Only trim at the absolute start of a new line if it's mostly whitespace
            String trimmed = text.trim();
            if (lastWasNewline && trimmed.length() > 0) {
                // Only trim if the text starts with whitespace
                if (text.startsWith(" ") || text.startsWith("\t")) {
                    text = text.replaceFirst("^\\s+", "");
                }
            } else {
                // Only compress multiple consecutive internal spaces
                // Preserve single spaces at boundaries for inline flow
                text = text.replaceAll("\\s{2,}", " ");
            }
        }
        
        if (!text.isEmpty()) {
            content.append(text);
            lastWasNewline = false;
        }
        
        return this;
    }
    
    public MarkdownBuilder newline() {
        if (!lastWasNewline) {
            content.append("\n");
            lastWasNewline = true;
        }
        return this;
    }
    
    public MarkdownBuilder ensureNewline() {
        if (!lastWasNewline) {
            newline();
        }
        return this;
    }
    
    public MarkdownBuilder ensureDoubleNewline() {
        ensureNewline();
        if (content.length() > 0 && !content.toString().endsWith("\n\n")) {
            newline();
        }
        // Don't set lastWasNewline=true here as it's already set by ensureNewline/newline
        return this;
    }
    
    public MarkdownBuilder startParagraph() {
        if (inParagraph) {
            endParagraph();
        }
        // Only add spacing if we have content already
        if (content.length() > 0) {
            ensureDoubleNewline();
        }
        inParagraph = true;
        return this;
    }
    
    public MarkdownBuilder endParagraph() {
        if (inParagraph) {
            ensureDoubleNewline();
            inParagraph = false;
        }
        return this;
    }
    
    public MarkdownBuilder startHeading(int level) {
        // Only add spacing if we have content already
        if (content.length() > 0) {
            ensureDoubleNewline();
        }
        
        // Respect max heading level
        level = Math.min(level, options.getMaxHeadingLevel());
        
        for (int i = 0; i < level; i++) {
            append("#");
        }
        append(" ");
        return this;
    }
    
    public MarkdownBuilder endHeading() {
        ensureDoubleNewline();
        return this;
    }
    
    public MarkdownBuilder startList() {
        if (!inList) {
            ensureDoubleNewline();
            inList = true;
        }
        return this;
    }
    
    public MarkdownBuilder endList() {
        if (inList) {
            ensureDoubleNewline();
            inList = false;
        }
        return this;
    }
    
    public MarkdownBuilder startListItem(int level, int ordinal) {
        ensureNewline();
        
        // Add indentation
        for (int i = 0; i < level; i++) {
            append("  ");
        }
        
        if (ordinal > 0) {
            append(ordinal + ". ");
        } else {
            append("- ");
        }
        
        currentIndentLevel = level;
        return this;
    }
    
    public MarkdownBuilder endListItem() {
        currentIndentLevel = 0;
        return this;
    }
    
    public MarkdownBuilder startBlockquote() {
        ensureDoubleNewline();
        inBlockquote = true;
        return this;
    }
    
    public MarkdownBuilder endBlockquote() {
        if (inBlockquote) {
            ensureDoubleNewline();
            inBlockquote = false;
        }
        return this;
    }
    
    public MarkdownBuilder startCodeBlock() {
        ensureDoubleNewline();
        append("```").newline();
        return this;
    }
    
    public MarkdownBuilder endCodeBlock() {
        ensureNewline();
        append("```").ensureDoubleNewline();
        return this;
    }
    
    public MarkdownBuilder lineBreak() {
        append("  ").newline();
        return this;
    }
    
    public MarkdownBuilder horizontalRule() {
        ensureDoubleNewline();
        append("---").ensureDoubleNewline();
        return this;
    }
    
    public MarkdownBuilder startTable() {
        ensureDoubleNewline();
        return this;
    }
    
    public MarkdownBuilder endTable() {
        ensureDoubleNewline();
        return this;
    }
    
    public boolean needsSpacing() {
        return !lastWasNewline && content.length() > 0;
    }
    
    @Override
    public String toString() {
        String result = content.toString();
        
        // Final cleanup
        if (options.isCleanWhitespace()) {
            // Remove trailing whitespace from lines (but be careful with inline markdown)
            // TODO: Need smarter whitespace handling that doesn't break inline markdown
            // result = result.replaceAll("[ \\t]+$", "");
            // result = result.replaceAll("(?m)[ \\t]+$", "");
            
            // Ensure no more than 2 consecutive newlines
            result = result.replaceAll("\n{3,}", "\n\n");
            
            // Trim the result (leading/trailing only)
            result = result.trim();
        }
        
        return result;
    }
}
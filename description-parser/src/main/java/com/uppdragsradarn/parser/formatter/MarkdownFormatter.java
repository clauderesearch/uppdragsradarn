package com.uppdragsradarn.parser.formatter;

import com.uppdragsradarn.parser.ParserOptions;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cleans up and fixes common formatting issues in Markdown content.
 * Language-agnostic formatting that focuses on structure and consistency.
 */
@Slf4j
public class MarkdownFormatter {
    
    // Common formatting patterns
    private static final Pattern MULTIPLE_BLANK_LINES = Pattern.compile("\n{3,}");
    private static final Pattern TRAILING_WHITESPACE = Pattern.compile("[ \t]+$", Pattern.MULTILINE);
    // Match list markers at start of line that aren't followed by a space (but not bold/italic)
    private static final Pattern BROKEN_LISTS = Pattern.compile("^(\\s*)([\\-\\+]|\\*(?!\\*))([^ ])", Pattern.MULTILINE);
    private static final Pattern BROKEN_ORDERED_LISTS = Pattern.compile("^(\\s*)(\\d+\\.)([^ ])", Pattern.MULTILINE);
    // Match bullet points (not bold/italic markers)
    private static final Pattern INCONSISTENT_BULLETS = Pattern.compile("^(\\s*)([\\*\\+])(\\s+)", Pattern.MULTILINE);
    private static final Pattern UNCLOSED_BOLD = Pattern.compile("\\*\\*([^*]+)$", Pattern.MULTILINE);
    private static final Pattern UNCLOSED_ITALIC = Pattern.compile("(?<!\\*)\\*([^*]+)$", Pattern.MULTILINE);
    private static final Pattern BROKEN_HEADINGS = Pattern.compile("^(#+)([^ #])", Pattern.MULTILINE);
    private static final Pattern EXCESSIVE_HEADING_MARKS = Pattern.compile("^(#{7,})", Pattern.MULTILINE);
    private static final Pattern BROKEN_LINKS = Pattern.compile("\\[([^\\]]+)\\](?!\\()");
    // Match multiple spaces that are truly consecutive (not around inline markdown)
    private static final Pattern MULTIPLE_SPACES = Pattern.compile("(?<!\\*) {2,}(?!\\*)");
    
    public String format(String markdown) {
        return format(markdown, null);
    }
    
    public String format(String markdown, ParserOptions options) {
        if (markdown == null || markdown.trim().isEmpty()) {
            return "";
        }
        
        String result = markdown;
        
        if (options == null || options.isFixFormatting()) {
            // Fix common formatting issues
            result = fixLists(result);
            result = fixHeadings(result);
            result = fixEmphasis(result);
            result = fixLinks(result);
            result = fixWhitespace(result);
            result = fixSpecialCharacters(result);
        }
        
        if (options == null || options.isCleanWhitespace()) {
            result = cleanWhitespace(result);
        }
        
        // Final trim
        result = result.trim();
        
        log.debug("Formatted markdown: input length {}, output length {}", 
                  markdown.length(), result.length());
        
        return result;
    }
    
    private String fixLists(String markdown) {
        // Fix missing space after bullet points
        markdown = BROKEN_LISTS.matcher(markdown).replaceAll("$1$2 $3");
        
        // Fix missing space after numbered lists
        markdown = BROKEN_ORDERED_LISTS.matcher(markdown).replaceAll("$1$2 $3");
        
        // Standardize bullet points to use '-'
        markdown = INCONSISTENT_BULLETS.matcher(markdown).replaceAll("$1-$3");
        
        return markdown;
    }
    
    private String fixHeadings(String markdown) {
        // Fix missing space after heading marks
        markdown = BROKEN_HEADINGS.matcher(markdown).replaceAll("$1 $2");
        
        // Limit heading marks to 6
        markdown = EXCESSIVE_HEADING_MARKS.matcher(markdown).replaceAll("######");
        
        // Ensure blank line before headings (except at start)
        markdown = markdown.replaceAll("(?<!^|\n\n)(^#{1,6} )", "\n\n$1");
        
        return markdown;
    }
    
    private String fixEmphasis(String markdown) {
        // Skip fixing emphasis for now - it's breaking correct markdown
        // TODO: Implement smarter emphasis fixing that doesn't break correct markdown
        return markdown;
    }
    
    private String fixLinks(String markdown) {
        // Fix broken link syntax (add empty href)
        markdown = BROKEN_LINKS.matcher(markdown).replaceAll("[$1]()");
        
        // Fix common link mistakes
        markdown = markdown.replaceAll("\\[([^\\]]+)\\]\\s+\\(([^)]+)\\)", "[$1]($2)");
        
        return markdown;
    }
    
    private String fixWhitespace(String markdown) {
        // Remove trailing whitespace from lines (but preserve spaces around inline markdown)
        // TODO: This needs to be smarter about inline markdown elements
        // markdown = TRAILING_WHITESPACE.matcher(markdown).replaceAll("");
        
        // Fix multiple spaces (except for line breaks and around markdown markers)
        markdown = MULTIPLE_SPACES.matcher(markdown).replaceAll(" ");
        
        // Ensure consistent line endings
        markdown = markdown.replaceAll("\r\n", "\n");
        markdown = markdown.replaceAll("\r", "\n");
        
        return markdown;
    }
    
    private String fixSpecialCharacters(String markdown) {
        // Fix common HTML entities that might have been missed
        markdown = markdown.replaceAll("&nbsp;", " ");
        markdown = markdown.replaceAll("&amp;", "&");
        markdown = markdown.replaceAll("&lt;", "<");
        markdown = markdown.replaceAll("&gt;", ">");
        markdown = markdown.replaceAll("&quot;", "\"");
        markdown = markdown.replaceAll("&apos;", "'");
        
        // Fix smart quotes and dashes
        markdown = markdown.replaceAll("[\u201C\u201D]", "\"");
        markdown = markdown.replaceAll("[\u2018\u2019]", "'");
        markdown = markdown.replaceAll("\u2013", "-");
        markdown = markdown.replaceAll("\u2014", "--");
        markdown = markdown.replaceAll("\u2026", "...");
        
        return markdown;
    }
    
    private String cleanWhitespace(String markdown) {
        // Reduce multiple blank lines to maximum of 2
        markdown = MULTIPLE_BLANK_LINES.matcher(markdown).replaceAll("\n\n");
        
        // Ensure proper spacing around block elements
        markdown = markdown.replaceAll("(?<!\n\n)(^```)", "\n\n$1");
        markdown = markdown.replaceAll("(```$)(?!\n\n)", "$1\n\n");
        
        // Ensure proper spacing around horizontal rules
        markdown = markdown.replaceAll("(?<!\n\n)(^---+$)", "\n\n$1");
        markdown = markdown.replaceAll("(^---+$)(?!\n\n)", "$1\n\n");
        
        return markdown;
    }
}
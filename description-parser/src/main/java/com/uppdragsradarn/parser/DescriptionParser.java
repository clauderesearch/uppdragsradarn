package com.uppdragsradarn.parser;

import com.uppdragsradarn.parser.converter.HtmlToMarkdownConverter;
import com.uppdragsradarn.parser.converter.JsonToMarkdownConverter;
import com.uppdragsradarn.parser.detector.PIIDetector;
import com.uppdragsradarn.parser.detector.PIIDetectionResult;
import com.uppdragsradarn.parser.formatter.MarkdownFormatter;
import com.uppdragsradarn.parser.model.ParseResult;
import lombok.extern.slf4j.Slf4j;

/**
 * Language-agnostic parser that converts various input formats (HTML, JSON, plain text)
 * into clean, well-structured Markdown suitable for display.
 */
@Slf4j
public class DescriptionParser {
    
    private final HtmlToMarkdownConverter htmlConverter;
    private final JsonToMarkdownConverter jsonConverter;
    private final MarkdownFormatter formatter;
    private final PIIDetector piiDetector;
    
    public DescriptionParser() {
        this.htmlConverter = new HtmlToMarkdownConverter();
        this.jsonConverter = new JsonToMarkdownConverter();
        this.formatter = new MarkdownFormatter();
        this.piiDetector = new PIIDetector();
    }
    
    /**
     * Parses input content into clean Markdown format.
     * Automatically detects the input type (HTML, JSON, or plain text).
     * @deprecated Use parseWithPIIDetection instead to get PII detection results
     *
     * @param content The raw content to parse
     * @return Clean, formatted Markdown
     */
    @Deprecated
    public String parse(String content) {
        if (content == null || content.trim().isEmpty()) {
            log.warn("Empty content provided");
            return "";
        }
        
        log.debug("Parsing content of length: {}", content.length());
        
        String markdown;
        
        // Detect content type and convert accordingly
        if (isJson(content)) {
            log.debug("Detected JSON content");
            markdown = jsonConverter.convert(content);
        } else if (isHtml(content)) {
            log.debug("Detected HTML content");
            markdown = htmlConverter.convert(content);
        } else {
            log.debug("Treating as plain text");
            markdown = content;
        }
        
        // Apply formatting fixes
        String formatted = formatter.format(markdown);
        
        log.debug("Parsing complete: input length {}, output length {}", 
                  content.length(), formatted.length());
        
        return formatted;
    }
    
    /**
     * Parses input content and detects potential PII.
     * Returns a ParseResult containing both the parsed content and PII detection results.
     *
     * @param content The raw content to parse
     * @return ParseResult with formatted Markdown and PII detection information
     */
    public ParseResult parseWithPIIDetection(String content) {
        if (content == null || content.trim().isEmpty()) {
            log.warn("Empty content provided");
            return ParseResult.success("");
        }
        
        log.debug("Parsing content with PII detection, length: {}", content.length());
        
        // First, parse the content normally
        String parsedContent = parse(content);
        
        // Then perform PII detection
        PIIDetectionResult piiResult = piiDetector.detect(parsedContent);
        
        if (piiResult.containsPII()) {
            log.warn("PII detected in content: {} matches found", piiResult.getMatches().size());
        }
        
        return ParseResult.withPII(parsedContent, piiResult);
    }
    
    /**
     * Parses content with provider-specific hints.
     * Providers can pass options to influence parsing behavior.
     * @deprecated Use parseWithPIIDetection instead to get PII detection results
     *
     * @param content The raw content to parse
     * @param options Provider-specific parsing options
     * @return Clean, formatted Markdown
     */
    @Deprecated
    public String parse(String content, ParserOptions options) {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }
        
        String markdown;
        
        // Apply any pre-processing based on options
        String preprocessed = content;
        if (options != null && options.getPreprocessor() != null) {
            preprocessed = options.getPreprocessor().apply(content);
        }
        
        // Convert to markdown
        if (isJson(preprocessed)) {
            markdown = jsonConverter.convert(preprocessed, options);
        } else if (isHtml(preprocessed)) {
            markdown = htmlConverter.convert(preprocessed, options);
        } else {
            markdown = preprocessed;
        }
        
        // Apply formatting fixes
        return formatter.format(markdown, options);
    }
    
    /**
     * Parses content with provider-specific hints and detects potential PII.
     * Returns a ParseResult containing both the parsed content and PII detection results.
     *
     * @param content The raw content to parse
     * @param options Provider-specific parsing options
     * @return ParseResult with formatted Markdown and PII detection information
     */
    public ParseResult parseWithPIIDetection(String content, ParserOptions options) {
        if (content == null || content.trim().isEmpty()) {
            return ParseResult.success("");
        }
        
        // First, parse the content normally
        String parsedContent = parse(content, options);
        
        // Then perform PII detection
        PIIDetectionResult piiResult = piiDetector.detect(parsedContent);
        
        if (piiResult.containsPII()) {
            log.warn("PII detected in content: {} matches found", piiResult.getMatches().size());
        }
        
        return ParseResult.withPII(parsedContent, piiResult);
    }
    
    private boolean isJson(String content) {
        String trimmed = content.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
               (trimmed.startsWith("[") && trimmed.endsWith("]"));
    }
    
    private boolean isHtml(String content) {
        String trimmed = content.trim().toLowerCase();
        return trimmed.contains("<") && trimmed.contains(">") &&
               (trimmed.contains("<p") || trimmed.contains("<div") || 
                trimmed.contains("<span") || trimmed.contains("<h") ||
                trimmed.contains("<ul") || trimmed.contains("<ol") ||
                trimmed.contains("<br") || trimmed.contains("<strong") ||
                trimmed.contains("<b>") || trimmed.contains("<i>") ||
                trimmed.contains("<em"));
    }
}
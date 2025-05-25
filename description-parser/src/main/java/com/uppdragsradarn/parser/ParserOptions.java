package com.uppdragsradarn.parser;

import lombok.Builder;
import lombok.Data;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Configuration options for the parser.
 * Providers can use this to influence parsing behavior without
 * modifying the core parser logic.
 */
@Data
@Builder
public class ParserOptions {
    
    /**
     * Optional preprocessor function to apply before parsing.
     * Useful for provider-specific content extraction.
     */
    private Function<String, String> preprocessor;
    
    /**
     * HTML elements to preserve as-is (don't convert to Markdown).
     * Useful for preserving custom styling or layouts.
     */
    @Builder.Default
    private Set<String> preserveElements = Set.of();
    
    /**
     * HTML elements to remove entirely.
     * Useful for filtering out navigation, ads, etc.
     */
    @Builder.Default
    private Set<String> removeElements = Set.of("script", "style", "nav", "header", "footer");
    
    /**
     * CSS selectors for elements to extract.
     * If provided, only elements matching these selectors will be processed.
     */
    @Builder.Default
    private Set<String> extractSelectors = Set.of();
    
    /**
     * CSS selectors for elements to exclude.
     * Elements matching these selectors will be removed.
     */
    @Builder.Default
    private Set<String> excludeSelectors = Set.of();
    
    /**
     * Custom attribute mappings for JSON conversion.
     * Maps JSON field names to Markdown section headers.
     */
    @Builder.Default
    private Map<String, String> jsonFieldMappings = Map.of();
    
    /**
     * Whether to preserve empty lines in the output.
     */
    @Builder.Default
    private boolean preserveEmptyLines = false;
    
    /**
     * Whether to preserve original indentation.
     */
    @Builder.Default
    private boolean preserveIndentation = false;
    
    /**
     * Maximum heading level to use (1-6).
     * Deeper headings will be adjusted to this level.
     */
    @Builder.Default
    private int maxHeadingLevel = 3;
    
    /**
     * Whether to convert relative URLs to absolute URLs.
     */
    @Builder.Default
    private boolean resolveUrls = false;
    
    /**
     * Base URL for resolving relative URLs.
     */
    private String baseUrl;
    
    /**
     * Whether to clean up excessive whitespace.
     */
    @Builder.Default
    private boolean cleanWhitespace = true;
    
    /**
     * Whether to fix common formatting issues.
     */
    @Builder.Default
    private boolean fixFormatting = true;
}
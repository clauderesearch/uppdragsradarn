package com.uppdragsradarn.parser.model;

import lombok.Builder;
import lombok.Data;

/**
 * Represents a structured section in a parsed document.
 * Used internally by converters to track document structure.
 */
@Data
@Builder
public class Section {
    private String title;
    private String content;
    private int level;
    private SectionType type;
    
    public enum SectionType {
        HEADING,
        PARAGRAPH,
        LIST,
        CODE_BLOCK,
        BLOCKQUOTE,
        TABLE,
        UNKNOWN
    }
}
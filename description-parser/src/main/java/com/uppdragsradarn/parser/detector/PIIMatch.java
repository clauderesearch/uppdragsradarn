package com.uppdragsradarn.parser.detector;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Represents a single PII match in text.
 */
@Data
@AllArgsConstructor
public class PIIMatch {
    private final String type;
    private final String value;
    private final int start;
    private final int end;
}
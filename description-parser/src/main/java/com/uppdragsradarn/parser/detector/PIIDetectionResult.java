package com.uppdragsradarn.parser.detector;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * Result of PII detection in text.
 */
@Data
@AllArgsConstructor
public class PIIDetectionResult {
    private final boolean hasPII;
    private final List<PIIMatch> matches;
    
    public static PIIDetectionResult empty() {
        return new PIIDetectionResult(false, List.of());
    }
    
    public boolean containsPII() {
        return hasPII;
    }
    
    /**
     * Gets a summary of the PII types found.
     */
    public String getSummary() {
        if (!hasPII) {
            return "No PII detected";
        }
        
        return matches.stream()
            .map(PIIMatch::getType)
            .distinct()
            .reduce((a, b) -> a + ", " + b)
            .orElse("");
    }
}
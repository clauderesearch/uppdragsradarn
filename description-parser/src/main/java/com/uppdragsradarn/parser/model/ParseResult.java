package com.uppdragsradarn.parser.model;

import com.uppdragsradarn.parser.detector.PIIDetectionResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParseResult {
    private String parsedContent;
    private PIIDetectionResult piiDetectionResult;
    private boolean needsManualReview;
    
    public static ParseResult success(String content) {
        return new ParseResult(content, PIIDetectionResult.empty(), false);
    }
    
    public static ParseResult withPII(String content, PIIDetectionResult piiResult) {
        return new ParseResult(content, piiResult, piiResult.containsPII());
    }
}
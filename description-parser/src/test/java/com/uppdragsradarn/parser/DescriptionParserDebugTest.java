package com.uppdragsradarn.parser;

import org.junit.jupiter.api.Test;

public class DescriptionParserDebugTest {
    
    private final DescriptionParser parser = new DescriptionParser();
    
    @Test
    public void debugHtmlParsing() {
        String html = """
            <p>This is a <strong>great</strong> opportunity for a <em>skilled</em> developer.</p>
            """;
            
        String result = parser.parse(html);
        System.out.println("=== HTML DEBUG ===");
        System.out.println("Input: " + html);
        System.out.println("Output: " + result);
        System.out.println("Output escaped: " + result.replace(" ", "·"));
    }
    
    @Test
    public void debugJsonParsing() {
        String json = """
            {
                "title": "Software Engineer",
                "description": "We are looking for a talented engineer"
            }
            """;
            
        String result = parser.parse(json);
        System.out.println("=== JSON DEBUG ===");
        System.out.println("Input: " + json);
        System.out.println("Output: " + result);
        System.out.println("Output escaped: " + result.replace(" ", "·"));
    }
}
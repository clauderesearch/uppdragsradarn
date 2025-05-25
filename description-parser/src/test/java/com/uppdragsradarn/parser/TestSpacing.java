package com.uppdragsradarn.parser;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class TestSpacing {
    
    private final DescriptionParser parser = new DescriptionParser();
    
    @Test
    public void testSimpleMarkdownConversion() {
        String html = "<p>This is a <strong>test</strong> case</p>";
        
        String result = parser.parse(html);
        System.out.println("Result: '" + result + "'");
        System.out.println("Result length: " + result.length());
        System.out.println("Expected: 'This is a **test** case'");
        System.out.println("Expected length: " + "This is a **test** case".length());
        
        // Character by character comparison
        String expected = "This is a **test** case";
        for (int i = 0; i < Math.max(result.length(), expected.length()); i++) {
            char r = i < result.length() ? result.charAt(i) : '?';
            char e = i < expected.length() ? expected.charAt(i) : '?';
            System.out.printf("pos %2d: result='%c' expected='%c' %s\n", i, r, e, r == e ? "OK" : "DIFF");
        }
        
        assertThat(result).isEqualTo("This is a **test** case");
    }
}
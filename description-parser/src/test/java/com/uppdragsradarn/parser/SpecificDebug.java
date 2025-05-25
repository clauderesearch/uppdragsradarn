package com.uppdragsradarn.parser;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class SpecificDebug {
    
    @Test
    public void testSpecificHtml() {
        String html = "<p>This is a <strong>great</strong> opportunity</p>";
        
        DescriptionParser parser = new DescriptionParser();
        String result = parser.parse(html);
        
        System.out.println("HTML: " + html);
        System.out.println("Result: '" + result + "'");
        System.out.println("Result characters:");
        for (int i = 0; i < result.length(); i++) {
            System.out.printf("  [%d]: '%c' (%d)\n", i, result.charAt(i), (int)result.charAt(i));
        }
        
        assertThat(result).contains("This is a **great** opportunity");
    }
}
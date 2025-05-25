package com.uppdragsradarn.parser;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class TestSimple {
    
    @Test
    public void testVerySimple() {
        DescriptionParser parser = new DescriptionParser();
        String result = parser.parse("<strong>test</strong>");
        System.out.println("Result: '" + result + "'");
        assertThat(result).isEqualTo("**test**");
    }
    
    @Test
    public void testWithSpaces() {
        DescriptionParser parser = new DescriptionParser();
        String result = parser.parse("before <strong>test</strong> after");
        System.out.println("Result: '" + result + "'");
        System.out.println("Result length: " + result.length());
        System.out.println("Expected length: " + "before **test** after".length());
        // Check if it has extra asterisks first
        assertThat(result.trim()).isEqualTo("before **test** after");
    }
}
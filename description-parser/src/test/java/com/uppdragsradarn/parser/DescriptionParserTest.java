package com.uppdragsradarn.parser;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class DescriptionParserTest {
    
    private final DescriptionParser parser = new DescriptionParser();
    
    @Test
    public void testHtmlToMarkdown() {
        String html = """
            <h1>Job Title</h1>
            <p>This is a <strong>great</strong> opportunity for a <em>skilled</em> developer.</p>
            <h2>Requirements</h2>
            <ul>
                <li>5+ years experience</li>
                <li>Java knowledge</li>
            </ul>
            """;
            
        String result = parser.parse(html);
        
        assertThat(result).contains("# Job Title");
        assertThat(result).contains("This is a **great** opportunity for a *skilled* developer.");
        assertThat(result).contains("## Requirements");
        assertThat(result).contains("- 5+ years experience");
        assertThat(result).contains("- Java knowledge");
    }
    
    @Test
    public void testJsonToMarkdown() {
        String json = """
            {
                "title": "Software Engineer",
                "description": "We are looking for a talented engineer",
                "requirements": ["Java", "Spring", "SQL"],
                "salary": {
                    "min": 50000,
                    "max": 80000,
                    "currency": "USD"
                }
            }
            """;
            
        String result = parser.parse(json);
        
        assertThat(result).contains("**Title:** Software Engineer");
        assertThat(result).contains("**Description:** We are looking for a talented engineer");
        assertThat(result).contains("**Requirements:**");
        assertThat(result).contains("- Java");
        System.out.println("JSON Result:");
        System.out.println(result);
        System.out.println("---");
        
        assertThat(result).contains("Salary");
        assertThat(result).contains("Currency");
        assertThat(result).contains("USD");
    }
    
    @Test
    public void testPlainText() {
        String text = "This is plain text\nWith multiple lines\n\nAnd paragraphs";
        
        String result = parser.parse(text);
        
        assertThat(result).isEqualTo(text);
    }
}
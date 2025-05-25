package com.uppdragsradarn.parser;

import com.uppdragsradarn.parser.converter.HtmlToMarkdownConverter;
import com.uppdragsradarn.parser.ParserOptions;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class TestDebug {
    
    @Test
    public void testJustHtmlConversion() {
        HtmlToMarkdownConverter converter = new HtmlToMarkdownConverter();
        String html = "<p>This is a <strong>test</strong> case</p>";
        
        // Test with no whitespace cleaning
        ParserOptions options = ParserOptions.builder()
            .cleanWhitespace(false)
            .build();
        
        String result = converter.convert(html, options);
        System.out.println("HTML conversion result: '" + result + "'");
        
        assertThat(result.trim()).isEqualTo("This is a **test** case");
    }
}
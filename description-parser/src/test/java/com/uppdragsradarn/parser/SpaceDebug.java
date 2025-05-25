package com.uppdragsradarn.parser;

import com.uppdragsradarn.parser.converter.HtmlToMarkdownConverter;
import com.uppdragsradarn.parser.converter.MarkdownBuilder;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class SpaceDebug {
    
    @Test
    public void testBuilderAppend() {
        ParserOptions options = ParserOptions.builder().build();
        MarkdownBuilder builder = new MarkdownBuilder(options);
        
        builder.append("before ");
        builder.append("**");
        builder.append("test");
        builder.append("**");
        builder.append(" after");
        
        String result = builder.toString();
        System.out.println("Builder result: '" + result + "'");
        System.out.println("Length: " + result.length());
        
        assertThat(result).isEqualTo("before **test** after");
    }
    
    @Test
    public void testMarkdownBuilder() {
        HtmlToMarkdownConverter converter = new HtmlToMarkdownConverter();
        String html = "before <strong>test</strong> after";
        
        ParserOptions options = ParserOptions.builder()
            .cleanWhitespace(true)
            .build();
        
        String result = converter.convert(html, options);
        System.out.println("Result: '" + result + "'");
        System.out.println("Length: " + result.length());
        
        assertThat(result).isEqualTo("before **test** after");
    }
}
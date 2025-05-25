package com.uppdragsradarn.parser;

import com.uppdragsradarn.parser.converter.HtmlToMarkdownConverter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

public class InlineDebug {
    
    @Test
    public void debugInline() {
        String html = "before <strong>test</strong> after";
        Document doc = Jsoup.parse(html);
        
        System.out.println("HTML: " + html);
        System.out.println("Body HTML: " + doc.body().html());
        System.out.println("Body text: " + doc.body().text());
        System.out.println("Body childNodes: " + doc.body().childNodes());
        
        HtmlToMarkdownConverter converter = new HtmlToMarkdownConverter();
        ParserOptions options = ParserOptions.builder()
            .cleanWhitespace(false)
            .fixFormatting(false)
            .build();
        String result = converter.convert(html, options);
        System.out.println("Converter result: '" + result + "'");
    }
}
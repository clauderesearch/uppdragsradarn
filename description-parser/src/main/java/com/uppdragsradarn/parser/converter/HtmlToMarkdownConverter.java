package com.uppdragsradarn.parser.converter;

import com.uppdragsradarn.parser.ParserOptions;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import lombok.extern.slf4j.Slf4j;

import java.util.StringJoiner;

/**
 * Converts HTML content to clean Markdown format.
 * Preserves structure, headings, lists, and formatting.
 */
@Slf4j
public class HtmlToMarkdownConverter {
    
    public String convert(String html) {
        return convert(html, null);
    }
    
    public String convert(String html, ParserOptions options) {
        if (html == null || html.trim().isEmpty()) {
            return "";
        }
        
        Document doc = Jsoup.parse(html);
        
        // Apply extraction selectors if provided
        if (options != null && !options.getExtractSelectors().isEmpty()) {
            Elements extracted = new Elements();
            for (String selector : options.getExtractSelectors()) {
                extracted.addAll(doc.select(selector));
            }
            doc.body().empty().append(extracted.outerHtml());
        }
        
        // Remove excluded elements
        if (options != null) {
            // Remove by element names
            for (String tag : options.getRemoveElements()) {
                doc.select(tag).remove();
            }
            
            // Remove by CSS selectors
            for (String selector : options.getExcludeSelectors()) {
                doc.select(selector).remove();
            }
        }
        
        // Convert to markdown
        MarkdownBuilder builder = new MarkdownBuilder(options);
        processNode(doc.body(), builder, 0);
        
        return builder.toString();
    }
    
    private void processNode(Node node, MarkdownBuilder builder, int listLevel) {
        if (node instanceof TextNode) {
            TextNode textNode = (TextNode) node;
            String text = textNode.text();
            
            
            // Always preserve text content, including spaces
            if (!text.isEmpty()) {
                builder.append(text);
            }
        } else if (node instanceof Element) {
            Element element = (Element) node;
            String tagName = element.tagName().toLowerCase();
            
            switch (tagName) {
                case "p":
                    builder.startParagraph();
                    processChildren(element, builder, listLevel);
                    builder.endParagraph();
                    break;
                    
                case "h1":
                case "h2":
                case "h3":
                case "h4":
                case "h5":
                case "h6":
                    int level = Character.getNumericValue(tagName.charAt(1));
                    builder.startHeading(level);
                    processChildren(element, builder, listLevel);
                    builder.endHeading();
                    break;
                    
                case "strong":
                case "b":
                    // Process inline element while preserving text flow
                    processInlineElement(element, builder, "**", listLevel);
                    break;
                    
                case "em":
                case "i":
                    // Process inline element while preserving text flow
                    processInlineElement(element, builder, "*", listLevel);
                    break;
                    
                case "code":
                    // Process inline element while preserving text flow
                    processInlineElement(element, builder, "`", listLevel);
                    break;
                    
                case "pre":
                    builder.startCodeBlock();
                    builder.append(element.text());
                    builder.endCodeBlock();
                    break;
                    
                case "br":
                    builder.lineBreak();
                    break;
                    
                case "hr":
                    builder.horizontalRule();
                    break;
                    
                case "ul":
                    builder.startList();
                    processListItems(element, builder, listLevel, false);
                    builder.endList();
                    break;
                    
                case "ol":
                    builder.startList();
                    processListItems(element, builder, listLevel, true);
                    builder.endList();
                    break;
                    
                case "li":
                    // Handled by processListItems
                    break;
                    
                case "a":
                    String href = element.attr("href");
                    String text = element.text();
                    if (!text.isEmpty()) {
                        builder.append("[").append(text).append("]");
                        if (!href.isEmpty()) {
                            builder.append("(").append(href).append(")");
                        }
                    }
                    break;
                    
                case "img":
                    String src = element.attr("src");
                    String alt = element.attr("alt");
                    if (!src.isEmpty()) {
                        builder.append("![").append(alt).append("](").append(src).append(")");
                    }
                    break;
                    
                case "blockquote":
                    builder.startBlockquote();
                    processChildren(element, builder, listLevel);
                    builder.endBlockquote();
                    break;
                    
                case "table":
                    processTable(element, builder);
                    break;
                    
                case "div":
                case "section":
                case "article":
                case "main":
                case "aside":
                    // Process children with implicit paragraph breaks
                    if (builder.needsSpacing()) {
                        builder.ensureDoubleNewline();
                    }
                    processChildren(element, builder, listLevel);
                    if (builder.needsSpacing()) {
                        builder.ensureDoubleNewline();
                    }
                    break;
                    
                default:
                    // For unknown elements, just process children
                    processChildren(element, builder, listLevel);
                    break;
            }
        }
    }
    
    private void processInlineElement(Element element, MarkdownBuilder builder, String marker, int listLevel) {
        // For inline elements, we need to wrap the content with markers
        builder.append(marker);
        processChildren(element, builder, listLevel);
        builder.append(marker);
    }
    
    private void processChildren(Element element, MarkdownBuilder builder, int listLevel) {
        for (Node child : element.childNodes()) {
            processNode(child, builder, listLevel);
        }
    }
    
    private void processListItems(Element list, MarkdownBuilder builder, int level, boolean ordered) {
        Elements items = list.select("> li");
        int index = 1;
        
        for (Element item : items) {
            builder.startListItem(level, ordered ? index++ : 0);
            
            // Process the list item content
            for (Node child : item.childNodes()) {
                if (child instanceof Element && ((Element) child).tagName().matches("ul|ol")) {
                    // Nested list
                    processNode(child, builder, level + 1);
                } else {
                    processNode(child, builder, level);
                }
            }
            
            builder.endListItem();
        }
    }
    
    private void processTable(Element table, MarkdownBuilder builder) {
        Elements rows = table.select("tr");
        if (rows.isEmpty()) return;
        
        builder.startTable();
        
        // Process header row if exists
        Elements headerCells = rows.first().select("th");
        if (!headerCells.isEmpty()) {
            for (Element cell : headerCells) {
                builder.append("| ").append(cell.text()).append(" ");
            }
            builder.append("|").newline();
            
            // Add separator row
            for (int i = 0; i < headerCells.size(); i++) {
                builder.append("|---");
            }
            builder.append("|").newline();
            
            // Skip the header row
            Elements dataRows = new Elements();
            for (int i = 1; i < rows.size(); i++) {
                dataRows.add(rows.get(i));
            }
            rows = dataRows;
        }
        
        // Process data rows
        for (Element row : rows) {
            Elements cells = row.select("td, th");
            for (Element cell : cells) {
                builder.append("| ").append(cell.text()).append(" ");
            }
            builder.append("|").newline();
        }
        
        builder.endTable();
    }
}
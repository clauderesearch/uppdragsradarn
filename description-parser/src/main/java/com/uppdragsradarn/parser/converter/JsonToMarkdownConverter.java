package com.uppdragsradarn.parser.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uppdragsradarn.parser.ParserOptions;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.Map;

/**
 * Converts JSON content to clean Markdown format.
 * Handles various JSON structures and formats them appropriately.
 */
@Slf4j
public class JsonToMarkdownConverter {
    
    private final ObjectMapper objectMapper;
    
    public JsonToMarkdownConverter() {
        this.objectMapper = new ObjectMapper();
    }
    
    public String convert(String json) {
        return convert(json, null);
    }
    
    public String convert(String json, ParserOptions options) {
        if (json == null || json.trim().isEmpty()) {
            return "";
        }
        
        try {
            JsonNode rootNode = objectMapper.readTree(json);
            MarkdownBuilder builder = new MarkdownBuilder(options);
            
            if (rootNode.isObject()) {
                convertObject(rootNode, builder, 0, options);
            } else if (rootNode.isArray()) {
                convertArray(rootNode, builder, 0, options);
            } else {
                // Simple value
                builder.append(rootNode.asText());
            }
            
            return builder.toString();
        } catch (Exception e) {
            log.error("Error parsing JSON: {}", e.getMessage());
            // Return original content if JSON parsing fails
            return json;
        }
    }
    
    private void convertObject(JsonNode node, MarkdownBuilder builder, int level, ParserOptions options) {
        Map<String, String> fieldMappings = options != null ? options.getJsonFieldMappings() : Map.of();
        
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String fieldName = field.getKey();
            JsonNode fieldValue = field.getValue();
            
            // Check if there's a custom mapping for this field
            String displayName = fieldMappings.getOrDefault(fieldName, formatFieldName(fieldName));
            
            if (fieldValue.isObject()) {
                // Nested object - create a section
                builder.startHeading(Math.min(level + 2, 3));
                builder.append(displayName);
                builder.endHeading();
                
                convertObject(fieldValue, builder, level + 1, options);
            } else if (fieldValue.isArray()) {
                // Array - create a list
                builder.startParagraph();
                builder.append("**").append(displayName).append(":**");
                builder.endParagraph();
                
                convertArray(fieldValue, builder, level, options);
            } else if (!fieldValue.isNull()) {
                // Simple value
                String value = fieldValue.asText();
                if (!value.isEmpty()) {
                    builder.startParagraph();
                    builder.append("**").append(displayName).append(":** ").append(value);
                    builder.endParagraph();
                }
            }
        }
    }
    
    private void convertArray(JsonNode array, MarkdownBuilder builder, int level, ParserOptions options) {
        if (array.size() == 0) {
            return;
        }
        
        builder.startList();
        
        for (JsonNode item : array) {
            if (item.isObject()) {
                // Complex object in list - format as list item with nested content
                builder.startListItem(0, 0);
                // Show object fields inline
                Iterator<Map.Entry<String, JsonNode>> fields = item.fields();
                StringBuilder itemText = new StringBuilder();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    if (itemText.length() > 0) {
                        itemText.append(", ");
                    }
                    itemText.append(formatFieldName(field.getKey())).append(": ").append(field.getValue().asText());
                }
                builder.append(itemText.toString());
                builder.endListItem();
            } else if (item.isArray()) {
                // Nested array - this is complex, so just show count
                builder.startListItem(0, 0);
                builder.append("[Array with " + item.size() + " items]");
                builder.endListItem();
            } else {
                // Simple value in list
                builder.startListItem(0, 0);
                builder.append(item.asText());
                builder.endListItem();
            }
        }
        
        builder.endList();
    }
    
    private String formatFieldName(String fieldName) {
        // Convert camelCase or snake_case to Title Case
        String formatted = fieldName.replaceAll("([a-z])([A-Z])", "$1 $2");
        formatted = formatted.replaceAll("_", " ");
        formatted = formatted.substring(0, 1).toUpperCase() + formatted.substring(1);
        return formatted;
    }
}
package com.uppdragsradarn.domain.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** JPA converter for Map<String, String> to/from JSON string */
@Converter
public class StringMapConverter implements AttributeConverter<Map<String, String>, String> {

  private static final Logger logger = LoggerFactory.getLogger(StringMapConverter.class);
  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public String convertToDatabaseColumn(Map<String, String> attribute) {
    if (attribute == null || attribute.isEmpty()) {
      return "{}";
    }

    try {
      return objectMapper.writeValueAsString(attribute);
    } catch (JsonProcessingException e) {
      logger.error("Error converting map to JSON: {}", e.getMessage(), e);
      return "{}";
    }
  }

  @Override
  public Map<String, String> convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.isEmpty() || "{}".equals(dbData)) {
      return new HashMap<>();
    }

    try {
      return objectMapper.readValue(dbData, new TypeReference<Map<String, String>>() {});
    } catch (IOException e) {
      logger.error("Error converting JSON to map: {}", e.getMessage(), e);
      return new HashMap<>();
    }
  }
}

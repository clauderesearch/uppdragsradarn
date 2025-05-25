package com.uppdragsradarn.infrastructure.service;

/**
 * Service interface for Large Language Model integration. Used for processing and cleaning job
 * descriptions and other text content.
 */
public interface LLMService {

  /**
   * Process text using an LLM with the given prompt
   *
   * @param prompt The full prompt including instructions and content
   * @return The processed text
   * @throws LLMProcessingException if processing fails
   */
  String processText(String prompt);

  /**
   * Process text with token limit
   *
   * @param prompt The full prompt including instructions and content
   * @param maxTokens Maximum tokens in response
   * @return The processed text
   * @throws LLMProcessingException if processing fails
   */
  String processText(String prompt, int maxTokens);

  /**
   * Process text with additional options
   *
   * @param prompt The full prompt including instructions and content
   * @param options Processing options
   * @return The processed text
   * @throws LLMProcessingException if processing fails
   */
  String processText(String prompt, LLMOptions options);

  /**
   * Check if the LLM service is available
   *
   * @return true if the service is available
   */
  boolean isAvailable();

  /** Options for LLM processing */
  class LLMOptions {
    private int maxTokens = 1000;
    private double temperature = 0.2;
    private String model = null;
    private boolean cacheResult = true;

    public int getMaxTokens() {
      return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
      this.maxTokens = maxTokens;
    }

    public double getTemperature() {
      return temperature;
    }

    public void setTemperature(double temperature) {
      this.temperature = temperature;
    }

    public String getModel() {
      return model;
    }

    public void setModel(String model) {
      this.model = model;
    }

    public boolean isCacheResult() {
      return cacheResult;
    }

    public void setCacheResult(boolean cacheResult) {
      this.cacheResult = cacheResult;
    }

    public static LLMOptions defaults() {
      return new LLMOptions();
    }

    public LLMOptions withMaxTokens(int maxTokens) {
      this.maxTokens = maxTokens;
      return this;
    }

    public LLMOptions withTemperature(double temperature) {
      this.temperature = temperature;
      return this;
    }

    public LLMOptions withModel(String model) {
      this.model = model;
      return this;
    }

    public LLMOptions withoutCache() {
      this.cacheResult = false;
      return this;
    }
  }

  /** Exception thrown when LLM processing fails */
  class LLMProcessingException extends RuntimeException {
    public LLMProcessingException(String message) {
      super(message);
    }

    public LLMProcessingException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}

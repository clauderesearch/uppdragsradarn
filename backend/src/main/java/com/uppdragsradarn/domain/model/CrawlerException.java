package com.uppdragsradarn.domain.model;

/** Exception thrown by crawler operations */
public class CrawlerException extends RuntimeException {

  public CrawlerException(String message) {
    super(message);
  }

  public CrawlerException(String message, Throwable cause) {
    super(message, cause);
  }
}

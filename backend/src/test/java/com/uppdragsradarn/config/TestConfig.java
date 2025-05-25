package com.uppdragsradarn.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

/** Test configuration for crawler tests and other integration tests */
@TestConfiguration
public class TestConfig {

  /**
   * Creates a test RestTemplate bean that can be used in tests
   *
   * @return RestTemplate configured for testing
   */
  @Bean
  @Primary
  public RestTemplate testRestTemplate() {
    return new RestTemplate();
  }
}

package com.uppdragsradarn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.context.annotation.Profile;

/**
 * Test application configuration that disables Elasticsearch and other external services for unit
 * testing
 */
@SpringBootApplication(exclude = {ElasticsearchRestClientAutoConfiguration.class})
@Profile("test")
public class TestApplication {

  public static void main(String[] args) {
    SpringApplication.run(TestApplication.class, args);
  }
}

package com.uppdragsradarn.application.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 3600) // 1 hour session timeout
public class RedisConfig {

  private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

  @Value("${spring.redis.host:localhost}")
  private String redisHost;

  @Value("${spring.redis.port:6379}")
  private int redisPort;

  @Value("${spring.redis.password:}")
  private String redisPassword;

  @Bean
  public RedisConnectionFactory redisConnectionFactory() {
    log.info("Configuring Redis connection to {}:{}", redisHost, redisPort);

    try {
      // Verify DNS resolution
      java.net.InetAddress address = java.net.InetAddress.getByName(redisHost);
      log.info("Redis host {} resolved to {}", redisHost, address.getHostAddress());
    } catch (Exception e) {
      log.error("Failed to resolve Redis host {}: {}", redisHost, e.getMessage());
    }

    RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
    redisConfig.setHostName(redisHost);
    redisConfig.setPort(redisPort);

    if (redisPassword != null && !redisPassword.isEmpty()) {
      redisConfig.setPassword(redisPassword);
      log.info("Redis password configured");
    } else {
      log.info("No Redis password provided");
    }

    LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(redisConfig);
    connectionFactory.setValidateConnection(true);

    return connectionFactory;
  }

  @Bean
  public RedisTemplate<String, Object> redisTemplate() {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(redisConnectionFactory());

    // Use String serializer for keys and JSON serializer for values
    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());
    template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

    template.afterPropertiesSet();

    log.info("Redis template configured with JSON serialization");

    return template;
  }
}

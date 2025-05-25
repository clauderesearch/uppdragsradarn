package com.uppdragsradarn.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;

import javax.cache.Cache;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.web.SecurityFilterChain;

import com.uppdragsradarn.application.config.RateLimitConfig;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.local.LocalBucket;

/** Test security configuration that provides mock OAuth2 config for integration tests */
@TestConfiguration
@EnableWebSecurity
public class TestSecurityConfig {

  /** Mock RateLimitConfig for tests */
  @MockBean private RateLimitConfig rateLimitConfig;

  /**
   * Creates a mock OAuth2 client registration repository for testing
   *
   * @return Mock repository to prevent actual OAuth2 connections
   */
  @Bean
  @Primary
  public ClientRegistrationRepository clientRegistrationRepository() {
    return new InMemoryClientRegistrationRepository(
        Collections.singletonList(
            ClientRegistration.withRegistrationId("cognito")
                .clientId("test-client-id")
                .clientSecret("test-client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .authorizationUri("http://localhost:8888/test/oauth2/authorize")
                .tokenUri("http://localhost:8888/test/oauth2/token")
                .userInfoUri("http://localhost:8888/test/oauth2/userInfo")
                .userNameAttributeName("email")
                .clientName("Cognito Test")
                .build()));
  }

  /**
   * Creates a test security filter chain that allows all requests
   *
   * @param http The HttpSecurity object
   * @return The SecurityFilterChain
   * @throws Exception if an error occurs
   */
  @Bean
  @Primary
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf()
        .disable()
        .authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers("/api/public/**")
                    .permitAll()
                    .requestMatchers("/api/crawler/public/**")
                    .permitAll()
                    .requestMatchers(
                        "/api/crawler/jobs/source/**",
                        "/api/crawler/jobs/scheduled",
                        "/api/crawler/jobs/*")
                    .hasAuthority("SCOPE_admin")
                    .anyRequest()
                    .permitAll());

    return http.build();
  }

  /** Mock cache for rate limiting tests */
  @Bean
  @Primary
  public Cache<String, LocalBucket> bucketCache() {
    Cache<String, LocalBucket> mockCache = mock(Cache.class);
    when(mockCache.get(anyString())).thenReturn(null);
    return mockCache;
  }

  /** Mock bandwidth for rate limiting tests */
  @Bean
  @Primary
  public Bandwidth defaultBandwidth() {
    return mock(Bandwidth.class);
  }

  /** Mock bucket for rate limiting tests */
  @Bean
  @Primary
  public Bucket testBucket() {
    Bucket mockBucket = mock(Bucket.class);
    when(rateLimitConfig.resolveBucket(anyString(), any(), any())).thenReturn(mockBucket);
    return mockBucket;
  }
}

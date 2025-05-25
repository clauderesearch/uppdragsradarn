package com.uppdragsradarn.test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;

import javax.cache.Cache;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.web.SecurityFilterChain;

import com.uppdragsradarn.application.config.RateLimitConfig;
import com.uppdragsradarn.application.service.UserService;
import com.uppdragsradarn.infrastructure.web.filter.RateLimitFilter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.local.LocalBucket;

/** Configuration for slice tests that mocks service dependencies */
@TestConfiguration
@EnableWebSecurity
public class SliceTestConfig {

  @MockBean public UserService userService;

  @MockBean public RateLimitConfig rateLimitConfig;

  /** Create a security filter chain that properly handles authentication for tests */
  @Bean
  @ConditionalOnMissingBean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
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
                    .authenticated());

    return http.build();
  }

  /** Creates a mock OAuth2 client registration repository for testing */
  @Bean
  @ConditionalOnMissingBean
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
    Bandwidth mockBandwidth = mock(Bandwidth.class);
    when(mockBandwidth.getCapacity()).thenReturn(100L);
    return mockBandwidth;
  }

  /** Create a mock bucket for testing */
  @Bean
  @Primary
  public Bucket testBucket() {
    Bucket mockBucket = mock(Bucket.class);
    ConsumptionProbe mockProbe = mock(ConsumptionProbe.class);
    when(mockProbe.isConsumed()).thenReturn(true);
    when(mockProbe.getRemainingTokens()).thenReturn(99L);
    when(mockBucket.tryConsumeAndReturnRemaining(any(Long.class))).thenReturn(mockProbe);

    when(rateLimitConfig.resolveBucket(anyString(), any(), any())).thenReturn(mockBucket);
    return mockBucket;
  }

  /** Create a mock RateLimitFilter that doesn't apply rate limiting in tests */
  @Bean
  @Primary
  public RateLimitFilter rateLimitFilter() {
    RateLimitFilter filter = mock(RateLimitFilter.class);
    return filter;
  }
}

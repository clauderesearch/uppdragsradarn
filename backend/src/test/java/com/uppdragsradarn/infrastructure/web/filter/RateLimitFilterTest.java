package com.uppdragsradarn.infrastructure.web.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import javax.cache.Cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.uppdragsradarn.application.config.RateLimitConfig;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.local.LocalBucket;
import jakarta.servlet.FilterChain;

public class RateLimitFilterTest {

  @Mock private RateLimitConfig rateLimitConfig;

  @Mock private Cache<String, LocalBucket> bucketCache;

  @Mock private Bandwidth bandwidth;

  @Mock private Bucket bucket;

  @Mock private ConsumptionProbe probe;

  private RateLimitFilter rateLimitFilter;

  private MockHttpServletRequest request;
  private MockHttpServletResponse response;
  private FilterChain filterChain;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    rateLimitFilter = new RateLimitFilter(rateLimitConfig, bucketCache, bandwidth);

    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    filterChain = new MockFilterChain();

    when(rateLimitConfig.resolveBucket(anyString(), any(), any())).thenReturn(bucket);
    when(bandwidth.getCapacity()).thenReturn(100L);
    when(bucket.tryConsumeAndReturnRemaining(anyLong())).thenReturn(probe);
    when(probe.isConsumed()).thenReturn(true);
    when(probe.getRemainingTokens()).thenReturn(99L);
  }

  @Test
  void testFilterDoesNotApplyToNonPublicEndpoints() throws Exception {
    request.setRequestURI("/api/private/resource");

    rateLimitFilter.doFilterInternal(request, response, filterChain);

    verify(bucket, never()).tryConsumeAndReturnRemaining(anyLong());

    verifyNoInteractions(bucket);
  }

  @Test
  void testFilterAppliesRateLimitingToPublicEndpoints() throws Exception {
    request.setRequestURI("/api/public/assignments");

    rateLimitFilter.doFilterInternal(request, response, filterChain);

    verify(bucket).tryConsumeAndReturnRemaining(1);

    assertEquals("99", response.getHeader("X-RateLimit-Remaining"));
    assertEquals("100", response.getHeader("X-RateLimit-Limit"));
  }

  @Test
  void testRateLimitExceeded() throws Exception {
    request.setRequestURI("/api/public/assignments");

    when(probe.isConsumed()).thenReturn(false);
    when(probe.getNanosToWaitForRefill()).thenReturn(1_000_000_000L);

    rateLimitFilter.doFilterInternal(request, response, filterChain);

    assertEquals(429, response.getStatus());

    assertEquals("1", response.getHeader("X-RateLimit-Retry-After-Seconds"));
    assertEquals("0", response.getHeader("X-RateLimit-Remaining"));
  }
}

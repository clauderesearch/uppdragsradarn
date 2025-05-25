package com.uppdragsradarn.application.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import javax.cache.Cache;
import javax.cache.CacheManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.local.LocalBucket;

@ExtendWith(MockitoExtension.class)
public class RateLimitConfigTest {

  @Mock private Cache<String, LocalBucket> bucketCache;
  @Mock private Bandwidth bandwidth;
  @Mock private CacheManager cacheManager;
  @Mock private Cache<String, LocalBucket> cache;

  private RateLimitConfig rateLimitConfig;

  @BeforeEach
  void setUp() {
    rateLimitConfig = new RateLimitConfig();

    rateLimitConfig.setCapacity(100);
    rateLimitConfig.setRefillTokens(10);
    rateLimitConfig.setRefillDuration(1);
  }

  @Test
  void testResolveBucketWhenEnabled() {
    rateLimitConfig.setEnabled(true);

    when(bucketCache.get(anyString())).thenReturn(null);

    Bucket result = rateLimitConfig.resolveBucket("test-ip", bucketCache, bandwidth);

    assertNotNull(result, "Bucket should not be null");
  }

  @Test
  void testResolveBucketWhenDisabled() {
    rateLimitConfig.setEnabled(false);

    Bucket result = rateLimitConfig.resolveBucket("test-ip", bucketCache, bandwidth);

    assertNotNull(result, "Bucket should not be null even when disabled");
  }

  @Test
  void testDefaultBandwidth() {
    Bandwidth bandwidth = rateLimitConfig.defaultBandwidth();

    assertNotNull(bandwidth, "Default bandwidth should not be null");
  }
}

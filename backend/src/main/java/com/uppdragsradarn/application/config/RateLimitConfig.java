package com.uppdragsradarn.application.config;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import io.github.bucket4j.local.LocalBucket;

/** Configuration for API rate limiting */
@Configuration
public class RateLimitConfig {

  @Value("${app.rate-limiting.capacity:100}")
  private int capacity;

  @Value("${app.rate-limiting.refill-tokens:10}")
  private int refillTokens;

  @Value("${app.rate-limiting.refill-duration:1}")
  private int refillDuration;

  @Value("${app.rate-limiting.enabled:true}")
  private boolean enabled;

  /** Cache manager for storing rate limit buckets */
  @Bean
  public CacheManager cacheManager() {
    return Caching.getCachingProvider().getCacheManager();
  }

  /** Cache for storing rate limit buckets by IP address */
  @Bean
  public Cache<String, LocalBucket> bucketCache(CacheManager cacheManager) {
    MutableConfiguration<String, LocalBucket> config =
        new MutableConfiguration<String, LocalBucket>()
            .setTypes(String.class, LocalBucket.class)
            .setStoreByValue(false)
            .setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(new Duration(TimeUnit.HOURS, 1)));

    return cacheManager.createCache("rate-limit-buckets", config);
  }

  /** Default bandwidth configuration for rate limiting */
  @Bean
  public Bandwidth defaultBandwidth() {
    return Bandwidth.classic(
        capacity,
        Refill.intervally(refillTokens, java.time.Duration.of(refillDuration, ChronoUnit.MINUTES)));
  }

  /** Create a resolvable bucket for a given key (typically IP address) */
  public Bucket resolveBucket(String key, Cache<String, LocalBucket> cache, Bandwidth limit) {
    // If rate limiting is disabled, return a bucket with a high limit
    if (!enabled) {
      return Bucket.builder()
          .addLimit(
              Bandwidth.classic(
                  Integer.MAX_VALUE,
                  Refill.intervally(1000, java.time.Duration.of(1, ChronoUnit.SECONDS))))
          .build();
    }

    LocalBucket bucket = cache.get(key);
    if (bucket == null) {
      bucket = (LocalBucket) Bucket.builder().addLimit(limit).build();
      cache.put(key, bucket);
    }
    return bucket;
  }

  // Protected getters and setters for testing
  protected boolean isEnabled() {
    return enabled;
  }

  protected void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  protected int getCapacity() {
    return capacity;
  }

  protected void setCapacity(int capacity) {
    this.capacity = capacity;
  }

  protected int getRefillTokens() {
    return refillTokens;
  }

  protected void setRefillTokens(int refillTokens) {
    this.refillTokens = refillTokens;
  }

  protected int getRefillDuration() {
    return refillDuration;
  }

  protected void setRefillDuration(int refillDuration) {
    this.refillDuration = refillDuration;
  }
}

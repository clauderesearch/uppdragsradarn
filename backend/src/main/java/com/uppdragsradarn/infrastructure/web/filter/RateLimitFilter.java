package com.uppdragsradarn.infrastructure.web.filter;

import java.io.IOException;

import javax.cache.Cache;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.uppdragsradarn.application.config.RateLimitConfig;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.local.LocalBucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Rate limiting filter that applies to public API endpoints */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

  private static final String RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";
  private static final String RATE_LIMIT_LIMIT_HEADER = "X-RateLimit-Limit";
  private static final String RATE_LIMIT_RETRY_HEADER = "X-RateLimit-Retry-After-Seconds";

  private final RateLimitConfig rateLimitConfig;
  private final Cache<String, LocalBucket> bucketCache;
  private final Bandwidth defaultBandwidth;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    // Only apply rate limiting to public API endpoints
    String path = request.getRequestURI();
    if (!path.startsWith("/api/public")) {
      filterChain.doFilter(request, response);
      return;
    }

    // Get client's IP address
    String clientIp = getClientIP(request);

    // Resolve the rate limit bucket for this IP
    Bucket bucket = rateLimitConfig.resolveBucket(clientIp, bucketCache, defaultBandwidth);

    // Try to consume a token from the bucket
    ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

    // Add rate limit headers to response
    response.addHeader(RATE_LIMIT_LIMIT_HEADER, String.valueOf(defaultBandwidth.getCapacity()));

    if (probe.isConsumed()) {
      // Request allowed - add remaining tokens to header
      response.addHeader(RATE_LIMIT_REMAINING_HEADER, String.valueOf(probe.getRemainingTokens()));
      // Continue with the request
      filterChain.doFilter(request, response);
    } else {
      // Request denied due to rate limiting
      log.warn("Rate limit exceeded for IP: {}", clientIp);

      // Add retry-after header
      long waitTimeSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
      response.addHeader(RATE_LIMIT_RETRY_HEADER, String.valueOf(waitTimeSeconds));
      response.addHeader(RATE_LIMIT_REMAINING_HEADER, "0");

      // Return 429 Too Many Requests status
      response.setStatus(429); // HTTP status code for "Too Many Requests"
      response.getWriter().write("Rate limit exceeded. Please try again later.");
    }
  }

  /** Extract client IP address from request, handling proxies */
  private String getClientIP(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
      // Get the first IP in case of multiple proxies
      return xForwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}

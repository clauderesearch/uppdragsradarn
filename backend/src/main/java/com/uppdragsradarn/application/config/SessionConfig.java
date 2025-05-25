package com.uppdragsradarn.application.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.SessionCookieConfig;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Order(1) // Ensure this runs early
@Slf4j
public class SessionConfig {

  @Value("${app.cookie-domain:#{null}}")
  private String cookieDomain;

  @Bean
  public ServletContextInitializer servletContextInitializer() {
    return new ServletContextInitializer() {
      @Override
      public void onStartup(ServletContext servletContext) throws ServletException {
        SessionCookieConfig sessionCookieConfig = servletContext.getSessionCookieConfig();
        sessionCookieConfig.setHttpOnly(true);
        sessionCookieConfig.setSecure(true);
        sessionCookieConfig.setPath("/");
        sessionCookieConfig.setMaxAge(3600); // 1 hour

        if (cookieDomain != null) {
          // Remove leading dot if present - Spring Boot handles this
          String domain = cookieDomain.startsWith(".") ? cookieDomain.substring(1) : cookieDomain;
          sessionCookieConfig.setDomain(domain);
          log.info("Setting session cookie domain to: {}", domain);
        }

        log.info(
            "Session cookie config - httpOnly: {}, secure: {}, path: {}, maxAge: {}, domain: {}",
            sessionCookieConfig.isHttpOnly(),
            sessionCookieConfig.isSecure(),
            sessionCookieConfig.getPath(),
            sessionCookieConfig.getMaxAge(),
            sessionCookieConfig.getDomain());
      }
    };
  }
}

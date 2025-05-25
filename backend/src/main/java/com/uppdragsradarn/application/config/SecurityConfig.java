package com.uppdragsradarn.application.config;

import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.web.AuthenticatedPrincipalOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.uppdragsradarn.application.dto.UserDto;
import com.uppdragsradarn.application.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfig {

  @Value("${spring.security.oauth2.client.registration.cognito.client-id}")
  private String clientId;

  @Value("${app.frontend.url:http://localhost}")
  private String frontendUrl;

  @Value("${app.frontend.admin-url:#{null}}")
  private String adminFrontendUrl;

  @Value("${app.allowed-origins:}")
  private List<String> allowedOrigins;

  @Value("${app.cookie-domain:#{null}}")
  private String cookieDomain;

  private final UserService userService;

  public SecurityConfig(UserService userService) {
    this.userService = userService;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    SimpleUrlAuthenticationSuccessHandler successHandler =
        new SimpleUrlAuthenticationSuccessHandler() {
          @Override
          protected String determineTargetUrl(
              HttpServletRequest request, HttpServletResponse response) {
            // Add debug logging for session cookie
            jakarta.servlet.http.Cookie[] cookies = request.getCookies();
            log.debug("Current cookies during OAuth callback redirect:");
            if (cookies != null) {
              for (jakarta.servlet.http.Cookie cookie : cookies) {
                log.debug(
                    "Cookie: {} = {}, domain: {}, path: {}",
                    cookie.getName(),
                    cookie.getValue(),
                    cookie.getDomain(),
                    cookie.getPath());
              }
            }

            // Check which OAuth client was used based on the URL
            String requestUrl = request.getRequestURL().toString();
            if (requestUrl.contains("cognito-admin")) {
              // Use the admin-specific frontend URL if configured, otherwise fall back to regular
              // frontend
              String adminUrl =
                  (adminFrontendUrl != null) ? adminFrontendUrl : frontendUrl + "/admin";
              return adminUrl + "/auth/callback";
            }
            return frontendUrl + "/auth/callback";
          }
        };

    return http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(
            csrf ->
                csrf.csrfTokenRepository(new HttpSessionCsrfTokenRepository())
                    .ignoringRequestMatchers(
                        "/oauth2/**",
                        "/login/**",
                        "/api/logout",
                        "/api/csrf-debug/info",
                        "/api/csrf-debug/compare")
                    .requireCsrfProtectionMatcher(
                        request -> {
                          String method = request.getMethod();
                          String path = request.getRequestURI();
                          boolean requiresProtection =
                              !"GET".equals(method)
                                  && !path.startsWith("/oauth2/")
                                  && !path.startsWith("/login")
                                  && !path.equals("/api/logout");

                          // Debug logging
                          if (log.isDebugEnabled()) {
                            log.debug(
                                "CSRF Protection Check - Path: {}, Method: {}, Requires Protection: {}, "
                                    + "Session ID: {}, CSRF Cookie: {}, CSRF Header: {}, CSRF Param: {}",
                                path,
                                method,
                                requiresProtection,
                                request.getRequestedSessionId(),
                                extractCsrfFromCookie(request),
                                request.getHeader("X-CSRF-TOKEN"),
                                request.getParameter("_csrf"));
                          }

                          return requiresProtection;
                        }))
        .authorizeHttpRequests(
            authorize ->
                authorize
                    // Session endpoint - available to all
                    .requestMatchers("/api/session")
                    .permitAll()
                    // CSRF token is now part of the session endpoint
                    // CSRF debug endpoints - available to all (for troubleshooting)
                    .requestMatchers("/api/csrf-debug/**")
                    .permitAll()
                    // OAuth2, login, and logout endpoints
                    .requestMatchers("/oauth2/**", "/login/**", "/api/logout")
                    .permitAll()
                    // Actuator endpoints
                    .requestMatchers("/actuator/health", "/actuator/info")
                    .permitAll()
                    // OpenAPI endpoints
                    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                    .permitAll()
                    // Protected endpoints
                    .requestMatchers("/**")
                    .authenticated()
                    .anyRequest()
                    .authenticated())
        .oauth2Login(
            oauth2 ->
                oauth2
                    .loginPage(frontendUrl + "/login")
                    .successHandler(successHandler)
                    .userInfoEndpoint(
                        userInfo ->
                            userInfo
                                .userService(this.oauth2UserService())
                                .oidcUserService(this.oidcUserService()))
                    // Customize the authorization endpoint path
                    .authorizationEndpoint(
                        authorization -> authorization.baseUri("/oauth2/authorization")))
        .exceptionHandling(
            exceptions ->
                exceptions.authenticationEntryPoint(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
        .logout(
            logout ->
                logout
                    .logoutSuccessUrl(frontendUrl)
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID"))
        .sessionManagement(
            session ->
                session
                    .sessionCreationPolicy(
                        org.springframework.security.config.http.SessionCreationPolicy.IF_REQUIRED)
                    .maximumSessions(1))
        .build();
  }

  /** Creates or updates a user in our database when logging in with OAuth2 */
  private void ensureUserExists(String email, String firstName, String lastName) {
    if (email != null) {
      Optional<com.uppdragsradarn.domain.model.User> userOpt =
          userService.getUserByEmailOptional(email);

      // Auto-create user if not exists
      if (userOpt.isEmpty()) {
        UserDto userDto =
            UserDto.builder()
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .notificationEmailEnabled(true)
                .build();
        userService.createUser(userDto);
      }
    }
  }

  @Bean
  public OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService() {
    DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    return userRequest -> {
      OAuth2User oauth2User = delegate.loadUser(userRequest);

      // Extract attributes
      Map<String, Object> attributes = oauth2User.getAttributes();

      // Create or update user
      String email = (String) attributes.get("email");
      String firstName = (String) attributes.get("given_name");
      String lastName = (String) attributes.get("family_name");
      ensureUserExists(email, firstName, lastName);

      // Add roles based on database roles
      Set<GrantedAuthority> authorities = new HashSet<>();

      // Get user from database to check roles
      if (email != null) {
        Optional<com.uppdragsradarn.domain.model.User> userOpt =
            userService.getUserByEmailOptional(email);
        if (userOpt.isPresent()) {
          com.uppdragsradarn.domain.model.User user = userOpt.get();
          // Add all roles from database
          user.getRoles()
              .forEach(
                  role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName())));
        }
      }

      return new DefaultOAuth2User(
          authorities, attributes, "email" // The key of the name attribute
          );
    };
  }

  @Bean
  public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
    OidcUserService delegate = new OidcUserService();

    return userRequest -> {
      OidcUser oidcUser = delegate.loadUser(userRequest);

      // Extract attributes
      Map<String, Object> attributes = oidcUser.getAttributes();

      // Create or update user
      String email = (String) attributes.get("email");
      String firstName = (String) attributes.get("given_name");
      String lastName = (String) attributes.get("family_name");
      ensureUserExists(email, firstName, lastName);

      // Add roles based on database roles
      Set<GrantedAuthority> authorities = new HashSet<>(oidcUser.getAuthorities());

      // Get user from database to check roles
      if (email != null) {
        Optional<com.uppdragsradarn.domain.model.User> userOpt =
            userService.getUserByEmailOptional(email);
        if (userOpt.isPresent()) {
          com.uppdragsradarn.domain.model.User user = userOpt.get();
          // Add all roles from database
          user.getRoles()
              .forEach(
                  role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName())));
        }
      }

      return new CustomOidcUser(authorities, oidcUser.getIdToken(), "email");
    };
  }

  @Bean
  public OAuth2AuthorizedClientService authorizedClientService(
      ClientRegistrationRepository clientRegistrationRepository) {
    return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
  }

  @Bean
  public OAuth2AuthorizedClientRepository authorizedClientRepository(
      OAuth2AuthorizedClientService authorizedClientService) {
    return new AuthenticatedPrincipalOAuth2AuthorizedClientRepository(authorizedClientService);
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    // Use configured allowed origins if available
    if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
      configuration.setAllowedOrigins(allowedOrigins);
    } else {
      // Fallback to default
      List<String> defaultOrigins = new ArrayList<>();
      defaultOrigins.add(frontendUrl);
      if (adminFrontendUrl != null) {
        defaultOrigins.add(adminFrontendUrl);
      }
      defaultOrigins.add("http://localhost:3000");
      defaultOrigins.add("http://localhost:3001"); // Admin default port
      defaultOrigins.add("http://localhost");
      configuration.setAllowedOrigins(defaultOrigins);
    }

    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(
        Arrays.asList(
            "Authorization", "Content-Type", "X-Auth-Token", "X-XSRF-TOKEN", "X-Requested-With"));
    configuration.setExposedHeaders(Collections.singletonList("Set-Cookie"));
    configuration.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  /** Helper method to extract CSRF token from cookies for debugging */
  private String extractCsrfFromCookie(HttpServletRequest request) {
    if (request.getCookies() != null) {
      for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
        if ("XSRF-TOKEN".equals(cookie.getName()) || "_csrf".equals(cookie.getName())) {
          return cookie.getValue();
        }
      }
    }
    return null;
  }
}

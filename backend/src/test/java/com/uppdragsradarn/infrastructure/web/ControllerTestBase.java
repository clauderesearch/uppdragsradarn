package com.uppdragsradarn.infrastructure.web;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/** Base class for controller tests with common annotations */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@TestPropertySource(
    properties = {
      "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8888/test",
      "spring.security.oauth2.client.registration.cognito.client-id=test-client-id",
      "spring.security.oauth2.client.registration.cognito.client-secret=test-client-secret",
      "spring.security.oauth2.client.registration.cognito.client-name=Cognito Test",
      "spring.security.oauth2.client.provider.cognito.issuer-uri=http://localhost:8888/test",
      "COGNITO_REGION=us-east-1",
      "COGNITO_USER_POOL_ID=test-user-pool",
      "COGNITO_APP_CLIENT_ID=test-client-id",
      "app.frontend.url=http://localhost:3000"
    })
public abstract class ControllerTestBase {
  // Base class with common configurations
}

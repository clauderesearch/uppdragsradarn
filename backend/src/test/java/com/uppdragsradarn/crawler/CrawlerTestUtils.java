package com.uppdragsradarn.crawler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.uppdragsradarn.domain.model.Assignment;
import com.uppdragsradarn.domain.model.Source;
import com.uppdragsradarn.domain.model.SourceType;

/** Utility class for crawler tests with helper methods to create test data */
public class CrawlerTestUtils {

  /**
   * Creates a test source type with specified name
   *
   * @param name The name of the source type (e.g., EWORK, EMAGINE, etc.)
   * @return A new SourceType instance for testing
   */
  public static SourceType createTestSourceType(String name) {
    return SourceType.builder().id(UUID.randomUUID()).name(name).build();
  }

  /**
   * Creates a test source with specified parameters
   *
   * @param name The name of the source
   * @param baseUrl The base URL for the source
   * @param sourceType The type of the source
   * @param configuration Optional configuration parameters
   * @return A new Source instance for testing
   */
  public static Source createTestSource(
      String name, String baseUrl, SourceType sourceType, String configuration) {
    Source source = new Source();
    source.setId(UUID.randomUUID());
    source.setName(name);
    source.setBaseUrl(baseUrl);
    source.setSourceType(sourceType);
    source.setConfiguration(configuration);
    source.setActive(true);
    source.setCreatedAt(LocalDateTime.now());
    source.setUpdatedAt(LocalDateTime.now());
    return source;
  }

  /**
   * Creates a basic test assignment with minimum required fields
   *
   * @param source The source of the assignment
   * @param title The title of the assignment
   * @param externalId The external ID from the source system
   * @return A new Assignment instance for testing
   */
  public static Assignment createTestAssignment(Source source, String title, String externalId) {
    Assignment assignment = new Assignment();
    assignment.setSource(source);
    assignment.setTitle(title);
    assignment.setExternalId(externalId);
    assignment.setApplicationUrl("https://example.com/job/" + externalId);
    assignment.setOriginalLocationText("Stockholm");
    assignment.setDescription("Test assignment description");
    assignment.setCreatedAt(LocalDateTime.now().minusDays(1));
    assignment.setUpdatedAt(LocalDateTime.now());
    assignment.setCreatedAt(LocalDateTime.now());
    return assignment;
  }

  /**
   * Creates a sample HTML job listing for testing
   *
   * @param title Job title
   * @param company Company name
   * @param location Job location
   * @param description Job description
   * @param details Job details (rate, duration, etc.)
   * @param skills Job skills required
   * @param url Job URL
   * @return HTML string for testing
   */
  public static String createSampleJobHtml(
      String title,
      String company,
      String location,
      String description,
      String details,
      String skills,
      String url) {
    return "<!DOCTYPE html><html><body>"
        + "<div class=\"job-card\" data-id=\"12345\">"
        + "<h5 class=\"job-card-title\">"
        + title
        + "</h5>"
        + "<div class=\"job-card-company\">"
        + company
        + "</div>"
        + "<div class=\"job-card-content\">"
        + description
        + "<br>"
        + "Location: "
        + location
        + "<br>"
        + details
        + "<br>"
        + "Skills: "
        + skills
        + "<br>"
        + "</div>"
        + "<a href=\""
        + url
        + "\" class=\"job-card-link\">Apply</a>"
        + "</div>"
        + "</body></html>";
  }

  /**
   * Sets up a mocked HTTP client for testing
   *
   * @param httpClientsMock The mocked HttpClients static mock
   * @param httpClient The mocked HttpClient
   * @param httpResponse The mocked HttpResponse
   * @param httpEntity The mocked HttpEntity
   * @param statusCode HTTP status code to return
   * @param html HTML content to return
   * @throws Exception If an error occurs
   */
  public static void setupMockedHttpClient(
      MockedStatic<org.apache.hc.client5.http.impl.classic.HttpClients> httpClientsMock,
      CloseableHttpClient httpClient,
      CloseableHttpResponse httpResponse,
      HttpEntity httpEntity,
      int statusCode,
      String html)
      throws Exception {

    // Create a custom builder mock
    org.apache.hc.client5.http.impl.classic.HttpClientBuilder builder =
        Mockito.mock(
            org.apache.hc.client5.http.impl.classic.HttpClientBuilder.class, Mockito.RETURNS_SELF);

    // Configure the builder mock
    when(builder.build()).thenReturn(httpClient);
    when(builder.setDefaultRequestConfig(any())).thenReturn(builder);

    // Setup HttpClients to return our mocked builder
    httpClientsMock
        .when(org.apache.hc.client5.http.impl.classic.HttpClients::custom)
        .thenReturn(builder);

    // Setup the response with the given status code
    when(httpResponse.getCode()).thenReturn(statusCode);

    // Setup the response with the given HTML content
    if (html != null) {
      InputStream inputStream = new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8));
      when(httpEntity.getContent()).thenReturn(inputStream);
      when(httpResponse.getEntity()).thenReturn(httpEntity);
    }

    // Setup the client to return our mocked response
    // Note: If this is overridden later, Mockito will use the last stubbing
    when(httpClient.execute(any())).thenReturn(httpResponse);
  }
}

package com.uppdragsradarn.integration;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

/** WireMock configuration for crawler integration tests */
public class CrawlerWireMockConfig implements BeforeAllCallback {

  private final WireMockExtension wireMockServer;

  public CrawlerWireMockConfig(WireMockExtension wireMockServer) {
    this.wireMockServer = wireMockServer;
  }

  @Override
  public void beforeAll(ExtensionContext context) {
    setupEmagineCrawlerStubs();
    setupASocietyGroupCrawlerStubs();
  }

  private void setupEmagineCrawlerStubs() {
    // Setup WireMock stub for Emagine Crawler
    String sampleHtml =
        "<!DOCTYPE html><html><body>"
            + "<div class=\"job-card\" data-id=\"12345\">"
            + "<h5 class=\"job-card-title\">Java Developer</h5>"
            + "<div class=\"job-card-company\">Tech Company</div>"
            + "<div class=\"job-card-content\">"
            + "We are looking for a Java Developer with Spring Boot experience.<br>"
            + "Location: Stockholm<br>"
            + "Rate: 800-900 kr per hour, Duration: 6 months, Start: 2023-09-01, Apply before: 2023-08-15<br>"
            + "Skills: Java, Spring Boot, Microservices<br>"
            + "</div>"
            + "<a href=\"https://test-emagine.com/jobs/java-developer\" class=\"job-card-link\">Apply</a>"
            + "</div>"
            + "</body></html>";

    wireMockServer.stubFor(
        WireMock.post(
                WireMock.urlPathMatching("/wp-content/themes/emagine-theme/ajax/api-country.php"))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/html")
                    .withBody(sampleHtml)));
  }

  private void setupASocietyGroupCrawlerStubs() {
    // Setup WireMock stub for A Society Group list page
    wireMockServer.stubFor(
        WireMock.get(WireMock.urlEqualTo("/sv/uppdrag"))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/html; charset=UTF-8")
                    .withBodyFile("asocietygroup/list_page.html")));

    // Setup WireMock stub for A Society Group detail page
    wireMockServer.stubFor(
        WireMock.get(WireMock.urlPathMatching("/sv/uppdrag/.*-13376"))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/html; charset=UTF-8")
                    .withBodyFile("asocietygroup/detail_page.html")));
  }
}

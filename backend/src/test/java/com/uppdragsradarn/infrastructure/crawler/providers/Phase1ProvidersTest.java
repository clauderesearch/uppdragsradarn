package com.uppdragsradarn.infrastructure.crawler.providers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.uppdragsradarn.application.service.LocationService;
import com.uppdragsradarn.domain.model.Currency;
import com.uppdragsradarn.domain.model.Source;
import com.uppdragsradarn.domain.model.SourceType;
import com.uppdragsradarn.domain.model.StatusType;
import com.uppdragsradarn.domain.repository.CurrencyRepository;
import com.uppdragsradarn.domain.repository.SkillRepository;
import com.uppdragsradarn.domain.repository.SourceTypeRepository;
import com.uppdragsradarn.domain.repository.StatusTypeRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Test class for Phase 1 providers. These tests verify that each provider can be instantiated and
 * supports the correct sources.
 */
@ExtendWith(MockitoExtension.class)
@Slf4j
public class Phase1ProvidersTest {

  @Mock private LocationService locationService;

  @Mock private SkillRepository skillRepository;

  @Mock private CurrencyRepository currencyRepository;

  @Mock private SourceTypeRepository sourceTypeRepository;

  @Mock private StatusTypeRepository statusTypeRepository;

  private Source webScraperSource;
  private StatusType activeStatus;
  private Currency sekCurrency;

  @BeforeEach
  void setUp() {
    // Setup common test data
    SourceType webScraper = SourceType.builder().id(UUID.randomUUID()).name("WEB_SCRAPER").build();

    webScraperSource =
        Source.builder().id(UUID.randomUUID()).sourceType(webScraper).active(true).build();

    activeStatus =
        StatusType.builder().id(UUID.randomUUID()).name("ACTIVE").entityType("ASSIGNMENT").build();

    sekCurrency = Currency.builder().code("SEK").name("Swedish Krona").symbol("kr").build();
  }

  @Test
  void testDevelopersBayProvider() {
    // Arrange
    DevelopersBayProvider provider =
        new DevelopersBayProvider(
            locationService,
            skillRepository,
            currencyRepository,
            sourceTypeRepository,
            statusTypeRepository);

    Source source =
        Source.builder()
            .id(UUID.randomUUID())
            .sourceType(webScraperSource.getSourceType())
            .active(true)
            .name("Developers Bay")
            .baseUrl("https://developersbay.se")
            .build();

    // Act & Assert
    assertTrue(provider.supports(source), "Provider should support Developers Bay source");
    assertEquals("Developers Bay Provider", provider.getName());

    // Test with different source name and URL
    source.setName("Different Provider");
    source.setBaseUrl("https://different-provider.com");
    assertFalse(provider.supports(source), "Provider should not support different source");
  }

  @Test
  void testInterimSearchProvider() {
    // Arrange
    InterimSearchProvider provider =
        new InterimSearchProvider(
            locationService,
            skillRepository,
            currencyRepository,
            sourceTypeRepository,
            statusTypeRepository);

    Source source =
        Source.builder()
            .id(UUID.randomUUID())
            .sourceType(webScraperSource.getSourceType())
            .active(true)
            .name("Interim Search")
            .baseUrl("https://www.interimsearch.com")
            .build();

    // Act & Assert
    assertTrue(provider.supports(source), "Provider should support Interim Search source");
    assertEquals("Interim Search Provider", provider.getName());
  }

  @Test
  void testBiolitProvider() {
    // Arrange
    BiolitProvider provider =
        new BiolitProvider(
            locationService,
            skillRepository,
            currencyRepository,
            sourceTypeRepository,
            statusTypeRepository);

    Source source =
        Source.builder()
            .id(UUID.randomUUID())
            .sourceType(webScraperSource.getSourceType())
            .active(true)
            .name("Biolit")
            .baseUrl("https://biolit.se")
            .build();

    // Act & Assert
    assertTrue(provider.supports(source), "Provider should support Biolit source");
    assertEquals("Biolit Provider", provider.getName());
  }

  @Test
  void testKonsultfabrikenProvider() {
    // Arrange
    KonsultfabrikenProvider provider =
        new KonsultfabrikenProvider(
            locationService,
            skillRepository,
            currencyRepository,
            sourceTypeRepository,
            statusTypeRepository);

    Source source =
        Source.builder()
            .id(UUID.randomUUID())
            .sourceType(webScraperSource.getSourceType())
            .active(true)
            .name("Konsultfabriken")
            .baseUrl("https://www.konsultfabriken.se")
            .build();

    // Act & Assert
    assertTrue(provider.supports(source), "Provider should support Konsultfabriken source");
    assertEquals("Konsultfabriken Provider", provider.getName());
  }

  @Test
  void testProvidersSupportBaseUrl() {
    // Test that providers also support sources by base URL

    // Developers Bay
    DevelopersBayProvider dbProvider =
        new DevelopersBayProvider(
            locationService,
            skillRepository,
            currencyRepository,
            sourceTypeRepository,
            statusTypeRepository);
    Source dbSource =
        Source.builder()
            .id(UUID.randomUUID())
            .sourceType(webScraperSource.getSourceType())
            .active(true)
            .name("Some Other Name")
            .baseUrl("https://developersbay.se/some-path")
            .build();
    assertTrue(dbProvider.supports(dbSource), "Should support by base URL");

    // Interim Search
    InterimSearchProvider isProvider =
        new InterimSearchProvider(
            locationService,
            skillRepository,
            currencyRepository,
            sourceTypeRepository,
            statusTypeRepository);
    Source isSource =
        Source.builder()
            .id(UUID.randomUUID())
            .sourceType(webScraperSource.getSourceType())
            .active(true)
            .name("Some Other Name")
            .baseUrl("https://interimsearch.com/path")
            .build();
    assertTrue(isProvider.supports(isSource), "Should support by base URL");

    // Biolit
    BiolitProvider blProvider =
        new BiolitProvider(
            locationService,
            skillRepository,
            currencyRepository,
            sourceTypeRepository,
            statusTypeRepository);
    Source blSource =
        Source.builder()
            .id(UUID.randomUUID())
            .sourceType(webScraperSource.getSourceType())
            .active(true)
            .name("Some Other Name")
            .baseUrl("https://biolit.se/path")
            .build();
    assertTrue(blProvider.supports(blSource), "Should support by base URL");

    // Konsultfabriken
    KonsultfabrikenProvider kfProvider =
        new KonsultfabrikenProvider(
            locationService,
            skillRepository,
            currencyRepository,
            sourceTypeRepository,
            statusTypeRepository);
    Source kfSource =
        Source.builder()
            .id(UUID.randomUUID())
            .sourceType(webScraperSource.getSourceType())
            .active(true)
            .name("Some Other Name")
            .baseUrl("https://konsultfabriken.se/path")
            .build();
    assertTrue(kfProvider.supports(kfSource), "Should support by base URL");
  }
}

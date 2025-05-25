package com.uppdragsradarn.infrastructure.crawler.providers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.uppdragsradarn.application.service.LocationService;
import com.uppdragsradarn.crawler.CrawlerTestUtils;
import com.uppdragsradarn.domain.model.Source;
import com.uppdragsradarn.domain.model.SourceType;
import com.uppdragsradarn.domain.repository.CurrencyRepository;
import com.uppdragsradarn.domain.repository.SkillRepository;
import com.uppdragsradarn.domain.repository.SourceTypeRepository;
import com.uppdragsradarn.domain.repository.StatusTypeRepository;

/**
 * Tests for EmagineProvider. Note: Complex extraction tests have been moved to
 * LlmJobExtractionServiceTest as the regex-based extraction has been replaced by LLM processing.
 */
class EmagineProviderTest {

  private EmagineProvider emagineProvider;
  private Source emagineSource;

  @BeforeEach
  void setUp() {
    // Create mock dependencies
    LocationService locationService = mock(LocationService.class);
    SkillRepository skillRepository = mock(SkillRepository.class);
    CurrencyRepository currencyRepository = mock(CurrencyRepository.class);
    SourceTypeRepository sourceTypeRepository = mock(SourceTypeRepository.class);
    StatusTypeRepository statusTypeRepository = mock(StatusTypeRepository.class);

    emagineProvider =
        new EmagineProvider(
            5,
            "TestUserAgent",
            10,
            "https://emagine-consulting.se",
            locationService,
            skillRepository,
            currencyRepository,
            sourceTypeRepository,
            statusTypeRepository);

    SourceType emagineSourceType = CrawlerTestUtils.createTestSourceType("EMAGINE");
    emagineSource =
        Source.builder()
            .id(UUID.randomUUID())
            .name("Emagine Consulting")
            .baseUrl("https://emagine-consulting.se")
            .sourceType(emagineSourceType)
            .active(true)
            .build();
  }

  @Test
  void shouldCreateProviderCorrectly() {
    // Test that the provider is created with correct configuration
    assertThat(emagineProvider).isNotNull();
    assertThat(emagineProvider.getName()).isEqualTo("Emagine Provider");
    assertThat(emagineProvider.supports(emagineSource)).isTrue();
  }

  @Test
  void getName_shouldReturnCorrectProviderName() {
    assertThat(emagineProvider.getName()).isEqualTo("Emagine Provider");
  }

  @Test
  void supports_shouldReturnTrueForEmagineSource() {
    assertThat(emagineProvider.supports(emagineSource)).isTrue();
  }

  @Test
  void supports_shouldReturnTrueForSourceWithEmagineInUrl() {
    Source sourceWithEmagineUrl =
        Source.builder().name("Other Source").baseUrl("https://emagine-consulting.se/jobs").build();

    assertThat(emagineProvider.supports(sourceWithEmagineUrl)).isTrue();
  }

  // NOTE: Detailed extraction tests are now in LlmJobExtractionServiceTest
  // as the complex regex-based extraction has been replaced by LLM processing
}

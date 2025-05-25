package com.uppdragsradarn.infrastructure.crawler.providers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.uppdragsradarn.application.service.LocationService;
import com.uppdragsradarn.crawler.CrawlerTestUtils;
import com.uppdragsradarn.domain.model.Assignment;
import com.uppdragsradarn.domain.model.Source;
import com.uppdragsradarn.domain.model.SourceType;
import com.uppdragsradarn.domain.repository.CurrencyRepository;
import com.uppdragsradarn.domain.repository.SkillRepository;
import com.uppdragsradarn.domain.repository.SourceTypeRepository;
import com.uppdragsradarn.domain.repository.StatusTypeRepository;

@ExtendWith(MockitoExtension.class)
class ExperisProviderTest {

  private ExperisProvider experisProvider;
  private Source experisSource;
  private Source nonExperisSource;

  @BeforeEach
  void setUp() {
    // Create mock dependencies
    LocationService locationService = mock(LocationService.class);
    SkillRepository skillRepository = mock(SkillRepository.class);
    CurrencyRepository currencyRepository = mock(CurrencyRepository.class);
    SourceTypeRepository sourceTypeRepository = mock(SourceTypeRepository.class);
    StatusTypeRepository statusTypeRepository = mock(StatusTypeRepository.class);

    experisProvider =
        new ExperisProvider(
            5,
            "TestUserAgent",
            10,
            "https://www.experis.se",
            20,
            locationService,
            skillRepository,
            currencyRepository,
            sourceTypeRepository,
            statusTypeRepository);

    SourceType experisSourceType = CrawlerTestUtils.createTestSourceType("EXPERIS");
    SourceType webScraperSourceType = CrawlerTestUtils.createTestSourceType("WEB_SCRAPER");

    experisSource =
        Source.builder()
            .id(UUID.randomUUID())
            .name("Experis")
            .baseUrl("https://experis.se/uppdrag")
            .sourceType(experisSourceType)
            .active(true)
            .build();

    nonExperisSource =
        Source.builder()
            .id(UUID.randomUUID())
            .name("Other")
            .baseUrl("https://other.com")
            .sourceType(webScraperSourceType)
            .active(true)
            .build();
  }

  @Test
  void getName_shouldReturnCorrectProviderName() {
    assertThat(experisProvider.getName()).isEqualTo("Experis Provider");
  }

  @Test
  void supports_shouldReturnTrue_forExperisSourceType() {
    assertThat(experisProvider.supports(experisSource)).isTrue();
  }

  @Test
  void supports_shouldReturnTrue_forSourceWithExperisInUrl() {
    SourceType apiSourceType = CrawlerTestUtils.createTestSourceType("API");

    Source source =
        Source.builder()
            .id(UUID.randomUUID())
            .name("Other")
            .baseUrl("https://something.experis.se")
            .sourceType(apiSourceType)
            .active(true)
            .build();

    assertThat(experisProvider.supports(source)).isTrue();
  }

  @Test
  void supports_shouldReturnFalse_forNonExperisSourceType() {
    assertThat(experisProvider.supports(nonExperisSource)).isFalse();
  }

  @Test
  void getAssignments_shouldReturnEmptyList_whenExceptionIsThrown() {
    ExperisProvider spyProvider = spy(experisProvider);
    doThrow(new RuntimeException("Test exception")).when(spyProvider).fetchAndParse(any());
    List<Assignment> assignments = spyProvider.getAssignments(experisSource);
    assertThat(assignments).isEmpty();
  }
}

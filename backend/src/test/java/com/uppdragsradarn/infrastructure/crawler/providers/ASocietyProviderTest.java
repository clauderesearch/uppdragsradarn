package com.uppdragsradarn.infrastructure.crawler.providers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.uppdragsradarn.application.service.LocationService;
import com.uppdragsradarn.crawler.CrawlerTestUtils;
import com.uppdragsradarn.domain.model.Source;
import com.uppdragsradarn.domain.model.SourceType;
import com.uppdragsradarn.domain.repository.CurrencyRepository;
import com.uppdragsradarn.domain.repository.SkillRepository;
import com.uppdragsradarn.domain.repository.StatusTypeRepository;

@ExtendWith(MockitoExtension.class)
class ASocietyProviderTest {

  private ASocietyProvider aSocietyProvider;
  private Source aSocietySource;
  private Source nonASocietySource;

  @BeforeEach
  void setUp() {
    // Create mock dependencies
    LocationService locationService = mock(LocationService.class);
    SkillRepository skillRepository = mock(SkillRepository.class);
    CurrencyRepository currencyRepository = mock(CurrencyRepository.class);
    StatusTypeRepository statusTypeRepository = mock(StatusTypeRepository.class);

    // Create provider with mock dependencies
    aSocietyProvider =
        spy(
            new ASocietyProvider(
                locationService, skillRepository, currencyRepository, statusTypeRepository));

    SourceType aSocietySourceType = CrawlerTestUtils.createTestSourceType("ASOCIETYGROUP");
    SourceType webScraperSourceType = CrawlerTestUtils.createTestSourceType("WEB_SCRAPER");

    aSocietySource =
        Source.builder()
            .id(UUID.randomUUID())
            .name("A Society Group")
            .baseUrl("https://www.asocietygroup.com")
            .sourceType(aSocietySourceType)
            .active(true)
            .build();

    nonASocietySource =
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
    assertThat(aSocietyProvider.getName()).isEqualTo("A Society Group Provider");
  }

  @Test
  void supports_shouldReturnTrue_forASocietyGroupSourceType() {
    assertThat(aSocietyProvider.supports(aSocietySource)).isTrue();
  }

  @Test
  void supports_shouldReturnTrue_forSourceWithASocietyInUrl() {
    SourceType apiSourceType = CrawlerTestUtils.createTestSourceType("API");

    Source source =
        Source.builder()
            .id(UUID.randomUUID())
            .name("Other")
            .baseUrl("https://something.asocietygroup.com")
            .sourceType(apiSourceType)
            .active(true)
            .build();

    assertThat(aSocietyProvider.supports(source)).isTrue();
  }

  @Test
  void supports_shouldReturnFalse_forNonASocietySourceType() {
    assertThat(aSocietyProvider.supports(nonASocietySource)).isFalse();
  }

  @Test
  void getAssignments_shouldReturnEmptyList_whenExceptionIsThrown() {
    doThrow(new RuntimeException("Test exception")).when(aSocietyProvider).fetchAndParse(any());
    var assignments = aSocietyProvider.getAssignments(aSocietySource);
    assertThat(assignments).isEmpty();
  }
}

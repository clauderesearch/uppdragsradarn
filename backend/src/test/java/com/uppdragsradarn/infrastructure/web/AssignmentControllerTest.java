package com.uppdragsradarn.infrastructure.web;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uppdragsradarn.application.dto.AssignmentDto;
import com.uppdragsradarn.application.dto.SearchCriteriaDto;
import com.uppdragsradarn.application.dto.SourceDto;
import com.uppdragsradarn.application.service.AssignmentService;
import com.uppdragsradarn.crawler.CrawlerTestUtils;
import com.uppdragsradarn.domain.exception.ResourceNotFoundException;
import com.uppdragsradarn.domain.model.Assignment;
import com.uppdragsradarn.domain.model.Source;
import com.uppdragsradarn.domain.model.SourceType;

@ExtendWith(MockitoExtension.class)
public class AssignmentControllerTest {

  private MockMvc mockMvc;
  private ObjectMapper objectMapper = new ObjectMapper();

  @Mock private AssignmentService assignmentService;

  @InjectMocks private AssignmentController assignmentController;

  private List<AssignmentDto> assignmentDtos;
  private Page<AssignmentDto> assignmentPage;
  private UUID assignmentId;
  private SearchCriteriaDto searchCriteria;
  private Assignment assignment;
  private UUID premiumAssignmentId;

  @BeforeEach
  void setUp() {
    PageableHandlerMethodArgumentResolver pageableResolver =
        new PageableHandlerMethodArgumentResolver();

    mockMvc =
        MockMvcBuilders.standaloneSetup(assignmentController)
            .setCustomArgumentResolvers(pageableResolver)
            .setViewResolvers((viewName, locale) -> new MappingJackson2JsonView())
            .build();

    assignmentId = UUID.randomUUID();
    premiumAssignmentId = UUID.randomUUID();
    UUID sourceId = UUID.randomUUID();

    assignmentDtos = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      assignmentDtos.add(
          AssignmentDto.builder()
              .id(UUID.randomUUID())
              .title("Test Assignment " + i)
              .description("Test Description " + i)
              .companyName("Test Company " + i)
              .location("Test Location " + i)
              .source(
                  SourceDto.builder()
                      .id(sourceId)
                      .name("Test Source")
                      .baseUrl("https://example.com")
                      .build())
              .applicationUrl("https://example.com/job/" + i)
              .build());
    }

    assignmentDtos.get(0).setId(assignmentId);

    assignmentPage = new PageImpl<>(assignmentDtos, PageRequest.of(0, 20), assignmentDtos.size());

    searchCriteria = new SearchCriteriaDto();
    searchCriteria.setKeyword("test");

    Source source = new Source();
    source.setId(sourceId);
    source.setName("Test Source");
    source.setBaseUrl("https://example.com");

    // Add sourceType to fix NullPointerException
    SourceType sourceType = CrawlerTestUtils.createTestSourceType("TEST_SOURCE_TYPE");
    source.setSourceType(sourceType);

    assignment = new Assignment();
    assignment.setId(assignmentId);
    assignment.setTitle("Test Assignment");
    assignment.setCompanyName("Test Company");
    assignment.setOriginalLocationText("Test Location");
    assignment.setSource(source);
    assignment.setActive(true);
    assignment.setCreatedAt(LocalDateTime.now());

    lenient()
        .when(
            assignmentService.searchAssignments(any(SearchCriteriaDto.class), any(Pageable.class)))
        .thenReturn(assignmentPage);
    lenient()
        .when(assignmentService.searchByTitle(any(String.class), any(Pageable.class)))
        .thenReturn(assignmentPage);
    lenient()
        .when(assignmentService.getAllAssignments(any(Pageable.class)))
        .thenReturn(assignmentPage);
    lenient()
        .when(assignmentService.getAssignmentById(eq(assignmentId)))
        .thenReturn(assignmentDtos.get(0));

    ResourceNotFoundException timeGatingException =
        new ResourceNotFoundException("Assignment is not yet available for your subscription tier");
    lenient()
        .when(assignmentService.getAssignmentById(eq(premiumAssignmentId)))
        .thenThrow(timeGatingException);
    lenient()
        .when(assignmentService.getAssignmentEntityWithoutTimeGating(eq(premiumAssignmentId)))
        .thenReturn(assignment);

    lenient()
        .when(assignmentService.searchByTitle(eq("error"), any(Pageable.class)))
        .thenThrow(new RuntimeException("Search error"));
  }

  @Test
  void searchAssignments_ValidInput_Returns200() throws Exception {
    mockMvc
        .perform(
            post("/api/assignments/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(searchCriteria)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.length()").value(3))
        .andExpect(jsonPath("$.totalElements").value(3));
  }

  @Test
  void searchAssignments_NullKeyword_Returns200() throws Exception {
    searchCriteria.setKeyword(null);

    mockMvc
        .perform(
            post("/api/assignments/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(searchCriteria)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.length()").value(3));
  }

  @Test
  void searchAssignments_ExceptionThrown_ReturnsEmptyPage() throws Exception {
    when(assignmentService.searchAssignments(any(SearchCriteriaDto.class), any(Pageable.class)))
        .thenThrow(new RuntimeException("Search error"));

    mockMvc
        .perform(
            post("/api/assignments/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(searchCriteria)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.length()").value(0))
        .andExpect(jsonPath("$.totalElements").value(0));
  }

  @Test
  void searchByKeyword_ValidKeyword_Returns200() throws Exception {
    mockMvc
        .perform(
            get("/api/assignments/search")
                .param("keyword", "test")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.length()").value(3));
  }

  @Test
  void searchByKeyword_NullKeyword_Returns200() throws Exception {
    when(assignmentService.searchByTitle(null, Pageable.unpaged())).thenReturn(assignmentPage);

    ResponseEntity<Page<AssignmentDto>> response =
        assignmentController.searchByKeyword(null, Pageable.unpaged());

    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertNotNull(response.getBody() != null);
    assertEquals(3, response.getBody().getContent().size());
  }

  @Test
  void searchByKeyword_ExceptionThrown_ReturnsEmptyPage() throws Exception {
    mockMvc
        .perform(
            get("/api/assignments/search")
                .param("keyword", "error")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.length()").value(0))
        .andExpect(jsonPath("$.totalElements").value(0));
  }

  @Test
  void getAllAssignments_NoKeyword_Returns200() throws Exception {
    mockMvc
        .perform(get("/api/assignments").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.length()").value(3))
        .andExpect(jsonPath("$.totalElements").value(3))
        .andExpect(jsonPath("$.currentPage").value(0))
        .andExpect(jsonPath("$.empty").value(false));
  }

  @Test
  void getAllAssignments_WithKeyword_Returns200() throws Exception {
    mockMvc
        .perform(
            get("/api/assignments")
                .param("keyword", "test")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.length()").value(3))
        .andExpect(jsonPath("$.totalElements").value(3));
  }

  @Test
  void getAllAssignments_ExceptionThrown_ReturnsEmptyResult() throws Exception {
    when(assignmentService.getAllAssignments(any(Pageable.class)))
        .thenThrow(new RuntimeException("Database error"));

    mockMvc
        .perform(get("/api/assignments").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.length()").value(0))
        .andExpect(jsonPath("$.totalElements").value(0))
        .andExpect(jsonPath("$.empty").value(true));
  }

  @Test
  void getAssignmentById_ExistingAssignment_Returns200() throws Exception {
    mockMvc
        .perform(
            get("/api/assignments/{assignmentId}", assignmentId)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(assignmentId.toString()));
  }

  @Test
  void getAssignmentById_PremiumOnlyAssignment_ReturnsLimitedData() throws Exception {
    ResourceNotFoundException timeGatingException =
        new ResourceNotFoundException("Assignment is not yet available for your subscription tier");
    when(assignmentService.getAssignmentById(eq(premiumAssignmentId)))
        .thenThrow(timeGatingException);
    when(assignmentService.getAssignmentEntityWithoutTimeGating(eq(premiumAssignmentId)))
        .thenReturn(assignment);

    ResponseEntity<AssignmentDto> response =
        assignmentController.getAssignmentById(premiumAssignmentId);

    assert (response.getStatusCode().is2xxSuccessful());
    AssignmentDto dto = response.getBody();
    assertNotNull(dto);
    assertEquals(assignment.getId(), dto.getId());
    assertEquals(assignment.getTitle(), dto.getTitle());
    assertTrue(dto.isPremiumOnly());
  }

  @Test
  void getAssignmentById_NonExistentAssignment_Returns404() throws Exception {
    UUID nonExistentId = UUID.randomUUID();

    when(assignmentService.getAssignmentById(eq(nonExistentId)))
        .thenThrow(new ResourceNotFoundException("Assignment not found"));

    mockMvc
        .perform(
            get("/api/assignments/{assignmentId}", nonExistentId)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }
}

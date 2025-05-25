package com.uppdragsradarn.infrastructure.web;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.uppdragsradarn.application.dto.AssignmentDto;
import com.uppdragsradarn.application.dto.SearchCriteriaDto;
import com.uppdragsradarn.application.service.AssignmentService;
import com.uppdragsradarn.domain.exception.ResourceNotFoundException;
import com.uppdragsradarn.domain.model.Assignment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Controller for public assignment operations */
@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Public Assignments", description = "Public APIs for accessing assignments")
public class AssignmentController {

  private final AssignmentService assignmentService;

  @Operation(summary = "Search assignments without authentication")
  @PostMapping("/search")
  public ResponseEntity<Page<AssignmentDto>> searchAssignments(
      @RequestBody SearchCriteriaDto searchCriteria,
      @PageableDefault(size = 20) Pageable pageable) {
    try {
      // Basic clean-up - just trim whitespace
      if (searchCriteria != null && searchCriteria.getKeyword() != null) {
        searchCriteria.setKeyword(searchCriteria.getKeyword().trim());
      }

      return ResponseEntity.ok(assignmentService.searchAssignments(searchCriteria, pageable));
    } catch (Exception e) {
      log.error("Search error: {}", e.getMessage());
      return ResponseEntity.ok(new PageImpl<>(java.util.Collections.emptyList(), pageable, 0));
    }
  }

  @Operation(summary = "Search assignments by keyword directly (simplified)")
  @GetMapping("/search")
  public ResponseEntity<Page<AssignmentDto>> searchByKeyword(
      @RequestParam(required = false) String keyword,
      @PageableDefault(size = 20) Pageable pageable) {
    try {
      if (keyword != null) {
        keyword = keyword.trim();
      }

      return ResponseEntity.ok(assignmentService.searchByTitle(keyword, pageable));
    } catch (Exception e) {
      log.error("Search error: {}", e.getMessage());
      return ResponseEntity.ok(new PageImpl<>(java.util.Collections.emptyList(), pageable, 0));
    }
  }

  /** Get all assignments or search by keyword with simplified response */
  @Operation(summary = "Get all assignments or search by keyword")
  @GetMapping
  public ResponseEntity<Map<String, Object>> getAllAssignments(
      @RequestParam(required = false) String keyword,
      @PageableDefault(size = 20) Pageable pageable) {
    try {
      Page<AssignmentDto> assignmentPage;

      // If keyword is provided, do a search. Otherwise, get all assignments
      if (keyword != null && !keyword.trim().isEmpty()) {
        assignmentPage = assignmentService.searchByTitle(keyword.trim(), pageable);
      } else {
        assignmentPage = assignmentService.getAllAssignments(pageable);
      }

      // Create a simplified response format
      Map<String, Object> response = new HashMap<>();
      response.put("content", assignmentPage.getContent());
      response.put("totalElements", assignmentPage.getTotalElements());
      response.put("totalPages", assignmentPage.getTotalPages());
      response.put("currentPage", assignmentPage.getNumber());
      response.put("size", assignmentPage.getSize());
      response.put("hasNext", assignmentPage.hasNext());
      response.put("hasPrevious", assignmentPage.hasPrevious());
      response.put("empty", assignmentPage.isEmpty());

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error retrieving assignments: {}", e.getMessage());

      // Return empty response with the same structure
      Map<String, Object> emptyResponse = new HashMap<>();
      emptyResponse.put("content", Collections.emptyList());
      emptyResponse.put("totalElements", 0);
      emptyResponse.put("totalPages", 0);
      emptyResponse.put("currentPage", pageable.getPageNumber());
      emptyResponse.put("size", pageable.getPageSize());
      emptyResponse.put("hasNext", false);
      emptyResponse.put("hasPrevious", false);
      emptyResponse.put("empty", true);

      return ResponseEntity.ok(emptyResponse);
    }
  }

  @Operation(summary = "Get assignment by ID without authentication")
  @GetMapping("/{assignmentId}")
  public ResponseEntity<AssignmentDto> getAssignmentById(@PathVariable UUID assignmentId) {
    try {
      return ResponseEntity.ok(assignmentService.getAssignmentById(assignmentId));
    } catch (ResourceNotFoundException e) {
      // Check if this is a time-gating error (only available to premium users)
      if (e.getMessage().contains("not yet available")) {
        // Create a simplified version of the assignment with only basic info
        // but marked as premium only
        Assignment assignment =
            assignmentService.getAssignmentEntityWithoutTimeGating(assignmentId);
        AssignmentDto limitedDto = createLimitedAssignmentDto(assignment);
        return ResponseEntity.ok(limitedDto);
      }
      // For any other errors, just re-throw
      throw e;
    }
  }

  /**
   * Creates a limited version of an assignment DTO for free users showing only basic information
   * and marking it as premium-only
   */
  private AssignmentDto createLimitedAssignmentDto(Assignment assignment) {
    return AssignmentDto.fromEntity(assignment, true);
  }
}

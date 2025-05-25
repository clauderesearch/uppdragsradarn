package com.uppdragsradarn.infrastructure.web.admin;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uppdragsradarn.application.service.AssignmentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Controller for search index operations */
@RestController
@RequestMapping("/search-index")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Search Index Operations", description = "APIs for managing search indexes")
public class AdminSearchIndexController {

  private final AssignmentService assignmentService;

  @Operation(summary = "Reindex all assignments (no-op)")
  @PostMapping("/reindex")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Map<String, Object>> reindexAll() {
    log.info("Reindex operation requested (no-op)");
    // This is now a no-op since we're not using Elasticsearch anymore
    return ResponseEntity.ok(
        Map.of(
            "success",
            true,
            "message",
            "Reindexing is no longer needed (using database search)",
            "count",
            0));
  }
}

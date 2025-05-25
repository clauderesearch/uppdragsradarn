package com.uppdragsradarn.infrastructure.web.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.uppdragsradarn.application.dto.AssignmentDto;
import com.uppdragsradarn.application.service.AssignmentService;
import com.uppdragsradarn.domain.model.User;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/assignments")
@RequiredArgsConstructor
public class AdminAssignmentController {

  private final AssignmentService assignmentService;

  @GetMapping("/pending-review")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Page<AssignmentDto>> getPendingReview(
      @AuthenticationPrincipal User user, Pageable pageable) {

    Page<AssignmentDto> pendingAssignments = assignmentService.findPendingReview(pageable);
    return ResponseEntity.ok(pendingAssignments);
  }

  @PostMapping("/{id}/approve")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> approveAssignment(
      @AuthenticationPrincipal User user, @PathVariable String id) {

    assignmentService.approveAssignment(id);
    return ResponseEntity.ok().build();
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<AssignmentDto> updateAssignment(
      @AuthenticationPrincipal User user,
      @PathVariable String id,
      @RequestBody AssignmentDto updates) {

    AssignmentDto updated = assignmentService.updateAssignment(id, updates);
    return ResponseEntity.ok(updated);
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Page<AssignmentDto>> getAllAssignments(
      @AuthenticationPrincipal User user, Pageable pageable) {

    Page<AssignmentDto> assignments = assignmentService.getAllAssignments(pageable);
    return ResponseEntity.ok(assignments);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<AssignmentDto> getAssignment(
      @AuthenticationPrincipal User user, @PathVariable String id) {

    AssignmentDto assignment = assignmentService.findById(id);
    return ResponseEntity.ok(assignment);
  }
}

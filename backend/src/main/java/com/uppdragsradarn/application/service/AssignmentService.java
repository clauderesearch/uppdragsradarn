package com.uppdragsradarn.application.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.uppdragsradarn.application.dto.AssignmentDto;
import com.uppdragsradarn.application.dto.SearchCriteriaDto;
import com.uppdragsradarn.application.dto.UserAssignmentDto;
import com.uppdragsradarn.domain.model.Assignment;

public interface AssignmentService {

  /**
   * Get all active assignments
   *
   * @param pageable Pagination information
   * @return A page of all active assignments
   */
  Page<AssignmentDto> getAllAssignments(Pageable pageable);

  /**
   * Search assignments by title
   *
   * @param title Title to search for
   * @param pageable Pagination information
   * @return A page of assignments with titles matching the search term
   */
  Page<AssignmentDto> searchByTitle(String title, Pageable pageable);

  /**
   * Search assignments based on criteria
   *
   * @param searchCriteria The search criteria
   * @param pageable Pagination information
   * @return A page of assignments matching the criteria
   */
  Page<AssignmentDto> searchAssignments(SearchCriteriaDto searchCriteria, Pageable pageable);

  /**
   * Get assignment by ID
   *
   * @param assignmentId The assignment ID
   * @return The assignment DTO
   * @throws ResourceNotFoundException if assignment not found
   */
  AssignmentDto getAssignmentById(UUID assignmentId);

  /**
   * Get assignment entity by ID
   *
   * @param assignmentId The assignment ID
   * @return The assignment entity
   * @throws ResourceNotFoundException if assignment not found
   */
  Assignment getAssignmentEntityById(UUID assignmentId);

  /**
   * Mark interest in an assignment
   *
   * @param assignmentId The assignment ID
   * @param userAssignmentDto The user assignment details
   * @return The created/updated user assignment
   */
  UserAssignmentDto markAssignmentInterest(UUID assignmentId, UserAssignmentDto userAssignmentDto);

  /**
   * Get all assignments for a user
   *
   * @param userId The user ID
   * @param pageable Pagination information
   * @return A page of assignments for the user
   */
  Page<AssignmentDto> getUserAssignments(UUID userId, Pageable pageable);

  /**
   * Get assignments for a user by status
   *
   * @param userId The user ID
   * @param status The assignment status
   * @param pageable Pagination information
   * @return A page of assignments for the user with the given status
   */
  Page<AssignmentDto> getUserAssignmentsByStatus(UUID userId, String status, Pageable pageable);

  /**
   * Create a new assignment
   *
   * @param assignment The assignment to create
   * @return The created assignment
   */
  Assignment createAssignment(Assignment assignment);

  /**
   * Update an existing assignment
   *
   * @param assignment The assignment to update
   * @return The updated assignment
   */
  Assignment updateAssignment(Assignment assignment);

  /**
   * Count all active assignments
   *
   * @return The number of active assignments
   */
  int countActiveAssignments();

  /**
   * Get assignment entity by ID without applying time-gating logic This is used to retrieve basic
   * assignment info for free users when the assignment is premium-only.
   *
   * @param assignmentId The assignment ID
   * @return The assignment entity
   * @throws ResourceNotFoundException if assignment not found
   */
  Assignment getAssignmentEntityWithoutTimeGating(UUID assignmentId);

  /**
   * Find assignments pending review (with PII detected)
   *
   * @param pageable Pagination information
   * @return A page of assignments needing review
   */
  Page<AssignmentDto> findPendingReview(Pageable pageable);

  /**
   * Approve an assignment (remove manual review flag)
   *
   * @param id The assignment ID
   */
  void approveAssignment(String id);

  /**
   * Update an assignment
   *
   * @param id The assignment ID
   * @param updates The updated assignment data
   * @return The updated assignment DTO
   */
  AssignmentDto updateAssignment(String id, AssignmentDto updates);

  /**
   * Find assignment by ID (as string)
   *
   * @param id The assignment ID
   * @return The assignment DTO
   */
  AssignmentDto findById(String id);
}

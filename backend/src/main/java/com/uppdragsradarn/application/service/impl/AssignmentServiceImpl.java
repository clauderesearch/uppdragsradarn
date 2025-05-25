package com.uppdragsradarn.application.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uppdragsradarn.application.dto.AssignmentDto;
import com.uppdragsradarn.application.dto.SearchCriteriaDto;
import com.uppdragsradarn.application.dto.UserAssignmentDto;
import com.uppdragsradarn.application.service.AssignmentService;
import com.uppdragsradarn.application.service.UserService;
import com.uppdragsradarn.domain.exception.ResourceNotFoundException;
import com.uppdragsradarn.domain.model.Assignment;
import com.uppdragsradarn.domain.model.StatusType;
import com.uppdragsradarn.domain.model.User;
import com.uppdragsradarn.domain.model.UserAssignment;
import com.uppdragsradarn.domain.repository.AssignmentRepository;
import com.uppdragsradarn.domain.repository.StatusTypeRepository;
import com.uppdragsradarn.domain.repository.UserAssignmentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssignmentServiceImpl implements AssignmentService {

  private final AssignmentRepository assignmentRepository;
  private final UserAssignmentRepository userAssignmentRepository;
  private final UserService userService;
  private final StatusTypeRepository statusTypeRepository;

  // Time delay for free users in hours
  private static final long FREE_USER_DELAY_HOURS = 72;

  @Override
  @Transactional(readOnly = true)
  public Page<AssignmentDto> getAllAssignments(Pageable pageable) {
    // Get current user
    Optional<User> currentUser = userService.getCurrentUser();
    boolean isPremium = currentUser.isPresent() && currentUser.get().isPremium();

    // Get all active assignments
    Page<Assignment> assignments =
        assignmentRepository.findVisibleAssignments(currentUser.orElse(null), pageable);

    // Map to DTOs with appropriate filtering for free users
    return assignments.map(
        assignment -> {
          boolean isNewlyPosted = isNewlyPosted(assignment);

          // Premium users get full details
          if (isPremium) {
            return AssignmentDto.fromEntity(assignment);
          }
          // Free users get limited details for newly posted assignments
          else if (isNewlyPosted) {
            return AssignmentDto.fromEntity(assignment, true);
          }
          // Free users get full details for older assignments
          else {
            return AssignmentDto.fromEntity(assignment);
          }
        });
  }

  /** Helper method to determine if an assignment is newly posted */
  private boolean isNewlyPosted(Assignment assignment) {
    if (assignment.getCreatedAt() == null) {
      return false;
    }

    // Calculate hours since assignment was created
    long hoursSinceCreation =
        java.time.Duration.between(assignment.getCreatedAt(), java.time.LocalDateTime.now())
            .toHours();

    // Assignment is newly posted if it's less than 48 hours old
    return hoursSinceCreation < 48;
  }

  @Override
  @Transactional(readOnly = true)
  public Page<AssignmentDto> searchByTitle(String title, Pageable pageable) {
    // Get current user
    Optional<User> currentUser = userService.getCurrentUser();
    boolean isPremium = currentUser.isPresent() && currentUser.get().isPremium();

    Page<Assignment> assignments;

    // If title is null or empty, return all visible assignments
    if (title == null || title.trim().isEmpty()) {
      assignments = assignmentRepository.findVisibleAssignments(currentUser.orElse(null), pageable);
    } else {
      // Search by title
      assignments =
          assignmentRepository.findVisibleAssignmentsByTitle(
              title.trim(), currentUser.orElse(null), pageable);
    }

    // Map to DTOs with appropriate filtering for free users
    return assignments.map(
        assignment -> {
          boolean isNewlyPosted = isNewlyPosted(assignment);

          // Premium users get full details
          if (isPremium) {
            return AssignmentDto.fromEntity(assignment);
          }
          // Free users get limited details for newly posted assignments
          else if (isNewlyPosted) {
            return AssignmentDto.fromEntity(assignment, true);
          }
          // Free users get full details for older assignments
          else {
            return AssignmentDto.fromEntity(assignment);
          }
        });
  }

  @Override
  @Transactional(readOnly = true)
  public Page<AssignmentDto> searchAssignments(
      SearchCriteriaDto searchCriteria, Pageable pageable) {
    // If search criteria is null, return all active assignments
    if (searchCriteria == null) {
      return getAllAssignments(pageable);
    }

    // Get current user
    Optional<User> currentUser = userService.getCurrentUser();
    boolean isPremium = currentUser.isPresent() && currentUser.get().isPremium();

    Page<Assignment> assignments;

    // If keyword is provided
    if (searchCriteria.getKeyword() != null && !searchCriteria.getKeyword().trim().isEmpty()) {
      String keyword = searchCriteria.getKeyword().trim();

      // If location is also provided, use both for search
      if (searchCriteria.getLocation() != null && !searchCriteria.getLocation().trim().isEmpty()) {
        String location = searchCriteria.getLocation().trim();

        // Use the specialized query that checks both normalized locations and legacy location field
        assignments = assignmentRepository.findByTitleAndLocation(keyword, location, pageable);
      } else {
        // Just search by title if no location is provided
        assignments =
            assignmentRepository.findVisibleAssignmentsByTitle(
                keyword, currentUser.orElse(null), pageable);
      }
    }
    // If only location is provided (no keyword)
    else if (searchCriteria.getLocation() != null
        && !searchCriteria.getLocation().trim().isEmpty()) {
      String location = searchCriteria.getLocation().trim();

      // Use empty string for title to match all titles, but filter by location
      assignments = assignmentRepository.findByTitleAndLocation("", location, pageable);
    }
    // No specific search criteria, return all assignments
    else {
      assignments = assignmentRepository.findVisibleAssignments(currentUser.orElse(null), pageable);
    }

    // Map to DTOs with appropriate filtering for free users
    return assignments.map(
        assignment -> {
          boolean isNewlyPosted = isNewlyPosted(assignment);

          // Premium users get full details
          if (isPremium) {
            return AssignmentDto.fromEntity(assignment);
          }
          // Free users get limited details for newly posted assignments
          else if (isNewlyPosted) {
            return AssignmentDto.fromEntity(assignment, true);
          }
          // Free users get full details for older assignments
          else {
            return AssignmentDto.fromEntity(assignment);
          }
        });
  }

  @Override
  @Transactional(readOnly = true)
  public AssignmentDto getAssignmentById(UUID assignmentId) {
    // Get current user
    Optional<User> currentUser = userService.getCurrentUser();
    boolean isPremium = currentUser.isPresent() && currentUser.get().isPremium();

    // Get the assignment
    Assignment assignment =
        assignmentRepository
            .findVisibleAssignmentById(assignmentId, currentUser.orElse(null))
            .orElseThrow(
                () ->
                    new ResourceNotFoundException("Assignment not found with id: " + assignmentId));

    boolean isNewlyPosted = isNewlyPosted(assignment);

    // Premium users get full details
    if (isPremium) {
      return AssignmentDto.fromEntity(assignment);
    }
    // Free users get limited details for newly posted assignments
    else if (isNewlyPosted) {
      return AssignmentDto.fromEntity(assignment, true);
    }
    // Free users get full details for older assignments
    else {
      return AssignmentDto.fromEntity(assignment);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public Assignment getAssignmentEntityById(UUID assignmentId) {
    // Get current user
    Optional<User> currentUser = userService.getCurrentUser();

    // Get the assignment with time gating applied at the repository level
    return assignmentRepository
        .findVisibleAssignmentById(assignmentId, currentUser.orElse(null))
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    "Assignment not found or not yet available with id: " + assignmentId));
  }

  @Override
  @Transactional
  public UserAssignmentDto markAssignmentInterest(
      UUID assignmentId, UserAssignmentDto userAssignmentDto) {
    Optional<User> currentUser = userService.getCurrentUser();

    if (currentUser.isEmpty()) {
      throw new IllegalStateException("No authenticated user found");
    }

    Assignment assignment = getAssignmentEntityById(assignmentId);
    User user = currentUser.get();

    UserAssignment userAssignment =
        userAssignmentRepository
            .findByUserAndAssignment(user, assignment)
            .orElse(new UserAssignment());

    userAssignment.setUser(user);
    userAssignment.setAssignment(assignment);
    StatusType status =
        statusTypeRepository
            .findById(userAssignmentDto.getStatusId())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Status with ID " + userAssignmentDto.getStatusId() + " not found"));
    userAssignment.setStatus(status);
    userAssignment.setNotes(userAssignmentDto.getNotes());

    userAssignment = userAssignmentRepository.save(userAssignment);

    return UserAssignmentDto.fromEntity(userAssignment);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<AssignmentDto> getUserAssignments(UUID userId, Pageable pageable) {
    User user = userService.getUserEntityById(userId);
    // Get current user for time gating check
    Optional<User> currentUser = userService.getCurrentUser();

    Page<UserAssignment> userAssignmentsPage = userAssignmentRepository.findByUser(user, pageable);

    // Filter assignments by time-gating rules at repository level
    List<AssignmentDto> visibleAssignments =
        userAssignmentsPage.getContent().stream()
            .map(UserAssignment::getAssignment)
            .filter(
                assignment ->
                    assignmentRepository
                        .findVisibleAssignmentById(assignment.getId(), currentUser.orElse(null))
                        .isPresent())
            .map(AssignmentDto::fromEntity)
            .collect(Collectors.toList());

    // Return as page
    return new PageImpl<>(
        visibleAssignments,
        pageable,
        visibleAssignments.size() // Use actual count since we've already filtered
        );
  }

  @Override
  @Transactional(readOnly = true)
  public Page<AssignmentDto> getUserAssignmentsByStatus(
      UUID userId, String status, Pageable pageable) {
    User user = userService.getUserEntityById(userId);
    // Find status type for USER_ASSIGNMENT with the given name
    StatusType statusType =
        statusTypeRepository
            .findByNameAndEntityType(status.toUpperCase(), "USER_ASSIGNMENT")
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Status with name " + status.toUpperCase() + " not found"));
    // Get current user for time gating check
    Optional<User> currentUser = userService.getCurrentUser();

    Page<UserAssignment> userAssignmentsPage =
        userAssignmentRepository.findByUserAndStatus(user, statusType, pageable);

    // Filter assignments by time-gating rules at repository level
    List<AssignmentDto> visibleAssignments =
        userAssignmentsPage.getContent().stream()
            .map(UserAssignment::getAssignment)
            .filter(
                assignment ->
                    assignmentRepository
                        .findVisibleAssignmentById(assignment.getId(), currentUser.orElse(null))
                        .isPresent())
            .map(AssignmentDto::fromEntity)
            .collect(Collectors.toList());

    // Return as page
    return new PageImpl<>(
        visibleAssignments,
        pageable,
        visibleAssignments.size() // Use actual count since we've already filtered
        );
  }

  @Override
  @Transactional
  public Assignment createAssignment(Assignment assignment) {
    // Ensure timestamps are set
    if (assignment.getCreatedAt() == null) {
      assignment.setCreatedAt(LocalDateTime.now());
    }
    assignment.setUpdatedAt(LocalDateTime.now());

    // Set the originalLocationText field for compatibility with old code
    if (assignment.getAssignmentLocations() != null
        && !assignment.getAssignmentLocations().isEmpty()) {
      assignment.setOriginalLocationText(assignment.getFormattedLocation());
    }

    // Save to database
    Assignment savedAssignment = assignmentRepository.save(assignment);

    return savedAssignment;
  }

  @Override
  @Transactional
  public Assignment updateAssignment(Assignment assignment) {
    assignment.setUpdatedAt(LocalDateTime.now());

    // Set the originalLocationText field for compatibility with old code
    if (assignment.getAssignmentLocations() != null
        && !assignment.getAssignmentLocations().isEmpty()) {
      assignment.setOriginalLocationText(assignment.getFormattedLocation());
    }

    // Save to database
    Assignment updatedAssignment = assignmentRepository.save(assignment);

    return updatedAssignment;
  }

  @Override
  @Transactional(readOnly = true)
  public int countActiveAssignments() {
    log.info("Counting active assignments");
    int count = assignmentRepository.findByActiveTrueAndNeedsManualReviewFalse().size();
    log.info("Current active assignment count: {}", count);
    return count;
  }

  @Override
  @Transactional(readOnly = true)
  public Assignment getAssignmentEntityWithoutTimeGating(UUID assignmentId) {
    return assignmentRepository
        .findById(assignmentId)
        .orElseThrow(
            () -> new ResourceNotFoundException("Assignment not found with id: " + assignmentId));
  }

  // Time gating logic has been moved to the repository layer

  @Override
  @Transactional(readOnly = true)
  public Page<AssignmentDto> findPendingReview(Pageable pageable) {
    Page<Assignment> pendingAssignments =
        assignmentRepository.findByNeedsManualReviewTrue(pageable);
    return pendingAssignments.map(AssignmentDto::fromEntity);
  }

  @Override
  @Transactional
  public void approveAssignment(String id) {
    UUID assignmentId = UUID.fromString(id);
    Assignment assignment =
        assignmentRepository
            .findById(assignmentId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Assignment not found with id: " + id));

    assignment.setNeedsManualReview(false);
    assignment.setPiiDetected("");
    assignment.setUpdatedAt(LocalDateTime.now());

    assignmentRepository.save(assignment);
  }

  @Override
  @Transactional
  public AssignmentDto updateAssignment(String id, AssignmentDto updates) {
    UUID assignmentId = UUID.fromString(id);
    Assignment assignment =
        assignmentRepository
            .findById(assignmentId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Assignment not found with id: " + id));

    // Update fields
    if (updates.getTitle() != null) {
      assignment.setTitle(updates.getTitle());
    }
    if (updates.getCompanyName() != null) {
      assignment.setCompanyName(updates.getCompanyName());
    }

    // We're only updating the original location text if no normalized locations exist
    if (updates.getLocation() != null
        && (assignment.getAssignmentLocations() == null
            || assignment.getAssignmentLocations().isEmpty())) {
      assignment.setOriginalLocationText(updates.getLocation());
    }

    if (updates.getDescription() != null) {
      assignment.setDescription(updates.getDescription());
    }
    assignment.setNeedsManualReview(updates.isNeedsManualReview());
    assignment.setUpdatedAt(LocalDateTime.now());

    // Set the originalLocationText field for compatibility with old code
    if (assignment.getAssignmentLocations() != null
        && !assignment.getAssignmentLocations().isEmpty()) {
      assignment.setOriginalLocationText(assignment.getFormattedLocation());
    }

    Assignment saved = assignmentRepository.save(assignment);
    return AssignmentDto.fromEntity(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public AssignmentDto findById(String id) {
    UUID assignmentId = UUID.fromString(id);
    Assignment assignment =
        assignmentRepository
            .findById(assignmentId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Assignment not found with id: " + id));

    return AssignmentDto.fromEntity(assignment);
  }
}

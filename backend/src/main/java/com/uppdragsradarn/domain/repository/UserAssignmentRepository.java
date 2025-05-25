package com.uppdragsradarn.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uppdragsradarn.domain.model.Assignment;
import com.uppdragsradarn.domain.model.StatusType;
import com.uppdragsradarn.domain.model.User;
import com.uppdragsradarn.domain.model.UserAssignment;

@Repository
public interface UserAssignmentRepository extends JpaRepository<UserAssignment, UUID> {

  Page<UserAssignment> findByUser(User user, Pageable pageable);

  Page<UserAssignment> findByUserAndStatus(User user, StatusType status, Pageable pageable);

  Optional<UserAssignment> findByUserAndAssignment(User user, Assignment assignment);

  boolean existsByUserAndAssignment(User user, Assignment assignment);
}

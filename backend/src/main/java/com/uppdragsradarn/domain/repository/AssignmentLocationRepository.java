package com.uppdragsradarn.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uppdragsradarn.domain.model.Assignment;
import com.uppdragsradarn.domain.model.AssignmentLocation;
import com.uppdragsradarn.domain.model.Location;

@Repository
public interface AssignmentLocationRepository extends JpaRepository<AssignmentLocation, UUID> {

  /** Find all location associations for an assignment */
  List<AssignmentLocation> findByAssignment(Assignment assignment);

  /** Find primary location for an assignment */
  Optional<AssignmentLocation> findByAssignmentAndPrimaryTrue(Assignment assignment);

  /** Find all assignments for a location */
  List<AssignmentLocation> findByLocation(Location location);

  /** Find assignment-location association by both entities */
  Optional<AssignmentLocation> findByAssignmentAndLocation(
      Assignment assignment, Location location);

  /** Find remote-enabled assignments */
  List<AssignmentLocation> findByRemoteTrue();

  /** Delete all location associations for an assignment */
  void deleteByAssignment(Assignment assignment);
}

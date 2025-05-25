package com.uppdragsradarn.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uppdragsradarn.domain.model.StatusType;

@Repository
public interface StatusTypeRepository extends JpaRepository<StatusType, UUID> {

  /**
   * Find a status type by name and entity type
   *
   * @param name The name of the status type
   * @param entityType The entity type the status applies to
   * @return Optional of StatusType
   */
  Optional<StatusType> findByNameAndEntityType(String name, String entityType);
}

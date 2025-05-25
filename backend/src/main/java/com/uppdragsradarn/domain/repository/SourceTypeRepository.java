package com.uppdragsradarn.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uppdragsradarn.domain.model.SourceType;

@Repository
public interface SourceTypeRepository extends JpaRepository<SourceType, UUID> {
  Optional<SourceType> findByName(String name);
}

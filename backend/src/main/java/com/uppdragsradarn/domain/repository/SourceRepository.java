package com.uppdragsradarn.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uppdragsradarn.domain.model.Source;
import com.uppdragsradarn.domain.model.SourceType;

@Repository
public interface SourceRepository extends JpaRepository<Source, UUID> {

  Optional<Source> findByName(String name);

  List<Source> findByActiveTrue();

  List<Source> findBySourceTypeAndActiveTrue(SourceType sourceType);
}

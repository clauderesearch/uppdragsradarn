package com.uppdragsradarn.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.uppdragsradarn.domain.model.Location;
import com.uppdragsradarn.domain.model.LocationAlias;

@Repository
public interface LocationAliasRepository extends JpaRepository<LocationAlias, UUID> {

  /** Find aliases by exact text match */
  Optional<LocationAlias> findByAliasTextAndIsActiveTrue(String aliasText);

  /** Find aliases by canonical location */
  List<LocationAlias> findByLocationAndIsActiveTrue(Location location);

  /** Find aliases for a provider source */
  List<LocationAlias> findBySourceProviderAndIsActiveTrue(String sourceProvider);

  /** Find by source text and provider */
  Optional<LocationAlias> findBySourceTextAndSourceProviderAndIsActiveTrue(
      String sourceText, String sourceProvider);

  /** Search for close matches to help with fuzzy matching */
  @Query(
      value =
          "SELECT * FROM location_aliases "
              + "WHERE is_active = true "
              + "ORDER BY similarity(alias_text, :text) DESC "
              + "LIMIT :limit",
      nativeQuery = true)
  List<LocationAlias> findSimilarAliases(@Param("text") String text, @Param("limit") int limit);

  /** Find aliases containing a substring (case insensitive) */
  List<LocationAlias> findByAliasTextContainingIgnoreCaseAndIsActiveTrue(String partialText);
}

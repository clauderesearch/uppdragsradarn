package com.uppdragsradarn.domain.model;

import java.time.LocalDateTime;
import java.util.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "sources")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"assignments"})
public class Source {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @EqualsAndHashCode.Include
  private UUID id;

  @Column(nullable = false, unique = true)
  private String name;

  @Column(name = "base_url")
  private String baseUrl;

  @ManyToOne
  @JoinColumn(name = "source_type_id", nullable = false)
  private SourceType sourceType;

  @Column(columnDefinition = "TEXT")
  private String configuration;

  @Type(JsonBinaryType.class)
  @Column(name = "parameters", columnDefinition = "jsonb")
  @Builder.Default
  private Map<String, Object> parameters = new HashMap<>();

  @Column(name = "active", nullable = false)
  private boolean active;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @OneToMany(mappedBy = "source", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @Builder.Default
  private Set<Assignment> assignments = new HashSet<>();
}

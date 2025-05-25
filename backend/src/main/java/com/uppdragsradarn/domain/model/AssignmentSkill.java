package com.uppdragsradarn.domain.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "assignment_skills")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(AssignmentSkillId.class)
public class AssignmentSkill {

  @Id
  @ManyToOne
  @JoinColumn(name = "assignment_id", nullable = false)
  private Assignment assignment;

  @Id
  @ManyToOne
  @JoinColumn(name = "skill_id", nullable = false)
  private Skill skill;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
}

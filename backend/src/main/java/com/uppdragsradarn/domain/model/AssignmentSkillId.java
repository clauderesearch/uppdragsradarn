package com.uppdragsradarn.domain.model;

import java.io.Serializable;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentSkillId implements Serializable {
  private static final long serialVersionUID = 1L;

  private UUID assignment;
  private UUID skill;
}

package com.uppdragsradarn.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchCriteriaDto {

  private String keyword;

  private String location;

  private Integer minRemotePercentage;

  private Integer minDurationMonths;

  private LocalDate earliestStartDate;

  private BigDecimal minHourlyRate;

  private Set<String> skills;

  private Integer minHoursPerWeek;

  private Set<String> companyNames;

  private Set<UUID> sourceIds;
}

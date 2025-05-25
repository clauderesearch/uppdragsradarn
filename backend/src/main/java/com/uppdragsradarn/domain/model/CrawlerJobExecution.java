package com.uppdragsradarn.domain.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Entity representing crawler job execution */
@Entity
@Table(name = "crawler_job_executions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrawlerJobExecution {

  @Id private String id;

  @ManyToOne
  @JoinColumn(name = "source_id")
  private Source source;

  @Column(name = "source_name")
  private String sourceName;

  @Column(name = "start_time")
  private LocalDateTime startTime;

  @Column(name = "end_time")
  private LocalDateTime endTime;

  @ManyToOne
  @JoinColumn(name = "status_id")
  private StatusType status;

  @Column(name = "assignments_found")
  private Integer assignmentsFound;

  @Column(name = "assignments_created")
  private Integer assignmentsCreated;

  @Column(name = "assignments_updated")
  private Integer assignmentsUpdated;

  @Column(name = "error_message")
  private String errorMessage;

  @Column(name = "processed_assignment_ids")
  private String processedAssignmentIds;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
}

package com.uppdragsradarn.infrastructure.web.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.uppdragsradarn.application.service.CrawlerService;
import com.uppdragsradarn.domain.model.CrawlerJobExecution;
import com.uppdragsradarn.domain.model.StatusType;

@ExtendWith(MockitoExtension.class)
public class AdminCrawlerControllerTest {

  @Mock private CrawlerService crawlerService;

  @InjectMocks private AdminCrawlerController controller;

  @Test
  void startCrawlerJob_Admin_Returns200() {
    // Setup test data
    UUID sourceId = UUID.randomUUID();
    String jobId = UUID.randomUUID().toString();

    // Setup job execution
    CrawlerJobExecution jobExecution = new CrawlerJobExecution();
    jobExecution.setId(jobId);
    // Note: CrawlerJobExecution now uses Source entity instead of sourceId
    jobExecution.setStartTime(LocalDateTime.now());
    StatusType runningStatus = new StatusType();
    runningStatus.setName("RUNNING");
    runningStatus.setEntityType("CRAWLER_JOB");
    jobExecution.setStatus(runningStatus);

    // Mock the service
    when(crawlerService.startCrawlerJob(eq(sourceId))).thenReturn(jobExecution);

    // Call the controller
    ResponseEntity<Map<String, Object>> response = controller.startCrawlerJob(sourceId);

    // Verify response
    assertEquals(HttpStatus.OK, response.getStatusCode());

    Map<String, Object> body = response.getBody();
    assertNotNull(body);
    assertTrue((Boolean) body.get("success"));
    assertEquals(jobId, body.get("jobId"));
  }

  @Test
  void startCrawlerJob_ServiceError_Returns500() {
    // Setup test data
    UUID sourceId = UUID.randomUUID();

    // Mock the service to throw exception
    when(crawlerService.startCrawlerJob(eq(sourceId)))
        .thenThrow(new RuntimeException("Service error"));

    // Call the controller
    ResponseEntity<Map<String, Object>> response = controller.startCrawlerJob(sourceId);

    // Verify response
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

    Map<String, Object> body = response.getBody();
    assertNotNull(body);
    assertFalse((Boolean) body.get("success"));
    assertNotNull(body.get("message"));
  }

  @Test
  void getCrawlerJobStatus_ExistingJob_Returns200() {
    // Setup test data
    String jobId = UUID.randomUUID().toString();

    // Setup job execution
    CrawlerJobExecution jobExecution = new CrawlerJobExecution();
    jobExecution.setId(jobId);
    StatusType runningStatus = new StatusType();
    runningStatus.setName("RUNNING");
    runningStatus.setEntityType("CRAWLER_JOB");
    jobExecution.setStatus(runningStatus);

    // Mock the service
    when(crawlerService.getCrawlerJobStatus(eq(jobId))).thenReturn(Optional.of(jobExecution));

    // Call the controller
    ResponseEntity<CrawlerJobExecution> response = controller.getCrawlerJobStatus(jobId);

    // Verify response
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(jobId, response.getBody().getId());
    assertEquals("RUNNING", response.getBody().getStatus().getName());
    assertEquals("CRAWLER_JOB", response.getBody().getStatus().getEntityType());
  }

  @Test
  void getCrawlerJobStatus_NonExistingJob_Returns404() {
    // Mock the service
    when(crawlerService.getCrawlerJobStatus(eq("non-existent-job"))).thenReturn(Optional.empty());

    // Call the controller
    ResponseEntity<CrawlerJobExecution> response =
        controller.getCrawlerJobStatus("non-existent-job");

    // Verify response
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void startScheduledCrawlerJobs_Admin_Returns200() {
    // Call the controller
    ResponseEntity<Map<String, Object>> response = controller.startScheduledCrawlerJobs();

    // Verify response
    assertEquals(HttpStatus.OK, response.getStatusCode());

    Map<String, Object> body = response.getBody();
    assertNotNull(body);
    assertTrue((Boolean) body.get("success"));
  }

  @Test
  void startScheduledCrawlerJobs_ServiceError_Returns500() {
    // Mock the service to throw exception
    doThrow(new RuntimeException("Service error")).when(crawlerService).startScheduledCrawlerJobs();

    // Call the controller
    ResponseEntity<Map<String, Object>> response = controller.startScheduledCrawlerJobs();

    // Verify response
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

    Map<String, Object> body = response.getBody();
    assertNotNull(body);
    assertFalse((Boolean) body.get("success"));
    assertNotNull(body.get("message"));
  }

  @Test
  void runCrawlersNow_PublicEndpoint_Returns200() {
    // Call the controller
    ResponseEntity<Map<String, Object>> response = controller.runCrawlersNow();

    // Verify response
    assertEquals(HttpStatus.OK, response.getStatusCode());

    Map<String, Object> body = response.getBody();
    assertNotNull(body);
    assertTrue((Boolean) body.get("success"));
  }

  @Test
  void runCrawlersNow_PublicEndpoint_ServiceError_Returns500() {
    // Mock the service to throw exception
    doThrow(new RuntimeException("Service error")).when(crawlerService).startScheduledCrawlerJobs();

    // Call the controller
    ResponseEntity<Map<String, Object>> response = controller.runCrawlersNow();

    // Verify response
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

    Map<String, Object> body = response.getBody();
    assertNotNull(body);
    assertFalse((Boolean) body.get("success"));
    assertNotNull(body.get("message"));
  }

  @Test
  void cancelCrawlerJob_ExistingJob_Admin_Returns200() {
    // Setup test data
    String jobId = UUID.randomUUID().toString();

    // Mock the service
    when(crawlerService.cancelCrawlerJob(eq(jobId))).thenReturn(true);

    // Call the controller
    ResponseEntity<Map<String, Object>> response = controller.cancelCrawlerJob(jobId);

    // Verify response
    assertEquals(HttpStatus.OK, response.getStatusCode());

    Map<String, Object> body = response.getBody();
    assertNotNull(body);
    assertTrue((Boolean) body.get("success"));
  }

  @Test
  void cancelCrawlerJob_NonExistingJob_Returns404() {
    // Mock the service
    when(crawlerService.cancelCrawlerJob(eq("non-existent-job"))).thenReturn(false);

    // Call the controller
    ResponseEntity<Map<String, Object>> response = controller.cancelCrawlerJob("non-existent-job");

    // Verify response
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

    Map<String, Object> body = response.getBody();
    assertNotNull(body);
    assertFalse((Boolean) body.get("success"));
  }
}

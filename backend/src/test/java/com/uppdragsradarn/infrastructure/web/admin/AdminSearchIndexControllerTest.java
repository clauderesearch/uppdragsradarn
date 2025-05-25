package com.uppdragsradarn.infrastructure.web.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.uppdragsradarn.application.service.AssignmentService;

@ExtendWith(MockitoExtension.class)
public class AdminSearchIndexControllerTest {

  @Mock private AssignmentService assignmentService;

  @InjectMocks private AdminSearchIndexController controller;

  @Test
  void reindexAll_Returns200() {
    // Test the controller directly
    ResponseEntity<Map<String, Object>> response = controller.reindexAll();

    // Verify response
    assertEquals(HttpStatus.OK, response.getStatusCode());

    Map<String, Object> body = response.getBody();
    assertTrue((Boolean) body.get("success"));
    assertEquals("Reindexing is no longer needed (using database search)", body.get("message"));
    assertEquals(0, body.get("count"));
  }

  // We can't test role-based security with direct controller testing
  // since we're bypassing Spring Security, but we can test the controller's logic
}

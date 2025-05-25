package com.uppdragsradarn.infrastructure.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.uppdragsradarn.test.SliceTestConfig;

@WebMvcTest(
    controllers = LogoutController.class,
    properties = {"spring.main.allow-bean-definition-overriding=true"})
@Import(SliceTestConfig.class)
public class LogoutControllerTest extends ControllerTestBase {

  @Autowired private MockMvc mockMvc;

  @Test
  @WithMockUser
  void logout_Authenticated_RedirectsToFrontend() throws Exception {
    mockMvc
        .perform(get("/logout"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login?logout"));
  }

  @Test
  @WithAnonymousUser
  void logout_Anonymous_RedirectsToFrontend() throws Exception {
    mockMvc
        .perform(get("/logout"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login?logout"));
  }
}

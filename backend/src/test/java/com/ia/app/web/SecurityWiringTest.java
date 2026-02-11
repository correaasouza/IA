package com.ia.app.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityWiringTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void publicEndpointsShouldBeAccessibleWithoutAuth() throws Exception {
    mockMvc.perform(get("/actuator/health"))
      .andExpect(status().isOk());

    // springdoc commonly redirects /swagger-ui.html to /swagger-ui/index.html
    mockMvc.perform(get("/swagger-ui.html"))
      .andExpect(status().is3xxRedirection());

    mockMvc.perform(get("/v3/api-docs"))
      .andExpect(status().isOk());
  }

  @Test
  void protectedEndpointShouldRequireAuth() throws Exception {
    mockMvc.perform(get("/api/me"))
      .andExpect(status().isUnauthorized());
  }
}


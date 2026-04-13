package com.baldwin.praecura.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AdminAccessIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  void adminCanAccessCoreModulesWithoutAuthOrServerErrors() throws Exception {
    List<String> urls = List.of(
        "/",
        "/reception",
        "/agenda",
        "/appointments",
        "/patients",
        "/clinical-workflow",
        "/inpatient",
        "/pharmacy",
        "/doctors",
        "/services",
        "/resources",
        "/sites",
        "/specialties",
        "/billing",
        "/billing/charges",
        "/billing/accounts-receivable",
        "/billing/cash",
        "/billing/fiscal",
        "/billing/fiscal/ecf",
        "/reports",
        "/reports/messages",
        "/reports/compliance",
        "/reports/audit",
        "/reports/executive",
        "/admin/users",
        "/admin/access",
        "/owner/branding"
    );

    for (String url : urls) {
      MvcResult result = mockMvc.perform(get(url)).andReturn();
      int status = result.getResponse().getStatus();
      assertThat(status)
          .withFailMessage("ADMIN no debería fallar en %s (status=%s).", url, status)
          .isNotEqualTo(401)
          .isNotEqualTo(403)
          .isLessThan(500);
    }
  }

  @Test
  @WithMockUser(username = "adminAuthority", authorities = {"ADMIN"})
  void adminAuthorityWithoutRolePrefixCanAccessAdminAndReception() throws Exception {
    List<String> urls = List.of(
        "/reception",
        "/billing",
        "/billing/accounts-receivable",
        "/insurance",
        "/pharmacy",
        "/inpatient",
        "/reports/executive",
        "/admin/access",
        "/admin/users",
        "/owner/branding"
    );

    for (String url : urls) {
      MvcResult result = mockMvc.perform(get(url)).andReturn();
      int status = result.getResponse().getStatus();
      assertThat(status)
          .withFailMessage("ADMIN (authority) no debería fallar en %s (status=%s).", url, status)
          .isNotEqualTo(401)
          .isNotEqualTo(403)
          .isLessThan(500);
    }
  }

  @Test
  @WithMockUser(username = "legacyAdmin", authorities = {"ROLE_ROLE_ADMIN"})
  void legacyDoubleRolePrefixAdminCanAccessAdminAndReception() throws Exception {
    List<String> urls = List.of(
        "/reception",
        "/billing",
        "/admin/access",
        "/admin/users",
        "/owner/branding"
    );

    for (String url : urls) {
      MvcResult result = mockMvc.perform(get(url)).andReturn();
      int status = result.getResponse().getStatus();
      assertThat(status)
          .withFailMessage("ROLE_ROLE_ADMIN no debería fallar en %s (status=%s).", url, status)
          .isNotEqualTo(401)
          .isNotEqualTo(403)
          .isLessThan(500);
    }
  }

  @Test
  @WithMockUser(username = "user", roles = {"USER"})
  void nonAdminIsDeniedFromAdminAndOwnerSections() throws Exception {
    List<String> restricted = List.of(
        "/admin/users",
        "/admin/access",
        "/owner/branding",
        "/billing",
        "/insurance",
        "/pharmacy",
        "/inpatient",
        "/reports/executive"
    );

    for (String url : restricted) {
      MvcResult result = mockMvc.perform(get(url)).andReturn();
      int status = result.getResponse().getStatus();
      assertThat(status)
          .withFailMessage("USER debe estar restringido en %s (status=%s).", url, status)
          .isIn(302, 403);
    }
  }
}

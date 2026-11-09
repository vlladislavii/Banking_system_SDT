package org.example.banking_system_sdt.event;

import com.jayway.jsonpath.JsonPath;
import org.example.banking_system_sdt.account.AccountService;
import org.example.banking_system_sdt.account.AccountType;
import org.example.banking_system_sdt.notification.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ObserverIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AuditLog auditLog;

    @Autowired
    private NotificationRepository notificationRepository;

    private Integer registerCustomer(String name, String email) throws Exception {
        String json = String.format("""
                {
                  "fullName": "%s",
                  "email": "%s",
                  "phone": "+987654321",
                  "address": "Street 10"
                }
                """, name, email);
        MvcResult result = mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    @Test
    void accountOpened_notifiesCustomerAndRecordsAuditLog() throws Exception {
        Integer customerId = registerCustomer("Grace", "grace@bank.com");

        int initialAuditSize = auditLog.getEntries().size();

        // Open an account
        accountService.openAccount(customerId.longValue(), customerId.longValue(), AccountType.CHECKING, new BigDecimal("100.00"));

        // 1. Verify AuditLog recorded ACCOUNT_OPENED
        assertTrue(auditLog.getEntries().size() > initialAuditSize);
        boolean foundInAudit = auditLog.getEntries().stream()
                .anyMatch(e -> e.getType() == EventType.ACCOUNT_OPENED && e.getRecipientIds().contains(customerId.longValue()));
        assertTrue(foundInAudit, "AuditLog should contain ACCOUNT_OPENED event");

        // 2. Verify Customer received notification via REST endpoint
        MvcResult notifResult = mockMvc.perform(get("/api/notifications")
                        .header("X-User-Id", customerId)
                        .param("customerId", customerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type").value("ACCOUNT_OPENED"))
                .andExpect(jsonPath("$[0].read").value(false))
                .andReturn();

        Integer notifId = JsonPath.read(notifResult.getResponse().getContentAsString(), "$[0].id");

        // 3. Mark notification as read
        mockMvc.perform(put("/api/notifications/" + notifId + "/read")
                        .header("X-User-Id", customerId))
                .andExpect(status().isNoContent());

        assertTrue(notificationRepository.findById(notifId.longValue()).orElseThrow().isRead());

        // 4. Admin can inspect audit log via REST endpoint
        mockMvc.perform(get("/api/audit-log")
                        .header("X-User-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // 5. Non-admin is forbidden from inspecting audit log
        mockMvc.perform(get("/api/audit-log")
                        .header("X-User-Id", customerId))
                .andExpect(status().isForbidden());
    }
}

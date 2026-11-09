package org.example.banking_system_sdt;

import com.jayway.jsonpath.JsonPath;
import org.example.banking_system_sdt.customer.User;
import org.example.banking_system_sdt.customer.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TransferEndToEndIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private Long getAdminId() {
        return userRepository.findByEmail("admin@bank.com")
                .map(User::getId)
                .orElse(1L);
    }

    private Long registerCustomer(String name, String email) throws Exception {
        String json = String.format("""
                {
                  "fullName": "%s",
                  "email": "%s",
                  "phone": "+380501234567",
                  "address": "Main Street 42"
                }
                """, name, email);

        MvcResult result = mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn();

        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private Long openAccount(Long ownerId, String type, double deposit) throws Exception {
        String json = String.format("""
                {
                  "ownerId": %d,
                  "type": "%s",
                  "initialDeposit": %.2f
                }
                """, ownerId, type, deposit);

        MvcResult result = mockMvc.perform(post("/api/accounts")
                        .header("X-User-Id", ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn();

        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    @Test
    @DisplayName("Complete end-to-end flow: customers -> accounts -> transfer -> rejection -> fee waiver -> second transfer -> notifications & audit")
    void fullBankingLifecycle_demonstratesAllFourPatterns() throws Exception {
        Long adminId = getAdminId();

        // 1. Register customers
        Long aliceId = registerCustomer("Alice Smith", "alice.e2e@bank.com");
        Long bobId = registerCustomer("Bob Jones", "bob.e2e@bank.com");

        // 2. Open Accounts (Factory Method & Strategy & Observer)
        // Alice opens Checking Account with 500.00 EUR (Fixed fee strategy: 1.00 EUR)
        Long aliceCheckingId = openAccount(aliceId, "CHECKING", 500.00);

        // Bob opens Savings Account with 200.00 EUR (Percentage fee strategy: 1.5%)
        Long bobSavingsId = openAccount(bobId, "SAVINGS", 200.00);

        // Verify initial balances
        mockMvc.perform(get("/api/accounts/" + aliceCheckingId + "/balance")
                        .header("X-User-Id", aliceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(500.00));

        mockMvc.perform(get("/api/accounts/" + bobSavingsId + "/balance")
                        .header("X-User-Id", bobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(200.00));

        // 3. Successful Transfer: 100.00 EUR Alice -> Bob (Chain of Responsibility + Strategy + Observer)
        // Checking fixed fee is 1.00 EUR -> Alice pays 101.00 EUR, Bob receives 100.00 EUR
        String transfer1 = String.format("""
                {
                  "sourceAccountId": %d,
                  "targetAccountId": %d,
                  "amount": 100.00
                }
                """, aliceCheckingId, bobSavingsId);

        mockMvc.perform(post("/api/transactions/transfer")
                        .header("X-User-Id", aliceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transfer1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESSFUL"))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.fee").value(1.00));

        // Check balances after transfer 1
        mockMvc.perform(get("/api/accounts/" + aliceCheckingId + "/balance")
                        .header("X-User-Id", aliceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(399.00));

        mockMvc.perform(get("/api/accounts/" + bobSavingsId + "/balance")
                        .header("X-User-Id", bobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(300.00));

        // 4. Rejected Transfer: 1000.00 EUR (Chain of Responsibility: SufficientFundsHandler rejects)
        String transferTooLarge = String.format("""
                {
                  "sourceAccountId": %d,
                  "targetAccountId": %d,
                  "amount": 1000.00
                }
                """, aliceCheckingId, bobSavingsId);

        mockMvc.perform(post("/api/transactions/transfer")
                        .header("X-User-Id", aliceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferTooLarge))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason", containsString("Insufficient funds")));

        // Balances should be unchanged
        mockMvc.perform(get("/api/accounts/" + aliceCheckingId + "/balance")
                        .header("X-User-Id", aliceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(399.00));

        // 5. Admin Waives Fees for Alice's Checking Account (Strategy swap at runtime)
        mockMvc.perform(post("/api/accounts/" + aliceCheckingId + "/waive-fees")
                        .header("X-User-Id", adminId))
                .andExpect(status().isNoContent());

        // Verify account now has NoFeeStrategy
        mockMvc.perform(get("/api/accounts/" + aliceCheckingId)
                        .header("X-User-Id", aliceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feeStrategy").value("NoFeeStrategy"));

        // 6. Transfer 100.00 EUR again with Waived Fee (0.00 EUR fee)
        mockMvc.perform(post("/api/transactions/transfer")
                        .header("X-User-Id", aliceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transfer1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESSFUL"))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.fee").value(0.00));

        // Final balances: Alice = 399 - 100 - 0 = 299.00; Bob = 300 + 100 = 400.00
        mockMvc.perform(get("/api/accounts/" + aliceCheckingId + "/balance")
                        .header("X-User-Id", aliceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(299.00));

        mockMvc.perform(get("/api/accounts/" + bobSavingsId + "/balance")
                        .header("X-User-Id", bobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(400.00));

        // 7. Verify Notifications (Observer pattern)
        MvcResult aliceNotifResult = mockMvc.perform(get("/api/notifications")
                        .header("X-User-Id", aliceId)
                        .param("customerId", aliceId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(4))))
                .andReturn();

        List<Integer> notifIds = JsonPath.read(aliceNotifResult.getResponse().getContentAsString(), "$[*].id");
        Long firstNotifId = ((Number) notifIds.getFirst()).longValue();

        // Mark a notification as read
        mockMvc.perform(put("/api/notifications/" + firstNotifId + "/read")
                        .header("X-User-Id", aliceId))
                .andExpect(status().isNoContent());

        // Verify Bob's notifications
        mockMvc.perform(get("/api/notifications")
                        .header("X-User-Id", bobId)
                        .param("customerId", bobId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(3))));

        // 8. Verify Audit Log (Admin only - Observer pattern)
        mockMvc.perform(get("/api/audit-log")
                        .header("X-User-Id", adminId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(5))));

        // Non-admin cannot view audit log (403 Forbidden)
        mockMvc.perform(get("/api/audit-log")
                        .header("X-User-Id", aliceId))
                .andExpect(status().isForbidden());

        // 9. Verify Transaction History for Alice's Checking Account
        mockMvc.perform(get("/api/transactions/history/" + aliceCheckingId)
                        .header("X-User-Id", aliceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].status", is(oneOf("SUCCESSFUL", "REJECTED"))));
    }
}

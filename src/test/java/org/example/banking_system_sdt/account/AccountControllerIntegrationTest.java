package org.example.banking_system_sdt.account;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private Integer registerCustomer(String name, String email) throws Exception {
        String json = String.format("""
                {
                  "fullName": "%s",
                  "email": "%s",
                  "phone": "+12345",
                  "address": "Test Ave"
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
    void openCheckingAccount_success() throws Exception {
        Integer customerId = registerCustomer("Charlie", "charlie@bank.com");

        String openJson = String.format("""
                {
                  "ownerId": %d,
                  "type": "CHECKING",
                  "initialDeposit": 50.00
                }
                """, customerId);

        mockMvc.perform(post("/api/accounts")
                        .header("X-User-Id", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(openJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.type").value("CHECKING"))
                .andExpect(jsonPath("$.balance").value(50.00))
                .andExpect(jsonPath("$.overdraftLimit").value(500.00))
                .andExpect(jsonPath("$.availableToSpend").value(550.00));
    }

    @Test
    void openSavingsAccount_depositValidation() throws Exception {
        Integer customerId = registerCustomer("David", "david@bank.com");

        // Deposit 50 EUR (minimum is 100 EUR) -> 400 Bad Request
        String openTooLow = String.format("""
                {
                  "ownerId": %d,
                  "type": "SAVINGS",
                  "initialDeposit": 50.00
                }
                """, customerId);

        mockMvc.perform(post("/api/accounts")
                        .header("X-User-Id", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(openTooLow))
                .andExpect(status().isBadRequest());

        // Deposit 150 EUR -> 201 Created
        String openValid = String.format("""
                {
                  "ownerId": %d,
                  "type": "SAVINGS",
                  "initialDeposit": 150.00
                }
                """, customerId);

        mockMvc.perform(post("/api/accounts")
                        .header("X-User-Id", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(openValid))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("SAVINGS"))
                .andExpect(jsonPath("$.balance").value(150.00))
                .andExpect(jsonPath("$.interestRate").value(2.50));
    }

    @Test
    void getAccountAndBalance_success() throws Exception {
        Integer customerId = registerCustomer("Emma", "emma@bank.com");

        String openJson = String.format("""
                {
                  "ownerId": %d,
                  "type": "CHECKING",
                  "initialDeposit": 25.00
                }
                """, customerId);

        MvcResult openResult = mockMvc.perform(post("/api/accounts")
                        .header("X-User-Id", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(openJson))
                .andExpect(status().isCreated())
                .andReturn();

        Integer accountId = JsonPath.read(openResult.getResponse().getContentAsString(), "$.id");

        // Get details
        mockMvc.perform(get("/api/accounts/" + accountId)
                        .header("X-User-Id", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(25.00));

        // Get balance endpoint
        mockMvc.perform(get("/api/accounts/" + accountId + "/balance")
                        .header("X-User-Id", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(25.00))
                .andExpect(jsonPath("$.currency").value("EUR"));
    }

    @Test
    void waiveFees_adminOnly() throws Exception {
        Integer customerId = registerCustomer("Frank", "frank@bank.com");

        String openJson = String.format("""
                {
                  "ownerId": %d,
                  "type": "CHECKING",
                  "initialDeposit": 10.00
                }
                """, customerId);

        MvcResult openResult = mockMvc.perform(post("/api/accounts")
                        .header("X-User-Id", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(openJson))
                .andExpect(status().isCreated())
                .andReturn();

        Integer accountId = JsonPath.read(openResult.getResponse().getContentAsString(), "$.id");

        // Customer cannot waive fees (403 Forbidden)
        mockMvc.perform(post("/api/accounts/" + accountId + "/waive-fees")
                        .header("X-User-Id", customerId))
                .andExpect(status().isForbidden());

        // Admin (ID 1) waives fees (204 No Content)
        mockMvc.perform(post("/api/accounts/" + accountId + "/waive-fees")
                        .header("X-User-Id", 1))
                .andExpect(status().isNoContent());

        // Fee strategy is now NoFeeStrategy
        mockMvc.perform(get("/api/accounts/" + accountId)
                        .header("X-User-Id", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feeStrategy").value("NoFeeStrategy"));
    }
}

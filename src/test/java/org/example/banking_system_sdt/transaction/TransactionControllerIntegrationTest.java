package org.example.banking_system_sdt.transaction;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private Integer registerCustomer(String name, String email) throws Exception {
        String json = String.format("""
                {
                  "fullName": "%s",
                  "email": "%s",
                  "phone": "+11223344",
                  "address": "Street X"
                }
                """, name, email);
        MvcResult result = mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private Integer openAccount(Integer ownerId, String type, double deposit) throws Exception {
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
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    @Test
    void transferFlow_successfulAndRejected() throws Exception {
        Integer senderId = registerCustomer("Sender", "sender@bank.com");
        Integer receiverId = registerCustomer("Receiver", "receiver@bank.com");

        Integer sourceAccountId = openAccount(senderId, "CHECKING", 500.00);
        Integer targetAccountId = openAccount(receiverId, "SAVINGS", 150.00);

        // 1. Successful transfer of 100 EUR (checking fixed fee is 1.00 EUR -> total 101.00)
        String transferJson = String.format("""
                {
                  "sourceAccountId": %d,
                  "targetAccountId": %d,
                  "amount": 100.00
                }
                """, sourceAccountId, targetAccountId);

        mockMvc.perform(post("/api/transactions/transfer")
                        .header("X-User-Id", senderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESSFUL"))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.fee").value(1.00));

        // Source balance should now be 500 - 101 = 399.00
        mockMvc.perform(get("/api/accounts/" + sourceAccountId + "/balance")
                        .header("X-User-Id", senderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(399.00));

        // Target balance should now be 150 + 100 = 250.00
        mockMvc.perform(get("/api/accounts/" + targetAccountId + "/balance")
                        .header("X-User-Id", receiverId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(250.00));

        // 2. Rejected transfer due to insufficient funds (transfer 50000 EUR)
        String tooLargeTransfer = String.format("""
                {
                  "sourceAccountId": %d,
                  "targetAccountId": %d,
                  "amount": 50000.00
                }
                """, sourceAccountId, targetAccountId);

        mockMvc.perform(post("/api/transactions/transfer")
                        .header("X-User-Id", senderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tooLargeTransfer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").isNotEmpty());

        // 3. Query account transaction history
        mockMvc.perform(get("/api/transactions/history/" + sourceAccountId)
                        .header("X-User-Id", senderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2))); // 1 successful, 1 rejected

        // 4. Admin search transactions
        mockMvc.perform(post("/api/transactions/search")
                        .header("X-User-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // 5. Non-admin search is forbidden
        mockMvc.perform(post("/api/transactions/search")
                        .header("X-User-Id", senderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}

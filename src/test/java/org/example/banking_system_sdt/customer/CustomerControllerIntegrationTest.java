package org.example.banking_system_sdt.customer;

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
class CustomerControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registerAndGetProfile_success() throws Exception {
        String json = """
                {
                  "fullName": "Alice Smith",
                  "email": "alice@example.com",
                  "phone": "+40700000001",
                  "address": "Strada Florilor 1"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.fullName").value("Alice Smith"))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andReturn();

        Integer customerId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/customers/" + customerId)
                        .header("X-User-Id", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Alice Smith"));
    }

    @Test
    void getProfile_forbiddenWhenNotOwnerOrAdmin() throws Exception {
        String jsonAlice = """
                {
                  "fullName": "Alice Unique",
                  "email": "alice.unique@example.com",
                  "phone": "111",
                  "address": "Addr 1"
                }
                """;
        MvcResult aliceResult = mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonAlice))
                .andExpect(status().isCreated())
                .andReturn();
        Integer aliceId = JsonPath.read(aliceResult.getResponse().getContentAsString(), "$.id");

        String jsonBob = """
                {
                  "fullName": "Bob Unique",
                  "email": "bob.unique@example.com",
                  "phone": "222",
                  "address": "Addr 2"
                }
                """;
        MvcResult bobResult = mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBob))
                .andExpect(status().isCreated())
                .andReturn();
        Integer bobId = JsonPath.read(bobResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/customers/" + aliceId)
                        .header("X-User-Id", bobId))
                .andExpect(status().isForbidden());
    }

    @Test
    void listCustomers_allowedForAdmin() throws Exception {
        mockMvc.perform(get("/api/customers")
                        .header("X-User-Id", 1))
                .andExpect(status().isOk());
    }
}

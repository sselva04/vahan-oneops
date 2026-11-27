package com.vahan.oneops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Release A: customers, job dates on outsourced stages, and supplier rejections with debit. */
@SpringBootTest
@AutoConfigureMockMvc
class ReleaseAFlowTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;

    private JsonNode send(MockHttpServletRequestBuilder request, String token, Object body, int expectedStatus)
            throws Exception {
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        }
        MvcResult result = mvc.perform(request).andExpect(status().is(expectedStatus)).andReturn();
        String text = result.getResponse().getContentAsString();
        return text.isBlank() ? json.nullNode() : json.readTree(text);
    }

    private String adminToken() throws Exception {
        return send(post("/api/auth/login"), null, Map.of("username", "admin", "password", "Admin#12345"), 200)
                .get("token").asText();
    }

    private long customerId(String admin, String name) throws Exception {
        for (JsonNode c : send(get("/api/admin/customers?includeInactive=true"), admin, null, 200)) {
            if (name.equals(c.get("name").asText())) {
                return c.get("id").asLong();
            }
        }
        throw new AssertionError("Customer not found: " + name);
    }

    private long newSupplier(String admin, String name) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("processes", List.of());
        return send(post("/api/admin/suppliers"), admin, body, 201).get("id").asLong();
    }

    /** Project with one part (given quantity) and one stage; returns {projectId, stageId}. */
    private long[] projectWithStage(String admin, String projectNo, String partNo, int qty) throws Exception {
        long projectId = send(post("/api/admin/projects"), admin,
                Map.of("drawingNo", projectNo, "name", "Release A test"), 201).get("id").asLong();
        long partId = send(post("/api/admin/projects/" + projectId + "/parts"), admin,
                Map.of("drawingNo", partNo, "name", "Slider", "quantity", qty), 201)
                .get("parts").get(0).get("id").asLong();
        long stageId = send(post("/api/admin/parts/" + partId + "/stages"), admin,
                Map.of("stages", List.of(Map.of("customName", "Wire Cut"))), 201)
                .get("parts").get(0).get("stages").get(0).get("id").asLong();
        return new long[]{projectId, stageId};
    }

    private Map<String, Object> outsourcing(Long supplierId, Integer qty, String rate, String sentOn, String receivedOn) {
        Map<String, Object> o = new HashMap<>();
        o.put("supplierId", supplierId);
        o.put("jobQty", qty);
        o.put("ratePerUnit", rate == null ? null : new BigDecimal(rate));
        o.put("preInspectionDone", false);
        o.put("finalInspectionDone", false);
        o.put("remarks", null);
        o.put("sentOn", sentOn);
        o.put("receivedOn", receivedOn);
        return o;
    }

    private Map<String, Object> rejection(Integer qty, String partCost, String machining, String debit, String bill) {
        Map<String, Object> r = new HashMap<>();
        r.put("rejectedQty", qty);
        r.put("partCost", partCost == null ? null : new BigDecimal(partCost));
        r.put("machiningCost", machining == null ? null : new BigDecimal(machining));
        r.put("debitAmount", debit == null ? null : new BigDecimal(debit));
        r.put("billNo", bill);
        r.put("rejectedOn", "2026-11-20");
        r.put("remarks", "  Offset wirecut done  ");
        return r;
    }

    private static void assertMoney(String expected, JsonNode actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual.decimalValue()), "expected " + expected + " but was " + actual);
    }

    // ---------- customers ----------

    @Test
    void customersAreListedAndEditable() throws Exception {
        String admin = adminToken();
        assertTrue(customerId(admin, "DENO") > 0);
        assertTrue(customerId(admin, "GI Auto") > 0);
        send(get("/api/admin/customers"), null, null, 401);

        long id = send(post("/api/admin/customers"), admin, Map.of("name", "  Test   Customer "), 201).get("id").asLong();
        send(post("/api/admin/customers"), admin, Map.of("name", "test customer"), 409);
        send(post("/api/admin/customers"), admin, Map.of("name", "  "), 400);
        assertEquals("Test Customer Ltd",
                send(put("/api/admin/customers/" + id), admin, Map.of("name", "Test Customer Ltd"), 200).get("name").asText());
        assertFalse(send(patch("/api/admin/customers/" + id), admin, Map.of("active", false), 200).get("active").asBoolean());
    }

    @Test
    void aProjectHasOneCustomerAndOldProjectsWithoutOneStillWork() throws Exception {
        String admin = adminToken();
        long deno = customerId(admin, "DENO");
        long wendt = customerId(admin, "WENDT");

        JsonNode withCustomer = send(post("/api/admin/projects"), admin,
                Map.of("drawingNo", "VDE-0626-71-00-00", "name", "With customer", "customerId", deno), 201);
        long projectId = withCustomer.get("id").asLong();
        assertEquals("DENO", withCustomer.get("customerName").asText());

        // A project without a customer (like those made before this release) still works.
        JsonNode without = send(post("/api/admin/projects"), admin,
                Map.of("drawingNo", "VDE-0626-72-00-00", "name", "No customer"), 201);
        assertTrue(without.get("customerName").isNull());

        // Change it, then see it in the list.
        assertEquals("WENDT", send(put("/api/admin/projects/" + projectId + "/customer"), admin,
                Map.of("customerId", wendt), 200).get("customerName").asText());
        boolean listed = false;
        for (JsonNode p : send(get("/api/admin/projects"), admin, null, 200)) {
            if (p.get("id").asLong() == projectId) {
                assertEquals("WENDT", p.get("customerName").asText());
                listed = true;
            }
        }
        assertTrue(listed);

        // Unknown customer, missing customer, deactivated customer for a new choice.
        send(put("/api/admin/projects/" + projectId + "/customer"), admin, Map.of("customerId", 999999), 400);
        send(put("/api/admin/projects/" + projectId + "/customer"), admin, new HashMap<String, Object>(), 400);
        long retired = send(post("/api/admin/customers"), admin, Map.of("name", "Test Retired Customer"), 201).get("id").asLong();
        send(patch("/api/admin/customers/" + retired), admin, Map.of("active", false), 200);
        send(post("/api/admin/projects"), admin,
                Map.of("drawingNo", "VDE-0626-73-00-00", "name", "Bad", "customerId", retired), 400);
    }

    // ---------- job dates ----------

    @Test
    void sentAndReceivedDatesGiveTheTurnaround() throws Exception {
        String admin = adminToken();
        long supplier = newSupplier(admin, "Release A Supplier Dates");
        long[] ids = projectWithStage(admin, "VDE-0626-74-00-00", "VDE-0626-74-00-01", 2);
        long stage = ids[1];

        JsonNode s = send(put("/api/admin/stages/" + stage + "/outsourcing"), admin,
                outsourcing(supplier, 2, "50", "2026-11-10", "2026-11-14"), 200).get("parts").get(0).get("stages").get(0);
        assertEquals("2026-11-10", s.get("sentOn").asText());
        assertEquals("2026-11-14", s.get("receivedOn").asText());
        assertEquals(4, s.get("turnaroundDays").asInt());

        // Only sent so far: no turnaround yet.
        JsonNode open = send(put("/api/admin/stages/" + stage + "/outsourcing"), admin,
                outsourcing(supplier, 2, "50", "2026-11-10", null), 200).get("parts").get(0).get("stages").get(0);
        assertTrue(open.get("turnaroundDays").isNull());

        // Back before it went out, or dates with no supplier: refused.
        send(put("/api/admin/stages/" + stage + "/outsourcing"), admin,
                outsourcing(supplier, 2, "50", "2026-11-10", "2026-11-09"), 400);
        send(put("/api/admin/stages/" + stage + "/outsourcing"), admin,
                outsourcing(null, null, null, "2026-11-10", null), 400);

        // The supplier page average counts jobs sent and received.
        send(put("/api/admin/stages/" + stage + "/outsourcing"), admin,
                outsourcing(supplier, 2, "50", "2026-11-10", "2026-11-14"), 200);
        JsonNode sup = send(get("/api/admin/suppliers/" + supplier), admin, null, 200);
        assertEquals(1, sup.get("jobsReceived").asInt());
        assertEquals(4.0, sup.get("avgTurnaroundDays").asDouble(), 0.001);
    }

    // ---------- rejections ----------

    @Test
    void aRejectionCalculatesTheDebitLikeTheSheet() throws Exception {
        String admin = adminToken();
        long supplier = newSupplier(admin, "Release A Supplier Debit");
        long[] ids = projectWithStage(admin, "VDE-0626-75-00-00", "VDE-0626-75-00-01", 5);
        long projectId = ids[0];
        long stage = ids[1];

        // Not given to a supplier yet: refused.
        send(post("/api/admin/stages/" + stage + "/rejections"), admin, rejection(1, "100", "0", null, null), 400);

        send(put("/api/admin/stages/" + stage + "/outsourcing"), admin, outsourcing(supplier, 5, "22.40", null, null), 200);

        // First row of Vahan's sheet: 5 pieces x 1500 - 112 = 7388.
        JsonNode detail = send(post("/api/admin/stages/" + stage + "/rejections"), admin,
                rejection(5, "1500", "112", null, " 345 "), 201);
        JsonNode st = detail.get("parts").get(0).get("stages").get(0);
        assertEquals(5, st.get("rejectedQty").asInt());
        JsonNode r = st.get("rejections").get(0);
        assertMoney("7388", r.get("debitAmount"));
        assertMoney("7388", r.get("calculatedDebit"));
        assertFalse(r.get("debitManual").asBoolean());
        assertEquals("345", r.get("billNo").asText());
        assertEquals("Offset wirecut done", r.get("remarks").asText());
        long rejectionId = r.get("id").asLong();

        // The debit never goes below zero when machining cost is more than the part value.
        // (all 5 pieces are already used, so lower the first one to 3 and add 2 more)
        send(put("/api/admin/rejections/" + rejectionId), admin, rejection(3, "10", "500", null, "345"), 200);
        JsonNode zero = send(get("/api/admin/projects/" + projectId), admin, null, 200)
                .get("parts").get(0).get("stages").get(0).get("rejections").get(0);
        assertMoney("0", zero.get("debitAmount"));

        // Typed amount overrides the calculation; the calculated figure is still shown.
        JsonNode manual = send(put("/api/admin/rejections/" + rejectionId), admin,
                rejection(3, "1500", "112", "4000", "345"), 200)
                .get("parts").get(0).get("stages").get(0).get("rejections").get(0);
        assertMoney("4000", manual.get("debitAmount"));
        assertTrue(manual.get("debitManual").asBoolean());
        assertMoney("4388", manual.get("calculatedDebit"));

        // Cannot reject more than was sent: 3 already, 3 more would be 6 of 5.
        send(post("/api/admin/stages/" + stage + "/rejections"), admin, rejection(3, "10", "0", null, null), 400);
        send(post("/api/admin/stages/" + stage + "/rejections"), admin, rejection(0, "10", "0", null, null), 400);
        send(post("/api/admin/stages/" + stage + "/rejections"), admin, rejection(1, "-1", "0", null, null), 400);
        send(post("/api/admin/stages/999999/rejections"), admin, rejection(1, "10", "0", null, null), 404);
        send(post("/api/admin/stages/" + stage + "/rejections"), admin, rejection(2, "300", null, null, "346"), 201);

        // Supplier page totals and the Rejections list.
        JsonNode sup = send(get("/api/admin/suppliers/" + supplier), admin, null, 200);
        assertMoney("4600", sup.get("debitTotal"));   // 4000 + (2 x 300 - 0)
        assertEquals(5, sup.get("rejectedQty").asInt());
        JsonNode rows = send(get("/api/admin/rejections?supplierId=" + supplier), admin, null, 200);
        assertEquals(2, rows.size());
        assertEquals("Release A Supplier Debit", rows.get(0).get("supplierName").asText());
        assertEquals("VDE-0626-75-00-01", rows.get(0).get("partDrawingNo").asText());
        assertEquals("Wire Cut", rows.get(0).get("stageName").asText());

        // A stage with rejections cannot be removed; once they are deleted it can.
        send(delete("/api/admin/stages/" + stage), admin, null, 409);
        send(delete("/api/admin/rejections/" + rejectionId), admin, null, 200);
        long second = rows.get(0).get("id").asLong();
        send(delete("/api/admin/rejections/" + second), admin, null, 200);
        send(delete("/api/admin/rejections/" + second), admin, null, 404);
        send(delete("/api/admin/stages/" + stage), admin, null, 200);
        assertEquals(0, send(get("/api/admin/rejections?supplierId=" + supplier), admin, null, 200).size());
    }
}

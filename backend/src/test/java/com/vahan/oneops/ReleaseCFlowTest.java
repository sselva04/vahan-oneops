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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Release C: raw material purchases and the monthly turnover / profit report. */
@SpringBootTest
@AutoConfigureMockMvc
class ReleaseCFlowTest {

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

    private long newSupplier(String admin, String name) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("processes", List.of());
        return send(post("/api/admin/suppliers"), admin, body, 201).get("id").asLong();
    }

    /** Returns {projectId, partId, stageId} (one part with one stage). */
    private long[] projectWithStage(String admin, String projectNo, String partNo) throws Exception {
        long projectId = send(post("/api/admin/projects"), admin,
                Map.of("drawingNo", projectNo, "name", "Release C test"), 201).get("id").asLong();
        long partId = send(post("/api/admin/projects/" + projectId + "/parts"), admin,
                Map.of("drawingNo", partNo, "name", "Plate", "quantity", 5), 201).get("parts").get(0).get("id").asLong();
        long stageId = send(post("/api/admin/parts/" + partId + "/stages"), admin,
                Map.of("stages", List.of(Map.of("customName", "Wire Cut"))), 201)
                .get("parts").get(0).get("stages").get(0).get("id").asLong();
        return new long[]{projectId, partId, stageId};
    }

    private Map<String, Object> purchase(long partId, long supplierId, String weight, String rate, String inDate, String outDate) {
        Map<String, Object> p = new HashMap<>();
        p.put("partId", partId);
        p.put("supplierId", supplierId);
        p.put("weightKg", weight == null ? null : new BigDecimal(weight));
        p.put("ratePerKg", rate == null ? null : new BigDecimal(rate));
        p.put("inDate", inDate);
        p.put("outDate", outDate);
        p.put("remarks", "  EN8 round bar  ");
        return p;
    }

    private static void assertMoney(String expected, JsonNode actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual.decimalValue()),
                "expected " + expected + " but was " + actual);
    }

    // ---------- raw material purchases ----------

    @Test
    void purchaseLog() throws Exception {
        String admin = adminToken();
        long supplier = newSupplier(admin, "Naveen Industrial C");
        long[] ids = projectWithStage(admin, "VDE-0626-91-00-00", "VDE-0626-91-00-01");
        send(get("/api/admin/rm-purchases"), null, null, 401);

        // sheet row 4: 4.4 kg at 450 = 1980
        JsonNode p = send(post("/api/admin/rm-purchases"), admin,
                purchase(ids[1], supplier, "4.4", "450", "2026-11-05", "2026-11-06"), 201);
        assertMoney("1980", p.get("totalCost"));
        assertMoney("4.4", p.get("weightKg"));
        assertEquals("Naveen Industrial C", p.get("supplierName").asText());
        assertEquals("VDE-0626-91-00-01", p.get("partDrawingNo").asText());
        assertEquals("VDE-0626-91-00-00", p.get("projectDrawingNo").asText());
        assertEquals("EN8 round bar", p.get("remarks").asText());
        long id = p.get("id").asLong();

        // validation
        send(post("/api/admin/rm-purchases"), admin, purchase(ids[1], supplier, "0", "450", "2026-11-05", null), 400);
        send(post("/api/admin/rm-purchases"), admin, purchase(ids[1], supplier, "4", "-1", "2026-11-05", null), 400);
        send(post("/api/admin/rm-purchases"), admin, purchase(ids[1], supplier, "4", "450", "2026-11-05", "2026-11-01"), 400);
        send(post("/api/admin/rm-purchases"), admin, purchase(999999, supplier, "4", "450", "2026-11-05", null), 400);
        send(post("/api/admin/rm-purchases"), admin, purchase(ids[1], 999999, "4", "450", "2026-11-05", null), 400);
        send(post("/api/admin/rm-purchases"), admin, purchase(ids[1], supplier, null, "450", "2026-11-05", null), 400);

        // a second purchase, then list with filters and totals
        send(post("/api/admin/rm-purchases"), admin, purchase(ids[1], supplier, "10", "100.50", "2026-11-20", null), 201);
        JsonNode list = send(get("/api/admin/rm-purchases?projectId=" + ids[0] + "&month=2026-11"), admin, null, 200);
        assertEquals(2, list.get("purchases").size());
        assertMoney("14.4", list.get("totals").get("weightKg"));
        assertMoney("2985", list.get("totals").get("totalCost"));   // 1980 + 1005.00
        assertEquals(0, send(get("/api/admin/rm-purchases?projectId=" + ids[0] + "&month=2026-10"), admin, null, 200)
                .get("purchases").size());
        assertEquals(2, send(get("/api/admin/rm-purchases?supplierId=" + supplier), admin, null, 200).get("purchases").size());
        send(get("/api/admin/rm-purchases?month=Nov"), admin, null, 400);

        // edit recalculates the total; delete
        JsonNode edited = send(put("/api/admin/rm-purchases/" + id), admin,
                purchase(ids[1], supplier, "5", "450", "2026-11-05", null), 200);
        assertMoney("2250", edited.get("totalCost"));
        send(delete("/api/admin/rm-purchases/" + id), admin, null, 204);
        send(delete("/api/admin/rm-purchases/" + id), admin, null, 404);
        send(put("/api/admin/rm-purchases/" + id), admin, purchase(ids[1], supplier, "5", "450", "2026-11-05", null), 404);
    }

    // ---------- monthly report ----------

    @Test
    void monthlyReportPullsEverythingTogether() throws Exception {
        String admin = adminToken();
        long supplier = newSupplier(admin, "Report Supplier");
        long[] ids = projectWithStage(admin, "VDE-0626-92-00-00", "VDE-0626-92-00-01");
        send(get("/api/admin/reports/monthly/2031-03"), null, null, 401);

        // before anything exists the month is all zero
        JsonNode empty = send(get("/api/admin/reports/monthly/2031-03"), admin, null, 200);
        assertEquals("2031-03", empty.get("month").asText());
        assertMoney("0", empty.get("turnover"));
        assertMoney("0", empty.get("profit"));

        // machine hours: 3 h at 450 = 1350, tax 243
        Map<String, Object> hours = new HashMap<>();
        hours.put("entryDate", "2031-03-10");
        hours.put("projectId", ids[0]);
        hours.put("childPartId", ids[1]);
        hours.put("quantity", 1);
        hours.put("hardened", false);
        hours.put("designMinutes", 20);
        hours.put("settingMinutes", 40);
        hours.put("runMinutes", 120);
        send(post("/api/admin/machine-hours"), admin, hours, 201);

        // outsourced work sent in March: 2 x 100 = 200
        Map<String, Object> outsourcing = new HashMap<>();
        outsourcing.put("supplierId", supplier);
        outsourcing.put("jobQty", 2);
        outsourcing.put("ratePerUnit", new BigDecimal("100"));
        outsourcing.put("preInspectionDone", false);
        outsourcing.put("finalInspectionDone", false);
        outsourcing.put("sentOn", "2031-03-05");
        send(put("/api/admin/stages/" + ids[2] + "/outsourcing"), admin, outsourcing, 200);

        // a rejection in March: 1 x 1000 - 100 = 900
        Map<String, Object> rejection = new HashMap<>();
        rejection.put("rejectedQty", 1);
        rejection.put("partCost", new BigDecimal("1000"));
        rejection.put("machiningCost", new BigDecimal("100"));
        rejection.put("rejectedOn", "2031-03-15");
        send(post("/api/admin/stages/" + ids[2] + "/rejections"), admin, rejection, 201);

        // tools: bought for 300, resharpened for 50 in March
        Map<String, Object> tool = new HashMap<>();
        tool.put("toolType", "Cutter");
        tool.put("description", "Endmil");
        tool.put("cost", new BigDecimal("300"));
        tool.put("purchasedOn", "2031-03-01");
        long toolId = send(post("/api/admin/tools"), admin, tool, 201).get(0).get("id").asLong();
        send(post("/api/admin/tools/" + toolId + "/resharpens"), admin,
                Map.of("resharpenedOn", "2031-03-20", "cost", 50), 201);

        // raw material bought in March: 10 kg x 100 = 1000
        send(post("/api/admin/rm-purchases"), admin, purchase(ids[1], supplier, "10", "100", "2031-03-02", null), 201);

        // typed expenses: 500 + 300
        JsonNode m = send(put("/api/admin/reports/monthly/2031-03/expenses"), admin,
                Map.of("consumables", 500, "salary", 300, "notes", "  Diwali bonus  "), 200);
        assertEquals(1, m.get("machineEntries").asInt());
        assertMoney("3", m.get("machineHours"));
        assertMoney("1350", m.get("turnover"));
        assertMoney("243", m.get("tax"));
        assertMoney("500", m.get("consumables"));
        assertMoney("300", m.get("salary"));
        assertMoney("800", m.get("totalExpense"));
        assertMoney("550", m.get("profit"));                 // 1350 - 800
        assertEquals("Diwali bonus", m.get("notes").asText());
        assertMoney("200", m.get("outsourcedCost"));
        assertMoney("900", m.get("rejectionDebit"));
        assertMoney("350", m.get("toolSpend"));
        assertMoney("1000", m.get("rmPurchases"));

        // several months at once: the neighbours are empty
        JsonNode months = send(get("/api/admin/reports/months?from=2031-02&count=3"), admin, null, 200);
        assertEquals(3, months.size());
        assertEquals("2031-02", months.get(0).get("month").asText());
        assertMoney("0", months.get(0).get("turnover"));
        assertMoney("550", months.get(1).get("profit"));
        assertMoney("0", months.get(2).get("turnover"));

        // a loss month shows a negative profit
        JsonNode loss = send(put("/api/admin/reports/monthly/2031-04/expenses"), admin,
                Map.of("consumables", 15500, "salary", 17083), 200);
        assertMoney("-32583", loss.get("profit"));

        // CSV
        MvcResult csv = mvc.perform(get("/api/admin/reports/monthly/2031-03/csv").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andReturn();
        String text = csv.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(text.contains("Month,2031-03"));
        assertTrue(text.contains("Profit (turnover - expense),550"));
        assertTrue(text.contains("VDE-0626-92-00-01"));
        assertTrue(csv.getResponse().getHeader("Content-Disposition").contains("vahan-2031-03.csv"));
    }

    @Test
    void reportValidation() throws Exception {
        String admin = adminToken();
        send(get("/api/admin/reports/monthly/March"), admin, null, 400);
        send(get("/api/admin/reports/months?from=2031-01&count=0"), admin, null, 400);
        send(get("/api/admin/reports/months?from=2031-01&count=25"), admin, null, 400);
        send(put("/api/admin/reports/monthly/2031-05/expenses"), admin, Map.of("consumables", -1, "salary", 0), 400);
        send(put("/api/admin/reports/monthly/2031-05/expenses"), admin, Map.of("salary", 0), 400);
        send(put("/api/admin/reports/monthly/nope/expenses"), admin, Map.of("consumables", 1, "salary", 0), 400);
    }
}

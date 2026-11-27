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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Release B: VMC machine hours (rate, tax, billable hours) and the tool register. */
@SpringBootTest
@AutoConfigureMockMvc
class ReleaseBFlowTest {

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
        for (JsonNode c : send(get("/api/admin/customers"), admin, null, 200)) {
            if (name.equals(c.get("name").asText())) {
                return c.get("id").asLong();
            }
        }
        throw new AssertionError("Customer not found: " + name);
    }

    /** Returns {projectId, partId}. */
    private long[] projectWithPart(String admin, String projectNo, String partNo, Long customerId) throws Exception {
        Map<String, Object> project = new HashMap<>();
        project.put("drawingNo", projectNo);
        project.put("name", "Release B test");
        project.put("customerId", customerId);
        long projectId = send(post("/api/admin/projects"), admin, project, 201).get("id").asLong();
        long partId = send(post("/api/admin/projects/" + projectId + "/parts"), admin,
                Map.of("drawingNo", partNo, "name", "Base plate", "quantity", 2), 201)
                .get("parts").get(0).get("id").asLong();
        return new long[]{projectId, partId};
    }

    private Map<String, Object> entry(long projectId, Long partId, Integer design, Integer setting, Integer run,
                                      String rate) {
        Map<String, Object> e = new HashMap<>();
        e.put("entryDate", "2026-11-20");
        e.put("projectId", projectId);
        e.put("childPartId", partId);
        e.put("quantity", 1);
        e.put("hardened", false);
        e.put("designMinutes", design);
        e.put("settingMinutes", setting);
        e.put("runMinutes", run);
        e.put("idleMinutes", 30);
        e.put("powerCutMinutes", 120);
        e.put("ratePerHour", rate == null ? null : new BigDecimal(rate));
        e.put("remarks", "  POWER CUT  ");
        return e;
    }

    private static void assertMoney(String expected, JsonNode actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual.decimalValue()),
                "expected " + expected + " but was " + actual);
    }

    // ---------- machine hours ----------

    @Test
    void machineHourMatchesTheSheet() throws Exception {
        String admin = adminToken();
        long deno = customerId(admin, "DENO");
        long[] ids = projectWithPart(admin, "VDE-0626-81-00-00", "VDE-0626-81-00-01", deno);
        send(get("/api/admin/machine-hours"), null, null, 401);

        // Sheet row 1: 20 + 40 + 120 minutes = 3 hours at 450 = 1350, tax 18% = 243
        JsonNode e = send(post("/api/admin/machine-hours"), admin, entry(ids[0], ids[1], 20, 40, 120, null), 201);
        assertMoney("3.00", e.get("billableHours"));
        assertMoney("450", e.get("ratePerHour"));
        assertMoney("18", e.get("taxPercent"));
        assertMoney("1350", e.get("amount"));
        assertMoney("243", e.get("taxAmount"));
        assertMoney("1593", e.get("total"));
        assertEquals(30, e.get("idleMinutes").asInt());       // recorded, not billed
        assertEquals(120, e.get("powerCutMinutes").asInt());
        assertEquals("POWER CUT", e.get("remarks").asText());
        assertEquals("DENO", e.get("customerName").asText()); // customer comes from the project
        assertEquals("VDE-0626-81-00-01", e.get("partDrawingNo").asText());

        // a typed rate overrides the default for this entry only
        JsonNode typed = send(post("/api/admin/machine-hours"), admin, entry(ids[0], null, 20, 40, 120, "500"), 201);
        assertMoney("1500", typed.get("amount"));
        assertMoney("270", typed.get("taxAmount"));

        // empty minutes count as 0
        JsonNode empty = send(post("/api/admin/machine-hours"), admin, entry(ids[0], null, null, null, null, null), 201);
        assertMoney("0", empty.get("amount"));

        // list filtered by project and month, with totals
        JsonNode list = send(get("/api/admin/machine-hours?projectId=" + ids[0] + "&month=2026-11"), admin, null, 200);
        assertEquals(3, list.get("entries").size());
        assertMoney("6.00", list.get("totals").get("billableHours"));
        assertMoney("2850", list.get("totals").get("amount"));
        assertMoney("513", list.get("totals").get("taxAmount"));
        assertMoney("3363", list.get("totals").get("total"));
        assertEquals(0, send(get("/api/admin/machine-hours?projectId=" + ids[0] + "&month=2026-10"), admin, null, 200)
                .get("entries").size());
        send(get("/api/admin/machine-hours?month=November"), admin, null, 400);
    }

    @Test
    void machineHourValidationAndEditAndDelete() throws Exception {
        String admin = adminToken();
        long[] a = projectWithPart(admin, "VDE-0626-82-00-00", "VDE-0626-82-00-01", null);
        long[] b = projectWithPart(admin, "VDE-0626-83-00-00", "VDE-0626-83-00-01", null);

        send(post("/api/admin/machine-hours"), admin, entry(a[0], b[1], 10, 10, 10, null), 400);      // part of another project
        send(post("/api/admin/machine-hours"), admin, entry(999999, null, 10, 10, 10, null), 400);    // unknown project
        send(post("/api/admin/machine-hours"), admin, entry(a[0], null, -5, 10, 10, null), 400);      // negative minutes
        Map<String, Object> noQty = entry(a[0], null, 10, 10, 10, null);
        noQty.put("quantity", 0);
        send(post("/api/admin/machine-hours"), admin, noQty, 400);

        long id = send(post("/api/admin/machine-hours"), admin, entry(a[0], a[1], 30, 30, 60, null), 201).get("id").asLong();
        JsonNode edited = send(put("/api/admin/machine-hours/" + id), admin, entry(a[0], a[1], 30, 30, 120, null), 200);
        assertMoney("3.00", edited.get("billableHours"));
        assertMoney("1350", edited.get("amount"));

        send(delete("/api/admin/machine-hours/" + id), admin, null, 204);
        send(delete("/api/admin/machine-hours/" + id), admin, null, 404);
        send(put("/api/admin/machine-hours/" + id), admin, entry(a[0], a[1], 1, 1, 1, null), 404);
    }

    @Test
    void changingTheDefaultRateNeverChangesOldEntries() throws Exception {
        String admin = adminToken();
        long[] ids = projectWithPart(admin, "VDE-0626-84-00-00", "VDE-0626-84-00-01", null);
        JsonNode defaults = send(get("/api/admin/machine-hour-settings"), admin, null, 200);
        assertMoney("450", defaults.get("ratePerHour"));
        assertMoney("18", defaults.get("taxPercent"));
        try {
            long id = send(post("/api/admin/machine-hours"), admin, entry(ids[0], null, 0, 0, 60, null), 201)
                    .get("id").asLong();

            send(put("/api/admin/machine-hour-settings"), admin, Map.of("ratePerHour", 600, "taxPercent", 12), 200);
            JsonNode fresh = send(post("/api/admin/machine-hours"), admin, entry(ids[0], null, 0, 0, 60, null), 201);
            assertMoney("600", fresh.get("amount"));
            assertMoney("72", fresh.get("taxAmount"));

            // old entry keeps its own rate and tax, even when edited
            JsonNode old = send(put("/api/admin/machine-hours/" + id), admin, entry(ids[0], null, 0, 0, 120, null), 200);
            assertMoney("450", old.get("ratePerHour"));
            assertMoney("18", old.get("taxPercent"));
            assertMoney("900", old.get("amount"));

            send(put("/api/admin/machine-hour-settings"), admin, Map.of("ratePerHour", -1, "taxPercent", 18), 400);
            send(put("/api/admin/machine-hour-settings"), admin, Map.of("ratePerHour", 450, "taxPercent", 101), 400);
        } finally {
            send(put("/api/admin/machine-hour-settings"), admin, Map.of("ratePerHour", 450, "taxPercent", 18), 200);
        }
    }

    // ---------- tools ----------

    private Map<String, Object> tool(String type, String description, String size, String cost) {
        Map<String, Object> t = new HashMap<>();
        t.put("toolType", type);
        t.put("description", description);
        t.put("toolSize", size);
        t.put("cost", cost == null ? null : new BigDecimal(cost));
        t.put("purchasedOn", "2026-08-01");
        return t;
    }

    @Test
    void toolLifecycle() throws Exception {
        String admin = adminToken();
        send(get("/api/admin/tools"), null, null, 401);

        Map<String, Object> input = tool("Cutter", "Ball Nose Endmil", "2.5x50", "1200");
        input.put("count", 3);
        JsonNode created = send(post("/api/admin/tools"), admin, input, 201);
        assertEquals(3, created.size());
        assertEquals("IN_STOCK", created.get(0).get("status").asText());
        long id = created.get(0).get("id").asLong();

        // issue it to the machine
        Map<String, Object> issued = tool("Cutter", "Ball Nose Endmil", "2.5x50", "1200");
        issued.put("issuedOn", "2026-08-05");
        assertEquals("ISSUED", send(put("/api/admin/tools/" + id), admin, issued, 200).get("status").asText());

        // two resharpenings
        send(post("/api/admin/tools/" + id + "/resharpens"), admin, Map.of("resharpenedOn", "2026-09-01", "cost", 150), 201);
        JsonNode afterTwo = send(post("/api/admin/tools/" + id + "/resharpens"), admin,
                Map.of("resharpenedOn", "2026-10-01", "cost", 200), 201);
        assertEquals(2, afterTwo.get("resharpenCount").asInt());
        assertMoney("350", afterTwo.get("resharpenCost"));
        assertMoney("1550", afterTwo.get("totalCost"));

        // breaking: reason/project/part need the date; broken and scrapped can't both be set
        Map<String, Object> reasonOnly = new HashMap<>(issued);
        reasonOnly.put("brokenReason", "HARDNESS");
        send(put("/api/admin/tools/" + id), admin, reasonOnly, 400);
        Map<String, Object> both = new HashMap<>(issued);
        both.put("brokenOn", "2026-11-01");
        both.put("scrappedOn", "2026-11-02");
        send(put("/api/admin/tools/" + id), admin, both, 400);

        long[] ids = projectWithPart(admin, "VDE-0626-85-00-00", "VDE-0626-85-00-01", null);
        long[] other = projectWithPart(admin, "VDE-0626-86-00-00", "VDE-0626-86-00-01", null);
        Map<String, Object> wrongPart = new HashMap<>(issued);
        wrongPart.put("brokenOn", "2026-11-01");
        wrongPart.put("brokenProjectId", ids[0]);
        wrongPart.put("brokenPartId", other[1]);
        send(put("/api/admin/tools/" + id), admin, wrongPart, 400);

        Map<String, Object> broken = new HashMap<>(issued);
        broken.put("brokenOn", "2026-11-01");
        broken.put("brokenReason", "HARDNESS");
        broken.put("brokenProjectId", ids[0]);
        broken.put("brokenPartId", ids[1]);
        JsonNode b = send(put("/api/admin/tools/" + id), admin, broken, 200);
        assertEquals("BROKEN", b.get("status").asText());
        assertEquals("VDE-0626-85-00-00", b.get("brokenProjectDrawingNo").asText());
        assertEquals("VDE-0626-85-00-01", b.get("brokenPartDrawingNo").asText());

        // a broken tool can't be resharpened, and one with history can't be deleted
        send(post("/api/admin/tools/" + id + "/resharpens"), admin, Map.of("cost", 100), 400);
        send(delete("/api/admin/tools/" + id), admin, null, 409);

        // status filter
        JsonNode brokenList = send(get("/api/admin/tools?status=BROKEN"), admin, null, 200);
        assertTrue(brokenList.toString().contains("\"id\":" + id));
        assertEquals(0, send(get("/api/admin/tools?status=SCRAPPED"), admin, null, 200).size());
        send(get("/api/admin/tools?status=LOST"), admin, null, 400);

        // removing a resharpening works; a never-resharpened tool can be deleted
        long rid = b.get("resharpens").get(0).get("id").asLong();
        assertEquals(1, send(delete("/api/admin/tools/resharpens/" + rid), admin, null, 200).get("resharpenCount").asInt());
        send(delete("/api/admin/tools/resharpens/" + rid), admin, null, 404);
        long clean = created.get(1).get("id").asLong();
        send(delete("/api/admin/tools/" + clean), admin, null, 204);
        send(delete("/api/admin/tools/" + clean), admin, null, 404);
    }

    @Test
    void toolValidation() throws Exception {
        String admin = adminToken();
        send(post("/api/admin/tools"), admin, tool(" ", "x", null, null), 400);
        send(post("/api/admin/tools"), admin, tool("Drill", " ", null, null), 400);
        send(post("/api/admin/tools"), admin, tool("Drill", "x", null, "-5"), 400);
        Map<String, Object> early = tool("Drill", "Dia 5 drill", "5", "100");
        early.put("issuedOn", "2026-07-01");   // before it was bought (2026-08-01)
        send(post("/api/admin/tools"), admin, early, 400);
        Map<String, Object> tooMany = tool("Drill", "x", null, null);
        tooMany.put("count", 101);
        send(post("/api/admin/tools"), admin, tooMany, 400);
        send(post("/api/admin/tools/999999/resharpens"), admin, Map.of("cost", 10), 404);
    }
}

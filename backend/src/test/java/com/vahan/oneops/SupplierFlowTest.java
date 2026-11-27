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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Suppliers (from Vahan's supplier sheet) and giving a stage to a supplier. */
@SpringBootTest
@AutoConfigureMockMvc
class SupplierFlowTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;

    // ---------- helpers ----------

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

    private String login(String username, String password) throws Exception {
        return send(post("/api/auth/login"), null,
                Map.of("username", username, "password", password), 200).get("token").asText();
    }

    private String adminToken() throws Exception {
        return login("admin", "Admin#12345");
    }

    private long createEmployee(String admin, String username, String fullName) throws Exception {
        return send(post("/api/admin/users"), admin,
                Map.of("username", username, "fullName", fullName, "role", "EMPLOYEE", "password", "Passw0rd!x"),
                201).get("id").asLong();
    }

    private static Map<String, Object> process(String stage, String person, String phone) {
        Map<String, Object> p = new HashMap<>();
        p.put("stageName", stage);
        p.put("contactPerson", person);
        p.put("contactPhone", phone);
        return p;
    }

    private static Map<String, Object> supplier(String name, String address, String mapUrl,
                                                List<Map<String, Object>> processes) {
        Map<String, Object> s = new HashMap<>();
        s.put("name", name);
        s.put("address", address);
        s.put("mapUrl", mapUrl);
        s.put("processes", processes);
        return s;
    }

    private static Map<String, Object> outsourcing(Long supplierId, Integer qty, BigDecimal rate,
                                                   boolean pre, boolean fin, String remarks) {
        Map<String, Object> o = new HashMap<>();
        o.put("supplierId", supplierId);
        o.put("jobQty", qty);
        o.put("ratePerUnit", rate);
        o.put("preInspectionDone", pre);
        o.put("finalInspectionDone", fin);
        o.put("remarks", remarks);
        return o;
    }

    private JsonNode byName(JsonNode suppliers, String name) {
        for (JsonNode s : suppliers) {
            if (name.equals(s.get("name").asText())) {
                return s;
            }
        }
        throw new AssertionError("Supplier not found: " + name);
    }

    private boolean hasProcess(JsonNode supplier, String stageName) {
        for (JsonNode p : supplier.get("processes")) {
            if (stageName.equals(p.get("stageName").asText())) {
                return true;
            }
        }
        return false;
    }

    // ---------- tests ----------

    @Test
    void theSupplierSheetIsLoaded() throws Exception {
        String admin = adminToken();
        JsonNode all = send(get("/api/admin/suppliers"), admin, null, 200);
        assertTrue(all.size() >= 37, "the 37 suppliers of the sheet should be there");

        // V.J ENGINEERING is on two rows of the sheet (turning and drilling): one supplier, two processes.
        JsonNode vj = byName(all, "V.J ENGINEERING");
        assertEquals(2, vj.get("processes").size());
        assertTrue(hasProcess(vj, "Turning"));
        assertTrue(hasProcess(vj, "Drilling"));

        // One supplier, two processes, a different contact for each.
        JsonNode rv = byName(all, "R.V. TOOLING");
        assertTrue(hasProcess(rv, "Cylindrical Grinding"));
        assertTrue(hasProcess(rv, "Surface Grinding"));
        String people = rv.get("processes").toString();
        assertTrue(people.contains("Kalaiyarasan") && people.contains("Raj Kumar"));

        // "Milling+SG" in the sheet means two processes.
        JsonNode vnr = byName(all, "V.N.R INDUSTRIES");
        assertTrue(hasProcess(vnr, "Conventional Milling"));
        assertTrue(hasProcess(vnr, "Surface Grinding"));
        assertEquals("https://maps.app.goo.gl/XaP49CM45dB83eDKA", vnr.get("mapUrl").asText());

        // Processes the stage list did not have yet were added to it.
        boolean found = false;
        for (JsonNode m : send(get("/api/stage-master"), admin, null, 200)) {
            found |= "Hot Chroming".equals(m.get("name").asText());
        }
        assertTrue(found, "Hot Chroming should now be a stage");
    }

    @Test
    void employeesCannotUseTheSupplierList() throws Exception {
        String admin = adminToken();
        createEmployee(admin, "emp_suppliers", "Supplier Test");
        String emp = login("emp_suppliers", "Passw0rd!x");
        send(get("/api/admin/suppliers"), emp, null, 403);
        send(get("/api/admin/suppliers"), null, null, 401);
    }

    @Test
    void adminAddsEditsAndDeactivatesASupplier() throws Exception {
        String admin = adminToken();

        JsonNode created = send(post("/api/admin/suppliers"), admin,
                supplier("  Test   Supplier Alpha ", "Hosur", "https://maps.example.com/alpha",
                        List.of(process("Wire Cut", "Anil", "9000000001"),
                                process("Laser Marking Alpha", "  ", null))), 201);
        long id = created.get("id").asLong();
        assertEquals("Test Supplier Alpha", created.get("name").asText());   // spacing cleaned up
        assertTrue(created.get("active").asBoolean());
        assertEquals(2, created.get("processes").size());
        assertTrue(created.get("processes").get(1).get("contactPerson").isNull());   // blank becomes empty

        // A process typed in that is not in the stage list yet is added to it.
        boolean added = false;
        for (JsonNode m : send(get("/api/stage-master"), admin, null, 200)) {
            added |= "Laser Marking Alpha".equals(m.get("name").asText());
        }
        assertTrue(added);

        // Same name, different case: refused.
        send(post("/api/admin/suppliers"), admin,
                supplier("test supplier alpha", null, null, List.of()), 409);

        // Map links must be web links (they are shown as clickable links).
        send(post("/api/admin/suppliers"), admin,
                supplier("Test Supplier Bad Link", null, "javascript:alert(1)", List.of()), 400);

        // Same process twice is refused, and nothing is changed.
        send(put("/api/admin/suppliers/" + id), admin,
                supplier("Test Supplier Alpha", "Hosur", null,
                        List.of(process("Drilling", null, null), process("drilling", null, null))), 400);
        assertEquals(2, send(get("/api/admin/suppliers/" + id), admin, null, 200).get("processes").size());

        // Editing replaces the details and the process list.
        JsonNode edited = send(put("/api/admin/suppliers/" + id), admin,
                supplier("Test Supplier Alpha Works", "MK Palli", null,
                        List.of(process("Drilling", "Bala", "9000000002"))), 200);
        assertEquals("Test Supplier Alpha Works", edited.get("name").asText());
        assertEquals("MK Palli", edited.get("address").asText());
        assertEquals(1, edited.get("processes").size());
        assertEquals("Drilling", edited.get("processes").get(0).get("stageName").asText());

        // Deactivate: gone from the normal list, still there when asked for.
        JsonNode off = send(patch("/api/admin/suppliers/" + id), admin, Map.of("active", false), 200);
        assertFalse(off.get("active").asBoolean());
        JsonNode active = send(get("/api/admin/suppliers"), admin, null, 200);
        boolean listed = false;
        for (JsonNode s : active) {
            listed |= s.get("id").asLong() == id;
        }
        assertFalse(listed);
        assertNotNull(byName(send(get("/api/admin/suppliers?includeInactive=true"), admin, null, 200),
                "Test Supplier Alpha Works"));

        send(get("/api/admin/suppliers/999999"), admin, null, 404);
    }

    @Test
    void aStageCanBeGivenToASupplierAndEmployeesSeeWhere() throws Exception {
        String admin = adminToken();
        long meena = createEmployee(admin, "meena_supplier", "Meena Coordinator");

        long supplierId = send(post("/api/admin/suppliers"), admin,
                supplier("Test Supplier Beta", "Zuzuvadi", null,
                        List.of(process("Wire Cut", "Vikram", "9000000003"))), 201).get("id").asLong();

        JsonNode project = send(post("/api/admin/projects"), admin,
                Map.of("drawingNo", "VDE-0324-88-00-00", "name", "Outsourcing test"), 201);
        long projectId = project.get("id").asLong();
        long partId = send(post("/api/admin/projects/" + projectId + "/parts"), admin,
                Map.of("drawingNo", "VDE-0324-88-00-01", "name", "Slider", "quantity", 10), 201)
                .get("parts").get(0).get("id").asLong();
        JsonNode withStage = send(post("/api/admin/parts/" + partId + "/stages"), admin,
                Map.of("stages", List.of(Map.of("customName", "Wire Cut", "assigneeId", meena))), 201);
        JsonNode stage = withStage.get("parts").get(0).get("stages").get(0);
        long stageId = stage.get("id").asLong();
        assertTrue(stage.get("supplierId").isNull());   // in-house until given to a supplier
        assertTrue(stage.get("totalCost").isNull());

        // Give it to the supplier: 5 pieces at 80.50 each.
        JsonNode updated = send(put("/api/admin/stages/" + stageId + "/outsourcing"), admin,
                outsourcing(supplierId, 5, new BigDecimal("80.50"), true, false, "  Chamfer rework  "), 200);
        JsonNode s = updated.get("parts").get(0).get("stages").get(0);
        assertEquals(supplierId, s.get("supplierId").asLong());
        assertEquals("Test Supplier Beta", s.get("supplierName").asText());
        assertEquals(5, s.get("jobQty").asInt());
        assertEquals(0, new BigDecimal("402.50").compareTo(s.get("totalCost").decimalValue()));
        assertTrue(s.get("preInspectionDone").asBoolean());
        assertFalse(s.get("finalInspectionDone").asBoolean());
        assertEquals("Chamfer rework", s.get("remarks").asText());

        // The person who owns the stage sees which supplier has it.
        String emp = login("meena_supplier", "Passw0rd!x");
        JsonNode mine = send(get("/api/my-work"), emp, null, 200);
        boolean seen = false;
        for (JsonNode w : mine) {
            if (w.get("stageId").asLong() == stageId) {
                assertEquals("Test Supplier Beta", w.get("supplierName").asText());
                seen = true;
            }
        }
        assertTrue(seen);

        // Rules: quantity must be at least 1; quantity and rate need a supplier; the supplier must exist.
        send(put("/api/admin/stages/" + stageId + "/outsourcing"), admin,
                outsourcing(supplierId, 0, new BigDecimal("10"), false, false, null), 400);
        send(put("/api/admin/stages/" + stageId + "/outsourcing"), admin,
                outsourcing(null, 3, null, false, false, null), 400);
        send(put("/api/admin/stages/" + stageId + "/outsourcing"), admin,
                outsourcing(999999L, 1, null, false, false, null), 400);

        // A deactivated supplier can stay on a stage it already has, but cannot be chosen for another.
        send(patch("/api/admin/suppliers/" + supplierId), admin, Map.of("active", false), 200);
        send(put("/api/admin/stages/" + stageId + "/outsourcing"), admin,
                outsourcing(supplierId, 5, new BigDecimal("80.50"), true, true, "Received"), 200);
        JsonNode second = send(post("/api/admin/parts/" + partId + "/stages"), admin,
                Map.of("stages", List.of(Map.of("customName", "Drilling"))), 201);
        long secondId = second.get("parts").get(0).get("stages").get(1).get("id").asLong();
        send(put("/api/admin/stages/" + secondId + "/outsourcing"), admin,
                outsourcing(supplierId, 1, null, false, false, null), 400);

        // Taking a stage back in-house clears the job details.
        JsonNode back = send(put("/api/admin/stages/" + stageId + "/outsourcing"), admin,
                outsourcing(null, null, null, false, false, null), 200);
        JsonNode inHouse = back.get("parts").get(0).get("stages").get(0);
        assertTrue(inHouse.get("supplierId").isNull());
        assertTrue(inHouse.get("supplierName").isNull());
        assertTrue(inHouse.get("jobQty").isNull());
        assertTrue(inHouse.get("totalCost").isNull());
    }
}

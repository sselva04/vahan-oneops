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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The central material list and the raw material of a part (theoretical weight, actual weight, cost). */
@SpringBootTest
@AutoConfigureMockMvc
class MaterialFlowTest {

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

    private long materialId(String admin, String name) throws Exception {
        for (JsonNode m : send(get("/api/admin/materials?includeInactive=true"), admin, null, 200)) {
            if (name.equals(m.get("name").asText())) {
                return m.get("id").asLong();
            }
        }
        throw new AssertionError("Material not found: " + name);
    }

    private static Map<String, Object> rm(Long materialId, String shape, String a, String b, String c,
                                          String manual, String actual) {
        Map<String, Object> m = new HashMap<>();
        m.put("materialId", materialId);
        m.put("shape", shape);
        m.put("dimA", a == null ? null : new BigDecimal(a));
        m.put("dimB", b == null ? null : new BigDecimal(b));
        m.put("dimC", c == null ? null : new BigDecimal(c));
        m.put("manualWeightKg", manual == null ? null : new BigDecimal(manual));
        m.put("actualWeightKg", actual == null ? null : new BigDecimal(actual));
        return m;
    }

    private static Map<String, Object> materialBody(String name, String density, String price) {
        Map<String, Object> m = new HashMap<>();
        m.put("name", name);
        m.put("density", new BigDecimal(density));
        m.put("pricePerKg", price == null ? null : new BigDecimal(price));
        return m;
    }

    private long newPart(String admin, String projectNo, String partNo, int qty) throws Exception {
        long projectId = send(post("/api/admin/projects"), admin,
                Map.of("drawingNo", projectNo, "name", "Raw material test"), 201).get("id").asLong();
        return send(post("/api/admin/projects/" + projectId + "/parts"), admin,
                Map.of("drawingNo", partNo, "name", "Plate", "quantity", qty), 201)
                .get("parts").get(0).get("id").asLong();
    }

    @Test
    void theStartingListIsThereAndEditable() throws Exception {
        String admin = adminToken();
        JsonNode all = send(get("/api/admin/materials"), admin, null, 200);
        long ohns = materialId(admin, "OHNS");
        boolean found = false;
        for (JsonNode m : all) {
            if ("OHNS".equals(m.get("name").asText())) {
                found = true;
                assertEquals(0, new BigDecimal("7.85").compareTo(m.get("density").decimalValue()));
                assertFalse(m.get("pricePerKg").isNull());   // the PO's rate was loaded; the exact value is editable
            }
        }
        assertTrue(found);

        // Callers who are not signed in are kept out (admin-only like everything under /api/admin).
        send(get("/api/admin/materials"), null, null, 401);

        // Add, rename clash, edit, deactivate.
        long id = send(post("/api/admin/materials"), admin, materialBody("  Test   Bronze ", "8.800", null), 201)
                .get("id").asLong();
        send(post("/api/admin/materials"), admin, materialBody("test bronze", "8.8", null), 409);
        send(post("/api/admin/materials"), admin, materialBody("Bad", "0", null), 400);
        send(post("/api/admin/materials"), admin, materialBody("Bad2", "7.85", "-1"), 400);
        JsonNode edited = send(put("/api/admin/materials/" + id), admin, materialBody("Test Bronze", "8.8", "620.50"), 200);
        assertEquals(0, new BigDecimal("620.50").compareTo(edited.get("pricePerKg").decimalValue()));
        assertFalse(send(patch("/api/admin/materials/" + id), admin, Map.of("active", false), 200).get("active").asBoolean());
        assertTrue(ohns > 0);
    }

    @Test
    void weightAndCostFollowSizeMaterialAndActualWeight() throws Exception {
        String admin = adminToken();
        long ohns = materialId(admin, "OHNS");
        long partId = newPart(admin, "VDE-0526-91-00-00", "VDE-0526-91-00-01", 1);

        // PO row 1: OHNS 128 x 100 x 20, qty 1 -> 2.01 kg at 105 = 211.05 (PO shows 211.26 from the rounded 2.012)
        JsonNode detail = send(put("/api/admin/parts/" + partId + "/raw-material"), admin,
                rm(ohns, "BLOCK", "128", "100", "20", null, null), 200);
        JsonNode r = detail.get("parts").get(0).get("rawMaterial");
        assertEquals("OHNS", r.get("materialName").asText());
        assertEquals(0, new BigDecimal("2.010").compareTo(r.get("theoreticalTotalKg").decimalValue()));
        assertTrue(r.get("actualWeightKg").isNull());
        assertFalse(r.get("costFromActual").asBoolean());
        assertEquals(0, new BigDecimal("211.05").compareTo(r.get("cost").decimalValue()));
        assertEquals(0, new BigDecimal("211.05").compareTo(detail.get("rawMaterialCost").decimalValue()));

        // The stock is weighed: 2.150 kg. Cost now follows the actual weight, and the difference is shown.
        JsonNode weighed = send(put("/api/admin/parts/" + partId + "/raw-material"), admin,
                rm(ohns, "BLOCK", "128", "100", "20", null, "2.150"), 200)
                .get("parts").get(0).get("rawMaterial");
        assertTrue(weighed.get("costFromActual").asBoolean());
        assertEquals(0, new BigDecimal("0.140").compareTo(weighed.get("differenceKg").decimalValue()));
        assertEquals(0, new BigDecimal("7.0").compareTo(weighed.get("differencePercent").decimalValue()));
        assertEquals(0, new BigDecimal("225.75").compareTo(weighed.get("cost").decimalValue()));

        // A price change does not rewrite this part's cost until its raw material is saved again.
        send(put("/api/admin/materials/" + ohns), admin, materialBody("OHNS", "7.85", "110"), 200);
        JsonNode still = send(get("/api/admin/projects/" + detail.get("id").asLong()), admin, null, 200)
                .get("parts").get(0).get("rawMaterial");
        assertEquals(0, new BigDecimal("105").compareTo(still.get("ratePerKg").decimalValue()));
        JsonNode resaved = send(put("/api/admin/parts/" + partId + "/raw-material"), admin,
                rm(ohns, "BLOCK", "128", "100", "20", null, "2.150"), 200).get("parts").get(0).get("rawMaterial");
        assertEquals(0, new BigDecimal("110").compareTo(resaved.get("ratePerKg").decimalValue()));
        send(put("/api/admin/materials/" + ohns), admin, materialBody("OHNS", "7.85", "105"), 200);   // put it back
    }

    @Test
    void quantityMultipliesAndEveryShapeHasItsOwnRules() throws Exception {
        String admin = adminToken();
        long en8 = materialId(admin, "EN8");
        long partId = newPart(admin, "VDE-0526-92-00-00", "VDE-0526-92-00-01", 4);

        // Hook clamp style: EN8 round 40 x 30, quantity 4.
        JsonNode round = send(put("/api/admin/parts/" + partId + "/raw-material"), admin,
                rm(en8, "ROUND", "40", "30", null, null, null), 200).get("parts").get(0).get("rawMaterial");
        assertEquals(0, new BigDecimal("0.296").compareTo(round.get("theoreticalPerPieceKg").decimalValue()));
        assertEquals(0, new BigDecimal("1.184").compareTo(round.get("theoreticalTotalKg").decimalValue()));
        assertEquals(0, new BigDecimal("105.38").compareTo(round.get("cost").decimalValue()));   // 1.184 x 89

        // Wrong sizes for the shape, inner >= outer, unknown material, missing shape.
        send(put("/api/admin/parts/" + partId + "/raw-material"), admin, rm(en8, "ROUND", "40", null, null, null, null), 400);
        send(put("/api/admin/parts/" + partId + "/raw-material"), admin, rm(en8, "ROUND", "40", "30", "5", null, null), 400);
        send(put("/api/admin/parts/" + partId + "/raw-material"), admin, rm(en8, "TUBE", "40", "40", "100", null, null), 400);
        send(put("/api/admin/parts/" + partId + "/raw-material"), admin, rm(999999L, "BLOCK", "1", "1", "1", null, null), 400);
        send(put("/api/admin/parts/" + partId + "/raw-material"), admin, rm(en8, null, "1", "1", "1", null, null), 400);
        send(put("/api/admin/parts/" + partId + "/raw-material"), admin, rm(en8, "BLOCK", "0", "1", "1", null, null), 400);
        send(put("/api/admin/parts/" + partId + "/raw-material"), admin, rm(null, "ROUND", "1", "1", null, null, null), 400);
        send(put("/api/admin/parts/999999/raw-material"), admin, rm(en8, "MANUAL", null, null, null, "1.2", null), 404);

        // Tube and manual weight work, and a material without a price gives no cost (not zero).
        long ms = materialId(admin, "MS");
        JsonNode tube = send(put("/api/admin/parts/" + partId + "/raw-material"), admin,
                rm(ms, "TUBE", "50", "40", "100", null, null), 200).get("parts").get(0).get("rawMaterial");
        assertEquals(0, new BigDecimal("2.220").compareTo(tube.get("theoreticalTotalKg").decimalValue()));   // 0.555 x 4
        assertTrue(tube.get("cost").isNull());
        JsonNode manual = send(put("/api/admin/parts/" + partId + "/raw-material"), admin,
                rm(ms, "MANUAL", null, null, null, "1.2", null), 200).get("parts").get(0).get("rawMaterial");
        assertEquals(0, new BigDecimal("4.800").compareTo(manual.get("theoreticalTotalKg").decimalValue()));

        // Clearing: everything empty and no material.
        JsonNode cleared = send(put("/api/admin/parts/" + partId + "/raw-material"), admin,
                rm(null, null, null, null, null, null, null), 200);
        assertTrue(cleared.get("parts").get(0).get("rawMaterial").isNull());
    }

    @Test
    void aDeactivatedMaterialCannotBePickedForANewPart() throws Exception {
        String admin = adminToken();
        long id = send(post("/api/admin/materials"), admin, materialBody("Test Retired", "7.85", "50"), 201)
                .get("id").asLong();
        long partId = newPart(admin, "VDE-0526-93-00-00", "VDE-0526-93-00-01", 1);
        send(put("/api/admin/parts/" + partId + "/raw-material"), admin, rm(id, "BLOCK", "10", "10", "10", null, null), 200);
        send(patch("/api/admin/materials/" + id), admin, Map.of("active", false), 200);
        // It may stay on the part that already has it...
        send(put("/api/admin/parts/" + partId + "/raw-material"), admin, rm(id, "BLOCK", "10", "10", "20", null, null), 200);
        // ...but not be chosen for another.
        long other = newPart(admin, "VDE-0526-94-00-00", "VDE-0526-94-00-01", 1);
        send(put("/api/admin/parts/" + other + "/raw-material"), admin, rm(id, "BLOCK", "10", "10", "10", null, null), 400);
    }
}

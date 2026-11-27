package com.vahan.oneops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Dev-only data import: load a small data set, run it again, then remove the batch. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "vahan.import.enabled=true")
class ImportFlowTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;

    private JsonNode send(MockHttpServletRequestBuilder request, String token, Object body, int expectedStatus) throws Exception {
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

    @Test
    void loadsRerunsAndRemovesABatch() throws Exception {
        String admin = send(post("/api/auth/login"), null, Map.of("username", "admin", "password", "Admin#12345"), 200)
                .get("token").asText();
        String b = "t-import-1";
        send(post("/api/admin/import/begin?batch=" + b), admin, null, 204);

        assertEquals(1, send(post("/api/admin/import/suppliers?batch=" + b), admin, List.of("Import Test Supplier"), 200).get("created").asInt());
        send(post("/api/admin/import/projects?batch=" + b), admin,
                List.of(Map.of("drawingNo", "IMP-0001-00-00", "name", "Import project", "customerName", "DENO")), 200);
        send(post("/api/admin/import/parts?batch=" + b), admin,
                List.of(Map.of("projectDrawingNo", "IMP-0001-00-00", "drawingNo", "IMP-0001-00-01", "name", "Plate", "quantity", 4)), 200);
        JsonNode stages = send(post("/api/admin/import/stages?batch=" + b), admin, List.of(Map.of(
                "partDrawingNo", "IMP-0001-00-01", "stageName", "Wire Cut", "sequenceNo", 1, "supplierName", "Import Test Supplier",
                "jobQty", 4, "ratePerUnit", 100, "sentOn", "2025-01-10", "receivedOn", "2025-01-15", "status", "DONE")), 200);
        assertEquals(1, stages.get("created").asInt());
        assertEquals(1, send(post("/api/admin/import/rejections?batch=" + b), admin, List.of(Map.of(
                "partDrawingNo", "IMP-0001-00-01", "stageName", "Wire Cut", "supplierName", "Import Test Supplier", "sentOn", "2025-01-10",
                "rejectedQty", 1, "partCost", 500, "machiningCost", 100, "debitAmount", 400, "rejectedOn", "2025-01-16")), 200)
                .get("created").asInt());
        assertEquals(1, send(post("/api/admin/import/machine-hours?batch=" + b), admin, List.of(Map.of(
                "entryDate", "2025-01-12", "partDrawingNo", "IMP-0001-00-01", "quantity", 2, "hardened", false,
                "designMinutes", 0, "settingMinutes", 30, "runMinutes", 30, "idleMinutes", 0, "powerCutMinutes", 0, "ratePerHour", 450)), 200)
                .get("created").asInt());
        assertEquals(1, send(post("/api/admin/import/tools?batch=" + b), admin, List.of(Map.of(
                "toolType", "Drill", "description", "Drill 10mm", "cost", 250,
                "resharpens", List.of(Map.of("resharpenedOn", "2025-02-01", "cost", 40)))), 200).get("created").asInt());

        // Running the same rows again creates nothing new.
        assertEquals(0, send(post("/api/admin/import/projects?batch=" + b), admin,
                List.of(Map.of("drawingNo", "IMP-0001-00-00", "name", "Import project")), 200).get("created").asInt());
        // Unknown part is reported, not fatal.
        JsonNode bad = send(post("/api/admin/import/parts?batch=" + b), admin,
                List.of(Map.of("projectDrawingNo", "NOPE", "drawingNo", "IMP-X", "name", "X", "quantity", 1)), 200);
        assertEquals(0, bad.get("created").asInt());
        assertTrue(bad.get("problems").size() == 1);

        send(post("/api/admin/import/begin?batch=" + b), admin, null, 409);
        JsonNode status = send(get("/api/admin/import/status"), admin, null, 200);
        assertTrue(status.get("enabled").asBoolean());
        assertEquals(1, status.get("batches").size());

        send(delete("/api/admin/import/batches/" + b), admin, null, 200);
        assertEquals(0, send(get("/api/admin/import/status"), admin, null, 200).get("batches").size());
        send(post("/api/admin/import/begin?batch=" + b), admin, null, 204);
    }
}

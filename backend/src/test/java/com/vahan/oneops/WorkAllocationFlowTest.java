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

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Runs the real app against an in-memory database and walks through what Vahan will do. */
@SpringBootTest
@AutoConfigureMockMvc
class WorkAllocationFlowTest {

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

    private long stageMasterId(String token, String name) throws Exception {
        for (JsonNode n : send(get("/api/stage-master"), token, null, 200)) {
            if (name.equals(n.get("name").asText())) {
                return n.get("id").asLong();
            }
        }
        throw new AssertionError("Stage master not found: " + name);
    }

    // ---------- tests ----------

    @Test
    void wrongPasswordIsRejected() throws Exception {
        send(post("/api/auth/login"), null, Map.of("username", "admin", "password", "nope"), 401);
    }

    @Test
    void endpointsNeedLogin() throws Exception {
        send(get("/api/my-work"), null, null, 401);
        send(get("/api/admin/projects"), null, null, 401);
    }

    @Test
    void employeeCannotUseAdminEndpoints() throws Exception {
        String admin = adminToken();
        createEmployee(admin, "emp_forbidden", "Forbidden Test");
        String emp = login("emp_forbidden", "Passw0rd!x");

        send(get("/api/admin/projects"), emp, null, 403);
        send(get("/api/admin/users"), emp, null, 403);
        send(get("/api/my-work"), emp, null, 200);
    }

    @Test
    void adminAllocatesStagesAndEmployeesSeeOnlyTheirOwnWork() throws Exception {
        String admin = adminToken();
        long ravi = createEmployee(admin, "ravi_flow", "Ravi Kumar");
        long santhosh = createEmployee(admin, "santhosh_flow", "Santhosh S");

        // Project number is entered as the drawing number; messy spacing and case are cleaned up.
        JsonNode project = send(post("/api/admin/projects"), admin,
                Map.of("drawingNo", " vde-0324-77-00-00 ", "name", "Test gauge"), 201);
        assertEquals("VDE-0324-77-00-00", project.get("drawingNo").asText());
        long projectId = project.get("id").asLong();

        JsonNode withPart = send(post("/api/admin/projects/" + projectId + "/parts"), admin,
                Map.of("drawingNo", "VDE-0324-77-00-01", "name", "Base plate", "quantity", 2), 201);
        long partId = withPart.get("parts").get(0).get("id").asLong();

        // This part goes Milling (Ravi) -> VMC (Santhosh) -> Heat Treatment (not allocated yet).
        List<Map<String, Object>> route = List.of(
                Map.of("stageMasterId", stageMasterId(admin, "Conventional Milling"), "assigneeId", ravi),
                Map.of("stageMasterId", stageMasterId(admin, "VMC"), "assigneeId", santhosh),
                Map.of("stageMasterId", stageMasterId(admin, "Heat Treatment")));
        JsonNode detail = send(post("/api/admin/parts/" + partId + "/stages"), admin, Map.of("stages", route), 201);
        JsonNode stages = detail.get("parts").get(0).get("stages");
        assertEquals(3, stages.size());
        assertEquals("Conventional Milling", stages.get(0).get("stageName").asText());
        assertEquals(1, stages.get(0).get("sequenceNo").asInt());
        assertEquals("Ravi Kumar", stages.get(0).get("assigneeName").asText());
        assertTrue(stages.get(2).get("assigneeId").isNull());

        // The admin overview counts the unallocated stage.
        boolean seen = false;
        for (JsonNode p : send(get("/api/admin/projects"), admin, null, 200)) {
            if (p.get("id").asLong() == projectId) {
                assertEquals(3, p.get("stageCount").asInt());
                assertEquals(1, p.get("stagesUnassigned").asInt());
                seen = true;
            }
        }
        assertTrue(seen, "project should be in the admin list");

        // Santhosh sees only his VMC stage, and knows milling must finish first.
        String santhoshToken = login("santhosh_flow", "Passw0rd!x");
        JsonNode mine = send(get("/api/my-work"), santhoshToken, null, 200);
        assertEquals(1, mine.size());
        assertEquals("VMC", mine.get(0).get("stageName").asText());
        assertEquals("VDE-0324-77-00-01", mine.get(0).get("partDrawingNo").asText());
        assertEquals("Conventional Milling", mine.get(0).get("waitingFor").get(0).asText());

        // Ravi's list does not contain Santhosh's work.
        JsonNode ravis = send(get("/api/my-work"), login("ravi_flow", "Passw0rd!x"), null, 200);
        assertEquals(1, ravis.size());
        assertEquals("Conventional Milling", ravis.get(0).get("stageName").asText());
        assertEquals(0, ravis.get(0).get("waitingFor").size());

        // The admin can open any person's work list.
        JsonNode viaAdmin = send(get("/api/admin/users/" + santhosh + "/work"), admin, null, 200);
        assertEquals(1, viaAdmin.size());

        // Re-allocating the last stage, then removing it.
        long lastStageId = stages.get(2).get("id").asLong();
        send(put("/api/admin/stages/" + lastStageId), admin, Map.of("assigneeId", ravi), 200);
        assertEquals(2, send(get("/api/my-work"), login("ravi_flow", "Passw0rd!x"), null, 200).size());
        JsonNode afterDelete = send(delete("/api/admin/stages/" + lastStageId), admin, null, 200);
        assertEquals(2, afterDelete.get("parts").get(0).get("stages").size());
    }

    @Test
    void duplicateDrawingNumberIsRejected() throws Exception {
        String admin = adminToken();
        send(post("/api/admin/projects"), admin, Map.of("drawingNo", "VDE-0324-78-00-00", "name", "First"), 201);
        send(post("/api/admin/projects"), admin, Map.of("drawingNo", "vde-0324-78-00-00", "name", "Second"), 409);
    }

    @Test
    void deactivatedUserLosesAccessImmediately() throws Exception {
        String admin = adminToken();
        long id = createEmployee(admin, "emp_deactivate", "Deactivate Test");
        String emp = login("emp_deactivate", "Passw0rd!x");
        send(get("/api/my-work"), emp, null, 200);

        send(patch("/api/admin/users/" + id), admin, Map.of("active", false), 200);

        send(get("/api/my-work"), emp, null, 401);
        send(post("/api/auth/login"), null,
                Map.of("username", "emp_deactivate", "password", "Passw0rd!x"), 401);
    }

    @Test
    void adminCannotDeactivateTheirOwnAccount() throws Exception {
        String admin = adminToken();
        long adminId = send(get("/api/auth/me"), admin, null, 200).get("id").asLong();
        send(patch("/api/admin/users/" + adminId), admin, Map.of("active", false), 400);
    }

    @Test
    void cannotAllocateToAnInactiveUser() throws Exception {
        String admin = adminToken();
        long id = createEmployee(admin, "emp_inactive_target", "Inactive Target");
        send(patch("/api/admin/users/" + id), admin, Map.of("active", false), 200);
        JsonNode project = send(post("/api/admin/projects"), admin,
                Map.of("drawingNo", "VDE-0324-79-00-00", "name", "Inactive test"), 201);
        JsonNode detail = send(post("/api/admin/projects/" + project.get("id").asLong() + "/parts"), admin,
                Map.of("drawingNo", "VDE-0324-79-00-01", "name", "Plate", "quantity", 1), 201);
        long partId = detail.get("parts").get(0).get("id").asLong();
        send(post("/api/admin/parts/" + partId + "/stages"), admin,
                Map.of("stages", List.of(Map.of("stageMasterId", stageMasterId(admin, "VMC"), "assigneeId", id))), 400);
    }

    @Test
    void changePasswordChecksTheCurrentOne() throws Exception {
        String admin = adminToken();
        createEmployee(admin, "emp_pwd", "Password Test");
        String emp = login("emp_pwd", "Passw0rd!x");
        send(post("/api/auth/change-password"), emp,
                Map.of("currentPassword", "wrong", "newPassword", "NewPassw0rd!"), 400);
        send(post("/api/auth/change-password"), emp,
                Map.of("currentPassword", "Passw0rd!x", "newPassword", "NewPassw0rd!"), 204);
        login("emp_pwd", "NewPassw0rd!");
    }

    // ---------- start / complete and custom stages ----------

    /** Creates a project with one part and returns the part id. */
    private long projectWithPart(String admin, String projectNo, String partNo) throws Exception {
        JsonNode project = send(post("/api/admin/projects"), admin, Map.of("drawingNo", projectNo, "name", "Flow test"), 201);
        JsonNode detail = send(post("/api/admin/projects/" + project.get("id").asLong() + "/parts"), admin,
                Map.of("drawingNo", partNo, "name", "Plate", "quantity", 1), 201);
        return detail.get("parts").get(0).get("id").asLong();
    }

    @Test
    void employeesStartAndCompleteTheirOwnStagesInOrder() throws Exception {
        String admin = adminToken();
        long ravi = createEmployee(admin, "ravi_progress", "Ravi P");
        long santhosh = createEmployee(admin, "santhosh_progress", "Santhosh P");
        long partId = projectWithPart(admin, "VDE-0324-80-00-00", "VDE-0324-80-00-01");

        JsonNode detail = send(post("/api/admin/parts/" + partId + "/stages"), admin, Map.of("stages", List.of(
                Map.of("stageMasterId", stageMasterId(admin, "Conventional Milling"), "assigneeId", ravi),
                Map.of("stageMasterId", stageMasterId(admin, "VMC"), "assigneeId", santhosh))), 201);
        long millingId = detail.get("parts").get(0).get("stages").get(0).get("id").asLong();
        long vmcId = detail.get("parts").get(0).get("stages").get(1).get("id").asLong();
        String raviToken = login("ravi_progress", "Passw0rd!x");
        String santhoshToken = login("santhosh_progress", "Passw0rd!x");

        // Not allowed: starting while an earlier stage is open, someone else's stage, finishing before starting.
        send(post("/api/my-work/" + vmcId + "/start"), santhoshToken, null, 409);
        send(post("/api/my-work/" + vmcId + "/start"), raviToken, null, 403);
        send(post("/api/my-work/" + millingId + "/complete"), raviToken, null, 409);

        // Ravi starts, then finishes, milling.
        JsonNode started = send(post("/api/my-work/" + millingId + "/start"), raviToken, null, 200);
        assertEquals("IN_PROGRESS", started.get(0).get("status").asText());
        assertTrue(!started.get(0).get("startedAt").isNull());
        send(post("/api/my-work/" + millingId + "/start"), raviToken, null, 409);   // cannot start twice

        JsonNode finished = send(post("/api/my-work/" + millingId + "/complete"), raviToken, null, 200);
        assertEquals("DONE", finished.get(0).get("status").asText());
        assertTrue(!finished.get(0).get("completedAt").isNull());
        send(post("/api/my-work/" + millingId + "/complete"), raviToken, null, 409);   // cannot finish twice

        // Santhosh is no longer waiting and can start VMC.
        JsonNode santhoshsWork = send(get("/api/my-work"), santhoshToken, null, 200);
        assertEquals(0, santhoshsWork.get(0).get("waitingFor").size());
        JsonNode vmcStarted = send(post("/api/my-work/" + vmcId + "/start"), santhoshToken, null, 200);
        assertEquals("IN_PROGRESS", vmcStarted.get(0).get("status").asText());

        // The admin sees the progress.
        JsonNode stages = send(get("/api/admin/projects/" + detail.get("id").asLong()), admin, null, 200)
                .get("parts").get(0).get("stages");
        assertEquals("DONE", stages.get(0).get("status").asText());
        assertEquals("IN_PROGRESS", stages.get(1).get("status").asText());
    }

    @Test
    void stagesCanBeTypedInAsWellAsPickedFromTheList() throws Exception {
        String admin = adminToken();
        long partId = projectWithPart(admin, "VDE-0324-81-00-00", "VDE-0324-81-00-01");

        // The same custom name typed twice (different case and spacing) becomes one stage.
        JsonNode detail = send(post("/api/admin/parts/" + partId + "/stages"), admin, Map.of("stages", List.of(
                Map.of("customName", "Hand  Deburring"),
                Map.of("customName", " hand deburring "))), 201);
        JsonNode stages = detail.get("parts").get(0).get("stages");
        assertEquals("Hand Deburring", stages.get(0).get("stageName").asText());
        assertEquals(stages.get(0).get("stageMasterId").asLong(), stages.get(1).get("stageMasterId").asLong());

        // It is now in the list for next time, exactly once.
        int matches = 0;
        for (JsonNode n : send(get("/api/stage-master"), admin, null, 200)) {
            if ("Hand Deburring".equals(n.get("name").asText())) {
                matches++;
            }
        }
        assertEquals(1, matches);

        // Both, or neither, is rejected.
        send(post("/api/admin/parts/" + partId + "/stages"), admin, Map.of("stages", List.of(
                Map.of("stageMasterId", stageMasterId(admin, "VMC"), "customName", "Also custom"))), 400);
        send(post("/api/admin/parts/" + partId + "/stages"), admin, Map.of("stages", List.of(
                Map.of("dueDate", "2026-12-01"))), 400);
    }
}

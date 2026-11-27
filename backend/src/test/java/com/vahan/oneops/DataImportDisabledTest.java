package com.vahan.oneops;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Without the switch (the default, and production) the import URLs do not exist. */
@SpringBootTest
@AutoConfigureMockMvc
class DataImportDisabledTest {

    @Autowired
    MockMvc mvc;

    @Test
    void importEndpointsAreAbsentByDefault() throws Exception {
        // Not 200 for anyone: unauthenticated is rejected, and no controller is registered behind it.
        int code = mvc.perform(get("/api/admin/import/status")).andReturn().getResponse().getStatus();
        org.junit.jupiter.api.Assertions.assertNotEquals(200, code);
    }
}

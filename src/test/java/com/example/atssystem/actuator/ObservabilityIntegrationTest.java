package com.example.atssystem.actuator;

import com.example.atssystem.support.PostgresIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"management.endpoints.web.exposure.include=health,info,prometheus",
		"management.endpoint.health.show-components=always",
		"management.endpoint.health.group.readiness.include=readinessState,db,skillCatalog"
})
@AutoConfigureMockMvc
class ObservabilityIntegrationTest extends PostgresIntegrationTestSupport {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void readinessIncludesDatabaseAndPopulatedSkillCatalog() throws Exception {
		mockMvc.perform(get("/actuator/health/readiness"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.components.db.status").value("UP"))
				.andExpect(jsonPath("$.components.skillCatalog.status").value("UP"));
	}

	@Test
	void prometheusEndpointPublishesJvmMetrics() throws Exception {
		mockMvc.perform(get("/actuator/prometheus"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("jvm_memory_used_bytes")));
	}

	@Test
	void unexposedActuatorEndpointsStayUnavailable() throws Exception {
		mockMvc.perform(get("/actuator/env"))
				.andExpect(status().isNotFound());
	}
}

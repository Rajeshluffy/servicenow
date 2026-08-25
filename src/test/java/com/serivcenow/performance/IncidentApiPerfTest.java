package com.serivcenow.performance;

import org.testng.annotations.Test;

import com.framework.performance.ApiPerformanceUtils;
import com.framework.performance.PerformanceTestBase;
import com.framework.utils.TestMetadata;
import com.serivcenow.api.services.IncidentSerivce;
import com.servicenow.pojos.IncidentRequestPayload;

/**
 * API response-time and load tests for the Incident SOM
 * ({@link IncidentSerivce}), built on {@code com.framework.performance}
 * (autoframex-performance module).
 *
 * <p>Separate from the project's existing Cucumber/step-defs flow
 * (step.defs.som.*) — {@link PerformanceTestBase} is a plain TestNG base,
 * run via its own suite ({@code performance-suite.xml}), not the Cucumber
 * runner. SLA breaches log as warnings, not failures — see
 * {@link PerformanceTestBase#assertSla}.
 */
@TestMetadata(
		name        = "Incident API Performance",
		description = "Response-time SLA and concurrent load checks for the Incident table API",
		authors     = "autoFrameX",
		category    = "performance"
)
public class IncidentApiPerfTest extends PerformanceTestBase {

	private final IncidentSerivce incidentService = new IncidentSerivce();

	@Test(groups = "performance")
	public void fetchIncidentRecordsRespondsWithinSla() {
		long ms = measureApi("fetchIncidentRecords",
				() -> incidentService.fetchIncidentRecords().getResponse(),
				2_000);
		reportStep("fetchIncidentRecords: " + ms + " ms", "info", false);
	}

	@Test(groups = "performance")
	public void createIncidentRespondsWithinSla() {
		IncidentRequestPayload payload = samplePayload();
		long ms = measureApi("createIncidentRecord",
				() -> incidentService.createIncidentRecord(payload).getResponse(),
				2_000);
		reportStep("createIncidentRecord: " + ms + " ms", "info", false);
	}

	@Test(groups = "performance")
	public void fetchIncidentRecordsUnderConcurrentLoad() {
		ApiPerformanceUtils.LoadTestResult result =
				runLoadTest(() -> incidentService.fetchIncidentRecords().getResponse(), 10, 30);
		reportStep(result.toString(), "info", false);
		assertSla(result.getP95Ms(), 3_000, "P95 fetchIncidentRecords under load");
	}

	private static IncidentRequestPayload samplePayload() {
		IncidentRequestPayload payload = new IncidentRequestPayload();
		payload.setShort_description("autoFrameX performance test incident");
		payload.setDescription("Created by IncidentApiPerfTest — safe to close/delete.");
		payload.setCategory("software");
		return payload;
	}
}

package step.defs.som;

import io.cucumber.java.After;
import io.cucumber.java.en.Then;

/**
 * Cucumber {@code @Then} step definitions for Incident Service response
 * assertions, plus the {@code @After} scenario-cleanup hook.
 *
 * <p>This class <em>reads</em> the response that was captured by
 * {@link IncidentRequestSteps} and asserts correctness.  It never calls a
 * request method directly — all service calls are routed through the shared
 * {@link IncidentScenarioContext}.
 *
 * <h3>Cleanup contract</h3>
 * <p>The {@link #cleanUp()} hook deletes the incident created during the
 * scenario (if {@code ctx.createdSysId} is non-null).  Steps that exercise
 * the delete operation must set {@code ctx.createdSysId = null} immediately
 * after calling {@code deleteIncidentRecord()} so this hook does not attempt
 * a second delete on a record that no longer exists (which would return 404
 * and print a misleading error after an otherwise-passing test).
 *
 * <h3>PicoContainer injection</h3>
 * <p>PicoContainer creates exactly one {@link IncidentScenarioContext} per
 * scenario and injects the <em>same</em> instance into both this class and
 * {@link IncidentRequestSteps}.  No static state, no cross-scenario leakage.
 *
 * @see IncidentRequestSteps
 * @see IncidentScenarioContext
 */
public class IncidentResponseSteps {

    private final IncidentScenarioContext ctx;

    /** PicoContainer calls this constructor and injects the shared context. */
    public IncidentResponseSteps(IncidentScenarioContext ctx) {
        this.ctx = ctx;
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    /**
     * Deletes the incident created during the scenario so the ServiceNow
     * developer instance is not cluttered with test data.
     *
     * <p>Guarded by a null-check: if the create step never ran (or a delete
     * step already cleaned up), this is a no-op.
     */
    @After
    public void cleanUp() {
        if (ctx.createdSysId != null) {
            ctx.incidentService.deleteIncidentRecord(ctx.createdSysId);
            ctx.createdSysId = null;
        }
    }

    // ── Create response assertions ────────────────────────────────────────────

    /**
     * Validates the HTTP 201 Created response and extracts {@code sys_id} and
     * incident number into context for use by later steps.
     *
     * <p>Extraction happens here — <em>after</em> status validation — to avoid
     * {@code JsonPathException} when ServiceNow returns HTML (hibernated instance,
     * auth failure, wrong URL) instead of the expected JSON body.
     */
    @Then("the incident should be created successfully")
    public void incidentCreatedSuccessfully() {
        // Throws AssertionError with a body preview when status != 201.
        ctx.incidentService.validateCreationResponse();
        // Only reached on a confirmed 201 JSON response.
        ctx.createdSysId            = ctx.incidentService.getLastCreatedSysId();
        ctx.createdIncidentNumber   = ctx.incidentService.getLastCreatedNumber();
    }

    /**
     * Validates the creation response body against the JSON Schema stored at
     * {@code schemas/incident_create_response_schema.json}.
     */
    @Then("the response should match the incident schema")
    public void responseMatchesIncidentSchema() {
        ctx.incidentService.validateCreationResponseSchema();
    }

    // ── Fetch response assertions ─────────────────────────────────────────────

    /**
     * Verifies that the incident fetched by number has the same incident number
     * that was returned during creation — confirming end-to-end round-trip
     * persistence.
     */
    @Then("the fetched incident number should match the created incident")
    public void fetchedIncidentNumberMatches() {
        ctx.incidentService
                .validateSuccessResponse()
                .validateIncidentNumber(ctx.createdIncidentNumber);
    }

    // ── Update response assertion ─────────────────────────────────────────────

    /**
     * Verifies the HTTP 200 OK response to a PUT update request.
     */
    @Then("the incident update should succeed")
    public void incidentUpdateSucceeds() {
        ctx.incidentService.validateSuccessResponse();
    }

    // ── Delete response assertion ─────────────────────────────────────────────

    /**
     * Verifies the HTTP 204 No Content response to a DELETE request.
     * Per RFC 7231 §6.3.5, 204 carries no body — only the status code is checked.
     */
    @Then("the incident deletion should return no content")
    public void incidentDeletionReturnsNoContent() {
        ctx.incidentService.validateDeletionResponse();
    }
}

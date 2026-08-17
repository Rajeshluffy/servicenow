package step.defs.som;

import com.servicenow.pojos.IncidentRequestPayload;

import io.cucumber.java.en.When;

/**
 * Cucumber {@code @When} step definitions for Incident Service mutations.
 *
 * <p>This class owns every step that <em>sends</em> a request to the ServiceNow
 * Incident API (create, fetch, update, delete).  Pure side-effects are written
 * into {@link IncidentScenarioContext} so that the paired
 * {@link IncidentResponseSteps} can validate the resulting response without
 * coupling the two classes together.
 *
 * <h3>State flow</h3>
 * <pre>
 *  createIncident()       → incidentService.createIncidentRecord(payload)
 *  fetchByNumber()        → incidentService.fetchIncidentRecordByNumber(ctx.createdIncidentNumber)
 *  updateShortDesc()      → incidentService.updateIncidentRecord(payload, ctx.createdSysId)
 *  deleteCreatedIncident()→ incidentService.deleteIncidentRecord(ctx.createdSysId)
 *                           ctx.createdSysId = null   ← prevents @After double-delete
 * </pre>
 *
 * <h3>PicoContainer injection</h3>
 * <p>PicoContainer creates exactly one {@link IncidentScenarioContext} per
 * scenario and injects the <em>same</em> instance into both this class and
 * {@link IncidentResponseSteps} — no static state, no shared mutable fields.
 *
 * @see IncidentResponseSteps
 * @see IncidentScenarioContext
 */
public class IncidentRequestSteps {

    private final IncidentScenarioContext ctx;

    /** PicoContainer calls this constructor and injects the shared context. */
    public IncidentRequestSteps(IncidentScenarioContext ctx) {
        this.ctx = ctx;
    }

    // ── Create ───────────────────────────────────────────────────────────────

    /**
     * Sends a POST to create a new incident.  The response is intentionally
     * <em>not</em> validated here — {@link IncidentResponseSteps#incidentCreatedSuccessfully()}
     * validates the status code and extracts {@code sys_id} / number into context
     * only after confirming a 201.  Extracting before validation caused
     * {@code JsonPathException} when ServiceNow returned HTML (hibernated instance,
     * auth failure).
     */
    @When("I create an incident with description {string} and short description {string}")
    public void createIncident(String description, String shortDescription) {
        IncidentRequestPayload payload = new IncidentRequestPayload();
        payload.setDescription(description);
        payload.setShort_description(shortDescription);
        ctx.incidentService.createIncidentRecord(payload);
    }

    // ── Fetch ────────────────────────────────────────────────────────────────

    /**
     * Fetches the incident by its human-readable number (e.g. {@code INC0001234}).
     *
     * <p>Precondition: {@code ctx.createdIncidentNumber} must already be populated
     * by a prior {@code "the incident should be created successfully"} step.
     */
    @When("I fetch the created incident by its number")
    public void fetchByNumber() {
        ctx.incidentService.fetchIncidentRecordByNumber(ctx.createdIncidentNumber);
    }

    // ── Update ───────────────────────────────────────────────────────────────

    /**
     * Sends a PUT to update the {@code short_description} of the previously
     * created incident.
     *
     * <p>Precondition: {@code ctx.createdSysId} must already be populated by a
     * prior {@code "the incident should be created successfully"} step.
     */
    @When("I update the incident short description to {string}")
    public void updateShortDescription(String newShortDescription) {
        IncidentRequestPayload patch = new IncidentRequestPayload();
        patch.setShort_description(newShortDescription);
        ctx.incidentService.updateIncidentRecord(patch, ctx.createdSysId);
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    /**
     * Sends a DELETE for the previously created incident and clears
     * {@code ctx.createdSysId} so the {@code @After} cleanup hook in
     * {@link IncidentResponseSteps} does not attempt a second delete on a
     * record that no longer exists.
     *
     * <p>Precondition: {@code ctx.createdSysId} must already be populated by a
     * prior {@code "the incident should be created successfully"} step.
     */
    @When("I delete the created incident")
    public void deleteCreatedIncident() {
        ctx.incidentService.deleteIncidentRecord(ctx.createdSysId);
        ctx.createdSysId = null; // prevent @After double-delete
    }
}

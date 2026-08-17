package step.defs.som;

import com.serivcenow.api.services.IncidentSerivce;

/**
 * Shared scenario context injected via Cucumber PicoContainer.
 *
 * <p>PicoContainer creates exactly <strong>one instance per scenario</strong> and
 * injects that same instance into every step definition class that declares it
 * as a constructor parameter — regardless of how many step classes exist.
 *
 * <h3>Multi-class sharing (key benefit)</h3>
 * <pre>
 * // IncidentRequestSteps  — @When steps (mutations)
 * public class IncidentRequestSteps {
 *     private final IncidentScenarioContext ctx;
 *     public IncidentRequestSteps(IncidentScenarioContext ctx) { this.ctx = ctx; }
 * }
 *
 * // IncidentResponseSteps — @Then steps + @After cleanup
 * public class IncidentResponseSteps {
 *     private final IncidentScenarioContext ctx;
 *     public IncidentResponseSteps(IncidentScenarioContext ctx) { this.ctx = ctx; }
 *     // ctx IS the same object that IncidentRequestSteps received
 * }
 * </pre>
 *
 * <h3>Thread safety</h3>
 * <p>PicoContainer creates a fresh context per scenario, so parallel scenario
 * execution (different threads) never shares state through this object.
 * All fields are written by one step and read by a later step in the same
 * scenario — there is no cross-scenario contention.
 *
 * @author Framework Team
 * @version 2.0
 */
public class IncidentScenarioContext {

    /**
     * Service under test — shared between all step classes in a scenario.
     * PicoContainer calls the no-arg constructor automatically.
     */
    public final IncidentSerivce incidentService = new IncidentSerivce();

    /**
     * {@code sys_id} of the incident created during the current scenario.
     * Written by {@link IncidentRequestSteps} create-step;
     * read by {@link IncidentResponseSteps} and @After cleanup.
     */
    public String createdSysId;

    /**
     * Human-readable incident number (e.g. {@code INC0001234}) returned by the
     * create call.  Written by the create-step once a 201 is confirmed;
     * consumed by fetch-by-number steps to verify round-trip behaviour.
     */
    public String createdIncidentNumber;
}

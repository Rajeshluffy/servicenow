package step.defs.som;

/**
 * @deprecated This class has been split into two focused step-definition classes.
 *
 * <ul>
 *   <li>{@link IncidentRequestSteps} — all {@code @When} mutation steps
 *       (create, fetch, update, delete).</li>
 *   <li>{@link IncidentResponseSteps} — all {@code @Then} assertion steps
 *       and the {@code @After} cleanup hook.</li>
 * </ul>
 *
 * <p>Both classes share state through {@link IncidentScenarioContext}, which
 * PicoContainer creates once per scenario and injects into every step class
 * that declares it as a constructor parameter.
 *
 * <p>This file is intentionally empty (no step annotations) so that Cucumber
 * does not register duplicate step definitions while the file remains in source
 * control for history purposes.  It may be deleted once all team members have
 * pulled the split classes.
 */
@Deprecated
public class IncidentServiceSteps {
    // Intentionally empty — see Javadoc above.
}

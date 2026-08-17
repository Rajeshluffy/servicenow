package runner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

@CucumberOptions(
        features = {"src/test/resources/features/IncidentService.feature"},
        glue     = {"step.defs.som"},
        dryRun   = false,
        plugin   = {
            "pretty",
            // Human-readable HTML report — published in Jenkins via publishHTML
            "html:reports/cucumber-result.html",
            // JUnit XML — consumed by the Jenkins 'junit' publisher; gives
            // one test-result row per scenario instead of one row for the whole runner
            "junit:reports/cucumber-junit.xml",
            // JSON — enables the Masterthought 'Cucumber Reports' Jenkins plugin
            // if installed; also useful for downstream report processing
            "json:reports/cucumber-result.json"
        }
)
public class TestNGRunner extends AbstractTestNGCucumberTests {

}

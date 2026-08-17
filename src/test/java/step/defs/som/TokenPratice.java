package step.defs.som;

import org.testng.annotations.Test;

import configManager.ConfigurationManager;
import com.api.rest.assured.base.RequestSpecBuilder;

public class TokenPratice {

	RequestSpecBuilder request = new RequestSpecBuilder();

	@Test
	public void oAuthPratices() {
		request.setBaseUri(ConfigurationManager.configuration().getBaseUri());
		request.setOauthBasePath(ConfigurationManager.configuration().getOAuthBasePath());
		request.setOAuth(
				ConfigurationManager.configuration().getBaseUri(),
				ConfigurationManager.configuration().getOAuthBasePath(),
				ConfigurationManager.configuration().getGrantType(),
				ConfigurationManager.configuration().getClientId(),
				ConfigurationManager.configuration().getClientSecret(),
				ConfigurationManager.configuration().getUsername(),
				ConfigurationManager.configuration().getPassword());
		request.build();
	}
}

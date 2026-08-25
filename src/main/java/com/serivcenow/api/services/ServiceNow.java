package com.serivcenow.api.services;

import io.restassured.http.ContentType;

import com.api.design.ResponseAPI;
import com.api.rest.assured.base.RequestSpecBuilder;
import com.api.rest.assured.base.RestAssuredBase;

import configManager.ConfigurationManager;

public class ServiceNow {

	protected ResponseAPI response;
	protected RestAssuredBase restAssured = new RestAssuredBase();
	protected RequestSpecBuilder requestBuilder;

	/**
	 * Exposes the response from the most recent call for callers outside this
	 * class hierarchy — e.g. {@code com.framework.performance.ApiPerformanceUtils}'s
	 * {@code Supplier<ResponseAPI>} contract, which the fluent {@code IncidentSerivce}
	 * methods (returning {@code this}) can't satisfy directly.
	 */
	public ResponseAPI getResponse() {
		return response;
	}

	public String serviceNowBaseUri() {
		return ConfigurationManager.configuration().getBaseUri();
	}

	public String serviceNowBasePath() {
		return ConfigurationManager.configuration().getBasePath();
	}

	public String serviceNowUsername() {
		return ConfigurationManager.configuration().getUsername();
	}

	public String serviceNowPassword() {
		return ConfigurationManager.configuration().getPassword();
	}

	protected RequestSpecBuilder globalRequest() {
		return new RequestSpecBuilder()
				.setBaseUri(ConfigurationManager.configuration().getBaseUri())
				.setBasePath(ConfigurationManager.configuration().getBasePath())
				// ServiceNow enforces glide.authenticate.basic_auth.restriction, which
				// blocks Basic Auth for accounts without the snc_basic_auth_api_access
				// role — OAuth client_credentials is the supported path instead.
				.setOAuth(ConfigurationManager.configuration().getBaseUri(),
						  ConfigurationManager.configuration().getOAuthBasePath(),
						  ConfigurationManager.configuration().getGrantType(),
						  ConfigurationManager.configuration().getClientId(),
						  ConfigurationManager.configuration().getClientSecret(),
						  ConfigurationManager.configuration().getUsername(),
						  ConfigurationManager.configuration().getPassword())
				.setAccept(ContentType.JSON);
	}

}

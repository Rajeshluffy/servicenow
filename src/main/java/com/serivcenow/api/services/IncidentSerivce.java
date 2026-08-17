package com.serivcenow.api.services;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;

import com.api.rest.assured.base.ResponseContentType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.servicenow.pojos.IncidentRequestPayload;

import io.restassured.http.ContentType;

public class IncidentSerivce extends ServiceNow {

    private static final String TABLE_NAME = "incident";
    private static final String CREATE_SCHEMA = "schemas/incident_create_response_schema.json";

    public IncidentSerivce() {
        requestBuilder = globalRequest();
    }

    // ── Queries ──────────────────────────────────────────────────────────────

    public IncidentSerivce fetchIncidentRecords() {
        response = restAssured.get(requestBuilder.build(), TABLE_NAME);
        return this;
    }

    public IncidentSerivce fetchIncidentRecord(String sysId) {
        response = restAssured.get(requestBuilder.build(), TABLE_NAME + "/" + sysId);
        return this;
    }

    public IncidentSerivce fetchIncidentRecordByNumber(String number) {
        response = restAssured.get(
                requestBuilder
                        .setQueryParamKey("sysparm_query")
                        .setQueryParamValue("number=" + number)
                        .build(),
                TABLE_NAME);
        return this;
    }

    public IncidentSerivce fetchOnlyHardwareCategoryIncidentRecords() {
        response = restAssured.get(
                requestBuilder.build().queryParam("sysparm_query", "category=hardware"),
                TABLE_NAME);
        return this;
    }

    // ── Mutations ────────────────────────────────────────────────────────────

    public IncidentSerivce createIncidentRecord() {
        response = restAssured.post(requestBuilder.setContentType(ContentType.JSON).build(), TABLE_NAME);
        return this;
    }

    public IncidentSerivce createIncidentRecord(IncidentRequestPayload payload) {
        response = restAssured.post(requestBuilder.setContentType(ContentType.JSON).build(), TABLE_NAME, payload);
        return this;
    }

    public IncidentSerivce updateIncidentRecord(IncidentRequestPayload payload, String sysId) {
        response = restAssured.put(
                requestBuilder.setContentType(ContentType.JSON).build(),
                TABLE_NAME + "/" + sysId,
                payload);
        return this;
    }

    public IncidentSerivce deleteIncidentRecord(String sysId) {
        response = restAssured.delete(requestBuilder.build(), TABLE_NAME + "/" + sysId);
        return this;
    }

    // ── Response accessors ───────────────────────────────────────────────────

    /** Returns the sys_id of the most recently created incident (from a 201 response). */
    public String getLastCreatedSysId() {
        return response.getJsonPath("result.sys_id");
    }

    /**
     * Returns the human-readable number (e.g. {@code INC0001234}) of the most
     * recently created incident.  Used by Cucumber steps that later fetch the
     * record by number to verify end-to-end round-trip behaviour.
     */
    public String getLastCreatedNumber() {
        return response.getJsonPath("result.number");
    }

    // ── Assertions ───────────────────────────────────────────────────────────

    public IncidentSerivce validateSuccessResponse() {
        MatcherAssert.assertThat(response.getStatusCode(), Matchers.equalTo(200));
        MatcherAssert.assertThat(response.getStatusMessage(), Matchers.equalToIgnoringCase("OK"));
        MatcherAssert.assertThat(response.getContentType(), Matchers.equalTo(ResponseContentType.JSON));
        return this;
    }

    public IncidentSerivce validateCreationResponse() {
        int status = response.getStatusCode();

        // Fail fast with a body-inclusive message so the developer can immediately
        // distinguish: hibernated instance (HTML 200), bad auth (HTML 401/200),
        // wrong URL (HTML 404), or an unexpected JSON error from the API.
        if (status != 201) {
            String ct      = response.getContentType();
            String body    = response.getBody();
            String preview = body.length() > 500 ? body.substring(0, 500) + "…" : body;
            boolean isHtml = !ct.toLowerCase().contains("json");

            String hint = isHtml
                    ? "\n  ► Response is HTML, not JSON. Most likely cause:"
                    + "\n    • ServiceNow instance is hibernating — wake it at"
                    + "\n      https://developer.servicenow.com → My Instance → Wake"
                    + "\n    • Or credentials are wrong / instance URL has changed."
                    : "";

            throw new AssertionError(
                    "Expected HTTP 201 Created but got " + status
                    + " " + response.getStatusMessage() + "."
                    + "\n  Content-Type : " + (ct.isEmpty() ? "(none)" : ct)
                    + hint
                    + "\n  Body preview :\n" + preview);
        }

        MatcherAssert.assertThat(response.getStatusMessage(), Matchers.equalToIgnoringCase("Created"));
        MatcherAssert.assertThat(response.getContentType(), Matchers.equalTo(ResponseContentType.JSON));
        return this;
    }

    public IncidentSerivce validateCreationResponseSchema() {
        response.verifySchema(CREATE_SCHEMA);
        return this;
    }

    public IncidentSerivce validateDeletionResponse() {
        MatcherAssert.assertThat(response.getStatusCode(), Matchers.equalTo(204));
        // 204 No Content carries no body or content-type — do not assert either
        return this;
    }

    public IncidentSerivce validateNotFoundResponse() {
        MatcherAssert.assertThat(response.getStatusCode(), Matchers.equalTo(404));
        MatcherAssert.assertThat(response.getStatusMessage(), Matchers.equalToIgnoringCase("Not Found"));
        MatcherAssert.assertThat(response.getContentType(), Matchers.equalTo(ResponseContentType.JSON));
        return this;
    }

    public IncidentSerivce validateCategories(String expected) {
        try {
            JsonNode root = new ObjectMapper().readTree(response.getBody());
            root.get("result").forEach(record ->
                    MatcherAssert.assertThat(
                            record.get("category").asText(),
                            Matchers.equalToIgnoringCase(expected)));
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse response for category validation", e);
        }
        return this;
    }

    public IncidentSerivce validateSysId(String expected) {
        MatcherAssert.assertThat(response.getJsonPath("result.sys_id"), Matchers.equalTo(expected));
        return this;
    }

    public IncidentSerivce validateIncidentNumber(String expected) {
        // "result[0].number" for list responses; "result.number" for single-record responses
        String actual = response.getJsonPath("result[0].number");
        MatcherAssert.assertThat(actual, Matchers.equalTo(expected));
        return this;
    }
}

package com.automation.utils;

import io.restassured.response.Response;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Assertions for backend-api error responses: { statusCode, message }.
 * statusCode is "101" for a thrown ApiError without a custom code (validation
 * 400s, staff-gate 403s — the default in backend-api's ApiError.ts), "10001"
 * (FAILURE) when a response class is sent directly, and "10003" for an
 * invalid access token. The events endpoints use a different, Go-style
 * envelope and do not go through here.
 */
public final class ApiErrorAssertions {

    public static final String THROWN_DEFAULT = "101";
    public static final String FAILURE = "10001";
    public static final String INVALID_ACCESS_TOKEN = "10003";

    private ApiErrorAssertions() {
        // Prevent instantiation
    }

    /** Asserts the HTTP status and that the body is a backend-api error envelope. */
    public static void assertApiError(Response response, int httpStatus) {
        assertThat("HTTP status", response.getStatusCode(), equalTo(httpStatus));
        assertThat("statusCode", response.jsonPath().getString("statusCode"),
                anyOf(equalTo(THROWN_DEFAULT), equalTo(FAILURE), equalTo(INVALID_ACCESS_TOKEN)));
        assertThat("message present", response.jsonPath().getString("message"), not(emptyOrNullString()));
    }

    /** As above, and the message contains the given fragment (case-insensitive). */
    public static void assertApiError(Response response, int httpStatus, String messageFragment) {
        assertApiError(response, httpStatus);
        assertThat("message", response.jsonPath().getString("message"),
                containsStringIgnoringCase(messageFragment));
    }
}

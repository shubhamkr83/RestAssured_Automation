package com.automation.tests;

import com.automation.base.BaseTest;
import com.automation.constants.Endpoints;
import com.automation.constants.HttpStatus;
import io.qameta.allure.*;
import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Test class demonstrating JSON Schema validation.
 * Validates API response structure against predefined schemas.
 */
@Epic("API Validation")
@Feature("Schema Validation")
public class SchemaValidationTest extends BaseTest {

    @Test(description = "Validate User response against JSON schema")
    @Story("User Schema")
    @Severity(SeverityLevel.CRITICAL)
    public void testUserResponseSchema() {
        Response response = restClient.get(Endpoints.USER_BY_ID, Map.of("id", 1));

        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        // Validate against JSON schema
        response.then()
                .body(JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/user-schema.json"));
    }

    @Test(description = "Validate Post response against JSON schema")
    @Story("Post Schema")
    @Severity(SeverityLevel.CRITICAL)
    public void testPostResponseSchema() {
        Response response = restClient.get(Endpoints.POST_BY_ID, Map.of("id", 1));

        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        // Validate against JSON schema
        response.then()
                .body(JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/post-schema.json"));
    }

    @Test(description = "Validate Users array response against JSON schema")
    @Story("User Schema")
    @Severity(SeverityLevel.NORMAL)
    public void testUsersArraySchema() {
        Response response = restClient.get(Endpoints.USERS);

        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        // Validate against JSON schema
        response.then()
                .body(JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/users-array-schema.json"));
    }
}

package com.automation.tests.bomb.CatalogTagPipeline.CatalogEditor;

import com.automation.base.BaseTest;
import com.automation.constants.HttpStatus;
import com.automation.constants.BombEndpoints;
import com.automation.models.response.MarkAsDoneResponse;
import com.automation.utils.VariableManager;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for BOMB Catalog Editor - Mark As Done endpoint.
 * Endpoint: PUT
 * https://api.bizup.app/v1/admin/editor/assign/videos/done/{{seller_id}}/{{catalog_id}}
 * Implements comprehensive Postman test scripts for marking catalog as done
 * validation.
 */
@Epic("BOMB Catalog Tag Pipeline")
@Feature("Catalog Editor")
public class MarkAsDoneTest extends BaseTest {

    private String authToken;
    private Response response;
    private MarkAsDoneResponse markAsDoneResponse;

    @BeforeClass
    public void setupAuth() {
        // Get token from VariableManager (thread-safe)
        authToken = VariableManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
        logger.info("Using BOMB token from VariableManager");
    }

    @Test(description = "Response status code is 200", priority = 1, groups = "bomb")
    @Story("Catalog Editor - Mark As Done")
    @Severity(SeverityLevel.BLOCKER)
    public void testStatusCode200() {
        // Send PUT request to mark catalog as done
        response = RestAssured.given()
                .header("authorization", "JWT " + authToken)
                .header("Content-Type", "application/json")
                .when()
                .put(BombEndpoints.EDITOR_MARK_AS_DONE + "/" + VariableManager.get("seller_id") + "/" + VariableManager.get("catalog_id", "682584c0240b174c4c1a55f4"));

        // Parse response for other tests
        markAsDoneResponse = JsonUtils.fromResponse(response, MarkAsDoneResponse.class);

        // Verify response status is 200 OK
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response time is less than threshold", priority = 2, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Mark As Done")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTimeLessThanThreshold() {
        // Use specific threshold of 20000ms as per Postman script
        long threshold = 20000;
        long actualResponseTime = response.getTime();

        // Verify response time is available
        assertThat("Response time should be available", actualResponseTime, notNullValue());

        // Verify response time is below threshold
        assertThat("Response time should be less than threshold",
                actualResponseTime, lessThan(threshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime, threshold);
    }
}

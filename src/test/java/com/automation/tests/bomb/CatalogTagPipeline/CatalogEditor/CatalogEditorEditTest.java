package com.automation.tests.bomb.CatalogTagPipeline.CatalogEditor;

import com.automation.base.BaseTest;
import com.automation.constants.BombEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.CatalogEditGroupResponse;
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
 * Test class for BOMB Catalog Editor - Catalog Edit endpoint.
 * Endpoint:
 * {{bizup_base}}/v1/admin/catalog/group/upload/{{catalog_foassign_id}}?limit=20&mode=edit
 * Implements comprehensive Postman test scripts for catalog edit mode
 * validation.
 */
@Epic("BOMB Catalog Tag Pipeline")
@Feature("Catalog Editor")
public class CatalogEditorEditTest extends BaseTest {

    private String authToken;
    private Response response;
    private CatalogEditGroupResponse catalogEditGroupResponse;

    // Catalog ID from previous tests
    private String catalogForAssignId;

    @BeforeClass
    public void setupAuth() {
        // Get token from VariableManager (thread-safe)
        authToken = VariableManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
        logger.info("Using BOMB token from VariableManager");

        // Get catalog ID from VariableManager or use fallback
        catalogForAssignId = VariableManager.get("catalog_foassign_id");
        if (catalogForAssignId == null || catalogForAssignId.isEmpty()) {
            catalogForAssignId = VariableManager.get("catalog_foassign_id", "6822f5dac17c6dcd589ba173");
            logger.warn("Catalog for assign ID not available, using default: {}", catalogForAssignId);
        } else {
            logger.info("Using catalog for assign ID from VariableManager: {}", catalogForAssignId);
        }
    }

    @Test(description = "Response status code is 200", priority = 1, groups = "bomb")
    @Story("Catalog Editor - Catalog Edit")
    @Severity(SeverityLevel.BLOCKER)
    public void testStatusCode200() {
        // Send GET request to fetch catalog edit data
        response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .queryParam("limit", 20)
                .queryParam("mode", "edit")
                .when()
                .get(BombEndpoints.CATALOG_GROUP_UPLOAD + "/" + catalogForAssignId);

        // Parse response for other tests
        catalogEditGroupResponse = JsonUtils.fromResponse(response, CatalogEditGroupResponse.class);

        // Verify response status is 200 OK
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response time is less than threshold", priority = 2, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Edit")
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

    @Test(description = "Presence of _id, name, total, data, and error properties", priority = 3, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Edit")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseBodyStructure() {
        // Validate response is an object
        assertThat("Response should not be null", catalogEditGroupResponse, notNullValue());

        // Validate data object has all required keys
        CatalogEditGroupResponse.CatalogEditGroupData data = catalogEditGroupResponse.getData();
        assertThat("Data should not be null", data, notNullValue());
        assertThat("Data should have _id", data.get_id(), notNullValue());
        assertThat("Data should have name", data.getName(), notNullValue());
        assertThat("Data should have total", data.getTotal(), notNullValue());
        assertThat("Data should have data array", data.getData(), notNullValue());
        // Note: error field can be null or present

        logger.info("Response body structure validated: _id={}, name={}, total={}, data size={}, error={}",
                data.get_id(), data.getName(), data.getTotal(),
                data.getData() != null ? data.getData().size() : 0, data.getError());
    }
}

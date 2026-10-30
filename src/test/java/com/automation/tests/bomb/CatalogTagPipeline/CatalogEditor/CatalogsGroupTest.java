package com.automation.tests.bomb.CatalogTagPipeline.CatalogEditor;

import com.automation.base.BaseTest;
import com.automation.constants.BombEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.CatalogGroupResponse;
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
 * Test class for BOMB Catalog Editor - Catalogs Group endpoint.
 * Endpoint:
 * {{bizup_base}}/v1/admin/catalog/upload/{{catalog_foassign_id}}?limit=20&mode=all
 * Implements comprehensive Postman test scripts for catalog group validation.
 */
@Epic("BOMB Catalog Tag Pipeline")
@Feature("Catalog Editor")
public class CatalogsGroupTest extends BaseTest {

    private String authToken;
    private Response response;
    private CatalogGroupResponse catalogGroupResponse;

    // Catalog ID and Seller ID
    private String catalogForAssignId;
    private String sellerId;

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
            catalogForAssignId = "6822f5dac17c6dcd589ba173"; // Fallback to a default ID
            logger.warn("Catalog for assign ID not available, using default: {}", catalogForAssignId);
        } else {
            logger.info("Using catalog for assign ID from VariableManager: {}", catalogForAssignId);
        }

        // Get seller ID from VariableManager or use fallback
        sellerId = VariableManager.get("seller_id");
        if (sellerId == null || sellerId.isEmpty()) {
            sellerId = "609341142100000000000000"; // Fallback to a default seller ID
            logger.warn("Seller ID not available, using default: {}", sellerId);
        } else {
            logger.info("Using seller ID from VariableManager: {}", sellerId);
        }
    }

    @Test(description = "Response status code is 200", priority = 1, groups = "bomb")
    @Story("Catalog Editor - Catalogs Group")
    @Severity(SeverityLevel.BLOCKER)
    public void testStatusCode200() {
        // Send GET request to fetch catalog group
        response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .queryParam("limit", 20)
                .queryParam("seller_id", sellerId)
                .queryParam("mode", "all")
                .when()
                .get(BombEndpoints.CATALOG_UPLOAD + "/" + catalogForAssignId);

        // Parse response for other tests
        catalogGroupResponse = JsonUtils.fromResponse(response, CatalogGroupResponse.class);

        // Verify response status is 200 OK
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response time is less than threshold", priority = 2, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalogs Group")
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

    @Test(description = "Data array contains at least one element", priority = 5, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalogs Group")
    @Severity(SeverityLevel.NORMAL)
    public void testDataArrayNotEmpty() {
        // Validate data array is not empty
        assertThat("Data array should not be empty",
                catalogGroupResponse.getData().getData(), not(empty()));

        logger.info("Data array validated: contains {} element(s)",
                catalogGroupResponse.getData().getData().size());
    }

}

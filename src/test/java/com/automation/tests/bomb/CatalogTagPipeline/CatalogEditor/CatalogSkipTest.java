package com.automation.tests.bomb.CatalogTagPipeline.CatalogEditor;

import com.automation.base.BaseTest;
import com.automation.constants.BombEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.CatalogSkipResponse;
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
 * Test class for BOMB Catalog Editor - Catalog Skip endpoint.
 * Endpoint: PUT
 * {{bizup_base}}/v1/admin/editor/assign/videos/skip/{{seller_id}}/{{catalog_foassign_id}}
 * Implements comprehensive Postman test scripts for catalog skip validation.
 */
@Epic("BOMB Catalog Tag Pipeline")
@Feature("Catalog Editor")
public class CatalogSkipTest extends BaseTest {

    private String authToken;
    private Response response;
    private CatalogSkipResponse catalogSkipResponse;

    // IDs from previous tests
    private String sellerId;
    private String catalogForAssignId;

    @BeforeClass
    public void setupAuth() {
        // Get token from VariableManager (thread-safe)
        authToken = VariableManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
        logger.info("Using BOMB token from VariableManager");
        
        // Get seller ID after VariableManager is initialized
        sellerId = VariableManager.getSellerId();
        logger.info("Using seller ID: {}", sellerId);
        
        // Get catalog ID from VariableManager or use fallback
        catalogForAssignId = VariableManager.get("catalog_foassign_id");
        if (catalogForAssignId == null || catalogForAssignId.isEmpty()) {
            catalogForAssignId = VariableManager.get("catalog_foassign_id", "6822f5dac17c6dcd589ba173");
            logger.warn("Catalog for assign ID not available from VariableManager, using default: {}", catalogForAssignId);
        } else {
            logger.info("Using catalog for assign ID from VariableManager: {}", catalogForAssignId);
        }
    }

    @Test(description = "Response status code is 200", priority = 1, groups = "bomb")
    @Story("Catalog Editor - Catalog Skip")
    @Severity(SeverityLevel.BLOCKER)
    public void testStatusCode200() {
        // Send PUT request to skip catalog
        response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .when()
                .post(BombEndpoints.EDITOR_SKIP_CATALOG + "/" + VariableManager.get("seller_id") + "/" + catalogForAssignId);

        // Parse response for other tests
        catalogSkipResponse = JsonUtils.fromResponse(response, CatalogSkipResponse.class);

        // Verify response status is 200 OK
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response time is less than threshold", priority = 2, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Skip")
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

    @Test(description = "Response has statusCode as string and message is 'success'", priority = 3, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Skip")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseMessage() {
        // Validate statusCode is a string
        assertThat("StatusCode should be a string",
                catalogSkipResponse.getStatusCode(), instanceOf(String.class));

        // Validate message is 'success'
        assertThat("Message should be 'success'",
                catalogSkipResponse.getMessage(), equalTo("success"));

        logger.info("Response message validated: statusCode={}, message={}",
                catalogSkipResponse.getStatusCode(), catalogSkipResponse.getMessage());
    }

    @Test(description = "Response data contains required properties with correct types", priority = 4, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Skip")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseDataSchema() {
        // Validate response is an object
        assertThat("Response should not be null", catalogSkipResponse, notNullValue());

        CatalogSkipResponse.CatalogSkipData data = catalogSkipResponse.getData();
        assertThat("Data should not be null", data, notNullValue());

        // Validate _id property
        assertThat("Data should have _id", data.get_id(), notNullValue());

        // Validate assignedBy property
        assertThat("Data should have assignedBy", data.getAssignedBy(), notNullValue());
        assertThat("AssignedBy should be a string", data.getAssignedBy(), instanceOf(String.class));

        // Validate editorId property
        assertThat("Data should have editorId", data.getEditorId(), notNullValue());
        assertThat("EditorId should be a string", data.getEditorId(), instanceOf(String.class));

        logger.info("Response data schema validated: _id={}, assignedBy={}, editorId={}",
                data.get_id(), data.getAssignedBy(), data.getEditorId());
    }

    @Test(description = "Response data 'status' property is 0", priority = 5, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Skip")
    @Severity(SeverityLevel.CRITICAL)
    public void testStatusPropertyIsZero() {
        // Validate status is 0
        assertThat("Status should be 0",
                catalogSkipResponse.getData().getStatus(), equalTo(0));

        logger.info("Status property validated: {}", catalogSkipResponse.getData().getStatus());
    }

    @Test(description = "Response data 'videoType' property is 'catalog'", priority = 6, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Skip")
    @Severity(SeverityLevel.CRITICAL)
    public void testVideoTypeIsCatalog() {
        // Validate videoType is 'catalog'
        assertThat("VideoType should be 'catalog'",
                catalogSkipResponse.getData().getVideoType(), equalTo("catalog"));

        logger.info("VideoType property validated: {}", catalogSkipResponse.getData().getVideoType());
    }

    @Test(description = "Response sellerId matches requested sellerId", priority = 7, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Skip")
    @Severity(SeverityLevel.CRITICAL)
    public void testSellerIdMatches() {
        // Validate sellerId matches requested seller ID
        assertThat(String.format("SellerId should match requested seller ID: %s", sellerId),
                catalogSkipResponse.getData().getSellerId(), equalTo(sellerId));

        logger.info("SellerId validated: {} matches expected: {}",
                catalogSkipResponse.getData().getSellerId(), sellerId);
    }
}

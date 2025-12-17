package com.automation.tests.buyerapp.CollectionListing.AllCollection;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.CollectionAllResponse;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.automation.utils.VariableManager;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for Collection All (Saree) API - Converted from Postman Script.
 * Endpoint: {{navo_base}}/v1/collection/all?suitable_for=saree
 * Validates response structure, headers, and collection data.
 */
@Epic("Buyer App Collection Listing")
@Feature("Collection All for Saree API")
public class CollectionAllForSareeTest extends BaseTest {

    private static Response collectionAllResponse;
    private static CollectionAllResponse collectionAllResponseData;
    private String buyerAppBaseUrl;

    @BeforeClass
    public void setupBuyerApp() {
        buyerAppBaseUrl = config.buyerAppBaseUrl();
        logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);
    }

    @Test(description = "Status code is 200", priority = 1, groups = "buyerapp")
    @Story("Collection All for Saree")
    @Severity(SeverityLevel.CRITICAL)
    public void testStatusCode200() {
        // Send GET request with authentication and query parameter
        collectionAllResponse = RestAssured.given()
                .baseUri(buyerAppBaseUrl)
                .contentType("application/json")
                .header("Authorization", "Bearer " + VariableManager.getBuyerAppToken())
                .queryParam("suitable_for", "saree")
                .when()
                .get(BuyerAppEndpoints.COLLECTION_ALL);

        // Parse response for other tests
        collectionAllResponseData = JsonUtils.fromResponse(collectionAllResponse, CollectionAllResponse.class);

        // Status code is 200
        assertThat("Status code is 200",
                collectionAllResponse.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response time is less than threshold", priority = 2, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
    @Story("Collection All for Saree")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTime() {
        // Get response time threshold from config (fallback to 20000ms)
        long responseTimeThreshold = config.responseTimeThreshold();
        long actualResponseTime = collectionAllResponse.getTime();

        // Validate response time measurement is available
        assertThat("Response time measurement should be available",
                actualResponseTime, notNullValue());

        // Response time is less than threshold
        assertThat(String.format("Response time is less than %dms", responseTimeThreshold),
                actualResponseTime, lessThan(responseTimeThreshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                responseTimeThreshold);
    }

    @Test(description = "Response has JSON body", priority = 3, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
    @Story("Collection All for Saree")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseHasJsonBody() {
        // Response has JSON body
        assertThat("Response should have a body",
                collectionAllResponse.getBody().asString(), not(emptyOrNullString()));
        assertThat("Response should be parseable as JSON",
                collectionAllResponseData, notNullValue());

        logger.info("Response has valid JSON body");
    }

    @Test(description = "Required headers are present", priority = 4, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
    @Story("Collection All for Saree")
    @Severity(SeverityLevel.MINOR)
    public void testRequiredHeaders() {
        // Required headers are present
        assertThat("Content-Type header should be present",
                collectionAllResponse.getHeader("Content-Type"), notNullValue());
        assertThat("Content-Type should include application/json",
                collectionAllResponse.getHeader("Content-Type"), containsString("application/json"));

        logger.info("Required headers validated: Content-Type = {}", 
                collectionAllResponse.getHeader("Content-Type"));
    }

    @Test(description = "Validate response structure", priority = 5, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
    @Story("Collection All for Saree")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseStructure() {
        // Top-level validation
        assertThat("Response should be an object", collectionAllResponseData, notNullValue());
        assertThat("statusCode should be '10000'",
                collectionAllResponseData.getStatusCode(), equalTo("10000"));
        assertThat("message should be 'success'",
                collectionAllResponseData.getMessage(), equalTo("success"));
        assertThat("data should be an object",
                collectionAllResponseData.getData(), notNullValue());

        // Collection data validation
        assertThat("data should have property 'result' that is an array",
                collectionAllResponseData.getData().getResult(), instanceOf(java.util.List.class));
        assertThat("result should not be empty",
                collectionAllResponseData.getData().getResult(), not(empty()));

        // Validate each collection item
        collectionAllResponseData.getData().getResult().forEach(item -> {
            assertThat("Item should be an object", item, notNullValue());
            assertThat("_id should be a string", item.get_id(), instanceOf(String.class));
            assertThat("name should be a string", item.getName(), instanceOf(String.class));
            assertThat("description should be a string", item.getDescription(), instanceOf(String.class));
            assertThat("image should be a string", item.getImage(), instanceOf(String.class));
        });

        logger.info("Response structure validated successfully: {} collections found",
                collectionAllResponseData.getData().getResult().size());
    }
}

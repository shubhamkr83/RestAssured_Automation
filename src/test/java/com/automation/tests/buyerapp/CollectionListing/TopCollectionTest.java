package com.automation.tests.buyerapp.CollectionListing;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.TopCollectionResponse;
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
 * Test class for Top Collection API - Converted from Postman Script.
 * Endpoint: {{navo_base}}/v1/collection/top
 * Validates response structure, headers, and collection data.
 */
@Epic("Buyer App Collection Listing")
@Feature("Top Collection API")
public class TopCollectionTest extends BaseTest {

    private static Response topCollectionResponse;
    private static TopCollectionResponse topCollectionResponseData;
    private String buyerAppBaseUrl;

    @BeforeClass
    public void setupBuyerApp() {
        buyerAppBaseUrl = config.buyerAppBaseUrl();
        logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);

        String token = VariableManager.getBuyerAppToken();
        if (token == null || token.isEmpty()) {
            throw new RuntimeException("Buyer App token not available. Please run LoginTest first.");
        }
        logger.info("Using Buyer App token from VariableManager");
    }

    @Test(description = "Response status code is 200", priority = 1, groups = "buyerapp")
    @Story("Top Collection")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseStatusCode200() {
        // Send GET request with authentication
        topCollectionResponse = RestAssured.given()
                .baseUri(buyerAppBaseUrl)
                .contentType("application/json")
                .header("Authorization", "JWT " + VariableManager.getBuyerAppToken())
                .when()
                .get(BuyerAppEndpoints.COLLECTION_TOP);

        // Parse response for other tests
        topCollectionResponseData = JsonUtils.fromResponse(topCollectionResponse, TopCollectionResponse.class);

        // Response status code is 200
        assertThat("Response status code is 200",
                topCollectionResponse.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response time is less than threshold", priority = 2, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Top Collection")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTime() {
        // Get response time threshold from config (fallback to 20000ms)
        long responseTimeThreshold = config.responseTimeThreshold();
        long actualResponseTime = topCollectionResponse.getTime();

        // Validate response time measurement is available
        assertThat("Response time measurement should be available",
                actualResponseTime, notNullValue());

        // Response time is less than threshold
        assertThat(String.format("Response time is less than %dms", responseTimeThreshold),
                actualResponseTime, lessThan(responseTimeThreshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                responseTimeThreshold);
    }

    @Test(description = "Response has the required fields - statusCode, message, and data", priority = 3, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Top Collection")
    @Severity(SeverityLevel.CRITICAL)
    public void testRequiredFields() {
        // Response has the required fields
        assertThat("Response should be an object", topCollectionResponseData, notNullValue());
        assertThat("statusCode should exist", topCollectionResponseData.getStatusCode(), notNullValue());
        assertThat("message should exist", topCollectionResponseData.getMessage(), notNullValue());
        assertThat("data should exist", topCollectionResponseData.getData(), notNullValue());

        logger.info("Required fields validated: statusCode, message, data");
    }

    @Test(description = "Validate the result object in the response", priority = 4, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Top Collection")
    @Severity(SeverityLevel.CRITICAL)
    public void testResultObjectValidation() {
        // Validate the result object in the response
        assertThat("Response should be an object", topCollectionResponseData, notNullValue());
        assertThat("data.result should exist and be an array",
                topCollectionResponseData.getData().getResult(), instanceOf(java.util.List.class));

        topCollectionResponseData.getData().getResult().forEach(item -> {
            assertThat("Item should be an object", item, notNullValue());
            assertThat("_id should be a string", item.get_id(), instanceOf(String.class));
            assertThat("name should be a string", item.getName(), instanceOf(String.class));
            assertThat("description should be a string", item.getDescription(), instanceOf(String.class));
            assertThat("image should be a string", item.getImage(), instanceOf(String.class));
        });

        logger.info("Result object validated: {} items found", topCollectionResponseData.getData().getResult().size());
    }

    @Test(description = "Response time is within an acceptable range", priority = 5, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Top Collection")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTimeAcceptableRange() {
        long actualResponseTime = topCollectionResponse.getTime();

        // Response time is within an acceptable range (3000ms)
        assertThat("Response time is within an acceptable range",
                actualResponseTime, lessThan(3000L));

        logger.info("Response time within acceptable range: {} ms (< 3000ms)", actualResponseTime);
    }

    @Test(description = "Content-Type header is present in the response", priority = 6, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Top Collection")
    @Severity(SeverityLevel.MINOR)
    public void testContentTypeHeaderPresent() {
        // Content-Type header is present in the response
        assertThat("Content-Type header should be present",
                topCollectionResponse.getHeader("Content-Type"), notNullValue());

        logger.info("Content-Type header verified: {}", topCollectionResponse.getHeader("Content-Type"));
    }

    @Test(description = "Content-Type header value is 'application/json'", priority = 7, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Top Collection")
    @Severity(SeverityLevel.MINOR)
    public void testContentTypeHeaderValue() {
        // Content-Type header value is 'application/json'
        assertThat("Content-Type should include application/json",
                topCollectionResponse.getHeader("Content-Type"), containsString("application/json"));

        logger.info("Content-Type header value validated: application/json");
    }
}

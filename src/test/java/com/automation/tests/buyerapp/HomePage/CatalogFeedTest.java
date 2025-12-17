package com.automation.tests.buyerapp.HomePage;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.HomeCatalogFeedResponse;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.automation.utils.VariableManager;
import static com.automation.tests.buyerapp.HomePage.FeedFilterSaveTest.suitableFor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for Home Catalog Feed API - Converted from Postman Script.
 * Endpoint: {{navo_base}}/v1/feed/home/catalog?size=6&page=0&suitable_for={{suitable_for}}
 * Validates response structure, headers, and parameter edge cases.
 */
@Epic("Buyer App Home Page")
@Feature("Home Catalog Feed API")
public class CatalogFeedTest extends BaseTest {

    private static Response catalogFeedResponse;
    private static HomeCatalogFeedResponse catalogFeedResponseData;
    private String buyerAppBaseUrl;

    @BeforeClass
    public void setupBuyerApp() {
        buyerAppBaseUrl = config.buyerAppBaseUrl();
        logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);
    }

    @Test(description = "Test the response status 200 for valid request", priority = 1, groups = "buyerapp")
    @Story("Home Catalog Feed")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseStatus() {
        // Get suitable_for parameter (use from previous test or default)
        String suitableForParam = (suitableFor != null && !suitableFor.isEmpty()) 
                ? suitableFor 
                : "saree"; // Default value

        // Send GET request with authentication and query parameters
        catalogFeedResponse = RestAssured.given()
                .baseUri(buyerAppBaseUrl)
                .contentType("application/json")
                .header("Authorization", "Bearer " + VariableManager.getBuyerAppToken())
                .queryParam("size", 6)
                .queryParam("page", 0)
                .queryParam("suitable_for", suitableForParam)
                .when()
                .get(BuyerAppEndpoints.FEED_HOME_CATALOG);

        // Parse response for other tests
        catalogFeedResponseData = JsonUtils.fromResponse(catalogFeedResponse, HomeCatalogFeedResponse.class);

        // Test the response status 200 for valid request
        assertThat("Test the response status 200 for valid request",
                catalogFeedResponse.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Test the response contains a valid JSON body", priority = 2, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
    @Story("Home Catalog Feed")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseHasJsonBody() {
        // Test the response contains a valid JSON body
        assertThat("Response should have a body",
                catalogFeedResponse.getBody().asString(), not(emptyOrNullString()));
        assertThat("Response should be parseable as JSON",
                catalogFeedResponseData, notNullValue());

        logger.info("Response contains valid JSON body");
    }

    @Test(description = "Test that response time is less than threshold", priority = 3, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
    @Story("Home Catalog Feed")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTime() {
        // Get response time threshold from config (fallback to 20000ms)
        long responseTimeThreshold = config.responseTimeThreshold();
        long actualResponseTime = catalogFeedResponse.getTime();

        // Validate response time measurement is available
        assertThat("Response time measurement should be available",
                actualResponseTime, notNullValue());

        // Test that response time is less than threshold
        assertThat(String.format("Test that response time is less than %dms", responseTimeThreshold),
                actualResponseTime, lessThan(responseTimeThreshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                responseTimeThreshold);
    }

    @Test(description = "Test that Content-Type header is present and set to application/json", priority = 4, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
    @Story("Home Catalog Feed")
    @Severity(SeverityLevel.MINOR)
    public void testContentTypeHeader() {
        // Test that Content-Type header is present and set to application/json
        assertThat("Content-Type header should include application/json",
                catalogFeedResponse.getHeader("Content-Type"), containsString("application/json"));

        logger.info("Content-Type header validated: {}", catalogFeedResponse.getHeader("Content-Type"));
    }

    @Test(description = "Test handling of empty suitable_for parameter", priority = 5, groups = "buyerapp")
    @Story("Home Catalog Feed")
    @Severity(SeverityLevel.NORMAL)
    public void testEmptySuitableForParameter() {
        // Test handling of empty suitable_for parameter
        Response response = RestAssured.given()
                .baseUri(buyerAppBaseUrl)
                .contentType("application/json")
                .header("Authorization", "Bearer " + VariableManager.getBuyerAppToken())
                .queryParam("size", 6)
                .queryParam("page", 0)
                .queryParam("suitable_for", "")
                .when()
                .get(BuyerAppEndpoints.FEED_HOME_CATALOG);

        assertThat("Response status should be 200 for empty suitable_for",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        HomeCatalogFeedResponse responseData = JsonUtils.fromResponse(response, HomeCatalogFeedResponse.class);
        assertThat("data should be an array",
                responseData.getData(), instanceOf(java.util.List.class));

        logger.info("Empty suitable_for parameter handled correctly");
    }

    @Test(description = "Test handling of maximum allowed size parameter", priority = 6, groups = "buyerapp")
    @Story("Home Catalog Feed")
    @Severity(SeverityLevel.NORMAL)
    public void testMaximumSizeParameter() {
        // Get suitable_for parameter
        String suitableForParam = (suitableFor != null && !suitableFor.isEmpty()) 
                ? suitableFor 
                : "saree";

        // Test handling of maximum allowed size parameter
        Response response = RestAssured.given()
                .baseUri(buyerAppBaseUrl)
                .contentType("application/json")
                .header("Authorization", "Bearer " + VariableManager.getBuyerAppToken())
                .queryParam("size", 50)
                .queryParam("page", 0)
                .queryParam("suitable_for", suitableForParam)
                .when()
                .get(BuyerAppEndpoints.FEED_HOME_CATALOG);

        assertThat("Response status should be 200 for maximum size",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        HomeCatalogFeedResponse responseData = JsonUtils.fromResponse(response, HomeCatalogFeedResponse.class);
        assertThat("data length should equal 50 for maximum size",
                responseData.getData().size(), equalTo(50));

        logger.info("Maximum size parameter (50) handled correctly: {} items returned", 
                responseData.getData().size());
    }

    @Test(description = "Test handling of different Accept-Language headers", priority = 7, groups = "buyerapp")
    @Story("Home Catalog Feed")
    @Severity(SeverityLevel.NORMAL)
    public void testAcceptLanguageHeader() {
        // Get suitable_for parameter
        String suitableForParam = (suitableFor != null && !suitableFor.isEmpty()) 
                ? suitableFor 
                : "saree";

        // Test handling of different Accept-Language headers
        Response response = RestAssured.given()
                .baseUri(buyerAppBaseUrl)
                .contentType("application/json")
                .header("Authorization", "Bearer " + VariableManager.getBuyerAppToken())
                .header("Accept-Language", "hi") // Hindi language
                .queryParam("size", 6)
                .queryParam("page", 0)
                .queryParam("suitable_for", suitableForParam)
                .when()
                .get(BuyerAppEndpoints.FEED_HOME_CATALOG);

        assertThat("Response status should be 200 for different Accept-Language",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Different Accept-Language header handled correctly");
    }
}

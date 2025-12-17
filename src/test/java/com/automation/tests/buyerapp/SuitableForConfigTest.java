package com.automation.tests.buyerapp;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.SuitableForConfigResponse;
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
 * Test class for Suitable For Config API - Converted from Postman Script.
 * Endpoint: {{navo_base}}/v1/feed/home/config
 * Validates response structure and config items.
 */
@Epic("Buyer App Configuration")
@Feature("Suitable For Config API")
public class SuitableForConfigTest extends BaseTest {

    private static Response configResponse;
    private static SuitableForConfigResponse configResponseData;
    private String buyerAppBaseUrl;

    @BeforeClass
    public void setupBuyerApp() {
        buyerAppBaseUrl = config.buyerAppBaseUrl();
        logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);
    }

    @Test(description = "Response status code is 200", priority = 1, groups = "buyerapp")
    @Story("Suitable For Config")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseStatusCode200() {
        // Send GET request with authentication
        configResponse = RestAssured.given()
                .baseUri(buyerAppBaseUrl)
                .contentType("application/json")
                .header("Authorization", "Bearer " + VariableManager.getBuyerAppToken())
                .when()
                .get(BuyerAppEndpoints.FEED_HOME_CONFIG);

        // Parse response for other tests
        configResponseData = JsonUtils.fromResponse(configResponse, SuitableForConfigResponse.class);

        // Response status code is 200
        assertThat("Response status code is 200",
                configResponse.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Content-Type header is present", priority = 2, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Suitable For Config")
    @Severity(SeverityLevel.MINOR)
    public void testContentTypeHeaderPresent() {
        // Content-Type header is present
        assertThat("Content-Type header should be present",
                configResponse.getHeader("Content-Type"), notNullValue());

        logger.info("Content-Type header verified: {}", configResponse.getHeader("Content-Type"));
    }

    @Test(description = "Response time is less than threshold", priority = 3, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Suitable For Config")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTime() {
        // Get response time threshold from config (fallback to 20000ms)
        long responseTimeThreshold = config.responseTimeThreshold();
        long actualResponseTime = configResponse.getTime();

        // Validate response time measurement is available
        assertThat("Response time measurement should be available",
                actualResponseTime, notNullValue());

        // Response time is less than threshold
        assertThat(String.format("Response time is less than %dms", responseTimeThreshold),
                actualResponseTime, lessThan(responseTimeThreshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                responseTimeThreshold);
    }

    @Test(description = "Response has required 'data' object with non-empty 'result' array", priority = 4, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Suitable For Config")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseStructure() {
        // Response has required 'data' object with non-empty 'result' array
        assertThat("Response should be an object", configResponseData, notNullValue());
        assertThat("data should exist and be an object", configResponseData.getData(), notNullValue());
        assertThat("data.result should exist and be an array with at least 1 element",
                configResponseData.getData().getResult(), 
                allOf(instanceOf(java.util.List.class), hasSize(greaterThanOrEqualTo(1))));

        logger.info("Response structure validated: {} config items found", 
                configResponseData.getData().getResult().size());
    }

    @Test(description = "Each result item contains required fields with correct types", priority = 5, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Suitable For Config")
    @Severity(SeverityLevel.CRITICAL)
    public void testResultItemsFields() {
        // Each result item contains required fields with correct types
        configResponseData.getData().getResult().forEach(item -> {
            assertThat("item should be an object", item, notNullValue());
            assertThat("type should be a string with at least 1 character",
                    item.getType(), allOf(instanceOf(String.class), not(emptyOrNullString())));
            assertThat("data should be an array", item.getData(), instanceOf(java.util.List.class));
            assertThat("api should be a string", item.getApi(), instanceOf(String.class));
            assertThat("title should be a string", item.getTitle(), instanceOf(String.class));
            assertThat("description should be a string", item.getDescription(), instanceOf(String.class));
            assertThat("imageUrl should be a string", item.getImageUrl(), instanceOf(String.class));
        });

        logger.info("Result items fields validated for all {} items", 
                configResponseData.getData().getResult().size());
    }
}

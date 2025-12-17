package com.automation.tests.buyerapp.ProfilePage;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.WatchedVideosResponse;
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
 * Test class for Watched Videos API - Converted from Postman Script.
 * Endpoint: {{navo_base}}/v1/user/viewed/videos
 * Validates response structure and video item fields.
 */
@Epic("Buyer App Profile Page")
@Feature("Watched Videos API")
public class WatchedVideosTest extends BaseTest {

    private static Response watchedVideosResponse;
    private static WatchedVideosResponse watchedVideosResponseData;
    private String buyerAppBaseUrl;

    @BeforeClass
    public void setupBuyerApp() {
        buyerAppBaseUrl = config.buyerAppBaseUrl();
        logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);
    }

    @Test(description = "Response status code is 200", priority = 1, groups = "buyerapp")
    @Story("Watched Videos")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseStatusCode200() {
        // Send GET request with authentication
        watchedVideosResponse = RestAssured.given()
                .baseUri(buyerAppBaseUrl)
                .contentType("application/json")
                .header("Authorization", "Bearer " + VariableManager.getBuyerAppToken())
                .when()
                .get(BuyerAppEndpoints.USER_VIEWED_VIDEOS);

        // Parse response for other tests
        watchedVideosResponseData = JsonUtils.fromResponse(watchedVideosResponse, WatchedVideosResponse.class);

        // Response status code is 200
        assertThat("Response status code is 200",
                watchedVideosResponse.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response time is less than threshold", priority = 2, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Watched Videos")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTime() {
        // Get response time threshold from config (fallback to 20000ms)
        long responseTimeThreshold = config.responseTimeThreshold();
        long actualResponseTime = watchedVideosResponse.getTime();

        // Validate response time measurement is available
        assertThat("Response time measurement should be available",
                actualResponseTime, notNullValue());

        // Response time is less than threshold
        assertThat(String.format("Response time is less than %dms", responseTimeThreshold),
                actualResponseTime, lessThan(responseTimeThreshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                responseTimeThreshold);
    }

    @Test(description = "Valid JSON response structure", priority = 3, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Watched Videos")
    @Severity(SeverityLevel.CRITICAL)
    public void testValidJsonResponseStructure() {
        // Valid JSON response structure
        assertThat("Response should be an object", watchedVideosResponseData, notNullValue());
        assertThat("data should exist and be an object", watchedVideosResponseData.getData(), notNullValue());
        assertThat("data.result should exist and be an array and not empty",
                watchedVideosResponseData.getData().getResult(), 
                allOf(instanceOf(java.util.List.class), not(empty())));

        logger.info("Valid JSON response structure verified: {} videos found", 
                watchedVideosResponseData.getData().getResult().size());
    }

    @Test(description = "Content-Type is application/json", priority = 4, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Watched Videos")
    @Severity(SeverityLevel.MINOR)
    public void testContentTypeHeader() {
        // Content-Type is application/json
        assertThat("Content-Type should include application/json",
                watchedVideosResponse.getHeader("Content-Type"), containsString("application/json"));

        logger.info("Content-Type header verified: {}", watchedVideosResponse.getHeader("Content-Type"));
    }

    @Test(description = "Each video item has required fields", priority = 5, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Watched Videos")
    @Severity(SeverityLevel.CRITICAL)
    public void testRequiredFieldsInVideoItems() {
        // Each video item has required fields
        watchedVideosResponseData.getData().getResult().forEach(item -> {
            assertThat("videoId should be present", item.getVideoId(), notNullValue());
            assertThat("_id should be present", item.get_id(), notNullValue());
            assertThat("phoneNumber should be present", item.getPhoneNumber(), notNullValue());
            assertThat("product should be present", item.getProduct(), notNullValue());
            assertThat("collection should be present", item.getCollection(), notNullValue());
            assertThat("market should be present", item.getMarket(), notNullValue());
            assertThat("priceText should be present", item.getPriceText(), notNullValue());
            assertThat("driveLink should be present", item.getDriveLink(), notNullValue());
            assertThat("isDeleted should be present", item.getIsDeleted(), notNullValue());
            assertThat("seller should be present", item.getSeller(), notNullValue());
            assertThat("thubmbnailDriveLink should be present", item.getThubmbnailDriveLink(), notNullValue());
            assertThat("createdAt should be present", item.getCreatedAt(), notNullValue());
        });

        logger.info("Required fields validated for all {} video items", 
                watchedVideosResponseData.getData().getResult().size());
    }
}

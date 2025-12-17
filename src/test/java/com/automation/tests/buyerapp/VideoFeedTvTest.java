package com.automation.tests.buyerapp;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.request.VideoFeedRequest;
import com.automation.models.response.VideoFeedResponse;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;

import com.automation.utils.VariableManager;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for Video Feed (T.V) API - Converted from Postman Script.
 * Endpoint: {{navo_base}}/v1/feed/tv
 * Validates response structure and video feed data.
 */
@Epic("Buyer App Video Feed")
@Feature("Video Feed (T.V) API")
public class VideoFeedTvTest extends BaseTest {

    private static Response videoFeedResponse;
    private static VideoFeedResponse videoFeedResponseData;
    private String buyerAppBaseUrl;

    @BeforeClass
    public void setupBuyerApp() {
        buyerAppBaseUrl = config.buyerAppBaseUrl();
        logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);
    }

    @Test(description = "Response status code is 200", priority = 1, groups = "buyerapp")
    @Story("Video Feed (T.V)")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseStatusCode200() {
        // Prepare request body
        VideoFeedRequest requestBody = VideoFeedRequest.builder()
                .excluded_videos(new ArrayList<>())
                .filters(VideoFeedRequest.Filters.builder()
                        .city(new ArrayList<>())
                        .productTags(new ArrayList<>())
                        .suitable_for(new ArrayList<>())
                        .testData("")
                        .build())
                .from(0)
                .liquidity("seller")
                .sort_by("popular")
                .sort_order("desc")
                .video_id("")
                .build();

        // Send POST request with authentication
        videoFeedResponse = RestAssured.given()
                .baseUri(buyerAppBaseUrl)
                .contentType("application/json")
                .header("Authorization", "Bearer " + VariableManager.getBuyerAppToken())
                .body(requestBody)
                .when()
                .post(BuyerAppEndpoints.FEED_TV);

        // Parse response for other tests
        videoFeedResponseData = JsonUtils.fromResponse(videoFeedResponse, VideoFeedResponse.class);

        // Response status code is 200
        assertThat("Response status code is 200",
                videoFeedResponse.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response Content-Type header is application/json", priority = 2, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Video Feed (T.V)")
    @Severity(SeverityLevel.MINOR)
    public void testContentTypeHeader() {
        // Response Content-Type header is application/json
        assertThat("Content-Type should include application/json",
                videoFeedResponse.getHeader("Content-Type"), containsString("application/json"));

        logger.info("Content-Type header verified: {}", videoFeedResponse.getHeader("Content-Type"));
    }

    @Test(description = "Response has valid JSON data", priority = 3, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Video Feed (T.V)")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseHasValidJsonData() {
        // Response has valid JSON data
        assertThat("Response should be an object", videoFeedResponseData, notNullValue());

        logger.info("Response has valid JSON data");
    }

    @Test(description = "Response time is less than threshold", priority = 4, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Video Feed (T.V)")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTime() {
        // Get response time threshold from config (fallback to 20000ms)
        long responseTimeThreshold = config.responseTimeThreshold();
        long actualResponseTime = videoFeedResponse.getTime();

        // Validate response time measurement is available
        assertThat("Response time measurement should be available",
                actualResponseTime, notNullValue());

        // Response time is less than threshold
        assertThat(String.format("Response time is less than %dms", responseTimeThreshold),
                actualResponseTime, lessThan(responseTimeThreshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                responseTimeThreshold);
    }

    @Test(description = "Data object structure is valid", priority = 5, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Video Feed (T.V)")
    @Severity(SeverityLevel.CRITICAL)
    public void testDataObjectStructure() {
        // Data object structure is valid
        assertThat("data should exist and be an object", videoFeedResponseData.getData(), notNullValue());

        logger.info("Data object structure is valid");
    }

    @Test(description = "Result array exists in data object", priority = 6, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Video Feed (T.V)")
    @Severity(SeverityLevel.CRITICAL)
    public void testResultArrayExists() {
        // Result array exists in data object
        assertThat("result should be an array", 
                videoFeedResponseData.getData().getResult(), instanceOf(java.util.List.class));

        logger.info("Result array exists in data object");
    }

    @Test(description = "Result array contains at least one item", priority = 7, dependsOnMethods = "testResultArrayExists", groups = "buyerapp")
    @Story("Video Feed (T.V)")
    @Severity(SeverityLevel.NORMAL)
    public void testResultArrayNotEmpty() {
        // Result array contains at least one item
        assertThat("result array should contain at least one item",
                videoFeedResponseData.getData().getResult(), hasSize(greaterThanOrEqualTo(1)));

        logger.info("Result array contains {} items", videoFeedResponseData.getData().getResult().size());
    }

    @Test(description = "Seller objects have required fields", priority = 8, dependsOnMethods = "testResultArrayNotEmpty", groups = "buyerapp")
    @Story("Video Feed (T.V)")
    @Severity(SeverityLevel.CRITICAL)
    public void testSellerObjectsRequiredFields() {
        // Seller objects have required fields
        videoFeedResponseData.getData().getResult().forEach(item -> {
            if (item.getSeller() != null) {
                VideoFeedResponse.Seller seller = item.getSeller();
                
                assertThat("seller should be an object", seller, notNullValue());

                // Required string fields
                assertThat("businessName should be a string", seller.getBusinessName(), instanceOf(String.class));
                assertThat("phoneNumber should be a string", seller.getPhoneNumber(), instanceOf(String.class));
                assertThat("name should be a string", seller.getName(), instanceOf(String.class));
                assertThat("id should be a string", seller.getId(), instanceOf(String.class));

                // Optional boolean fields
                if (seller.getIsSuper() != null) {
                    assertThat("isSuper should be a boolean", seller.getIsSuper(), instanceOf(Boolean.class));
                }
                if (seller.getIsDeleted() != null) {
                    assertThat("isDeleted should be a boolean", seller.getIsDeleted(), instanceOf(Boolean.class));
                }
                if (seller.getOrdersEnabled() != null) {
                    assertThat("ordersEnabled should be a boolean", seller.getOrdersEnabled(), instanceOf(Boolean.class));
                }
                if (seller.getIsCatalogAvailable() != null) {
                    assertThat("isCatalogAvailable should be a boolean", seller.getIsCatalogAvailable(), instanceOf(Boolean.class));
                }

                // Optional number field
                if (seller.getMov() != null) {
                    assertThat("mov should be a number", seller.getMov(), instanceOf(Integer.class));
                }
            }
        });

        logger.info("Seller objects required fields validated for all items");
    }
}

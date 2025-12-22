package com.automation.tests.bomb.VideoTaggingPipeline.VideoTagging;

import com.automation.base.BaseTest;
import com.automation.constants.BombEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.request.VideoTaggingRequest;
import com.automation.models.response.VideoTaggingResponse;
import com.automation.utils.VariableManager;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for BOMB Video Tagging endpoint.
 * Endpoint: PUT
 * {{bizup_base}}/v1/admin/editor/edit/videos/{{seller_id}}/{{video_id}}
 * Implements comprehensive Postman test scripts for video tagging validation.
 */
@Epic("BOMB Video Tagging Pipeline")
@Feature("Video Tagging")
public class VideoTaggingTest extends BaseTest {

    private String authToken;
    private Response response;
    private VideoTaggingResponse videoTaggingResponse;

    // Request data
    private VideoTaggingRequest requestBody;
    private String sellerId;
    private String videoId;
    private String videoTitle;
    private String price;

    // Product and tag IDs
    private static final String PRODUCT_ID = "67c59d8ef22202c05e7d612b";

    @BeforeClass
    public void setupAuth() {
        // Get token from VariableManager (thread-safe)
        authToken = VariableManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
        logger.info("Using BOMB token from VariableManager");

        // Get seller ID from VariableManager
        sellerId = VariableManager.getSellerId();
        logger.info("Using seller ID: {}", sellerId);
        
        // Get price from VariableManager
        price = VariableManager.get("price", "500");
        logger.info("Using price: {}", price);

        // Get video ID from previous test
        // Use VariableManager to get video ID either from properties or set by previous test
        String prevVideoId = VariableManager.getVideoId();
        if (prevVideoId != null) {
            videoId = prevVideoId;
            logger.info("Using video ID from VariableManager: {}", videoId);
        } else {
            // Fallback to a default ID if not available
            videoId = "default_video_id";
            logger.warn("Video ID not available, using default");
        }

        // Get video title from previous test
        String prevVideoTitle = VariableManager.get("video_title");
        if (prevVideoTitle != null) {
            videoTitle = prevVideoTitle;
            logger.info("Using video title from VariableManager: {}", videoTitle);
        } else {
            // Fallback to a default title if not available
            videoTitle = "Default Video Title";
            logger.warn("Video title not available, using default");
        }

        // Build request body
        requestBody = VideoTaggingRequest.builder()
                .product_id(PRODUCT_ID)
                .tags(Arrays.asList(
                        "67c5a9d69c74685b50b1a489",
                        "67c5a9c59c74685b50b1a069",
                        "67c5a9d19c74685b50b1a345",
                        "67c5a9cd9c74685b50b1a23d",
                        "67c5a9c69c74685b50b1a0b1",
                        "67c5a9ba9c74685b50b19dc9",
                        "67c5a9d39c74685b50b1a3a5",
                        "67c5a9d29c74685b50b1a399",
                        "67c5a9d29c74685b50b1a38d",
                        "67c5a9bb9c74685b50b19df9",
                        "67c5a9ba9c74685b50b19dd5"))
                .suggested(Collections.emptyList())
                .title(videoTitle)
                .price(price)
                .build();
    }

    @Test(description = "Test the response status is 200", priority = 1, groups = "bomb")
    @Story("Video Tagging")
    @Severity(SeverityLevel.BLOCKER)
    public void testStatusCode200() {
        // Send PUT request to tag video
        response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .body(requestBody)
                .when()
                .post(BombEndpoints.VIDEO_TAGGING + "/" + sellerId + "/" + videoId);

        // Parse response for other tests
        videoTaggingResponse = JsonUtils.fromResponse(response, VideoTaggingResponse.class);

        // Verify response status is 200 OK
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Validate response time is under threshold", priority = 2, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Tagging")
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

    @Test(description = "Verify Content-Type header includes application/json", priority = 3, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Tagging")
    @Severity(SeverityLevel.NORMAL)
    public void testContentTypeHeader() {
        // Verify Content-Type header
        assertThat("Content-Type should include application/json",
                response.getContentType(), containsString("application/json"));

        logger.info("Content-Type header verified: {}", response.getContentType());
    }

    @Test(description = "Validate response has a valid JSON body", priority = 4, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Tagging")
    @Severity(SeverityLevel.NORMAL)
    public void testValidJsonBody() {
        // Verify response has valid JSON structure
        assertThat("Response should have valid JSON structure", videoTaggingResponse, notNullValue());

        logger.info("Response has valid JSON body");
    }

    @Test(description = "Check response contains required fields: statusCode, message, and data", priority = 5, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Tagging")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseContainsRequiredFields() {
        // Validate response has all required fields
        assertThat("Response should have statusCode", videoTaggingResponse.getStatusCode(), notNullValue());
        assertThat("Response should have message", videoTaggingResponse.getMessage(), notNullValue());
        assertThat("Message should be 'success'", videoTaggingResponse.getMessage(), equalTo("success"));
        assertThat("Response should have data", videoTaggingResponse.getData(), notNullValue());

        logger.info("Response contains required fields");
    }

    @Test(description = "Verify videoId matches the expected value", priority = 6, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Tagging")
    @Severity(SeverityLevel.CRITICAL)
    public void testVideoIdMatches() {
        VideoTaggingResponse.VideoTaggingData data = videoTaggingResponse.getData();

        // Validate _id is a string
        assertThat("Video ID should be a string", data.get_id(), instanceOf(String.class));

        // Validate _id matches expected video ID
        assertThat("Video ID should match expected value",
                data.get_id(), equalTo(videoId));

        logger.info("Video ID validated: {} matches expected: {}", data.get_id(), videoId);
    }

    @Test(description = "Verify seller ID matches the expected seller ID", priority = 7, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Tagging")
    @Severity(SeverityLevel.CRITICAL)
    public void testSellerIdMatches() {
        VideoTaggingResponse.VideoTaggingData data = videoTaggingResponse.getData();

        // Validate seller matches expected seller ID
        assertThat("Seller ID should match expected value",
                data.getSeller(), equalTo(sellerId));

        logger.info("Seller ID validated: {} matches expected: {}", data.getSeller(), sellerId);
    }

    @Test(description = "Verify Title matches the expected value", priority = 8, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Tagging")
    @Severity(SeverityLevel.CRITICAL)
    public void testTitleMatches() {
        VideoTaggingResponse.VideoTaggingData data = videoTaggingResponse.getData();

        // Validate title is a string
        assertThat("Title should be a string", data.getTitle(), instanceOf(String.class));

        // Validate title matches expected video title
        assertThat("Title should match expected value",
                data.getTitle(), equalTo(videoTitle));

        logger.info("Title validated: {} matches expected: {}", data.getTitle(), videoTitle);
    }

    @Test(description = "Verify Price matches the expected value", priority = 9, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Tagging")
    @Severity(SeverityLevel.CRITICAL)
    public void testPriceMatches() {
        VideoTaggingResponse.VideoTaggingData data = videoTaggingResponse.getData();

        // Validate priceText is a string
        assertThat("PriceText should be a string", data.getPriceText(), instanceOf(String.class));

        // Validate priceText matches expected price
        assertThat("PriceText should match expected value",
                data.getPriceText(), equalTo(price));

        logger.info("Price validated: {} matches expected: {}", data.getPriceText(), price);
    }

    @Test(description = "Ensure no error is present in the response data", priority = 10, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Tagging")
    @Severity(SeverityLevel.NORMAL)
    public void testNoErrorInResponseData() {
        // Validate data is not undefined/null
        assertThat("Response data should not be undefined",
                videoTaggingResponse.getData(), notNullValue());

        logger.info("No error in response data");
    }
}

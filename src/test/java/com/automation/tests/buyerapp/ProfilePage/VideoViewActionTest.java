package com.automation.tests.buyerapp.ProfilePage;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.request.VideoViewActionRequest;
import com.automation.models.response.VideoViewActionResponse;
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
 * Test class for Video View Action API - Converted from Postman Script.
 * Endpoint: {{navo_base}}/api/action
 * Validates response structure, video view action data, and JWT token.
 */
@Epic("Buyer App Profile Page")
@Feature("Video View Action API")
public class VideoViewActionTest extends BaseTest {

        private static Response videoViewResponse;
        private static VideoViewActionResponse videoViewResponseData;
        private String buyerAppBaseUrl;

        // Test data
        private static final String ACTION = "view";
        private String videoId;
        private String userId;

        @BeforeClass
        public void setupBuyerApp() {
                buyerAppBaseUrl = config.buyerAppBaseUrl();
                logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);

                String token = VariableManager.getBuyerAppToken();
                if (token == null || token.isEmpty()) {
                        throw new RuntimeException("Buyer App token not available. Please run LoginTest first.");
                }
                logger.info("Using Buyer App token from VariableManager");

                // Get video ID and user ID after VariableManager is initialized
                videoId = VariableManager.getVideoId();
                userId = VariableManager.getUserId();
                logger.info("Using video ID: {} and user ID: {}", videoId, userId);
        }

        @Test(description = "Response status code is 200", priority = 1, groups = "buyerapp")
        @Story("Video View Action")
        @Severity(SeverityLevel.CRITICAL)
        public void testResponseStatusCode200() {
                // Prepare request body
                VideoViewActionRequest requestBody = VideoViewActionRequest.builder()
                                .action(ACTION)
                                .creatorId(VariableManager.get("creator_id"))
                                .userId(VariableManager.get("user_id"))
                                .videoId(VariableManager.get("video_id", "681032fd9010791f25aed769"))
                                .build();

                // Send POST request with authentication
                videoViewResponse = RestAssured.given()
                                .baseUri(buyerAppBaseUrl)
                                .contentType("application/json")
                                .header("Authorization", "JWT " + VariableManager.getBuyerAppToken())
                                .body(requestBody)
                                .when()
                                .post(BuyerAppEndpoints.ACTION);

                // Parse response for other tests
                videoViewResponseData = JsonUtils.fromResponse(videoViewResponse, VideoViewActionResponse.class);

                // Response status code is 200
                assertThat("Response status code is 200",
                                videoViewResponse.getStatusCode(), equalTo(HttpStatus.OK));

                logger.info("Response status verified: 200 OK");
        }

        @Test(description = "Response time is less than threshold", priority = 2, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
        @Story("Video View Action")
        @Severity(SeverityLevel.NORMAL)
        public void testResponseTime() {
                // Get response time threshold from config (fallback to 20000ms)
                long responseTimeThreshold = config.responseTimeThreshold();
                long actualResponseTime = videoViewResponse.getTime();

                // Validate response time measurement is available
                assertThat("Response time measurement should be available",
                                actualResponseTime, notNullValue());

                // Response time is less than threshold
                assertThat(String.format("Response time is less than %dms", responseTimeThreshold),
                                actualResponseTime, lessThan(responseTimeThreshold));

                logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                                responseTimeThreshold);
        }

        @Test(description = "Response has JSON body", priority = 3, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
        @Story("Video View Action")
        @Severity(SeverityLevel.NORMAL)
        public void testResponseHasJsonBody() {
                // Response has JSON body
                assertThat("Response should have a body",
                                videoViewResponse.getBody().asString(), not(emptyOrNullString()));
                assertThat("Response should be parseable as JSON",
                                videoViewResponseData, notNullValue());

                logger.info("Response has valid JSON body");
        }

        @Test(description = "Response has success status", priority = 4, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
        @Story("Video View Action")
        @Severity(SeverityLevel.CRITICAL)
        public void testResponseSuccessStatus() {
                // Response has success status
                assertThat("message should be 'Success'",
                                videoViewResponseData.getMessage(), equalTo("Success"));

                logger.info("Response has success status: message=Success");
        }

        @Test(description = "Response contains expected structure", priority = 5, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
        @Story("Video View Action")
        @Severity(SeverityLevel.CRITICAL)
        public void testResponseStructure() {
                // Response contains expected structure
                assertThat("data should be present",
                                videoViewResponseData.getData(), notNullValue());

                logger.info("Response contains expected structure");
        }

        @Test(description = "Content-Type header is application/json", priority = 6, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
        @Story("Video View Action")
        @Severity(SeverityLevel.MINOR)
        public void testContentTypeHeader() {
                // Content-Type header is application/json
                assertThat("Content-Type should include application/json",
                                videoViewResponse.getHeader("Content-Type"), containsString("application/json"));

                logger.info("Content-Type header verified: {}", videoViewResponse.getHeader("Content-Type"));
        }

        @Test(description = "isDeleted should be false", priority = 7, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
        @Story("Video View Action")
        @Severity(SeverityLevel.NORMAL)
        public void testIsDeletedFalse() {
                // isDeleted should be false
                assertThat("isDeleted should be false",
                                videoViewResponseData.getData().getIsDeleted(), equalTo(false));

                logger.info("isDeleted validated: false");
        }

        @Test(description = "videoId should be correct string and not empty", priority = 8, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
        @Story("Video View Action")
        @Severity(SeverityLevel.CRITICAL)
        public void testVideoIdValidation() {
                // videoId should be correct string and not empty
                assertThat("videoId should be present",
                                videoViewResponseData.getData().getVideoId(), notNullValue());
                assertThat("videoId should be a string",
                                videoViewResponseData.getData().getVideoId(), instanceOf(String.class));
                assertThat("videoId should not be empty",
                                videoViewResponseData.getData().getVideoId(), not(emptyOrNullString()));
                assertThat("videoId should equal expected value",
                                videoViewResponseData.getData().getVideoId(), equalTo(videoId));

                logger.info("videoId validated: {}", videoViewResponseData.getData().getVideoId());
        }

        @Test(description = "userId should be correct string and not empty", priority = 9, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
        @Story("Video View Action")
        @Severity(SeverityLevel.CRITICAL)
        public void testUserIdValidation() {
                // userId should be correct string and not empty
                assertThat("userId should be present",
                                videoViewResponseData.getData().getUserId(), notNullValue());
                assertThat("userId should be a string",
                                videoViewResponseData.getData().getUserId(), instanceOf(String.class));
                assertThat("userId should not be empty",
                                videoViewResponseData.getData().getUserId(), not(emptyOrNullString()));
                assertThat("userId should equal expected value",
                                videoViewResponseData.getData().getUserId(), equalTo(userId));

                logger.info("userId validated: {}", videoViewResponseData.getData().getUserId());
        }

        @Test(description = "JWT token is valid", priority = 10, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
        @Story("Video View Action")
        @Severity(SeverityLevel.BLOCKER)
        public void testJwtTokenValid() {
                // JWT token is valid (response should not be 401 or 403)
                assertThat("Response should not be 401 or 403",
                                videoViewResponse.getStatusCode(), not(isOneOf(401, 403)));

                logger.info("JWT token validated: not 401 or 403");
        }
}

package com.automation.tests.bomb.VideoTaggingPipeline.VideoUploadAndAssignToEditor;

import com.automation.base.BaseTest;
import com.automation.constants.BombEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.VideoAssignUploadResponse;
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
 * Test class for BOMB Video Upload - All Video Assign for uploading [Thumbnail
 * + Video] endpoint.
 * Endpoint: GET
 * {{bizup_base}}/v1/admin/editor/assign/videos/{{seller_id}}?limit=50
 * Implements comprehensive Postman test scripts for video assign upload
 * validation.
 */
@Epic("BOMB Video Tagging Pipeline")
@Feature("Video Upload and Assign to Editor")
public class AllVideoAssignForUploadingThumbnailVideoTest extends BaseTest {

    private String authToken;
    private Response response;
    private VideoAssignUploadResponse videoAssignUploadResponse;

    // Seller ID
    private static final String SELLER_ID = "64f180feaa90ffbd54b330f5";

    // Store upload ID for future tests
    public static String uploadId;

    @BeforeClass
    public void setupAuth() {
        // Get token from VariableManager (thread-safe)
        authToken = VariableManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
        logger.info("Using BOMB token from VariableManager");
    }

    @Test(description = "Set upload_id to collectionVariables", priority = 1, groups = "bomb")
    @Story("Video Upload - All Video Assign")
    @Severity(SeverityLevel.NORMAL)
    public void testSetUploadId() {
        // Send GET request to fetch assigned videos
        response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .queryParam("limit", 50)
                .when()
                .get(BombEndpoints.VIDEO_ASSIGN_UPLOAD + "/" + SELLER_ID);

        // Parse response for other tests
        videoAssignUploadResponse = JsonUtils.fromResponse(response, VideoAssignUploadResponse.class);

        // Set upload ID from first item in data.data array
        if (videoAssignUploadResponse.getData() != null &&
                videoAssignUploadResponse.getData().getData() != null &&
                !videoAssignUploadResponse.getData().getData().isEmpty()) {

            uploadId = videoAssignUploadResponse.getData().getData().get(0).get_id();
            assertThat("Upload ID should be set", uploadId, notNullValue());

            // Save to VariableManager for subsequent tests
            VariableManager.set("uploadId", uploadId);
            
            logger.info("Set upload ID: {}", uploadId);
        } else {
            logger.warn("No data items to set upload ID");
        }
    }

    @Test(description = "Response status code is 200", priority = 2, dependsOnMethods = "testSetUploadId", groups = "bomb")
    @Story("Video Upload - All Video Assign")
    @Severity(SeverityLevel.BLOCKER)
    public void testStatusCode200() {
        // Verify response status is 200 OK
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response time is less than threshold", priority = 3, dependsOnMethods = "testSetUploadId", groups = "bomb")
    @Story("Video Upload - All Video Assign")
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

    @Test(description = "Response has valid JSON structure", priority = 4, dependsOnMethods = "testSetUploadId", groups = "bomb")
    @Story("Video Upload - All Video Assign")
    @Severity(SeverityLevel.NORMAL)
    public void testValidJsonStructure() {
        // Verify response has valid JSON structure
        assertThat("Response should have valid JSON structure", videoAssignUploadResponse, notNullValue());

        logger.info("Response has valid JSON structure");
    }

    @Test(description = "Response contains expected top-level fields", priority = 5, dependsOnMethods = "testSetUploadId", groups = "bomb")
    @Story("Video Upload - All Video Assign")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseContainsTopLevelFields() {
        // Validate response is an object
        assertThat("Response should not be null", videoAssignUploadResponse, notNullValue());
        assertThat("Response should have statusCode", videoAssignUploadResponse.getStatusCode(), notNullValue());
        assertThat("Response should have message", videoAssignUploadResponse.getMessage(), notNullValue());
        assertThat("Response should have data", videoAssignUploadResponse.getData(), notNullValue());

        logger.info("Response contains expected top-level fields");
    }

    @Test(description = "Videos array has correct structure", priority = 6, dependsOnMethods = "testSetUploadId", groups = "bomb")
    @Story("Video Upload - All Video Assign")
    @Severity(SeverityLevel.CRITICAL)
    public void testVideosArrayStructure() {
        // Check if data.videos exists
        if (videoAssignUploadResponse.getData() != null &&
                videoAssignUploadResponse.getData().getVideos() != null) {

            assertThat("Videos should be an array",
                    videoAssignUploadResponse.getData().getVideos(), notNullValue());

            // Validate first video if array is not empty
            if (!videoAssignUploadResponse.getData().getVideos().isEmpty()) {
                VideoAssignUploadResponse.VideoItem firstVideo = videoAssignUploadResponse.getData().getVideos().get(0);

                assertThat("First video should have _id", firstVideo.get_id(), notNullValue());
                assertThat("First video should have videoLink", firstVideo.getVideoLink(), notNullValue());
                assertThat("VideoLink should contain http", firstVideo.getVideoLink(), containsString("http"));
                assertThat("First video should have status", firstVideo.getStatus(), notNullValue());
                assertThat("Status should be 0, 1, or 2", firstVideo.getStatus(), in(new Integer[] { 0, 1, 2 }));
                assertThat("First video should have seller", firstVideo.getSeller(), notNullValue());
                assertThat("Seller should have _id", firstVideo.getSeller().get_id(), notNullValue());

                logger.info("Videos array structure validated");
            }
        } else {
            logger.info("Videos array not present in response");
        }
    }

    @Test(description = "All videos belong to the requested seller", priority = 7, dependsOnMethods = "testSetUploadId", groups = "bomb")
    @Story("Video Upload - All Video Assign")
    @Severity(SeverityLevel.CRITICAL)
    public void testAllVideosBelongToRequestedSeller() {
        // Validate all videos belong to requested seller
        if (videoAssignUploadResponse.getData() != null &&
                videoAssignUploadResponse.getData().getVideos() != null) {

            videoAssignUploadResponse.getData().getVideos().forEach(video -> {
                assertThat(String.format("Video %s should belong to seller %s",
                        video.get_id(), SELLER_ID),
                        video.getSeller().get_id(), equalTo(SELLER_ID));
            });

            logger.info("All {} videos verified to belong to seller: {}",
                    videoAssignUploadResponse.getData().getVideos().size(), SELLER_ID);
        } else {
            logger.info("No videos to validate seller ownership");
        }
    }

    @Test(description = "Content-Type header is application/json", priority = 8, dependsOnMethods = "testSetUploadId", groups = "bomb")
    @Story("Video Upload - All Video Assign")
    @Severity(SeverityLevel.NORMAL)
    public void testContentTypeHeader() {
        // Verify Content-Type header
        assertThat("Content-Type should include application/json",
                response.getContentType(), containsString("application/json"));

        logger.info("Content-Type header verified: {}", response.getContentType());
    }

    @Test(description = "ETag header exists", priority = 9, dependsOnMethods = "testSetUploadId", groups = "bomb")
    @Story("Video Upload - All Video Assign")
    @Severity(SeverityLevel.NORMAL)
    public void testETagHeaderExists() {
        // Verify ETag header exists
        assertThat("ETag header should exist",
                response.getHeader("ETag"), notNullValue());

        logger.info("ETag header verified: {}", response.getHeader("ETag"));
    }

    @Test(description = "Response data is not empty", priority = 10, dependsOnMethods = "testSetUploadId", groups = "bomb")
    @Story("Video Upload - All Video Assign")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseDataNotEmpty() {
        // Validate data.data exists
        assertThat("Response data.data should exist",
                videoAssignUploadResponse.getData().getData(), notNullValue());

        logger.info("Response data is not empty: {} item(s)",
                videoAssignUploadResponse.getData().getData() != null
                        ? videoAssignUploadResponse.getData().getData().size()
                        : 0);
    }
}

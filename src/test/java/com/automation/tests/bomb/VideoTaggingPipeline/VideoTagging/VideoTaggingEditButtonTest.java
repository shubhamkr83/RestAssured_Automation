package com.automation.tests.bomb.VideoTaggingPipeline.VideoTagging;

import com.automation.base.BaseTest;
import com.automation.constants.BombEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.VideoTaggingEditResponse;
import com.automation.utils.VariableManager;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for BOMB Video Tagging Edit Button endpoint.
 * Endpoint: GET
 * {{bizup_base}}/v1/admin/editor/edit/videos/{{seller_id}}?limit=100
 * Implements comprehensive Postman test scripts for video tagging edit
 * validation.
 */
@Epic("BOMB Video Tagging Pipeline")
@Feature("Video Tagging")
public class VideoTaggingEditButtonTest extends BaseTest {

    private String authToken;
    private Response response;
    private VideoTaggingEditResponse videoTaggingEditResponse;

    // Seller ID
    private static final String SELLER_ID = "63ee780c9689be92acce8f35";

    // Store video ID for future tests
    public static String videoId;

    @BeforeClass
    public void setupAuth() {
        // Get token from VariableManager (thread-safe)
        authToken = VariableManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
        logger.info("Using BOMB token from VariableManager");
    }

    @Test(description = "Set video id to collectionVariables variable", priority = 1, groups = "bomb")
    @Story("Video Tagging - Edit Button")
    @Severity(SeverityLevel.NORMAL)
    public void testSetVideoId() {
        // Send GET request to fetch videos for editing
        response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .queryParam("limit", 100)
                .when()
                .get(BombEndpoints.VIDEOS_BY_SELLER.replace("{sellerId}", SELLER_ID));

        // Parse response for other tests
        videoTaggingEditResponse = JsonUtils.fromResponse(response, VideoTaggingEditResponse.class);

        // Set video ID from first item in data.data array
        if (videoTaggingEditResponse.getData() != null &&
                videoTaggingEditResponse.getData().getData() != null &&
                !videoTaggingEditResponse.getData().getData().isEmpty()) {

            videoId = videoTaggingEditResponse.getData().getData().get(0).get_id();
            assertThat("Video ID should be set", videoId, notNullValue());

            logger.info("Set video ID: {}", videoId);
        } else {
            logger.warn("No data items to set video ID");
        }
    }

    @Test(description = "Status code is 200", priority = 2, dependsOnMethods = "testSetVideoId", groups = "bomb")
    @Story("Video Tagging - Edit Button")
    @Severity(SeverityLevel.BLOCKER)
    public void testStatusCode200() {
        // Verify response status is 200 OK
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response time is less than threshold", priority = 3, dependsOnMethods = "testSetVideoId", groups = "bomb")
    @Story("Video Tagging - Edit Button")
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

    @Test(description = "Response has correct content-type", priority = 4, dependsOnMethods = "testSetVideoId", groups = "bomb")
    @Story("Video Tagging - Edit Button")
    @Severity(SeverityLevel.NORMAL)
    public void testContentTypeHeader() {
        // Verify Content-Type header
        assertThat("Content-Type should include application/json",
                response.getContentType(), containsString("application/json"));

        logger.info("Content-Type header verified: {}", response.getContentType());
    }

    @Test(description = "Response has correct structure", priority = 5, dependsOnMethods = "testSetVideoId", groups = "bomb")
    @Story("Video Tagging - Edit Button")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseStructure() {
        // Validate response is an object
        assertThat("Response should not be null", videoTaggingEditResponse, notNullValue());

        // Validate response has all keys
        assertThat("Response should have statusCode", videoTaggingEditResponse.getStatusCode(), notNullValue());
        assertThat("Response should have message", videoTaggingEditResponse.getMessage(), notNullValue());
        assertThat("Response should have data", videoTaggingEditResponse.getData(), notNullValue());

        // Validate statusCode and message values
        assertThat("StatusCode should be '10000'", videoTaggingEditResponse.getStatusCode(), equalTo("10000"));
        assertThat("Message should be 'success'", videoTaggingEditResponse.getMessage(), equalTo("success"));
        assertThat("Data should be an object", videoTaggingEditResponse.getData(), notNullValue());

        logger.info("Response structure validated");
    }

    @Test(description = "Data object has correct structure", priority = 6, dependsOnMethods = "testSetVideoId", groups = "bomb")
    @Story("Video Tagging - Edit Button")
    @Severity(SeverityLevel.CRITICAL)
    public void testDataObjectStructure() {
        // Validate data has data property
        assertThat("Data should have data property", videoTaggingEditResponse.getData().getData(), notNullValue());

        // Validate data.data is an array
        assertThat("Data.data should be an array",
                videoTaggingEditResponse.getData().getData(), instanceOf(java.util.List.class));

        logger.info("Data object structure validated: {} video(s)",
                videoTaggingEditResponse.getData().getData().size());
    }

    @Test(description = "Content type is video", priority = 7, dependsOnMethods = "testSetVideoId", groups = "bomb")
    @Story("Video Tagging - Edit Button")
    @Severity(SeverityLevel.CRITICAL)
    public void testContentTypeIsVideo() {
        // Validate content type if data is present
        if (videoTaggingEditResponse.getData() != null &&
                videoTaggingEditResponse.getData().getData() != null &&
                !videoTaggingEditResponse.getData().getData().isEmpty()) {

            VideoTaggingEditResponse.VideoItem video = videoTaggingEditResponse.getData().getData().get(0);

            assertThat("ContentType should be 'video'",
                    video.getContentType(), equalTo("video"));

            logger.info("Content type validated: {}", video.getContentType());
        } else {
            logger.info("No video data to validate content type");
        }
    }

    @Test(description = "Media links are valid", priority = 8, dependsOnMethods = "testSetVideoId", groups = "bomb")
    @Story("Video Tagging - Edit Button")
    @Severity(SeverityLevel.CRITICAL)
    public void testMediaLinksValid() {
        // Validate media links if data is present
        if (videoTaggingEditResponse.getData() != null &&
                videoTaggingEditResponse.getData().getData() != null &&
                !videoTaggingEditResponse.getData().getData().isEmpty()) {

            VideoTaggingEditResponse.VideoItem video = videoTaggingEditResponse.getData().getData().get(0);

            // Validate driveLink
            assertThat("DriveLink should match Firebase pattern",
                    video.getDriveLink(), matchesPattern("^https://firebasestorage\\.googleapis\\.com/.+"));

            // Validate thubmbnailDriveLink
            assertThat("ThubmbnailDriveLink should match Firebase pattern",
                    video.getThubmbnailDriveLink(), matchesPattern("^https://firebasestorage\\.googleapis\\.com/.+"));

            logger.info("Media links validated: driveLink={}, thumbnailLink={}",
                    video.getDriveLink(), video.getThubmbnailDriveLink());
        } else {
            logger.info("No video data to validate media links");
        }
    }

    @Test(description = "Seller and editor IDs match expected value", priority = 9, dependsOnMethods = "testSetVideoId", groups = "bomb")
    @Story("Video Tagging - Edit Button")
    @Severity(SeverityLevel.CRITICAL)
    public void testSellerAndEditorIdsMatch() {
        // Validate seller and editor IDs if data is present
        if (videoTaggingEditResponse.getData() != null &&
                videoTaggingEditResponse.getData().getData() != null &&
                !videoTaggingEditResponse.getData().getData().isEmpty()) {

            VideoTaggingEditResponse.VideoItem video = videoTaggingEditResponse.getData().getData().get(0);

            assertThat("Seller should match expected seller ID",
                    video.getSeller(), equalTo(SELLER_ID));
            assertThat("Editor should match expected seller ID",
                    video.getEditor(), equalTo(SELLER_ID));

            logger.info("Seller and editor IDs validated: seller={}, editor={}",
                    video.getSeller(), video.getEditor());
        } else {
            logger.info("No video data to validate seller and editor IDs");
        }
    }

    @Test(description = "Timestamps are valid", priority = 10, dependsOnMethods = "testSetVideoId", groups = "bomb")
    @Story("Video Tagging - Edit Button")
    @Severity(SeverityLevel.NORMAL)
    public void testTimestampsValid() {
        // Validate timestamps if data is present
        if (videoTaggingEditResponse.getData() != null &&
                videoTaggingEditResponse.getData().getData() != null &&
                !videoTaggingEditResponse.getData().getData().isEmpty()) {

            VideoTaggingEditResponse.VideoItem video = videoTaggingEditResponse.getData().getData().get(0);

            // Validate uploadDate
            assertThat("UploadDate should not be null", video.getUploadDate(), notNullValue());
            boolean isUploadDateValid = isValidDate(video.getUploadDate());
            assertThat("UploadDate should be a valid date", isUploadDateValid, is(true));

            // Validate createdAt
            assertThat("CreatedAt should not be null", video.getCreatedAt(), notNullValue());
            boolean isCreatedAtValid = isValidDate(video.getCreatedAt());
            assertThat("CreatedAt should be a valid date", isCreatedAtValid, is(true));

            // Validate updatedAt
            assertThat("UpdatedAt should not be null", video.getUpdatedAt(), notNullValue());
            boolean isUpdatedAtValid = isValidDate(video.getUpdatedAt());
            assertThat("UpdatedAt should be a valid date", isUpdatedAtValid, is(true));

            logger.info("Timestamps validated: uploadDate={}, createdAt={}, updatedAt={}",
                    video.getUploadDate(), video.getCreatedAt(), video.getUpdatedAt());
        } else {
            logger.info("No video data to validate timestamps");
        }
    }

    @Test(description = "Upload ID matches expected pattern", priority = 11, dependsOnMethods = "testSetVideoId", groups = "bomb")
    @Story("Video Tagging - Edit Button")
    @Severity(SeverityLevel.NORMAL)
    public void testUploadIdPattern() {
        // Validate upload ID pattern if data is present
        if (videoTaggingEditResponse.getData() != null &&
                videoTaggingEditResponse.getData().getData() != null &&
                !videoTaggingEditResponse.getData().getData().isEmpty()) {

            VideoTaggingEditResponse.VideoItem video = videoTaggingEditResponse.getData().getData().get(0);

            // Validate uploadId matches MongoDB ObjectId pattern (24 hex characters)
            assertThat("UploadId should match MongoDB ObjectId pattern",
                    video.getUploadId(), matchesPattern("^[a-f0-9]{24}$"));

            logger.info("Upload ID validated: {}", video.getUploadId());
        } else {
            logger.info("No video data to validate upload ID");
        }
    }

    @Test(description = "Language codes are valid", priority = 12, dependsOnMethods = "testSetVideoId", groups = "bomb")
    @Story("Video Tagging - Edit Button")
    @Severity(SeverityLevel.NORMAL)
    public void testLanguageCodesValid() {
        // Validate language codes if data is present
        if (videoTaggingEditResponse.getData() != null &&
                videoTaggingEditResponse.getData().getData() != null &&
                !videoTaggingEditResponse.getData().getData().isEmpty()) {

            VideoTaggingEditResponse.VideoItem video = videoTaggingEditResponse.getData().getData().get(0);

            // Validate language is an array
            assertThat("Language should be an array", video.getLanguage(), notNullValue());
            assertThat("Language should be an array", video.getLanguage(), instanceOf(java.util.List.class));

            // Validate language includes 1 and 2
            assertThat("Language should include 1", video.getLanguage(), hasItem(1));
            assertThat("Language should include 2", video.getLanguage(), hasItem(2));

            logger.info("Language codes validated: {}", video.getLanguage());
        } else {
            logger.info("No video data to validate language codes");
        }
    }

    /**
     * Helper method to validate if a string is a valid date
     */
    private boolean isValidDate(String dateString) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            sdf.setLenient(false);
            sdf.parse(dateString);
            return true;
        } catch (ParseException e) {
            // Try alternative ISO 8601 format
            try {
                SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                sdf2.setLenient(false);
                sdf2.parse(dateString);
                return true;
            } catch (ParseException e2) {
                return false;
            }
        }
    }
}

package com.automation.tests.bomb.VideoTaggingPipeline.VideoUploadAndAssignToEditor;

import com.automation.base.BaseTest;
import com.automation.constants.BombEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.request.VideoThumbnailUploadRequest;
import com.automation.models.response.VideoThumbnailUploadResponse;
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
 * Test class for BOMB Video Upload [Thumbnail + Video] endpoint.
 * Endpoint: POST {{bizup_base}}/v1/admin/editor/upload/videos/{{seller_id}}
 * Implements comprehensive Postman test scripts for video and thumbnail upload
 * validation.
 */
@Epic("BOMB Video Tagging Pipeline")
@Feature("Video Upload and Assign to Editor")
public class UploadThumbnailVideoTest extends BaseTest {

    private String authToken;
    private Response response;
    private VideoThumbnailUploadResponse videoThumbnailUploadResponse;

    // Request data
    private VideoThumbnailUploadRequest requestBody;
    private String sellerId;
    private String sellerPhone;
    private String sellerName;
    private String businessName;
    private String marketId;
    private String videoLink;
    private String thumbnailLink;
    private String description;

    // Upload ID from previous test
    private String uploadId;

    @BeforeClass
    public void setupAuth() {
        // Get token from VariableManager (thread-safe)
        authToken = VariableManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
        logger.info("Using BOMB token from VariableManager");

        // Get seller-related data from VariableManager
        sellerId = VariableManager.getSellerId();
        sellerPhone = VariableManager.get("phoneNumber");
        sellerName = VariableManager.get("seller_name");
        businessName = VariableManager.get("business_name");
        marketId = VariableManager.get("market_id");
        logger.info("Using seller data: sellerId={}, sellerPhone={}, sellerName={}, businessName={}, marketId={}", 
                    sellerId, sellerPhone, sellerName, businessName, marketId);

        // Get video/thumbnail data from VariableManager
        videoLink = VariableManager.get("video_link");
        thumbnailLink = VariableManager.get("thumbnail_link");
        description = VariableManager.get("video_description");
        logger.info("Using video data: videoLink={}, thumbnailLink={}, description={}", videoLink, thumbnailLink, description);

        // Get upload ID from previous test
        String prevUploadId = VariableManager.get("upload_id");
        if (prevUploadId != null) {
            uploadId = prevUploadId;
            logger.info("Using upload ID from VariableManager: {}", uploadId);
        } else {
            // Fallback to a default ID if not available (valid MongoDB ObjectId)
            uploadId = "693a92f840d8d24c390119a0";
            logger.warn("Upload ID not available from previous test, using default: {}", uploadId);
        }

        // Build request body
        requestBody = VideoThumbnailUploadRequest.builder()
                .seller(VideoThumbnailUploadRequest.Seller.builder()
                        ._id(sellerId)
                        .phoneNumber(sellerPhone)
                        .name(sellerName)
                        .businessName(businessName)
                        .marketId(marketId)
                        .build())
                .market(null)
                .videoLink(videoLink)
                .thumbnailLink(thumbnailLink)
                .introVideo(false)
                .uploadId(uploadId)
                .fabricText("")
                .priceText("")
                .description(description)
                .build();
    }

    @Test(description = "Status code is 200", priority = 1, groups = "bomb")
    @Story("Video Upload - Thumbnail + Video")
    @Severity(SeverityLevel.BLOCKER)
    public void testStatusCode200() {
        // Send POST request to upload video and thumbnail
        response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .body(requestBody)
                .when()
                .post(BombEndpoints.VIDEO_THUMBNAIL_UPLOAD + "/" + sellerId);

        // Parse response for other tests
        videoThumbnailUploadResponse = JsonUtils.fromResponse(response, VideoThumbnailUploadResponse.class);

        // Verify response status is 200 OK
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response time is less than threshold", priority = 2, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload - Thumbnail + Video")
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

    @Test(description = "Success message exists", priority = 3, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload - Thumbnail + Video")
    @Severity(SeverityLevel.CRITICAL)
    public void testSuccessMessage() {
        // Validate message property exists
        assertThat("Response should have message", videoThumbnailUploadResponse.getMessage(), notNullValue());

        // Validate message is 'success'
        assertThat("Message should be 'success'",
                videoThumbnailUploadResponse.getMessage(), equalTo("success"));

        logger.info("Success message validated: {}", videoThumbnailUploadResponse.getMessage());
    }

    @Test(description = "Response has correct structure", priority = 4, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload - Thumbnail + Video")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseStructure() {
        // Validate response has message and data
        assertThat("Response should have message", videoThumbnailUploadResponse.getMessage(), notNullValue());
        assertThat("Response should have data", videoThumbnailUploadResponse.getData(), notNullValue());

        VideoThumbnailUploadResponse.VideoThumbnailData data = videoThumbnailUploadResponse.getData();

        // Validate data has all required keys
        assertThat("Data should have _id", data.get_id(), notNullValue());
        assertThat("Data should have seller", data.getSeller(), notNullValue());
        assertThat("Data should have driveLink", data.getDriveLink(), notNullValue());
        assertThat("Data should have thubmbnailDriveLink", data.getThubmbnailDriveLink(), notNullValue());
        assertThat("Data should have contentType", data.getContentType(), notNullValue());
        // status can be null
        assertThat("Data should have createdAt", data.getCreatedAt(), notNullValue());
        assertThat("Data should have updatedAt", data.getUpdatedAt(), notNullValue());

        logger.info("Response structure validated");
    }

    @Test(description = "ContentType is video and status is null", priority = 5, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload - Thumbnail + Video")
    @Severity(SeverityLevel.CRITICAL)
    public void testContentTypeAndStatus() {
        VideoThumbnailUploadResponse.VideoThumbnailData data = videoThumbnailUploadResponse.getData();

        // Validate contentType is 'video'
        assertThat("ContentType should be 'video'",
                data.getContentType(), equalTo("video"));

        // Validate status is null
        assertThat("Status should be null",
                data.getStatus(), nullValue());

        logger.info("ContentType and status validated: contentType={}, status={}",
                data.getContentType(), data.getStatus());
    }

    @Test(description = "Seller ID matches collection variable", priority = 6, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload - Thumbnail + Video")
    @Severity(SeverityLevel.CRITICAL)
    public void testSellerIdMatches() {
        VideoThumbnailUploadResponse.VideoThumbnailData data = videoThumbnailUploadResponse.getData();

        // Validate seller matches expected seller ID
        assertThat("Seller should match expected seller ID",
                data.getSeller(), equalTo(sellerId));

        logger.info("Seller validated: {} matches expected: {}", data.getSeller(), sellerId);
    }

    @Test(description = "Video link is valid", priority = 7, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload - Thumbnail + Video")
    @Severity(SeverityLevel.CRITICAL)
    public void testVideoLinkValid() {
        VideoThumbnailUploadResponse.VideoThumbnailData data = videoThumbnailUploadResponse.getData();

        // Validate driveLink is a string
        assertThat("DriveLink should be a string", data.getDriveLink(), instanceOf(String.class));

        // Validate driveLink contains https://
        assertThat("DriveLink should contain https://",
                data.getDriveLink(), containsString("https://"));

        logger.info("Video link validated: {}", data.getDriveLink());
    }

    @Test(description = "Thumbnail link is valid", priority = 8, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload - Thumbnail + Video")
    @Severity(SeverityLevel.CRITICAL)
    public void testThumbnailLinkValid() {
        VideoThumbnailUploadResponse.VideoThumbnailData data = videoThumbnailUploadResponse.getData();

        // Validate thubmbnailDriveLink is a string
        assertThat("ThubmbnailDriveLink should be a string",
                data.getThubmbnailDriveLink(), instanceOf(String.class));

        // Validate thubmbnailDriveLink contains https://
        assertThat("ThubmbnailDriveLink should contain https://",
                data.getThubmbnailDriveLink(), containsString("https://"));

        logger.info("Thumbnail link validated: {}", data.getThubmbnailDriveLink());
    }

    @Test(description = "CreatedAt and UpdatedAt are valid dates", priority = 9, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload - Thumbnail + Video")
    @Severity(SeverityLevel.NORMAL)
    public void testTimestampsValid() {
        VideoThumbnailUploadResponse.VideoThumbnailData data = videoThumbnailUploadResponse.getData();

        // Validate createdAt is a valid date
        assertThat("CreatedAt should not be null", data.getCreatedAt(), notNullValue());
        boolean isCreatedAtValid = isValidDate(data.getCreatedAt());
        assertThat("CreatedAt should be a valid date", isCreatedAtValid, is(true));

        // Validate updatedAt is a valid date
        assertThat("UpdatedAt should not be null", data.getUpdatedAt(), notNullValue());
        boolean isUpdatedAtValid = isValidDate(data.getUpdatedAt());
        assertThat("UpdatedAt should be a valid date", isUpdatedAtValid, is(true));

        logger.info("Timestamps validated: createdAt={}, updatedAt={}",
                data.getCreatedAt(), data.getUpdatedAt());
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

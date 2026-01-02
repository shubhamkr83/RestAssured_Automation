package com.automation.tests.bomb.VideoTaggingPipeline.VideoUploadAndAssignToEditor;

import com.automation.base.BaseTest;
import com.automation.constants.BombEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.VideoUploadMarkAsDoneResponse;
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
 * Test class for BOMB Video Upload Mark As Done endpoint.
 * Endpoint: PUT
 * {{bizup_base}}/v1/admin/editor/assign/videos/done/{{seller_id}}/{{upload_id}}
 * Implements comprehensive Postman test scripts for marking video upload as
 * done validation.
 */
@Epic("BOMB Video Tagging Pipeline")
@Feature("Video Upload and Assign to Editor")
public class VideoUploadMarkAsDoneTest extends BaseTest {

    private String authToken;
    private Response response;
    private VideoUploadMarkAsDoneResponse videoUploadMarkAsDoneResponse;

    // IDs
    private String sellerId;
    private String uploadId;

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
        
        // Get upload ID from previous test
        String prevUploadId = VariableManager.get("upload_id");
        if (prevUploadId != null) {
            uploadId = prevUploadId;
            logger.info("Using upload ID from previous test: {}", uploadId);
        } else {
            // Fallback to a default ID if not available (valid MongoDB ObjectId)
            uploadId = "693a92f840d8d24c390119a0";
            logger.warn("Upload ID not available from previous test, using default: {}", uploadId);
        }
    }

    @Test(description = "Status code is 200", priority = 1, groups = "bomb")
    @Story("Video Upload - Mark As Done")
    @Severity(SeverityLevel.BLOCKER)
    public void testStatusCode200() {
        // Send PUT request to mark video upload as done
        response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .when()
                .post(BombEndpoints.VIDEO_UPLOAD_DONE + "/" + sellerId + "/" + uploadId);

        // Parse response for other tests
        videoUploadMarkAsDoneResponse = JsonUtils.fromResponse(response, VideoUploadMarkAsDoneResponse.class);

        // Verify response status is 200 OK
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response time is less than threshold", priority = 2, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload - Mark As Done")
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

    @Test(description = "Response has correct content-type", priority = 3, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload - Mark As Done")
    @Severity(SeverityLevel.NORMAL)
    public void testContentTypeHeader() {
        // Verify Content-Type header
        assertThat("Content-Type should include application/json",
                response.getContentType(), containsString("application/json"));

        logger.info("Content-Type header verified: {}", response.getContentType());
    }

    @Test(description = "Success message exists", priority = 4, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload - Mark As Done")
    @Severity(SeverityLevel.CRITICAL)
    public void testSuccessMessage() {
        // Validate message property exists
        assertThat("Response should have message", videoUploadMarkAsDoneResponse.getMessage(), notNullValue());

        // Validate message is a string and not empty
        assertThat("Message should be a string",
                videoUploadMarkAsDoneResponse.getMessage(), instanceOf(String.class));
        assertThat("Message should not be empty",
                videoUploadMarkAsDoneResponse.getMessage(), not(emptyString()));

        // Validate message includes 'success'
        assertThat("Message should include 'success'",
                videoUploadMarkAsDoneResponse.getMessage(), containsString("success"));

        logger.info("Success message validated: {}", videoUploadMarkAsDoneResponse.getMessage());
    }

    @Test(description = "Response has correct structure", priority = 5, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload - Mark As Done")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseStructure() {
        // Validate response has message and data
        assertThat("Response should have message", videoUploadMarkAsDoneResponse.getMessage(), notNullValue());
        assertThat("Response should have data", videoUploadMarkAsDoneResponse.getData(), notNullValue());

        VideoUploadMarkAsDoneResponse.VideoUploadDoneData data = videoUploadMarkAsDoneResponse.getData();

        // Validate data has all required keys
        assertThat("Data should have _id", data.get_id(), notNullValue());
        assertThat("Data should have sellerId", data.getSellerId(), notNullValue());
        assertThat("Data should have phoneNumber", data.getPhoneNumber(), notNullValue());
        assertThat("Data should have url", data.getUrl(), notNullValue());
        assertThat("Data should have assignedBy", data.getAssignedBy(), notNullValue());
        assertThat("Data should have editorId", data.getEditorId(), notNullValue());
        assertThat("Data should have uploadedBy", data.getUploadedBy(), notNullValue());
        assertThat("Data should have status", data.getStatus(), notNullValue());
        assertThat("Data should have createdAt", data.getCreatedAt(), notNullValue());
        assertThat("Data should have updatedAt", data.getUpdatedAt(), notNullValue());

        logger.info("Response structure validated");
    }

    @Test(description = "Status is updated to 'done'", priority = 6, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload - Mark As Done")
    @Severity(SeverityLevel.CRITICAL)
    public void testStatusUpdatedToDone() {
        VideoUploadMarkAsDoneResponse.VideoUploadDoneData data = videoUploadMarkAsDoneResponse.getData();

        // Validate status is 1 (done)
        assertThat("Status should be 1 (done)",
                data.getStatus(), equalTo(1));

        logger.info("Status validated: {}", data.getStatus());
    }

    @Test(description = "VideoType remains 'video'", priority = 7, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload - Mark As Done")
    @Severity(SeverityLevel.NORMAL)
    public void testVideoTypeRemains() {
        VideoUploadMarkAsDoneResponse.VideoUploadDoneData data = videoUploadMarkAsDoneResponse.getData();

        // Validate videoType is 'Video'
        assertThat("VideoType should be 'Video'",
                data.getVideoType(), equalTo("Video"));

        logger.info("VideoType validated: {}", data.getVideoType());
    }

    @Test(description = "Seller ID matches collection variable", priority = 8, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload - Mark As Done")
    @Severity(SeverityLevel.CRITICAL)
    public void testSellerIdMatches() {
        VideoUploadMarkAsDoneResponse.VideoUploadDoneData data = videoUploadMarkAsDoneResponse.getData();

        // Validate sellerId matches expected seller ID
        assertThat("SellerId should match expected seller ID",
                data.getSellerId(), equalTo(sellerId));

        logger.info("SellerId validated: {} matches expected: {}", data.getSellerId(), sellerId);
    }

    @Test(description = "Uploaded ID matches expected value", priority = 9, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload - Mark As Done")
    @Severity(SeverityLevel.CRITICAL)
    public void testUploadedIdMatches() {
        VideoUploadMarkAsDoneResponse.VideoUploadDoneData data = videoUploadMarkAsDoneResponse.getData();

        // Validate _id matches expected upload ID
        assertThat("Upload ID should match expected upload ID",
                data.get_id(), equalTo(uploadId));

        logger.info("Upload ID validated: {} matches expected: {}", data.get_id(), uploadId);
    }

    @Test(description = "Video and thumbnail links are valid", priority = 10, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload - Mark As Done")
    @Severity(SeverityLevel.CRITICAL)
    public void testVideoLinksValid() {
        VideoUploadMarkAsDoneResponse.VideoUploadDoneData data = videoUploadMarkAsDoneResponse.getData();

        // Validate url is a string
        assertThat("URL should be a string", data.getUrl(), instanceOf(String.class));

        // Validate url contains https://
        assertThat("URL should contain https://",
                data.getUrl(), containsString("https://"));

        logger.info("Video URL validated: {}", data.getUrl());
    }

    @Test(description = "URL parameters match response data", priority = 11, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload - Mark As Done")
    @Severity(SeverityLevel.CRITICAL)
    public void testUrlParametersMatch() {
        VideoUploadMarkAsDoneResponse.VideoUploadDoneData data = videoUploadMarkAsDoneResponse.getData();

        // Validate sellerId from URL matches response
        assertThat("SellerId from URL should match response data",
                data.getSellerId(), equalTo(sellerId));

        // Validate upload ID from URL matches response
        assertThat("Upload ID from URL should match response data",
                data.get_id(), equalTo(uploadId));

        logger.info("URL parameters validated: sellerId={}, uploadId={}", sellerId, uploadId);
    }
}

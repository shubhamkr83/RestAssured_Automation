package com.automation.tests.bomb.VideoTaggingPipeline.VideoUploadAndAssignToEditor;

import com.automation.base.BaseTest;
import com.automation.constants.BombEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.request.VideoUploadRequest;
import com.automation.models.response.VideoUploadResponse;
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
 * Test class for BOMB Video Upload endpoint.
 * Endpoint: POST {{bizup_base}}/v1/admin/pipeline/video
 * Implements comprehensive Postman test scripts for video upload validation.
 */
@Epic("BOMB Video Tagging Pipeline")
@Feature("Video Upload and Assign to Editor")
public class VideoUploadTest extends BaseTest {

    private String authToken;
    private Response response;
    private VideoUploadResponse videoUploadResponse;

    // Request data
    private VideoUploadRequest requestBody;
    private static final String SELLER_ID = "63ee780c9689be92acce8f35";
    private static final String SELLER_PHONE = "+916204843730";
    private static final String SELLER_NAME = "Shubham Kr (Test Shop)";
    private static final String BUSINESS_NAME = "SK Shop (Test)";
    private static final String SELLER_CREATED_AT = "2023-09-01T06:13:18.149Z";
    private static final String MARKET_ID = "6454921e6144f73eceac9de5";
    private static final String SELLER_LABEL = "SK Shop (Test) (+916204843730)";
    private static final String SELLER_VALUE = "64f180feaa90ffbd54b330f5";

    private static final String EDITOR_ID = "652e699e42e117518ebb86cd";
    private static final String EDITOR_NAME = "Shubham Kumar";
    private static final String EDITOR_PHONE = "+916204843730";
    private static final String EDITOR_USER_ID = "64f180feaa90ffbd54b330f5";
    private static final String EDITOR_LABEL = "Shubham Kumar";
    private static final String EDITOR_VALUE = "64f180feaa90ffbd54b330f5";

    private static final String VIDEO_LINK = "https://firebasestorage.googleapis.com/v0/b/bizup-3df17.appspot.com/o/editor%2Fvideo%2F1ba37ead-ca46-45a6-bf93-2966c55e999f.mp4?alt=media&token=e0ee167d-e7eb-4428-92ed-c97b39215010";
    private static final String VIDEO_TYPE = "Video";
    private static final String DESCRIPTION = "Test";

    @BeforeClass
    public void setupAuth() {
        // Get token from VariableManager (thread-safe)
        authToken = VariableManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
        logger.info("Using BOMB token from VariableManager");

        // Build request body with complete seller and editor information
        requestBody = VideoUploadRequest.builder()
                .videoLink(VIDEO_LINK)
                .videoType(VIDEO_TYPE)
                .description(DESCRIPTION)
                .market(null)
                .seller(VideoUploadRequest.Seller.builder()
                        ._id(SELLER_ID)
                        .phoneNumber(SELLER_PHONE)
                        .name(SELLER_NAME)
                        .businessName(BUSINESS_NAME)
                        .createdAt(SELLER_CREATED_AT)
                        .marketId(MARKET_ID)
                        .smell_test(50)
                        .assigned_score(0)
                        .deprioritisation_status(false)
                        .label(SELLER_LABEL)
                        .value(SELLER_VALUE)
                        .build())
                .editor(VideoUploadRequest.Editor.builder()
                        ._id(EDITOR_ID)
                        .name(EDITOR_NAME)
                        .role(java.util.Arrays.asList(
                                "editor_review", "admin", "editor_catalog", "editor_video",
                                "pipeline_online", "pipeline_onground", "pipeline_wa",
                                "manage_seller", "manage_buyer", "salesman", "whatsapp_message"))
                        .status(1)
                        .phoneNumber(EDITOR_PHONE)
                        .user_id(EDITOR_USER_ID)
                        .label(EDITOR_LABEL)
                        .value(EDITOR_VALUE)
                        .build())
                .build();
    }

    @Test(description = "Response status code is 200", priority = 1, groups = "bomb")
    @Story("Video Upload")
    @Severity(SeverityLevel.BLOCKER)
    public void testStatusCode200() {
        // Send POST request to upload video
        response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .body(requestBody)
                .when()
                .post(BombEndpoints.VIDEO_UPLOAD);

        // Parse response for other tests
        videoUploadResponse = JsonUtils.fromResponse(response, VideoUploadResponse.class);

        // Verify response status is 200 OK (or 201 Created)
        assertThat("Status code should be 200 or 201",
                response.getStatusCode(), anyOf(equalTo(HttpStatus.OK), equalTo(HttpStatus.CREATED)));

        logger.info("Response status verified: {}", response.getStatusCode());
    }

    @Test(description = "Response time is less than threshold", priority = 2, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload")
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

    @Test(description = "Response has valid JSON structure", priority = 3, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload")
    @Severity(SeverityLevel.NORMAL)
    public void testValidJsonStructure() {
        // Verify response has valid JSON structure
        assertThat("Response should have valid JSON structure", videoUploadResponse, notNullValue());

        logger.info("Response has valid JSON structure");
    }

    @Test(description = "Response contains expected fields", priority = 4, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseContainsExpectedFields() {
        // Validate response is an object
        assertThat("Response should not be null", videoUploadResponse, notNullValue());
        assertThat("Response should have statusCode", videoUploadResponse.getStatusCode(), notNullValue());
        assertThat("Response should have message", videoUploadResponse.getMessage(), notNullValue());

        // For 201 Created, validate data fields
        if (response.getStatusCode() == HttpStatus.CREATED && videoUploadResponse.getData() != null) {
            VideoUploadResponse.VideoUploadData data = videoUploadResponse.getData();
            assertThat("Data should have _id", data.get_id(), notNullValue());
            assertThat("Data should have videoLink matching request", data.getVideoLink(), equalTo(VIDEO_LINK));
            assertThat("Data should have videoType matching request", data.getVideoType(), equalTo(VIDEO_TYPE));
            assertThat("Data should have status 0", data.getStatus(), equalTo(0));

            logger.info("Response data fields validated for 201 Created");
        }

        logger.info("Response contains expected fields");
    }

    @Test(description = "Seller information matches request", priority = 5, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload")
    @Severity(SeverityLevel.CRITICAL)
    public void testSellerInfoMatchesRequest() {
        // Validate seller information if present
        if (videoUploadResponse.getData() != null && videoUploadResponse.getData().getSeller() != null) {
            VideoUploadResponse.Seller seller = videoUploadResponse.getData().getSeller();

            assertThat("Seller _id should match request", seller.get_id(), equalTo(SELLER_ID));
            assertThat("Seller phoneNumber should match request", seller.getPhoneNumber(), equalTo(SELLER_PHONE));

            logger.info("Seller information validated: _id={}, phoneNumber={}", seller.get_id(),
                    seller.getPhoneNumber());
        } else {
            logger.info("Seller information not present in response");
        }
    }

    @Test(description = "Editor information matches request", priority = 6, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload")
    @Severity(SeverityLevel.CRITICAL)
    public void testEditorInfoMatchesRequest() {
        // Validate editor information if present
        if (videoUploadResponse.getData() != null && videoUploadResponse.getData().getEditor() != null) {
            VideoUploadResponse.Editor editor = videoUploadResponse.getData().getEditor();

            assertThat("Editor _id should match request", editor.get_id(), equalTo(EDITOR_ID));
            assertThat("Editor name should match request", editor.getName(), equalTo(EDITOR_NAME));

            logger.info("Editor information validated: _id={}, name={}", editor.get_id(), editor.getName());
        } else {
            logger.info("Editor information not present in response");
        }
    }

    @Test(description = "Content-Type header is application/json", priority = 7, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload")
    @Severity(SeverityLevel.NORMAL)
    public void testContentTypeHeader() {
        // Verify Content-Type header
        assertThat("Content-Type should include application/json",
                response.getContentType(), containsString("application/json"));

        logger.info("Content-Type header verified: {}", response.getContentType());
    }

    @Test(description = "Success message is returned", priority = 8, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload")
    @Severity(SeverityLevel.NORMAL)
    public void testSuccessMessage() {
        // Validate statusCode is a string
        assertThat("StatusCode should be a string",
                videoUploadResponse.getStatusCode(), instanceOf(String.class));

        // Validate message is 'success'
        assertThat("Message should be 'success'",
                videoUploadResponse.getMessage(), equalTo("success"));

        logger.info("Success message validated: statusCode={}, message={}",
                videoUploadResponse.getStatusCode(), videoUploadResponse.getMessage());
    }

    @Test(description = "Video processing status is valid", priority = 9, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload")
    @Severity(SeverityLevel.NORMAL)
    public void testVideoProcessingStatus() {
        // Validate video processing status if present
        if (videoUploadResponse.getData() != null && videoUploadResponse.getData().getStatus() != null) {
            Integer status = videoUploadResponse.getData().getStatus();

            assertThat("Status should be a number", status, instanceOf(Integer.class));
            assertThat("Status should be 0, 1, or 2", status, in(new Integer[] { 0, 1, 2 }));

            logger.info("Video processing status validated: {}", status);
        } else {
            logger.info("Video processing status not present in response");
        }
    }

    @Test(description = "Video link is properly stored", priority = 10, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload")
    @Severity(SeverityLevel.CRITICAL)
    public void testVideoLinkProperlyStored() {
        // Check if data.url exists
        if (videoUploadResponse.getData() != null && videoUploadResponse.getData().getUrl() != null) {
            String url = videoUploadResponse.getData().getUrl();
            assertThat("URL should be a string", url, instanceOf(String.class));

            logger.info("Video URL validated: {}", url);
        }

        // Check if data.videoLink exists
        if (videoUploadResponse.getData() != null && videoUploadResponse.getData().getVideoLink() != null) {
            String videoLink = videoUploadResponse.getData().getVideoLink();

            assertThat("VideoLink should include Firebase domain",
                    videoLink, containsString("firebasestorage.googleapis.com"));
            assertThat("VideoLink should include .mp4",
                    videoLink.toLowerCase(), containsString(".mp4"));
            assertThat("VideoLink should match request",
                    videoLink, equalTo(VIDEO_LINK));

            logger.info("Video link validated: {}", videoLink);
        } else if (videoUploadResponse.getVideoLink() != null) {
            // Check root level videoLink
            String videoLink = videoUploadResponse.getVideoLink();

            assertThat("VideoLink should be a string", videoLink, instanceOf(String.class));
            assertThat("VideoLink should include Firebase domain",
                    videoLink, containsString("firebasestorage.googleapis.com"));

            logger.info("Root level video link validated: {}", videoLink);
        }
    }

    @Test(description = "Video link exists in response", priority = 11, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload")
    @Severity(SeverityLevel.CRITICAL)
    public void testVideoLinkExists() {
        // Get video link from data.url
        String videoLink = videoUploadResponse.getData() != null
                ? videoUploadResponse.getData().getUrl()
                : null;

        // Validate video link
        assertThat("Video link should exist", videoLink, notNullValue());
        assertThat("Video link should be a string", videoLink, instanceOf(String.class));
        assertThat("Video link should contain Firebase domain",
                videoLink, containsString("firebasestorage.googleapis.com"));

        logger.info("Video link exists and validated: {}", videoLink);
    }
}

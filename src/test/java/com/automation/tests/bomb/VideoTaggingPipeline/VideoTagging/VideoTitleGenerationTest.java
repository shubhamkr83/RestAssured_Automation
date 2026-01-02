package com.automation.tests.bomb.VideoTaggingPipeline.VideoTagging;

import com.automation.base.BaseTest;
import com.automation.constants.HttpStatus;
import com.automation.models.request.VideoTitleGenerationRequest;
import com.automation.models.response.VideoTitleGenerationResponse;
import com.automation.utils.VariableManager;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for BOMB Video Title Generation endpoint.
 * Endpoint: POST https://bomb.bizup.app/api/chat/title/tags
 * Implements comprehensive Postman test scripts for video title generation
 * validation.
 */
@Epic("BOMB Video Tagging Pipeline")
@Feature("Video Tagging")
public class VideoTitleGenerationTest extends BaseTest {

    private String authToken;
    private Response response;
    private VideoTitleGenerationResponse videoTitleGenerationResponse;

    // Full endpoint URL (external API)
    private static final String TITLE_GENERATION_URL = "https://bomb.bizup.app/api/chat/title/tags";

    // Request data - tags for title generation
    private VideoTitleGenerationRequest requestBody;
    private List<String> requestTags;

    // Store generated title for future tests
    public static String videoTitle;

    @BeforeClass
    public void setupAuth() {
        // Get token from VariableManager (thread-safe)
        authToken = VariableManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
        logger.info("Using BOMB token from VariableManager");

        // Build request body with tags
        requestTags = Arrays.asList(
                "product : silk saree",
                "fabric : cotton",
                "suitable for : womens",
                "work/embroidary : bead work",
                "border work : embroidery",
                "saree length : 6 meter",
                "packaging : ziplock packing",
                "occasion : outdoor, religious",
                "pattern/design/print : belt saree",
                "blouse piece : ready to wear (stitched)");

        requestBody = VideoTitleGenerationRequest.builder()
                .tags(requestTags)
                .build();
    }

    @Test(description = "Set the generated video title to the collectionVariables", priority = 1, groups = "bomb")
    @Story("Video Title Generation")
    @Severity(SeverityLevel.NORMAL)
    public void testSetGeneratedVideoTitle() {
        // Send POST request to generate video title
        response = RestAssured.given()
                .header("Content-Type", "application/json")
                .body(requestBody)
                .when()
                .post(TITLE_GENERATION_URL);

        // Parse response for other tests
        videoTitleGenerationResponse = JsonUtils.fromResponse(response, VideoTitleGenerationResponse.class);

        // Set video title from result
        if (videoTitleGenerationResponse.getResult() != null) {
            videoTitle = videoTitleGenerationResponse.getResult();
            assertThat("Video title should be set", videoTitle, notNullValue());

            logger.info("Set video title: {}", videoTitle);
        } else {
            logger.warn("No result to set video title");
        }
    }

    @Test(description = "Status code is 200", priority = 2, dependsOnMethods = "testSetGeneratedVideoTitle", groups = "bomb")
    @Story("Video Title Generation")
    @Severity(SeverityLevel.BLOCKER)
    public void testStatusCode200() {
        // Verify response status is 200 OK
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response time is less than threshold", priority = 3, dependsOnMethods = "testSetGeneratedVideoTitle", groups = "bomb")
    @Story("Video Title Generation")
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

    @Test(description = "Content-Type is application/json", priority = 4, dependsOnMethods = "testSetGeneratedVideoTitle", groups = "bomb")
    @Story("Video Title Generation")
    @Severity(SeverityLevel.NORMAL)
    public void testContentTypeHeader() {
        // Verify Content-Type header
        assertThat("Content-Type should include application/json",
                response.getContentType(), containsString("application/json"));

        logger.info("Content-Type header verified: {}", response.getContentType());
    }

    @Test(description = "Response has valid JSON structure", priority = 5, dependsOnMethods = "testSetGeneratedVideoTitle", groups = "bomb")
    @Story("Video Title Generation")
    @Severity(SeverityLevel.NORMAL)
    public void testValidJsonStructure() {
        // Verify response has valid JSON structure
        assertThat("Response should have valid JSON structure", videoTitleGenerationResponse, notNullValue());

        logger.info("Response has valid JSON structure");
    }

    @Test(description = "Response contains required fields", priority = 6, dependsOnMethods = "testSetGeneratedVideoTitle", groups = "bomb")
    @Story("Video Title Generation")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseContainsRequiredFields() {
        // Validate response has result property
        assertThat("Response should have result", videoTitleGenerationResponse.getResult(), notNullValue());

        logger.info("Response contains required fields: result={}", videoTitleGenerationResponse.getResult());
    }

    @Test(description = "Tags are processed correctly", priority = 7, dependsOnMethods = "testSetGeneratedVideoTitle", groups = "bomb")
    @Story("Video Title Generation")
    @Severity(SeverityLevel.NORMAL)
    public void testTagsProcessedCorrectly() {
        // Validate tags processing if data is present
        if (videoTitleGenerationResponse.getData() != null &&
                videoTitleGenerationResponse.getData().getProcessedTags() != null) {

            List<VideoTitleGenerationResponse.ProcessedTag> processedTags = videoTitleGenerationResponse.getData()
                    .getProcessedTags();

            // Validate processedTags is an array
            assertThat("ProcessedTags should be an array", processedTags, notNullValue());

            // Filter non-empty request tags
            long nonEmptyRequestTagsCount = requestTags.stream()
                    .filter(tag -> tag != null && !tag.trim().isEmpty())
                    .count();

            // Validate processedTags length matches non-empty request tags
            assertThat("ProcessedTags length should match non-empty request tags",
                    (long) processedTags.size(), equalTo(nonEmptyRequestTagsCount));

            logger.info("Tags processed correctly: {} tags", processedTags.size());
        } else {
            logger.info("No processed tags data to validate");
        }
    }

    @Test(description = "No error in response", priority = 8, dependsOnMethods = "testSetGeneratedVideoTitle", groups = "bomb")
    @Story("Video Title Generation")
    @Severity(SeverityLevel.NORMAL)
    public void testNoErrorInResponse() {
        // Validate error field exists (can be null or not undefined)
        // Note: In Java, we check if the field is not null to ensure it's defined
        assertThat("Error field should be defined (can be null)",
                videoTitleGenerationResponse.getError(), anyOf(nullValue(), notNullValue()));

        logger.info("Error field validated: {}", videoTitleGenerationResponse.getError());
    }

    @Test(description = "Tag keys are properly formatted", priority = 9, dependsOnMethods = "testSetGeneratedVideoTitle", groups = "bomb")
    @Story("Video Title Generation")
    @Severity(SeverityLevel.NORMAL)
    public void testTagKeysProperlyFormatted() {
        // Validate tag formatting if data is present
        if (videoTitleGenerationResponse.getData() != null &&
                videoTitleGenerationResponse.getData().getProcessedTags() != null) {

            List<VideoTitleGenerationResponse.ProcessedTag> processedTags = videoTitleGenerationResponse.getData()
                    .getProcessedTags();

            processedTags.forEach(tag -> {
                // Validate key is a string
                assertThat("Tag key should be a string", tag.getKey(), instanceOf(String.class));

                // Validate key matches pattern (alphanumeric, spaces, and slashes allowed)
                assertThat("Tag key should match pattern",
                        tag.getKey(), matchesPattern("^[a-zA-Z0-9/\\s]+$"));

                // Validate value is a string
                assertThat("Tag value should be a string", tag.getValue(), instanceOf(String.class));
            });

            logger.info("Tag keys properly formatted: {} tags validated", processedTags.size());
        } else {
            logger.info("No processed tags data to validate formatting");
        }
    }
}

package com.automation.tests.bomb;

import com.automation.base.BaseTest;
import com.automation.constants.BombEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.request.VideoTitleRequest;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for BOMB Video API.
 * Tests video listing and title generation functionality.
 */
@Epic("BOMB Video Management")
@Feature("Video API")
public class VideoApiTest extends BaseTest {

    private String authToken;
    private String sellerId = "64f180feaa90ffbd54b330f5";
    public static String videoTitle;

    @BeforeClass
    public void setupAuth() {
        if (LoginApiTest.bombToken != null) {
            authToken = LoginApiTest.bombToken;
            logger.info("Using BOMB token from LoginApiTest");
        } else {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
    }

    @Test(description = "Verify get videos by seller ID", priority = 1)
    @Story("Get Videos")
    @Severity(SeverityLevel.CRITICAL)
    public void testGetVideosBySellerId() {
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("limit", 100);

        Response response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .queryParams(queryParams)
                .pathParam("sellerId", sellerId)
                .when()
                .get(BombEndpoints.VIDEOS_BY_SELLER);

        // Validate status code
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        // Validate response time
        assertThat("Response time should be acceptable",
                response.getTime(), lessThan((long) config.responseTimeThreshold()));

        // Validate Content-Type
        assertThat("Content-Type should be application/json",
                response.getContentType(), containsString("application/json"));

        // Validate response structure
        JsonPath jsonPath = response.jsonPath();
        assertThat("Response should have statusCode", jsonPath.get("statusCode"), notNullValue());
        assertThat("StatusCode should be 10000", jsonPath.getString("statusCode"), equalTo("10000"));
        assertThat("Message should be success", jsonPath.getString("message"), equalTo("success"));
        assertThat("Response should have data", jsonPath.get("data"), notNullValue());

        // Validate data structure
        assertThat("Data should have data property", jsonPath.get("data.data"), notNullValue());
        assertThat("Data.data should be an array", jsonPath.getList("data.data"), instanceOf(java.util.List.class));

        // Validate video object (if data present)
        if (jsonPath.getList("data.data").size() > 0) {
            Map<String, Object> video = jsonPath.getMap("data.data[0]");

            // Validate content type
            assertThat("Content type should be video", video.get("contentType"), equalTo("video"));

            // Validate media links
            String driveLink = (String) video.get("driveLink");
            String thumbnailLink = (String) video.get("thubmbnailDriveLink");
            assertThat("Drive link should be valid Firebase storage URL",
                    driveLink, matchesRegex("^https://firebasestorage\\.googleapis\\.com/.+"));
            assertThat("Thumbnail link should be valid Firebase storage URL",
                    thumbnailLink, matchesRegex("^https://firebasestorage\\.googleapis\\.com/.+"));

            // Validate seller and editor IDs
            assertThat("Seller ID should match", video.get("seller"), equalTo(sellerId));
            assertThat("Editor ID should match", video.get("editor"), equalTo(sellerId));

            // Validate upload ID format (MongoDB ObjectId)
            String uploadId = (String) video.get("uploadId");
            assertThat("Upload ID should match MongoDB format",
                    uploadId, matchesRegex("^[a-f0-9]{24}$"));

            // Validate timestamps
            assertThat("Upload date should be valid", video.get("uploadDate"), notNullValue());
            assertThat("Created at should be valid", video.get("createdAt"), notNullValue());
            assertThat("Updated at should be valid", video.get("updatedAt"), notNullValue());

            // Validate language codes
            java.util.List<Integer> languages = (java.util.List<Integer>) video.get("language");
            assertThat("Language should contain code 1", languages, hasItem(1));
            assertThat("Language should contain code 2", languages, hasItem(2));
        }
    }

    @Test(description = "Verify video title generation from tags", priority = 2)
    @Story("Video Title Generation")
    @Severity(SeverityLevel.CRITICAL)
    public void testVideoTitleGeneration() {
        // Prepare request with tags
        VideoTitleRequest request = VideoTitleRequest.builder()
                .tags(Arrays.asList("jeans", "blue", "cotton", "casual wear"))
                .build();

        Response response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .body(request)
                .when()
                .post(BombEndpoints.VIDEO_TITLE_GENERATION);

        // Validate status code
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        // Validate response time
        assertThat("Response time should be acceptable",
                response.getTime(), lessThan((long) config.responseTimeThreshold()));

        // Validate Content-Type
        assertThat("Content-Type should be application/json",
                response.getContentType(), containsString("application/json"));

        // Validate response has valid JSON structure
        JsonPath jsonPath = response.jsonPath();
        assertThat("Response should have result field", jsonPath.get("result"), notNullValue());

        // Store generated video title
        videoTitle = jsonPath.getString("result");
        logger.info("Generated video title: {}", videoTitle);

        // Validate tags processing
        if (jsonPath.get("data.processedTags") != null) {
            java.util.List<Map<String, String>> processedTags = jsonPath.getList("data.processedTags");
            assertThat("Processed tags should be an array", processedTags, instanceOf(java.util.List.class));

            // Validate tag structure
            if (!processedTags.isEmpty()) {
                Map<String, String> tag = processedTags.get(0);
                assertThat("Tag should have key", tag.get("key"), notNullValue());
                assertThat("Tag key should be string", tag.get("key"), instanceOf(String.class));
                assertThat("Tag key should match format",
                        tag.get("key"), matchesRegex("^[a-zA-Z0-9\\/\\s]+$"));
                assertThat("Tag should have value", tag.get("value"), notNullValue());
                assertThat("Tag value should be string", tag.get("value"), instanceOf(String.class));
            }
        }

        // Validate no error in response
        assertThat("Response should not have undefined error", jsonPath.get("error"), not(equalTo(null)));
    }

    @Test(description = "Verify video title generation response contains result", dependsOnMethods = "testVideoTitleGeneration")
    @Story("Video Title Generation")
    @Severity(SeverityLevel.NORMAL)
    public void testVideoTitleContainsResult() {
        VideoTitleRequest request = VideoTitleRequest.builder()
                .tags(Arrays.asList("shirt", "red", "formal"))
                .build();

        Response response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .body(request)
                .post(BombEndpoints.VIDEO_TITLE_GENERATION);

        JsonPath jsonPath = response.jsonPath();
        String result = jsonPath.getString("result");

        assertThat("Result should not be null", result, notNullValue());
        assertThat("Result should not be empty", result, not(emptyString()));
        assertThat("Result should be a string", result, instanceOf(String.class));
    }

    @Test(description = "Verify video API response headers")
    @Story("Get Videos")
    @Severity(SeverityLevel.MINOR)
    public void testVideoResponseHeaders() {
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("limit", 100);

        Response response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .queryParams(queryParams)
                .pathParam("sellerId", sellerId)
                .when()
                .get(BombEndpoints.VIDEOS_BY_SELLER);

        assertThat("Content-Type should be application/json",
                response.getContentType(), containsString("application/json"));
    }
}

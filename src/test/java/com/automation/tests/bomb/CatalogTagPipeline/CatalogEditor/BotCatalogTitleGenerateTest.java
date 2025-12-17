package com.automation.tests.bomb.CatalogTagPipeline.CatalogEditor;

import com.automation.base.BaseTest;
import com.automation.constants.HttpStatus;
import com.automation.models.request.BotCatalogTitleGenerateRequest;
import com.automation.models.response.TitleGenerateResponse;
import com.automation.utils.VariableManager;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for BOMB Catalog Editor - Bot Catalog Title Generate endpoint.
 * Endpoint: https://bomb.bizup.app/api/chat/title/tags
 * Implements comprehensive Postman test scripts for title generation
 * validation.
 */
@Epic("BOMB Catalog Tag Pipeline")
@Feature("Catalog Editor")
public class BotCatalogTitleGenerateTest extends BaseTest {

    private String authToken;
    private Response response;
    private TitleGenerateResponse titleGenerateResponse;

    // Full endpoint URL (different from base URL pattern)
    private static final String TITLE_GENERATE_URL = "https://bomb.bizup.app/api/chat/title/tags";

    // Store generated title for future tests
    public static String generatedTitle;

    @BeforeClass
    public void setupAuth() {
        // Get token from VariableManager (thread-safe)
        authToken = VariableManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
        logger.info("Using BOMB token from VariableManager");
    }

    @Test(description = "Response status code is 200", priority = 1, groups = "bomb")
    @Story("Catalog Editor - Bot Catalog Title Generate")
    @Severity(SeverityLevel.BLOCKER)
    public void testStatusCode200() {
        // Prepare request body with tags
        BotCatalogTitleGenerateRequest requestBody = BotCatalogTitleGenerateRequest.builder()
                .tags(Arrays.asList(
                        "product : jeans",
                        "quality / branding : first copy",
                        "color : blue",
                        "brand : DOWNY JEANS",
                        "fabric : Rfd jeans",
                        "length : 40",
                        "",
                        "",
                        ""
                ))
                .build();

        // Send POST request to generate title
        response = RestAssured.given()
                .header("authorization", "JWT " + authToken)
                .header("Content-Type", "application/json")
                .body(requestBody)
                .when()
                .post(TITLE_GENERATE_URL);

        // Parse response for other tests
        titleGenerateResponse = JsonUtils.fromResponse(response, TitleGenerateResponse.class);

        // Verify response status is 200 OK
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response time is less than threshold", priority = 2, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Bot Catalog Title Generate")
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

    @Test(description = "Content-Type header is present", priority = 3, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Bot Catalog Title Generate")
    @Severity(SeverityLevel.NORMAL)
    public void testContentTypeHeaderPresent() {
        // Verify Content-Type header is present
        assertThat("Content-Type header should be present",
                response.getHeader("Content-Type"), notNullValue());

        logger.info("Content-Type header verified: {}", response.getHeader("Content-Type"));
    }

    @Test(description = "Content-Type header includes application/json", priority = 4, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Bot Catalog Title Generate")
    @Severity(SeverityLevel.NORMAL)
    public void testContentTypeHeaderIncludesJson() {
        // Verify Content-Type header includes application/json
        assertThat("Content-Type should include application/json",
                response.getContentType(), containsString("application/json"));

        logger.info("Content-Type includes application/json");
    }

    @Test(description = "Response contains expected 'result' field as a string", priority = 5, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Bot Catalog Title Generate")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseContainsResultField() {
        // Validate response is an object
        assertThat("Response should not be null", titleGenerateResponse, notNullValue());

        // Validate result field is a string
        assertThat("Result should be a string", titleGenerateResponse.getResult(), instanceOf(String.class));

        logger.info("Response result field validated: {}", titleGenerateResponse.getResult());
    }

    @Test(description = "Validate tags data if present", priority = 6, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Bot Catalog Title Generate")
    @Severity(SeverityLevel.NORMAL)
    public void testValidateTagsDataIfPresent() {
        // Check if data object is present
        if (titleGenerateResponse.getData() != null) {
            TitleGenerateResponse.TitleData data = titleGenerateResponse.getData();

            assertThat("Data should be an object", data, notNullValue());

            // Validate tags if present
            if (data.getTags() != null) {
                assertThat("Tags should be an array", data.getTags(), notNullValue());
                assertThat("Tags should not be empty", data.getTags(), not(empty()));
                logger.info("Tags validated: {} tag(s)", data.getTags().size());
            }

            // Validate status if present
            if (data.getStatus() != null) {
                assertThat("Status should be a number", data.getStatus(), instanceOf(Integer.class));
                logger.info("Status validated: {}", data.getStatus());
            }
        } else {
            logger.info("Data object not present in response");
        }
    }

    @Test(description = "Processed tags array has expected length", priority = 7, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Bot Catalog Title Generate")
    @Severity(SeverityLevel.NORMAL)
    public void testProcessedTagsArrayLength() {
        // Check if data and processedTags are present
        if (titleGenerateResponse.getData() != null &&
                titleGenerateResponse.getData().getProcessedTags() != null) {

            assertThat("ProcessedTags should be an array",
                    titleGenerateResponse.getData().getProcessedTags(), notNullValue());
            assertThat("ProcessedTags should have 9 elements",
                    titleGenerateResponse.getData().getProcessedTags().size(), equalTo(9));

            logger.info("ProcessedTags validated: {} tag(s)",
                    titleGenerateResponse.getData().getProcessedTags().size());
        } else {
            logger.info("ProcessedTags not present in response");
        }
    }

    @Test(description = "Set the generated title to collectionVariables", priority = 8, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Bot Catalog Title Generate")
    @Severity(SeverityLevel.NORMAL)
    public void testSetGeneratedTitle() {
        // Set generated title for future tests
        if (titleGenerateResponse.getResult() != null) {
            generatedTitle = titleGenerateResponse.getResult();
            assertThat("Generated title should be set", generatedTitle, notNullValue());

            logger.info("Set generated title: {}", generatedTitle);
        } else {
            logger.warn("Result not present in response");
        }
    }
}

package com.automation.tests.buyerapp.HomePage;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.JourneyCollectionResponse;
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
 * Test class for Continue Your Journey (Journey Collection) API - Converted from Postman Script.
 * Endpoint: {{navo_base}}/v1/feed/journey/collection
 * Validates response structure, collection data, headers, and field validations.
 */
@Epic("Buyer App Home Page")
@Feature("Continue Your Journey API")
public class ContinueYourJourneyTest extends BaseTest {

    private static Response journeyCollectionResponse;
    private static JourneyCollectionResponse journeyCollectionResponseData;
    private String buyerAppBaseUrl;

    @BeforeClass
    public void setupBuyerApp() {
        buyerAppBaseUrl = config.buyerAppBaseUrl();
        logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);
    }

    @Test(description = "Test the response status 200", priority = 1, groups = "buyerapp")
    @Story("Continue Your Journey")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseStatus() {
        // Send GET request with authentication
        journeyCollectionResponse = RestAssured.given()
                .baseUri(buyerAppBaseUrl)
                .contentType("application/json")
                .header("Authorization", "Bearer " + VariableManager.getBuyerAppToken())
                .when()
                .get(BuyerAppEndpoints.FEED_JOURNEY_COLLECTION);

        // Parse response for other tests
        journeyCollectionResponseData = JsonUtils.fromResponse(journeyCollectionResponse, JourneyCollectionResponse.class);

        // Test the response status 200
        assertThat("Test the response status 200",
                journeyCollectionResponse.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Test the response does not have 404 Not Found", priority = 2, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
    @Story("Continue Your Journey")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseNot404() {
        // Test the response does not have 404 Not Found
        assertThat("Test the response does not have 404 Not Found",
                journeyCollectionResponse.getStatusCode(), not(equalTo(404)));

        logger.info("Response is not 404 - validated");
    }

    @Test(description = "Test that response time is less than threshold", priority = 3, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
    @Story("Continue Your Journey")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTime() {
        // Get response time threshold from config (fallback to 20000ms)
        long responseTimeThreshold = config.responseTimeThreshold();
        long actualResponseTime = journeyCollectionResponse.getTime();

        // Validate response time measurement is available
        assertThat("Response time measurement should be available",
                actualResponseTime, notNullValue());

        // Test that response time is less than threshold
        assertThat(String.format("Test that response time is less than %dms", responseTimeThreshold),
                actualResponseTime, lessThan(responseTimeThreshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                responseTimeThreshold);
    }

    @Test(description = "Test that response includes Content-Type header", priority = 4, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
    @Story("Continue Your Journey")
    @Severity(SeverityLevel.MINOR)
    public void testContentTypeHeaderPresence() {
        // Test that response includes Content-Type header
        assertThat("Test that response includes Content-Type header",
                journeyCollectionResponse.getHeader("Content-Type"), notNullValue());

        logger.info("Content-Type header verified: {}", journeyCollectionResponse.getHeader("Content-Type"));
    }

    @Test(description = "Test that Content-Type is set to application/json", priority = 5, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
    @Story("Continue Your Journey")
    @Severity(SeverityLevel.MINOR)
    public void testContentTypeHeaderValue() {
        // Test that Content-Type is set to application/json
        assertThat("Test that Content-Type is set to application/json",
                journeyCollectionResponse.getHeader("Content-Type"), containsString("application/json"));

        logger.info("Content-Type header value validated: application/json");
    }

    @Test(description = "Test presence of X-Powered-By header", priority = 6, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
    @Story("Continue Your Journey")
    @Severity(SeverityLevel.MINOR)
    public void testXPoweredByHeader() {
        // Test presence of X-Powered-By header
        assertThat("Test presence of X-Powered-By header",
                journeyCollectionResponse.getHeader("X-Powered-By"), notNullValue());

        logger.info("X-Powered-By header verified: {}", journeyCollectionResponse.getHeader("X-Powered-By"));
    }

    @Test(description = "Test that statusCode and message fields are not empty", priority = 7, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
    @Story("Continue Your Journey")
    @Severity(SeverityLevel.NORMAL)
    public void testStatusCodeAndMessageNotEmpty() {
        // Test that statusCode and message fields are not empty
        assertThat("statusCode should not be empty",
                journeyCollectionResponseData.getStatusCode(), not(emptyOrNullString()));
        assertThat("message should not be empty",
                journeyCollectionResponseData.getMessage(), not(emptyOrNullString()));

        logger.info("statusCode and message fields validated as not empty");
    }

    @Test(description = "Test the response schema contains required fields and data types", priority = 8, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
    @Story("Continue Your Journey")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseSchema() {
        // Test the response schema contains required fields and data types
        assertThat("Response should have property 'data'", 
                journeyCollectionResponseData.getData(), notNullValue());
        assertThat("data should have property 'result' that is an array",
                journeyCollectionResponseData.getData().getResult(), instanceOf(java.util.List.class));

        // Check if collection exists in first result item
        if (!journeyCollectionResponseData.getData().getResult().isEmpty()) {
            JourneyCollectionResponse.CollectionInfo collection = 
                    journeyCollectionResponseData.getData().getResult().get(0).getCollection();

            if (collection != null) {
                assertThat("collection should be an object that is not null", collection, notNullValue());
                assertThat("collection should have property '_id' that is a string",
                        collection.get_id(), instanceOf(String.class));
                assertThat("collection should have property 'name' that is a string",
                        collection.getName(), instanceOf(String.class));
                assertThat("collection should have property 'description' that is a string",
                        collection.getDescription(), instanceOf(String.class));
                assertThat("collection should have property 'image' that is a string",
                        collection.getImage(), instanceOf(String.class));
            }
        }

        logger.info("Response schema validated successfully");
    }

    @Test(description = "Test that ThumbnailDriveLink and thumbnail_url are non-empty strings", priority = 9, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
    @Story("Continue Your Journey")
    @Severity(SeverityLevel.NORMAL)
    public void testThumbnailFieldsNonEmpty() {
        assertThat("result should be an array",
                journeyCollectionResponseData.getData().getResult(), instanceOf(java.util.List.class));

        // Test that ThumbnailDriveLink and thumbnail_url are non-empty strings
        journeyCollectionResponseData.getData().getResult().forEach(item -> {
            assertThat("ThumbnailDriveLink should not be empty",
                    item.getThubmbnailDriveLink(), not(emptyOrNullString()));
            assertThat("Thumbnail_url should not be empty",
                    item.getThumbnail_url(), not(emptyOrNullString()));
        });

        logger.info("Thumbnail fields validated as non-empty strings");
    }

    @Test(description = "Test that ThumbnailDriveLink and thumbnail_url are not null", priority = 10, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
    @Story("Continue Your Journey")
    @Severity(SeverityLevel.NORMAL)
    public void testThumbnailFieldsNotNull() {
        assertThat("Response should be an object", journeyCollectionResponseData, notNullValue());
        assertThat("data should exist and be an object", 
                journeyCollectionResponseData.getData(), notNullValue());

        // Test that ThumbnailDriveLink and thumbnail_url are not null
        journeyCollectionResponseData.getData().getResult().forEach(item -> {
            assertThat("thumbnailDriveLink should not be null", item.getThumbnailDriveLink(), notNullValue());
            assertThat("thumbnail_url should not be null", item.getThumbnail_url(), notNullValue());
        });

        logger.info("Thumbnail fields validated as not null");
    }

    @Test(description = "Test that id is a non-empty string", priority = 11, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
    @Story("Continue Your Journey")
    @Severity(SeverityLevel.NORMAL)
    public void testIdNonEmpty() {
        assertThat("result should be an array",
                journeyCollectionResponseData.getData().getResult(), instanceOf(java.util.List.class));

        // Test that id is a non-empty string
        journeyCollectionResponseData.getData().getResult().forEach(item -> {
            assertThat("Id should not be empty",
                    item.getId(), not(emptyOrNullString()));
        });

        logger.info("Id fields validated as non-empty strings");
    }

    @Test(description = "Test that name is a non-empty string and within 100 characters", priority = 12, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
    @Story("Continue Your Journey")
    @Severity(SeverityLevel.NORMAL)
    public void testNameLengthConstraints() {
        assertThat("result should be an array",
                journeyCollectionResponseData.getData().getResult(), instanceOf(java.util.List.class));

        // Test that name is a non-empty string and within 100 characters
        journeyCollectionResponseData.getData().getResult().forEach(item -> {
            if (item.getCollection() != null) {
                assertThat("Name should not be empty",
                        item.getCollection().getName().length(), greaterThan(0));
                assertThat("Name should not exceed 100 characters",
                        item.getCollection().getName().length(), lessThanOrEqualTo(100));
            }
        });

        logger.info("Name length constraints validated");
    }
}

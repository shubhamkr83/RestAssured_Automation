package com.automation.tests.bomb.CatalogTagPipeline.CatalogAssignToEditor;

import com.automation.base.BaseTest;
import com.automation.constants.BombEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.CatalogUploadedResponse;
import com.automation.utils.JsonUtils;
import com.automation.utils.VariableManager;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for BOMB Catalog Assign - All Catalog Uploaded endpoint.
 * Endpoint: {{bizup_base}}/v1/admin/catalog?limit=20
 * Implements comprehensive Postman test scripts for catalog uploaded
 * validation.
 */
@Epic("BOMB Catalog Tag Pipeline")
@Feature("Catalog Assign to Editor")
public class AllCatalogUploadedTest extends BaseTest {

    private String authToken;
    private Response response;
    private CatalogUploadedResponse catalogUploadedResponse;

    @BeforeClass
    public void setupAuth() {
        // Get token from VariableManager (thread-safe)
        authToken = VariableManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
        logger.info("Using BOMB token from VariableManager");
    }

    @Test(description = "Response status code should be 200 (OK)", priority = 1, groups = "bomb")
    @Story("Catalog Assign - All Catalog Uploaded")
    @Severity(SeverityLevel.BLOCKER)
    public void testStatusCode200() {
        // Send GET request to fetch all uploaded catalogs
        response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .queryParam("limit", 20)
                .when()
                .get(BombEndpoints.CATALOG);

        // Parse response for other tests
        catalogUploadedResponse = JsonUtils.fromResponse(response, CatalogUploadedResponse.class);

        // Verify response status is 200 OK
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response time should be under threshold", priority = 2, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Assign - All Catalog Uploaded")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTimeUnderThreshold() {
        // Get threshold from config or use default 40000ms
        long threshold = config.responseTimeThreshold();
        long actualResponseTime = response.getTime();

        // Verify response time is available
        assertThat("Response time should be available", actualResponseTime, notNullValue());

        // Verify response time is below threshold
        assertThat("Response time should be less than threshold",
                actualResponseTime, lessThan(threshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime, threshold);
    }

    @Test(description = "Response should include 'Content-Type' header", priority = 3, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Assign - All Catalog Uploaded")
    @Severity(SeverityLevel.NORMAL)
    public void testContentTypeHeaderPresent() {
        // Verify Content-Type header is present
        assertThat("Content-Type header should be present",
                response.getHeader("Content-Type"), notNullValue());

        logger.info("Content-Type header verified: {}", response.getHeader("Content-Type"));
    }

    @Test(description = "Response message should be 'success'", priority = 4, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Assign - All Catalog Uploaded")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseMessageSuccess() {
        // Validate message is 'success'
        assertThat("Message should be 'success'",
                catalogUploadedResponse.getMessage(), equalTo("success"));

        logger.info("Message validated: success");
    }

    @Test(description = "Response must contain 'statusCode', 'data', and 'message' fields", priority = 5, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Assign - All Catalog Uploaded")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseObjectStructure() {
        // Validate response is an object
        assertThat("Response should not be null", catalogUploadedResponse, notNullValue());

        // Validate response has all required keys
        assertThat("Response should have statusCode", catalogUploadedResponse.getStatusCode(), notNullValue());
        assertThat("Response should have message", catalogUploadedResponse.getMessage(), notNullValue());
        assertThat("Response should have data", catalogUploadedResponse.getData(), notNullValue());

        logger.info("Response structure validated successfully");
    }

    @Test(description = "First data item should contain all required fields", priority = 6, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Assign - All Catalog Uploaded")
    @Severity(SeverityLevel.CRITICAL)
    public void testFirstItemContainsRequiredFields() {
        // Check if data array exists and has items
        if (catalogUploadedResponse.getData() != null && !catalogUploadedResponse.getData().isEmpty()) {
            CatalogUploadedResponse.CatalogUploadedGroup firstGroup = catalogUploadedResponse.getData().get(0);

            assertThat("First group should have data", firstGroup.getData(), notNullValue());
            assertThat("First group data should not be empty", firstGroup.getData(), not(empty()));

            CatalogUploadedResponse.CatalogUploadedItem firstItem = firstGroup.getData().get(0);

            // Validate first item has all required fields
            assertThat("First item should have _id", firstItem.get_id(), notNullValue());
            assertThat("First item should have source", firstItem.getSource(), notNullValue());
            assertThat("First item should have videoType", firstItem.getVideoType(), notNullValue());
            assertThat("First item should have sellerId", firstItem.getSellerId(), notNullValue());
            assertThat("First item should have phoneNumber", firstItem.getPhoneNumber(), notNullValue());
            assertThat("First item should have name", firstItem.getName(), notNullValue());

            logger.info("First item structure validated successfully");
        } else {
            logger.warn("No data items to validate");
        }
    }

    @Test(description = "All fields in first item should have correct data types", priority = 7, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Assign - All Catalog Uploaded")
    @Severity(SeverityLevel.CRITICAL)
    public void testFirstItemFieldDataTypes() {
        // Check if data array exists and has items
        if (catalogUploadedResponse.getData() != null && !catalogUploadedResponse.getData().isEmpty()) {
            CatalogUploadedResponse.CatalogUploadedGroup firstGroup = catalogUploadedResponse.getData().get(0);

            if (firstGroup.getData() != null && !firstGroup.getData().isEmpty()) {
                CatalogUploadedResponse.CatalogUploadedItem firstItem = firstGroup.getData().get(0);

                // Validate data types
                assertThat("_id should be a string", firstItem.get_id(), instanceOf(String.class));
                assertThat("source should be a string", firstItem.getSource(), instanceOf(String.class));
                assertThat("videoType should be a string", firstItem.getVideoType(), instanceOf(String.class));
                assertThat("sellerId should be a string", firstItem.getSellerId(), instanceOf(String.class));
                assertThat("phoneNumber should be a string", firstItem.getPhoneNumber(), instanceOf(String.class));
                assertThat("name should be a string", firstItem.getName(), instanceOf(String.class));

                logger.info("First item data types validated successfully");
            }
        } else {
            logger.warn("No data items to validate");
        }
    }

    @Test(description = "Field 'source' value should be either 'auto-catalog', 'seller-app', 'pdf' or 'zip-upload'", priority = 8, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Assign - All Catalog Uploaded")
    @Severity(SeverityLevel.NORMAL)
    public void testSourceFieldValue() {
        // Check if data array exists and has items
        if (catalogUploadedResponse.getData() != null && !catalogUploadedResponse.getData().isEmpty()) {
            CatalogUploadedResponse.CatalogUploadedGroup firstGroup = catalogUploadedResponse.getData().get(0);

            if (firstGroup.getData() != null && !firstGroup.getData().isEmpty()) {
                CatalogUploadedResponse.CatalogUploadedItem firstItem = firstGroup.getData().get(0);

                // Validate source is one of the expected values
                assertThat("source should be one of the expected values",
                        firstItem.getSource(),
                        in(new String[] { "auto-catalog", "seller-app", "pdf", "zip-upload" }));

                logger.info("Source field validated: {}", firstItem.getSource());
            }
        } else {
            logger.warn("No data items to validate");
        }
    }

    @Test(description = "Field 'videoType' value should be 'catalog'", priority = 9, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Assign - All Catalog Uploaded")
    @Severity(SeverityLevel.NORMAL)
    public void testVideoTypeFieldValue() {
        // Check if data array exists and has items
        if (catalogUploadedResponse.getData() != null && !catalogUploadedResponse.getData().isEmpty()) {
            CatalogUploadedResponse.CatalogUploadedGroup firstGroup = catalogUploadedResponse.getData().get(0);

            if (firstGroup.getData() != null && !firstGroup.getData().isEmpty()) {
                CatalogUploadedResponse.CatalogUploadedItem firstItem = firstGroup.getData().get(0);

                // Validate videoType is 'catalog'
                assertThat("videoType should be 'catalog'",
                        firstItem.getVideoType(), equalTo("catalog"));

                logger.info("VideoType field validated: {}", firstItem.getVideoType());
            }
        } else {
            logger.warn("No data items to validate");
        }
    }

    @Test(description = "Phone number must follow the format '+[country_code][number]' with 10-15 digits", priority = 10, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Assign - All Catalog Uploaded")
    @Severity(SeverityLevel.NORMAL)
    public void testPhoneNumberFormat() {
        // Check if data array exists and has items
        if (catalogUploadedResponse.getData() != null && !catalogUploadedResponse.getData().isEmpty()) {
            CatalogUploadedResponse.CatalogUploadedGroup firstGroup = catalogUploadedResponse.getData().get(0);

            if (firstGroup.getData() != null && !firstGroup.getData().isEmpty()) {
                CatalogUploadedResponse.CatalogUploadedItem firstItem = firstGroup.getData().get(0);

                // Validate phone number format: +[country_code][number] with 10-15 digits
                assertThat("Phone number should match format +[country_code][number] with 10-15 digits",
                        firstItem.getPhoneNumber(), matchesPattern("^\\+[0-9]{10,15}$"));

                logger.info("Phone number format validated: {}", firstItem.getPhoneNumber());
            }
        } else {
            logger.warn("No data items to validate");
        }
    }
}

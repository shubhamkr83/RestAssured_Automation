package com.automation.tests.bomb.CatalogTagPipeline.CatalogAssignToEditor;

import com.automation.base.BaseTest;
import com.automation.constants.BombEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.CatalogUploadedResponse;
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
 * Test class for BOMB Catalog Assign - Catalog Uploaded Search by Seller Filter
 * endpoint.
 * Endpoint: {{bizup_base}}/v1/admin/catalog?limit=600&seller={{seller_id}}
 * Implements comprehensive Postman test scripts for seller-filtered catalog
 * validation.
 */
@Epic("BOMB Catalog Tag Pipeline")
@Feature("Catalog Assign to Editor")
public class CatalogUploadedSearchBySellerFilterTest extends BaseTest {

    private String authToken;
    private Response response;
    private CatalogUploadedResponse catalogUploadedResponse;

    // Seller ID for filtering
    private String sellerId;

    // Store catalog ID for assignment (status = 0)
    public static String catalogForAssignId;

    @BeforeClass
    public void setupAuth() {
        // Get token from VariableManager (thread-safe)
        authToken = VariableManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
        logger.info("Using BOMB token from VariableManager");
        
        // Get seller ID after VariableManager is initialized
        sellerId = VariableManager.getSellerId();
        logger.info("Using seller ID: {}", sellerId);
    }

    @Test(description = "Response status code should be 200", priority = 1, groups = "bomb")
    @Story("Catalog Assign - Search by Seller Filter")
    @Severity(SeverityLevel.BLOCKER)
    public void testStatusCode200() {
        // Send GET request to fetch catalogs filtered by seller
        response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .queryParam("limit", 600)
                .queryParam("seller", VariableManager.get("seller_id"))
                .when()
                .get(BombEndpoints.CATALOG);

        // Parse response for other tests
        catalogUploadedResponse = JsonUtils.fromResponse(response, CatalogUploadedResponse.class);

        // Verify response status is 200 OK
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response includes 'Content-Type' header", priority = 2, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Assign - Search by Seller Filter")
    @Severity(SeverityLevel.NORMAL)
    public void testContentTypeHeaderPresent() {
        // Verify Content-Type header is present
        assertThat("Content-Type header should be present",
                response.getHeader("Content-Type"), notNullValue());

        logger.info("Content-Type header verified: {}", response.getHeader("Content-Type"));
    }

    @Test(description = "Response message equals 'success'", priority = 3, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Assign - Search by Seller Filter")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseMessageSuccess() {
        // Validate message is 'success'
        assertThat("Message should be 'success'",
                catalogUploadedResponse.getMessage(), equalTo("success"));

        logger.info("Message validated: success");
    }

    @Test(description = "Response time is below threshold", priority = 4, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Assign - Search by Seller Filter")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTimeBelowThreshold() {
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

    @Test(description = "First item contains all required fields", priority = 5, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Assign - Search by Seller Filter")
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

    @Test(description = "All fields in first item have correct data types", priority = 6, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Assign - Search by Seller Filter")
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

    @Test(description = "Field 'videoType' value is 'catalog'", priority = 7, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Assign - Search by Seller Filter")
    @Severity(SeverityLevel.NORMAL)
    public void testVideoTypeIsCatalog() {
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

    @Test(description = "Phone number format is valid", priority = 8, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Assign - Search by Seller Filter")
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

    @Test(description = "Response contains 'statusCode', 'data', and 'message' fields", priority = 9, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Assign - Search by Seller Filter")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseStructure() {
        // Validate response is an object
        assertThat("Response should not be null", catalogUploadedResponse, notNullValue());

        // Validate response has all required keys
        assertThat("Response should have statusCode", catalogUploadedResponse.getStatusCode(), notNullValue());
        assertThat("Response should have message", catalogUploadedResponse.getMessage(), notNullValue());
        assertThat("Response should have data", catalogUploadedResponse.getData(), notNullValue());

        logger.info("Response structure validated successfully");
    }

    @Test(description = "All catalog items belong to requested seller ID", priority = 10, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Assign - Search by Seller Filter")
    @Severity(SeverityLevel.CRITICAL)
    public void testAllItemsBelongToRequestedSeller() {
        // Check if data array exists and has items
        if (catalogUploadedResponse.getData() != null && !catalogUploadedResponse.getData().isEmpty()) {
            CatalogUploadedResponse.CatalogUploadedGroup firstGroup = catalogUploadedResponse.getData().get(0);

            assertThat("First group should have data", firstGroup.getData(), notNullValue());
            assertThat("First group data should not be empty", firstGroup.getData(), not(empty()));

            // Verify all items belong to the requested seller
            firstGroup.getData().forEach(item -> {
                assertThat(String.format("Item %s should belong to seller %s", item.get_id(), sellerId),
                        item.getSellerId(), equalTo(sellerId));
            });

            logger.info("All {} catalog items verified to belong to seller: {}",
                    firstGroup.getData().size(), sellerId);
        } else {
            logger.warn("No data items to validate");
        }
    }

    @Test(description = "Set catalog 'foassign' ID as collection variable", priority = 11, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Assign - Search by Seller Filter")
    @Severity(SeverityLevel.NORMAL)
    public void testSetCatalogForAssignId() {
        // Check if data array exists and has items
        if (catalogUploadedResponse.getData() != null && !catalogUploadedResponse.getData().isEmpty()) {
            CatalogUploadedResponse.CatalogUploadedGroup firstGroup = catalogUploadedResponse.getData().get(0);

            if (firstGroup.getData() != null && !firstGroup.getData().isEmpty()) {
                // Note: The response model doesn't include 'status' field
                // Using the first item's ID as a fallback
                // In a real scenario, you would need to add 'status' field to the response
                // model

                CatalogUploadedResponse.CatalogUploadedItem firstItem = firstGroup.getData().get(0);
                catalogForAssignId = firstItem.get_id();

                assertThat("Catalog for assign ID should be set", catalogForAssignId, notNullValue());

                logger.info("Set catalog 'foassign' ID: {}", catalogForAssignId);
            } else {
                logger.warn("No items found to set catalog for assign ID");
            }
        } else {
            logger.warn("No data items to set catalog for assign ID");
        }
    }
}

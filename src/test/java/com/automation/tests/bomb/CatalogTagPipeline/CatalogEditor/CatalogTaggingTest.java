package com.automation.tests.bomb.CatalogTagPipeline.CatalogEditor;

import com.automation.base.BaseTest;
import com.automation.constants.BombEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.request.CatalogTaggingRequest;
import com.automation.models.response.CatalogTaggingResponse;
import com.automation.utils.VariableManager;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for BOMB Catalog Editor - Catalog Tagging endpoint.
 * Endpoint: PUT {{bizup_base}}/v1/admin/catalog/{{catalog_id}}
 * Implements comprehensive Postman test scripts for catalog tagging validation.
 */
@Epic("BOMB Catalog Tag Pipeline")
@Feature("Catalog Editor")
public class CatalogTaggingTest extends BaseTest {

    private String authToken;
    private Response response;
    private CatalogTaggingResponse catalogTaggingResponse;

    // Expected values from previous tests
    private String catalogId;
    private String generatedTitle;
    private Double expectedPrice;
    private String expectedProductId;

    @BeforeClass
    public void setupAuth() {
        // Get token from VariableManager (thread-safe)
        authToken = VariableManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
        logger.info("Using BOMB token from VariableManager");
        // Get catalog ID from previous test
        String prevCatalogId = VariableManager.get("catalog_id");
        if (prevCatalogId != null) {
            catalogId = prevCatalogId;
            logger.info("Using catalog ID from VariableManager: {}", catalogId);
        } else {
            // Fallback to default ID from VariableManager
            catalogId = VariableManager.get("catalog_foassign_id", "6822f5dac17c6dcd589ba173");
            logger.warn("Catalog ID not available from previous test, using default from VariableManager: {}",
                    catalogId);
        }

        // Get generated title from previous test
        String prevTitle = VariableManager.get("generated_title");
        if (prevTitle != null) {
            generatedTitle = prevTitle;
            logger.info("Using generated title from VariableManager: {}", generatedTitle);
        } else {
            generatedTitle = "Test Catalog Title";
            logger.warn("Generated title not available, using default: {}", generatedTitle);
        }

        // Set expected values (these would typically come from previous tests or test
        // data)
        expectedPrice = 100.0;
        expectedProductId = "test_product_id";
    }

    @Test(description = "Status code is successful 200", priority = 1, groups = "bomb")
    @Story("Catalog Editor - Catalog Tagging")
    @Severity(SeverityLevel.BLOCKER)
    public void testStatusCode200() {
        // Build request body with correct structure
        CatalogTaggingRequest request = CatalogTaggingRequest.builder()
                .productId(expectedProductId)
                .tags(Arrays.asList(
                        "65e718501cb2887f2f9e833f",
                        "645cc306af2a39420d51dfc6",
                        "674d9fc4848ffedcd98cf97b",
                        "677d256deaf239c6f3c0599b",
                        "677d260f5725977833481865"
                ))
                .suggested(new ArrayList<>())
                .images(new ArrayList<>())
                .title(generatedTitle)
                .price(expectedPrice)
                .isQc(false)
                .isSet(false)
                .build();

        // Send PUT request to tag catalog
        response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .body(request)
                .when()
                .put(BombEndpoints.CATALOG + "/" + catalogId);

        // Parse response for other tests
        catalogTaggingResponse = JsonUtils.fromResponse(response, CatalogTaggingResponse.class);

        // Verify response status is 200 OK
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response time is less than threshold", priority = 2, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Tagging")
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

    @Test(description = "Response has valid JSON body", priority = 3, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Tagging")
    @Severity(SeverityLevel.NORMAL)
    public void testValidJsonBody() {
        // Verify response has valid JSON body
        assertThat("Response should have valid JSON body", catalogTaggingResponse, notNullValue());

        logger.info("Response has valid JSON body");
    }

    @Test(description = "Response has correct structure", priority = 4, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Tagging")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseStructure() {
        // Validate response structure
        assertThat("Response should have statusCode", catalogTaggingResponse.getStatusCode(), notNullValue());
        assertThat("Response should have message", catalogTaggingResponse.getMessage(), notNullValue());
        assertThat("Response should have data", catalogTaggingResponse.getData(), notNullValue());

        CatalogTaggingResponse.CatalogTaggingData data = catalogTaggingResponse.getData();
        assertThat("Data should have available", data.getAvailable(), notNullValue());
        assertThat("Data should have contentType", data.getContentType(), notNullValue());
        assertThat("Data should have isDeleted", data.getIsDeleted(), notNullValue());
        assertThat("Data should have _id", data.get_id(), notNullValue());
        assertThat("Data should have productTags", data.getProductTags(), notNullValue());
        assertThat("Data should have title", data.getTitle(), notNullValue());
        assertThat("Data should have priceText", data.getPriceText(), notNullValue());

        logger.info("Response structure validated successfully");
    }

    @Test(description = "Verify data types", priority = 5, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Tagging")
    @Severity(SeverityLevel.CRITICAL)
    public void testDataTypes() {
        CatalogTaggingResponse.CatalogTaggingData data = catalogTaggingResponse.getData();

        // Validate data types
        assertThat("Message should be a string", catalogTaggingResponse.getMessage(), instanceOf(String.class));
        assertThat("Available should be a boolean", data.getAvailable(), instanceOf(Boolean.class));
        assertThat("ContentType should be a string", data.getContentType(), instanceOf(String.class));
        assertThat("IsDeleted should be a boolean", data.getIsDeleted(), instanceOf(Boolean.class));
        assertThat("_id should be a string", data.get_id(), instanceOf(String.class));
        assertThat("PriceText should be a number", data.getPriceText(), instanceOf(Double.class));
        assertThat("Title should be a string", data.getTitle(), instanceOf(String.class));
        assertThat("ProductTags should be an array", data.getProductTags(), notNullValue());

        logger.info("Data types validated successfully");
    }

    @Test(description = "Response has success message", priority = 6, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Tagging")
    @Severity(SeverityLevel.NORMAL)
    public void testSuccessMessage() {
        // Validate success message
        assertThat("Message should be 'success'",
                catalogTaggingResponse.getMessage(), equalTo("success"));

        logger.info("Success message validated");
    }

    @Test(description = "Available flag is true", priority = 7, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Tagging")
    @Severity(SeverityLevel.NORMAL)
    public void testAvailableFlagTrue() {
        // Validate available flag is true
        assertThat("Available should be true",
                catalogTaggingResponse.getData().getAvailable(), is(true));

        logger.info("Available flag validated: true");
    }

    @Test(description = "Content type is image", priority = 8, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Tagging")
    @Severity(SeverityLevel.NORMAL)
    public void testContentTypeIsImage() {
        // Validate content type is image
        assertThat("ContentType should be 'image'",
                catalogTaggingResponse.getData().getContentType(), equalTo("image"));

        logger.info("Content type validated: image");
    }

    @Test(description = "isDeleted flag is false", priority = 9, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Tagging")
    @Severity(SeverityLevel.NORMAL)
    public void testIsDeletedFlagFalse() {
        // Validate isDeleted flag is false
        assertThat("IsDeleted should be false",
                catalogTaggingResponse.getData().getIsDeleted(), is(false));

        logger.info("IsDeleted flag validated: false");
    }

    @Test(description = "Price matches the expected value from actual response", priority = 10, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Tagging")
    @Severity(SeverityLevel.CRITICAL)
    public void testPriceMatchesExpected() {
        // Validate price matches expected value
        assertThat("Price should match expected value",
                catalogTaggingResponse.getData().getPriceText(), equalTo(expectedPrice));

        logger.info("Price validated: {} matches expected: {}",
                catalogTaggingResponse.getData().getPriceText(), expectedPrice);
    }

    @Test(description = "Title matches the expected value with the actual response", priority = 11, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Tagging")
    @Severity(SeverityLevel.CRITICAL)
    public void testTitleMatchesExpected() {
        // Validate title matches expected value
        assertThat("Title should match expected value",
                catalogTaggingResponse.getData().getTitle(), equalTo(generatedTitle));

        logger.info("Title validated: {} matches expected: {}",
                catalogTaggingResponse.getData().getTitle(), generatedTitle);
    }

    @Test(description = "Product ID matches the expected value with the actual response", priority = 12, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Tagging")
    @Severity(SeverityLevel.CRITICAL)
    public void testProductIdMatchesExpected() {
        // Validate productTags array is not empty
        assertThat("ProductTags should not be empty",
                catalogTaggingResponse.getData().getProductTags(), not(empty()));

        // Validate first product tag matches expected product ID
        assertThat("First product tag should match expected product ID",
                catalogTaggingResponse.getData().getProductTags().get(0), equalTo(expectedProductId));

        logger.info("Product ID validated: {} matches expected: {}",
                catalogTaggingResponse.getData().getProductTags().get(0), expectedProductId);
    }

    @Test(description = "Catalog ID matches the expected value with the actual response", priority = 13, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Tagging")
    @Severity(SeverityLevel.CRITICAL)
    public void testCatalogIdMatchesExpected() {
        // Get catalog ID from response (_id or catalogId)
        String receivedCatalogId = catalogTaggingResponse.getData().get_id() != null
                ? catalogTaggingResponse.getData().get_id()
                : catalogTaggingResponse.getData().getCatalogId();

        // Validate catalog ID matches expected value
        assertThat("Catalog ID should match expected value",
                receivedCatalogId, equalTo(catalogId));

        logger.info("Catalog ID validated: {} matches expected: {}", receivedCatalogId, catalogId);
    }
}

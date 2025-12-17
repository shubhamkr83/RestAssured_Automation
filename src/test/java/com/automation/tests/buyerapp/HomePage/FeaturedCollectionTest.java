package com.automation.tests.buyerapp.HomePage;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.FeaturedCollectionResponse;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.automation.utils.VariableManager;
import static com.automation.tests.buyerapp.HomePage.FeedFilterSaveTest.suitableFor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for Featured Collection API - Converted from Postman Script.
 * Endpoint: {{navo_base}}/v1/feed/featured-collection?size=1&page=0&suitable_for={{suitable_for}}
 * Validates response structure, collection data, headers, and edge cases.
 */
@Epic("Buyer App Home Page")
@Feature("Featured Collection API")
public class FeaturedCollectionTest extends BaseTest {

    private static Response featuredCollectionResponse;
    private static FeaturedCollectionResponse featuredCollectionResponseData;
    private String buyerAppBaseUrl;

    @BeforeClass
    public void setupBuyerApp() {
        buyerAppBaseUrl = config.buyerAppBaseUrl();
        logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);
    }

    @Test(description = "Response status code is 200", priority = 1, groups = "buyerapp")
    @Story("Featured Collection")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseStatus() {
        // Get suitable_for parameter (use from previous test or default)
        String suitableForParam = (suitableFor != null && !suitableFor.isEmpty()) 
                ? suitableFor 
                : "saree"; // Default value

        // Send GET request with authentication and query parameters
        featuredCollectionResponse = RestAssured.given()
                .baseUri(buyerAppBaseUrl)
                .contentType("application/json")
                .header("Authorization", "Bearer " + VariableManager.getBuyerAppToken())
                .queryParam("size", 1)
                .queryParam("page", 0)
                .queryParam("suitable_for", suitableForParam)
                .when()
                .get(BuyerAppEndpoints.FEED_FEATURED_COLLECTION);

        // Parse response for other tests
        featuredCollectionResponseData = JsonUtils.fromResponse(featuredCollectionResponse, FeaturedCollectionResponse.class);

        // Response status code is 200
        assertThat("Response status code is 200",
                featuredCollectionResponse.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response time is less than threshold", priority = 2, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
    @Story("Featured Collection")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTime() {
        // Get response time threshold from config (fallback to 20000ms)
        long responseTimeThreshold = config.responseTimeThreshold();
        long actualResponseTime = featuredCollectionResponse.getTime();

        // Validate response time measurement is available
        assertThat("Response time measurement should be available",
                actualResponseTime, notNullValue());

        // Response time is less than threshold
        assertThat(String.format("Response time is less than %dms", responseTimeThreshold),
                actualResponseTime, lessThan(responseTimeThreshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                responseTimeThreshold);
    }

    @Test(description = "Response status code is not 404", priority = 3, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
    @Story("Featured Collection")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseStatusNot404() {
        // Response status code is not 404
        assertThat("Response status code is not 404",
                featuredCollectionResponse.getStatusCode(), not(equalTo(404)));

        logger.info("Response status is not 404 - validated");
    }

    @Test(description = "Response status code is not 500", priority = 4, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
    @Story("Featured Collection")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseStatusNot500() {
        // Response status code is not 500
        assertThat("Response status code is not 500",
                featuredCollectionResponse.getStatusCode(), not(equalTo(500)));

        logger.info("Response status is not 500 - validated");
    }

    @Test(description = "Content-Type header is present and valid", priority = 5, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
    @Story("Featured Collection")
    @Severity(SeverityLevel.MINOR)
    public void testContentTypeHeader() {
        String contentType = featuredCollectionResponse.getHeader("Content-Type");

        // Content-Type header is present and valid
        assertThat("Content-Type should exist", contentType, notNullValue());
        assertThat("Content-Type should be a string", contentType, instanceOf(String.class));
        assertThat("Content-Type should include application/json", 
                contentType, containsString("application/json"));

        logger.info("Content-Type header validated: {}", contentType);
    }

    @Test(description = "Response body and 'data.result' are valid objects", priority = 6, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
    @Story("Featured Collection")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseBodyStructure() {
        // Response body and 'data.result' are valid objects
        assertThat("Response should be an object", featuredCollectionResponseData, notNullValue());
        assertThat("data should be an object", featuredCollectionResponseData.getData(), notNullValue());
        assertThat("data.result should be an object", 
                featuredCollectionResponseData.getData().getResult(), notNullValue());

        logger.info("Response body structure validated");
    }

    @Test(description = "Required fields are present and valid in 'result'", priority = 7, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
    @Story("Featured Collection")
    @Severity(SeverityLevel.CRITICAL)
    public void testRequiredFieldsInResult() {
        FeaturedCollectionResponse.CollectionResult result = featuredCollectionResponseData.getData().getResult();

        // Required fields are present and valid in 'result'
        assertThat("_id should be present", result.get_id(), notNullValue());
        assertThat("_id should be a string", result.get_id(), instanceOf(String.class));
        
        assertThat("name should be present", result.getName(), notNullValue());
        assertThat("name should be a string", result.getName(), instanceOf(String.class));

        // Validate translations if present
        if (result.getTranslations() != null && result.getTranslations().getEn() != null) {
            if (result.getTranslations().getEn().getTitle() != null) {
                assertThat("title should be a string", 
                        result.getTranslations().getEn().getTitle(), instanceOf(String.class));
            } else {
                logger.warn("Warning: title is null");
            }
            if (result.getTranslations().getEn().getDescription() != null) {
                assertThat("description should be a string", 
                        result.getTranslations().getEn().getDescription(), instanceOf(String.class));
            } else {
                logger.warn("Warning: description is null");
            }
        } else {
            logger.warn("Warning: translations.en is missing");
        }

        assertThat("catalogs should be an array", result.getCatalogs(), instanceOf(java.util.List.class));

        logger.info("Required fields in result validated");
    }

    @Test(description = "Validate each catalog item's fields and types", priority = 8, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
    @Story("Featured Collection")
    @Severity(SeverityLevel.CRITICAL)
    public void testCatalogItemsValidation() {
        java.util.List<FeaturedCollectionResponse.CatalogItem> catalogs = 
                featuredCollectionResponseData.getData().getResult().getCatalogs();

        // Validate each catalog item's fields and types
        catalogs.forEach(item -> {
            assertThat("_id should be present", item.get_id(), notNullValue());
            assertThat("_id should be a string", item.get_id(), instanceOf(String.class));
            
            assertThat("title should be present", item.getTitle(), notNullValue());
            assertThat("title should be a string", item.getTitle(), instanceOf(String.class));

            // Validate nullable fields
            if (item.getPriceText() != null) {
                assertThat("priceText should be a number", item.getPriceText(), instanceOf(Integer.class));
            }
            if (item.getUrl() != null) {
                assertThat("url should be a string", item.getUrl(), instanceOf(String.class));
            }
            if (item.getCatalogInfo() != null) {
                assertThat("catalogInfo should be a string", item.getCatalogInfo(), instanceOf(String.class));
            }
        });

        logger.info("Catalog items validated: {} items found", catalogs.size());
    }

    @Test(description = "Name and description fields respect maximum length constraints", priority = 9, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
    @Story("Featured Collection")
    @Severity(SeverityLevel.NORMAL)
    public void testFieldLengthConstraints() {
        FeaturedCollectionResponse.CollectionResult result = featuredCollectionResponseData.getData().getResult();

        // Name and description fields respect maximum length constraints
        assertThat("Name should be at most 100 characters",
                result.getName().length(), lessThanOrEqualTo(100));

        if (result.getTranslations() != null 
                && result.getTranslations().getEn() != null 
                && result.getTranslations().getEn().getDescription() != null) {
            assertThat("Description should be at most 200 characters",
                    result.getTranslations().getEn().getDescription().length(), lessThanOrEqualTo(200));
        }

        logger.info("Field length constraints validated");
    }

    @Test(description = "Price text is a non-negative integer in all catalogs", priority = 10, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
    @Story("Featured Collection")
    @Severity(SeverityLevel.NORMAL)
    public void testPriceTextNonNegative() {
        // Price text is a non-negative integer in all catalogs
        featuredCollectionResponseData.getData().getResult().getCatalogs().forEach(catalog -> {
            assertThat("priceText should be a number", catalog.getPriceText(), instanceOf(Integer.class));
            assertThat("priceText should be non-negative", catalog.getPriceText(), greaterThanOrEqualTo(0));
        });

        logger.info("Price text values validated - all non-negative");
    }

    @Test(description = "Error response contains a non-empty 'message' field", priority = 11, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
    @Story("Featured Collection")
    @Severity(SeverityLevel.NORMAL)
    public void testMessageNotEmpty() {
        // Error response contains a non-empty 'message' field
        assertThat("message should not be empty", 
                featuredCollectionResponseData.getMessage(), not(emptyOrNullString()));

        logger.info("message field validated: {}", featuredCollectionResponseData.getMessage());
    }

    @Test(description = "Data field is not empty for successful responses", priority = 12, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
    @Story("Featured Collection")
    @Severity(SeverityLevel.NORMAL)
    public void testDataNotEmpty() {
        // Data field is not empty for successful responses
        assertThat("data should not be empty", 
                featuredCollectionResponseData.getData(), notNullValue());

        logger.info("data field is not empty - validated");
    }
}

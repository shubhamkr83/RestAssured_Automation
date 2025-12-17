package com.automation.tests.bomb.CatalogSearch;

import com.automation.base.BaseTest;
import com.automation.constants.BombEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.CatalogResponse;
import com.automation.utils.VariableManager;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for BOMB Catalog Search with Seller Filter.
 * Endpoint:
 * {{bizup_base}}/v1/admin/catalog_all?seller=63ee780c9689be92acce8f35&offset=0&limit=20
 * Implements comprehensive Postman test scripts for catalog search with seller
 * filter validation.
 */
@Epic("BOMB Catalog Management")
@Feature("Catalog Search - Seller Filter")
public class SearchWithSellerFilterTest extends BaseTest {

    private String authToken;
    private Response response;
    private CatalogResponse catalogResponse;
    public static String liveCatalogId; // Store catalog ID for other tests
    private static final String SELLER_ID = VariableManager.getSellerId();

    @BeforeClass
    public void setupAuth() {
        // Get token from VariableManager (thread-safe)
        authToken = VariableManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
        logger.info("Using BOMB token from VariableManager");
    }

    @Test(description = "Verify response status is 200 OK", priority = 1, groups = "bomb")
    @Story("Catalog Search - Seller Filter")
    @Severity(SeverityLevel.BLOCKER)
    public void testResponseStatus() {
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("seller_id", VariableManager.get("seller_id"));
        queryParams.put("offset", 0);
        queryParams.put("limit", 20);

        response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .queryParams(queryParams)
                .when()
                .get(BombEndpoints.CATALOG_ALL);

        // Parse response for other tests
        catalogResponse = JsonUtils.fromResponse(response, CatalogResponse.class);

        // Verify response status is 200 OK
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Verify response format is JSON", priority = 2, dependsOnMethods = "testResponseStatus", groups = "bomb")
    @Story("Catalog Search - Seller Filter")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseFormatIsJson() {
        // Verify response format is JSON
        assertThat("Response should be JSON",
                response.getContentType(), containsString("application/json"));

        logger.info("Response format verified: JSON");
    }

    @Test(description = "Validate root response structure", priority = 3, dependsOnMethods = "testResponseStatus", groups = "bomb")
    @Story("Catalog Search - Seller Filter")
    @Severity(SeverityLevel.CRITICAL)
    public void testValidateRootResponseStructure() {
        // Validate response is an object
        assertThat("Response should be an object", catalogResponse, notNullValue());
        assertThat("Response should be an instance of CatalogResponse",
                catalogResponse, instanceOf(CatalogResponse.class));

        // Validate response includes required keys
        assertThat("StatusCode should not be null", catalogResponse.getStatusCode(), notNullValue());
        assertThat("Message should not be null", catalogResponse.getMessage(), notNullValue());
        assertThat("Data should not be null", catalogResponse.getData(), notNullValue());

        // Validate message is 'success'
        assertThat("Message should be 'success'",
                catalogResponse.getMessage(), equalTo("success"));

        logger.info("Root response structure validated successfully");
    }

    @Test(description = "Validate data object structure", priority = 4, dependsOnMethods = "testResponseStatus", groups = "bomb")
    @Story("Catalog Search - Seller Filter")
    @Severity(SeverityLevel.CRITICAL)
    public void testValidateDataObjectStructure() {
        // Validate data is an object
        assertThat("Data should be an object", catalogResponse.getData(), notNullValue());
        assertThat("Data should be an instance of CatalogData",
                catalogResponse.getData(), instanceOf(CatalogResponse.CatalogData.class));

        // Validate data includes required keys: total, items
        assertThat("Total should not be null", catalogResponse.getData().getTotal(), notNullValue());
        assertThat("Items should not be null", catalogResponse.getData().getItems(), notNullValue());

        // Validate total.value is a number and at least 0
        assertThat("Total value should be a number",
                catalogResponse.getData().getTotal().getValue(), instanceOf(Integer.class));
        assertThat("Total value should be at least 0",
                catalogResponse.getData().getTotal().getValue(), greaterThanOrEqualTo(0));

        // Validate items is an array
        assertThat("Items should be an array", catalogResponse.getData().getItems(), notNullValue());

        logger.info("Data object structure validated successfully");
    }

    @Test(description = "Verify seller IDs match request parameter", priority = 5, dependsOnMethods = "testResponseStatus", groups = "bomb")
    @Story("Catalog Search - Seller Filter")
    @Severity(SeverityLevel.CRITICAL)
    public void testVerifySellerIdsMatchRequestParameter() {
        // Verify seller IDs match the request parameter
        if (catalogResponse.getData().getItems() != null && catalogResponse.getData().getItems().size() > 0) {
            catalogResponse.getData().getItems().forEach(item -> {
                assertThat("Seller ID should match request parameter",
                        item.getSeller().get_id(), equalTo(SELLER_ID));
            });

            logger.info("All seller IDs match request parameter: {}", SELLER_ID);
        } else {
            logger.info("No items to validate seller IDs");
        }
    }

    @Test(description = "Validate buckets structure", priority = 6, dependsOnMethods = "testResponseStatus", groups = "bomb")
    @Story("Catalog Search - Seller Filter")
    @Severity(SeverityLevel.NORMAL)
    public void testValidateBucketsStructure() {
        // Validate buckets is an array and not empty
        assertThat("Buckets should be an array", catalogResponse.getData().getBuckets(), notNullValue());
        assertThat("Buckets should not be empty", catalogResponse.getData().getBuckets(), not(empty()));

        // Validate bucket structure
        catalogResponse.getData().getBuckets().forEach(bucket -> {
            assertThat("Bucket _id should be a string", bucket.get_id(), instanceOf(String.class));
            assertThat("Bucket name should be a string", bucket.getName(), instanceOf(String.class));
            assertThat("Bucket doc_count should be a number", bucket.getDoc_count(), instanceOf(Integer.class));
        });

        logger.info("Buckets structure validated successfully");
    }

    @Test(description = "Validate boolean flags", priority = 7, dependsOnMethods = "testResponseStatus", groups = "bomb")
    @Story("Catalog Search - Seller Filter")
    @Severity(SeverityLevel.NORMAL)
    public void testValidateBooleanFlags() {
        // Validate boolean flags if items exist
        if (catalogResponse.getData().getItems() != null && catalogResponse.getData().getItems().size() > 0) {
            CatalogResponse.CatalogItem item = catalogResponse.getData().getItems().get(0);

            assertThat("Item visible should be true", item.getVisible(), is(true));
            assertThat("Seller deprioritisation_status should be false",
                    item.getSeller().getDeprioritisation_status(), is(false));
            assertThat("Seller isCatalogAvailable should be true",
                    item.getSeller().getIsCatalogAvailable(), is(true));
            assertThat("Item available should be true", item.getAvailable(), is(true));

            logger.info("Boolean flags validated successfully");
        } else {
            logger.info("No items to validate boolean flags");
        }
    }

    @Test(description = "Validate product structure", priority = 8, dependsOnMethods = "testResponseStatus", groups = "bomb")
    @Story("Catalog Search - Seller Filter")
    @Severity(SeverityLevel.NORMAL)
    public void testValidateProductStructure() {
        // Validate product structure if items and products exist
        if (catalogResponse.getData().getItems() != null && catalogResponse.getData().getItems().size() > 0) {
            CatalogResponse.CatalogItem item = catalogResponse.getData().getItems().get(0);

            if (item.getProduct() != null && item.getProduct().size() > 0) {
                CatalogResponse.Product product = item.getProduct().get(0);

                // Validate product includes required keys
                assertThat("Product should include name", product.getName(), notNullValue());
                assertThat("Product should include id", product.getId(), notNullValue());

                // Validate data types
                assertThat("Product name should be a string", product.getName(), instanceOf(String.class));
                assertThat("Product id should be a string", product.getId(), instanceOf(String.class));

                // Validate product ID matches MongoDB ObjectId format (24 hex characters)
                assertThat("Product id should match MongoDB ObjectId format",
                        product.getId(), matchesRegex("^[0-9a-fA-F]{24}$"));

                logger.info("Product structure validated successfully");
            } else {
                logger.info("No products to validate");
            }
        } else {
            logger.info("No catalog items to validate products");
        }
    }

    @Test(description = "Verify pagination limit", priority = 9, dependsOnMethods = "testResponseStatus", groups = "bomb")
    @Story("Catalog Search - Seller Filter")
    @Severity(SeverityLevel.NORMAL)
    public void testVerifyPaginationLimit() {
        // Verify pagination limit is respected (limit=20)
        assertThat("Items count should not exceed limit of 20",
                catalogResponse.getData().getItems().size(), lessThanOrEqualTo(20));

        logger.info("Pagination limit verified: items count = {}", catalogResponse.getData().getItems().size());
    }

    @Test(description = "Validate catalog item structure", priority = 10, dependsOnMethods = "testResponseStatus", groups = "bomb")
    @Story("Catalog Search - Seller Filter")
    @Severity(SeverityLevel.CRITICAL)
    public void testValidateCatalogItemStructure() {
        // Validate catalog item structure if items exist
        if (catalogResponse.getData().getItems() != null && catalogResponse.getData().getItems().size() > 0) {
            CatalogResponse.CatalogItem item = catalogResponse.getData().getItems().get(0);

            // Validate item includes required keys
            assertThat("Item should include _id", item.get_id(), notNullValue());
            assertThat("Item should include title", item.getTitle(), notNullValue());
            assertThat("Item should include price", item.getPrice(), notNullValue());
            assertThat("Item should include sellerId", item.getSellerId(), notNullValue());
            assertThat("Item should include visible", item.getVisible(), notNullValue());
            assertThat("Item should include available", item.getAvailable(), notNullValue());

            // Validate data types
            assertThat("Item _id should be a string", item.get_id(), instanceOf(String.class));
            assertThat("Item title should be a string", item.getTitle(), instanceOf(String.class));
            assertThat("Item price should be a number", item.getPrice(), instanceOf(Double.class));
            assertThat("Item price should be above 0", item.getPrice(), greaterThan(0.0));

            logger.info("Catalog item structure validated successfully");
        } else {
            logger.info("No catalog items to validate");
        }
    }

    @Test(description = "Verify response performance", priority = 11, dependsOnMethods = "testResponseStatus", groups = "bomb")
    @Story("Catalog Search - Seller Filter")
    @Severity(SeverityLevel.NORMAL)
    public void testVerifyResponsePerformance() {
        // Get threshold from config or use default 40000ms
        long threshold = config.responseTimeThreshold();
        long actualResponseTime = response.getTime();

        // Verify response time is below threshold
        assertThat("Response time should be below threshold",
                actualResponseTime, lessThan(threshold));

        logger.info("Response performance verified: {} ms (Threshold: {} ms)", actualResponseTime, threshold);
    }

    @Test(description = "Verify security headers", priority = 12, dependsOnMethods = "testResponseStatus", groups = "bomb")
    @Story("Catalog Search - Seller Filter")
    @Severity(SeverityLevel.MINOR)
    public void testVerifySecurityHeaders() {
        // Verify Content-Type header includes application/json
        assertThat("Content-Type header should include application/json",
                response.getContentType(), containsString("application/json"));

        logger.info("Security headers verified: Content-Type = {}", response.getContentType());
    }

    @Test(description = "Set Id to live_catalog_id collection variable", priority = 13, dependsOnMethods = "testResponseStatus", groups = "bomb")
    @Story("Catalog Search - Seller Filter")
    @Severity(SeverityLevel.NORMAL)
    public void testSetLiveCatalogId() {
        // Store first catalog ID in static variable for use in other tests
        if (catalogResponse.getData().getItems() != null && catalogResponse.getData().getItems().size() > 0) {
            liveCatalogId = catalogResponse.getData().getItems().get(0).getId();

            assertThat("Live catalog ID should not be null", liveCatalogId, notNullValue());
            assertThat("Live catalog ID should not be empty", liveCatalogId, not(emptyOrNullString()));

            logger.info("Live catalog ID stored successfully: {}", liveCatalogId);
        } else {
            logger.warn("No items available to set live catalog ID");
        }
    }
}

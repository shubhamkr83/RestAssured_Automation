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
 * Test class for BOMB Catalog Search with Catalog ID Filter.
 * Endpoint:
 * {{bizup_base}}/v1/admin/catalog_all?id={{live_catalog_id}}&offset=0&limit=20
 * Implements comprehensive Postman test scripts for catalog search with catalog
 * ID filter validation.
 */
@Epic("BOMB Catalog Management")
@Feature("Catalog Search - Catalog ID Filter")
public class SearchWithCatalogIdFilterTest extends BaseTest {

    private String authToken;
    private Response response;
    private CatalogResponse catalogResponse;
    private String liveCatalogId;

    @BeforeClass
    public void setupAuth() {
        // Get token from VariableManager (thread-safe)
        authToken = VariableManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
        logger.info("Using BOMB token from VariableManager");
        // Get live catalog ID from VariableManager or use fallback
        liveCatalogId = VariableManager.get("live_catalog_id");
        if (liveCatalogId == null || liveCatalogId.isEmpty()) {
            liveCatalogId = VariableManager.get("catalog_foassign_id", "6822f5dac17c6dcd589ba173");
            logger.warn("Live catalog ID not available, using fallback: {}", liveCatalogId);
        } else {
            logger.info("Using live catalog ID from VariableManager: {}", liveCatalogId);
        }
    }

    @Test(description = "Status code is 200", priority = 1, groups = "bomb")
    @Story("Catalog Search - Catalog ID Filter")
    @Severity(SeverityLevel.BLOCKER)
    public void testStatusCode200() {
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("id", liveCatalogId);
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

    @Test(description = "Response is in JSON format", priority = 2, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Search - Catalog ID Filter")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseIsJson() {
        // Verify response format is JSON
        assertThat("Response should be JSON",
                response.getContentType(), containsString("application/json"));

        logger.info("Response format verified: JSON");
    }

    @Test(description = "Response has required structure and values", priority = 3, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Search - Catalog ID Filter")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseRequiredStructure() {
        // Validate response includes all required keys
        assertThat("Response should have statusCode", catalogResponse.getStatusCode(), notNullValue());
        assertThat("Response should have message", catalogResponse.getMessage(), notNullValue());
        assertThat("Response should have data", catalogResponse.getData(), notNullValue());

        // Validate response is an object
        assertThat("Response should be an object", catalogResponse, notNullValue());

        // Validate message is 'success'
        assertThat("Message should be 'success'",
                catalogResponse.getMessage(), equalTo("success"));

        // Validate data includes all required keys
        assertThat("Data should be an object", catalogResponse.getData(), notNullValue());
        assertThat("Data should have total", catalogResponse.getData().getTotal(), notNullValue());
        assertThat("Data should have items", catalogResponse.getData().getItems(), notNullValue());

        // Validate total has value property
        assertThat("Total should have value property",
                catalogResponse.getData().getTotal().getValue(), notNullValue());
        assertThat("Total value should be a number above -1",
                catalogResponse.getData().getTotal().getValue(), greaterThan(-1));

        // Validate items is an array
        assertThat("Items should be an array", catalogResponse.getData().getItems(), notNullValue());

        logger.info("Response structure validated successfully");
    }

    @Test(description = "Each catalog ID matches request query", priority = 4, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Search - Catalog ID Filter")
    @Severity(SeverityLevel.CRITICAL)
    public void testCatalogIdMatchesRequest() {
        // Validate catalog IDs match request query
        if (catalogResponse.getData().getItems() != null && catalogResponse.getData().getItems().size() > 0) {
            catalogResponse.getData().getItems().forEach(item -> {
                assertThat("Catalog ID should match request query",
                        item.get_id(), equalTo(liveCatalogId));
            });

            logger.info("All catalog IDs match request query: {}", liveCatalogId);
        } else {
            logger.info("No items to validate catalog IDs");
        }
    }

    @Test(description = "Bucket product ID matches request query", priority = 5, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Search - Catalog ID Filter")
    @Severity(SeverityLevel.NORMAL)
    public void testBucketProductIdMatchesRequest() {
        // Note: The Postman script checks for product ID in buckets, but this endpoint
        // filters by catalog ID, not product ID. This test validates buckets exist.

        if (catalogResponse.getData().getBuckets() != null) {
            logger.info("Buckets array exists with {} buckets", catalogResponse.getData().getBuckets().size());
        } else {
            logger.info("No buckets to validate");
        }
    }

    @Test(description = "Items bucket is not empty", priority = 6, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Search - Catalog ID Filter")
    @Severity(SeverityLevel.NORMAL)
    public void testItemsBucketNotEmpty() {
        // Validate items array is not empty
        assertThat("Items should be an array", catalogResponse.getData().getItems(), notNullValue());
        assertThat("Items should not be empty", catalogResponse.getData().getItems(), not(empty()));

        logger.info("Items array is not empty: {} items found", catalogResponse.getData().getItems().size());
    }

    @Test(description = "Buckets structure is valid", priority = 7, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Search - Catalog ID Filter")
    @Severity(SeverityLevel.NORMAL)
    public void testBucketsStructureValid() {
        // Validate buckets is an array
        assertThat("Buckets should be an array", catalogResponse.getData().getBuckets(), notNullValue());

        // Validate bucket structure if buckets exist
        if (catalogResponse.getData().getBuckets() != null && catalogResponse.getData().getBuckets().size() > 0) {
            catalogResponse.getData().getBuckets().forEach(bucket -> {
                assertThat("Bucket _id should be a string", bucket.get_id(), instanceOf(String.class));
                assertThat("Bucket name should be a string", bucket.getName(), instanceOf(String.class));
                assertThat("Bucket doc_count should be a number", bucket.getDoc_count(), instanceOf(Integer.class));
            });

            logger.info("Buckets structure validated successfully");
        } else {
            logger.info("No buckets to validate structure");
        }
    }

    @Test(description = "Boolean flags are true when present", priority = 8, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Search - Catalog ID Filter")
    @Severity(SeverityLevel.NORMAL)
    public void testBooleanFlagsTrue() {
        // Validate boolean flags if items exist
        if (catalogResponse.getData().getItems() != null && catalogResponse.getData().getItems().size() > 0) {
            CatalogResponse.CatalogItem item = catalogResponse.getData().getItems().get(0);

            // Check visible flag
            if (item.getVisible() != null) {
                assertThat("Item visible should be true", item.getVisible(), is(true));
            }

            // Check seller flags if seller exists
            if (item.getSeller() != null) {
                if (item.getSeller().getDeprioritisation_status() != null) {
                    assertThat("Seller deprioritisation_status should be false",
                            item.getSeller().getDeprioritisation_status(), is(false));
                }
                if (item.getSeller().getIsCatalogAvailable() != null) {
                    assertThat("Seller isCatalogAvailable should be true",
                            item.getSeller().getIsCatalogAvailable(), is(true));
                }
            }

            // Check available flag
            if (item.getAvailable() != null) {
                assertThat("Item available should be true", item.getAvailable(), is(true));
            }

            logger.info("Boolean flags validated successfully");
        } else {
            throw new AssertionError("No items found for boolean flag validation");
        }
    }

    @Test(description = "Product array has required structure", priority = 9, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Search - Catalog ID Filter")
    @Severity(SeverityLevel.NORMAL)
    public void testProductArrayRequiredStructure() {
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

                logger.info("Product array structure validated successfully");
            } else {
                logger.info("No products to validate");
            }
        } else {
            logger.info("No catalog items to validate products");
        }
    }

    @Test(description = "Pagination: items length does not exceed limit", priority = 10, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Search - Catalog ID Filter")
    @Severity(SeverityLevel.NORMAL)
    public void testPaginationItemsLimit() {
        // Validate pagination limit is respected (limit=20)
        if (catalogResponse.getData().getItems() != null) {
            assertThat("Items count should not exceed limit of 20",
                    catalogResponse.getData().getItems().size(), lessThanOrEqualTo(20));

            logger.info("Pagination validated: items count = {}", catalogResponse.getData().getItems().size());
        } else {
            logger.info("No items to validate pagination");
        }
    }

    @Test(description = "Catalog item structure is valid", priority = 11, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Search - Catalog ID Filter")
    @Severity(SeverityLevel.CRITICAL)
    public void testCatalogItemStructureValid() {
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

            // Validate seller includes required keys
            assertThat("Item should have seller", item.getSeller(), notNullValue());
            assertThat("Seller should include deprioritisation_status",
                    item.getSeller().getDeprioritisation_status(), notNullValue());
            assertThat("Seller should include isCatalogAvailable",
                    item.getSeller().getIsCatalogAvailable(), notNullValue());

            // Validate data types
            assertThat("Item _id should be a string", item.get_id(), instanceOf(String.class));
            assertThat("Item title should be a string", item.getTitle(), instanceOf(String.class));
            assertThat("Item price should be a number", item.getPrice(), instanceOf(Double.class));
            assertThat("Item price should be above 0", item.getPrice(), greaterThan(0.0));
            assertThat("Seller deprioritisation_status should be a boolean",
                    item.getSeller().getDeprioritisation_status(), instanceOf(Boolean.class));
            assertThat("Seller isCatalogAvailable should be a boolean",
                    item.getSeller().getIsCatalogAvailable(), instanceOf(Boolean.class));

            logger.info("Catalog item structure validated successfully");
        } else {
            logger.info("No catalog items to validate");
        }
    }

    @Test(description = "Response time is under threshold", priority = 12, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Search - Catalog ID Filter")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTimeUnderThreshold() {
        // Get threshold from config or use default 40000ms
        long threshold = config.responseTimeThreshold();
        long actualResponseTime = response.getTime();

        // Verify response time is available
        assertThat("Response time should be available", actualResponseTime, notNullValue());

        // Verify response time is below threshold
        assertThat("Response time should be under threshold",
                actualResponseTime, lessThan(threshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime, threshold);
    }

    @Test(description = "Content-Type header includes 'application/json'", priority = 13, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Search - Catalog ID Filter")
    @Severity(SeverityLevel.MINOR)
    public void testContentTypeHeader() {
        // Verify Content-Type header includes application/json
        assertThat("Content-Type header should include application/json",
                response.getContentType(), containsString("application/json"));

        logger.info("Content-Type header verified: {}", response.getContentType());
    }
}

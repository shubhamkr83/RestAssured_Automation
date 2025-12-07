package com.automation.tests.bomb;

import com.automation.base.BaseTest;
import com.automation.constants.BombEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.CatalogResponse;
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
 * Test class for BOMB Catalog Search API.
 * Covers various catalog search scenarios with different filters.
 */
@Epic("BOMB Catalog Management")
@Feature("Catalog Search API")
public class CatalogSearchApiTest extends BaseTest {

    private String authToken;
    public static String liveCatalogId;

    @BeforeClass
    public void setupAuth() {
        // Ensure login test runs first and token is available
        if (LoginApiTest.bombToken != null) {
            authToken = LoginApiTest.bombToken;
            logger.info("Using BOMB token from LoginApiTest");
        } else {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
    }

    @Test(description = "Verify catalog search returns all catalogs", priority = 1)
    @Story("Catalog Search - All Catalogs")
    @Severity(SeverityLevel.CRITICAL)
    public void testGetAllCatalogs() {
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("offset", 0);
        queryParams.put("limit", 20);

        Response response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .queryParams(queryParams)
                .when()
                .get(BombEndpoints.CATALOG_ALL);

        // Validate status code
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        // Validate response format
        assertThat("Response should be JSON",
                response.getContentType(), containsString("application/json"));

        // Parse and validate response structure
        CatalogResponse catalogResponse = JsonUtils.fromResponse(response, CatalogResponse.class);

        assertThat("Response should not be null", catalogResponse, notNullValue());
        assertThat("Message should be success",
                catalogResponse.getMessage(), equalTo("success"));
        assertThat("Data should not be null", catalogResponse.getData(), notNullValue());
        assertThat("Total should not be null", catalogResponse.getData().getTotal(), notNullValue());
        assertThat("Total value should be non-negative",
                catalogResponse.getData().getTotal().getValue(), greaterThanOrEqualTo(0));
        assertThat("Items should be a list", catalogResponse.getData().getItems(), notNullValue());

        // Validate buckets
        assertThat("Buckets should be a list", catalogResponse.getData().getBuckets(), notNullValue());

        // Validate bucket structure (if present)
        if (!catalogResponse.getData().getBuckets().isEmpty()) {
            CatalogResponse.Bucket bucket = catalogResponse.getData().getBuckets().get(0);
            assertThat("Bucket ID should be string", bucket.get_id(), instanceOf(String.class));
            assertThat("Bucket name should be string", bucket.getName(), instanceOf(String.class));
            assertThat("Bucket doc_count should be number", bucket.getDoc_count(), instanceOf(Integer.class));
        }

        // Validate catalog items (if present)
        if (!catalogResponse.getData().getItems().isEmpty()) {
            CatalogResponse.CatalogItem item = catalogResponse.getData().getItems().get(0);
            assertThat("Item ID should be present", item.get_id(), not(emptyOrNullString()));
            assertThat("Item title should be present", item.getTitle(), not(emptyOrNullString()));
            assertThat("Item price should be positive", item.getPrice(), greaterThan(0.0));
        }

        // Validate pagination
        assertThat("Items count should not exceed limit",
                catalogResponse.getData().getItems().size(), lessThanOrEqualTo(20));

        // Validate response time
        assertThat("Response time should be acceptable",
                response.getTime(), lessThan((long) config.responseTimeThreshold()));
    }

    @Test(description = "Verify catalog search with seller filter", priority = 2)
    @Story("Catalog Search - Seller Filter")
    @Severity(SeverityLevel.CRITICAL)
    public void testCatalogSearchWithSellerFilter() {
        String sellerId = "63ee780c9689be92acce8f35";

        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("seller", sellerId);
        queryParams.put("offset", 0);
        queryParams.put("limit", 20);

        Response response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .queryParams(queryParams)
                .when()
                .get(BombEndpoints.CATALOG_ALL);

        // Validate response
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        CatalogResponse catalogResponse = JsonUtils.fromResponse(response, CatalogResponse.class);

        assertThat("Message should be success",
                catalogResponse.getMessage(), equalTo("success"));
        assertThat("Data should contain items",
                catalogResponse.getData().getItems(), notNullValue());

        // Verify seller IDs match
        if (!catalogResponse.getData().getItems().isEmpty()) {
            catalogResponse.getData().getItems().forEach(item -> {
                assertThat("Seller ID should match filter",
                        item.getSeller().get_id(), equalTo(sellerId));
            });

            // Store first item's ID
            liveCatalogId = catalogResponse.getData().getItems().get(0).getId();
            logger.info("Stored live catalog ID: {}", liveCatalogId);
        }

        // Validate buckets structure
        assertThat("Buckets should not be empty",
                catalogResponse.getData().getBuckets(), not(empty()));

        catalogResponse.getData().getBuckets().forEach(bucket -> {
            assertThat("Bucket ID should be string", bucket.get_id(), instanceOf(String.class));
            assertThat("Bucket name should be string", bucket.getName(), instanceOf(String.class));
            assertThat("Bucket doc_count should be number", bucket.getDoc_count(), instanceOf(Integer.class));
        });

        // Validate boolean flags
        if (!catalogResponse.getData().getItems().isEmpty()) {
            CatalogResponse.CatalogItem item = catalogResponse.getData().getItems().get(0);
            assertThat("Visible should be true", item.getVisible(), is(true));
            assertThat("Deprioritisation status should be false",
                    item.getSeller().getDeprioritisation_status(), is(false));
            assertThat("Catalog available should be true",
                    item.getSeller().getIsCatalogAvailable(), is(true));
            assertThat("Available should be true", item.getAvailable(), is(true));
        }
    }

    @Test(description = "Verify catalog search with product filter", priority = 3)
    @Story("Catalog Search - Product Filter")
    @Severity(SeverityLevel.NORMAL)
    public void testCatalogSearchWithProductFilter() {
        String productId = "645b93e45c2997f4f2e82c50";

        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("product", productId);
        queryParams.put("offset", 0);
        queryParams.put("limit", 20);

        Response response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .queryParams(queryParams)
                .when()
                .get(BombEndpoints.CATALOG_ALL);

        // Validate response
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        CatalogResponse catalogResponse = JsonUtils.fromResponse(response, CatalogResponse.class);

        // Validate response structure
        assertThat("Response should have correct structure", catalogResponse, notNullValue());
        assertThat("Message should be success", catalogResponse.getMessage(), equalTo("success"));
        assertThat("Data should contain total", catalogResponse.getData().getTotal(), notNullValue());
        assertThat("Total value should be non-negative",
                catalogResponse.getData().getTotal().getValue(), greaterThanOrEqualTo(0));

        // Validate items array
        assertThat("Items should not be empty",
                catalogResponse.getData().getItems(), not(empty()));

        // Validate product structure
        if (!catalogResponse.getData().getItems().isEmpty()) {
            CatalogResponse.CatalogItem item = catalogResponse.getData().getItems().get(0);
            if (item.getProduct() != null && !item.getProduct().isEmpty()) {
                CatalogResponse.Product product = item.getProduct().get(0);
                assertThat("Product name should be present", product.getName(), not(emptyOrNullString()));
                assertThat("Product ID should match MongoDB format",
                        product.getId(), matchesRegex("^[0-9a-fA-F]{24}$"));
            }
        }

        // Validate catalog item structure
        if (!catalogResponse.getData().getItems().isEmpty()) {
            CatalogResponse.CatalogItem item = catalogResponse.getData().getItems().get(0);
            assertThat("Item ID should be present", item.get_id(), not(emptyOrNullString()));
            assertThat("Title should be present", item.getTitle(), not(emptyOrNullString()));
            assertThat("Price should be positive", item.getPrice(), greaterThan(0.0));
            assertThat("Seller should be present", item.getSeller(), notNullValue());
        }

        // Validate pagination
        assertThat("Items count should not exceed limit",
                catalogResponse.getData().getItems().size(), lessThanOrEqualTo(20));
    }

    @Test(description = "Verify catalog search with catalog ID filter", priority = 4)
    @Story("Catalog Search - Catalog ID Filter")
    @Severity(SeverityLevel.NORMAL)
    public void testCatalogSearchWithCatalogIdFilter() {
        // Use the stored catalog ID from previous test
        if (liveCatalogId == null) {
            logger.warn("Live catalog ID not set, using default");
            liveCatalogId = "6822f5dac17c6dcd589ba173";
        }

        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("id", liveCatalogId);
        queryParams.put("offset", 0);
        queryParams.put("limit", 20);

        Response response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .queryParams(queryParams)
                .when()
                .get(BombEndpoints.CATALOG_ALL);

        // Validate response
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        CatalogResponse catalogResponse = JsonUtils.fromResponse(response, CatalogResponse.class);

        // Validate response structure
        assertThat("Message should be success", catalogResponse.getMessage(), equalTo("success"));
        assertThat("Data total value should be non-negative",
                catalogResponse.getData().getTotal().getValue(), greaterThanOrEqualTo(0));

        // Verify catalog IDs match
        if (!catalogResponse.getData().getItems().isEmpty()) {
            catalogResponse.getData().getItems().forEach(item -> {
                assertThat("Catalog ID should match filter",
                        item.get_id(), equalTo(liveCatalogId));
            });
        }

        // Validate items are not empty
        assertThat("Items should not be empty",
                catalogResponse.getData().getItems(), not(empty()));

        // Validate buckets structure
        assertThat("Buckets should be present", catalogResponse.getData().getBuckets(), notNullValue());

        // Validate response time
        assertThat("Response time should be acceptable",
                response.getTime(), lessThan((long) config.responseTimeThreshold()));

        // Validate Content-Type header
        assertThat("Content-Type should be application/json",
                response.getContentType(), containsString("application/json"));
    }

    @Test(description = "Verify response headers contain correct Content-Type")
    @Story("Catalog Search - Headers")
    @Severity(SeverityLevel.MINOR)
    public void testSecurityHeaders() {
        Response response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .queryParam("offset", 0)
                .queryParam("limit", 20)
                .when()
                .get(BombEndpoints.CATALOG_ALL);

        assertThat("Content-Type header should include application/json",
                response.getContentType(), containsString("application/json"));
    }
}

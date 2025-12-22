package com.automation.tests.buyerapp.CollectionListing.AllCollectionCounts;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.CollectionAllResponse;
import com.automation.models.response.CollectionByIdResponse;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

import com.automation.utils.VariableManager;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for Collection Counts (Saree) - Converted from Postman Script.
 * Endpoint: {{navo_base}}/v1/collection/all?suitable_for=saree
 * Fetches all collections and validates item counts for each collection.
 */
@Epic("Buyer App Collection Listing")
@Feature("Collection Counts for Saree API")
public class CollectionCountsForSareeTest extends BaseTest {

    private static List<CollectionItemCount> collectionCounts;
    private String buyerAppBaseUrl;

    @BeforeClass
    public void setupBuyerApp() {
        buyerAppBaseUrl = config.buyerAppBaseUrl();
        logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);

        String token = VariableManager.getBuyerAppToken();
        if (token == null || token.isEmpty()) {
            throw new RuntimeException("Buyer App token not available. Please run LoginTest first.");
        }
        logger.info("Using Buyer App token from VariableManager");
    }

    @Test(description = "Fetch all collections and get item counts", priority = 1, groups = "buyerapp")
    @Story("Collection Counts for Saree")
    @Severity(SeverityLevel.CRITICAL)
    public void testFetchAllCollectionsAndCounts() {
        // Step 1: Get all collections
        Response allCollectionsResponse = RestAssured.given()
                .baseUri(buyerAppBaseUrl)
                .contentType("application/json")
                .header("Authorization", "JWT " + VariableManager.getBuyerAppToken())
                .queryParam("suitable_for", "saree")
                .when()
                .get(BuyerAppEndpoints.COLLECTION_ALL);

        CollectionAllResponse allCollectionsData = JsonUtils.fromResponse(allCollectionsResponse,
                CollectionAllResponse.class);

        assertThat("Status code should be 200", allCollectionsResponse.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat("Collections should not be empty", allCollectionsData.getData().getResult(), not(empty()));

        logger.info("Fetched {} collections for Saree", allCollectionsData.getData().getResult().size());

        // Step 2: Iterate through each collection and get item counts
        collectionCounts = new ArrayList<>();

        for (CollectionAllResponse.CollectionItem collection : allCollectionsData.getData().getResult()) {
            try {
                // Add delay to avoid rate limiting
                Thread.sleep(200);

                Response collectionByIdResponse = RestAssured.given()
                        .baseUri(buyerAppBaseUrl)
                        .contentType("application/json")
                        .header("Authorization", "JWT " + VariableManager.getBuyerAppToken())
                        .queryParam("limit", 35)
                        .queryParam("offset", 0)
                        .when()
                        .get("/v1/collection/" + collection.get_id());

                if (collectionByIdResponse.getStatusCode() == 401 || collectionByIdResponse.getStatusCode() == 10003) {
                    logger.error("Authentication failed for collection: {}", collection.getName());
                    collectionCounts
                            .add(new CollectionItemCount(collection.get_id(), collection.getName(), -1, "AUTH_ERROR"));
                    break; // Stop processing on auth error
                } else if (collectionByIdResponse.getStatusCode() == 200) {
                    CollectionByIdResponse collectionData = JsonUtils.fromResponse(collectionByIdResponse,
                            CollectionByIdResponse.class);
                    int totalItems = collectionData.getData().getTotal().getValue();
                    collectionCounts
                            .add(new CollectionItemCount(collection.get_id(), collection.getName(), totalItems, null));
                    logger.info("Collection: {} - Items: {}", collection.getName(), totalItems);
                } else {
                    logger.error("API error for collection: {} - Status: {}", collection.getName(),
                            collectionByIdResponse.getStatusCode());
                    collectionCounts
                            .add(new CollectionItemCount(collection.get_id(), collection.getName(), -1, "API_ERROR"));
                }
            } catch (Exception e) {
                logger.error("Error processing collection: {} - {}", collection.getName(), e.getMessage());
                collectionCounts.add(new CollectionItemCount(collection.get_id(), collection.getName(), -1, "ERROR"));
            }
        }

        logger.info("Processing completed for {} collections", collectionCounts.size());
    }

    @Test(description = "No authentication errors occurred", priority = 2, dependsOnMethods = "testFetchAllCollectionsAndCounts", groups = "buyerapp")
    @Story("Collection Counts for Saree")
    @Severity(SeverityLevel.BLOCKER)
    public void testNoAuthenticationErrors() {
        boolean hasAuthError = collectionCounts.stream().anyMatch(c -> "AUTH_ERROR".equals(c.getError()));
        assertThat("No authentication errors should occur", hasAuthError, is(false));

        logger.info("No authentication errors found");
    }

    @Test(description = "Total items retrieved", priority = 3, dependsOnMethods = "testFetchAllCollectionsAndCounts", groups = "buyerapp")
    @Story("Collection Counts for Saree")
    @Severity(SeverityLevel.CRITICAL)
    public void testTotalItemsRetrieved() {
        int totalAllItems = collectionCounts.stream()
                .filter(c -> c.getTotalItems() >= 0)
                .mapToInt(CollectionItemCount::getTotalItems)
                .sum();

        assertThat("Total items should be greater than 0", totalAllItems, greaterThan(0));

        logger.info("Total items across all collections: {}", totalAllItems);
    }

    @Test(description = "All collections have item counts", priority = 4, dependsOnMethods = "testFetchAllCollectionsAndCounts", groups = "buyerapp")
    @Story("Collection Counts for Saree")
    @Severity(SeverityLevel.CRITICAL)
    public void testAllCollectionsHaveItemCounts() {
        long successCount = collectionCounts.stream().filter(c -> c.getTotalItems() >= 0).count();

        assertThat(String.format("All %d collections should have item counts", collectionCounts.size()),
                successCount, equalTo((long) collectionCounts.size()));

        logger.info("All {} collections have valid item counts", successCount);
    }

    @Test(description = "Each collection has valid item count", priority = 5, dependsOnMethods = "testFetchAllCollectionsAndCounts", groups = "buyerapp")
    @Story("Collection Counts for Saree")
    @Severity(SeverityLevel.CRITICAL)
    public void testEachCollectionHasValidItemCount() {
        for (CollectionItemCount collection : collectionCounts) {
            assertThat(String.format("%s collection should have valid item count", collection.getName()),
                    collection.getTotalItems(), greaterThanOrEqualTo(0));
        }

        logger.info("Each collection validated for item count");
    }

    @Test(description = "No empty collections found", priority = 6, dependsOnMethods = "testFetchAllCollectionsAndCounts", groups = "buyerapp")
    @Story("Collection Counts for Saree")
    @Severity(SeverityLevel.NORMAL)
    public void testNoEmptyCollections() {
        long emptyCollectionsCount = collectionCounts.stream()
                .filter(c -> c.getTotalItems() == 0)
                .count();

        assertThat("No empty collections should be found", emptyCollectionsCount, equalTo(0L));

        logger.info("No empty collections found");
    }

    @Test(description = "Average items per collection is reasonable", priority = 7, dependsOnMethods = "testFetchAllCollectionsAndCounts", groups = "buyerapp")
    @Story("Collection Counts for Saree")
    @Severity(SeverityLevel.NORMAL)
    public void testAverageItemsPerCollection() {
        long successCount = collectionCounts.stream().filter(c -> c.getTotalItems() >= 0).count();
        int totalAllItems = collectionCounts.stream()
                .filter(c -> c.getTotalItems() >= 0)
                .mapToInt(CollectionItemCount::getTotalItems)
                .sum();

        double avgItems = successCount > 0 ? (double) totalAllItems / successCount : 0;

        assertThat("Average items per collection should be greater than 0", avgItems, greaterThan(0.0));

        logger.info("Average items per collection: {}", avgItems);
    }

    // Helper class to store collection count data
    @Data
    @AllArgsConstructor
    private static class CollectionItemCount {
        private String id;
        private String name;
        private int totalItems;
        private String error;
    }
}

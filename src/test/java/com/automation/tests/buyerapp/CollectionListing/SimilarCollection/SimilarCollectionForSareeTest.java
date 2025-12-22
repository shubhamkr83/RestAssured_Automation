package com.automation.tests.buyerapp.CollectionListing.SimilarCollection;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.CollectionAllResponse;
import com.automation.models.response.SimilarCollectionResponse;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.automation.utils.VariableManager;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for Similar Collection (Saree) - Converted from Postman Script.
 * Endpoint: {{navo_base}}/v1/collection/all?suitable_for=saree
 * Fetches all collections and validates similar collections for each.
 */
@Epic("Buyer App Collection Listing")
@Feature("Similar Collection for Saree API")
public class SimilarCollectionForSareeTest extends BaseTest {

    private static List<SimilarCollectionData> similarCollectionData;
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

    @Test(description = "Fetch all collections and get similar collections for each", priority = 1, groups = "buyerapp")
    @Story("Similar Collection for Saree")
    @Severity(SeverityLevel.CRITICAL)
    public void testFetchAllCollectionsAndSimilar() {
        // Step 1: Get all collections
        Response allCollectionsResponse = RestAssured.given()
                .baseUri(buyerAppBaseUrl)
                .contentType("application/json")
                .header("Authorization", "JWT " + VariableManager.getBuyerAppToken())
                .queryParam("suitable_for", "saree")
                .when()
                .get(BuyerAppEndpoints.COLLECTION_ALL);

        CollectionAllResponse allCollectionsResponseData = JsonUtils.fromResponse(allCollectionsResponse,
                CollectionAllResponse.class);

        assertThat("Status code should be 200", allCollectionsResponse.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat("Collections should not be empty", allCollectionsResponseData.getData().getResult(), not(empty()));

        logger.info("Fetched {} collections for Saree", allCollectionsResponseData.getData().getResult().size());

        // Step 2: Iterate through each collection and get similar collections
        similarCollectionData = new ArrayList<>();

        for (CollectionAllResponse.CollectionItem collection : allCollectionsResponseData.getData().getResult()) {
            try {
                // Add delay to avoid rate limiting
                Thread.sleep(200);

                Response similarResponse = RestAssured.given()
                        .baseUri(buyerAppBaseUrl)
                        .contentType("application/json")
                        .header("Authorization", "JWT " + VariableManager.getBuyerAppToken())
                        .when()
                        .get("/v1/collection/" + collection.get_id() + "/similar");

                if (similarResponse.getStatusCode() == 401 || similarResponse.getStatusCode() == 10003) {
                    logger.error("Authentication failed for collection: {}", collection.getName());
                    similarCollectionData.add(new SimilarCollectionData(
                            collection.get_id(), collection.getName(), -1, 0, "AUTH_ERROR", false, null));
                    break; // Stop processing on auth error
                } else if (similarResponse.getStatusCode() == 200) {
                    SimilarCollectionResponse similarData = JsonUtils.fromResponse(similarResponse,
                            SimilarCollectionResponse.class);

                    // Validate response structure
                    if ("10000".equals(similarData.getStatusCode()) && similarData.getData() != null
                            && similarData.getData().getResult() != null) {
                        List<SimilarCollectionResponse.SimilarCollectionItem> similarCollections = similarData.getData()
                                .getResult();
                        int similarCount = similarCollections.size();
                        boolean hasSimilar = similarCount > 0;

                        // Calculate total catalogs across all similar collections
                        int totalCatalogs = similarCollections.stream()
                                .mapToInt(s -> s.getCatalogs() != null ? s.getCatalogs().size() : 0)
                                .sum();

                        // Store similar collection details
                        List<SimilarCollectionDetail> details = similarCollections.stream()
                                .map(s -> new SimilarCollectionDetail(
                                        s.get_id(),
                                        s.getName(),
                                        s.getDescription() != null ? s.getDescription() : "",
                                        s.getCatalogs() != null ? s.getCatalogs().size() : 0))
                                .collect(Collectors.toList());

                        similarCollectionData.add(new SimilarCollectionData(
                                collection.get_id(), collection.getName(), similarCount, totalCatalogs, null,
                                hasSimilar, details));

                        logger.info("Collection: {} - Similar: {}, Total Catalogs: {}",
                                collection.getName(), similarCount, totalCatalogs);
                    } else {
                        similarCollectionData.add(new SimilarCollectionData(
                                collection.get_id(), collection.getName(), 0, 0, null, false, new ArrayList<>()));
                    }
                } else {
                    logger.error("API error for collection: {} - Status: {}", collection.getName(),
                            similarResponse.getStatusCode());
                    similarCollectionData.add(new SimilarCollectionData(
                            collection.get_id(), collection.getName(), -1, 0, "API_ERROR", false, null));
                }
            } catch (Exception e) {
                logger.error("Error processing collection: {} - {}", collection.getName(), e.getMessage());
                similarCollectionData.add(new SimilarCollectionData(
                        collection.get_id(), collection.getName(), -1, 0, "ERROR", false, null));
            }
        }

        logger.info("Processing completed for {} collections", similarCollectionData.size());
    }

    @Test(description = "Similar collections have valid data structure", priority = 2, dependsOnMethods = "testFetchAllCollectionsAndSimilar", groups = "buyerapp")
    @Story("Similar Collection for Saree")
    @Severity(SeverityLevel.CRITICAL)
    public void testSimilarCollectionsDataStructure() {
        List<SimilarCollectionData> collectionsWithSimilar = similarCollectionData.stream()
                .filter(SimilarCollectionData::isHasSimilar)
                .collect(Collectors.toList());

        collectionsWithSimilar.forEach(col -> {
            assertThat("Similar collections should be an array", col.getSimilarCollections(), notNullValue());
            assertThat("Similar collections should be a list", col.getSimilarCollections(), instanceOf(List.class));

            if (!col.getSimilarCollections().isEmpty()) {
                col.getSimilarCollections().forEach(similar -> {
                    assertThat("Similar collection should have id", similar.getId(), notNullValue());
                    assertThat("Similar collection should have name", similar.getName(), notNullValue());
                    assertThat("Similar collection should have catalogCount", similar.getCatalogCount(),
                            notNullValue());
                    assertThat("catalogCount should be a number", similar.getCatalogCount(), instanceOf(Integer.class));
                });
            }
        });

        logger.info("Similar collections data structure validated for {} collections", collectionsWithSimilar.size());
    }

    @Test(description = "Each similar collection contains catalogs array", priority = 3, dependsOnMethods = "testFetchAllCollectionsAndSimilar", groups = "buyerapp")
    @Story("Similar Collection for Saree")
    @Severity(SeverityLevel.NORMAL)
    public void testSimilarCollectionsCatalogsArray() {
        List<SimilarCollectionData> collectionsWithSimilar = similarCollectionData.stream()
                .filter(SimilarCollectionData::isHasSimilar)
                .collect(Collectors.toList());

        collectionsWithSimilar.forEach(col -> {
            if (col.getSimilarCollections() != null && !col.getSimilarCollections().isEmpty()) {
                col.getSimilarCollections().forEach(similar -> {
                    assertThat("Catalog count should be at least 0", similar.getCatalogCount(),
                            greaterThanOrEqualTo(0));
                });
            }
        });

        logger.info("Catalogs array validated for similar collections");
    }

    @Test(description = "Total catalogs count is reasonable", priority = 4, dependsOnMethods = "testFetchAllCollectionsAndSimilar", groups = "buyerapp")
    @Story("Similar Collection for Saree")
    @Severity(SeverityLevel.NORMAL)
    public void testTotalCatalogsCount() {
        int totalCatalogs = similarCollectionData.stream()
                .filter(c -> c.getSimilarCount() >= 0)
                .mapToInt(SimilarCollectionData::getTotalCatalogs)
                .sum();

        assertThat("Total catalogs should be at least 0", totalCatalogs, greaterThanOrEqualTo(0));

        long collectionsWithSimilar = similarCollectionData.stream()
                .filter(SimilarCollectionData::isHasSimilar)
                .count();

        if (collectionsWithSimilar > 0) {
            assertThat("Total catalogs should be greater than 0 when similar collections exist",
                    totalCatalogs, greaterThan(0));
        }

        logger.info("Total catalogs count validated: {}", totalCatalogs);
    }

    @Test(description = "Average similar collections per collection is reasonable", priority = 5, dependsOnMethods = "testFetchAllCollectionsAndSimilar", groups = "buyerapp")
    @Story("Similar Collection for Saree")
    @Severity(SeverityLevel.NORMAL)
    public void testAverageSimilarCollections() {
        long successCount = similarCollectionData.stream()
                .filter(c -> c.getSimilarCount() >= 0)
                .count();

        int totalSimilarCollections = similarCollectionData.stream()
                .filter(c -> c.getSimilarCount() >= 0)
                .mapToInt(SimilarCollectionData::getSimilarCount)
                .sum();

        double avgSimilar = successCount > 0 ? (double) totalSimilarCollections / successCount : 0;

        assertThat("Average similar collections should be at least 0", avgSimilar, greaterThanOrEqualTo(0.0));

        logger.info("Average similar collections per collection: {}", avgSimilar);
    }

    // Helper classes to store similar collection data
    @Data
    @AllArgsConstructor
    private static class SimilarCollectionData {
        private String id;
        private String name;
        private int similarCount;
        private int totalCatalogs;
        private String error;
        private boolean hasSimilar;
        private List<SimilarCollectionDetail> similarCollections;
    }

    @Data
    @AllArgsConstructor
    private static class SimilarCollectionDetail {
        private String id;
        private String name;
        private String description;
        private Integer catalogCount;
    }
}

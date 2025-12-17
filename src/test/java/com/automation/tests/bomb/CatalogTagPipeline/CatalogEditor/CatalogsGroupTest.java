package com.automation.tests.bomb.CatalogTagPipeline.CatalogEditor;

import com.automation.base.BaseTest;
import com.automation.constants.BombEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.CatalogGroupResponse;
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
 * Test class for BOMB Catalog Editor - Catalogs Group endpoint.
 * Endpoint:
 * {{bizup_base}}/v1/admin/catalog/upload/{{catalog_foassign_id}}?limit=20&mode=all
 * Implements comprehensive Postman test scripts for catalog group validation.
 */
@Epic("BOMB Catalog Tag Pipeline")
@Feature("Catalog Editor")
public class CatalogsGroupTest extends BaseTest {

    private String authToken;
    private Response response;
    private CatalogGroupResponse catalogGroupResponse;

    // Catalog ID and Seller ID
    private static final String SELLER_ID = VariableManager.getSellerId();
    private String catalogForAssignId;
    private String sellerId;

    @BeforeClass
    public void setupAuth() {
        // Get token from VariableManager (thread-safe)
        authToken = VariableManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
        logger.info("Using BOMB token from VariableManager");

        // Get catalog ID from VariableManager or use fallback
        catalogForAssignId = VariableManager.get("catalog_foassign_id");
        if (catalogForAssignId == null || catalogForAssignId.isEmpty()) {
            catalogForAssignId = "6822f5dac17c6dcd589ba173"; // Fallback to a default ID
            logger.warn("Catalog for assign ID not available, using default: {}", catalogForAssignId);
        } else {
            logger.info("Using catalog for assign ID from VariableManager: {}", catalogForAssignId);
        }

        // Get seller ID from VariableManager or use fallback
        sellerId = VariableManager.get("seller_id");
        if (sellerId == null || sellerId.isEmpty()) {
            sellerId = "609341142100000000000000"; // Fallback to a default seller ID
            logger.warn("Seller ID not available, using default: {}", sellerId);
        } else {
            logger.info("Using seller ID from VariableManager: {}", sellerId);
        }
    }

    @Test(description = "Response status code is 200", priority = 1, groups = "bomb")
    @Story("Catalog Editor - Catalogs Group")
    @Severity(SeverityLevel.BLOCKER)
    public void testStatusCode200() {
        // Send GET request to fetch catalog group
        response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .queryParam("limit", 20)
                .queryParam("seller_id", sellerId)
                .queryParam("mode", "all")
                .when()
                .get(BombEndpoints.CATALOG_UPLOAD + "/" + catalogForAssignId);

        // Parse response for other tests
        catalogGroupResponse = JsonUtils.fromResponse(response, CatalogGroupResponse.class);

        // Verify response status is 200 OK
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response time is less than threshold", priority = 2, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalogs Group")
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

    @Test(description = "Data object has the expected schema", priority = 3, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalogs Group")
    @Severity(SeverityLevel.CRITICAL)
    public void testDataObjectSchema() {
        // Validate data object structure
        assertThat("Response data should not be null", catalogGroupResponse.getData(), notNullValue());
        assertThat("Data should have total", catalogGroupResponse.getData().getTotal(), notNullValue());
        assertThat("Data should have data array", catalogGroupResponse.getData().getData(), notNullValue());

        // Validate each item in data array
        catalogGroupResponse.getData().getData().forEach(item -> {
            assertThat("Item should have _index", item.get_index(), notNullValue());
            assertThat("Item should have _id", item.get_id(), notNullValue());
            assertThat("Item should have _score", item.get_score(), notNullValue());
            assertThat("Item should have _source", item.get_source(), notNullValue());

            CatalogGroupResponse.CatalogSource source = item.get_source();
            CatalogGroupResponse.Seller seller = source.getSeller();

            // Validate seller object
            assertThat("Seller should have phoneNumber", seller.getPhoneNumber(), notNullValue());
            assertThat("Seller should have smell_test", seller.getSmell_test(), notNullValue());
            assertThat("Seller should have address", seller.getAddress(), notNullValue());
            assertThat("Seller should have isSuper", seller.getIsSuper(), notNullValue());
            assertThat("Seller should have mov", seller.getMov(), notNullValue());
            assertThat("Seller should have businessName", seller.getBusinessName(), notNullValue());
            assertThat("Seller should have name", seller.getName(), notNullValue());
            assertThat("Seller should have cod", seller.getCod(), notNullValue());
            assertThat("Seller should have _id", seller.get_id(), notNullValue());
            assertThat("Seller should have deprioritisation_status", seller.getDeprioritisation_status(),
                    notNullValue());
            assertThat("Seller should have isCatalogAvailable", seller.getIsCatalogAvailable(), notNullValue());

            // Validate source fields
            assertThat("Source should have thubmbnailDriveLink", source.getThubmbnailDriveLink(), notNullValue());
            assertThat("Source should have uploadId", source.getUploadId(), notNullValue());
            assertThat("Source should have displaySetting", source.getDisplaySetting(), notNullValue());
            assertThat("Source should have available", source.getAvailable(), notNullValue());
            assertThat("Source should have priceTags", source.getPriceTags(), notNullValue());
            assertThat("Source should have sellerId", source.getSellerId(), notNullValue());
            assertThat("Source should have isDeleted", source.getIsDeleted(), notNullValue());
            assertThat("Source should have displayTags", source.getDisplayTags(), notNullValue());
            assertThat("Source should have driveLink", source.getDriveLink(), notNullValue());
            assertThat("Source should have id", source.getId(), notNullValue());
            assertThat("Source should have contentType", source.getContentType(), notNullValue());
            assertThat("Source should have uploadedBy", source.getUploadedBy(), notNullValue());
            assertThat("Source should have segmentTags", source.getSegmentTags(), notNullValue());
            assertThat("Source should have updatedAt", source.getUpdatedAt(), notNullValue());
            assertThat("Source should have thumbnail", source.getThumbnail(), notNullValue());
            assertThat("Source should have visible", source.getVisible(), notNullValue());
            assertThat("Source should have isNew", source.getIsNew(), notNullValue());
            assertThat("Source should have setType", source.getSetType(), notNullValue());
            assertThat("Source should have url", source.getUrl(), notNullValue());
            assertThat("Source should have tags", source.getTags(), notNullValue());
            assertThat("Source should have processed", source.getProcessed(), notNullValue());
            assertThat("Source should have phoneNumber", source.getPhoneNumber(), notNullValue());
            assertThat("Source should have uploadDate", source.getUploadDate(), notNullValue());
            assertThat("Source should have original_upload", source.getOriginal_upload(), notNullValue());
            assertThat("Source should have productTags", source.getProductTags(), notNullValue());
        });

        logger.info("Data object schema validated successfully");
    }

    @Test(description = "Validate the seller object schema", priority = 4, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalogs Group")
    @Severity(SeverityLevel.CRITICAL)
    public void testSellerObjectSchema() {
        // Validate seller object field types
        catalogGroupResponse.getData().getData().forEach(item -> {
            CatalogGroupResponse.Seller seller = item.get_source().getSeller();

            assertThat("Seller should be an object", seller, notNullValue());
            assertThat("phoneNumber should be a string", seller.getPhoneNumber(), instanceOf(String.class));
            assertThat("smell_test should be a number", seller.getSmell_test(), instanceOf(Integer.class));
            assertThat("address should be a string", seller.getAddress(), instanceOf(String.class));
            assertThat("isSuper should be a boolean", seller.getIsSuper(), instanceOf(Boolean.class));
            assertThat("mov should be a number", seller.getMov(), instanceOf(Integer.class));
            assertThat("businessName should be a string", seller.getBusinessName(), instanceOf(String.class));
            assertThat("name should be a string", seller.getName(), instanceOf(String.class));
            assertThat("cod should be a number", seller.getCod(), instanceOf(Integer.class));
            assertThat("deprioritisation_status should be a boolean", seller.getDeprioritisation_status(),
                    instanceOf(Boolean.class));
            assertThat("isCatalogAvailable should be a boolean", seller.getIsCatalogAvailable(),
                    instanceOf(Boolean.class));
            assertThat("ordersEnabled should be a boolean", seller.getOrdersEnabled(), instanceOf(Boolean.class));
        });

        logger.info("Seller object schema validated successfully");
    }

    @Test(description = "Data array contains at least one element", priority = 5, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalogs Group")
    @Severity(SeverityLevel.NORMAL)
    public void testDataArrayNotEmpty() {
        // Validate data array is not empty
        assertThat("Data array should not be empty",
                catalogGroupResponse.getData().getData(), not(empty()));

        logger.info("Data array validated: contains {} element(s)",
                catalogGroupResponse.getData().getData().size());
    }

    @Test(description = "All catalog items belong to requested seller", priority = 6, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalogs Group")
    @Severity(SeverityLevel.CRITICAL)
    public void testAllItemsBelongToRequestedSeller() {
        // Validate all items belong to the requested seller
        assertThat("Data array should not be empty",
                catalogGroupResponse.getData().getData(), not(empty()));

        catalogGroupResponse.getData().getData().forEach(item -> {
            assertThat(String.format("Item %s should belong to seller %s",
                    item.get_id(), SELLER_ID),
                    item.get_source().getSeller().get_id(), equalTo(SELLER_ID));
        });

        logger.info("All {} catalog items verified to belong to seller: {}",
                catalogGroupResponse.getData().getData().size(), SELLER_ID);
    }
}

package com.automation.tests.bomb.CatalogTagPipeline.CatalogEditor;

import com.automation.base.BaseTest;
import com.automation.constants.BombEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.CatalogTagResponse;
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
 * Test class for BOMB Catalog Editor - Catalog Tag endpoint.
 * Endpoint:
 * {{bizup_base}}/v1/admin/catalog/group/upload/{{catalog_foassign_id}}?limit=20&mode=add
 * Implements comprehensive Postman test scripts for catalog tagging validation.
 */
@TmsLink("TC-ADMIN-005")
@TmsLink("TC-ADMIN-006")
@TmsLink("TC-ADMIN-007")
@Epic("BOMB Catalog Tag Pipeline")
@Feature("Catalog Editor")
public class CatalogTagTest extends BaseTest {

    private String authToken;
    private Response response;
    private CatalogTagResponse catalogTagResponse;

    // Catalog ID and Seller ID
    private String sellerId;
    private String catalogForAssignId;

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

        // Get catalog ID from VariableManager or use fallback
        catalogForAssignId = VariableManager.get("catalog_foassign_id");
        if (catalogForAssignId == null || catalogForAssignId.isEmpty()) {
            catalogForAssignId = VariableManager.get("catalog_foassign_id", "6822f5dac17c6dcd589ba173");
            logger.warn("Catalog for assign ID not available, using default: {}", catalogForAssignId);
        } else {
            logger.info("Using catalog for assign ID from VariableManager: {}", catalogForAssignId);
        }
    }

    @Test(description = "Response status code is 200", priority = 1, groups = "bomb")
    @Story("Catalog Editor - Catalog Tag")
    @Severity(SeverityLevel.BLOCKER)
    public void testStatusCode200() {
        // Send GET request to fetch catalog tag data
        response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .queryParam("seller_id", VariableManager.get("seller_id"))
                .queryParam("mode", "add")
                .when()
                .get(BombEndpoints.CATALOG_GROUP_UPLOAD + "/" + catalogForAssignId);

        // Parse response for other tests
        catalogTagResponse = JsonUtils.fromResponse(response, CatalogTagResponse.class);

        // Verify response status is 200 OK
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Content-Type header is application/json", priority = 2, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Tag")
    @Severity(SeverityLevel.NORMAL)
    public void testContentTypeHeader() {
        // Verify Content-Type header
        assertThat("Content-Type should be application/json",
                response.getContentType(), containsString("application/json"));

        logger.info("Content-Type header verified: {}", response.getContentType());
    }

    @Test(description = "Response has the required fields - statusCode, message, and data", priority = 3, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Tag")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseHasRequiredFields() {
        // Validate response has required fields
        assertThat("Response should not be null", catalogTagResponse, notNullValue());
        assertThat("Response should have statusCode", catalogTagResponse.getStatusCode(), notNullValue());
        assertThat("Response should have message", catalogTagResponse.getMessage(), notNullValue());
        assertThat("Response should have data", catalogTagResponse.getData(), notNullValue());

        logger.info("Response has all required fields");
    }

    @Test(description = "Response time is less than threshold", priority = 4, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Tag")
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

    @Test(description = "Response data object has basic fields", priority = 5, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Tag")
    @Severity(SeverityLevel.CRITICAL)
    public void testDataObjectHasBasicFields() {
        CatalogTagResponse.CatalogTagData data = catalogTagResponse.getData();

        // Validate data object has basic fields
        assertThat("Data should not be null", data, notNullValue());
        assertThat("Data _id should be a string", data.get_id(), instanceOf(String.class));
        assertThat("Data name should be a string", data.getName(), instanceOf(String.class));
        assertThat("Data total should be a number", data.getTotal(), instanceOf(Integer.class));

        logger.info("Data object basic fields validated: _id={}, name={}, total={}",
                data.get_id(), data.getName(), data.getTotal());
    }

    @Test(description = "Data array items have the required structure", priority = 6, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Tag")
    @Severity(SeverityLevel.CRITICAL)
    public void testDataArrayItemsStructure() {
        // Validate data array items structure
        assertThat("Data array should not be null", catalogTagResponse.getData().getData(), notNullValue());

        catalogTagResponse.getData().getData().forEach(item -> {
            assertThat("Item should be an object", item, notNullValue());
            assertThat("Item _index should be a string", item.get_index(), instanceOf(String.class));
            assertThat("Item _id should be a string", item.get_id(), instanceOf(String.class));
            assertThat("Item _source should be an object", item.get_source(), notNullValue());
        });

        logger.info("Data array items structure validated successfully");
    }

    @Test(description = "Source objects contain required basic fields", priority = 8, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Tag")
    @Severity(SeverityLevel.CRITICAL)
    public void testSourceObjectsBasicFields() {
        // Validate source objects basic fields
        catalogTagResponse.getData().getData().forEach(item -> {
            CatalogTagResponse.CatalogTagSource source = item.get_source();

            assertThat("thubmbnailDriveLink should be a string", source.getThubmbnailDriveLink(),
                    instanceOf(String.class));
            assertThat("uploadId should be a string", source.getUploadId(), instanceOf(String.class));
            assertThat("priceTags should be an array", source.getPriceTags(), notNullValue());
            // description can be string or null
            if (source.getDescription() != null) {
                assertThat("description should be a string", source.getDescription(), instanceOf(String.class));
            }
            assertThat("uploadedDate should be a string", source.getUploadedDate(), instanceOf(String.class));
            assertThat("createdAt should be a string", source.getCreatedAt(), instanceOf(String.class));
            assertThat("sellerId should be a string", source.getSellerId(), instanceOf(String.class));
            assertThat("isDeleted should be false", source.getIsDeleted(), is(false));
        });

        logger.info("Source objects basic fields validated successfully");
    }

    @Test(description = "Thumbnail objects have correct structure", priority = 9, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Tag")
    @Severity(SeverityLevel.NORMAL)
    public void testThumbnailObjectsStructure() {
        // Validate thumbnail objects
        catalogTagResponse.getData().getData().forEach(item -> {
            CatalogTagResponse.Thumbnail thumbnail = item.get_source().getThumbnail();

            assertThat("Thumbnail should be an object", thumbnail, notNullValue());
            assertThat("fileName should be a string", thumbnail.getFileName(), instanceOf(String.class));
            assertThat("mimeType should be a string", thumbnail.getMimeType(), instanceOf(String.class));
            assertThat("_id should be a string", thumbnail.get_id(), instanceOf(String.class));
            assertThat("url should be a string", thumbnail.getUrl(), instanceOf(String.class));
        });

        logger.info("Thumbnail objects structure validated successfully");
    }

    @Test(description = "Source objects have all required additional fields", priority = 10, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Tag")
    @Severity(SeverityLevel.NORMAL)
    public void testSourceObjectsAdditionalFields() {
        // Validate source objects additional fields
        catalogTagResponse.getData().getData().forEach(item -> {
            CatalogTagResponse.CatalogTagSource source = item.get_source();

            assertThat("phoneNumber should be a string", source.getPhoneNumber(), instanceOf(String.class));
            assertThat("uploadDate should be a string", source.getUploadDate(), instanceOf(String.class));
            assertThat("original_upload should be a string", source.getOriginal_upload(), instanceOf(String.class));
            assertThat("productTags should be an array", source.getProductTags(), notNullValue());
        });

        logger.info("Source objects additional fields validated successfully");
    }

    @Test(description = "Set Catalog Id to Collection Variables", priority = 13, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Tag")
    @Severity(SeverityLevel.NORMAL)
    public void testSetCatalogId() {
        // Store catalog ID using VariableManager (thread-safe)
        if (catalogTagResponse.getData().getData() != null && !catalogTagResponse.getData().getData().isEmpty()) {
            String catalogId = catalogTagResponse.getData().getData().get(0).get_id();
            VariableManager.set("catalog_id", catalogId);
            
            String storedId = VariableManager.get("catalog_id");
            assertThat("Catalog ID should be set", storedId, notNullValue());

            logger.info("Set catalog ID in VariableManager: {}", catalogId);
        } else {
            logger.warn("No catalog items to set catalog ID");
        }
    }
}

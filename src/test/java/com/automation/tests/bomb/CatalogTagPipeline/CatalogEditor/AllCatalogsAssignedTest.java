package com.automation.tests.bomb.CatalogTagPipeline.CatalogEditor;

import com.automation.base.BaseTest;
import com.automation.constants.BombEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.CatalogEditorResponse;
import com.automation.utils.JsonUtils;
import com.automation.utils.VariableManager;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for BOMB Catalog Editor - All Catalogs Assigned endpoint.
 * Endpoint:
 * {{bizup_base}}/v1/admin/catalog?limit=20&editor={{seller_id}}&sort=status
 * Implements comprehensive Postman test scripts for editor-assigned catalogs
 * validation.
 */
@Epic("BOMB Catalog Tag Pipeline")
@Feature("Catalog Editor")
public class AllCatalogsAssignedTest extends BaseTest {

    private String authToken;
    private Response response;
    private CatalogEditorResponse catalogEditorResponse;

    // Editor ID (using seller_id as per Postman script)

    // Catalog ID is now stored in VariableManager - no static variable needed

    @BeforeClass
    public void setupAuth() {
        // Get token from VariableManager (thread-safe)
        authToken = VariableManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
        logger.info("Using BOMB token from VariableManager");
    }

    @Test(description = "Response status code is 200", priority = 1, groups = "bomb")
    @Story("Catalog Editor - All Catalogs Assigned")
    @Severity(SeverityLevel.BLOCKER)
    public void testStatusCode200() {
        // Send GET request to fetch catalogs assigned to editor
        response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .queryParam("limit", 20)
                .queryParam("editor", VariableManager.get("editor_id"))
                .queryParam("sort", "status")
                .when()
                .get(BombEndpoints.CATALOG);

        // Parse response for other tests
        catalogEditorResponse = JsonUtils.fromResponse(response, CatalogEditorResponse.class);

        // Verify response status is 200 OK
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response time is less than threshold", priority = 2, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - All Catalogs Assigned")
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

    @Test(description = "Validate the response schema for required fields", priority = 3, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - All Catalogs Assigned")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseSchemaValidation() {
        // Validate response is an object
        assertThat("Response should not be null", catalogEditorResponse, notNullValue());

        // Validate response has required fields
        assertThat("Response should have statusCode", catalogEditorResponse.getStatusCode(), notNullValue());
        assertThat("Response should have message", catalogEditorResponse.getMessage(), notNullValue());
        assertThat("Response should have data", catalogEditorResponse.getData(), notNullValue());
        assertThat("Data should be an array", catalogEditorResponse.getData(), instanceOf(List.class));

        // Validate nested structure
        catalogEditorResponse.getData().forEach(group -> {
            assertThat("Group should have pagination", group.getPagination(), notNullValue());
            assertThat("Pagination should be an array", group.getPagination(), instanceOf(List.class));

            group.getPagination().forEach(paginationItem -> {
                assertThat("Pagination item should have total", paginationItem.getTotal(), notNullValue());
            });

            assertThat("Group should have data", group.getData(), notNullValue());
            assertThat("Data should be an array", group.getData(), instanceOf(List.class));

            group.getData().forEach(dataItem -> {
                assertThat("Data item should have _id", dataItem.get_id(), notNullValue());
                assertThat("Data item should have source", dataItem.getSource(), notNullValue());
                assertThat("Data item should have status", dataItem.getStatus(), notNullValue());
                assertThat("Data item should have priority", dataItem.getPriority(), notNullValue());
                assertThat("Data item should have videoType", dataItem.getVideoType(), notNullValue());
                assertThat("Data item should have sellerId", dataItem.getSellerId(), notNullValue());
                assertThat("Data item should have phoneNumber", dataItem.getPhoneNumber(), notNullValue());
                assertThat("Data item should have name", dataItem.getName(), notNullValue());
                assertThat("Data item should have createdAt", dataItem.getCreatedAt(), notNullValue());
                assertThat("Data item should have updatedAt", dataItem.getUpdatedAt(), notNullValue());
                assertThat("Data item should have editorId", dataItem.getEditorId(), notNullValue());
                assertThat("Data item should have total", dataItem.getTotal(), notNullValue());
                assertThat("Data item should have tagged", dataItem.getTagged(), notNullValue());
                assertThat("Data item should have active", dataItem.getActive(), notNullValue());
            });
        });

        logger.info("Response schema validated successfully");
    }

    @Test(description = "Data array is present and contains expected number of elements", priority = 4, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - All Catalogs Assigned")
    @Severity(SeverityLevel.NORMAL)
    public void testDataArrayContainsExpectedElements() {
        // Validate response is an object
        assertThat("Response should not be null", catalogEditorResponse, notNullValue());
        assertThat("Data should exist and be an array", catalogEditorResponse.getData(), notNullValue());
        assertThat("Data array should contain 1 element", catalogEditorResponse.getData().size(), equalTo(1));

        logger.info("Data array validated: contains {} element(s)", catalogEditorResponse.getData().size());
    }

    @Test(description = "Set Catalog foassign ID to Collection Variables", priority = 5, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - All Catalogs Assigned")
    @Severity(SeverityLevel.NORMAL)
    public void testSetCatalogForAssignId() {
        // Check if data array exists and has items
        if (catalogEditorResponse.getData() != null && !catalogEditorResponse.getData().isEmpty()) {
            CatalogEditorResponse.CatalogEditorGroup firstGroup = catalogEditorResponse.getData().get(0);

            if (firstGroup.getData() != null && !firstGroup.getData().isEmpty()) {
                // Find the first item with status = 0
                String foundId = null;

                for (CatalogEditorResponse.CatalogEditorItem item : firstGroup.getData()) {
                    if (item.getStatus() != null && item.getStatus() == 0) {
                        foundId = item.get_id();
                        break;
                    }
                }

                if (foundId != null) {
                    VariableManager.set("catalog_foassign_id", foundId);
                    String storedId = VariableManager.get("catalog_foassign_id");
                    assertThat("Catalog for assign ID should be set", storedId, notNullValue());
                    logger.info("Found and set catalog foassign ID with status 0 in VariableManager: {}", foundId);
                } else {
                    logger.warn("No items with status 0 found");
                }
            } else {
                logger.warn("No data items to process");
            }
        } else {
            logger.warn("No data groups to process");
        }
    }
}

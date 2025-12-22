package com.automation.tests.bomb.CatalogTagPipeline.CatalogEditor;

import com.automation.base.BaseTest;
import com.automation.constants.BombEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.CatalogEditorResponse;
import com.automation.utils.VariableManager;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for BOMB Catalog Editor - Fetch Catalog Uploaded endpoint.
 * Endpoint:
 * {{bizup_base}}/v1/admin/editor/assign/catalog/{{seller_id}}?limit=20&mode=all
 * Implements comprehensive Postman test scripts for fetching uploaded catalogs.
 */
@Epic("BOMB Catalog Tag Pipeline")
@Feature("Catalog Editor")
public class FetchCatalogUploadedTest extends BaseTest {

    private String authToken;
    private Response response;
    private CatalogEditorResponse catalogEditorResponse;


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
    @Story("Catalog Editor - Fetch Catalog Uploaded")
    @Severity(SeverityLevel.BLOCKER)
    public void testStatusCode200() {
        // Send GET request to fetch uploaded catalogs
        response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .queryParam("seller_id", VariableManager.get("seller_id"))
                .queryParam("mode", "all")
                .when()
                .post(BombEndpoints.EDITOR_ASSIGN_CATALOG + "/" + VariableManager.get("seller_id"));

        // Parse response for other tests
        catalogEditorResponse = JsonUtils.fromResponse(response, CatalogEditorResponse.class);

        // Verify response status is 200 OK
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response time is less than threshold", priority = 2, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Fetch Catalog Uploaded")
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

    @Test(description = "Response has the required fields - statusCode, message, and data", priority = 3, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Fetch Catalog Uploaded")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseHasRequiredFields() {
        // Validate response is an object
        assertThat("Response should not be null", catalogEditorResponse, notNullValue());

        // Validate response has required fields
        assertThat("Response should have statusCode", catalogEditorResponse.getStatusCode(), notNullValue());
        assertThat("Response should have message", catalogEditorResponse.getMessage(), notNullValue());
        assertThat("Response should have data", catalogEditorResponse.getData(), notNullValue());

        logger.info("Response has all required fields");
    }

    @Test(description = "Pagination total is a non-negative integer", priority = 4, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Fetch Catalog Uploaded")
    @Severity(SeverityLevel.NORMAL)
    public void testPaginationTotalIsNonNegative() {
        // Check if data array exists and has items
        if (catalogEditorResponse.getData() != null && !catalogEditorResponse.getData().isEmpty()) {
            CatalogEditorResponse.CatalogEditorGroup firstGroup = catalogEditorResponse.getData().get(0);

            if (firstGroup.getPagination() != null && !firstGroup.getPagination().isEmpty()) {
                CatalogEditorResponse.Pagination pagination = firstGroup.getPagination().get(0);

                // Validate pagination total is a non-negative integer
                assertThat("Pagination total should be a number", pagination.getTotal(), instanceOf(Integer.class));
                assertThat("Pagination total should be non-negative", pagination.getTotal(), greaterThanOrEqualTo(0));

                logger.info("Pagination total validated: {}", pagination.getTotal());
            } else {
                logger.warn("No pagination data to validate");
            }
        } else {
            logger.warn("No data groups to validate");
        }
    }

    @Test(description = "Validate the response schema for required fields", priority = 5, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Fetch Catalog Uploaded")
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

    @Test(description = "Data array is present and contains expected number of elements", priority = 6, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Fetch Catalog Uploaded")
    @Severity(SeverityLevel.NORMAL)
    public void testDataArrayContainsExpectedElements() {
        // Validate response is an object
        assertThat("Response should not be null", catalogEditorResponse, notNullValue());
        assertThat("Data should exist and be an array", catalogEditorResponse.getData(), notNullValue());
        assertThat("Data array should contain 1 element", catalogEditorResponse.getData().size(), equalTo(1));

        logger.info("Data array validated: contains {} element(s)", catalogEditorResponse.getData().size());
    }
}

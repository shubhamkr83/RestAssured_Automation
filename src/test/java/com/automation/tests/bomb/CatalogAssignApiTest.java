package com.automation.tests.bomb;

import com.automation.base.BaseTest;
import com.automation.constants.BombEndpoints;
import com.automation.constants.HttpStatus;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for BOMB Catalog Assign/Upload API.
 * Tests catalog upload and assignment functionality.
 */
@Epic("BOMB Catalog Management")
@Feature("Catalog Assign API")
public class CatalogAssignApiTest extends BaseTest {

    private String authToken;
    public static String catalogForAssignId;

    @BeforeClass
    public void setupAuth() {
        if (LoginApiTest.bombToken != null) {
            authToken = LoginApiTest.bombToken;
            logger.info("Using BOMB token from LoginApiTest");
        } else {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
    }

    @Test(description = "Verify catalog uploaded search returns catalogs", priority = 1)
    @Story("Catalog Upload Search")
    @Severity(SeverityLevel.CRITICAL)
    public void testCatalogUploadedSearch() {
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("limit", 20);

        Response response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .queryParams(queryParams)
                .when()
                .get(BombEndpoints.CATALOG);

        // Validate status code
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        // Validate response time
        assertThat("Response time should be acceptable",
                response.getTime(), lessThan((long) config.responseTimeThreshold()));

        // Validate Content-Type header
        assertThat("Content-Type should include application/json",
                response.getContentType(), containsString("application/json"));

        // Validate response structure
        JsonPath jsonPath = response.jsonPath();
        assertThat("Response should have statusCode", jsonPath.get("statusCode"), notNullValue());
        assertThat("Response should have data", jsonPath.get("data"), notNullValue());
        assertThat("Response should have message", jsonPath.get("message"), notNullValue());
        assertThat("Message should be success", jsonPath.getString("message"), equalTo("success"));

        // Validate data array structure
        List<Map<String, Object>> dataList = jsonPath.getList("data");
        if (dataList != null && !dataList.isEmpty()) {
            Map<String, Object> firstDataItem = dataList.get(0);
            List<Map<String, Object>> dataArray = (List<Map<String, Object>>) firstDataItem.get("data");

            if (dataArray != null && !dataArray.isEmpty()) {
                Map<String, Object> firstItem = dataArray.get(0);

                // Validate required fields
                assertThat("Item should have _id", firstItem.get("_id"), notNullValue());
                assertThat("Item should have source", firstItem.get("source"), notNullValue());
                assertThat("Item should have videoType", firstItem.get("videoType"), notNullValue());
                assertThat("Item should have sellerId", firstItem.get("sellerId"), notNullValue());
                assertThat("Item should have phoneNumber", firstItem.get("phoneNumber"), notNullValue());
                assertThat("Item should have name", firstItem.get("name"), notNullValue());

                // Validate data types
                assertThat("_id should be string", firstItem.get("_id"), instanceOf(String.class));
                assertThat("source should be string", firstItem.get("source"), instanceOf(String.class));
                assertThat("videoType should be string", firstItem.get("videoType"), instanceOf(String.class));
                assertThat("sellerId should be string", firstItem.get("sellerId"), instanceOf(String.class));
                assertThat("phoneNumber should be string", firstItem.get("phoneNumber"), instanceOf(String.class));
                assertThat("name should be string", firstItem.get("name"), instanceOf(String.class));

                // Validate field values
                String source = (String) firstItem.get("source");
                assertThat("Source should be valid",
                        source,
                        anyOf(equalTo("auto-catalog"), equalTo("seller-app"), equalTo("pdf"), equalTo("zip-upload")));

                assertThat("VideoType should be catalog",
                        firstItem.get("videoType"), equalTo("catalog"));

                // Validate phone number format
                String phoneNumber = (String) firstItem.get("phoneNumber");
                assertThat("Phone number should match format",
                        phoneNumber, matchesRegex("^\\+[0-9]{10,15}$"));
            }
        }
    }

    @Test(description = "Verify catalog upload search with seller filter", priority = 2)
    @Story("Catalog Upload Search - Seller Filter")
    @Severity(SeverityLevel.CRITICAL)
    public void testCatalogUploadedSearchBySellerFilter() {
        String sellerId = "63ee780c9689be92acce8f35";

        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("seller", sellerId);
        queryParams.put("limit", 20);

        Response response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .queryParams(queryParams)
                .when()
                .get(BombEndpoints.CATALOG);

        // Validate response
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        assertThat("Content-Type should include application/json",
                response.getContentType(), containsString("application/json"));

        JsonPath jsonPath = response.jsonPath();
        assertThat("Message should be success", jsonPath.getString("message"), equalTo("success"));

        // Validate response structure
        assertThat("Response should have required fields",
                jsonPath.get("statusCode"), notNullValue());
        assertThat("Response should have data", jsonPath.get("data"), notNullValue());

        // Validate data array
        List<Map<String, Object>> dataList = jsonPath.getList("data");
        if (dataList != null && !dataList.isEmpty()) {
            Map<String, Object> firstDataItem = dataList.get(0);
            List<Map<String, Object>> catalogItems = (List<Map<String, Object>>) firstDataItem.get("data");

            if (catalogItems != null && !catalogItems.isEmpty()) {
                // Verify all items belong to requested seller
                catalogItems.forEach(item -> {
                    assertThat("Item sellerId should match filter",
                            item.get("sellerId"), equalTo(sellerId));
                });

                // Store catalog ID with status 0 for assignment
                for (Map<String, Object> item : catalogItems) {
                    Integer status = (Integer) item.get("status");
                    if (status != null && status == 0) {
                        catalogForAssignId = (String) item.get("_id");
                        logger.info("Stored catalog for assign ID: {}", catalogForAssignId);
                        break;
                    }
                }

                // Validate first item structure
                Map<String, Object> firstItem = catalogItems.get(0);
                assertThat("Item should have required fields", firstItem.get("_id"), notNullValue());
                assertThat("VideoType should be catalog", firstItem.get("videoType"), equalTo("catalog"));

                // Validate phone number format
                String phoneNumber = (String) firstItem.get("phoneNumber");
                assertThat("Phone number should match format",
                        phoneNumber, matchesRegex("^\\+[0-9]{10,15}$"));
            }
        }

        // Validate response time
        assertThat("Response time should be acceptable",
                response.getTime(), lessThan((long) config.responseTimeThreshold()));
    }

    @Test(description = "Verify all catalog items belong to specified seller", dependsOnMethods = "testCatalogUploadedSearchBySellerFilter")
    @Story("Catalog Upload Search - Seller Validation")
    @Severity(SeverityLevel.NORMAL)
    public void testAllItemsBelongToSeller() {
        String sellerId = "63ee780c9689be92acce8f35";

        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("seller", sellerId);
        queryParams.put("limit", 20);

        Response response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .queryParams(queryParams)
                .when()
                .get(BombEndpoints.CATALOG);

        JsonPath jsonPath = response.jsonPath();
        List<Map<String, Object>> dataList = jsonPath.getList("data");

        assertThat("Data list should not be empty", dataList, not(empty()));

        if (!dataList.isEmpty()) {
            Map<String, Object> firstDataItem = dataList.get(0);
            List<Map<String, Object>> catalogItems = (List<Map<String, Object>>) firstDataItem.get("data");

            assertThat("Catalog items should not be empty", catalogItems, not(empty()));

            // Verify all items match seller ID
            catalogItems.forEach(item -> {
                String itemSellerId = (String) item.get("sellerId");
                assertThat(String.format("Item %s should belong to seller %s", item.get("_id"), sellerId),
                        itemSellerId, equalTo(sellerId));
            });
        }
    }
}

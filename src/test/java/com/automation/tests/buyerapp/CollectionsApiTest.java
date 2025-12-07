package com.automation.tests.buyerapp;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for Buyer App Collections API.
 */
@Epic("Buyer App Collections")
@Feature("Collections API")
public class CollectionsApiTest extends BaseTest {

    private String authToken;
    private String buyerAppBaseUrl;
    public static String collectionId;

    @BeforeClass
    public void setupAuth() {
        buyerAppBaseUrl = config.buyerAppBaseUrl();

        if (BuyerLoginApiTest.buyerAppToken != null) {
            authToken = BuyerLoginApiTest.buyerAppToken;
            logger.info("Using Buyer App token from BuyerLoginApiTest");
        } else {
            throw new RuntimeException("Buyer App token not available. Please run BuyerLoginApiTest first.");
        }
    }

    @Test(description = "Verify get all collections for Saree", priority = 1)
    @Story("All Collections")
    @Severity(SeverityLevel.CRITICAL)
    public void testGetAllCollectionsSaree() {
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("suitable_for", "saree");

        Response response = RestAssured.given()
                .baseUri(buyerAppBaseUrl)
                .header("Authorization", "JWT " + authToken)
                .header("AppVersion", "3.2.0-debug")
                .header("AppVersionCode", "154")
                .header("User-Segment", "2")
                .header("Accept-Language", "en")
                .queryParams(queryParams)
                .when()
                .get(BuyerAppEndpoints.COLLECTION_ALL);

        // Validate response
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        assertThat("Response time should be acceptable",
                response.getTime(), lessThan((long) config.responseTimeThreshold()));

        JsonPath jsonPath = response.jsonPath();

        // Validate response structure
        assertThat("statusCode should be present", jsonPath.get("statusCode"), notNullValue());
        assertThat("message should be present", jsonPath.get("message"), notNullValue());
        assertThat("data.result should be array", jsonPath.getList("data.result"), notNullValue());

        // Store collection ID for future tests
        if (jsonPath.getList("data.result") != null && !jsonPath.getList("data.result").isEmpty()) {
            collectionId = jsonPath.getString("data.result[0]._id");
            logger.info("Stored collection ID: {}", collectionId);

            // Validate collection structure
            assertThat("Collection should have _id", jsonPath.get("data.result[0]._id"), notNullValue());
            assertThat("Collection should have name", jsonPath.get("data.result[0].name"), notNullValue());
            assertThat("Collection should have catalogCount", jsonPath.get("data.result[0].catalogCount"),
                    notNullValue());
        }
    }

    @Test(description = "Verify get all collections for Readymade", priority = 2)
    @Story("All Collections")
    @Severity(SeverityLevel.NORMAL)
    public void testGetAllCollectionsReadymade() {
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("suitable_for", "readymade");

        Response response = RestAssured.given()
                .baseUri(buyerAppBaseUrl)
                .header("Authorization", "JWT " + authToken)
                .header("AppVersion", "3.2.0-debug")
                .header("AppVersionCode", "154")
                .header("User-Segment", "2")
                .header("Accept-Language", "en")
                .queryParams(queryParams)
                .when()
                .get(BuyerAppEndpoints.COLLECTION_ALL);

        // Validate response
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        assertThat("Response time should be acceptable",
                response.getTime(), lessThan((long) config.responseTimeThreshold()));

        JsonPath jsonPath = response.jsonPath();

        // Validate response structure
        assertThat("statusCode should be present", jsonPath.get("statusCode"), notNullValue());
        assertThat("data.result should be array", jsonPath.getList("data.result"), notNullValue());
    }

    @Test(description = "Verify get top collections", priority = 3)
    @Story("Top Collections")
    @Severity(SeverityLevel.NORMAL)
    public void testGetTopCollections() {
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("suitable_for", "saree");

        Response response = RestAssured.given()
                .baseUri(buyerAppBaseUrl)
                .header("Authorization", "JWT " + authToken)
                .header("AppVersion", "3.2.0-debug")
                .header("AppVersionCode", "154")
                .header("User-Segment", "2")
                .header("Accept-Language", "en")
                .queryParams(queryParams)
                .when()
                .get(BuyerAppEndpoints.COLLECTION_TOP);

        // Validate response
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        assertThat("Response time should be acceptable",
                response.getTime(), lessThan((long) config.responseTimeThreshold()));

        JsonPath jsonPath = response.jsonPath();

        // Validate response has required fields
        assertThat("Response should have statusCode", jsonPath.get("statusCode"), notNullValue());
        assertThat("Response should have message", jsonPath.get("message"), notNullValue());
        assertThat("Response should have data", jsonPath.get("data"), notNullValue());
    }

    @Test(description = "Verify collection count is reasonable", dependsOnMethods = "testGetAllCollectionsSaree")
    @Story("All Collections")
    @Severity(SeverityLevel.NORMAL)
    public void testCollectionCount() {
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("suitable_for", "saree");

        Response response = RestAssured.given()
                .baseUri(buyerAppBaseUrl)
                .header("Authorization", "JWT " + authToken)
                .header("AppVersion", "3.2.0-debug")
                .header("AppVersionCode", "154")
                .header("User-Segment", "2")
                .header("Accept-Language", "en")
                .queryParams(queryParams)
                .when()
                .get(BuyerAppEndpoints.COLLECTION_ALL);

        JsonPath jsonPath = response.jsonPath();

        // Validate collection count
        int collectionCount = jsonPath.getList("data.result").size();
        assertThat("Collection count should be at least 0", collectionCount, greaterThanOrEqualTo(0));

        logger.info("Total collections found: {}", collectionCount);
    }

    @Test(description = "Verify each collection has required fields", dependsOnMethods = "testGetAllCollectionsSaree")
    @Story("All Collections")
    @Severity(SeverityLevel.NORMAL)
    public void testCollectionFields() {
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("suitable_for", "saree");

        Response response = RestAssured.given()
                .baseUri(buyerAppBaseUrl)
                .header("Authorization", "JWT " + authToken)
                .header("AppVersion", "3.2.0-debug")
                .header("AppVersionCode", "154")
                .header("User-Segment", "2")
                .header("Accept-Language", "en")
                .queryParams(queryParams)
                .when()
                .get(BuyerAppEndpoints.COLLECTION_ALL);

        JsonPath jsonPath = response.jsonPath();

        // Validate each collection has required fields
        if (jsonPath.getList("data.result") != null && !jsonPath.getList("data.result").isEmpty()) {
            for (int i = 0; i < Math.min(5, jsonPath.getList("data.result").size()); i++) {
                String basePath = String.format("data.result[%d]", i);
                assertThat("Collection should have _id", jsonPath.get(basePath + "._id"), notNullValue());
                assertThat("Collection should have name", jsonPath.get(basePath + ".name"), notNullValue());

                // Validate name is a string
                assertThat("Collection name should be string",
                        jsonPath.get(basePath + ".name"), instanceOf(String.class));
            }
        }
    }

    @Test(description = "Verify response headers are correct")
    @Story("All Collections")
    @Severity(SeverityLevel.MINOR)
    public void testCollectionResponseHeaders() {
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("suitable_for", "saree");

        Response response = RestAssured.given()
                .baseUri(buyerAppBaseUrl)
                .header("Authorization", "JWT " + authToken)
                .header("AppVersion", "3.2.0-debug")
                .header("AppVersionCode", "154")
                .header("User-Segment", "2")
                .header("Accept-Language", "en")
                .queryParams(queryParams)
                .when()
                .get(BuyerAppEndpoints.COLLECTION_ALL);

        assertThat("Content-Type should be application/json",
                response.getContentType(), containsString("application/json"));
    }
}

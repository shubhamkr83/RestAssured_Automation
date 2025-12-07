package com.automation.tests.buyerapp;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.SearchResponse;
import com.automation.utils.JsonUtils;
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
 * Test class for Buyer App Search API.
 * Tests user/seller search functionality with filters and recommendations.
 */
@Epic("Buyer App Search")
@Feature("Search API")
public class SearchApiTest extends BaseTest {

    private String authToken;
    private String buyerAppBaseUrl;
    public static String recommendedProduct;
    public static String recommendedProductId;

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

    @Test(description = "Verify search product with query and pagination", priority = 1)
    @Story("Product Search")
    @Severity(SeverityLevel.CRITICAL)
    public void testSearchProduct() {
        String searchQuery = "jeans";
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("q", searchQuery);
        queryParams.put("page", 1);
        queryParams.put("pageSize", 20);

        Response response = RestAssured.given()
                .baseUri(buyerAppBaseUrl)
                .header("Authorization", "JWT " + authToken)
                .header("AppVersion", "3.2.0-debug")
                .header("AppVersionCode", "154")
                .header("User-Segment", "2")
                .header("Accept-Language", "en")
                .queryParams(queryParams)
                .when()
                .get(BuyerAppEndpoints.USER_SEARCH);

        // Validate status code
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        // Validate response time
        assertThat("Response time should be acceptable",
                response.getTime(), lessThan((long) config.responseTimeThreshold()));

        // Validate Content-Type
        assertThat("Content-Type should be application/json",
                response.getContentType(), containsString("application/json"));

        // Validate response has valid JSON body
        assertThat("Response should have JSON body",
                response.getBody().asString(), not(emptyOrNullString()));

        JsonPath jsonPath = response.jsonPath();

        // Validate response structure
        assertThat("statusCode should be 10000", jsonPath.getString("statusCode"), equalTo("10000"));
        assertThat("message should be success", jsonPath.getString("message"), equalTo("success"));
        assertThat("data should be present", jsonPath.get("data"), notNullValue());
        assertThat("data.items should be array", jsonPath.getList("data.items"), notNullValue());

        // Validate items array if present
        if (jsonPath.getList("data.items") != null && !jsonPath.getList("data.items").isEmpty()) {
            assertThat("Items should have at least 1 result",
                    jsonPath.getList("data.items").size(), greaterThanOrEqualTo(1));

            // Validate first item structure
            Map<String, Object> firstItem = jsonPath.getMap("data.items[0]");
            assertThat("Item should have _id", firstItem.get("_id"), notNullValue());
            assertThat("Item should have name", firstItem.get("name"), notNullValue());

            // Validate seller object if present
            if (firstItem.containsKey("seller")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> seller = (Map<String, Object>) firstItem.get("seller");
                assertThat("Seller should have phoneNumber", seller.get("phoneNumber"), notNullValue());
                assertThat("Seller should have address", seller.get("address"), notNullValue());
                assertThat("Seller should have businessName", seller.get("businessName"), notNullValue());
                assertThat("Seller should have name", seller.get("name"), notNullValue());
                assertThat("Seller should have _id", seller.get("_id"), notNullValue());
                assertThat("Seller should have deprioritisation_status",
                        seller.get("deprioritisation_status"), instanceOf(Boolean.class));
                assertThat("Seller should have isCatalogAvailable",
                        seller.get("isCatalogAvailable"), instanceOf(Boolean.class));

                // Validate phone number format if present
                if (seller.get("phoneNumber") != null) {
                    String phoneNumber = (String) seller.get("phoneNumber");
                    assertThat("Phone number should start with +",
                            phoneNumber, startsWith("+"));
                    assertThat("Phone number should be valid international format",
                            phoneNumber, matchesRegex("^\\+[0-9]{10,15}$"));
                }
            }

            // Security validation - no sensitive data exposed
            String responseBody = response.getBody().asString();
            assertThat("Response should not contain token", responseBody, not(containsString("\"token\"")));
            assertThat("Response should not contain password", responseBody, not(containsString("\"password\"")));
        }

        logger.info("Search product test completed with query: {}", searchQuery);
    }

    @Test(description = "Verify recommended chips/buckets for search", priority = 2, dependsOnMethods = "testSearchProduct")
    @Story("Recommended Chips")
    @Severity(SeverityLevel.CRITICAL)
    public void testSearchRecommendedChips() {
        String searchQuery = "saree";
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("q", searchQuery);
        queryParams.put("from", 0);
        queryParams.put("to", 1000000000);
        queryParams.put("page", 0);
        queryParams.put("pageSize", 0);

        Response response = RestAssured.given()
                .baseUri(buyerAppBaseUrl)
                .header("Authorization", "JWT " + authToken)
                .header("AppVersion", "3.2.0-debug")
                .header("AppVersionCode", "154")
                .header("User-Segment", "2")
                .header("Accept-Language", "en")
                .queryParams(queryParams)
                .when()
                .get(BuyerAppEndpoints.USER_SEARCH);

        // Validate response
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        assertThat("Response time should be acceptable",
                response.getTime(), lessThan((long) config.responseTimeThreshold()));

        assertThat("Content-Type should be application/json",
                response.getContentType(), containsString("application/json"));

        // Parse response
        SearchResponse searchResponse = JsonUtils.fromResponse(response, SearchResponse.class);

        // Validate response structure
        assertThat("Response should not be null", searchResponse, notNullValue());
        assertThat("StatusCode should be 10000", searchResponse.getStatusCode(), equalTo("10000"));
        assertThat("Message should be success", searchResponse.getMessage(), equalTo("success"));
        assertThat("Data should be present", searchResponse.getData(), notNullValue());
        assertThat("Items should be array", searchResponse.getData().getItems(), notNullValue());

        JsonPath jsonPath = response.jsonPath();

        // Validate items if present
        if (searchResponse.getData().getItems() != null && !searchResponse.getData().getItems().isEmpty()) {
            SearchResponse.UserItem firstUser = searchResponse.getData().getItems().get(0);

            assertThat("User should have _id", firstUser.get_id(), notNullValue());
            assertThat("User _id should be string", firstUser.get_id(), instanceOf(String.class));
            assertThat("User should have name", firstUser.getName(), notNullValue());
            assertThat("User name should be string", firstUser.getName(), instanceOf(String.class));

            // Validate phone number format if present
            if (firstUser.getPhoneNumber() != null) {
                assertThat("Phone number should be valid international format",
                        firstUser.getPhoneNumber(), matchesRegex("^\\+[0-9]{10,15}$"));
            }

            // Validate business info if present
            if (firstUser.getBusinessInfo() != null) {
                assertThat("BusinessInfo should have businessName",
                        firstUser.getBusinessInfo().getBusinessName(), notNullValue());
                assertThat("BusinessName should be string",
                        firstUser.getBusinessInfo().getBusinessName(), instanceOf(String.class));
            }

            // Validate search results contain query term (case-insensitive)
            String lowerQuery = searchQuery.toLowerCase();
            for (SearchResponse.UserItem user : searchResponse.getData().getItems()) {
                String searchableText = "";
                if (user.getName() != null)
                    searchableText += user.getName().toLowerCase() + " ";
                if (user.getBusinessInfo() != null && user.getBusinessInfo().getBusinessName() != null)
                    searchableText += user.getBusinessInfo().getBusinessName().toLowerCase() + " ";
                if (user.getBusinessInfo() != null && user.getBusinessInfo().getDescription() != null)
                    searchableText += user.getBusinessInfo().getDescription().toLowerCase() + " ";
                if (user.getTags() != null)
                    searchableText += String.join(" ", user.getTags()).toLowerCase();

                // At least one field should contain the search term
                assertThat("Search result should contain query term",
                        searchableText, containsString(lowerQuery));
            }
        }

        // Validate and store recommended buckets if present
        if (jsonPath.getList("data.buckets") != null && !jsonPath.getList("data.buckets").isEmpty()) {
            Map<String, Object> firstBucket = jsonPath.getMap("data.buckets[0]");
            recommendedProduct = (String) firstBucket.get("name");
            recommendedProductId = (String) firstBucket.get("_id");

            logger.info("Stored recommended product: {} (ID: {})", recommendedProduct, recommendedProductId);

            assertThat("Bucket should have name", firstBucket.get("name"), notNullValue());
            assertThat("Bucket should have _id", firstBucket.get("_id"), notNullValue());
        }

        // Security validation
        String responseBody = response.getBody().asString();
        assertThat("Response should not contain password", responseBody, not(containsString("\"password\"")));
        assertThat("Response should not contain creditCard", responseBody, not(containsString("\"creditCard\"")));
        assertThat("Response should not contain token", responseBody, not(containsString("\"token\"")));

        logger.info("Recommended chips test completed successfully");
    }

    @Test(description = "Verify search with recommended chip/product filter", priority = 3, dependsOnMethods = "testSearchRecommendedChips")
    @Story("Recommended Chip Select")
    @Severity(SeverityLevel.NORMAL)
    public void testSearchWithRecommendChipSelect() {
        // Use stored recommended product or default
        String searchQuery = recommendedProduct != null ? recommendedProduct : "lower";
        String productId = recommendedProductId != null ? recommendedProductId : "645b93e45c2997f4f2e82c50";

        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("q", searchQuery);
        queryParams.put("product", productId);
        queryParams.put("page", 1);
        queryParams.put("pageSize", 20);

        Response response = RestAssured.given()
                .baseUri(buyerAppBaseUrl)
                .header("Authorization", "JWT " + authToken)
                .header("AppVersion", "3.2.0-debug")
                .header("AppVersionCode", "154")
                .header("User-Segment", "2")
                .header("Accept-Language", "en")
                .queryParams(queryParams)
                .when()
                .get(BuyerAppEndpoints.USER_SEARCH);

        // Validate response
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        assertThat("Response time should be acceptable",
                response.getTime(), lessThan((long) config.responseTimeThreshold()));

        assertThat("Content-Type should be application/json",
                response.getContentType(), containsString("application/json"));

        JsonPath jsonPath = response.jsonPath();

        // Validate response structure
        assertThat("statusCode should be 10000", jsonPath.getString("statusCode"), equalTo("10000"));
        assertThat("message should be success", jsonPath.getString("message"), equalTo("success"));
        assertThat("data should be present", jsonPath.get("data"), notNullValue());
        assertThat("data.items should be array", jsonPath.getList("data.items"), notNullValue());

        // Validate pagination fields
        if (jsonPath.get("data.totalCount") != null) {
            assertThat("totalCount should not be negative",
                    jsonPath.getInt("data.totalCount"), greaterThanOrEqualTo(0));
        }

        // Validate items if present
        if (jsonPath.getList("data.items") != null && !jsonPath.getList("data.items").isEmpty()) {
            assertThat("Items should have at least 1 result",
                    jsonPath.getList("data.items").size(), greaterThanOrEqualTo(1));

            // Validate first item has required fields
            assertThat("First item should have _id", jsonPath.get("data.items[0]._id"), notNullValue());
            assertThat("First item should have name", jsonPath.get("data.items[0].name"), notNullValue());

            // Validate phone number format if present
            if (jsonPath.get("data.items[0].phoneNumber") != null) {
                String phoneNumber = jsonPath.getString("data.items[0].phoneNumber");
                assertThat("Phone number should be valid international format",
                        phoneNumber, matchesRegex("^\\+[0-9]{10,15}$"));
            }
        }

        // Security validation
        String responseBody = response.getBody().asString();
        assertThat("Response should not contain sensitive data",
                responseBody, not(containsString("\"password\"")));

        logger.info("Recommend chip select test completed with product filter: {}", productId);
    }

    @Test(description = "Verify search response headers", priority = 4)
    @Story("Product Search")
    @Severity(SeverityLevel.MINOR)
    public void testSearchResponseHeaders() {
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("q", "shirt");
        queryParams.put("page", 1);
        queryParams.put("pageSize", 10);

        Response response = RestAssured.given()
                .baseUri(buyerAppBaseUrl)
                .header("Authorization", "JWT " + authToken)
                .header("AppVersion", "3.2.0-debug")
                .header("AppVersionCode", "154")
                .header("User-Segment", "2")
                .header("Accept-Language", "en")
                .queryParams(queryParams)
                .when()
                .get(BuyerAppEndpoints.USER_SEARCH);

        // Validate headers
        assertThat("Content-Type header should be present",
                response.getHeader("Content-Type"), notNullValue());
        assertThat("Content-Type should be application/json",
                response.getContentType(), containsString("application/json"));
    }
}

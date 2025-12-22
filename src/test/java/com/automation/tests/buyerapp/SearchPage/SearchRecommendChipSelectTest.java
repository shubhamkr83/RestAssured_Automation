package com.automation.tests.buyerapp.SearchPage;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.SearchRecommendChipSelectResponse;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.automation.utils.VariableManager;
import static com.automation.tests.buyerapp.SearchPage.SearchRecommendedChipsTest.searchRecommend;
import static com.automation.tests.buyerapp.SearchPage.SearchRecommendedChipsTest.searchRecommendId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for Search Recommend Chip Select API - Converted from Postman
 * Script.
 * Endpoint:
 * {{navo_base}}/v1/user/search?q={{search_recommend}}&product={{search_recommend_id}}&page=1&pageSize=20
 * Validates response structure, items array, and buckets.
 */
@Epic("Buyer App Search Page")
@Feature("Search Recommend Chip Select API")
public class SearchRecommendChipSelectTest extends BaseTest {

        private static Response searchChipSelectResponse;
        private static SearchRecommendChipSelectResponse searchChipSelectResponseData;
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

        @Test(description = "Response status code is 200", priority = 1, groups = "buyerapp")
        @Story("Search Recommend Chip Select")
        @Severity(SeverityLevel.CRITICAL)
        public void testResponseStatusCode200() {
                // Get search recommend parameters from previous test
                String searchQuery = (searchRecommend != null && !searchRecommend.isEmpty())
                                ? searchRecommend
                                : "saree"; // Default value
                String productId = (searchRecommendId != null && !searchRecommendId.isEmpty())
                                ? searchRecommendId
                                : ""; // Default empty

                // Send GET request with authentication and query parameters
                searchChipSelectResponse = RestAssured.given()
                                .baseUri(buyerAppBaseUrl)
                                .contentType("application/json")
                                .header("Authorization", "JWT " + VariableManager.getBuyerAppToken())
                                .queryParam("q", searchQuery)
                                .queryParam("product", productId)
                                .queryParam("page", 1)
                                .queryParam("pageSize", 20)
                                .when()
                                .get(BuyerAppEndpoints.USER_SEARCH);

                // Parse response for other tests
                searchChipSelectResponseData = JsonUtils.fromResponse(searchChipSelectResponse,
                                SearchRecommendChipSelectResponse.class);

                // Response status code is 200
                assertThat("Response status code is 200",
                                searchChipSelectResponse.getStatusCode(), equalTo(HttpStatus.OK));

                logger.info("Response status verified: 200 OK");
        }

        @Test(description = "Response time is less than threshold", priority = 2, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
        @Story("Search Recommend Chip Select")
        @Severity(SeverityLevel.NORMAL)
        public void testResponseTime() {
                // Get response time threshold from config (fallback to 20000ms)
                long responseTimeThreshold = config.responseTimeThreshold();
                long actualResponseTime = searchChipSelectResponse.getTime();

                // Validate response time measurement is available
                assertThat("Response time measurement should be available",
                                actualResponseTime, notNullValue());

                // Response time is less than threshold
                assertThat(String.format("Response time is less than %dms", responseTimeThreshold),
                                actualResponseTime, lessThan(responseTimeThreshold));

                logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                                responseTimeThreshold);
        }

        @Test(description = "Response contains required fields: statusCode, message, and data", priority = 3, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
        @Story("Search Recommend Chip Select")
        @Severity(SeverityLevel.CRITICAL)
        public void testRequiredFields() {
                // Response contains required fields
                assertThat("Response should be an object", searchChipSelectResponseData, notNullValue());
                assertThat("statusCode should be present", searchChipSelectResponseData.getStatusCode(),
                                notNullValue());
                assertThat("message should be present", searchChipSelectResponseData.getMessage(), notNullValue());
                assertThat("data should be present", searchChipSelectResponseData.getData(), notNullValue());

                logger.info("Required fields validated: statusCode, message, data");
        }

        @Test(description = "Validate structure of 'items' array and nested objects", priority = 4, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
        @Story("Search Recommend Chip Select")
        @Severity(SeverityLevel.CRITICAL)
        public void testItemsArrayStructure() {
                // Validate structure of 'items' array and nested objects
                assertThat("Response should be an object", searchChipSelectResponseData, notNullValue());
                assertThat("data.items should be an array and not empty",
                                searchChipSelectResponseData.getData().getItems(),
                                allOf(instanceOf(java.util.List.class), not(empty())));

                searchChipSelectResponseData.getData().getItems().forEach(item -> {
                        assertThat("Item should be an object", item, notNullValue());
                        assertThat("seller should be an object", item.getSeller(), notNullValue());
                        assertThat("thumbnail should be an object", item.getThumbnail(), notNullValue());
                        assertThat("product should be an array with at least 1 element",
                                        item.getProduct(), hasSize(greaterThanOrEqualTo(1)));
                        assertThat("tags should be an array with at least 1 element",
                                        item.getTags(), hasSize(greaterThanOrEqualTo(1)));
                        assertThat("market should be an object", item.getMarket(), notNullValue());
                        assertThat("attributes should be an array with at least 1 element",
                                        item.getAttributes(), hasSize(greaterThanOrEqualTo(1)));
                });

                logger.info("Items array structure validated: {} items found",
                                searchChipSelectResponseData.getData().getItems().size());
        }

        @Test(description = "Buckets array is present and has at least one element", priority = 5, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
        @Story("Search Recommend Chip Select")
        @Severity(SeverityLevel.NORMAL)
        public void testBucketsArrayPresent() {
                // Buckets array is present and has at least one element
                assertThat("Response should be an object", searchChipSelectResponseData, notNullValue());
                assertThat("data.buckets should be an array with at least 1 element",
                                searchChipSelectResponseData.getData().getBuckets(),
                                hasSize(greaterThanOrEqualTo(1)));

                logger.info("Buckets array validated: {} buckets found",
                                searchChipSelectResponseData.getData().getBuckets().size());
        }
}

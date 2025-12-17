package com.automation.tests.buyerapp.SearchPage;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.SearchRecommendedChipsResponse;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.automation.utils.VariableManager;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for Search Recommended Chips API - Converted from Postman Script.
 * Endpoint: {{navo_base}}/v1/user/search?q={{search_product}}&from=0&to=1000000000&page=0&pageSize=0
 * Validates response structure, search results, and security.
 */
@Epic("Buyer App Search Page")
@Feature("Search Recommended Chips API")
public class SearchRecommendedChipsTest extends BaseTest {

    private static Response searchRecommendedResponse;
    private static SearchRecommendedChipsResponse searchRecommendedResponseData;
    private String buyerAppBaseUrl;
    private static final String SEARCH_QUERY = "saree"; // Default search query
    public static String searchRecommend; // Store recommended product name
    public static String searchRecommendId; // Store recommended product ID

    @BeforeClass
    public void setupBuyerApp() {
        buyerAppBaseUrl = config.buyerAppBaseUrl();
        logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);
    }

    @Test(description = "Status code is 200", priority = 1, groups = "buyerapp")
    @Story("Search Recommended Chips")
    @Severity(SeverityLevel.CRITICAL)
    public void testStatusCode200() {
        // Send GET request with authentication and query parameters
        searchRecommendedResponse = RestAssured.given()
                .baseUri(buyerAppBaseUrl)
                .contentType("application/json")
                .header("Authorization", "Bearer " + VariableManager.getBuyerAppToken())
                .queryParam("q", SEARCH_QUERY)
                .queryParam("from", 0)
                .queryParam("to", 1000000000)
                .queryParam("page", 0)
                .queryParam("pageSize", 0)
                .when()
                .get(BuyerAppEndpoints.USER_SEARCH);

        // Parse response for other tests
        searchRecommendedResponseData = JsonUtils.fromResponse(searchRecommendedResponse, SearchRecommendedChipsResponse.class);

        // Status code is 200
        assertThat("Status code is 200",
                searchRecommendedResponse.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response time is less than threshold", priority = 2, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
    @Story("Search Recommended Chips")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTime() {
        // Get response time threshold from config (fallback to 20000ms)
        long responseTimeThreshold = config.responseTimeThreshold();
        long actualResponseTime = searchRecommendedResponse.getTime();

        // Validate response time measurement is available
        assertThat("Response time measurement should be available",
                actualResponseTime, notNullValue());

        // Response time is less than threshold
        assertThat(String.format("Response time is less than %dms", responseTimeThreshold),
                actualResponseTime, lessThan(responseTimeThreshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                responseTimeThreshold);
    }

    @Test(description = "Response has valid JSON body", priority = 3, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
    @Story("Search Recommended Chips")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseHasValidJsonBody() {
        // Response has valid JSON body
        assertThat("Response should have a body",
                searchRecommendedResponse.getBody().asString(), not(emptyOrNullString()));
        assertThat("Response should be parseable as JSON",
                searchRecommendedResponseData, notNullValue());

        logger.info("Response has valid JSON body");
    }

    @Test(description = "Required headers are present and valid", priority = 4, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
    @Story("Search Recommended Chips")
    @Severity(SeverityLevel.MINOR)
    public void testRequiredHeaders() {
        // Required headers are present and valid
        assertThat("Content-Type header should be present",
                searchRecommendedResponse.getHeader("Content-Type"), notNullValue());
        assertThat("Content-Type should include application/json",
                searchRecommendedResponse.getHeader("Content-Type"), containsString("application/json"));

        logger.info("Required headers validated: Content-Type = {}", 
                searchRecommendedResponse.getHeader("Content-Type"));
    }

    @Test(description = "Validate search response structure and key fields", priority = 5, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
    @Story("Search Recommended Chips")
    @Severity(SeverityLevel.CRITICAL)
    public void testSearchResponseStructure() {
        // Validate search response structure and key fields
        assertThat("Response should be an object", searchRecommendedResponseData, notNullValue());
        assertThat("statusCode should be '10000'",
                searchRecommendedResponseData.getStatusCode(), equalTo("10000"));
        assertThat("message should be 'success'",
                searchRecommendedResponseData.getMessage(), equalTo("success"));
        assertThat("data should be an object",
                searchRecommendedResponseData.getData(), notNullValue());
        assertThat("data.items should be an array",
                searchRecommendedResponseData.getData().getItems(), instanceOf(java.util.List.class));

        if (!searchRecommendedResponseData.getData().getItems().isEmpty()) {
            SearchRecommendedChipsResponse.SearchUserItem user = searchRecommendedResponseData.getData().getItems().get(0);
            assertThat("_id should be a string", user.get_id(), instanceOf(String.class));
            assertThat("name should be a string", user.getName(), instanceOf(String.class));

            if (user.getPhoneNumber() != null) {
                assertThat("Phone number should be in international format",
                        user.getPhoneNumber(), matchesRegex("^\\+[0-9]{10,15}$"));
            }

            if (user.getBusinessInfo() != null) {
                assertThat("businessInfo.businessName should be a string",
                        user.getBusinessInfo().getBusinessName(), instanceOf(String.class));
            }
        }

        logger.info("Search response structure validated");
    }

    @Test(description = "Verify search results contain the query term 'saree'", priority = 6, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
    @Story("Search Recommended Chips")
    @Severity(SeverityLevel.NORMAL)
    public void testSearchResultsContainQueryTerm() {
        java.util.List<SearchRecommendedChipsResponse.SearchUserItem> items = 
                searchRecommendedResponseData.getData().getItems();

        if (!items.isEmpty()) {
            items.forEach(user -> {
                StringBuilder searchFields = new StringBuilder();
                
                if (user.getName() != null) searchFields.append(user.getName()).append(" ");
                if (user.getBusinessInfo() != null) {
                    if (user.getBusinessInfo().getBusinessName() != null) 
                        searchFields.append(user.getBusinessInfo().getBusinessName()).append(" ");
                    if (user.getBusinessInfo().getDescription() != null) 
                        searchFields.append(user.getBusinessInfo().getDescription()).append(" ");
                }
                if (user.getTags() != null) 
                    searchFields.append(String.join(" ", user.getTags()));

                assertThat("Search fields should contain 'saree'",
                        searchFields.toString().toLowerCase(), containsString("saree"));
            });
        } else {
            Integer totalCount = searchRecommendedResponseData.getData().getTotalCount();
            assertThat("Total count should be 0 when no items",
                    totalCount != null ? totalCount : 0, equalTo(0));
        }

        logger.info("Search results contain query term validation completed");
    }

    @Test(description = "No sensitive data exposed and phone numbers are valid", priority = 7, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
    @Story("Search Recommended Chips")
    @Severity(SeverityLevel.BLOCKER)
    public void testSecurityValidation() {
        String responseBody = searchRecommendedResponse.getBody().asString();
        
        // No sensitive data exposed
        assertThat("Response should not include 'password'",
                responseBody, not(containsString("password")));
        assertThat("Response should not include 'creditCard'",
                responseBody, not(containsString("creditCard")));
        assertThat("Response should not include 'token'",
                responseBody, not(containsString("token")));

        // Phone numbers are valid
        searchRecommendedResponseData.getData().getItems().forEach(user -> {
            if (user.getPhoneNumber() != null) {
                assertThat("Phone number should be in international format",
                        user.getPhoneNumber(), matchesRegex("^\\+[0-9]{10,15}$"));
            }
        });

        logger.info("Security validation passed: no sensitive data exposed, phone numbers valid");
    }

    @Test(description = "Set recommended product and ID", priority = 8, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
    @Story("Search Recommended Chips")
    @Severity(SeverityLevel.NORMAL)
    public void testSetRecommendedProductAndId() {
        // Set recommended product and ID from buckets
        if (searchRecommendedResponseData.getData().getBuckets() != null 
                && !searchRecommendedResponseData.getData().getBuckets().isEmpty()) {
            searchRecommend = searchRecommendedResponseData.getData().getBuckets().get(0).getName();
            searchRecommendId = searchRecommendedResponseData.getData().getBuckets().get(0).get_id();

            assertThat("searchRecommend should not be null", searchRecommend, notNullValue());
            assertThat("searchRecommendId should not be null", searchRecommendId, notNullValue());

            logger.info("Recommended product set: {} (ID: {})", searchRecommend, searchRecommendId);
        } else {
            logger.warn("No buckets found in response to set recommended product");
        }
    }
}
